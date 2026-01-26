# Development Environment Outputs

output "rds_endpoint" {
  description = "RDS instance endpoint"
  value       = module.rds.db_instance_endpoint
}

output "rds_address" {
  description = "RDS instance address (hostname)"
  value       = module.rds.db_instance_address
}

output "rds_port" {
  description = "RDS instance port"
  value       = module.rds.db_instance_port
}

output "database_name" {
  description = "Database name"
  value       = module.rds.db_instance_name
}

output "database_username" {
  description = "Database username"
  value       = module.rds.db_instance_username
  sensitive   = true
}

output "database_password" {
  description = "Database password"
  value       = module.rds.db_instance_password
  sensitive   = true
}

output "jdbc_connection_string" {
  description = "JDBC connection string for Spring Boot"
  value       = module.rds.jdbc_connection_string
}

output "connection_string" {
  description = "Full PostgreSQL connection string"
  value       = module.rds.connection_string
  sensitive   = true
}

output "secrets_manager_secret_arn" {
  description = "ARN of the Secrets Manager secret containing DB credentials"
  value       = module.rds.secrets_manager_secret_arn
}

output "security_group_id" {
  description = "Security group ID for RDS"
  value       = module.rds.db_security_group_id
}

# Connection command
output "psql_command" {
  description = "Command to connect via psql"
  value       = "psql -h ${module.rds.db_instance_address} -p ${module.rds.db_instance_port} -U ${module.rds.db_instance_username} -d ${module.rds.db_instance_name}"
}

# Helm values to update
output "helm_values" {
  description = "Values to update in Helm chart"
  value = <<-EOT
    Update your Helm values.yaml with:

    postgresql:
      enabled: false

    backend:
      env:
        DB_HOST: "${module.rds.db_instance_address}"
        DB_PORT: "${module.rds.db_instance_port}"
        DB_NAME: "${module.rds.db_instance_name}"
        DB_USER: "${module.rds.db_instance_username}"
        # DB_PASSWORD: Store in Kubernetes Secret or use Secrets Manager
  EOT
}
