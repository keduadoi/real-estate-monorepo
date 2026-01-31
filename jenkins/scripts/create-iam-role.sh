#!/bin/bash

# Create IAM Role for Jenkins CI/CD Pipeline
# This script creates an IAM user/role with necessary permissions

set -e

POLICY_NAME="JenkinsRealEstatePolicy"
USER_NAME="jenkins-cicd"
REGION="us-east-1"

echo "=========================================="
echo "Creating IAM User and Policy for Jenkins"
echo "=========================================="
echo ""

# Create IAM policy
echo "1. Creating IAM policy..."
POLICY_ARN=$(aws iam create-policy \
    --policy-name $POLICY_NAME \
    --policy-document file://jenkins/config/jenkins-iam-policy.json \
    --query 'Policy.Arn' \
    --output text 2>/dev/null || \
    aws iam list-policies \
        --query "Policies[?PolicyName=='$POLICY_NAME'].Arn" \
        --output text)

echo "✓ Policy ARN: $POLICY_ARN"
echo ""

# Create IAM user
echo "2. Creating IAM user..."
aws iam create-user --user-name $USER_NAME 2>/dev/null || echo "✓ User already exists"
echo "✓ User created: $USER_NAME"
echo ""

# Attach policy to user
echo "3. Attaching policy to user..."
aws iam attach-user-policy \
    --user-name $USER_NAME \
    --policy-arn $POLICY_ARN

echo "✓ Policy attached"
echo ""

# Create access key
echo "4. Creating access key..."
ACCESS_KEY=$(aws iam create-access-key --user-name $USER_NAME --output json 2>/dev/null || echo "{}")

if [ "$ACCESS_KEY" != "{}" ]; then
    ACCESS_KEY_ID=$(echo $ACCESS_KEY | jq -r '.AccessKey.AccessKeyId')
    SECRET_ACCESS_KEY=$(echo $ACCESS_KEY | jq -r '.AccessKey.SecretAccessKey')

    echo "=========================================="
    echo "Credentials Created Successfully!"
    echo "=========================================="
    echo ""
    echo "AWS Access Key ID: $ACCESS_KEY_ID"
    echo "AWS Secret Access Key: $SECRET_ACCESS_KEY"
    echo ""
    echo "⚠️  IMPORTANT: Save these credentials securely!"
    echo "⚠️  You won't be able to retrieve the secret key again."
    echo ""

    # Save to file (be careful with this!)
    cat > jenkins-aws-credentials.txt <<EOF
AWS Access Key ID: $ACCESS_KEY_ID
AWS Secret Access Key: $SECRET_ACCESS_KEY
User Name: $USER_NAME
Policy ARN: $POLICY_ARN
Created: $(date)
EOF

    echo "✓ Credentials saved to: jenkins-aws-credentials.txt"
    echo ""
else
    echo "⚠️  Access key already exists or couldn't create"
    echo "   List existing keys: aws iam list-access-keys --user-name $USER_NAME"
    echo ""
fi

echo "=========================================="
echo "Next Steps"
echo "=========================================="
echo ""
echo "1. Add credentials to Jenkins:"
echo "   - Go to: Manage Jenkins → Manage Credentials"
echo "   - Add AWS Credentials with ID: aws-credentials"
echo "   - Use the Access Key ID and Secret Access Key above"
echo ""
echo "2. Get your AWS Account ID:"
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo "   Account ID: $AWS_ACCOUNT_ID"
echo "   Add as Secret text with ID: aws-account-id"
echo ""
echo "3. Create ECR repository:"
echo "   aws ecr create-repository --repository-name real-estate-backend --region $REGION"
echo ""
