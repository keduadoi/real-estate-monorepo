# Current Setup - Kubernetes Primary

## ✅ Migration Complete!

Your Real Estate Backend is now running exclusively on **Kubernetes** with Helm.

## Current Status

### What's Running

**Kubernetes on Minikube**
```
✅ Minikube:     Running (3.5GB RAM, 2 CPUs)
✅ PostgreSQL:   Running (1/1 Ready)
✅ Backend:      Running (1/1 Ready)
✅ Port Forward: Active on localhost:8080
```

### What's Stopped

**Docker Compose**
```
❌ Stopped - No longer in use
```

## Access Your Application

### Primary URL
```
http://localhost:8080
```

This is automatically forwarded to your Kubernetes backend via `kubectl port-forward`.

### Test It Now

```bash
# Health check
curl http://localhost:8080/actuator/health

# Expected response
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"}
  }
}
```

## Quick Reference Scripts

I've created helper scripts to manage your Kubernetes setup:

### Start Everything
```bash
./k8s-start.sh
```
This will:
- Start minikube (if not running)
- Deploy the application (if not deployed)
- Set up port forwarding on port 8080
- Test the connection

### Stop Port Forwarding
```bash
./k8s-stop.sh
```
This will:
- Stop the port-forward process
- Show current pod status
- Leave pods running (for quick restart)

### Check Status
```bash
./k8s-status.sh
```
This will show:
- Minikube status
- Helm releases
- Pod status
- Service status
- Port forwarding status
- Health check results

## Common Operations

### View Logs
```bash
# Backend logs
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=backend -f

# PostgreSQL logs
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=database -f
```

### Access Database
```bash
kubectl exec -it -n real-estate-dev \
  real-estate-real-estate-backend-postgres-0 -- \
  psql -U postgres -d realestatedb
```

### Restart Application
```bash
# Restart backend
kubectl rollout restart deployment/real-estate-real-estate-backend-backend \
  -n real-estate-dev

# Wait for new pod
kubectl rollout status deployment/real-estate-real-estate-backend-backend \
  -n real-estate-dev
```

### Update Configuration
```bash
# Edit values
nano helm/real-estate-backend/values-dev.yaml

# Apply changes
helm upgrade real-estate ./helm/real-estate-backend \
  -f ./helm/real-estate-backend/values-dev.yaml
```

### Scale Application
```bash
# Scale to 2 replicas
kubectl scale deployment/real-estate-real-estate-backend-backend \
  --replicas=2 -n real-estate-dev

# Scale back to 1
kubectl scale deployment/real-estate-real-estate-backend-backend \
  --replicas=1 -n real-estate-dev
```

## Architecture

```
┌──────────────────────────────────────────────┐
│   Your Machine (localhost)                   │
│                                              │
│   Port 8080 (Port Forward)                   │
│         ↓                                    │
└─────────┼────────────────────────────────────┘
          │
          ↓
┌──────────────────────────────────────────────┐
│   Minikube Cluster                           │
│                                              │
│   ┌────────────────────────────────────┐    │
│   │  Namespace: real-estate-dev        │    │
│   │                                    │    │
│   │  ┌──────────────────────────┐     │    │
│   │  │  Backend Service         │     │    │
│   │  │  NodePort: 30080         │     │    │
│   │  └──────────────────────────┘     │    │
│   │            ↓                       │    │
│   │  ┌──────────────────────────┐     │    │
│   │  │  Backend Pod             │     │    │
│   │  │  Spring Boot App         │     │    │
│   │  │  Port: 8080              │     │    │
│   │  └──────────────────────────┘     │    │
│   │            ↓                       │    │
│   │  ┌──────────────────────────┐     │    │
│   │  │  PostgreSQL Service      │     │    │
│   │  │  ClusterIP: 5432         │     │    │
│   │  └──────────────────────────┘     │    │
│   │            ↓                       │    │
│   │  ┌──────────────────────────┐     │    │
│   │  │  PostgreSQL Pod          │     │    │
│   │  │  postgres:18-alpine      │     │    │
│   │  │  Port: 5432              │     │    │
│   │  └──────────────────────────┘     │    │
│   │                                    │    │
│   │  📦 Persistent Volumes:            │    │
│   │  - Backend uploads: 2Gi           │    │
│   │  - PostgreSQL data: 2Gi           │    │
│   └────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

## Resource Usage

### Current Allocation
- **Minikube VM**: 3.5GB RAM, 2 CPUs
- **Backend Pod**: 512Mi RAM, 500m CPU
- **PostgreSQL Pod**: 256Mi RAM, 250m CPU
- **Storage**: 4Gi total (2Gi × 2 PVCs)

### Actual Usage
```bash
# Check actual resource usage
kubectl top pods -n real-estate-dev
```

## Startup Behavior

### What Happens on Machine Restart?

**Minikube:**
- Stops when you shut down
- Needs manual restart: `minikube start`
- Or use the helper script: `./k8s-start.sh`

**Port Forward:**
- Does not survive terminal closure
- Needs to be restarted: `./k8s-start.sh`

**Application Data:**
- Persisted in volumes
- Survives minikube restarts
- Survives pod restarts

### Recommended Startup

After restarting your machine:
```bash
cd backend
./k8s-start.sh
```

This handles everything automatically!

## If You Want to Go Back to Docker Compose

You can always switch back:

```bash
# Stop Kubernetes
./k8s-stop.sh
minikube stop

# Start Docker Compose
docker-compose up -d
```

But I recommend staying with Kubernetes for:
- ✅ Production-like environment
- ✅ Better resource management
- ✅ Scalability practice
- ✅ Real-world deployment patterns

## Troubleshooting

### Port 8080 Already in Use
```bash
# Kill any process on 8080
lsof -ti:8080 | xargs kill -9

# Restart port-forward
./k8s-start.sh
```

### Pods Not Ready
```bash
# Check pod status
kubectl get pods -n real-estate-dev

# Check logs
kubectl logs -n real-estate-dev <pod-name>

# Describe pod for events
kubectl describe pod -n real-estate-dev <pod-name>
```

### Cannot Access Application
```bash
# Verify port-forward is running
lsof -i:8080

# If not, restart
./k8s-start.sh

# Check pod health
kubectl get pods -n real-estate-dev
```

### Minikube Won't Start
```bash
# Delete and recreate
minikube delete
minikube start --memory=3500 --cpus=2

# Redeploy application
helm install real-estate ./helm/real-estate-backend \
  -f ./helm/real-estate-backend/values-dev.yaml
```

## Documentation

- **Quick Start**: [docs/KUBERNETES_QUICKSTART.md](KUBERNETES_QUICKSTART.md)
- **Full Migration**: [docs/KUBERNETES_MIGRATION.md](KUBERNETES_MIGRATION.md)
- **Deployment Summary**: [docs/DEPLOYMENT_SUMMARY.md](DEPLOYMENT_SUMMARY.md)
- **Helm Chart**: [helm/real-estate-backend/README.md](../helm/real-estate-backend/README.md)

## Support

If you encounter any issues:
1. Check this document first
2. Run `./k8s-status.sh` to diagnose
3. Check pod logs
4. Refer to the troubleshooting sections in documentation

---

**Last Updated**: January 22, 2026
**Status**: ✅ Kubernetes is your primary setup
**Docker Compose**: Stopped (can be restarted if needed)
**Port**: localhost:8080 → Kubernetes backend
