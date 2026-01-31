# Production Environment RDS Configuration

terraform {
  required_version = ">= 1.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    random = {
      source  = "hashicorp/random"
      version = "~> 3.5"
    }
  }

  # IMPORTANT: Configure remote state for production
  # backend "s3" {
  #   bucket         = "your-terraform-state-bucket"
  #   key            = "real-estate/prod/rds/terraform.tfstate"
  #   region         = "us-east-1"
  #   encrypt        = true
  #   dynamodb_table = "terraform-state-lock"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "RealEstate"
      Environment = "Production"
      ManagedBy   = "Terraform"
    }
  }
}

# Get default VPC or create custom VPC (recommended for production)
data "aws_vpc" "main" {
  id = var.vpc_id != "" ? var.vpc_id : data.aws_vpc.default[0].id
}

data "aws_vpc" "default" {
  count   = var.vpc_id == "" ? 1 : 0
  default = true
}

# Get subnets
data "aws_subnets" "main" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main.id]
  }

  filter {
    name   = "tag:Tier"
    values = ["private"] # Use private subnets for production RDS
  }
}

# Fallback to all subnets if no private subnets tagged
data "aws_subnets" "fallback" {
  count = length(data.aws_subnets.main.ids) == 0 ? 1 : 0

  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.main.id]
  }
}

locals {
  subnet_ids = length(data.aws_subnets.main.ids) > 0 ? data.aws_subnets.main.ids : data.aws_subnets.fallback[0].ids
}

# RDS Module
module "rds" {
  source = "../../modules/rds"

  project_name        = var.project_name
  vpc_id              = data.aws_vpc.main.id
  subnet_ids          = local.subnet_ids
  allowed_cidr_blocks = var.allowed_cidr_blocks

  # Database Configuration
  db_instance_identifier = "${var.project_name}-prod-db"
  db_name                = var.db_name
  db_username            = var.db_username
  db_password            = var.db_password # Must be provided for production

  # Instance Configuration (Production sizing)
  instance_class    = var.instance_class # e.g., db.t3.small, db.r6g.large
  allocated_storage = var.allocated_storage
  storage_type      = "gp3"
  storage_encrypted = true
  engine_version    = "16.3"

  # Network (Private for production)
  publicly_accessible = false

  # Backup (Full backups for production)
  backup_retention_period  = 30 # 30 days for production
  skip_final_snapshot      = false # Always create final snapshot
  delete_automated_backups = false

  # High Availability (Enabled for production)
  multi_az = true

  # Monitoring (Full monitoring for production)
  monitoring_interval             = 60
  performance_insights_enabled    = true
  performance_insights_retention_period = 7
  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  # Security
  deletion_protection           = true # Prevent accidental deletion
  create_secrets_manager_secret = true

  # Alarms (Enabled for production)
  create_cloudwatch_alarms     = true
  cpu_alarm_threshold          = 80
  free_storage_alarm_threshold = 5368709120 # 5GB
  connection_alarm_threshold   = 100

  # Upgrades
  auto_minor_version_upgrade  = true
  allow_major_version_upgrade = false

  tags = {
    Environment = "Production"
    CostCenter  = "Operations"
    Compliance  = "Required"
  }
}
