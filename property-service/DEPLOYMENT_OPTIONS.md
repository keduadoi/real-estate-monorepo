# Backend Deployment Options

This project supports **3 different deployment approaches** for the backend and database:

## 📊 Deployment Options Overview

| Option | PostgreSQL Location | Backend Location | Use Case |
|--------|-------------------|------------------|----------|
| **1. Kubernetes (Plain)** | Inside K8s | Inside K8s | Learning K8s fundamentals |
| **2. Kubernetes (Helm)** | Inside K8s | Inside K8s | Production deployments |
| **3. Docker Compose** | Separate Docker | Separate Docker | Local development |

---

## Option 1: Plain Kubernetes ✅ (PostgreSQL Inside K8s)

**Location:** `backend/k8s-plain/`

### Architecture
```
┌─────────────────────────────────────────┐
│  Kubernetes Cluster                     │
│                                         │
│  ┌──────────────┐  ┌─────────────────┐ │
│  │ PostgreSQL   │  │ Backend App     │ │
│  │ StatefulSet  │◄─┤ Deployment      │ │
│  │ (ClusterIP)  │  │ (NodePort 30080)│ │
│  └──────────────┘  └─────────────────┘ │
└─────────────────────────────────────────┘
```

### Deploy
```bash
cd backend/k8s-plain
./deploy.sh local

# Verify architecture
./verify-architecture.sh
```

### Verify PostgreSQL is Inside K8s
```bash
# List all pods in cluster
kubectl get pods -n real-estate

# Should show:
# real-estate-backend-postgres-0   (PostgreSQL)
# real-estate-backend-backend-xxx  (Backend)
```

### Characteristics
- ✅ PostgreSQL runs as a StatefulSet inside K8s
- ✅ Backend runs as a Deployment inside K8s
- ✅ Both share the same K8s network
- ✅ PostgreSQL accessible only within cluster
- ✅ Backend accessible via NodePort 30080
- ✅ No Docker containers outside K8s

---

## Option 2: Helm Kubernetes ✅ (PostgreSQL Inside K8s)

**Location:** `backend/helm/real-estate-backend/`

### Architecture
```
┌─────────────────────────────────────────┐
│  Kubernetes Cluster (Helm Managed)     │
│                                         │
│  ┌──────────────┐  ┌─────────────────┐ │
│  │ PostgreSQL   │  │ Backend App     │ │
│  │ StatefulSet  │◄─┤ Deployment      │ │
│  │ (ClusterIP)  │  │ (NodePort 30080)│ │
│  └──────────────┘  └─────────────────┘ │
└─────────────────────────────────────────┘
```

### Deploy
```bash
cd backend/helm
helm upgrade --install real-estate ./real-estate-backend \
  --values ./real-estate-backend/values-local.yaml \
  --namespace real-estate \
  --create-namespace
```

### Verify
```bash
# List Helm releases
helm list -n real-estate

# List all resources
kubectl get all -n real-estate
```

### Characteristics
- ✅ PostgreSQL runs as a StatefulSet inside K8s
- ✅ Backend runs as a Deployment inside K8s
- ✅ Managed by Helm (templated, versioned)
- ✅ Easy rollback and upgrades
- ✅ Configuration via values.yaml
- ✅ No Docker containers outside K8s

---

## Option 3: Docker Compose (PostgreSQL Separate Docker)

**Location:** `backend/docker-compose-db.yml` (if you create one)

### Architecture
```
┌──────────────────────┐  ┌─────────────────────┐
│  Docker Container    │  │  Docker Container   │
│  ─────────────────   │  │  ──────────────     │
│  PostgreSQL          │  │  Backend App        │
│  postgres:18-alpine  │◄─┤  real-estate:latest │
│  Port: 5432          │  │  Port: 8080         │
└──────────────────────┘  └─────────────────────┘
        ▲                          ▲
        │                          │
        └──────── docker network ──┘
```

### Example docker-compose.yml
```yaml
version: '3.8'

services:
  postgres:
    image: postgres:18-alpine
    container_name: real-estate-postgres
    environment:
      POSTGRES_DB: realestatedb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - real-estate-network

  backend:
    image: real-estate-backend:latest
    container_name: real-estate-backend
    environment:
      SPRING_PROFILES_ACTIVE: dev
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: realestatedb
      DB_USER: postgres
      DB_PASSWORD: postgres
    ports:
      - "8080:8080"
    depends_on:
      - postgres
    networks:
      - real-estate-network
    volumes:
      - uploads:/app/uploads

volumes:
  postgres-data:
  uploads:

networks:
  real-estate-network:
    driver: bridge
```

### Deploy
```bash
# Build backend image
docker build -t real-estate-backend:latest -f backend/Dockerfile backend/

# Start services
docker-compose -f backend/docker-compose.yml up -d

# Verify
docker ps
```

### Characteristics
- ✅ PostgreSQL runs as a separate Docker container
- ✅ Backend runs as a separate Docker container
- ✅ Simple for local development
- ✅ No Kubernetes required
- ✅ Quick to start/stop
- ❌ Not suitable for production
- ❌ No orchestration features

---

## 🔍 Detailed Comparison

### PostgreSQL Location

| Deployment | PostgreSQL Runs As | Accessible From |
|------------|-------------------|-----------------|
| Plain K8s | K8s StatefulSet | Only within K8s cluster |
| Helm K8s | K8s StatefulSet | Only within K8s cluster |
| Docker Compose | Docker Container | Other Docker containers + host |

### Access Patterns

#### Plain K8s / Helm K8s
```bash
# PostgreSQL is NOT accessible from host machine
# Only accessible from within the K8s cluster

# To access PostgreSQL from your machine:
kubectl port-forward service/real-estate-backend-postgres 5432:5432 -n real-estate

# Then connect to localhost:5432
psql -h localhost -p 5432 -U postgres -d realestatedb
```

#### Docker Compose
```bash
# PostgreSQL IS accessible from host machine on port 5432
psql -h localhost -p 5432 -U postgres -d realestatedb

# No port-forwarding needed
```

### Networking

#### Plain K8s / Helm K8s
```yaml
# Backend connects to PostgreSQL using K8s service DNS
DB_HOST: real-estate-backend-postgres  # K8s service name
DB_PORT: 5432

# This works because both are in the same K8s namespace
# Kubernetes DNS resolves the service name to the pod IP
```

#### Docker Compose
```yaml
# Backend connects to PostgreSQL using Docker service name
DB_HOST: postgres  # Docker Compose service name
DB_PORT: 5432

# This works because both are in the same Docker network
# Docker DNS resolves the service name to the container IP
```

---

## 🎯 When to Use Each Option

### Use Plain Kubernetes When:
- 🎓 Learning Kubernetes fundamentals
- 🔍 Need to understand K8s resources deeply
- 📚 Want to see raw YAML without templating
- 🏫 Teaching/training purposes

**PostgreSQL:** ✅ Inside K8s (StatefulSet)

### Use Helm Kubernetes When:
- 🏭 Production deployments
- 🌍 Multiple environments (dev/staging/prod)
- 👥 Team collaboration
- 🔄 Need release management and rollbacks
- 📦 Want to distribute your application

**PostgreSQL:** ✅ Inside K8s (StatefulSet)

### Use Docker Compose When:
- 💻 Local development only
- ⚡ Quick testing
- 🐛 Debugging without K8s complexity
- 👨‍💻 Single developer environment
- ❌ NOT for production

**PostgreSQL:** Separate Docker container

---

## 🧪 Verify Your Deployment

### Check if PostgreSQL is Inside Kubernetes

```bash
# Deploy with K8s
cd backend/k8s-plain
./deploy.sh local

# Verify architecture
./verify-architecture.sh

# You should see PostgreSQL as a Pod in K8s:
kubectl get pods -n real-estate

# Expected output:
# NAME                                        READY   STATUS
# real-estate-backend-postgres-0              1/1     Running
# real-estate-backend-backend-xxx             1/1     Running
```

### Check if PostgreSQL is Separate Docker

```bash
# Deploy with Docker Compose
docker-compose -f docker-compose.yml up -d

# List Docker containers (not K8s pods):
docker ps

# Expected output:
# CONTAINER ID   IMAGE                    NAMES
# abc123...      postgres:18-alpine       real-estate-postgres
# def456...      real-estate-backend      real-estate-backend
```

---

## 💡 Key Takeaways

### Plain K8s & Helm (Current Implementation)
```
✅ PostgreSQL is INSIDE Kubernetes
✅ Runs as a StatefulSet
✅ ClusterIP service (internal only)
✅ Persistent storage via PVC
✅ Health checks configured
✅ Resource limits set
✅ Part of the same K8s deployment
```

### Docker Compose (Alternative)
```
✅ PostgreSQL is SEPARATE Docker container
✅ Not part of Kubernetes
✅ Direct port access (5432)
✅ Simpler for local dev
✅ Different deployment method
```

---

## 🚀 Quick Commands Reference

### Current Deployment (K8s with PostgreSQL Inside)

```bash
# Deploy
cd backend/k8s-plain
./deploy.sh local

# Verify both are in K8s
kubectl get all -n real-estate

# Check PostgreSQL
kubectl logs statefulset/real-estate-backend-postgres -n real-estate

# Access PostgreSQL from outside K8s
kubectl port-forward service/real-estate-backend-postgres 5432:5432 -n real-estate
psql -h localhost -p 5432 -U postgres -d realestatedb

# Cleanup
./cleanup.sh
```

### Alternative: Docker Compose (If You Want Separate Docker)

```bash
# Create docker-compose.yml (see example above)
# Build and run
docker-compose up -d

# Access PostgreSQL directly
psql -h localhost -p 5432 -U postgres -d realestatedb

# Cleanup
docker-compose down
```

---

## 📚 Summary

**Your current setup (both Plain K8s and Helm):**
- ✅ PostgreSQL runs **INSIDE** Kubernetes cluster
- ✅ Backend runs **INSIDE** Kubernetes cluster
- ✅ Both managed as Kubernetes resources
- ✅ No separate Docker containers outside K8s
- ✅ Production-ready architecture

**If you want separate Docker containers:**
- Create a `docker-compose.yml` file
- Use Docker Compose instead of Kubernetes
- Simpler but less production-ready

The plain Kubernetes deployment mirrors the Helm deployment architecture - both run PostgreSQL inside the cluster! 🎉
