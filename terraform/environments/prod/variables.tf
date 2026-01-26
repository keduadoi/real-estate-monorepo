# Production Environment Variables

variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "real-estate"
}

variable "vpc_id" {
  description = "VPC ID (leave empty to use default VPC)"
  type        = string
  default     = ""
}

variable "db_name" {
  description = "Database name"
  type        = string
  default     = "realestatedb"
}

variable "db_username" {
  description = "Database master username"
  type        = string
  default     = "postgres"
}

variable "db_password" {
  description = "Database master password (REQUIRED for production)"
  type        = string
  sensitive   = true

  validation {
    condition     = var.db_password != null && length(var.db_password) >= 16
    error_message = "Database password must be at least 16 characters for production."
  }
}

variable "instance_class" {
  description = "RDS instance class"
  type        = string
  default     = "db.t3.small" # Minimum recommended for production

  validation {
    condition     = can(regex("^db\\.(t3|r6g|m6g)\\.(small|medium|large|xlarge)", var.instance_class))
    error_message = "For production, use at least db.t3.small or db.r6g.large for better performance."
  }
}

variable "allocated_storage" {
  description = "Allocated storage in GB"
  type        = number
  default     = 100 # More storage for production
}

variable "allowed_cidr_blocks" {
  description = "CIDR blocks allowed to connect to RDS (EKS cluster CIDR)"
  type        = list(string)

  validation {
    condition     = !contains(var.allowed_cidr_blocks, "0.0.0.0/0")
    error_message = "Production RDS must not be open to the internet. Specify EKS cluster CIDR blocks."
  }
}
