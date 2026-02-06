# Local Kubernetes Setup Guide

This guide explains how to run the Real Estate application locally with PostgreSQL in Docker and the backend in Kubernetes, matching the production architecture (RDS + EKS).

## Architecture

```
Production:                    Local Development:
┌─────────────┐               ┌─────────────────┐
│     RDS     │               │  Docker Desktop │
│ (PostgreSQL)│               │   (PostgreSQL)  │
└─────────────┘               └─────────────────┘
      ▲                             ▲
      │ External DB                 │ External DB
      │                             │
┌─────────────┐               ┌─────────────┐
│     EKS     │               │  Kind/K3d   │
│  (Backend)  │               │  (Backend)  │
└─────────────┘               └─────────────┘
```

## Prerequisites

- Docker Desktop (with Kubernetes enabled) OR Kind/K3d
- kubectl
- Helm 3
- Java 17+ and Maven (for building the app)

## Option 1: Using Kind

### 1. Install Kind

```bash
brew install kind
```

### 2. Create Kind Cluster

```bash
# Create cluster with port mapping for NodePort
cat <<EOF | kind create cluster --name real-estate --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 30080
        hostPort: 30080
        protocol: TCP
EOF
```

### 3. Start PostgreSQL in Docker

```bash
cd backend
docker-compose -f docker-compose-db.yml up -d

# Verify it's running
docker ps | grep postgres
docker exec -it real-estate-postgres-local psql -U postgres -d realestatedb -c "SELECT version();"
```

### 4. Build Backend Image

```bash
cd backend
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .
```

### 5. Load Image into Kind

```bash
kind load docker-image real-estate-backend:latest --name real-estate
```

### 6. Deploy Backend to Kubernetes

```bash
cd backend
helm install real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-local.yaml
```

### 7. Verify Deployment

```bash
# Check pods
kubectl get pods -n real-estate

# Check services
kubectl get svc -n real-estate

# Check logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate

# Test health endpoint
curl http://localhost:30080/actuator/health
```

## Option 2: Using K3d

### 1. Install K3d

```bash
brew install k3d
```

### 2. Create K3d Cluster

```bash
k3d cluster create real-estate \
  --port "30080:30080@loadbalancer" \
  --api-port 6550
```

### 3. Start PostgreSQL in Docker

```bash
cd backend
docker-compose -f docker-compose-db.yml up -d
```

### 4. Build and Import Backend Image

```bash
cd backend
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .

# Import to k3d
k3d image import real-estate-backend:latest -c real-estate
```

### 5. Update values-local.yaml for K3d

```bash
# Edit backend/helm/real-estate-backend/values-local.yaml
# Change dbHost to: host.k3d.internal
```

### 6. Deploy Backend to Kubernetes

```bash
cd backend
helm install real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-local.yaml
```

## Option 3: Using Docker Desktop Kubernetes

### 1. Enable Kubernetes in Docker Desktop

- Open Docker Desktop Settings
- Go to Kubernetes tab
- Check "Enable Kubernetes"
- Click "Apply & Restart"

### 2. Start PostgreSQL in Docker

```bash
cd backend
docker-compose -f docker-compose-db.yml up -d
```

### 3. Build Backend Image

```bash
cd backend
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .
```

### 4. Deploy Backend to Kubernetes

```bash
cd backend
helm install real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-local.yaml
```

## Option 4: Using Minikube

### 1. Install Minikube

```bash
brew install minikube
```

### 2. Start Minikube

```bash
minikube start --memory=3500 --cpus=2
```

### 3. Start PostgreSQL in Docker

```bash
cd backend
docker-compose -f docker-compose-db.yml up -d

# Verify it's running
docker ps | grep postgres
```

### 4. Build Backend Image in Minikube

```bash
# Point Docker CLI to Minikube's Docker daemon
eval $(minikube docker-env)

cd backend
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .
```

### 5. Deploy Backend to Kubernetes

```bash
cd backend
helm install real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-minikube.yaml
```

### 6. Verify Deployment

```bash
# Check pods
kubectl get pods -n real-estate

# Check services
kubectl get svc -n real-estate

# Check logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate

# Test health endpoint
curl http://$(minikube ip):30080/actuator/health
```

### 7. Access the Application

```bash
# Option 1: NodePort
export NODE_IP=$(minikube ip)
echo "Application URL: http://$NODE_IP:30080"

# Option 2: Minikube service (auto-opens browser)
minikube service real-estate-backend-backend -n real-estate

# Option 3: Port forwarding
kubectl port-forward -n real-estate svc/real-estate-backend-backend 8080:8080
curl http://localhost:8080/actuator/health
```

## Network Configuration Notes

### Database Host Configuration

The `dbHost` value in `values-local.yaml` needs to be set based on your platform:

| Platform | dbHost Value | Notes |
|----------|--------------|-------|
| **Mac (Kind/Docker Desktop)** | `host.docker.internal` | Default in values-local.yaml |
| **Windows (Kind/Docker Desktop)** | `host.docker.internal` | Default in values-local.yaml |
| **Linux (Kind)** | `172.17.0.1` | Docker bridge IP |
| **K3d (All platforms)** | `host.k3d.internal` | K3d-specific |
| **Minikube (Mac/Linux)** | `host.minikube.internal` | Default in values-minikube.yaml |
| **Minikube (Alternative)** | Minikube IP | Get via `minikube ssh "route -n \| grep ^0.0.0.0 \| awk '{ print \\$2 }'"` |

### For Linux Users with Kind

Edit `values-local.yaml` and change:

```yaml
backend:
  env:
    dbHost: 172.17.0.1  # Instead of host.docker.internal
```

Or find your Docker bridge IP:

```bash
docker network inspect bridge | grep Gateway
```

## Common Operations

### Rebuild and Redeploy Backend

```bash
# Build new image
cd backend
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .

# Load to Kind
kind load docker-image real-estate-backend:latest --name real-estate
# OR for K3d
k3d image import real-estate-backend:latest -c real-estate
# OR for Minikube (build in Minikube's Docker)
eval $(minikube docker-env) && docker build -t real-estate-backend:latest .

# Upgrade Helm release (use appropriate values file)
helm upgrade real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-local.yaml -n real-estate
# OR for Minikube
helm upgrade real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-minikube.yaml -n real-estate

# Force pod restart
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate
```

### View Logs

```bash
# Backend logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate

# PostgreSQL logs
docker logs -f real-estate-postgres-local
```

### Access PostgreSQL

```bash
# Using Docker exec
docker exec -it real-estate-postgres-local psql -U postgres -d realestatedb

# Using psql client
psql -h localhost -p 5432 -U postgres -d realestatedb
```

### Clean Up

```bash
# Uninstall Helm release
helm uninstall real-estate -n real-estate

# Delete namespace
kubectl delete namespace real-estate

# Stop PostgreSQL
docker-compose -f backend/docker-compose-db.yml down

# Delete Kind cluster
kind delete cluster --name real-estate

# OR Delete K3d cluster
k3d cluster delete real-estate

# OR Stop/Delete Minikube
minikube stop
minikube delete
```

## Troubleshooting

### Backend Can't Connect to PostgreSQL

1. **Check PostgreSQL is running:**
   ```bash
   docker ps | grep postgres
   ```

2. **Verify network connectivity from backend pod:**
   ```bash
   kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- sh
   # Inside pod:
   ping host.docker.internal
   nc -zv host.docker.internal 5432
   ```

3. **Check backend environment variables:**
   ```bash
   kubectl exec deployment/real-estate-backend-backend -n real-estate -- env | grep DB_
   ```

4. **For Linux, verify correct host:**
   ```bash
   # Find Docker bridge IP
   docker network inspect bridge | grep Gateway
   # Update values-local.yaml with this IP
   ```

### Backend Pod CrashLooping

1. **Check logs:**
   ```bash
   kubectl logs deployment/real-estate-backend-backend -n real-estate
   ```

2. **Check events:**
   ```bash
   kubectl get events -n real-estate --sort-by='.lastTimestamp'
   ```

3. **Verify image exists in cluster:**
   ```bash
   # For Kind
   docker exec -it real-estate-control-plane crictl images | grep real-estate

   # For K3d
   k3d image list -c real-estate

   # For Minikube
   eval $(minikube docker-env) && docker images | grep real-estate
   ```

### Port Already in Use

If port 30080 or 5432 is already in use:

```bash
# Find process using port
lsof -i :30080
lsof -i :5432

# Kill the process or change ports in values-local.yaml
```

## Differences from Production

| Component | Local | Production |
|-----------|-------|------------|
| Database | Docker (postgres:18-alpine) | AWS RDS (PostgreSQL) |
| Kubernetes | Kind/K3d/Minikube | AWS EKS |
| Database Access | host.docker.internal:5432 or host.minikube.internal:5432 | RDS endpoint |
| TLS/SSL | Not required | Required |
| Storage | Local PV | EBS volumes |
| Load Balancer | NodePort | AWS ALB/NLB |

## Next Steps

- Set up production deployment with AWS EKS + RDS
- Configure CI/CD pipeline to build and deploy
- Add ingress controller for proper load balancing
- Configure SSL/TLS certificates
- Set up monitoring and logging
