# Plain Kubernetes Architecture

## 🏗️ Architecture Overview

**IMPORTANT:** PostgreSQL runs **INSIDE** the Kubernetes cluster, not as a separate Docker container!

```
┌────────────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                              │
│                    Namespace: real-estate                          │
│                                                                    │
│  ┌────────────────────────────┐   ┌──────────────────────────┐   │
│  │  PostgreSQL Database       │   │  Backend Application     │   │
│  │  ─────────────────────     │   │  ──────────────────      │   │
│  │                            │   │                          │   │
│  │  Type: StatefulSet         │◄──┤  Type: Deployment        │   │
│  │  Image: postgres:18-alpine │   │  Image: real-estate:latest│  │
│  │  Replicas: 1               │   │  Replicas: 1             │   │
│  │                            │   │                          │   │
│  │  Service:                  │   │  Service:                │   │
│  │    Name: real-estate-      │   │    Name: real-estate-    │   │
│  │          backend-postgres  │   │          backend-backend │   │
│  │    Type: ClusterIP         │   │    Type: NodePort        │   │
│  │    Port: 5432              │   │    Port: 8080            │   │
│  │    (Internal Only)         │   │    NodePort: 30080       │   │
│  │                            │   │                          │   │
│  │  Storage:                  │   │  Storage:                │   │
│  │  ┌──────────────────────┐ │   │  ┌────────────────────┐  │   │
│  │  │ PVC: postgres-pvc    │ │   │  │ PVC: backend-pvc   │  │   │
│  │  │ Size: 5Gi            │ │   │  │ Size: 5Gi          │  │   │
│  │  │ Path: /pgdata        │ │   │  │ Path: /app/uploads │  │   │
│  │  └──────────────────────┘ │   │  └────────────────────┘  │   │
│  │                            │   │                          │   │
│  │  ConfigMap & Secret:       │   │  ConfigMap & Secret:     │   │
│  │  - POSTGRES_DB             │   │  - DB_HOST=real-estate-  │   │
│  │  - POSTGRES_USER           │   │        backend-postgres  │   │
│  │  - POSTGRES_PASSWORD       │   │  - DB_PORT=5432          │   │
│  │                            │   │  - SPRING_PROFILES_ACTIVE│   │
│  └────────────────────────────┘   │  - CORS_ALLOWED_ORIGINS  │   │
│                                    └──────────────────────────┘   │
│                                                │                   │
└────────────────────────────────────────────────┼───────────────────┘
                                                 │
                                                 ▼
                                         http://localhost:30080
                                         (Accessible from host)
```

## 🔍 Component Details

### 1. PostgreSQL StatefulSet

**File:** `base/postgres-statefulset.yaml`

```yaml
Kind: StatefulSet
Name: real-estate-backend-postgres
Namespace: real-estate
Replicas: 1
Image: postgres:18-alpine
```

**Why StatefulSet?**
- Stable network identity
- Persistent storage
- Ordered deployment and scaling
- Perfect for databases

**Network Access:**
- Service Type: `ClusterIP` (internal only)
- DNS Name: `real-estate-backend-postgres.real-estate.svc.cluster.local`
- Short Name: `real-estate-backend-postgres` (within same namespace)
- Port: `5432`

**NOT accessible from outside Kubernetes by default!**

### 2. Backend Deployment

**File:** `base/backend-deployment.yaml`

```yaml
Kind: Deployment
Name: real-estate-backend-backend
Namespace: real-estate
Replicas: 1
Image: real-estate-backend:latest
```

**Why Deployment?**
- Stateless application
- Easy scaling
- Rolling updates
- Self-healing

**Network Access:**
- Service Type: `NodePort` (external access)
- Port: `8080` (container)
- NodePort: `30080` (host machine)

**Accessible from outside Kubernetes on port 30080!**

### 3. Internal Communication

```
Backend Pod → DNS Lookup → real-estate-backend-postgres
                         ↓
                    ClusterIP Service
                         ↓
                  PostgreSQL Pod (5432)
```

The backend connects to PostgreSQL using the service name:
```yaml
DB_HOST: real-estate-backend-postgres
DB_PORT: 5432
```

Kubernetes DNS automatically resolves this to the PostgreSQL pod's IP address.

## 🧪 Verify the Architecture

Run the verification script:

```bash
cd backend/k8s-plain
./verify-architecture.sh
```

This will show:
1. ✅ PostgreSQL running as a Pod in K8s
2. ✅ Backend running as a Pod in K8s
3. ✅ Both in the same namespace
4. ✅ Services connecting them
5. ✅ No separate Docker containers

## 🚫 What's NOT in the Architecture

**NO separate Docker containers outside Kubernetes:**
- ❌ PostgreSQL does NOT run as `docker run postgres`
- ❌ Backend does NOT run as `docker run backend`
- ❌ No `docker-compose.yml` in use
- ❌ No containers managed by Docker directly

**Everything runs inside Kubernetes!**

## 🔌 Accessing PostgreSQL from Outside K8s

Since PostgreSQL uses ClusterIP (internal only), to access it from your host machine:

```bash
# Method 1: Port forwarding
kubectl port-forward service/real-estate-backend-postgres 5432:5432 -n real-estate

# Now connect from your machine
psql -h localhost -p 5432 -U postgres -d realestatedb
```

```bash
# Method 2: Exec into backend pod
BACKEND_POD=$(kubectl get pods -n real-estate -l component=backend -o jsonpath='{.items[0].metadata.name}')
kubectl exec -it $BACKEND_POD -n real-estate -- bash

# Inside the backend pod, you can connect to PostgreSQL
psql -h real-estate-backend-postgres -p 5432 -U postgres -d realestatedb
```

## 📊 Resource Hierarchy

```
Namespace: real-estate
├── ConfigMap: real-estate-backend-config
├── Secret: real-estate-backend-secret
├── PersistentVolumeClaim: real-estate-backend-postgres-pvc
├── PersistentVolumeClaim: real-estate-backend-backend-pvc
├── StatefulSet: real-estate-backend-postgres
│   └── Pod: real-estate-backend-postgres-0
│       └── Container: postgres
├── Service: real-estate-backend-postgres (ClusterIP)
├── Deployment: real-estate-backend-backend
│   └── Pod: real-estate-backend-backend-xxx
│       └── Container: backend
└── Service: real-estate-backend-backend (NodePort)
```

## 🔄 Lifecycle

### Startup Sequence

1. **Namespace** is created first
2. **ConfigMap & Secret** are created (configuration data)
3. **PVCs** are created (storage claims)
4. **PostgreSQL StatefulSet** starts
   - Creates Pod `real-estate-backend-postgres-0`
   - Mounts PVC to `/pgdata`
   - Waits for readiness probe (pg_isready)
5. **PostgreSQL Service** exposes the StatefulSet
6. **Backend Deployment** starts
   - Creates Pod `real-estate-backend-backend-xxx`
   - Connects to PostgreSQL using service DNS
   - Waits for health check (`/actuator/health`)
7. **Backend Service** exposes the Deployment on NodePort 30080

### Health Checks

**PostgreSQL:**
```yaml
livenessProbe:
  exec:
    command: ["/bin/sh", "-c", "pg_isready -U postgres"]
  initialDelaySeconds: 30

readinessProbe:
  exec:
    command: ["/bin/sh", "-c", "pg_isready -U postgres"]
  initialDelaySeconds: 5
```

**Backend:**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 90

readinessProbe:
  httpGet:
    path: /actuator/health
    port: 8080
  initialDelaySeconds: 60
```

## 🎯 Key Differences from Docker Compose

| Aspect | Kubernetes (This Setup) | Docker Compose |
|--------|------------------------|----------------|
| PostgreSQL Location | Inside K8s cluster | Separate Docker container |
| Orchestration | Kubernetes | Docker Engine |
| Networking | K8s Services & DNS | Docker networks |
| Storage | PersistentVolumeClaims | Docker volumes |
| Scaling | K8s controllers | Manual |
| Health Checks | Liveness/Readiness probes | Healthchecks |
| Service Discovery | K8s DNS | Docker DNS |
| External Access | NodePort/LoadBalancer | Port mapping |

## 💡 Why PostgreSQL Inside K8s?

**Advantages:**
1. ✅ **Unified Management**: Everything in one place
2. ✅ **K8s Features**: Self-healing, health checks, resource limits
3. ✅ **Service Discovery**: Automatic DNS resolution
4. ✅ **Persistent Storage**: PVC management
5. ✅ **Network Policies**: Can apply K8s network policies
6. ✅ **Scalability**: Can scale with K8s (StatefulSet features)
7. ✅ **Production-like**: Same architecture in dev and prod

**Considerations:**
- ⚠️ Not directly accessible from host (need port-forward)
- ⚠️ Requires K8s knowledge to debug
- ⚠️ More complex than standalone Docker container

## 📝 Summary

**Current Architecture:**
- ✅ PostgreSQL: **StatefulSet in Kubernetes**
- ✅ Backend: **Deployment in Kubernetes**
- ✅ Both: **Same namespace, same cluster**
- ✅ Connection: **Kubernetes service DNS**
- ✅ Storage: **PersistentVolumeClaims**
- ✅ No separate Docker containers

This is a **production-ready architecture** that runs entirely within Kubernetes!
