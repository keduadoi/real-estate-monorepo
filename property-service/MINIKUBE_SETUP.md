# Minikube Setup with External PostgreSQL

Quick start guide for running the Real Estate backend in Minikube with PostgreSQL in Docker.

## Why This Setup?

This approach matches production architecture where:
- Database runs externally (simulating AWS RDS)
- Backend runs in Kubernetes (simulating AWS EKS)

## Prerequisites

- Docker Desktop
- Minikube
- Helm 3
- kubectl

## Quick Start

### 1. Start Minikube

```bash
minikube start --memory=3500 --cpus=2

# Verify
minikube status
```

### 2. Start PostgreSQL in Docker

```bash
cd backend
docker-compose -f docker-compose-db.yml up -d

# Verify PostgreSQL is running
docker ps | grep postgres
docker logs real-estate-postgres-local
```

### 3. Build Backend Image

**Important:** Build the image in Minikube's Docker environment:

```bash
# Point Docker CLI to Minikube's Docker daemon
eval $(minikube docker-env)

# Build
cd backend
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .

# Verify image exists in Minikube
docker images | grep real-estate-backend
```

### 4. Deploy Backend to Minikube

```bash
cd backend
helm install real-estate ./helm/real-estate-backend \
  -f ./helm/real-estate-backend/values-minikube.yaml

# Watch deployment
kubectl get pods -n real-estate -w
```

### 5. Access the Application

```bash
# Option 1: Minikube service (opens browser automatically)
minikube service real-estate-backend-backend -n real-estate

# Option 2: Get URL manually
export NODE_IP=$(minikube ip)
echo "Application URL: http://$NODE_IP:30080"

# Option 3: Port forwarding
kubectl port-forward -n real-estate svc/real-estate-backend-backend 8080:8080
```

### 6. Test the Application

```bash
# Health check
curl http://$(minikube ip):30080/actuator/health

# Expected response
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

## Network Configuration

The key configuration for Minikube to access Docker PostgreSQL:

```yaml
# values-minikube.yaml
backend:
  env:
    dbHost: host.minikube.internal  # Special DNS name for Minikube
    dbPort: 5432
    dbName: realestatedb
    dbUser: postgres
    dbPassword: postgres
```

### Alternative: Use Host IP

If `host.minikube.internal` doesn't work:

```bash
# Get host IP from inside Minikube
minikube ssh "route -n | grep ^0.0.0.0 | awk '{ print \$2 }'"

# Use this IP in values-minikube.yaml
# Example: dbHost: 192.168.65.2
```

## Common Operations

### View Logs

```bash
# Backend logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate

# PostgreSQL logs
docker logs -f real-estate-postgres-local
```

### Rebuild and Redeploy

```bash
# 1. Build new image in Minikube
eval $(minikube docker-env)
cd backend
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .

# 2. Restart deployment
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate

# 3. Watch rollout
kubectl rollout status deployment/real-estate-backend-backend -n real-estate
```

### Access PostgreSQL

```bash
# From host
docker exec -it real-estate-postgres-local psql -U postgres -d realestatedb

# Or using psql client
psql -h localhost -p 5432 -U postgres -d realestatedb

# Inside Minikube (test connectivity)
kubectl run -it --rm psql --image=postgres:18-alpine --restart=Never -n real-estate -- \
  psql -h host.minikube.internal -U postgres -d realestatedb
```

### Debug Database Connection

```bash
# Check environment variables
kubectl exec deployment/real-estate-backend-backend -n real-estate -- env | grep DB_

# Test connectivity from pod
kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- sh
# Inside pod:
ping host.minikube.internal
nc -zv host.minikube.internal 5432
curl http://localhost:8080/actuator/health
```

### Clean Up

```bash
# Uninstall Helm release
helm uninstall real-estate -n real-estate

# Delete namespace
kubectl delete namespace real-estate

# Stop PostgreSQL
cd backend
docker-compose -f docker-compose-db.yml down

# Stop Minikube
minikube stop

# Delete Minikube (removes all data)
minikube delete
```

## Troubleshooting

### Backend Can't Connect to PostgreSQL

**Symptom:** Backend logs show connection refused to database

**Solutions:**

1. **Verify PostgreSQL is accessible from host:**
   ```bash
   psql -h localhost -p 5432 -U postgres -d realestatedb
   ```

2. **Check if host.minikube.internal resolves:**
   ```bash
   kubectl run -it --rm busybox --image=busybox --restart=Never -n real-estate -- \
     nslookup host.minikube.internal
   ```

3. **Try host IP instead:**
   ```bash
   # Get host IP
   HOST_IP=$(minikube ssh "route -n | grep ^0.0.0.0 | awk '{ print \$2 }'")
   echo $HOST_IP

   # Update values-minikube.yaml with this IP
   helm upgrade real-estate ./helm/real-estate-backend \
     --set backend.env.dbHost=$HOST_IP \
     -f ./helm/real-estate-backend/values-minikube.yaml
   ```

### Backend Pod CrashLooping

**Check logs:**
```bash
kubectl logs deployment/real-estate-backend-backend -n real-estate
kubectl describe pod -n real-estate -l app.kubernetes.io/component=backend
```

**Common causes:**
- Image not built in Minikube's Docker (forgot `eval $(minikube docker-env)`)
- Database connection failure
- Insufficient resources

**Fix:**
```bash
# Rebuild in correct Docker context
eval $(minikube docker-env)
docker build -t real-estate-backend:latest .
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate
```

### Port 5432 Already in Use

If you have local PostgreSQL running:

```bash
# Stop local PostgreSQL
brew services stop postgresql

# Or change Docker PostgreSQL port
# Edit docker-compose-db.yml:
ports:
  - "5433:5432"  # Use port 5433 on host
```

### Minikube IP Changes

After restarting Minikube, the IP may change:

```bash
# Get new IP
minikube ip

# Access application at new IP
curl http://$(minikube ip):30080/actuator/health
```

## Architecture

```
┌─────────────────────────────────┐
│       Host Machine              │
│                                 │
│  ┌──────────────────────────┐  │
│  │  Docker PostgreSQL       │  │
│  │  Port: 5432              │  │
│  │  Container: postgres     │  │
│  └──────────────────────────┘  │
│              ▲                  │
│              │                  │
│              │ host.minikube.internal
│              │                  │
│  ┌──────────────────────────┐  │
│  │  Minikube VM             │  │
│  │                          │  │
│  │  ┌────────────────────┐  │  │
│  │  │  Backend Pod       │  │  │
│  │  │  - Spring Boot     │  │  │
│  │  │  - Port: 8080      │  │  │
│  │  └────────────────────┘  │  │
│  │         │                │  │
│  │         ▼                │  │
│  │  NodePort: 30080         │  │
│  └──────────────────────────┘  │
│              │                  │
└──────────────┼──────────────────┘
               ▼
        http://MINIKUBE_IP:30080
```

## Comparison with Other Options

| Feature | Minikube | Kind | K3d |
|---------|----------|------|-----|
| Ease of Setup | Easy | Easy | Easy |
| Resources | Medium | Low | Very Low |
| Speed | Medium | Fast | Fastest |
| Production-like | Medium | High | Medium |
| Network Config | host.minikube.internal | host.docker.internal | host.k3d.internal |
| Multi-node | Yes | Yes | Yes |
| LoadBalancer | Via tunnel | No | Built-in |

## Next Steps

- Migrate to Kind/K3d for faster development cycles
- Set up production deployment with AWS EKS + RDS
- Configure CI/CD pipeline
- Add monitoring and logging

## Resources

- [Minikube Documentation](https://minikube.sigs.k8s.io/docs/)
- [Complete K8s Setup Guide](./LOCAL_K8S_SETUP.md)
- [Helm Chart README](./helm/real-estate-backend/README.md)
