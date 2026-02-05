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
│  │                    KONG GATEWAY (Docker)                         │   │
│  │              DB-less mode, declarative config                    │   │
│  │              Proxy: :8000    Admin: :8001                       │   │
│  └────────────────────────────┬────────────────────────────────────┘   │
│                               │ (via host.docker.internal)              │
│                               ▼                                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │               MICROSERVICES (Docker Compose)                     │   │
│  │                                                                   │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐              │   │
│  │  │   Backend   │  │    Auth     │  │    Post     │              │   │
│  │  │   Service   │  │   Service   │  │   Service   │              │   │
│  │  │  :8080      │  │  :8081      │  │  :8082      │              │   │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘              │   │
│  │         │                │                │                       │   │
│  │         ▼                ▼                ▼                       │   │
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

- **Docker Desktop** - For running all microservices, databases, and Kong Gateway
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
# 1. Start backend (DB + app)
cd backend && docker compose up -d --build

# 2. Start auth-service (DB + app)
cd ../auth-service && docker compose up -d --build

# 3. Start post-service (DB + app)
cd ../post-service && docker compose up -d --build

# 4. Start Kong Gateway
cd ../kong && docker compose up -d

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

## Docker Containers

| Container | Service | Port |
|-----------|---------|------|
| `real-estate-postgres` | Backend PostgreSQL | 5432 |
| `real-estate-backend` | Backend Spring Boot | 8080 |
| `auth-db` | Auth PostgreSQL | 5433 |
| `auth-service` | Auth Spring Boot | 8081 |
| `post-db` | Post PostgreSQL | 5434 |
| `post-service` | Post Spring Boot | 8082 |
| `kong-gateway` | Kong API Gateway | 8000, 8001 |

## Scripts Reference

### Master Scripts (in `scripts/` directory)

| Script | Description |
|--------|-------------|
| `start-all-services.sh` | Build and start all services via Docker Compose, start Kong, optionally start frontend |
| `status-all-services.sh` | Check status of all Docker containers and service health |
| `stop-all-services.sh` | Stop services (with options for partial/full cleanup) |

### Kong Gateway Scripts (`kong/`)

| Script | Description |
|--------|-------------|
| `docker-start.sh` | Start Kong API Gateway via Docker Compose (DB-less mode) |
| `docker-status.sh` | Check Kong container and admin API status |
| `docker-stop.sh` | Stop Kong container |

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

# Docker containers
docker ps

# Kong only
cd kong && ./docker-status.sh
```

### View Logs
```bash
# Backend
docker logs -f real-estate-backend

# Auth Service
docker logs -f auth-service

# Post Service
docker logs -f post-service

# Kong
docker logs -f kong-gateway

# Database logs
docker logs -f real-estate-postgres
docker logs -f auth-db
docker logs -f post-db
```

### Restart a Service
```bash
# Restart backend app only (keeps DB running)
cd backend && docker compose restart backend

# Restart auth-service app only
cd auth-service && docker compose restart auth-service

# Restart post-service app only
cd post-service && docker compose restart post-service

# Restart Kong
cd kong && docker compose restart

# Rebuild and restart (after code changes)
cd backend && docker compose up -d --build
```

### Kong Gateway Management
```bash
# Start Kong
cd kong && ./docker-start.sh

# Check Kong status
cd kong && ./docker-status.sh

# Stop Kong
cd kong && ./docker-stop.sh

# View Kong routes
curl http://localhost:8001/routes | jq

# View Kong services
curl http://localhost:8001/services | jq

# View Kong plugins
curl http://localhost:8001/plugins | jq

# Kong logs
docker logs -f kong-gateway
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

### Stop Services

```bash
# Stop all (keep data)
./scripts/stop-all-services.sh
# Choose option 2

# Stop apps only (keep DBs running)
./scripts/stop-all-services.sh
# Choose option 1
```

### Full Reset
```bash
./scripts/stop-all-services.sh
# Choose option 4 and type DELETE

# Or manually:
cd backend && docker compose down -v
cd ../auth-service && docker compose down -v
cd ../post-service && docker compose down -v
cd ../kong && docker compose down
```

## Troubleshooting

### Service Not Starting

1. Check container status:
   ```bash
   docker ps -a
   ```

2. Check logs:
   ```bash
   docker logs real-estate-backend
   docker logs auth-service
   docker logs post-service
   ```

3. Check if port is in use:
   ```bash
   lsof -i :8080
   lsof -i :8081
   lsof -i :8082
   ```

4. Rebuild from scratch:
   ```bash
   cd backend && docker compose down && docker compose up -d --build
   ```

### Kong Not Starting

1. Check container status:
   ```bash
   docker ps -a --filter "name=kong-gateway"
   ```

2. Check logs:
   ```bash
   docker logs kong-gateway
   ```

3. Verify config:
   ```bash
   curl http://localhost:8001/status
   curl http://localhost:8001/routes | jq
   ```

### Database Connection Issues

1. Verify database is running:
   ```bash
   docker ps | grep -E "postgres|auth-db|post-db"
   ```

2. Test database connection:
   ```bash
   docker exec real-estate-postgres pg_isready -U postgres
   docker exec auth-db pg_isready -U postgres
   docker exec post-db pg_isready -U postgres
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

### First Build Is Slow

The first `docker compose up --build` for each service runs a full Maven build inside Docker (multi-stage Dockerfile). This can take several minutes. Subsequent builds use Docker layer caching and are much faster.

## Environment Configuration

### Frontend (.env.local)
```bash
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-key-here-change-in-production-min-32-characters-long
NEXT_PUBLIC_KONG_URL=http://127.0.0.1:8000
NEXT_PUBLIC_API_URL=http://127.0.0.1:8000/api
NEXT_PUBLIC_AUTH_API_URL=http://127.0.0.1:8000
```

### Kong Configuration
Kong runs in **DB-less mode** locally using a declarative config file at `kong/config/kong-local.yaml`. This file defines all services, routes, and plugins. Services point to `host.docker.internal` to reach backend services running on the host via Docker port mappings.

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

**Last Updated:** 2026-02-05
