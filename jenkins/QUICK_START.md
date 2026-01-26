# Jenkins CI/CD Quick Start Guide

Get your Jenkins pipeline running in 15 minutes!

## ⚡ Prerequisites Checklist

Before starting, ensure you have:

- [ ] AWS Account
- [ ] GitHub repository with code
- [ ] Jenkins server running (or will install)
- [ ] `aws` CLI configured
- [ ] `kubectl` installed
- [ ] EKS cluster created (or will create)

---

## 🚀 15-Minute Setup

### 1. Deploy Infrastructure (5 minutes)

```bash
# Create RDS database
cd terraform/environments/dev
terraform init
terraform apply -auto-approve

# Save RDS endpoint
export RDS_ENDPOINT=$(terraform output -raw rds_address)
echo "RDS Endpoint: $RDS_ENDPOINT"

# Create ECR repository
aws ecr create-repository \
    --repository-name real-estate-backend \
    --region us-east-1

# Create EKS cluster (if not exists)
eksctl create cluster \
    --name real-estate-cluster \
    --region us-east-1 \
    --nodegroup-name workers \
    --node-type t3.medium \
    --nodes 2
```

### 2. Configure Jenkins (5 minutes)

```bash
# Make scripts executable
chmod +x jenkins/scripts/*.sh

# Run setup (follow prompts)
./jenkins/scripts/setup-jenkins.sh

# Create IAM user for Jenkins
./jenkins/scripts/create-iam-role.sh
```

**Save the AWS credentials displayed!**

### 3. Add Credentials to Jenkins (2 minutes)

Go to: **Manage Jenkins → Manage Credentials → Global → Add Credentials**

Add these 2 credentials:

| ID | Type | Value |
|----|------|-------|
| `aws-credentials` | AWS Credentials | (from step 2) |
| `aws-account-id` | Secret text | (12-digit number) |

### 4. Configure Kubernetes RBAC (1 minute)

```bash
kubectl apply -f jenkins/config/k8s-jenkins-rbac.yaml
```

### 5. Create Pipeline (2 minutes)

1. Jenkins Dashboard → **New Item**
2. Name: `real-estate-backend`
3. Type: **Pipeline**
4. Configure:
   - Definition: **Pipeline script from SCM**
   - SCM: **Git**
   - Repository URL: `https://github.com/your-username/real-estate-ui.git`
   - Branch: `*/main`
   - Script Path: `Jenkinsfile`
5. **Save**

### 6. Run First Build! (5-10 minutes)

1. Click **Build with Parameters**
2. Settings:
   - Environment: `dev`
   - Run Tests: ✅
   - Run Security Scan: ✅
   - Deploy to EKS: ✅
3. Click **Build**

Watch the magic happen! ✨

---

## 📊 What Happens

```
Build → Test → Scan → Build Image → Push ECR → Deploy EKS → Verify
  ↓       ↓       ↓         ↓           ↓          ↓         ↓
 ✓       ✓       ✓         ✓           ✓          ✓         ✓
```

---

## 🎯 Verify Deployment

```bash
# Check pods
kubectl get pods -n dev

# Check service
kubectl get svc -n dev

# Get endpoint
kubectl get svc real-estate-backend-backend -n dev \
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'

# Test health
curl http://<endpoint>:8080/actuator/health
```

---

## 🔄 Next Builds

After first successful build:

1. Make code changes
2. Commit and push
3. Jenkins auto-builds (webhook/poll)
4. Or manually: **Build with Parameters**

---

## 🐛 Quick Troubleshooting

### Build Fails at "Push to ECR"

```bash
# Verify AWS credentials
aws sts get-caller-identity
```

### Build Fails at "Deploy to EKS"

```bash
# Verify kubeconfig
kubectl cluster-info

# Check RBAC
kubectl get serviceaccount jenkins-deployer
```

### Deployment Succeeds but Pod Crashes

```bash
# Check logs
kubectl logs -f deployment/real-estate-backend-backend -n dev

# Check events
kubectl get events -n dev --sort-by='.lastTimestamp'
```

### Can't Access Application

```bash
# Check service type
kubectl get svc real-estate-backend-backend -n dev

# If NodePort, get node IP and port
kubectl get nodes -o wide
kubectl get svc real-estate-backend-backend -n dev

# Access: http://<node-ip>:<node-port>/actuator/health
```

---

## 🎓 Optional Enhancements

After basic setup works:

### Add Slack Notifications

1. Create Slack webhook: https://api.slack.com/messaging/webhooks
2. Add to Jenkins credentials:
   - ID: `slack-webhook-url`
   - Type: Secret text
   - Value: Your webhook URL
3. Rebuild - you'll get Slack notifications!

### Add SonarQube

```bash
# Run SonarQube
docker run -d --name sonarqube -p 9000:9000 sonarqube:lts

# Configure in Jenkins (see main README)
```

### Add Performance Tests

1. Add JMeter test plans to `backend/src/test/jmeter/`
2. Tests run automatically in staging environment

---

## 📋 Commands Reference

### Common Jenkins Operations

```bash
# Restart Jenkins
sudo systemctl restart jenkins

# View Jenkins logs
sudo journalctl -u jenkins -f

# Get admin password
sudo cat /var/lib/jenkins/secrets/initialAdminPassword
```

### Common Kubernetes Operations

```bash
# Get pods in all namespaces
kubectl get pods --all-namespaces

# Describe pod
kubectl describe pod <pod-name> -n dev

# Get logs
kubectl logs -f <pod-name> -n dev

# Execute command in pod
kubectl exec -it <pod-name> -n dev -- /bin/bash

# Delete and redeploy
helm uninstall real-estate-backend -n dev
# Then trigger Jenkins build
```

### Common Docker Operations

```bash
# View local images
docker images | grep real-estate

# Clean up
docker system prune -a

# View running containers
docker ps
```

---

## 🎉 Success Checklist

After pipeline completes successfully:

- [ ] Jenkins build shows ✅ green
- [ ] Docker image in ECR
- [ ] Pods running in EKS
- [ ] Service accessible
- [ ] Health endpoint returns OK
- [ ] Database migrations applied
- [ ] Logs show no errors

---

## 🚨 Emergency Rollback

If deployment breaks production:

```bash
# Rollback with Helm
helm rollback real-estate-backend -n prod

# Or redeploy previous version
helm upgrade real-estate-backend ./helm/real-estate-backend \
    --set backend.image.tag=<previous-tag> \
    -n prod
```

---

## 📚 Next Steps

Once basic pipeline works:

1. ✅ Set up staging environment
2. ✅ Configure production pipeline
3. ✅ Add automated tests
4. ✅ Set up monitoring (Prometheus/Grafana)
5. ✅ Configure autoscaling
6. ✅ Add blue-green deployment

See [README.md](./README.md) for details.

---

## 💡 Pro Tips

1. **Always test in dev first** before deploying to production
2. **Use parameters** to skip stages during debugging
3. **Check logs** - they tell you everything
4. **Keep Terraform state** - it's your infrastructure source of truth
5. **Tag images** properly - makes rollbacks easier

---

## 🤔 Need Help?

- **Detailed docs**: See `jenkins/README.md`
- **Terraform issues**: See `terraform/README.md`
- **Kubernetes issues**: `kubectl describe pod <pod-name>`
- **Application issues**: Check application logs in CloudWatch or kubectl logs

---

**You're all set!** 🎊

Your production-ready CI/CD pipeline is now running!
