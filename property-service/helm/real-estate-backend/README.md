# Real Estate Backend Helm Chart

A Helm chart for deploying the Real Estate Backend application with optional PostgreSQL database.

## Overview

This chart can deploy the backend in two modes:

1. **Bundled Mode** (Default): PostgreSQL runs inside Kubernetes alongside the backend
2. **External Mode**: Backend connects to external PostgreSQL (Docker, RDS, Cloud SQL, etc.)

## Deployment Modes

### Mode 1: Bundled PostgreSQL (Default)

Use `values.yaml` or `values-dev.yaml` - PostgreSQL runs as a StatefulSet in K8s.

```bash
helm install real-estate . -f values.yaml
```

**When to use:**
- Testing the full stack in K8s
- Development environments where you want everything in K8s
- Learning/demo purposes

### Mode 2: External PostgreSQL (Recommended for Local Dev)

Use `values-local.yaml` or `values-prod.yaml` - Backend connects to external database.

```bash
helm install real-estate . -f values-local.yaml
```

**When to use:**
- **Local development** (PostgreSQL in Docker) - **Matches production architecture**
- Production (AWS RDS, Google Cloud SQL, Azure Database)
- Staging environments with managed databases
- Dev/prod parity

## Configuration

### Key Values

| Parameter | Description | Default | values-local.yaml |
|-----------|-------------|---------|-------------------|
| `postgresql.enabled` | Deploy PostgreSQL in K8s | `true` | `false` |
| `backend.env.dbHost` | Database hostname | `postgres` | `host.docker.internal` |
| `backend.env.dbPort` | Database port | `5432` | `5432` |
| `backend.env.dbName` | Database name | `realestatedb` | `realestatedb` |
| `backend.env.dbUser` | Database username | `postgres` | `postgres` |
| `backend.env.dbPassword` | Database password | `postgres` | `postgres` |

### Values Files

- **values.yaml**: Default - bundled PostgreSQL, suitable for K8s testing
- **values-dev.yaml**: Development - bundled PostgreSQL with dev settings (Minikube)
- **values-local.yaml**: Local development - external Docker PostgreSQL for Kind/K3d/Docker Desktop
- **values-minikube.yaml**: Local development - external Docker PostgreSQL for Minikube
- **values-prod.yaml**: Production - external managed database (RDS)

## Prerequisites

- Kubernetes cluster (Kind, K3d, Minikube, Docker Desktop, EKS, etc.)
- kubectl configured
- Helm 3.x
- Backend Docker image built
- (For external mode) PostgreSQL running externally

## Installation

### Option 1: Local Development with External PostgreSQL (Recommended)

This setup matches production architecture (EKS + RDS).

#### Using Kind

```bash
# 1. Create Kind cluster with port mapping
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

# 2. Start PostgreSQL in Docker
cd backend
docker-compose -f docker-compose-db.yml up -d

# 3. Build and load backend image
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .
kind load docker-image real-estate-backend:latest --name real-estate

# 4. Deploy backend to K8s
helm install real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-local.yaml

# 5. Verify
kubectl get pods -n real-estate
curl http://localhost:30080/actuator/health
```

#### Using K3d

```bash
# 1. Create K3d cluster
k3d cluster create real-estate --port "30080:30080@loadbalancer"

# 2. Start PostgreSQL in Docker
cd backend
docker-compose -f docker-compose-db.yml up -d

# 3. Build and import backend image
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .
k3d image import real-estate-backend:latest -c real-estate

# 4. Update values-local.yaml for K3d
# Change dbHost to: host.k3d.internal

# 5. Deploy
helm install real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-local.yaml
```

#### Using Docker Desktop Kubernetes

```bash
# 1. Enable Kubernetes in Docker Desktop Settings

# 2. Start PostgreSQL in Docker
cd backend
docker-compose -f docker-compose-db.yml up -d

# 3. Build backend image
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .

# 4. Deploy
helm install real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-local.yaml
```

### Option 2: Minikube with External PostgreSQL (Recommended)

```bash
# 1. Start Minikube
minikube start --memory=3500 --cpus=2

# 2. Start PostgreSQL in Docker
cd backend
docker-compose -f docker-compose-db.yml up -d

# 3. Build backend image in Minikube's Docker
eval $(minikube docker-env)
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .

# 4. Deploy backend to K8s
helm install real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-minikube.yaml

# 5. Access application
minikube service real-estate-backend-backend -n real-estate
# Or use NodePort
curl http://$(minikube ip):30080/actuator/health
```

### Option 3: Bundled PostgreSQL in K8s (Old Approach)

```bash
# Start Minikube
minikube start --memory=3500 --cpus=2

# Set Docker environment
eval $(minikube docker-env)

# Build image
cd backend
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .

# Install chart with bundled PostgreSQL
helm install real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-dev.yaml

# Access application
minikube service real-estate-real-estate-backend-backend -n real-estate-dev
```

## Network Configuration for External Database

### Platform-Specific Database Hosts

When using external PostgreSQL (`postgresql.enabled: false`), set the correct `dbHost`:

| Environment | dbHost Value | Notes |
|-------------|--------------|-------|
| **Mac/Windows + Docker** | `host.docker.internal` | Default in values-local.yaml |
| **Linux + Kind** | `172.17.0.1` | Docker bridge IP |
| **K3d (All platforms)** | `host.k3d.internal` | K3d DNS |
| **Minikube** | `host.minikube.internal` | Default in values-minikube.yaml |
| **AWS EKS** | RDS endpoint | e.g., `mydb.123.us-east-1.rds.amazonaws.com` |
| **GCP GKE** | Cloud SQL IP | Private or public IP |

### Find Docker Bridge IP (Linux)

```bash
docker network inspect bridge | grep Gateway
# Use the Gateway IP as dbHost
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
# OR for Minikube
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

# PostgreSQL logs (if bundled)
kubectl logs -f real-estate-real-estate-backend-postgres-0 -n real-estate

# PostgreSQL logs (if Docker)
docker logs -f real-estate-postgres-local
```

### Access PostgreSQL

```bash
# Docker PostgreSQL
docker exec -it real-estate-postgres-local psql -U postgres -d realestatedb

# K8s PostgreSQL (bundled mode)
kubectl exec -it real-estate-real-estate-backend-postgres-0 -n real-estate -- \
  psql -U postgres -d realestatedb

# Using psql client
psql -h localhost -p 5432 -U postgres -d realestatedb
```

### Upgrade Configuration

```bash
# Edit values file, then upgrade
helm upgrade real-estate ./helm/real-estate-backend -f ./helm/real-estate-backend/values-local.yaml

# Or pass values via command line
helm upgrade real-estate ./helm/real-estate-backend --set backend.replicaCount=2
```

### Clean Up

```bash
# Uninstall Helm release
helm uninstall real-estate -n real-estate

# Delete namespace
kubectl delete namespace real-estate

# Stop Docker PostgreSQL
docker-compose -f backend/docker-compose-db.yml down

# Delete Kind cluster
kind delete cluster --name real-estate

# OR Delete K3d cluster
k3d cluster delete real-estate
```

## Accessing the Application

### Local Development (NodePort)

```bash
# Access backend
curl http://localhost:30080/actuator/health

# View all endpoints
curl http://localhost:30080/actuator
```

### Port Forwarding

```bash
kubectl port-forward -n real-estate svc/real-estate-backend-backend 8080:8080
curl http://localhost:8080/actuator/health
```

### Minikube Service

```bash
minikube service real-estate-real-estate-backend-backend -n real-estate-dev
```

## Troubleshooting

### Backend Can't Connect to Database

1. **Check PostgreSQL is running:**
   ```bash
   # Docker
   docker ps | grep postgres

   # K8s
   kubectl get pods -n real-estate -l app.kubernetes.io/component=database
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
   ```

### Image Pull Errors

```bash
# Verify image exists
docker images | grep real-estate-backend

# For Kind, load image
kind load docker-image real-estate-backend:latest --name real-estate

# For K3d, import image
k3d image import real-estate-backend:latest -c real-estate

# For Minikube, use Minikube's Docker
eval $(minikube docker-env)
docker build -t real-estate-backend:latest .
```

### Port Already in Use

```bash
# Find process using port
lsof -i :30080
lsof -i :5432

# Kill the process or change ports in values file
```

## Architecture

### Local Development (External PostgreSQL)

```
┌─────────────────┐
│  Docker Desktop │
│   (PostgreSQL)  │
└─────────────────┘
        ▲
        │ External DB
        │ host.docker.internal:5432
        │
┌───────────────────────────────┐
│    Kind/K3d Cluster           │
│                               │
│  ┌─────────────────────────┐ │
│  │  Backend Deployment     │ │
│  │  - Spring Boot          │ │
│  │  - Port: 8080           │ │
│  │  - NodePort: 30080      │ │
│  └─────────────────────────┘ │
└───────────────────────────────┘
```

### Bundled Mode

```
┌─────────────────────────────────┐
│   Kubernetes Cluster            │
│                                 │
│  ┌──────────────────────────┐  │
│  │  Backend Deployment      │  │
│  └──────────────────────────┘  │
│             ▲                   │
│             │                   │
│  ┌──────────────────────────┐  │
│  │  PostgreSQL StatefulSet  │  │
│  └──────────────────────────┘  │
└─────────────────────────────────┘
```

### Production

```
┌─────────────────┐
│    AWS RDS      │
│  (PostgreSQL)   │
└─────────────────┘
        ▲
        │ Private VPC
        │
┌───────────────────────────────┐
│         AWS EKS               │
│                               │
│  ┌─────────────────────────┐ │
│  │  Backend Deployment     │ │
│  │  - Multiple replicas    │ │
│  │  - Auto-scaling         │ │
│  │  - Load Balancer        │ │
│  └─────────────────────────┘ │
└───────────────────────────────┘
```

## Chart Structure

```
real-estate-backend/
├── Chart.yaml                    # Chart metadata
├── values.yaml                   # Default values (bundled PostgreSQL)
├── values-dev.yaml              # Development values
├── values-local.yaml            # Local development (external DB) ⭐
├── values-prod.yaml             # Production values (external DB)
├── templates/
│   ├── _helpers.tpl             # Template helpers
│   ├── namespace.yaml           # Namespace definition
│   ├── configmap.yaml           # ConfigMap (conditional DB config) ⭐
│   ├── secret.yaml              # Secret (conditional credentials) ⭐
│   ├── backend-deployment.yaml  # Backend Deployment
│   ├── backend-service.yaml     # Backend Service
│   ├── backend-pvc.yaml         # Backend PVC for uploads
│   ├── postgres-statefulset.yaml # PostgreSQL (conditional)
│   ├── postgres-service.yaml    # PostgreSQL Service (conditional)
│   └── postgres-pvc.yaml        # PostgreSQL PVC (conditional)
└── README.md                    # This file
```

⭐ = Modified to support both bundled and external database modes

## Resources

- [Local Development Setup Guide](../../LOCAL_K8S_SETUP.md) - Detailed guide for local K8s setup
- [Helm Documentation](https://helm.sh/docs/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Kind Documentation](https://kind.sigs.k8s.io/)
- [K3d Documentation](https://k3d.io/)

## Support

For issues and questions, please refer to the main project README or open an issue in the repository.
