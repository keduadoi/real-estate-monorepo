# Quick Start Guide - Terraform RDS Setup

## ⚡ 5-Minute Setup (Development)

### 1. Prerequisites
```bash
# Install Terraform (if not installed)
brew install terraform

# Configure AWS credentials
aws configure
# Enter: Access Key ID, Secret Access Key, Region (us-east-1), Format (json)
```

### 2. Create Configuration
```bash
cd terraform/environments/dev

# Copy example config
cp terraform.tfvars.example terraform.tfvars

# Edit if needed (optional - defaults work fine)
nano terraform.tfvars
```

### 3. Deploy RDS
```bash
# Initialize Terraform
terraform init

# Preview changes
terraform plan

# Create RDS (~5-10 minutes)
terraform apply
# Type: yes
```

### 4. Get Connection Details
```bash
# View all outputs
terraform output

# Get connection string
terraform output jdbc_connection_string

# Get password (if auto-generated)
terraform output -raw database_password
```

---

## 📋 Command Cheat Sheet

### Initial Setup
```bash
terraform init              # Initialize Terraform
terraform validate          # Validate configuration
terraform fmt              # Format code
```

### Deployment
```bash
terraform plan             # Preview changes
terraform apply            # Apply changes (create/update)
terraform apply -auto-approve  # Apply without confirmation
```

### Viewing State
```bash
terraform show             # Show current state
terraform output           # Show all outputs
terraform output rds_address   # Show specific output
terraform output -raw database_password  # Show sensitive output
terraform state list       # List all resources
```

### Updates
```bash
# After modifying terraform.tfvars or *.tf files
terraform plan             # Check what will change
terraform apply            # Apply changes
```

### Cleanup
```bash
terraform destroy          # Delete all resources
terraform destroy -auto-approve  # Delete without confirmation
```

---

## 🎯 Common Scenarios

### Change Instance Size
```bash
# Edit terraform.tfvars
instance_class = "db.t3.small"

# Apply change
terraform apply
```

### Increase Storage
```bash
# Edit terraform.tfvars
allocated_storage = 50

# Apply change (no downtime!)
terraform apply
```

### Enable Multi-AZ (Production)
```bash
# In environments/prod/main.tf or terraform.tfvars
multi_az = true

# Apply change
terraform apply
```

### Get Password After Creation
```bash
# Show password
terraform output -raw database_password

# Or get from Secrets Manager
aws secretsmanager get-secret-value \
  --secret-id $(terraform output -raw secrets_manager_secret_arn) \
  --query SecretString --output text | jq -r .password
```

---

## 🔗 Connect to RDS

### Via psql (if publicly accessible)
```bash
# Install psql (if needed)
brew install postgresql

# Connect
psql -h $(terraform output -raw rds_address) \
     -p 5432 \
     -U postgres \
     -d realestatedb
# Password: [from terraform output]
```

### Via Spring Boot
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://YOUR_RDS_ADDRESS:5432/realestatedb
    username: postgres
    password: YOUR_PASSWORD
```

### Via Environment Variables
```bash
export DB_HOST=$(terraform output -raw rds_address)
export DB_PORT=5432
export DB_NAME=realestatedb
export DB_USER=postgres
export DB_PASSWORD=$(terraform output -raw database_password)
```

---

## 🚀 Production Deployment

```bash
cd terraform/environments/prod

# Create config with STRONG password
cp terraform.tfvars.example terraform.tfvars
nano terraform.tfvars
# Set: db_password (16+ chars)
# Set: allowed_cidr_blocks (EKS cluster CIDR, NOT 0.0.0.0/0)

# Deploy
terraform init
terraform plan
terraform apply

# Get connection details (save securely!)
terraform output > rds-connection-info.txt
```

---

## ⚠️ Important Notes

### Cost
- **Dev (db.t4g.micro)**: ~$15-16/month
- **Prod (db.t3.small + Multi-AZ)**: ~$70-90/month

### Security
- ✅ **Dev**: Publicly accessible OK, auto-generated password
- ❌ **Prod**: Must be private, custom strong password required

### Backups
- **Dev**: 1 day retention, skip final snapshot
- **Prod**: 30 days retention, final snapshot created

### Deletion
```bash
# Dev: Quick deletion
terraform destroy

# Prod: Creates final snapshot before deletion
terraform destroy
```

---

## 🐛 Troubleshooting

### Can't connect to RDS
```bash
# Check if publicly accessible
terraform output | grep publicly

# Check security group
terraform output security_group_id

# Test connectivity
telnet $(terraform output -raw rds_address) 5432
```

### Password not showing
```bash
# Use -raw flag
terraform output -raw database_password

# Check Secrets Manager
terraform output secrets_manager_secret_arn
```

### Terraform errors
```bash
# Reinitialize
terraform init -upgrade

# Validate config
terraform validate

# Check AWS credentials
aws sts get-caller-identity
```

---

## 📚 More Information

See [README.md](./README.md) for:
- Detailed configuration options
- Advanced topics (remote state, multi-environment)
- Security best practices
- Cost optimization tips
- Comprehensive troubleshooting guide
