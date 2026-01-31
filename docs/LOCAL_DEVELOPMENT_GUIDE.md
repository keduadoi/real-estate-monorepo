# Local Development Guide

This guide explains how to start, stop, and manage all services for the Real Estate Platform in a local development environment.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         LOCAL DEVELOPMENT SETUP                          │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    FRONTEND (Host Machine)                       │   │
│  │                 Next.js @ http://localhost:3000                  │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │                                          │
│                               ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │              KUBERNETES (Minikube) + Port Forwards               │   │
│  │                                                                   │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │    Kong     │  │   Backend   │  │    Auth     │              │   │
│  │  │  Gateway    │  │   Service   │  │   Service   │              │   │
│  │  │ :8000 → :80 │  │ :8080→:8080 │  │ :8081→:8081 │              │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘              │   │
│  │                                                                   │   │
│  │  ┌─────────────┐                                                 │   │
│  │  │    Post     │                                                 │   │
│  │  │   Service   │                                                 │   │
│  │  │ :8082→:8082 │                                                 │   │
│  │  └─────────────┘                                                 │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                               │                                          │
│                               ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    DATABASES (Docker)                            │   │
│  │                                                                   │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │  Backend DB │  │   Auth DB   │  │   Post DB   │              │   │
│  │  │ Port: 5432  │  │ Port: 5433  │  │ Port: 5434  │              │   │
│  │  │ realestatedb│  │   authdb    │  │   postdb    │              │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

## Prerequisites

- **Docker Desktop** - For running PostgreSQL databases
- **Minikube** - For running Kubernetes locally
- **kubectl** - Kubernetes CLI
- **Helm 3** - Kubernetes package manager
- **Node.js 18+** - For frontend development

## Quick Start

### Option 1: Start All Services at Once

```bash
# From project root
chmod +x scripts/*.sh
./scripts/start-all-services.sh
```

### Option 2: Start Services Individually

```bash
# 1. Start Minikube
minikube start --memory=8192 --cpus=4

# 2. Start databases
cd backend && docker-compose -f docker-compose-db.yml up -d
cd ../auth-service && docker-compose -f docker-compose-db.yml up -d
cd ../post-service && docker-compose -f docker-compose-db.yml up -d

# 3. Deploy services to K8s
cd ../backend && ./k8s-start-external.sh
cd ../auth-service && ./k8s-start-external.sh
cd ../post-service && ./k8s-start-external.sh

# 4. Start port-forwards
kubectl port-forward svc/kong-proxy 8000:80 -n kong &
kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate &
kubectl port-forward svc/auth-service 8081:8081 -n real-estate &
kubectl port-forward svc/post-service 8082:8082 -n real-estate &

# 5. Start frontend
cd ../frontend && npm run dev
```

## Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | Next.js application |
| Kong Gateway | http://localhost:8000 | API Gateway (routes all API traffic) |
| Backend API | http://localhost:8080 | Property service (direct access) |
| Auth Service | http://localhost:8081 | Authentication service (direct access) |
| Post Service | http://localhost:8082 | Social feed service (direct access) |
| Kong Admin | http://localhost:8001 | Kong admin API |

## Database Connections

| Database | Host | Port | Database | User | Password |
|----------|------|------|----------|------|----------|
| Backend DB | localhost | 5432 | realestatedb | postgres | postgres |
| Auth DB | localhost | 5433 | authdb | postgres | postgres |
| Post DB | localhost | 5434 | postdb | postgres | postgres |

Connect with psql:
```bash
# Backend DB
psql -h localhost -p 5432 -U postgres -d realestatedb

# Auth DB
psql -h localhost -p 5433 -U postgres -d authdb

# Post DB
psql -h localhost -p 5434 -U postgres -d postdb
```

## Scripts Reference

### Master Scripts (in `scripts/` directory)

| Script | Description |
|--------|-------------|
| `start-all-services.sh` | Start all databases, deploy all K8s services, setup port-forwards, optionally start frontend |
| `status-all-services.sh` | Check status of all services |
| `stop-all-services.sh` | Stop services (with options for partial/full cleanup) |

### Backend Service Scripts (`backend/`)

| Script | Description |
|--------|-------------|
| `k8s-start-external.sh` | Start backend with external DB (Docker) |
| `k8s-status-external.sh` | Check backend service status |
| `k8s-stop-external.sh` | Stop backend service |

### Auth Service Scripts (`auth-service/`)

| Script | Description |
|--------|-------------|
| `k8s-start-external.sh` | Start auth service with external DB (Docker) |
| `k8s-status-external.sh` | Check auth service status |
| `k8s-stop-external.sh` | Stop auth service |

### Post Service Scripts (`post-service/`)

| Script | Description |
|--------|-------------|
| `k8s-start-external.sh` | Start post service with external DB (Docker) |
| `k8s-status-external.sh` | Check post service status |
| `k8s-stop-external.sh` | Stop post service |

### Kong Gateway Scripts (`kong/`)

| Script | Description |
|--------|-------------|
| `k8s-start.sh` | Start Kong API Gateway (includes PostgreSQL & Redis in K8s) |
| `k8s-status.sh` | Check Kong status, routes, and services |
| `k8s-stop.sh` | Stop Kong (with options for partial/full cleanup) |

### Frontend Scripts (`frontend/`)

| Script | Description |
|--------|-------------|
| `start-dev.sh` | Start Next.js development server |
| `status.sh` | Check frontend and backend connectivity status |
| `stop.sh` | Stop frontend development server |

## Common Operations

### Check Status
```bash
# All services
./scripts/status-all-services.sh

# Individual service
cd backend && ./k8s-status-external.sh
```

### View Logs
```bash
# Backend
kubectl logs -f deployment/real-estate-backend-backend -n real-estate

# Auth Service
kubectl logs -f deployment/auth-service -n real-estate

# Post Service
kubectl logs -f deployment/post-service -n real-estate

# Kong
kubectl logs -f deployment/kong -n kong

# Database logs
docker logs -f real-estate-postgres-local
docker logs -f auth-db
docker logs -f post-db
```

### Restart a Service
```bash
# Restart backend
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate

# Restart auth-service
kubectl rollout restart deployment/auth-service -n real-estate

# Restart post-service
kubectl rollout restart deployment/post-service -n real-estate

# Restart Kong
kubectl rollout restart deployment/kong -n kong
```

### Kong Gateway Management
```bash
# Start Kong
cd kong && ./k8s-start.sh

# Check Kong status
cd kong && ./k8s-status.sh

# View Kong routes
curl http://localhost:8001/routes | jq

# View Kong services
curl http://localhost:8001/services | jq

# View Kong plugins
curl http://localhost:8001/plugins | jq

# Kong logs
kubectl logs -f deployment/kong -n kong
```

### Frontend Management
```bash
# Start frontend
cd frontend && ./start-dev.sh

# Check frontend status
cd frontend && ./status.sh

# Stop frontend
cd frontend && ./stop.sh

# Build for production
cd frontend && npm run build

# Run production build locally
cd frontend && npm run start
```

### Rebuild and Redeploy
```bash
# Point to Minikube's Docker
eval $(minikube docker-env)

# Rebuild and redeploy backend
cd backend
mvn clean package -DskipTests
docker build -t real-estate-backend:latest .
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate

# Similar for other services...
```

### Stop Services

```bash
# Stop all (keep data)
./scripts/stop-all-services.sh
# Choose option 2

# Stop individual service
cd backend && ./k8s-stop-external.sh
# Choose option 1 (K8s only) or 2 (K8s + DB)
```

### Full Reset
```bash
./scripts/stop-all-services.sh
# Choose option 4 and type DELETE

# Or manually:
helm uninstall real-estate-backend -n real-estate
helm uninstall auth-service -n real-estate
helm uninstall post-service -n real-estate
helm uninstall kong-gateway -n kong
kubectl delete namespace real-estate
kubectl delete namespace kong
cd backend && docker-compose -f docker-compose-db.yml down -v
cd ../auth-service && docker-compose -f docker-compose-db.yml down -v
cd ../post-service && docker-compose -f docker-compose-db.yml down -v
```

## Port Forward Management

Port forwards are required because Minikube runs in a VM/container and NodePorts aren't directly accessible on Mac/Windows.

### Start Port Forwards
```bash
kubectl port-forward svc/kong-proxy 8000:80 -n kong &
kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate &
kubectl port-forward svc/auth-service 8081:8081 -n real-estate &
kubectl port-forward svc/post-service 8082:8082 -n real-estate &
```

### Kill All Port Forwards
```bash
pkill -f "port-forward"
```

### Check Active Port Forwards
```bash
ps aux | grep port-forward | grep -v grep
```

## Troubleshooting

### Service Not Starting

1. Check pod status:
   ```bash
   kubectl get pods -n real-estate
   kubectl describe pod <pod-name> -n real-estate
   ```

2. Check logs:
   ```bash
   kubectl logs <pod-name> -n real-estate
   ```

3. Check events:
   ```bash
   kubectl get events -n real-estate --sort-by='.lastTimestamp'
   ```

### Database Connection Issues

1. Verify database is running:
   ```bash
   docker ps | grep -E "postgres|auth-db|post-db"
   ```

2. Test database connection:
   ```bash
   docker exec real-estate-postgres-local pg_isready -U postgres
   ```

3. Check from inside K8s pod:
   ```bash
   kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- sh
   # Inside pod:
   nc -zv host.docker.internal 5432
   ```

### Port Forward Issues

1. Check if port is in use:
   ```bash
   lsof -i :8080
   ```

2. Kill existing port-forward:
   ```bash
   pkill -f "port-forward.*8080"
   ```

### Frontend API Errors

1. Verify Kong is running:
   ```bash
   curl http://localhost:8000/api/properties
   ```

2. Check frontend .env.local:
   ```
   NEXT_PUBLIC_API_URL=http://127.0.0.1:8000/api
   NEXT_PUBLIC_AUTH_API_URL=http://127.0.0.1:8000
   ```

## Environment Configuration

### Frontend (.env.local)
```bash
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-key-here-change-in-production-min-32-characters-long
NEXT_PUBLIC_KONG_URL=http://127.0.0.1:8000
NEXT_PUBLIC_API_URL=http://127.0.0.1:8000/api
NEXT_PUBLIC_AUTH_API_URL=http://127.0.0.1:8000
```

### Kubernetes Values (values-local.yaml)
Each service has a `values-local.yaml` that configures:
- `host.docker.internal` for database host (accessible from K8s to Docker)
- External database ports (5432, 5433, 5434)
- `pullPolicy: Never` or `IfNotPresent` for local images

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
# Properties
curl http://localhost:8000/api/properties

# Posts
curl http://localhost:8000/api/posts

# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

---

**Last Updated:** 2026-01-31
