# Kubernetes Deployment Guide (Minikube)

This guide explains how to deploy, manage, and operate all services for the Real Estate Platform using **Minikube** with external PostgreSQL databases in Docker.

> **Note:** The deployment scripts (`k8s-start-external.sh`, `k8s-stop-external.sh`, etc.) are **Minikube-specific**. They rely on `minikube status`, `minikube start`, and `eval $(minikube docker-env)` to build images directly into Minikube's Docker daemon without a container registry. The underlying Helm charts and kubectl commands are provider-agnostic, but adapting to another K8s provider (EKS, Kind, k3s, etc.) would require replacing the Minikube checks and pushing images to a container registry instead.

## Architecture Overview

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                      MINIKUBE KUBERNETES DEPLOYMENT SETUP                          │
├───────────────────────────────────────────────────────────────────────────────────┤
│                                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐      │
│  │                    FRONTEND (Host Machine)                              │      │
│  │                 Next.js @ http://localhost:3000                         │      │
│  └──────────────────────────┬──────────────────────────────────────────────┘      │
│                             │                                                     │
│                             ▼                                                     │
│  ┌─────────────────────────────────────────────────────────────────────────┐      │
│  │                       MINIKUBE CLUSTER                                   │      │
│  │                                                                          │      │
│  │  Namespace: kong ──────────────────────────────────────────────────┐    │      │
│  │  │  ┌──────────────┐  ┌───────────────┐  ┌──────────────┐        │    │      │
│  │  │  │ Kong Gateway │  │ Kong Postgres │  │  Kong Redis  │        │    │      │
│  │  │  │ Proxy: :8000 │  │    :5432      │  │    :6379     │        │    │      │
│  │  │  │ Admin: :8001 │  └───────────────┘  └──────────────┘        │    │      │
│  │  │  └──────┬───────┘                                              │    │      │
│  │  └─────────┼──────────────────────────────────────────────────────┘    │      │
│  │            │ (K8s DNS: *.real-estate.svc.cluster.local)                │      │
│  │            ▼                                                            │      │
│  │  Namespace: real-estate ───────────────────────────────────────────┐   │      │
│  │  │                                                                 │   │      │
│  │  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐            │   │      │
│  │  │  │  Property   │  │    Auth     │  │    Post     │            │   │      │
│  │  │  │  Service    │  │   Service   │  │   Service   │            │   │      │
│  │  │  │  :8080      │  │   :8081     │  │   :8082     │            │   │      │
│  │  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘            │   │      │
│  │  │         │                │                │                    │   │      │
│  │  │         │    Kafka Events (user-activity-events)               │   │      │
│  │  │         ├────────────────┼────────────────┤                    │   │      │
│  │  │         ▼                ▼                ▼                    │   │      │
│  │  │  ┌──────────────────────────────────────────────┐             │   │      │
│  │  │  │              Apache Kafka (KRaft)             │             │   │      │
│  │  │  │                  :9092                        │             │   │      │
│  │  │  └────────────────────┬─────────────────────────┘             │   │      │
│  │  │                       │                                        │   │      │
│  │  │                       ▼                                        │   │      │
│  │  │  ┌─────────────────────────────────────────┐                  │   │      │
│  │  │  │          Analytics Service               │                  │   │      │
│  │  │  │              :8083                        │                  │   │      │
│  │  │  └────────────────────┬────────────────────┘                  │   │      │
│  │  │                       │                                        │   │      │
│  │  │                       ▼                                        │   │      │
│  │  │  ┌─────────────────────────────────────────┐                  │   │      │
│  │  │  │            MongoDB 7.0                   │                  │   │      │
│  │  │  │         analyticsdb :27017               │                  │   │      │
│  │  │  └─────────────────────────────────────────┘                  │   │      │
│  │  └────────────────────────────────────────────────────────────────┘   │      │
│  └──────────────────────────────────────────────────────────────────────┘      │
│                                                                                   │
│  ┌─────────────────────────────────────────────────────────────────────────┐      │
│  │               DOCKER CONTAINERS (External PostgreSQL DBs)               │      │
│  │                                                                          │      │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                     │      │
│  │  │ Property DB │  │   Auth DB   │  │   Post DB   │                     │      │
│  │  │ Port: 5432  │  │ Port: 5433  │  │ Port: 5434  │                     │      │
│  │  │ realestatedb│  │   authdb    │  │   postdb    │                     │      │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                     │      │
│  └─────────────────────────────────────────────────────────────────────────┘      │
│                                                                                   │
└───────────────────────────────────────────────────────────────────────────────────┘
```

This setup mirrors the production architecture (EKS + RDS/DocumentDB/MSK) by running application services inside Kubernetes while PostgreSQL databases remain external. Kafka and MongoDB run inside the cluster as StatefulSets. Images are built directly into Minikube's Docker daemon via `eval $(minikube docker-env)`, eliminating the need for a container registry.

## Prerequisites

- **Minikube** - Local Kubernetes cluster ([install guide](https://minikube.sigs.k8s.io/docs/start/))
- **kubectl** - Kubernetes CLI (bundled with Minikube or install separately)
- **Helm 3** - Kubernetes package manager
- **Docker Desktop** - Required for running external databases and as Minikube's container runtime
- **Maven** - For building Java microservices
- **Node.js 18+** - For frontend development

## Quick Start

### Start All Services (Step by Step)

Services must be started in this order:

```bash
# From project root
chmod +x **/*.sh

# 1. Start property-service (DB in Docker + app in K8s)
cd property-service && ./k8s-start-external.sh

# 2. Start auth-service (DB in Docker + app in K8s)
cd ../auth-service && ./k8s-start-external.sh

# 3. Start post-service (DB in Docker + app in K8s)
cd ../post-service && ./k8s-start-external.sh

# 4. Start analytics-service (Kafka + MongoDB + app in K8s)
cd ../analytics-service && ./k8s-start-external.sh

# 5. Start Kong API Gateway (fully in K8s with bundled PostgreSQL)
cd ../kong && ./k8s-start.sh

# 6. Configure Kong routes and plugins
cd ../kong && ./scripts/configure-routes.sh http://localhost:30001
cd ../kong && ./scripts/configure-plugins.sh http://localhost:30001

# 7. Start frontend (on host machine)
cd ../frontend && npm run dev
```

### What Each Start Script Does

Each `k8s-start-external.sh` script performs these steps automatically:

1. **[Minikube]** Checks if Minikube is running; starts it if not (4096MB memory, 2 CPUs)
2. Starts the service's PostgreSQL in Docker via `docker-compose-db.yml`
3. **[Minikube]** Configures Docker to use Minikube's daemon (`eval $(minikube docker-env)`)
4. Builds the Java app with Maven (`mvn clean package -DskipTests`)
5. **[Minikube]** Builds the Docker image directly into Minikube (no registry push needed)
6. Creates the `real-estate` namespace
7. Deploys via Helm with `values-local.yaml`
8. Waits for pods to be ready (120s timeout)
9. Sets up `kubectl port-forward` for local access
10. Runs health check via `/actuator/health`

## Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | Next.js application |
| Kong Proxy | http://localhost:8000 | API Gateway (routes all API traffic) |
| Kong Admin | http://localhost:8001 | Kong admin API |
| Property Service | http://localhost:8080 | Property service (via port-forward) |
| Auth Service | http://localhost:8081 | Authentication service (via port-forward) |
| Post Service | http://localhost:8082 | Social feed service (via port-forward) |
| Analytics Service | http://localhost:8083 | Analytics/event pipeline (via port-forward) |

## Kubernetes Namespaces

| Namespace | Purpose | Components |
|-----------|---------|------------|
| `real-estate` | Microservices | property-service, auth-service, post-service, analytics-service |
| `real-estate` | Event Pipeline | Kafka (KRaft), MongoDB |
| `kong` | API Gateway | Kong, Kong PostgreSQL, Kong Redis |

## Database & Infrastructure Connections

### PostgreSQL (External - Docker Containers)

PostgreSQL databases run as Docker containers **outside** Kubernetes, simulating production RDS instances.

| Database | Container | Host Port | Database | User | Password |
|----------|-----------|-----------|----------|------|----------|
| Property DB | `property-db` | 5432 | realestatedb | postgres | postgres |
| Auth DB | `auth-db` | 5433 | authdb | postgres | postgres |
| Post DB | `post-db` | 5434 | postdb | postgres | postgres |

Connect with psql:
```bash
psql -h localhost -p 5432 -U postgres -d realestatedb
psql -h localhost -p 5433 -U postgres -d authdb
psql -h localhost -p 5434 -U postgres -d postdb
```

K8s pods connect to these databases via the host gateway IP (`192.168.105.1` for QEMU driver, `host.docker.internal` for Docker Desktop).

### MongoDB (In-Cluster - Analytics)

MongoDB runs as a StatefulSet inside K8s for analytics event persistence.

| Component | Namespace | Port | Database | Storage |
|-----------|-----------|------|----------|---------|
| MongoDB 7.0 | real-estate | 27017 | analyticsdb | 10Gi PVC |

Connect via port-forward:
```bash
kubectl port-forward svc/analytics-mongo 27017:27017 -n real-estate &
mongosh mongodb://localhost:27017/analyticsdb
```

### Apache Kafka (In-Cluster - Event Pipeline)

Kafka runs in KRaft mode (no ZooKeeper) as a StatefulSet inside K8s.

| Component | Namespace | Port | Topics | Storage |
|-----------|-----------|------|--------|---------|
| Kafka (KRaft) | real-estate | 9092 | user-activity-events | 10Gi PVC |

**Event flow:** property-service, auth-service, post-service → Kafka (`user-activity-events` topic) → analytics-service → MongoDB

## Kubernetes Resources

### Helm Releases

| Release | Namespace | Chart | Values File |
|---------|-----------|-------|-------------|
| `real-estate-backend` | real-estate | `property-service/helm/real-estate-backend` | `values-local.yaml` |
| `auth-service` | real-estate | `auth-service/helm/auth-service` | `values-local.yaml` |
| `post-service` | real-estate | `post-service/helm/post-service` | `values-local.yaml` |
| `analytics-service` | real-estate | `analytics-service/helm/analytics-service` | `values-local.yaml` |
| `kong-gateway` | kong | `kong/helm/kong` | `values-local.yaml` |

### Pods

| Pod | Namespace | Image | Port |
|-----|-----------|-------|------|
| `real-estate-backend-backend-*` | real-estate | `real-estate-backend:latest` | 8080 |
| `auth-service-*` | real-estate | `auth-service:latest` | 8081 |
| `post-service-*` | real-estate | `post-service:latest` | 8082 |
| `analytics-service-*` | real-estate | `analytics-service:latest` | 8083 |
| `analytics-kafka-*` | real-estate | `confluentinc/cp-kafka:7.5.0` | 9092 |
| `analytics-mongo-*` | real-estate | `mongo:7.0` | 27017 |
| `kong-*` | kong | `kong:3.5` | 8000, 8001 |
| `kong-postgres-*` | kong | `postgres:18-alpine` | 5432 |
| `kong-redis-*` | kong | `redis` | 6379 |

### Services

| Service | Namespace | Type | Port | NodePort |
|---------|-----------|------|------|----------|
| `real-estate-backend-backend` | real-estate | NodePort | 8080 | 30080 |
| `auth-service` | real-estate | NodePort | 8081 | 30081 |
| `post-service` | real-estate | NodePort | 8082 | 30082 |
| `analytics-service` | real-estate | NodePort | 8083 | 30083 |
| `analytics-kafka` | real-estate | ClusterIP | 9092 | - |
| `analytics-mongo` | real-estate | ClusterIP | 27017 | - |
| `kong-proxy` | kong | NodePort | 80 | 30000 |
| `kong-admin` | kong | NodePort | 8001 | 30001 |
| `kong-postgres` | kong | ClusterIP | 5432 | - |
| `kong-redis` | kong | ClusterIP | 6379 | - |

### Port-Forward Mappings

All local access uses `kubectl port-forward`:

```bash
# These are set up automatically by the start scripts
kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate &
kubectl port-forward svc/auth-service 8081:8081 -n real-estate &
kubectl port-forward svc/post-service 8082:8082 -n real-estate &
kubectl port-forward svc/analytics-service 8083:8083 -n real-estate &
kubectl port-forward svc/kong-proxy 8000:80 -n kong &
kubectl port-forward svc/kong-admin 8001:8001 -n kong &

# Optional: access Kafka and MongoDB directly for debugging
kubectl port-forward svc/analytics-kafka 9092:9092 -n real-estate &
kubectl port-forward svc/analytics-mongo 27017:27017 -n real-estate &
```

## Kong Gateway Configuration

Kong runs in **database mode** (backed by its own PostgreSQL in K8s) and is configured via the Admin API.

### Configure Routes

After Kong is running, set up services and routes:

```bash
# Configure services and routes
cd kong && ./scripts/configure-routes.sh http://localhost:30001

# Configure plugins (CORS, JWT, ACL, rate limiting)
cd kong && ./scripts/configure-plugins.sh http://localhost:30001
```

### Kong Services (Internal DNS)

Kong routes traffic to backend services using Kubernetes DNS:

| Kong Service | Target URL |
|-------------|------------|
| `auth-service` | `http://auth-service.real-estate.svc.cluster.local:8081` |
| `backend-service` | `http://real-estate-backend-backend.real-estate.svc.cluster.local:8080` |
| `post-service` | `http://post-service.real-estate.svc.cluster.local:8082` |

### Kong Plugins

| Plugin | Scope | Description |
|--------|-------|-------------|
| CORS | Global | Cross-origin resource sharing |
| Correlation-ID | Global | Request tracing via X-Request-ID |
| JWT | Protected routes | RS256 token validation via auth-service JWKS |
| ACL | Admin routes | Role-based access (`admin` group) |
| Rate Limiting | Auth/Upload routes | 10/min on auth, 10/min on uploads, 30/min on properties |

## Scripts Reference

### Per-Service Scripts

Each service directory (`property-service/`, `auth-service/`, `post-service/`, `analytics-service/`) contains:

| Script | Description |
|--------|-------------|
| `k8s-start-external.sh` | Build image, start DB in Docker, deploy to K8s via Helm |
| `k8s-stop-external.sh` | Stop service (3 options: scale down / stop all / full cleanup) |
| `k8s-status-external.sh` | Check Minikube, pods, DB, health, and network connectivity |

### Kong Scripts (`kong/`)

| Script | Description |
|--------|-------------|
| `k8s-start.sh` | Deploy Kong + PostgreSQL + Redis to K8s via Helm |
| `k8s-stop.sh` | Stop Kong (3 options: scale down / stop all / full cleanup) |
| `k8s-status.sh` | Check Kong pods, services, admin API health, route count |

### Kong Configuration Scripts (`kong/scripts/`)

| Script | Description |
|--------|-------------|
| `setup-kong.sh` | Deploy Kong via Helm (alternative to `k8s-start.sh`) |
| `configure-routes.sh` | Create all Kong services and routes via Admin API |
| `configure-plugins.sh` | Configure JWT, CORS, ACL, rate limiting plugins |
| `configure-header-transformer.sh` | Set up header transformation rules |
| `configure-observability.sh` | Configure Prometheus metrics and logging |
| `configure-tls.sh` | Set up TLS/HTTPS certificates |
| `configure-traffic-management.sh` | Configure traffic policies |
| `security-hardening.sh` | Apply security hardening settings |
| `test-kong-integration.sh` | Run integration tests for Kong routing |

## Common Operations

### Check Status

```bash
# Per-service status
cd property-service && ./k8s-status-external.sh
cd auth-service && ./k8s-status-external.sh
cd post-service && ./k8s-status-external.sh
cd analytics-service && ./k8s-status-external.sh

# Kong status
cd kong && ./k8s-status.sh

# Quick pod overview
kubectl get pods -n real-estate
kubectl get pods -n kong

# All services and endpoints
kubectl get svc -n real-estate
kubectl get svc -n kong

# Helm releases
helm list -n real-estate
helm list -n kong

# Check Kafka and MongoDB health
kubectl exec -n real-estate svc/analytics-kafka -- kafka-topics --bootstrap-server localhost:9092 --list
kubectl exec -n real-estate svc/analytics-mongo -- mongosh --eval "db.adminCommand('ping')"
```

### View Logs

```bash
# Microservice logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate
kubectl logs -f deployment/auth-service -n real-estate
kubectl logs -f deployment/post-service -n real-estate
kubectl logs -f deployment/analytics-service -n real-estate

# Kafka and MongoDB logs
kubectl logs -f statefulset/analytics-kafka -n real-estate
kubectl logs -f statefulset/analytics-mongo -n real-estate

# Kong logs
kubectl logs -f deployment/kong -n kong

# Database logs (Docker - PostgreSQL)
docker logs -f property-db
docker logs -f auth-db
docker logs -f post-db
```

### Restart a Service

```bash
# Restart a pod (rolling restart)
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate
kubectl rollout restart deployment/auth-service -n real-estate
kubectl rollout restart deployment/post-service -n real-estate
kubectl rollout restart deployment/analytics-service -n real-estate
kubectl rollout restart deployment/kong -n kong

# Restart Kafka or MongoDB (StatefulSets)
kubectl rollout restart statefulset/analytics-kafka -n real-estate
kubectl rollout restart statefulset/analytics-mongo -n real-estate
```

### Rebuild and Redeploy (After Code Changes)

```bash
# IMPORTANT: Point Docker to Minikube's daemon first (required for every new terminal session)
eval $(minikube docker-env)

# Rebuild property-service
cd property-service
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate

# Rebuild auth-service
cd ../auth-service
mvn clean package -DskipTests
docker build -t auth-service:latest .
kubectl rollout restart deployment/auth-service -n real-estate

# Rebuild post-service
cd ../post-service
mvn clean package -DskipTests
docker build -t post-service:latest .
kubectl rollout restart deployment/post-service -n real-estate

# Rebuild analytics-service
cd ../analytics-service
mvn clean package -DskipTests
docker build -t analytics-service:latest .
kubectl rollout restart deployment/analytics-service -n real-estate
```

### Helm Upgrade (After Values Changes)

```bash
# Upgrade property-service
helm upgrade real-estate-backend ./property-service/helm/real-estate-backend \
  -f ./property-service/helm/real-estate-backend/values-local.yaml \
  -n real-estate

# Upgrade auth-service
helm upgrade auth-service ./auth-service/helm/auth-service \
  -f ./auth-service/helm/auth-service/values-local.yaml \
  -n real-estate

# Upgrade post-service
helm upgrade post-service ./post-service/helm/post-service \
  -f ./post-service/helm/post-service/values-local.yaml \
  -n real-estate

# Upgrade analytics-service (includes Kafka + MongoDB)
helm upgrade analytics-service ./analytics-service/helm/analytics-service \
  -f ./analytics-service/helm/analytics-service/values-local.yaml \
  -n real-estate

# Upgrade Kong
helm upgrade kong-gateway ./kong/helm/kong \
  -f ./kong/helm/kong/values-local.yaml \
  -n kong
```

### Scale Services

```bash
# Scale up/down
kubectl scale deployment/real-estate-backend-backend --replicas=2 -n real-estate
kubectl scale deployment/auth-service --replicas=0 -n real-estate
kubectl scale deployment/analytics-service --replicas=2 -n real-estate
kubectl scale deployment/kong --replicas=2 -n kong

# Note: Kafka and MongoDB are StatefulSets — scale carefully
kubectl scale statefulset/analytics-kafka --replicas=3 -n real-estate   # For HA cluster
kubectl scale statefulset/analytics-mongo --replicas=3 -n real-estate   # For replica set
```

### Kong Route Management

```bash
# List all routes
curl http://localhost:8001/routes | python3 -m json.tool

# List all services
curl http://localhost:8001/services | python3 -m json.tool

# List all plugins
curl http://localhost:8001/plugins | python3 -m json.tool

# Kong status
curl http://localhost:8001/status | python3 -m json.tool
```

### Stop Services

```bash
# Stop individual services (interactive - choose option 1/2/3)
cd property-service && ./k8s-stop-external.sh
cd auth-service && ./k8s-stop-external.sh
cd post-service && ./k8s-stop-external.sh
cd analytics-service && ./k8s-stop-external.sh
cd kong && ./k8s-stop.sh
```

Stop options for each service:
1. **Scale down app only** - Keep DB running, scale K8s deployment to 0
2. **Stop app + DB** - Scale K8s deployment to 0, stop Docker DB (preserve data)
3. **Full cleanup** - Uninstall Helm release, delete PVCs, remove DB container + volume

### Full Reset

```bash
# Stop and clean up everything
cd property-service && ./k8s-stop-external.sh   # Choose option 3
cd ../auth-service && ./k8s-stop-external.sh     # Choose option 3
cd ../post-service && ./k8s-stop-external.sh     # Choose option 3
cd ../analytics-service && ./k8s-stop-external.sh # Choose option 3
cd ../kong && ./k8s-stop.sh                      # Choose option 3

# Or manually:
helm uninstall real-estate-backend -n real-estate
helm uninstall auth-service -n real-estate
helm uninstall post-service -n real-estate
helm uninstall analytics-service -n real-estate
helm uninstall kong-gateway -n kong
kubectl delete namespace real-estate
kubectl delete namespace kong
docker-compose -f property-service/docker-compose-db.yml down -v
docker-compose -f auth-service/docker-compose-db.yml down -v
docker-compose -f post-service/docker-compose-db.yml down -v
```

## Resource Allocation

### Minikube Cluster

| Resource | Minimum | Recommended (all services) |
|----------|---------|---------------------------|
| Memory | 6144 MB | 8192 MB |
| CPUs | 2 | 4 |

Start with recommended resources: `minikube start --memory=8192 --cpus=4`

### Per-Service Resources

| Component | CPU Request | CPU Limit | Memory Request | Memory Limit |
|-----------|-------------|-----------|----------------|--------------|
| Property Service | 500m | 1000m | 512Mi | 1024Mi |
| Auth Service | - | - | - | - |
| Post Service | 100m | 500m | 256Mi | 512Mi |
| Analytics Service | 100m | 500m | 256Mi | 512Mi |
| Kafka (KRaft) | 250m | 1000m | 512Mi | 1024Mi |
| MongoDB | 250m | 500m | 256Mi | 512Mi |
| Kong | 250m | 500m | 256Mi | 512Mi |
| Kong PostgreSQL | 100m | 250m | 128Mi | 256Mi |
| Kong Redis | 50m | 100m | 64Mi | 128Mi |

## Health Checks

All Spring Boot services expose health endpoints:

```bash
# Via port-forward
curl http://localhost:8080/actuator/health   # Property service
curl http://localhost:8081/actuator/health   # Auth service
curl http://localhost:8082/actuator/health   # Post service
curl http://localhost:8083/actuator/health   # Analytics service (includes mongo: UP, kafka: UP)

# Auth-specific: JWKS endpoint
curl http://localhost:8081/.well-known/jwks.json

# Kong status
curl http://localhost:8001/status

# Kafka health
kubectl exec -n real-estate svc/analytics-kafka -- \
  kafka-topics --bootstrap-server localhost:9092 --describe --topic user-activity-events

# MongoDB health
kubectl exec -n real-estate svc/analytics-mongo -- \
  mongosh --eval "db.adminCommand('ping')" --quiet

# Check analytics event count
kubectl exec -n real-estate svc/analytics-mongo -- \
  mongosh analyticsdb --eval "db.activity_events.countDocuments({})" --quiet
```

## API Testing

### Get JWT Token

```bash
TOKEN=$(curl -s -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}' \
  | jq -r '.accessToken')
```

### Test Protected Endpoint

```bash
curl http://localhost:8000/api/properties/user \
  -H "Authorization: Bearer $TOKEN"
```

### Test Public Endpoints

```bash
# Properties (via Kong)
curl http://localhost:8000/api/properties

# Posts (via Kong)
curl http://localhost:8000/api/posts

# Direct service health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

## Environment Configuration

### Database Host (values-local.yaml)

Services in K8s connect to Docker databases via the host gateway IP:

| Minikube Driver | DB Host Value |
|-----------------|---------------|
| QEMU | `192.168.105.1` |
| Docker Desktop (Mac/Windows) | `host.docker.internal` |
| Linux with Kind | `172.17.0.1` |

To change, update `dbHost` in each service's `helm/<service>/values-local.yaml`.

### Frontend (.env.local)

```bash
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-key-here-change-in-production-min-32-characters-long
NEXT_PUBLIC_KONG_URL=http://127.0.0.1:8000
NEXT_PUBLIC_API_URL=http://127.0.0.1:8000/api
NEXT_PUBLIC_AUTH_API_URL=http://127.0.0.1:8000
```

## Troubleshooting

### Minikube Issues

```bash
# Check status
minikube status

# Delete and recreate
minikube delete
minikube start --memory=4096 --cpus=2

# Check available resources
minikube ssh -- free -m
minikube ssh -- df -h

# Verify Docker points to Minikube
eval $(minikube docker-env)
docker images | grep -E "real-estate|auth-service|post-service"
```

### Pod Not Starting

```bash
# Check pod status
kubectl get pods -n real-estate
kubectl get pods -n kong

# Describe pod for events
kubectl describe pod -n real-estate -l app.kubernetes.io/name=auth-service

# Check recent events
kubectl get events -n real-estate --sort-by='.lastTimestamp'

# Check container logs
kubectl logs -n real-estate -l app.kubernetes.io/component=backend
```

### Database Connection Issues

```bash
# Verify Docker databases are running
docker ps | grep -E "property-db|auth-db|post-db"

# Test database readiness
docker exec property-db pg_isready -U postgres
docker exec auth-db pg_isready -U postgres
docker exec post-db pg_isready -U postgres

# Test connectivity from K8s pod to Docker DB
kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- sh
# Inside pod:
ping 192.168.105.1
nc -zv 192.168.105.1 5432
```

### Kong Not Routing Properly

```bash
# Check Kong is running
kubectl get pods -n kong

# Check Kong admin API
curl http://localhost:8001/status

# Verify routes exist
curl http://localhost:8001/routes | python3 -m json.tool

# Verify services exist
curl http://localhost:8001/services | python3 -m json.tool

# Check Kong logs for errors
kubectl logs -f deployment/kong -n kong

# Re-configure routes if needed
cd kong && ./scripts/configure-routes.sh http://localhost:30001
```

### Port-Forward Disconnected

```bash
# Kill stale port-forwards
pkill -f "port-forward"

# Re-establish port-forwards
kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate &
kubectl port-forward svc/auth-service 8081:8081 -n real-estate &
kubectl port-forward svc/post-service 8082:8082 -n real-estate &
kubectl port-forward svc/analytics-service 8083:8083 -n real-estate &
kubectl port-forward svc/kong-proxy 8000:80 -n kong &
kubectl port-forward svc/kong-admin 8001:8001 -n kong &
```

### Analytics Pipeline Issues

```bash
# Verify Kafka topic exists
kubectl exec -n real-estate svc/analytics-kafka -- \
  kafka-topics --bootstrap-server localhost:9092 --list

# Check if events are being produced (consume from beginning)
kubectl exec -n real-estate svc/analytics-kafka -- \
  kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic user-activity-events --from-beginning --max-messages 5

# Check analytics-service consumer logs
kubectl logs -f deployment/analytics-service -n real-estate | grep -i "activity"

# Verify MongoDB has events
kubectl exec -n real-estate svc/analytics-mongo -- \
  mongosh analyticsdb --eval "db.activity_events.find().sort({_id:-1}).limit(3).pretty()" --quiet

# Check Kafka consumer group lag
kubectl exec -n real-estate svc/analytics-kafka -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 \
  --describe --group analytics-group
```

### Kafka Not Starting

```bash
# Check Kafka pod status and logs
kubectl describe pod -n real-estate -l app=analytics-kafka
kubectl logs -n real-estate -l app=analytics-kafka --tail=50

# Verify PVC is bound
kubectl get pvc -n real-estate | grep kafka

# If stuck in CrashLoopBackOff, check storage
kubectl exec -n real-estate svc/analytics-kafka -- df -h /var/lib/kafka/data
```

### MongoDB Connection Issues

```bash
# Check MongoDB pod
kubectl describe pod -n real-estate -l app=analytics-mongo
kubectl logs -n real-estate -l app=analytics-mongo --tail=50

# Verify PVC is bound
kubectl get pvc -n real-estate | grep mongo

# Test connectivity from analytics-service pod
kubectl exec -n real-estate deployment/analytics-service -- \
  wget -qO- http://analytics-mongo:27017 || echo "Connection test complete"
```

### First Build Is Slow

The first run of each `k8s-start-external.sh` performs a full Maven build (`mvn clean package`) and Docker image build inside Minikube. This can take several minutes per service. Subsequent rebuilds use Docker layer caching and are much faster.

## Production Deployment (EKS)

When deploying to production on AWS EKS, replace the local infrastructure with managed services:

### Infrastructure Mapping

| Local (Minikube) | Production (AWS) | Notes |
|------------------|-----------------|-------|
| PostgreSQL in Docker | Amazon RDS (PostgreSQL) | Multi-AZ, automated backups |
| MongoDB StatefulSet | Amazon DocumentDB or MongoDB Atlas | Managed, auto-scaling |
| Kafka StatefulSet | Amazon MSK (Managed Kafka) | Multi-broker, auto-scaling |
| Minikube | Amazon EKS | Managed K8s control plane |
| `host.docker.internal` | RDS/MSK endpoints | VPC-internal DNS |
| NodePort services | AWS ALB Ingress | TLS termination, WAF |
| Local PVCs | EBS gp3 volumes | Or managed service storage |

### Production Kafka Configuration

```yaml
# values-production.yaml (analytics-service)
kafka:
  enabled: false  # Use Amazon MSK instead of in-cluster Kafka
  bootstrapServers: "b-1.msk-cluster.xxxx.kafka.us-east-1.amazonaws.com:9092,b-2.msk-cluster.xxxx.kafka.us-east-1.amazonaws.com:9092"
  topicReplicationFactor: 3
  topicPartitions: 6
```

### Production MongoDB Configuration

```yaml
# values-production.yaml (analytics-service)
mongodb:
  enabled: false  # Use Amazon DocumentDB instead of in-cluster MongoDB
  uri: "mongodb://user:pass@docdb-cluster.cluster-xxxx.us-east-1.docdb.amazonaws.com:27017/analyticsdb?tls=true&replicaSet=rs0&readPreference=secondaryPreferred"
```

### Production Checklist

- [ ] **Kafka**: Use Amazon MSK with 3+ brokers, enable encryption in transit (TLS), configure IAM authentication
- [ ] **MongoDB**: Use DocumentDB or MongoDB Atlas with replica set, enable TLS, configure backup retention
- [ ] **PostgreSQL**: Use RDS Multi-AZ with automated backups, read replicas for reporting
- [ ] **Secrets**: Store credentials in AWS Secrets Manager, inject via External Secrets Operator
- [ ] **Networking**: All services in private subnets, ALB in public subnet with WAF
- [ ] **Monitoring**: CloudWatch metrics for MSK/DocumentDB, Prometheus for K8s pods
- [ ] **Scaling**: HPA for analytics-service (based on Kafka consumer lag), node auto-scaling via Karpenter
- [ ] **Storage**: Use EBS gp3 for any in-cluster PVCs, enable volume snapshots
- [ ] **Topic retention**: Set `user-activity-events` retention to 7 days in production
- [ ] **MongoDB indexes**: Create indexes on `eventType`, `userId`, `timestamp` for query performance

---

**Last Updated:** 2026-02-15
