# Backend Startup Scripts

This directory contains scripts for starting and managing the Real Estate backend application.

## Available Approaches

### 1. Bundled Mode (Original - Both PostgreSQL + Backend in K8s)

Uses the original approach where both PostgreSQL and backend run inside Kubernetes.

**Scripts:**
- `k8s-start.sh` - Start backend with bundled PostgreSQL
- `k8s-stop.sh` - Stop port forwarding
- `k8s-status.sh` - Check status

**When to use:**
- Testing full Kubernetes stack
- Learning Kubernetes StatefulSets
- No external dependencies needed

### 2. External PostgreSQL Mode (New - Matches Production)

**Recommended for development.** PostgreSQL runs in Docker (simulating RDS), backend runs in Minikube (simulating EKS).

**Scripts:**
- `k8s-start-external.sh` - Start PostgreSQL in Docker + Backend in Minikube
- `k8s-stop-external.sh` - Stop services (with options)
- `k8s-status-external.sh` - Check status of both Docker and K8s

**When to use:**
- Local development (recommended)
- Dev/prod parity
- Faster iterations
- Direct PostgreSQL access

## Quick Start

### External PostgreSQL Mode (Recommended)

```bash
# Start everything
./k8s-start-external.sh

# Check status
./k8s-status-external.sh

# View logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate
docker logs -f real-estate-postgres-local

# Stop everything
./k8s-stop-external.sh
```

### Bundled Mode (Original)

```bash
# Start everything
./k8s-start.sh

# Check status
./k8s-status.sh

# Stop port forwarding
./k8s-stop.sh
```

## Comparison

| Feature | Bundled Mode | External Mode |
|---------|--------------|---------------|
| **PostgreSQL Location** | Kubernetes StatefulSet | Docker container |
| **Backend Location** | Kubernetes Deployment | Kubernetes Deployment |
| **Access Method** | Port forwarding (8080) | NodePort (30080) |
| **DB Access** | Port forward (5432) | Direct (localhost:5432) |
| **Startup Time** | ~2 minutes | ~1 minute |
| **Resource Usage** | Higher (StatefulSet + PVCs) | Lower (no K8s DB) |
| **Matches Production** | ❌ No | ✅ Yes (RDS + EKS) |
| **Data Persistence** | K8s PVC | Docker volume |
| **Rebuild Backend** | Complex | Simple (see below) |

## Detailed Usage

### k8s-start-external.sh

Starts the backend with external PostgreSQL.

**What it does:**
1. Checks if Minikube is running (starts if needed)
2. Starts PostgreSQL in Docker using `docker-compose-db.yml`
3. Builds backend Docker image in Minikube's Docker daemon
4. Deploys backend to Kubernetes using `values-minikube.yaml`
5. Waits for pods to be ready
6. Tests the backend health endpoint
7. Displays access URLs and useful commands

**Usage:**
```bash
./k8s-start-external.sh
```

**Access:**
- Backend: `http://$(minikube ip):30080`
- Health: `http://$(minikube ip):30080/actuator/health`
- PostgreSQL: `localhost:5432`

### k8s-stop-external.sh

Stops backend services with multiple options.

**Options:**
1. **Stop backend only** - Scales backend to 0 replicas, keeps PostgreSQL running
2. **Stop both** - Stops backend and PostgreSQL, keeps data
3. **Full cleanup** - Removes everything including data

**Usage:**
```bash
./k8s-stop-external.sh
# Then choose option 1, 2, or 3
```

### k8s-status-external.sh

Shows comprehensive status of all services.

**What it checks:**
- Minikube status
- PostgreSQL container status and health
- Helm release status
- Kubernetes pods and services
- Backend health endpoint
- Database connectivity
- Network connectivity (backend -> PostgreSQL)

**Usage:**
```bash
./k8s-status-external.sh
```

## Common Workflows

### First Time Setup

```bash
# 1. Start everything
./k8s-start-external.sh

# 2. Verify everything is running
./k8s-status-external.sh

# 3. Test the API
curl http://$(minikube ip):30080/actuator/health
```

### Daily Development

```bash
# Start services (if not running)
./k8s-start-external.sh

# Make code changes...

# Rebuild and redeploy
eval $(minikube docker-env)
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate

# Watch the rollout
kubectl rollout status deployment/real-estate-backend-backend -n real-estate

# Check logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate
```

### Debugging Database Issues

```bash
# Check PostgreSQL logs
docker logs -f real-estate-postgres-local

# Connect to PostgreSQL
psql -h localhost -p 5432 -U postgres -d realestatedb

# Test connectivity from backend pod
kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- sh
# Inside pod:
ping host.minikube.internal
nc -zv host.minikube.internal 5432
env | grep DB_
```

### Clean Slate

```bash
# Full cleanup (removes all data)
./k8s-stop-external.sh
# Choose option 3

# Start fresh
./k8s-start-external.sh
```

### Switching Between Modes

**From Bundled to External:**
```bash
# Stop bundled mode
helm uninstall real-estate -n real-estate-dev
kubectl delete namespace real-estate-dev

# Start external mode
./k8s-start-external.sh
```

**From External to Bundled:**
```bash
# Stop external mode
./k8s-stop-external.sh
# Choose option 3 (full cleanup)

# Start bundled mode
./k8s-start.sh
```

## Environment Variables

### External Mode (values-minikube.yaml)

```yaml
backend:
  env:
    dbHost: host.minikube.internal  # Special DNS for Minikube
    dbPort: 5432
    dbName: realestatedb
    dbUser: postgres
    dbPassword: postgres
```

### Bundled Mode (values-dev.yaml)

```yaml
backend:
  env:
    # DB connection is auto-configured via service name
postgresql:
  enabled: true  # PostgreSQL runs in K8s
```

## Troubleshooting

### Backend can't connect to PostgreSQL

**Symptom:** Backend logs show "Connection refused" or timeout

**Solutions:**
1. Check PostgreSQL is running:
   ```bash
   docker ps | grep postgres
   docker logs real-estate-postgres-local
   ```

2. Test from host:
   ```bash
   psql -h localhost -p 5432 -U postgres -d realestatedb
   ```

3. Test from backend pod:
   ```bash
   kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- sh
   ping host.minikube.internal
   nc -zv host.minikube.internal 5432
   ```

4. Check environment variables:
   ```bash
   kubectl exec deployment/real-estate-backend-backend -n real-estate -- env | grep DB_
   ```

### Image not found in Minikube

**Symptom:** `ImagePullBackOff` or `ErrImagePull`

**Solution:**
```bash
# Make sure to build in Minikube's Docker
eval $(minikube docker-env)
docker build -t real-estate-backend:latest .

# Verify image exists
docker images | grep real-estate-backend

# Restart deployment
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate
```

### Port 5432 already in use

**Symptom:** Docker PostgreSQL fails to start

**Solutions:**
```bash
# Check what's using the port
lsof -i :5432

# Option 1: Stop local PostgreSQL
brew services stop postgresql

# Option 2: Change Docker port
# Edit docker-compose-db.yml:
ports:
  - "5433:5432"  # Use different host port
```

### Minikube IP changed

**Symptom:** Can't access backend after restart

**Solution:**
```bash
# Get new IP
minikube ip

# Access at new URL
curl http://$(minikube ip):30080/actuator/health
```

## Performance Tips

1. **Allocate enough resources to Minikube:**
   ```bash
   minikube start --memory=3500 --cpus=2
   ```

2. **Speed up rebuilds with Maven wrapper:**
   ```bash
   ./mvnw clean package -DskipTests -T 1C  # Parallel build
   ```

3. **Use Docker layer caching:**
   ```bash
   # Build only changed layers
   docker build -t real-estate-backend:latest .
   ```

4. **Keep PostgreSQL running between sessions:**
   ```bash
   # Use option 1 when stopping (backend only)
   ./k8s-stop-external.sh
   ```

## File Structure

```
backend/
├── k8s-start.sh              # Original bundled mode start
├── k8s-stop.sh               # Original bundled mode stop
├── k8s-status.sh             # Original bundled mode status
├── k8s-start-external.sh     # New external PostgreSQL start ⭐
├── k8s-stop-external.sh      # New external PostgreSQL stop ⭐
├── k8s-status-external.sh    # New external PostgreSQL status ⭐
├── docker-compose.yml        # Original bundled PostgreSQL
├── docker-compose-db.yml     # External PostgreSQL only ⭐
└── helm/real-estate-backend/
    ├── values.yaml           # Default (bundled)
    ├── values-dev.yaml       # Bundled mode config
    ├── values-minikube.yaml  # External mode config ⭐
    └── values-local.yaml     # External mode (Kind/K3d)
```

⭐ = New files for external PostgreSQL approach

## Related Documentation

- [Minikube Setup Guide](./MINIKUBE_SETUP.md)
- [Local K8s Setup Guide](./LOCAL_K8S_SETUP.md)
- [Helm Chart README](./helm/real-estate-backend/README.md)

## Support

For issues or questions:
- Check status: `./k8s-status-external.sh`
- View logs: `kubectl logs -f deployment/real-estate-backend-backend -n real-estate`
- Refer to troubleshooting section above
