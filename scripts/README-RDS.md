# AWS RDS Setup Guide

This guide helps you create an RDS PostgreSQL instance for your Real Estate backend.

## Prerequisites

1. **AWS Account** with appropriate permissions
2. **AWS CLI installed** (already done ✅)
3. **AWS Credentials configured**

## Step 1: Configure AWS Credentials

```bash
aws configure
```

Enter:
- **AWS Access Key ID**: Your IAM user access key
- **AWS Secret Access Key**: Your IAM user secret key
- **Default region**: `us-east-1` (or your preferred region)
- **Default output format**: `json`

**How to get credentials:**
1. Go to [AWS Console](https://console.aws.amazon.com)
2. Navigate to: IAM → Users → [Your User] → Security Credentials
3. Click "Create access key" → Choose "Command Line Interface (CLI)"
4. Copy the Access Key ID and Secret Access Key

## Step 2: Edit Configuration (IMPORTANT!)

Edit the script before running:

```bash
nano scripts/create-rds.sh
```

**Change these values:**
```bash
REGION="us-east-1"                          # Your AWS region
DB_PASSWORD="YourSecurePassword123!"        # ⚠️ CHANGE THIS!
DB_INSTANCE_CLASS="db.t4g.micro"            # Size (see options below)
```

**Instance Class Options:**
- `db.t4g.micro` - 1 vCPU, 1GB RAM (~$15-20/month) - Good for development
- `db.t4g.small` - 2 vCPU, 2GB RAM (~$30-35/month) - Good for staging
- `db.t3.micro` - 2 vCPU, 1GB RAM (~$15-17/month) - Free tier eligible
- `db.t3.small` - 2 vCPU, 2GB RAM (~$30-36/month) - Production ready

## Step 3: Run the Script

```bash
cd /Users/ducnt/Working/real-estate-ui
./scripts/create-rds.sh
```

The script will:
1. ✅ Verify AWS credentials
2. ✅ Find/create VPC and subnets
3. ✅ Create security group for RDS
4. ✅ Create DB subnet group
5. ✅ Create RDS PostgreSQL instance
6. ✅ Wait for instance to be available (5-10 minutes)
7. ✅ Display connection details

## Step 4: Get Connection Details

After completion, check:
```bash
cat rds-connection-info.txt
```

You'll see:
```
Endpoint: your-instance.xxxxx.us-east-1.rds.amazonaws.com
Port: 5432
Database: realestatedb
Username: postgres
Password: [your password]
```

## Step 5: Update Your Configuration

### For EKS Deployment:

Update `backend/helm/real-estate-backend/values.yaml`:

```yaml
postgresql:
  enabled: false  # Don't deploy postgres in K8s

backend:
  env:
    DB_HOST: "your-instance.xxxxx.us-east-1.rds.amazonaws.com"
    DB_PORT: "5432"
```

### For Local Testing:

Update `backend/.env` or `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://your-instance.xxxxx.us-east-1.rds.amazonaws.com:5432/realestatedb
    username: postgres
    password: YourSecurePassword123!
```

---

## Alternative: Manual AWS CLI Commands

If you prefer to run commands manually instead of using the script:

### 1. Create Security Group:
```bash
# Get default VPC
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=is-default,Values=true" --query 'Vpcs[0].VpcId' --output text)

# Create security group
SG_ID=$(aws ec2 create-security-group \
  --group-name real-estate-rds-sg \
  --description "RDS PostgreSQL Security Group" \
  --vpc-id $VPC_ID \
  --query 'GroupId' \
  --output text)

# Allow PostgreSQL port
aws ec2 authorize-security-group-ingress \
  --group-id $SG_ID \
  --protocol tcp \
  --port 5432 \
  --cidr 0.0.0.0/0
```

### 2. Create DB Subnet Group:
```bash
# Get subnets
SUBNETS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[*].SubnetId' --output text)

# Create subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name real-estate-subnet-group \
  --db-subnet-group-description "Real Estate DB Subnet Group" \
  --subnet-ids $SUBNETS
```

### 3. Create RDS Instance:
```bash
aws rds create-db-instance \
  --db-instance-identifier real-estate-db \
  --db-instance-class db.t4g.micro \
  --engine postgres \
  --engine-version 16.3 \
  --master-username postgres \
  --master-user-password YourSecurePassword123! \
  --allocated-storage 20 \
  --storage-type gp3 \
  --db-name realestatedb \
  --vpc-security-group-ids $SG_ID \
  --db-subnet-group-name real-estate-subnet-group \
  --publicly-accessible \
  --backup-retention-period 7 \
  --storage-encrypted
```

### 4. Wait for Availability:
```bash
aws rds wait db-instance-available --db-instance-identifier real-estate-db
```

### 5. Get Endpoint:
```bash
aws rds describe-db-instances \
  --db-instance-identifier real-estate-db \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

---

## Cost Estimation

**db.t4g.micro (Recommended for dev/staging):**
- Instance: $12.41/month
- Storage (20GB gp3): $2.76/month
- Backup storage (7 days): ~$0.50/month
- **Total: ~$15-16/month**

**db.t3.micro (Free tier eligible for 12 months):**
- First year: FREE (750 hours/month)
- After free tier: ~$15-17/month

**Production (db.t3.small + Multi-AZ):**
- Instance (Multi-AZ): $60-70/month
- Storage: $5-10/month
- **Total: ~$70-80/month**

---

## Testing Connection

Once RDS is created, test the connection:

```bash
# Install psql if needed (macOS)
brew install postgresql

# Connect to RDS
psql -h your-instance.xxxxx.rds.amazonaws.com \
     -p 5432 \
     -U postgres \
     -d realestatedb
```

---

## Cleanup (Delete RDS)

To delete the RDS instance and avoid charges:

```bash
# Delete RDS instance (skip final snapshot for dev)
aws rds delete-db-instance \
  --db-instance-identifier real-estate-db \
  --skip-final-snapshot

# Delete subnet group (after instance is deleted)
aws rds delete-db-subnet-group \
  --db-subnet-group-name real-estate-subnet-group

# Delete security group
aws ec2 delete-security-group --group-id $SG_ID
```

---

## Security Best Practices

### For Development:
- ✅ Publicly accessible is OK for testing
- ✅ Password: Use strong password (16+ chars)
- ⚠️ Security group: 0.0.0.0/0 is acceptable temporarily

### For Production:
- ❌ **NOT publicly accessible** - use VPC peering or VPN
- ✅ **Restrict security group** to EKS cluster CIDR only
- ✅ **Enable Multi-AZ** for high availability
- ✅ **Use secrets manager** for credentials
- ✅ **Enable encryption** at rest and in transit
- ✅ **Enable deletion protection**
- ✅ **Monitor with CloudWatch**

---

## Troubleshooting

### "InvalidClientTokenId" error:
```bash
# Check if credentials are configured
aws sts get-caller-identity

# If not, run:
aws configure
```

### "No default VPC found":
```bash
# Create a VPC first or specify an existing VPC in the script
```

### "At least 2 subnets required":
```bash
# RDS needs subnets in multiple availability zones
# Check your VPC has subnets in at least 2 AZs
aws ec2 describe-subnets --filters "Name=vpc-id,Values=YOUR_VPC_ID"
```

### Can't connect to RDS:
1. Check security group allows your IP
2. Verify RDS is publicly accessible (for testing)
3. Check VPC route tables and network ACLs
4. Verify credentials are correct

---

## Questions?

- Script issues: Check script output and error messages
- AWS permissions: Ensure IAM user has RDS and EC2 permissions
- Cost concerns: Use `db.t3.micro` for free tier or `db.t4g.micro` for ARM-based cost savings
