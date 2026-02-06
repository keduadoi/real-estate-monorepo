# ✅ Deployment Successful - Plain Kubernetes

Your Real Estate Backend has been successfully deployed using plain Kubernetes!

## 🎉 What's Running

```
Namespace: real-estate
├── PostgreSQL Database (StatefulSet)
│   ├── Pod: real-estate-backend-postgres-0
│   ├── Service: real-estate-backend-postgres (ClusterIP:5432)
│   └── Storage: 5Gi PVC
│
└── Backend Application (Deployment)
    ├── Pod: real-estate-backend-backend-xxx
    ├── Service: real-estate-backend-backend (NodePort:30080)
    └── Storage: 5Gi PVC
```

## ✅ Health Status

**Backend Application:** ✅ UP
- Status: Running and healthy
- Database connection: ✅ Connected to PostgreSQL
- Health endpoint: `/actuator/health` responding

**PostgreSQL Database:** ✅ UP
- Status: Running and ready
- Version: PostgreSQL 18 (Alpine)

## 🌐 Access the Application

### Option 1: From Inside the Cluster

```bash
# Execute into backend pod
kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- bash

# Test health endpoint
curl localhost:8080/actuator/health
```

### Option 2: Port Forward (Recommended for Development)

```bash
# Forward backend port to localhost
kubectl port-forward service/real-estate-backend-backend 8080:8080 -n real-estate

# Access from your browser or curl
curl http://localhost:8080/actuator/health
```

### Option 3: Via Minikube IP (NodePort)

```bash
# Get minikube IP
minikube ip
# Example: 192.168.49.2

# Access via NodePort
curl http://192.168.49.2:30080/actuator/health

# Or open in browser
open http://$(minikube ip):30080/actuator/health
```

### Option 4: Minikube Service (Easiest)

```bash
# Open service in browser automatically
minikube service real-estate-backend-backend -n real-estate
```

## 🔍 Verify Deployment

```bash
# Check all resources
./status.sh

# Or manually
kubectl get all -n real-estate

# Check logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate
kubectl logs -f statefulset/real-estate-backend-postgres -n real-estate

# Check health
kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- \
  curl -s localhost:8080/actuator/health | jq
```

## 🗄️ Access PostgreSQL Database

### From Outside Kubernetes

```bash
# Port forward PostgreSQL
kubectl port-forward service/real-estate-backend-postgres 5432:5432 -n real-estate

# Connect with psql (in another terminal)
psql -h localhost -p 5432 -U postgres -d realestatedb
# Password: postgres
```

### From Inside Backend Pod

```bash
# Execute into backend pod
kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- bash

# Install psql (if needed)
apt-get update && apt-get install -y postgresql-client

# Connect to PostgreSQL
psql -h real-estate-backend-postgres -p 5432 -U postgres -d realestatedb
```

## 📊 Deployment Details

### Configuration

**ConfigMap:** `real-estate-backend-config`
```yaml
SPRING_PROFILES_ACTIVE: dev
SERVER_PORT: 8080
DB_HOST: real-estate-backend-postgres
DB_PORT: 5432
DB_NAME: realestatedb
CORS_ALLOWED_ORIGINS: http://localhost:3000,http://localhost:3001
```

**Secret:** `real-estate-backend-secret`
- DB credentials (base64 encoded)
- PostgreSQL credentials

### Resources

**Backend:**
- Requests: 500m CPU, 512Mi Memory
- Limits: 1000m CPU, 1024Mi Memory

**PostgreSQL:**
- Requests: 250m CPU, 256Mi Memory
- Limits: 500m CPU, 512Mi Memory

### Health Checks

**Backend:**
- Liveness Probe: HTTP GET /actuator/health (every 10s)
- Readiness Probe: HTTP GET /actuator/health (every 10s)

**PostgreSQL:**
- Liveness Probe: pg_isready (every 10s)
- Readiness Probe: pg_isready (every 10s)

## 🔄 Management Commands

### View Status
```bash
./status.sh
```

### View Logs
```bash
# Backend logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate

# PostgreSQL logs
kubectl logs -f statefulset/real-estate-backend-postgres -n real-estate

# Follow logs from all pods
kubectl logs -f -l app=real-estate-backend -n real-estate --all-containers
```

### Restart Services
```bash
# Restart backend
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate

# Restart PostgreSQL (use with caution)
kubectl rollout restart statefulset/real-estate-backend-postgres -n real-estate

# Check rollout status
kubectl rollout status deployment/real-estate-backend-backend -n real-estate
```

### Scale Backend
```bash
# Scale to 3 replicas
kubectl scale deployment/real-estate-backend-backend --replicas=3 -n real-estate

# Check scaling
kubectl get deployment real-estate-backend-backend -n real-estate
```

### Update Image
```bash
# Build new image
cd ../
docker build -t real-estate-backend:v2 -f Dockerfile .

# For minikube, load image
minikube image load real-estate-backend:v2

# Update deployment
kubectl set image deployment/real-estate-backend-backend \
  backend=real-estate-backend:v2 -n real-estate

# Watch rollout
kubectl rollout status deployment/real-estate-backend-backend -n real-estate
```

## 🗑️ Cleanup

### Remove Everything (Including Data)
```bash
./cleanup.sh
```

### Remove Only Application (Keep Data)
```bash
kubectl delete deployment/real-estate-backend-backend -n real-estate
kubectl delete statefulset/real-estate-backend-postgres -n real-estate
# PVCs are preserved
```

### Remove Namespace (Removes Everything)
```bash
kubectl delete namespace real-estate
```

## 📝 What Changed from Helm Deployment

### Before (Helm)
- Deployed with: `helm install`
- Configuration: Template-based with values.yaml
- Resource names: `real-estate-real-estate-backend-*`
- Rollback: `helm rollback`

### Now (Plain Kubernetes)
- Deployed with: `kubectl apply`
- Configuration: Direct YAML files
- Resource names: `real-estate-backend-*`
- Rollback: `kubectl rollout undo`

## 🎓 Learning Points

1. **PostgreSQL runs INSIDE Kubernetes** as a StatefulSet
2. **Both backend and database** share the same K8s cluster
3. **Service discovery** uses Kubernetes DNS (real-estate-backend-postgres)
4. **PersistentVolumeClaims** manage storage
5. **NodePort** exposes backend externally on port 30080
6. **ClusterIP** keeps PostgreSQL internal to the cluster

## 🚀 Next Steps

1. **Connect Frontend**: Update frontend to use backend URL
2. **Test API Endpoints**: Use curl or Postman to test
3. **Monitor Resources**: Use `kubectl top` to monitor usage
4. **Explore K8s Features**: Try scaling, rolling updates
5. **Compare with Helm**: Deploy using Helm to see differences

## 📚 Documentation

- **Architecture**: See `ARCHITECTURE.md`
- **Comparison**: See `/HELM_VS_PLAIN_K8S.md`
- **Deployment Options**: See `/backend/DEPLOYMENT_OPTIONS.md`

## ⚠️ Important Notes

- **Default Credentials**: Change PostgreSQL password for production!
- **Old PVCs**: Previous Helm PVCs still exist but are not used
- **Minikube Only**: Currently configured for minikube cluster
- **Data Persistence**: Data survives pod restarts but review backup strategy

## 🎉 Success!

Your application is now running on Kubernetes using plain manifests!

Test it:
```bash
# Port forward
kubectl port-forward service/real-estate-backend-backend 8080:8080 -n real-estate

# Test in another terminal
curl http://localhost:8080/actuator/health
```

Enjoy exploring Kubernetes! 🚀
