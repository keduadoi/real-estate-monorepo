# Development Environment RDS Configuration

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

  # Uncomment and configure for remote state
  # backend "s3" {
  #   bucket         = "your-terraform-state-bucket"
  #   key            = "real-estate/dev/rds/terraform.tfstate"
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
      Environment = "Development"
      ManagedBy   = "Terraform"
    }
  }
}

# Get default VPC (for quick start)
data "aws_vpc" "default" {
  default = true
}

# Get default subnets
data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# RDS Module
module "rds" {
  source = "../../modules/rds"

  project_name           = var.project_name
  vpc_id                 = data.aws_vpc.default.id
  subnet_ids             = data.aws_subnets.default.ids
  allowed_cidr_blocks    = var.allowed_cidr_blocks

  # Database Configuration
  db_instance_identifier = "${var.project_name}-dev-db"
  db_name                = var.db_name
  db_username            = var.db_username
  db_password            = var.db_password # Or leave null for auto-generated

  # Instance Configuration (Small for dev)
  instance_class    = "db.t4g.micro"  # ~$15/month
  allocated_storage = 20
  storage_type      = "gp3"
  storage_encrypted = true

  # Network (Public for dev - easier testing)
  publicly_accessible = true

  # Backup (Minimal for dev)
  backup_retention_period = 1
  skip_final_snapshot     = true # No snapshot on delete for dev
  delete_automated_backups = true

  # High Availability (Disabled for dev - cost savings)
  multi_az = false

  # Monitoring (Basic for dev)
  monitoring_interval              = 60
  performance_insights_enabled     = false # Disable to save costs
  enabled_cloudwatch_logs_exports  = ["postgresql"]

  # Security
  deletion_protection           = false # Allow easy deletion in dev
  create_secrets_manager_secret = true

  # Alarms (Disabled for dev)
  create_cloudwatch_alarms = false

  tags = {
    Environment = "Development"
    CostCenter  = "Engineering"
  }
}
