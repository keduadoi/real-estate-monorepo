#!/bin/bash

# RDS Creation Script for Real Estate Backend
# This script creates a PostgreSQL RDS instance in AWS

set -e

# Configuration Variables (CHANGE THESE)
REGION="us-east-1"                          # AWS region
DB_INSTANCE_ID="real-estate-db"             # RDS instance identifier
DB_NAME="realestatedb"                      # Database name
DB_USERNAME="postgres"                      # Master username
DB_PASSWORD="YourSecurePassword123!"        # CHANGE THIS! Must be 8+ chars
DB_INSTANCE_CLASS="db.t4g.micro"            # Instance size (Free tier eligible: db.t3.micro)
ALLOCATED_STORAGE=20                        # Storage in GB (Free tier: 20GB)
ENGINE_VERSION="16.3"                       # PostgreSQL version
VPC_SECURITY_GROUP=""                       # Leave empty to create new one
PUBLICLY_ACCESSIBLE=true                    # Set to false for production

echo "=========================================="
echo "Creating RDS PostgreSQL Instance"
echo "=========================================="
echo "Instance ID: $DB_INSTANCE_ID"
echo "Database: $DB_NAME"
echo "Region: $REGION"
echo "Instance Class: $DB_INSTANCE_CLASS"
echo "=========================================="
echo ""

# Check AWS CLI is configured
echo "Checking AWS credentials..."
if ! aws sts get-caller-identity --region $REGION > /dev/null 2>&1; then
    echo "❌ Error: AWS credentials not configured or invalid"
    echo "Run: aws configure"
    exit 1
fi
echo "✅ AWS credentials verified"
echo ""

# Get default VPC
echo "Getting default VPC..."
DEFAULT_VPC=$(aws ec2 describe-vpcs --filters "Name=is-default,Values=true" --region $REGION --query 'Vpcs[0].VpcId' --output text)
if [ "$DEFAULT_VPC" == "None" ] || [ -z "$DEFAULT_VPC" ]; then
    echo "❌ Error: No default VPC found. Please create a VPC first."
    exit 1
fi
echo "✅ Using VPC: $DEFAULT_VPC"
echo ""

# Create security group if not provided
if [ -z "$VPC_SECURITY_GROUP" ]; then
    echo "Creating security group..."
    SG_NAME="real-estate-rds-sg"

    # Check if security group already exists
    EXISTING_SG=$(aws ec2 describe-security-groups \
        --filters "Name=group-name,Values=$SG_NAME" "Name=vpc-id,Values=$DEFAULT_VPC" \
        --region $REGION \
        --query 'SecurityGroups[0].GroupId' \
        --output text 2>/dev/null || echo "None")

    if [ "$EXISTING_SG" != "None" ] && [ -n "$EXISTING_SG" ]; then
        VPC_SECURITY_GROUP=$EXISTING_SG
        echo "✅ Using existing security group: $VPC_SECURITY_GROUP"
    else
        VPC_SECURITY_GROUP=$(aws ec2 create-security-group \
            --group-name $SG_NAME \
            --description "Security group for Real Estate RDS PostgreSQL" \
            --vpc-id $DEFAULT_VPC \
            --region $REGION \
            --query 'GroupId' \
            --output text)

        echo "✅ Created security group: $VPC_SECURITY_GROUP"

        # Add rule to allow PostgreSQL access from anywhere (change for production!)
        echo "Adding inbound rule for PostgreSQL (port 5432)..."
        aws ec2 authorize-security-group-ingress \
            --group-id $VPC_SECURITY_GROUP \
            --protocol tcp \
            --port 5432 \
            --cidr 0.0.0.0/0 \
            --region $REGION > /dev/null

        echo "✅ Security group rule added (port 5432 open to 0.0.0.0/0)"
        echo "⚠️  WARNING: For production, restrict this to your EKS cluster CIDR!"
    fi
    echo ""
fi

# Create DB subnet group (uses default VPC subnets)
echo "Creating DB subnet group..."
SUBNET_GROUP_NAME="real-estate-db-subnet-group"

# Get all subnets in the VPC
SUBNETS=$(aws ec2 describe-subnets \
    --filters "Name=vpc-id,Values=$DEFAULT_VPC" \
    --region $REGION \
    --query 'Subnets[*].SubnetId' \
    --output text)

if [ -z "$SUBNETS" ]; then
    echo "❌ Error: No subnets found in VPC $DEFAULT_VPC"
    exit 1
fi

# Convert space-separated to array
SUBNET_ARRAY=($SUBNETS)

# Check if at least 2 subnets (RDS requires multiple AZs)
if [ ${#SUBNET_ARRAY[@]} -lt 2 ]; then
    echo "❌ Error: RDS requires at least 2 subnets in different availability zones"
    echo "Found subnets: ${SUBNET_ARRAY[@]}"
    exit 1
fi

# Check if subnet group already exists
if aws rds describe-db-subnet-groups --db-subnet-group-name $SUBNET_GROUP_NAME --region $REGION > /dev/null 2>&1; then
    echo "✅ Using existing subnet group: $SUBNET_GROUP_NAME"
else
    aws rds create-db-subnet-group \
        --db-subnet-group-name $SUBNET_GROUP_NAME \
        --db-subnet-group-description "Subnet group for Real Estate RDS" \
        --subnet-ids ${SUBNET_ARRAY[@]} \
        --region $REGION > /dev/null

    echo "✅ Created DB subnet group: $SUBNET_GROUP_NAME"
fi
echo ""

# Create RDS instance
echo "Creating RDS PostgreSQL instance..."
echo "This may take 5-10 minutes..."
echo ""

aws rds create-db-instance \
    --db-instance-identifier $DB_INSTANCE_ID \
    --db-instance-class $DB_INSTANCE_CLASS \
    --engine postgres \
    --engine-version $ENGINE_VERSION \
    --master-username $DB_USERNAME \
    --master-user-password $DB_PASSWORD \
    --allocated-storage $ALLOCATED_STORAGE \
    --storage-type gp3 \
    --db-name $DB_NAME \
    --vpc-security-group-ids $VPC_SECURITY_GROUP \
    --db-subnet-group-name $SUBNET_GROUP_NAME \
    --publicly-accessible \
    --backup-retention-period 7 \
    --preferred-backup-window "03:00-04:00" \
    --preferred-maintenance-window "mon:04:00-mon:05:00" \
    --enable-cloudwatch-logs-exports '["postgresql","upgrade"]' \
    --storage-encrypted \
    --no-multi-az \
    --auto-minor-version-upgrade \
    --region $REGION \
    --tags Key=Project,Value=RealEstate Key=Environment,Value=Production

echo ""
echo "✅ RDS instance creation initiated!"
echo ""
echo "=========================================="
echo "Waiting for RDS instance to be available..."
echo "=========================================="
echo "This typically takes 5-10 minutes..."
echo ""

aws rds wait db-instance-available \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $REGION

echo ""
echo "✅ RDS instance is now available!"
echo ""

# Get RDS endpoint
ENDPOINT=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $REGION \
    --query 'DBInstances[0].Endpoint.Address' \
    --output text)

PORT=$(aws rds describe-db-instances \
    --db-instance-identifier $DB_INSTANCE_ID \
    --region $REGION \
    --query 'DBInstances[0].Endpoint.Port' \
    --output text)

echo "=========================================="
echo "RDS Instance Created Successfully! 🎉"
echo "=========================================="
echo ""
echo "Connection Details:"
echo "-------------------"
echo "Endpoint:  $ENDPOINT"
echo "Port:      $PORT"
echo "Database:  $DB_NAME"
echo "Username:  $DB_USERNAME"
echo "Password:  $DB_PASSWORD"
echo ""
echo "Connection String:"
echo "jdbc:postgresql://$ENDPOINT:$PORT/$DB_NAME"
echo ""
echo "Test Connection:"
echo "psql -h $ENDPOINT -p $PORT -U $DB_USERNAME -d $DB_NAME"
echo ""
echo "=========================================="
echo "Next Steps:"
echo "=========================================="
echo "1. Update your Helm values.yaml:"
echo "   backend.env.DB_HOST: $ENDPOINT"
echo "   backend.env.DB_PORT: $PORT"
echo ""
echo "2. Update Vercel environment variables:"
echo "   NEXT_PUBLIC_API_URL: https://your-backend-domain.com/api"
echo ""
echo "3. Deploy backend to EKS with:"
echo "   helm install real-estate ./helm/real-estate-backend \\"
echo "     --set postgresql.enabled=false \\"
echo "     --set backend.env.DB_HOST=$ENDPOINT"
echo ""
echo "=========================================="
echo ""

# Save connection details to file
cat > rds-connection-info.txt <<EOF
RDS Connection Information
==========================

Endpoint: $ENDPOINT
Port: $PORT
Database: $DB_NAME
Username: $DB_USERNAME
Password: $DB_PASSWORD

JDBC URL: jdbc:postgresql://$ENDPOINT:$PORT/$DB_NAME
Connection String: postgresql://$DB_USERNAME:$DB_PASSWORD@$ENDPOINT:$PORT/$DB_NAME

Created: $(date)
Region: $REGION
Instance ID: $DB_INSTANCE_ID
Instance Class: $DB_INSTANCE_CLASS
Storage: ${ALLOCATED_STORAGE}GB
EOF

echo "💾 Connection details saved to: rds-connection-info.txt"
echo ""
echo "⚠️  SECURITY REMINDER:"
echo "- Change the database password immediately for production"
echo "- Restrict security group access to your EKS cluster IP range"
echo "- Enable Multi-AZ for high availability in production"
echo ""
