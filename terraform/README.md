# Terraform Infrastructure for Real Estate Application

This directory contains Terraform configurations to provision AWS infrastructure for the Real Estate backend application.

## 📁 Directory Structure

```
terraform/
├── modules/
│   ├── rds/              # RDS PostgreSQL module
│   ├── eks/              # EKS cluster module (future)
│   └── networking/       # VPC and networking (future)
├── environments/
│   ├── dev/              # Development environment
│   ├── staging/          # Staging environment (future)
│   └── prod/             # Production environment
└── README.md
```

## 🚀 Quick Start - Development Environment

### Prerequisites

1. **Install Terraform** (v1.0+)
   ```bash
   # macOS
   brew install terraform

   # Verify installation
   terraform version
   ```

2. **Configure AWS Credentials**
   ```bash
   aws configure
   ```

   Enter your AWS credentials:
   - AWS Access Key ID
   - AWS Secret Access Key
   - Default region (e.g., `us-east-1`)
   - Output format: `json`

### Step 1: Navigate to Dev Environment

```bash
cd terraform/environments/dev
```

### Step 2: Create terraform.tfvars

```bash
cp terraform.tfvars.example terraform.tfvars
nano terraform.tfvars
```

**Minimal configuration:**
```hcl
aws_region   = "us-east-1"
project_name = "real-estate"
db_name      = "realestatedb"
db_username  = "postgres"
# Leave db_password commented for auto-generated password
```

### Step 3: Initialize Terraform

```bash
terraform init
```

This will:
- Download required providers (AWS, Random)
- Initialize the backend
- Prepare modules

### Step 4: Plan Infrastructure

```bash
terraform plan
```

Review the planned changes. You should see:
- 1 VPC security group
- 1 DB subnet group
- 1 RDS instance
- 1 Secrets Manager secret (optional)
- 2-3 IAM roles (for monitoring)
- 3 CloudWatch alarms (optional)

### Step 5: Apply Configuration

```bash
terraform apply
```

Type `yes` when prompted. This will create:
- RDS PostgreSQL instance (~5-10 minutes)
- Associated security groups and networking
- Secrets Manager secret with credentials

### Step 6: Get Connection Details

```bash
# View all outputs
terraform output

# Get specific values
terraform output rds_address
terraform output jdbc_connection_string

# Get sensitive values (password)
terraform output -raw database_password
```

**Save these outputs!** You'll need them to configure your backend.

---

## 🔧 Configuration Options

### Development Environment

**File:** `environments/dev/main.tf`

**Default configuration:**
- Instance: `db.t4g.micro` (~$15/month)
- Storage: 20GB gp3
- Multi-AZ: Disabled (cost savings)
- Publicly accessible: Yes (easier testing)
- Backups: 1 day retention
- Deletion protection: Disabled
- CloudWatch alarms: Disabled

**Good for:**
- Local development
- Testing
- Quick iterations
- Cost-conscious scenarios

### Production Environment

**File:** `environments/prod/main.tf`

**Default configuration:**
- Instance: `db.t3.small` (~$70/month with Multi-AZ)
- Storage: 100GB gp3
- Multi-AZ: Enabled (high availability)
- Publicly accessible: No (private subnet)
- Backups: 30 days retention
- Deletion protection: Enabled
- CloudWatch alarms: Enabled

**Good for:**
- Production workloads
- High availability requirements
- Compliance needs
- Mission-critical applications

---

## 📊 Cost Estimates

### Development
| Resource | Configuration | Monthly Cost |
|----------|--------------|--------------|
| RDS Instance | db.t4g.micro | $12.41 |
| Storage | 20GB gp3 | $2.76 |
| Backups | 1 day | ~$0.50 |
| **Total** | | **~$15-16** |

### Production
| Resource | Configuration | Monthly Cost |
|----------|--------------|--------------|
| RDS Instance | db.t3.small (Multi-AZ) | $60-70 |
| Storage | 100GB gp3 | $13.80 |
| Backups | 30 days | ~$5 |
| Performance Insights | 7 days | ~$1 |
| Enhanced Monitoring | 60s interval | ~$1 |
| **Total** | | **~$80-90** |

---

## 🔐 Security Best Practices

### Development
- ✅ Auto-generated passwords stored in Secrets Manager
- ✅ Encryption at rest enabled
- ⚠️ Publicly accessible (acceptable for dev)
- ⚠️ Security group open to 0.0.0.0/0 (acceptable for dev)

### Production
- ✅ Strong password (16+ characters, set in tfvars)
- ✅ Multi-AZ for high availability
- ✅ Private subnet (NOT publicly accessible)
- ✅ Security group restricted to EKS cluster CIDR
- ✅ Deletion protection enabled
- ✅ 30-day backup retention
- ✅ Encryption at rest and in transit
- ✅ CloudWatch alarms for monitoring

---

## 🔄 Common Workflows

### Update Configuration

```bash
# Modify terraform.tfvars or main.tf
nano terraform.tfvars

# Plan changes
terraform plan

# Apply changes
terraform apply
```

### Scale Instance

```bash
# Edit terraform.tfvars
instance_class = "db.t3.medium"

# Apply change (minimal downtime)
terraform apply
```

### Increase Storage

```bash
# Edit terraform.tfvars
allocated_storage = 50

# Apply change (no downtime)
terraform apply
```

### View Current State

```bash
# List all resources
terraform state list

# Show specific resource
terraform state show module.rds.aws_db_instance.main
```

### Destroy Infrastructure

```bash
# ⚠️ WARNING: This deletes everything!
terraform destroy

# For dev (skip final snapshot)
# Already configured in dev environment

# For prod (creates final snapshot)
# Configured to create final snapshot automatically
```

---

## 🔗 Integrating with Backend

### Option 1: Environment Variables

```bash
# Get values from Terraform
export DB_HOST=$(terraform output -raw rds_address)
export DB_PORT=$(terraform output -raw rds_port)
export DB_NAME=$(terraform output -raw database_name)
export DB_USER=$(terraform output -raw database_username)
export DB_PASSWORD=$(terraform output -raw database_password)
```

### Option 2: Update application.yml

```yaml
spring:
  datasource:
    url: jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/realestatedb
    username: postgres
    password: YOUR_PASSWORD
```

### Option 3: Update Helm values.yaml

```yaml
postgresql:
  enabled: false  # Don't deploy postgres in K8s

backend:
  env:
    DB_HOST: "your-rds-instance.xxxxx.rds.amazonaws.com"
    DB_PORT: "5432"
    DB_NAME: "realestatedb"
    DB_USER: "postgres"
    # Store password in Kubernetes Secret
```

### Option 4: Use AWS Secrets Manager (Recommended for Production)

```bash
# Get secret ARN
terraform output secrets_manager_secret_arn

# Use External Secrets Operator in Kubernetes
# to sync credentials from Secrets Manager
```

---

## 🧪 Testing Connection

### From Local Machine (if publicly accessible)

```bash
# Get connection details
terraform output psql_command

# Connect using psql
psql -h $(terraform output -raw rds_address) \
     -p $(terraform output -raw rds_port) \
     -U $(terraform output -raw database_username) \
     -d $(terraform output -raw database_name)
```

### From EKS Cluster

```bash
# Deploy a postgres client pod
kubectl run postgres-client --rm -it --restart=Never \
  --image=postgres:16-alpine \
  --env="PGPASSWORD=$(terraform output -raw database_password)" \
  -- psql -h $(terraform output -raw rds_address) \
         -U $(terraform output -raw database_username) \
         -d $(terraform output -raw database_name)
```

---

## 🔧 Troubleshooting

### Issue: "No default VPC found"

**Solution:**
```bash
# Create a VPC first
aws ec2 create-default-vpc

# Or specify existing VPC in terraform.tfvars
vpc_id = "vpc-xxxxx"
```

### Issue: "At least 2 subnets in different AZs required"

**Solution:**
RDS requires subnets in at least 2 availability zones.

```bash
# Check available subnets
aws ec2 describe-subnets \
  --filters "Name=vpc-id,Values=YOUR_VPC_ID" \
  --query 'Subnets[*].[SubnetId,AvailabilityZone]'

# Ensure you have subnets in at least 2 different AZs
```

### Issue: "InvalidClientTokenId"

**Solution:**
```bash
# Reconfigure AWS credentials
aws configure

# Verify credentials
aws sts get-caller-identity
```

### Issue: Can't connect to RDS

**Solutions:**
1. Check security group allows your IP
   ```bash
   terraform output security_group_id
   aws ec2 describe-security-groups --group-ids sg-xxxxx
   ```

2. Verify RDS is accessible
   ```bash
   terraform output rds_address
   telnet YOUR_RDS_ENDPOINT 5432
   ```

3. Check publicly_accessible setting
   ```bash
   aws rds describe-db-instances \
     --db-instance-identifier $(terraform output -raw db_instance_id) \
     --query 'DBInstances[0].PubliclyAccessible'
   ```

### Issue: Password not showing

**Solution:**
```bash
# Use -raw flag for sensitive outputs
terraform output -raw database_password

# Or check Secrets Manager
aws secretsmanager get-secret-value \
  --secret-id $(terraform output -raw secrets_manager_secret_arn) \
  --query 'SecretString' --output text | jq .
```

---

## 📚 Advanced Topics

### Remote State Configuration

For team collaboration, configure S3 backend:

**1. Create S3 bucket and DynamoDB table:**
```bash
# Create S3 bucket
aws s3 mb s3://your-terraform-state-bucket --region us-east-1

# Enable versioning
aws s3api put-bucket-versioning \
  --bucket your-terraform-state-bucket \
  --versioning-configuration Status=Enabled

# Create DynamoDB table for state locking
aws dynamodb create-table \
  --table-name terraform-state-lock \
  --attribute-definitions AttributeName=LockID,AttributeType=S \
  --key-schema AttributeName=LockID,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

**2. Update main.tf backend configuration:**
```hcl
terraform {
  backend "s3" {
    bucket         = "your-terraform-state-bucket"
    key            = "real-estate/dev/rds/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "terraform-state-lock"
  }
}
```

**3. Reinitialize:**
```bash
terraform init -migrate-state
```

### Multi-Environment Deployment

Deploy to multiple environments:

```bash
# Development
cd environments/dev
terraform apply

# Staging (create staging directory first)
cd ../staging
terraform apply

# Production
cd ../prod
terraform apply
```

### Importing Existing RDS

If you already have an RDS instance:

```bash
# Import existing RDS
terraform import module.rds.aws_db_instance.main your-db-identifier

# Import security group
terraform import module.rds.aws_security_group.rds sg-xxxxx
```

---

## 🚨 Important Notes

### Before Destroying

- ⚠️ **Backup your data** - Create manual snapshot
- ⚠️ **Export Secrets** - Save credentials from Secrets Manager
- ⚠️ **Check final snapshot** - Verify `skip_final_snapshot` setting

### Cost Management

- 💰 Use `db.t3.micro` for free tier (first 12 months)
- 💰 Disable Multi-AZ in dev/staging
- 💰 Reduce backup retention period
- 💰 Disable Performance Insights in non-prod
- 💰 Delete old snapshots regularly

### Security Checklist

- [ ] Strong password (16+ characters)
- [ ] Encryption at rest enabled
- [ ] Security group restricted (no 0.0.0.0/0 in prod)
- [ ] Not publicly accessible (prod)
- [ ] Deletion protection enabled (prod)
- [ ] Automated backups configured
- [ ] CloudWatch alarms set up (prod)
- [ ] Credentials in Secrets Manager
- [ ] VPC in private subnets (prod)

---

## 📞 Support

For issues or questions:
1. Check Troubleshooting section above
2. Review AWS RDS documentation
3. Check Terraform AWS provider docs
4. Review CloudWatch logs for RDS

---

## 🔄 Next Steps

After RDS is created:

1. **Test Connection** - Verify you can connect to RDS
2. **Update Backend** - Configure Spring Boot to use RDS
3. **Deploy to EKS** - Update Helm charts with RDS endpoint
4. **Configure Monitoring** - Set up CloudWatch dashboards
5. **Test Backups** - Verify automated backups work
6. **Document** - Save connection details securely
7. **Monitor Costs** - Enable AWS Cost Explorer alerts
