# Kubernetes Quick Start Guide

This guide provides quick instructions to get the Real Estate Backend running on Kubernetes locally using Minikube and Helm.

## Prerequisites

Before starting, ensure you have:
- Docker Desktop installed and running
- At least 4GB RAM available for Minikube
- Command line access (Terminal/PowerShell)

## Installation Steps

### 1. Install Required Tools

```bash
# Install Helm
brew install helm

# Verify installations
minikube version  # Should show v1.37.0 or higher
helm version      # Should show v4.1.0 or higher
kubectl version --client
```

### 2. Start Minikube

```bash
# Start minikube with required resources
minikube start --memory=3500 --cpus=2

# Verify it's running
minikube status
```

Expected output:
```
minikube
type: Control Plane
host: Running
kubelet: Running
apiserver: Running
```

### 3. Build Docker Image

```bash
# Configure Docker to use Minikube's daemon
eval $(minikube docker-env)

# Build the application image
cd backend
docker build -t real-estate-backend:latest .
```

This will take 2-3 minutes. You should see:
```
[+] Building ... done
=> exporting to image
=> => naming to docker.io/library/real-estate-backend:latest
```

### 4. Deploy Application

```bash
# From the project root directory
helm install real-estate ./backend/helm/real-estate-backend \
  -f ./backend/helm/real-estate-backend/values-dev.yaml
```

You should see:
```
NAME: real-estate
LAST DEPLOYED: ...
NAMESPACE: default
STATUS: deployed
REVISION: 1
```

### 5. Wait for Pods to Start

```bash
# Watch pod status (wait until both show 1/1 Ready)
kubectl get pods -n real-estate-dev -w
```

Wait for:
```
NAME                                         READY   STATUS    RESTARTS   AGE
real-estate-...-backend-xxxxx                1/1     Running   0          2m
real-estate-...-postgres-0                   1/1     Running   0          2m
```

Press `Ctrl+C` to stop watching.

### 6. Access the Application

Choose one of these methods:

#### Method A: Port Forwarding (Recommended)

```bash
# Forward port 8080
kubectl port-forward -n real-estate-dev \
  svc/real-estate-real-estate-backend-backend 8080:8080
```

Keep this terminal open. In a new terminal:
```bash
curl http://localhost:8080/actuator/health
```

#### Method B: Minikube Service

```bash
minikube service real-estate-real-estate-backend-backend -n real-estate-dev
```

This will open the application in your browser automatically.

### 7. Verify Installation

Test the health endpoint:

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

## Quick Reference Commands

### View Resources

```bash
# All resources in namespace
kubectl get all -n real-estate-dev

# Just pods
kubectl get pods -n real-estate-dev

# Services
kubectl get svc -n real-estate-dev

# Storage
kubectl get pvc -n real-estate-dev
```

### View Logs

```bash
# Backend application logs
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=backend -f

# PostgreSQL logs
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=database -f

# Specific pod logs
kubectl logs -n real-estate-dev <pod-name> -f
```

### Access Database

```bash
# Connect to PostgreSQL
kubectl exec -it -n real-estate-dev \
  real-estate-real-estate-backend-postgres-0 -- \
  psql -U postgres -d realestatedb

# Inside psql:
\dt          # List tables
\q           # Quit
```

### Restart Application

```bash
# Restart backend
kubectl rollout restart deployment/real-estate-real-estate-backend-backend \
  -n real-estate-dev

# Restart PostgreSQL
kubectl delete pod real-estate-real-estate-backend-postgres-0 -n real-estate-dev
```

### Update Configuration

```bash
# Edit values
nano backend/helm/real-estate-backend/values-dev.yaml

# Apply changes
helm upgrade real-estate ./backend/helm/real-estate-backend \
  -f ./backend/helm/real-estate-backend/values-dev.yaml
```

### Stop Application

```bash
# Uninstall Helm release
helm uninstall real-estate

# Delete namespace (also deletes all resources)
kubectl delete namespace real-estate-dev
```

### Stop Minikube

```bash
# Stop cluster (keeps data)
minikube stop

# Delete cluster (removes everything)
minikube delete
```

## Common Issues & Solutions

### Issue: Pods Not Starting

**Check pod status:**
```bash
kubectl describe pod -n real-estate-dev <pod-name>
```

**Check logs:**
```bash
kubectl logs -n real-estate-dev <pod-name>
```

### Issue: Image Not Found

**Rebuild image in minikube context:**
```bash
eval $(minikube docker-env)
cd backend
docker build -t real-estate-backend:latest .
```

### Issue: Can't Access Application

**Use port-forward instead:**
```bash
kubectl port-forward -n real-estate-dev \
  svc/real-estate-real-estate-backend-backend 8080:8080
```

### Issue: Database Connection Error

**Check if PostgreSQL is running:**
```bash
kubectl get pods -n real-estate-dev -l app.kubernetes.io/component=database
```

**Restart if needed:**
```bash
kubectl delete pod -n real-estate-dev real-estate-real-estate-backend-postgres-0
```

### Issue: Out of Memory

**Increase minikube resources:**
```bash
minikube delete
minikube start --memory=4096 --cpus=2
# Then repeat steps 3-4
```

## Testing Checklist

- [ ] Minikube is running (`minikube status`)
- [ ] Image is built (`docker images | grep real-estate`)
- [ ] Helm release installed (`helm list`)
- [ ] Both pods are Running (`kubectl get pods -n real-estate-dev`)
- [ ] Health check passes (`curl http://localhost:8080/actuator/health`)
- [ ] Database is accessible (`kubectl exec ... psql`)
- [ ] Logs show no errors (`kubectl logs ...`)

## Next Steps

Once you have the application running:

1. **Explore the API**
   - Visit http://localhost:8080/actuator
   - Test property endpoints
   - Upload images

2. **Modify Configuration**
   - Edit `values-dev.yaml`
   - Run `helm upgrade`
   - Observe changes

3. **Scale the Application**
   ```bash
   # Scale backend to 2 replicas
   kubectl scale deployment/real-estate-real-estate-backend-backend \
     --replicas=2 -n real-estate-dev
   ```

4. **Read Full Documentation**
   - See `backend/docs/KUBERNETES_MIGRATION.md` for detailed migration info
   - See `backend/helm/real-estate-backend/README.md` for Helm chart details

## Cleanup

When you're done:

```bash
# Remove application
helm uninstall real-estate
kubectl delete namespace real-estate-dev

# Stop minikube
minikube stop

# Or completely remove minikube
minikube delete
```

## Getting Help

If you encounter issues:

1. Check the troubleshooting section above
2. View detailed logs: `kubectl logs -n real-estate-dev <pod-name>`
3. Describe resources: `kubectl describe pod -n real-estate-dev <pod-name>`
4. Check events: `kubectl get events -n real-estate-dev --sort-by=.metadata.creationTimestamp`
5. Consult the full migration guide: `backend/docs/KUBERNETES_MIGRATION.md`

## Summary

You now have:
- ✅ Kubernetes cluster running locally (Minikube)
- ✅ PostgreSQL database with persistent storage
- ✅ Spring Boot backend application
- ✅ Helm-managed deployment
- ✅ Health checks and monitoring ready
- ✅ Production-ready deployment patterns

Happy developing! 🚀
