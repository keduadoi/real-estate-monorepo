# Jenkins CI/CD Pipeline for Real Estate Backend

Production-ready Jenkins pipeline for building, testing, and deploying the Real Estate backend to AWS EKS.

## 📋 Table of Contents

- [Architecture Overview](#architecture-overview)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Pipeline Stages](#pipeline-stages)
- [Configuration](#configuration)
- [Security](#security)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## 🏗️ Architecture Overview

```
┌─────────────┐
│   GitHub    │
│  Repository │
└──────┬──────┘
       │ Webhook/Poll
       ▼
┌─────────────┐
│   Jenkins   │
│   Server    │
└──────┬──────┘
       │
       ├──── Build & Test (Maven)
       ├──── Code Quality (SonarQube)
       ├──── Security Scan (OWASP, Trivy)
       ├──── Build Docker Image
       │
       ▼
┌─────────────┐
│  Amazon ECR │
│   Registry  │
└──────┬──────┘
       │
       ▼
┌─────────────┐      ┌──────────────┐
│ RDS         │◄─────┤  Amazon EKS  │
│ PostgreSQL  │      │   Cluster    │
└─────────────┘      └──────────────┘
                            ▲
                            │ Deploy via Helm
                            │
                     ┌──────┴───────┐
                     │   Backend    │
                     │ Application  │
                     └──────────────┘
```

---

## 📦 Prerequisites

### 1. Infrastructure

- ✅ **Jenkins Server** (v2.400+)
- ✅ **AWS Account** with appropriate permissions
- ✅ **EKS Cluster** (created via eksctl or Terraform)
- ✅ **RDS PostgreSQL** (created via Terraform)
- ✅ **ECR Repository** for Docker images

### 2. Required Tools

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 17+ | Build Spring Boot app |
| Maven | 3.8+ | Dependency management |
| Docker | 20.10+ | Container builds |
| kubectl | 1.27+ | Kubernetes management |
| Helm | 3.12+ | Kubernetes deployments |
| AWS CLI | 2.x | AWS operations |
| Trivy | Latest | Security scanning |

### 3. Jenkins Plugins

Required Jenkins plugins (install via script):

- Git & GitHub
- Pipeline (workflow-aggregator)
- Docker Pipeline
- Kubernetes CLI
- AWS Credentials
- Amazon ECR
- Slack Notification
- Email Extension
- SonarQube Scanner
- OWASP Dependency Check
- JUnit
- HTML Publisher
- AnsiColor
- Timestamper

---

## 🚀 Quick Start

### Step 1: Install Jenkins (if not installed)

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk -y
wget -q -O - https://pkg.jenkins.io/debian-stable/jenkins.io.key | sudo apt-key add -
sudo sh -c 'echo deb https://pkg.jenkins.io/debian-stable binary/ > /etc/apt/sources.list.d/jenkins.list'
sudo apt update
sudo apt install jenkins -y
sudo systemctl start jenkins
sudo systemctl enable jenkins

# Get initial admin password
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

### Step 2: Run Setup Script

```bash
chmod +x jenkins/scripts/setup-jenkins.sh
./jenkins/scripts/setup-jenkins.sh
```

### Step 3: Create IAM User

```bash
chmod +x jenkins/scripts/create-iam-role.sh
./jenkins/scripts/create-iam-role.sh
```

Save the AWS credentials displayed!

### Step 4: Configure Jenkins Credentials

Go to **Manage Jenkins → Manage Credentials → Global → Add Credentials**

Add these credentials:

| ID | Type | Value |
|----|------|-------|
| `aws-credentials` | AWS Credentials | Access Key + Secret Key from Step 3 |
| `aws-account-id` | Secret text | Your 12-digit AWS account ID |
| `slack-webhook-url` | Secret text | Slack webhook URL (optional) |
| `sonarqube-token` | Secret text | SonarQube token (optional) |

### Step 5: Create ECR Repository

```bash
aws ecr create-repository \
    --repository-name real-estate-backend \
    --region us-east-1
```

### Step 6: Set Up Kubernetes RBAC

```bash
# Apply RBAC configuration
kubectl apply -f jenkins/config/k8s-jenkins-rbac.yaml

# Verify
kubectl get serviceaccount jenkins-deployer
kubectl get clusterrolebinding jenkins-deployer-binding
```

### Step 7: Deploy RDS with Terraform

```bash
cd terraform/environments/dev
terraform init
terraform apply
```

Save the RDS endpoint!

### Step 8: Create Jenkins Pipeline

1. Go to Jenkins Dashboard
2. Click **New Item**
3. Name: `real-estate-backend`
4. Type: **Pipeline**
5. Configure:
   - **Definition**: Pipeline script from SCM
   - **SCM**: Git
   - **Repository URL**: Your GitHub repo URL
   - **Branch**: `*/main` or `*/develop`
   - **Script Path**: `Jenkinsfile`
6. Save

### Step 9: Run First Build

1. Click **Build with Parameters**
2. Select:
   - Environment: `dev`
   - Run Tests: ✅
   - Deploy to EKS: ✅
3. Click **Build**

---

## 📊 Pipeline Stages

### 1. Initialize
- Print build information
- Send Slack notification
- Set environment variables

### 2. Checkout
- Clone repository
- Display commit information

### 3. Build Maven Project
- Compile Spring Boot application
- Package JAR file
- Skip tests (run in next stage)

### 4. Run Tests (Parallel)
- **Unit Tests**: JUnit tests
- **Integration Tests**: Spring Boot integration tests
- Generate test reports

### 5. Code Quality Analysis
- Run SonarQube analysis
- Check code coverage
- Identify code smells

### 6. Quality Gate
- Wait for SonarQube quality gate
- Fail pipeline if quality gate fails

### 7. Security Scan (Parallel)
- **OWASP Dependency Check**: Check for vulnerable dependencies
- **Trivy Filesystem Scan**: Scan codebase for secrets/vulnerabilities

### 8. Build Docker Image
- Build multi-stage Docker image
- Tag with commit SHA, environment, and latest
- Use BuildKit for optimization

### 9. Scan Docker Image
- Trivy container scan
- Fail on CRITICAL vulnerabilities
- Generate vulnerability report

### 10. Push to ECR
- Authenticate with AWS ECR
- Push Docker image with multiple tags
- Clean up local images

### 11. Database Migration
- Get RDS endpoint from Terraform
- Run Flyway migrations
- Verify migration success

### 12. Update kubeconfig
- Configure kubectl for EKS cluster
- Verify connection
- List nodes

### 13. Deploy to EKS
- Deploy using Helm
- Update image tag
- Configure environment variables
- Wait for deployment to be ready
- Use `--atomic` flag for automatic rollback

### 14. Smoke Tests
- Get service endpoint
- Test health endpoint
- Verify application is responding

### 15. Performance Tests (Staging Only)
- Run JMeter load tests
- Generate performance report
- Check response times

---

## ⚙️ Configuration

### Environment Variables

Configure in `Jenkinsfile`:

```groovy
environment {
    AWS_REGION = 'us-east-1'
    ECR_REPOSITORY = 'real-estate-backend'
    EKS_CLUSTER_NAME = 'real-estate-cluster'
    // ... other variables
}
```

### Pipeline Parameters

Available build parameters:

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| ENVIRONMENT | Choice | dev | Target environment (dev/staging/prod) |
| RUN_TESTS | Boolean | true | Run unit and integration tests |
| RUN_SECURITY_SCAN | Boolean | true | Run security scans |
| SKIP_DB_MIGRATION | Boolean | false | Skip database migration |
| DEPLOY_TO_EKS | Boolean | true | Deploy to EKS cluster |

### Environment-Specific Configuration

Create Helm values files for each environment:

```bash
backend/helm/real-estate-backend/
├── values.yaml              # Default values
├── values-dev.yaml          # Development overrides
├── values-staging.yaml      # Staging overrides
└── values-prod.yaml         # Production overrides
```

**Example `values-prod.yaml`:**

```yaml
backend:
  replicaCount: 3
  image:
    pullPolicy: Always
  resources:
    limits:
      cpu: 2000m
      memory: 2Gi
    requests:
      cpu: 1000m
      memory: 1Gi
  env:
    SPRING_PROFILES_ACTIVE: prod
    DB_HOST: "${RDS_ENDPOINT}"  # Replaced by pipeline
```

---

## 🔐 Security

### Best Practices

1. **Credentials Management**
   - Store all secrets in Jenkins Credentials
   - Use AWS Secrets Manager for database passwords
   - Never commit credentials to Git

2. **IAM Permissions**
   - Use least privilege principle
   - Create dedicated IAM user for Jenkins
   - Rotate access keys regularly

3. **Image Security**
   - Scan images before deployment
   - Use trusted base images
   - Keep images updated

4. **Network Security**
   - RDS in private subnet
   - Security groups restrict access
   - Use VPN/bastion for debugging

5. **Kubernetes Security**
   - Use RBAC for service accounts
   - Limit namespace permissions
   - Enable Pod Security Policies

### Security Scanning Results

Pipeline generates these reports:

- **OWASP Dependency Check**: `dependency-check-report.html`
- **Trivy Filesystem Scan**: `trivy-report.json`
- **Trivy Image Scan**: `trivy-image-report.json`

Access via Jenkins build artifacts.

---

## 📈 Monitoring

### Jenkins Monitoring

Monitor pipeline execution:

```bash
# View build console output
http://jenkins-url/job/real-estate-backend/lastBuild/console

# View test results
http://jenkins-url/job/real-estate-backend/lastBuild/testReport/

# View artifacts
http://jenkins-url/job/real-estate-backend/lastBuild/artifact/
```

### Application Monitoring

After deployment:

```bash
# Check pod status
kubectl get pods -n dev

# View logs
kubectl logs -f deployment/real-estate-backend-backend -n dev

# Check service
kubectl get service real-estate-backend-backend -n dev

# Health check
curl http://<service-endpoint>:8080/actuator/health
```

### Notifications

Configure notifications in Jenkinsfile:

- **Slack**: Real-time build status
- **Email**: Build failures and test results
- **GitHub**: Commit status checks

---

## 🔧 Troubleshooting

### Common Issues

#### 1. "Cannot connect to Docker daemon"

**Solution:**
```bash
# Add Jenkins user to docker group
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins
```

#### 2. "kubectl: command not found"

**Solution:**
```bash
# Install kubectl for Jenkins user
sudo su - jenkins
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/
```

#### 3. "Access denied to ECR"

**Solution:**
```bash
# Verify AWS credentials
aws sts get-caller-identity

# Test ECR login
aws ecr get-login-password --region us-east-1 | \
    docker login --username AWS --password-stdin <account-id>.dkr.ecr.us-east-1.amazonaws.com
```

#### 4. "Helm deployment failed"

**Solution:**
```bash
# Check Helm release
helm list -n dev

# View release history
helm history real-estate-backend -n dev

# Rollback if needed
helm rollback real-estate-backend -n dev
```

#### 5. "Database migration failed"

**Solution:**
```bash
# Check RDS connectivity
psql -h <rds-endpoint> -U postgres -d realestatedb

# View Flyway history
SELECT * FROM flyway_schema_history;

# Manually run migration
flyway migrate -url=jdbc:postgresql://<rds-endpoint>:5432/realestatedb
```

#### 6. "Tests failing in pipeline but pass locally"

**Possible causes:**
- Environment variables missing
- Database not available
- Port conflicts
- Test data not seeded

**Solution:**
```bash
# Check test configuration
cat backend/src/test/resources/application-test.yml

# Run tests with same profile
./mvnw test -Dspring.profiles.active=test
```

---

## 🚀 Advanced Topics

### Multi-Branch Pipeline

For feature branch deployments:

```groovy
// In Jenkinsfile
when {
    branch 'feature/*'
}
steps {
    // Deploy to ephemeral environment
}
```

### Blue-Green Deployment

```bash
# Deploy new version
helm upgrade real-estate-backend ./helm/real-estate-backend \
    --set backend.image.tag=v2.0.0 \
    --reuse-values

# Test new version
# If OK, traffic automatically routes to new pods
# If issues, rollback:
helm rollback real-estate-backend
```

### Canary Deployment

Use Flagger or Argo Rollouts for progressive delivery.

### Auto-Scaling

Configure HPA in Helm values:

```yaml
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

---

## 📚 Additional Resources

- [Jenkins Documentation](https://www.jenkins.io/doc/)
- [AWS EKS Best Practices](https://aws.github.io/aws-eks-best-practices/)
- [Helm Documentation](https://helm.sh/docs/)
- [Spring Boot on Kubernetes](https://spring.io/guides/gs/spring-boot-kubernetes/)

---

## 🤝 Contributing

To improve the pipeline:

1. Test changes in dev environment first
2. Update documentation
3. Create PR with detailed description
4. Ensure all stages pass

---

## 📞 Support

For issues:
1. Check troubleshooting section
2. Review Jenkins console output
3. Check Kubernetes pod logs
4. Review CloudWatch logs (for RDS issues)

---

**Pipeline Version:** 1.0.0
**Last Updated:** 2026-01-22
**Maintained by:** DevOps Team
