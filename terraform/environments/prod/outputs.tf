# Production Environment Outputs

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

output "jdbc_connection_string" {
  description = "JDBC connection string for Spring Boot"
  value       = module.rds.jdbc_connection_string
}

output "secrets_manager_secret_arn" {
  description = "ARN of the Secrets Manager secret containing DB credentials"
  value       = module.rds.secrets_manager_secret_arn
}

output "security_group_id" {
  description = "Security group ID for RDS"
  value       = module.rds.db_security_group_id
}

# Helm values to update
output "helm_values" {
  description = "Values to update in Helm chart for production"
  value = <<-EOT
    Update your production Helm values.yaml with:

    postgresql:
      enabled: false

    backend:
      env:
        DB_HOST: "${module.rds.db_instance_address}"
        DB_PORT: "${module.rds.db_instance_port}"
        DB_NAME: "${module.rds.db_instance_name}"

    IMPORTANT: For production, use Kubernetes External Secrets Operator
    to fetch credentials from AWS Secrets Manager:

    Secret ARN: ${module.rds.secrets_manager_secret_arn}
  EOT
}

# Instructions
output "next_steps" {
  description = "Next steps after RDS creation"
  value = <<-EOT
    ====================================
    Production RDS Created Successfully
    ====================================

    1. Update Security Group to allow EKS cluster access:
       aws ec2 authorize-security-group-ingress \
         --group-id ${module.rds.db_security_group_id} \
         --protocol tcp \
         --port 5432 \
         --source-group <EKS_NODE_SECURITY_GROUP_ID>

    2. Configure Kubernetes External Secrets:
       - Install External Secrets Operator
       - Create SecretStore pointing to AWS Secrets Manager
       - Create ExternalSecret to sync DB credentials

    3. Update Helm deployment:
       helm upgrade real-estate ./helm/real-estate-backend \
         --set postgresql.enabled=false \
         --set backend.env.DB_HOST=${module.rds.db_instance_address}

    4. Enable monitoring and alerts:
       - CloudWatch Alarms are already configured
       - Set up SNS topic for alarm notifications
       - Configure PagerDuty/Slack integration

    5. Configure automated backups verification
    6. Set up database migration CI/CD pipeline
    7. Test disaster recovery procedures
  EOT
}
