# Local Development Guide

This guide explains how to start, stop, and manage all services for the Real Estate Platform in a local development environment.

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                          LOCAL DEVELOPMENT SETUP                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                               │
│  ┌────────────────────────────────────────────────────────────────────┐      │
│  │                    FRONTEND (Host Machine)                          │      │
│  │                 Next.js @ http://localhost:3000                     │      │
│  └───────────────────────────┬────────────────────────────────────────┘      │
│                              │                                                │
│                              ▼                                                │
│  ┌────────────────────────────────────────────────────────────────────┐      │
│  │                    KONG GATEWAY (Docker)                            │      │
│  │              DB-less mode, declarative config                       │      │
│  │              Proxy: :8000    Admin: :8001                          │      │
│  └───────────────────────────┬────────────────────────────────────────┘      │
│                              │ (via host.docker.internal)                     │
│                              ▼                                                │
│  ┌────────────────────────────────────────────────────────────────────┐      │
│  │                MICROSERVICES (Docker Compose)                       │      │
│  │                                                                     │      │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │      │
│  │  │ Property │ │   Auth   │ │   Post   │ │Analytics │ │  Price   │ │      │
│  │  │ Service  │ │ Service  │ │ Service  │ │ Service  │ │ Service  │ │      │
│  │  │  :8080   │ │  :8081   │ │  :8082   │ │  :8083   │ │  :8084   │ │      │
│  │  │          │ │          │ │          │ │          │ │gRPC:9090 │ │      │
│  │  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ │      │
│  │       │            │            │             │            │       │      │
│  │       │       Kafka events ─────┼─────────────▶│◀───────────┘       │      │
│  │       │   gRPC ─────────────────┼─────────────────────────▶│       │      │
│  │       │            │            │             │            │       │      │
│  │       ▼            ▼            ▼             ▼            ▼       │      │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ │      │
│  │  │Property  │ │ Auth DB  │ │ Post DB  │ │  Kafka   │ │ Price DB │ │      │
│  │  │DB :5432  │ │  :5433   │ │  :5434   │ │  :29092  │ │  :5435   │ │      │
│  │  │realestate│ │  authdb  │ │  postdb  │ │(KRaft)   │ │ pricedb  │ │      │
│  │  │db        │ │          │ │          │ ├──────────┤ │          │ │      │
│  │  └──────────┘ └──────────┘ └──────────┘ │ MongoDB  │ └──────────┘ │      │
│  │                                          │  :27017  │             │      │
│  │                                          │analytics │             │      │
│  │                                          │db        │             │      │
│  │                                          └──────────┘             │      │
│  └────────────────────────────────────────────────────────────────────┘      │
│                                                                               │
└──────────────────────────────────────────────────────────────────────────────┘
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
# 1. Start property-service (DB + app)
cd property-service && docker compose up -d --build

# 2. Start auth-service (DB + app)
cd ../auth-service && docker compose up -d --build

# 3. Start post-service (DB + app)
cd ../post-service && docker compose up -d --build

# 4. Start analytics-service (Kafka + MongoDB + app)
cd ../analytics-service && docker compose up -d --build

# 5. Start price-service (DB + app + gRPC)
cd ../price-service && docker compose up -d --build

# 6. Start news-service (DB + app)
cd ../news-service && docker compose up -d --build

# 7. Start ai-search-service (stateless, regex parser by default; set
#    AI_SEARCH_PARSER_MODE=llm + ANTHROPIC_API_KEY=... to use Claude)
cd ../ai-search-service && docker compose up -d --build

# 8. Start comment-service (DB + app)
cd ../comment-service && docker compose up -d --build

# 9. Start Kong Gateway
cd ../kong && docker compose up -d

# 10. Start frontend (set NEXT_PUBLIC_AI_SEARCH=1 in .env.local for the AI bar)
cd ../frontend && npm run dev
```

## Service URLs

| Service | URL | Description |
|---------|-----|-------------|
| Frontend | http://localhost:3000 | Next.js application |
| Kong Gateway | http://localhost:8000 | API Gateway (routes all API traffic) |
| Property Service | http://localhost:8080 | Property service (direct access) |
| Auth Service | http://localhost:8081 | Authentication service (direct access) |
| Post Service | http://localhost:8082 | Social feed service (direct access) |
| Analytics Service | http://localhost:8083 | Analytics service (direct access) |
| Price Service | http://localhost:8084 | Price service (direct access) |
| Price Service gRPC | localhost:9090 | gRPC endpoint (used by property-service) |
| News Service | http://localhost:8085 | News service (direct access) |
| AI Search Service | http://localhost:8086 | Natural-language query parser (regex / LLM) |
| Comment Service | http://localhost:8087 | Property comments service (direct access) |
| Kong Admin | http://localhost:8001 | Kong admin API |
| Kafka | localhost:29092 | Event broker (external listener) |
| MongoDB | localhost:27017 | Analytics event store (analyticsdb) |

## Database Connections

| Database | Host | Port | Database | User | Password |
|----------|------|------|----------|------|----------|
| Property DB | localhost | 5432 | realestatedb | postgres | postgres |
| Auth DB | localhost | 5433 | authdb | postgres | postgres |
| Post DB | localhost | 5434 | postdb | postgres | postgres |
| Price DB | localhost | 5435 | pricedb | postgres | postgres |
| News DB | localhost | 5436 | newsdb | postgres | postgres |
| Comment DB | localhost | 5437 | commentsdb | postgres | postgres |
| MongoDB | localhost | 27017 | analyticsdb | (none) | (none) |

> ai-search-service is stateless — no database.

Connect with psql:
```bash
# Property DB
psql -h localhost -p 5432 -U postgres -d realestatedb

# Auth DB
psql -h localhost -p 5433 -U postgres -d authdb

# Post DB
psql -h localhost -p 5434 -U postgres -d postdb

# Price DB
psql -h localhost -p 5435 -U postgres -d pricedb

# News DB
psql -h localhost -p 5436 -U postgres -d newsdb

# Comment DB
psql -h localhost -p 5437 -U postgres -d commentsdb

# MongoDB (Analytics)
docker exec analytics-mongo mongosh analyticsdb
```

## Docker Containers

| Container | Service | Port |
|-----------|---------|------|
| `property-db` | Property PostgreSQL | 5432 |
| `property-service` | Property Spring Boot | 8080 |
| `auth-db` | Auth PostgreSQL | 5433 |
| `auth-service` | Auth Spring Boot | 8081 |
| `post-db` | Post PostgreSQL | 5434 |
| `post-service` | Post Spring Boot | 8082 |
| `analytics-kafka` | Apache Kafka (KRaft) | 29092 |
| `analytics-mongo` | MongoDB 7.0 | 27017 |
| `analytics-service` | Analytics Spring Boot | 8083 |
| `price-db` | Price PostgreSQL | 5435 |
| `price-service` | Price Spring Boot | 8084, 9090 (gRPC) |
| `news-db` | News PostgreSQL | 5436 |
| `news-service` | News Spring Boot | 8085 |
| `ai-search-service` | AI Search Spring Boot (stateless) | 8086 |
| `comment-db` | Comment PostgreSQL | 5437 |
| `comment-service` | Comment Spring Boot | 8087 |
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
# Property Service
docker logs -f property-service

# Auth Service
docker logs -f auth-service

# Post Service
docker logs -f post-service

# Analytics Service
docker logs -f analytics-service

# Price Service
docker logs -f price-service

# News Service
docker logs -f news-service

# AI Search Service
docker logs -f ai-search-service

# Comment Service
docker logs -f comment-service

# Kong
docker logs -f kong-gateway

# Database / Kafka logs
docker logs -f property-db
docker logs -f auth-db
docker logs -f post-db
docker logs -f analytics-kafka
```

### Restart a Service
```bash
# Restart property-service app only (keeps DB running)
cd property-service && docker compose restart property-service

# Restart auth-service app only
cd auth-service && docker compose restart auth-service

# Restart post-service app only
cd post-service && docker compose restart post-service

# Restart analytics-service app only (keeps Kafka running)
cd analytics-service && docker compose restart analytics-service

# Restart price-service app only (keeps DB running)
cd price-service && docker compose restart price-service

# Restart news-service app only (keeps DB running)
cd news-service && docker compose restart news-service

# Restart ai-search-service (stateless — pick up env var changes like AI_SEARCH_PARSER_MODE)
cd ai-search-service && docker compose restart ai-search-service

# Restart comment-service app only (keeps DB running)
cd comment-service && docker compose restart comment-service

# Restart Kong
cd kong && docker compose restart

# Rebuild and restart (after code changes)
cd property-service && docker compose up -d --build
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
cd property-service && docker compose down -v
cd ../auth-service && docker compose down -v
cd ../post-service && docker compose down -v
cd ../analytics-service && docker compose down -v
cd ../price-service && docker compose down -v
cd ../news-service && docker compose down -v
cd ../ai-search-service && docker compose down -v
cd ../comment-service && docker compose down -v
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
   docker logs property-service
   docker logs auth-service
   docker logs post-service
   docker logs analytics-service
   docker logs price-service
   docker logs news-service
   docker logs ai-search-service
   docker logs comment-service
   ```

3. Check if port is in use:
   ```bash
   lsof -i :8080
   lsof -i :8081
   lsof -i :8082
   lsof -i :8083
   lsof -i :8084
   lsof -i :8085
   lsof -i :8086
   lsof -i :8087
   ```

4. Rebuild from scratch:
   ```bash
   cd property-service && docker compose down && docker compose up -d --build
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
   docker exec property-db pg_isready -U postgres
   docker exec auth-db pg_isready -U postgres
   docker exec post-db pg_isready -U postgres
   docker exec price-db pg_isready -U postgres
   docker exec news-db pg_isready -U postgres
   docker exec comment-db pg_isready -U postgres
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

# Show the natural-language AI search bar on /buy, /rent, /search.
NEXT_PUBLIC_AI_SEARCH=1
```

### AI Search Service (parser mode switch)

`ai-search-service` is stateless and chooses its parser at startup via env vars:

```bash
# Default — in-process regex, $0 per query, no API key needed
AI_SEARCH_PARSER_MODE=regex

# Use Claude Haiku 4.5 for broader coverage (~$0.001–0.002 per query)
AI_SEARCH_PARSER_MODE=llm
AI_SEARCH_PARSER_FALLBACK=regex          # silently fall back to regex on LLM error
ANTHROPIC_API_KEY=sk-ant-...              # required when MODE=llm
```

Set these in your shell or in `ai-search-service/.env` before running
`docker compose up -d --build` (or `cd ai-search-service && docker compose restart`).

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

# AI Search (regex parser, public)
curl -X POST http://localhost:8000/api/ai-search/parse \
  -H "Content-Type: application/json" \
  -d '{"query":"a house near the beach with 3 bedrooms, max 5 billion vnd","locale":"en"}'

# News (public reads)
curl http://localhost:8000/api/news

# Comments on a property (public read)
curl http://localhost:8000/api/properties/1/comments

# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8085/actuator/health
curl http://localhost:8086/actuator/health
curl http://localhost:8087/actuator/health
```

---

**Last Updated:** 2026-05-10
