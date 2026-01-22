# Kubernetes Migration Guide

This document describes the complete process of migrating the Real Estate Backend application from Docker Compose to Kubernetes with Helm for local development.

## Table of Contents

- [Overview](#overview)
- [Migration Process](#migration-process)
- [Prerequisites](#prerequisites)
- [Step-by-Step Migration](#step-by-step-migration)
- [Architecture Comparison](#architecture-comparison)
- [Configuration Management](#configuration-management)
- [Troubleshooting](#troubleshooting)
- [Rollback Instructions](#rollback-instructions)

## Overview

### Before: Docker Compose

The original setup used Docker Compose with:
- PostgreSQL 18-alpine container
- Spring Boot backend container
- Shared network
- Volume mounts for data persistence and uploads

### After: Kubernetes with Helm

The new setup uses Kubernetes with Helm providing:
- Declarative infrastructure as code
- Scalability and high availability
- Better resource management
- Environment-specific configurations
- Easy rollback and upgrades
- Production-ready deployment patterns

## Migration Process

### Phase 1: Environment Setup

**1. Install Required Tools**

```bash
# Install Helm
brew install helm

# Verify installation
helm version
# Output: version.BuildInfo{Version:"v4.1.0", ...}
```

**2. Start Minikube**

```bash
# Start minikube with appropriate resources
minikube start --memory=3500 --cpus=2

# Verify minikube is running
minikube status
# Output:
# minikube
# type: Control Plane
# host: Running
# kubelet: Running
# apiserver: Running
```

**3. Configure Docker Environment**

```bash
# Point Docker CLI to minikube's Docker daemon
eval $(minikube docker-env)

# Verify docker context
docker ps
```

### Phase 2: Helm Chart Creation

**1. Create Chart Structure**

```bash
mkdir -p backend/helm/real-estate-backend/templates
```

**2. Create Chart Metadata (Chart.yaml)**

Key decisions made:
- Chart API version: v2 (Helm 3+)
- Application version: 1.0.0
- Chart type: application

**3. Define Values (values.yaml)**

Configuration categories:
- Namespace settings
- Backend application config
- PostgreSQL database config
- Storage configurations
- Resource limits and requests
- Health check probes

**4. Create Environment-Specific Values (values-dev.yaml)**

Development overrides:
- Image pull policy: Never (use local images)
- Reduced resource requests
- Smaller storage sizes (2Gi instead of 5Gi)
- More lenient health check timings

### Phase 3: Kubernetes Resources

**1. Namespace Template**

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: real-estate-dev
```

Purpose: Isolate resources in development environment

**2. ConfigMap Template**

Stores non-sensitive configuration:
- Spring profiles
- Database connection details
- CORS settings
- Server port

**3. Secret Template**

Stores sensitive data (base64 encoded):
- Database credentials
- PostgreSQL user and password

**4. Persistent Volume Claims**

Two PVCs created:
- `backend-pvc`: 2Gi for file uploads
- `postgres-pvc`: 2Gi for database data

**5. PostgreSQL StatefulSet**

Key configurations:
- Image: postgres:18-alpine
- Volume mount: /pgdata (custom path to avoid Docker overlay issues)
- Health probes: pg_isready checks
- Resources: 256Mi memory, 250m CPU

**Important Fix Applied:**
- Changed PGDATA path from default `/var/lib/postgresql/data` to `/pgdata`
- This resolved Docker overlay filesystem mounting issues in minikube

**6. PostgreSQL Service**

- Type: ClusterIP (internal only)
- Port: 5432
- Used by backend for database connections

**7. Backend Deployment**

Key configurations:
- Image: real-estate-backend:latest
- Pull policy: Never (local image)
- Environment variables from ConfigMap and Secrets
- Volume mount: /app/uploads
- Health probes: /actuator/health endpoint
- Resources: 512Mi memory, 500m CPU

**8. Backend Service**

- Type: NodePort
- Internal port: 8080
- NodePort: 30080 (accessible from host)

**9. Helper Templates (_helpers.tpl)**

Reusable template functions:
- Chart name generation
- Selector labels
- Common labels
- Service name helpers

**10. Post-Install Notes (NOTES.txt)**

Displays after installation:
- How to check status
- How to access the application
- Common commands
- Database connection info

### Phase 4: Docker Image Build

**1. Build in Minikube Context**

```bash
# Ensure using minikube's Docker
eval $(minikube docker-env)

# Build the image
cd backend
docker build -t real-estate-backend:latest .
```

Build process:
- Multi-stage build (Maven build + JRE runtime)
- Total build time: ~2-3 minutes
- Final image size: 352MB

**2. Verify Image**

```bash
docker images | grep real-estate-backend
# Output: real-estate-backend:latest  bdd57225d019  352MB
```

### Phase 5: Deployment

**1. Install Helm Chart**

```bash
helm install real-estate ./backend/helm/real-estate-backend \
  -f ./backend/helm/real-estate-backend/values-dev.yaml
```

**2. Monitor Deployment**

```bash
# Watch pod status
kubectl get pods -n real-estate-dev -w

# Check logs
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=backend -f
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=database -f
```

**3. Verify Health**

```bash
# Port forward to access locally
kubectl port-forward -n real-estate-dev \
  svc/real-estate-real-estate-backend-backend 8080:8080

# Test health endpoint
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {"status": "UP"},
    "livenessState": {"status": "UP"},
    "ping": {"status": "UP"},
    "readinessState": {"status": "UP"}
  }
}
```

### Phase 6: Validation

**1. Database Migrations**

Flyway migrations executed successfully:
- V1__initial_schema.sql
- V2__add_image_constraints.sql

**2. Application Startup**

Verified:
- Spring Boot started in ~37 seconds
- All endpoints registered
- Database connection established
- File upload directory created

**3. Resource Status**

Final state:
```
NAME                                         READY   STATUS    RESTARTS   AGE
pod/real-estate-backend-backend-xxxxx        1/1     Running   0          5m
pod/real-estate-backend-postgres-0           1/1     Running   0          5m

NAME                                   TYPE        CLUSTER-IP    PORT(S)
service/real-estate-backend-backend    NodePort    10.97.x.x     8080:30080/TCP
service/real-estate-backend-postgres   ClusterIP   10.108.x.x    5432/TCP

NAME                                    READY   UP-TO-DATE   AVAILABLE
deployment/real-estate-backend-backend  1/1     1            1

NAME                                    READY
statefulset/real-estate-backend-postgres  1/1
```

## Architecture Comparison

### Docker Compose Architecture

```
docker-compose.yml
├── networks
│   └── real-estate-network (bridge)
├── services
│   ├── postgres
│   │   ├── image: postgres:18-alpine
│   │   ├── ports: 5432:5432
│   │   └── volumes: postgres_data
│   └── backend
│       ├── build: ./Dockerfile
│       ├── ports: 8080:8080
│       ├── depends_on: postgres
│       └── volumes: ./uploads
└── volumes
    └── postgres_data
```

### Kubernetes Architecture

```
Kubernetes Cluster (Minikube)
└── Namespace: real-estate-dev
    ├── ConfigMap: real-estate-backend-config
    ├── Secret: real-estate-backend-secret
    ├── PersistentVolumeClaim: backend-pvc (2Gi)
    ├── PersistentVolumeClaim: postgres-pvc (2Gi)
    ├── StatefulSet: postgres
    │   ├── Replicas: 1
    │   ├── Image: postgres:18-alpine
    │   ├── Volume: postgres-pvc -> /pgdata
    │   ├── Service: ClusterIP (5432)
    │   └── Health: pg_isready
    ├── Deployment: backend
    │   ├── Replicas: 1
    │   ├── Image: real-estate-backend:latest
    │   ├── Volume: backend-pvc -> /app/uploads
    │   ├── Service: NodePort (8080:30080)
    │   └── Health: /actuator/health
    └── Helm Release: real-estate (v1)
```

### Key Differences

| Aspect | Docker Compose | Kubernetes + Helm |
|--------|----------------|-------------------|
| **Orchestration** | Single host | Cluster-ready |
| **Scaling** | Manual | Declarative replicas |
| **Configuration** | Environment files | ConfigMaps/Secrets |
| **Service Discovery** | DNS by service name | ClusterIP/DNS |
| **Health Checks** | Docker healthcheck | Liveness/Readiness probes |
| **Upgrades** | Recreate containers | Rolling updates |
| **Rollback** | Manual | `helm rollback` |
| **Secrets** | Plain text env vars | Base64 encoded Secrets |
| **Storage** | Named volumes | PersistentVolumes |
| **Networking** | Bridge network | CNI (Bridge/Overlay) |

## Configuration Management

### Environment Variables Mapping

| Docker Compose | Kubernetes | Source |
|----------------|------------|--------|
| `SPRING_PROFILES_ACTIVE` | `SPRING_PROFILES_ACTIVE` | ConfigMap |
| `DB_HOST` | `DB_HOST` | ConfigMap |
| `DB_PORT` | `DB_PORT` | ConfigMap |
| `DB_NAME` | `DB_NAME` | ConfigMap |
| `DB_USER` | `DB_USER` | Secret |
| `DB_PASSWORD` | `DB_PASSWORD` | Secret |
| `CORS_ALLOWED_ORIGINS` | `CORS_ALLOWED_ORIGINS` | ConfigMap |

### Storage Mapping

| Docker Compose | Kubernetes |
|----------------|------------|
| `postgres_data` volume | `postgres-pvc` (2Gi PVC) |
| `./uploads` bind mount | `backend-pvc` (2Gi PVC) |

### Network Mapping

| Docker Compose | Kubernetes |
|----------------|------------|
| `real-estate-network` | Namespace-scoped networking |
| Service name `postgres` | `real-estate-backend-postgres.real-estate-dev.svc.cluster.local` |
| Port 5432 | ClusterIP service on port 5432 |
| Port 8080 | NodePort 30080 (externally accessible) |

## Troubleshooting

### Issue 1: PostgreSQL Volume Mount Errors

**Problem:**
```
Error mounting "/tmp/hostpath-provisioner/.../postgres-pvc" to rootfs at
"/var/lib/postgresql/data": no such file or directory
```

**Root Cause:**
Docker overlay filesystem in minikube doesn't support mounting to
`/var/lib/postgresql/data` path.

**Solution:**
Changed PGDATA path to `/pgdata`:
```yaml
env:
  - name: PGDATA
    value: /pgdata
volumeMounts:
  - name: postgres-storage
    mountPath: /pgdata
```

### Issue 2: Pods in CrashLoopBackOff

**Diagnosis:**
```bash
kubectl describe pod -n real-estate-dev <pod-name>
kubectl logs -n real-estate-dev <pod-name>
```

**Common Causes:**
- Image not found (forgot to build in minikube context)
- Configuration errors in ConfigMap/Secret
- Volume mount issues
- Resource constraints

**Solutions:**
- Rebuild image: `eval $(minikube docker-env) && docker build -t real-estate-backend:latest .`
- Check configs: `kubectl get configmap,secret -n real-estate-dev`
- Check resources: `kubectl top pods -n real-estate-dev`

### Issue 3: Cannot Access Application via NodePort

**Problem:**
```bash
curl http://$(minikube ip):30080
# Connection refused or timeout
```

**Solutions:**

**Option 1: Use Port Forward**
```bash
kubectl port-forward -n real-estate-dev \
  svc/real-estate-real-estate-backend-backend 8080:8080
curl http://localhost:8080/actuator/health
```

**Option 2: Use Minikube Service**
```bash
minikube service real-estate-real-estate-backend-backend -n real-estate-dev
```

**Option 3: Check Minikube Tunnel**
```bash
# In a separate terminal
minikube tunnel
```

### Issue 4: Database Connection Failures

**Diagnosis:**
```bash
# Check if PostgreSQL is running
kubectl get pods -n real-estate-dev -l app.kubernetes.io/component=database

# Check service
kubectl get svc -n real-estate-dev real-estate-real-estate-backend-postgres

# Test connection from backend pod
kubectl exec -n real-estate-dev deployment/real-estate-real-estate-backend-backend -- \
  nc -zv real-estate-real-estate-backend-postgres 5432
```

**Solutions:**
- Verify Secret has correct credentials
- Check ConfigMap has correct DB_HOST (service name)
- Ensure PostgreSQL pod is Running and Ready

### Issue 5: Helm Upgrade Not Applying Changes

**Problem:**
Changes to templates not reflected after `helm upgrade`

**Solution:**
For StatefulSet changes, need to delete and recreate:
```bash
helm uninstall real-estate
kubectl delete pvc --all -n real-estate-dev
helm install real-estate ./backend/helm/real-estate-backend \
  -f ./backend/helm/real-estate-backend/values-dev.yaml
```

## Rollback Instructions

### Rollback to Docker Compose

If you need to revert to Docker Compose:

**1. Uninstall Helm Release**
```bash
helm uninstall real-estate
kubectl delete namespace real-estate-dev
```

**2. Stop Minikube (Optional)**
```bash
minikube stop
# or delete completely
minikube delete
```

**3. Start Docker Compose**
```bash
cd backend
docker-compose up -d
```

**4. Verify**
```bash
docker-compose ps
curl http://localhost:8080/actuator/health
```

### Rollback Helm Release

To rollback to a previous Helm release version:

**1. View Release History**
```bash
helm history real-estate
```

**2. Rollback to Previous Version**
```bash
helm rollback real-estate
```

**3. Rollback to Specific Revision**
```bash
helm rollback real-estate 1
```

## Migration Checklist

- [x] Install Helm
- [x] Start Minikube cluster
- [x] Create Helm chart structure
- [x] Define values.yaml and values-dev.yaml
- [x] Create Kubernetes resource templates
  - [x] Namespace
  - [x] ConfigMap
  - [x] Secret
  - [x] PostgreSQL PVC
  - [x] PostgreSQL StatefulSet
  - [x] PostgreSQL Service
  - [x] Backend PVC
  - [x] Backend Deployment
  - [x] Backend Service
- [x] Create helper templates
- [x] Create NOTES.txt
- [x] Build Docker image in minikube context
- [x] Install Helm chart
- [x] Verify all pods are running
- [x] Test application health endpoint
- [x] Verify database connectivity
- [x] Test file upload functionality
- [x] Create migration documentation
- [x] Create usage guide

## Next Steps

1. **Production Deployment**
   - Create `values-prod.yaml` with production configurations
   - Set up external database (RDS, Cloud SQL, etc.)
   - Configure Ingress for external access
   - Set up TLS/SSL certificates
   - Configure monitoring and logging

2. **CI/CD Integration**
   - Automate Docker image builds
   - Implement automated testing
   - Set up automatic deployments
   - Configure rollback strategies

3. **Scalability**
   - Test horizontal pod autoscaling
   - Implement database read replicas
   - Configure load balancing
   - Set up resource quotas

4. **Security Hardening**
   - Use external secrets management (Vault, Sealed Secrets)
   - Implement network policies
   - Configure pod security policies
   - Enable RBAC

5. **Monitoring & Observability**
   - Deploy Prometheus for metrics
   - Set up Grafana dashboards
   - Configure log aggregation (ELK, Loki)
   - Implement distributed tracing

## Conclusion

The migration from Docker Compose to Kubernetes with Helm is now complete. The application is running successfully in a local Kubernetes environment with:

- **Better Configuration Management**: Separation of configuration via ConfigMaps and Secrets
- **Scalability**: Ready to scale horizontally with replica adjustments
- **Production Patterns**: Using industry-standard deployment patterns
- **Easy Upgrades**: Helm-based versioned deployments with rollback capability
- **Environment Parity**: Same deployment process for dev/staging/production

The Helm chart is production-ready and can be deployed to any Kubernetes cluster with minimal modifications to the values file.
