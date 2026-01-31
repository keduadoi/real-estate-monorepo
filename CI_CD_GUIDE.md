# CI/CD Guide

This project uses **separated CI and CD pipelines** for better control over builds and deployments.

## Pipeline Architecture

```
┌─────────────────┐
│   Code Push     │
│   to GitHub     │
└────────┬────────┘
         │
         │ (automatic)
         ▼
┌─────────────────────────────────┐
│   CI Pipeline (Jenkinsfile.ci)  │
│                                  │
│  1. Build Spring Boot app        │
│  2. Run unit tests               │
│  3. Run integration tests        │
│  4. Code quality (SonarQube)     │
│  5. Security scans               │
│  6. Build Docker image           │
│  7. Scan Docker image            │
│  8. Push to ECR                  │
│  9. Create deployment manifest   │
└─────────────┬───────────────────┘
              │
              │ (manual trigger)
              ▼
┌─────────────────────────────────┐
│   CD Pipeline (Jenkinsfile.cd)  │
│                                  │
│  1. Verify image exists          │
│  2. Manual approval (prod only)  │
│  3. Database migrations          │
│  4. Deploy to EKS via Helm       │
│  5. Smoke tests                  │
│  6. Tag deployment               │
└──────────────────────────────────┘
```

## CI Pipeline (Continuous Integration)

**File:** `Jenkinsfile.ci`

### Trigger
- **Automatic** on every push to GitHub (via webhook)
- Runs on all branches

### What it does
1. ✅ Build the application
2. ✅ Run all tests (unit + integration)
3. ✅ Code quality analysis (SonarQube)
4. ✅ Security vulnerability scanning (OWASP, Trivy)
5. ✅ Build Docker image
6. ✅ Scan Docker image
7. ✅ Push image to Amazon ECR
8. ✅ Create deployment manifest

### What it does NOT do
- ❌ Deploy to any environment
- ❌ Run database migrations
- ❌ Modify Kubernetes/EKS

### Output
After a successful CI build, you'll get:
- Docker image tagged with commit hash (e.g., `abc12345`)
- Image pushed to ECR
- Deployment manifest artifact
- Slack notification with image tag

### Jenkins Setup
```groovy
Job name: real-estate-backend-ci
Type: Pipeline
Pipeline script from SCM:
  - SCM: Git
  - Script Path: Jenkinsfile.ci
  - Branches: */develop, */staging, */main
```

## CD Pipeline (Continuous Deployment)

**File:** `Jenkinsfile.cd`

### Trigger
- **Manual only** - triggered by a user when ready to deploy

### Parameters
| Parameter | Description | Required |
|-----------|-------------|----------|
| `ENVIRONMENT` | Target environment (dev/staging/prod) | Yes |
| `IMAGE_TAG` | Docker image tag from CI build | Yes |
| `SKIP_DB_MIGRATION` | Skip database migration | No |
| `RUN_SMOKE_TESTS` | Run smoke tests after deploy | No |
| `RUN_PERFORMANCE_TESTS` | Run performance tests (staging only) | No |

### What it does
1. ✅ Verify the image exists in ECR
2. ✅ Request manual approval (for production only)
3. ✅ Run database migrations (Flyway)
4. ✅ Deploy to EKS using Helm
5. ✅ Run smoke tests
6. ✅ Tag the deployment

### What it does NOT do
- ❌ Build the application
- ❌ Run tests on the code
- ❌ Build Docker images

### Jenkins Setup
```groovy
Job name: real-estate-backend-cd
Type: Pipeline
Pipeline script from SCM:
  - SCM: Git
  - Script Path: Jenkinsfile.cd
  - This build is parameterized: Yes
```

## Typical Workflow

### 1. Developer pushes code
```bash
git add .
git commit -m "Add new feature"
git push origin develop
```

### 2. CI Pipeline runs automatically
- GitHub webhook triggers Jenkins
- CI pipeline builds, tests, and publishes image
- You get a Slack notification with the image tag (e.g., `abc12345`)

### 3. Deploy to dev/staging (when ready)
1. Go to Jenkins → `real-estate-backend-cd`
2. Click **"Build with Parameters"**
3. Fill in:
   - Environment: `dev` or `staging`
   - Image Tag: `abc12345` (from CI notification)
4. Click **"Build"**
5. Deployment runs automatically (no approval needed)

### 4. Deploy to production (when ready)
1. Go to Jenkins → `real-estate-backend-cd`
2. Click **"Build with Parameters"**
3. Fill in:
   - Environment: `prod`
   - Image Tag: `abc12345` (from CI notification)
4. Click **"Build"**
5. ⚠️ **Manual approval required** - approve in Jenkins UI
6. Deployment proceeds after approval

## Environment-Specific Behavior

### Development (`dev`)
- No approval required
- Deploys immediately
- Lower resource limits
- Debug logging enabled

### Staging (`staging`)
- No approval required
- Optional performance tests
- Production-like configuration
- Used for final testing

### Production (`prod`)
- ⚠️ **Manual approval required**
- Rollback capability
- Full resource allocation
- Monitoring and alerting

## Rollback Procedure

If you need to rollback a deployment:

1. Find the previous working image tag
   - Check previous CD build artifacts
   - Or check deployment history: `kubectl rollout history deployment/real-estate-backend -n prod`

2. Run CD pipeline with the old image tag
   ```
   Environment: prod
   Image Tag: <previous-working-tag>
   ```

3. Or use kubectl directly:
   ```bash
   kubectl rollout undo deployment/real-estate-backend -n prod
   ```

## Jenkins Configuration Required

### Credentials
Configure these in Jenkins → Credentials:
- `aws-account-id` - AWS account ID
- `aws-credentials` - AWS access key and secret
- `sonarqube-token` - SonarQube authentication token
- `slack-webhook-url` - Slack webhook for notifications

### Plugins Required
- Pipeline
- Git
- Docker Pipeline
- AWS Steps
- Kubernetes CLI
- SonarQube Scanner
- Slack Notification
- HTML Publisher

### Webhook Setup
In GitHub repository settings:
1. Go to Settings → Webhooks → Add webhook
2. Payload URL: `https://your-jenkins.com/github-webhook/`
3. Content type: `application/json`
4. Events: `Just the push event`
5. Active: ✓

## Benefits of Separated CI/CD

✅ **Controlled deployments** - Deploy when YOU decide, not automatically

✅ **Flexibility** - Build once, deploy many times to different environments

✅ **Faster feedback** - CI gives quick feedback on code quality

✅ **Safer production** - Manual approval gate for production deployments

✅ **Easy rollback** - Just redeploy a previous image tag

✅ **Cost efficient** - Don't waste resources deploying every commit

✅ **Better testing** - Test the exact same artifact in staging before prod

## Troubleshooting

### CI pipeline fails
- Check test results in Jenkins
- Review SonarQube quality gate
- Check security scan reports
- Fix issues and push again

### CD pipeline can't find image
- Verify CI pipeline completed successfully
- Check ECR repository for the image tag
- Ensure you're using the correct tag from CI build

### Deployment fails
- Check Kubernetes pod logs: `kubectl logs -n <env> -l app=real-estate-backend`
- Check Helm deployment status: `helm status real-estate-backend -n <env>`
- Review database migration logs
- Check AWS resources (RDS, EKS)

### Manual approval not showing
- Verify you're in the correct user group (`admin`, `devops-team`)
- Check Jenkins user permissions
- Look for the "Paused for Input" stage in build console

## Monitoring

After deployment, monitor:
- Application logs: `kubectl logs -n <env> -l app=real-estate-backend -f`
- Pod status: `kubectl get pods -n <env>`
- Service health: `curl http://<service-url>:8080/actuator/health`
- Database: Check RDS metrics in AWS console

## Legacy Jenkinsfile

The old `Jenkinsfile` (combined CI/CD) is kept for reference but should not be used. It has been replaced by the separated CI/CD approach.
