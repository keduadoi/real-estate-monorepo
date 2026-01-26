# RDS Module Outputs

output "db_instance_id" {
  description = "ID of the RDS instance"
  value       = aws_db_instance.main.id
}

output "db_instance_arn" {
  description = "ARN of the RDS instance"
  value       = aws_db_instance.main.arn
}

output "db_instance_endpoint" {
  description = "Connection endpoint for the RDS instance"
  value       = aws_db_instance.main.endpoint
}

output "db_instance_address" {
  description = "Address of the RDS instance"
  value       = aws_db_instance.main.address
}

output "db_instance_port" {
  description = "Port of the RDS instance"
  value       = aws_db_instance.main.port
}

output "db_instance_name" {
  description = "Name of the database"
  value       = aws_db_instance.main.db_name
}

output "db_instance_username" {
  description = "Master username"
  value       = aws_db_instance.main.username
  sensitive   = true
}

output "db_instance_password" {
  description = "Master password (if generated)"
  value       = var.db_password != null ? var.db_password : try(random_password.db_password[0].result, null)
  sensitive   = true
}

output "db_security_group_id" {
  description = "ID of the RDS security group"
  value       = aws_security_group.rds.id
}

output "db_subnet_group_name" {
  description = "Name of the DB subnet group"
  value       = aws_db_subnet_group.main.name
}

output "db_subnet_group_arn" {
  description = "ARN of the DB subnet group"
  value       = aws_db_subnet_group.main.arn
}

output "secrets_manager_secret_id" {
  description = "ID of the Secrets Manager secret (if created)"
  value       = try(aws_secretsmanager_secret.db_password[0].id, null)
}

output "secrets_manager_secret_arn" {
  description = "ARN of the Secrets Manager secret (if created)"
  value       = try(aws_secretsmanager_secret.db_password[0].arn, null)
}

output "monitoring_role_arn" {
  description = "ARN of the enhanced monitoring IAM role"
  value       = try(aws_iam_role.rds_monitoring[0].arn, null)
}

output "connection_string" {
  description = "PostgreSQL connection string"
  value       = "postgresql://${aws_db_instance.main.username}:${var.db_password != null ? var.db_password : try(random_password.db_password[0].result, "PASSWORD")}@${aws_db_instance.main.address}:${aws_db_instance.main.port}/${aws_db_instance.main.db_name}"
  sensitive   = true
}

output "jdbc_connection_string" {
  description = "JDBC connection string for Spring Boot"
  value       = "jdbc:postgresql://${aws_db_instance.main.address}:${aws_db_instance.main.port}/${aws_db_instance.main.db_name}"
}
