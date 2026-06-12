# Real Estate Application - Monorepo

Full-stack real estate platform: a Next.js frontend behind a Kong API gateway, backed by eight Spring Boot microservices with Kafka-based analytics.

## 🏗️ Architecture

```
Browser ──▶ Next.js Frontend (3000)
                 │
                 ▼
        Kong API Gateway (8000)          JWT validation, rate limiting, CORS
                 │
   ┌─────────┬───┴─────┬─────────┬──────────┬─────────┬───────────┬──────────┐
   ▼         ▼         ▼         ▼          ▼         ▼           ▼          ▼
 AUTH    PROPERTY    POST     PRICE       NEWS    AI SEARCH    COMMENT   ANALYTICS
 8081      8080      8082      8084       8085      8086         8087       8083
   │         │         │         │          │                      │          ▲
   ▼         ▼         ▼         ▼          ▼                      ▼          │
 authdb  propertydb  postdb   pricedb    newsdb               commentsdb   MongoDB
 (5433)  (5432)      (5434)   (5435)     (5436)                 (5437)     (27017)
            │ ▲                  ▲
            │ └── gRPC ──────────┘
            ▼
          Redis (6379)        Kafka (29092) ◀── user-activity events from
                                                auth / property / post / price
```

- **Kong routes**: `/auth/*` → auth · `/api/properties/*`, `/api/upload/*`, `/api/admin/*` → property · `/api/posts/*` → post · `/api/prices/*` → price · `/api/news/*` → news · `/api/ai-search/*` → ai-search · `/api/comments/*`, `/api/properties/{id}/comments`, `/api/captcha` → comment · `/.well-known/jwks.json` → auth
- Full details: [docs/DEPLOYMENT_ARCHITECTURE.md](docs/DEPLOYMENT_ARCHITECTURE.md)

## 🧩 Services

| Service | Port | Database | Purpose |
|---------|------|----------|---------|
| `frontend` | 3000 | — | Next.js 14 App Router UI (vi/en i18n, NextAuth) |
| `kong` | 8000 / 8001 | — | API gateway (proxy / admin), JWT validation via JWKS |
| `auth-service` | 8081 | PostgreSQL :5433 | Users, JWT issuing/refresh, JWKS, key rotation |
| `property-service` | 8080 | PostgreSQL :5432 + Redis :6379 | Property CRUD, image uploads, search, cached reads, gRPC client to price-service |
| `post-service` | 8082 | PostgreSQL :5434 | Social feed: posts (text + up to 10 images), likes, single-level replies |
| `analytics-service` | 8083 | MongoDB :27017 | Consumes user-activity events from Kafka :29092 |
| `price-service` | 8084 (gRPC 9090) | PostgreSQL :5435 | Price management, price history, gRPC server |
| `news-service` | 8085 | PostgreSQL :5436 | Real-estate news/articles, public reads, admin CRUD |
| `ai-search-service` | 8086 | — | Natural language → structured property filters (regex default, optional LLM) |
| `comment-service` | 8087 | PostgreSQL :5437 | Property comment threads with replies, likes, captcha, admin moderation |

All Java services: Spring Boot 3.2.x, Java 17, Maven, Spring Data JPA, Flyway migrations, one Docker Compose stack each (app + its database).

Analytics is fire-and-forget: producers send on a dedicated daemon thread with a bounded queue and a 2s `max.block.ms`, so a Kafka outage never blocks request handling.

## 📁 Repository Structure

```
real-estate-monorepo/
├── frontend/             # Next.js 14 app (App Router, Tailwind, NextAuth, next-intl)
├── auth-service/         # Each service: src/, pom.xml, Dockerfile,
├── property-service/     #   docker-compose.yml, helm/ chart
├── post-service/
├── price-service/
├── news-service/
├── ai-search-service/
├── comment-service/
├── analytics-service/
├── kong/                 # Gateway config, docker/k8s start scripts
├── kafka/                # Kafka (KRaft) docker-compose
├── grpc-proto/           # Shared protobuf definitions (property ↔ price)
├── scripts/              # start/stop/status-all-services.sh, RDS helpers
├── docs/                 # Architecture docs, PRPs, Postman collection
├── jenkins/              # CI/CD pipelines (Jenkinsfile.ci / .cd)
├── monitoring/           # Prometheus/Grafana helm charts
└── terraform/            # AWS infrastructure (EKS, RDS, MSK)
```

## 🏃 Getting Started

### Prerequisites

- Docker Desktop
- Node.js 18+
- Java 17 + Maven (for building services locally)

### Start everything

```bash
# Start Kong, all microservices, databases, Kafka, and MongoDB (Docker)
./scripts/start-all-services.sh

# Check status / stop
./scripts/status-all-services.sh
./scripts/stop-all-services.sh
```

### Start the frontend

```bash
cd frontend
npm install
npm run dev          # or ./start-dev.sh
```

Open `http://localhost:3000`. The frontend talks to the services through Kong at `http://localhost:8000`.

### Run a single service

```bash
cd post-service
docker compose up -d --build    # app + its database
# or, against an already-running database:
mvn spring-boot:run
```

See [docs/LOCAL_DEVELOPMENT_GUIDE.md](docs/LOCAL_DEVELOPMENT_GUIDE.md) for the full local workflow.

## 📋 Features

- ✅ Property listings: search, filters, map view (Leaflet), image galleries with fullscreen lightbox
- ✅ AI-powered natural-language property search
- ✅ Authentication: register/login with JWT (RS256 + JWKS), token refresh, key rotation
- ✅ Social feed: posts with image attachments, likes, single-level replies, infinite scroll
- ✅ Property comments: threads, replies, likes, captcha, admin moderation
- ✅ News section with admin-managed articles
- ✅ Vietnamese/English i18n with cookie-based switcher
- ✅ User activity analytics via Kafka → MongoDB
- ✅ Price history with gRPC property↔price integration
- ✅ Observability: Spring Actuator + Prometheus metrics, Grafana dashboards

## 🔗 API

All client traffic goes through Kong (`http://localhost:8000`). Key route groups:

| Prefix | Service | Notes |
|--------|---------|-------|
| `/auth/*` | auth-service | Public: register, login, refresh; rate-limited 10/min |
| `/api/properties/*` | property-service | Public reads, JWT-protected writes |
| `/api/upload/*` | property-service | Image uploads (also used for post images) |
| `/api/posts/*` | post-service | Feed, posts, `{id}/like`, `{id}/replies` |
| `/api/prices/*` | price-service | Price reads/updates |
| `/api/news/*` | news-service | Public reads, admin CRUD |
| `/api/ai-search/*` | ai-search-service | NL query parsing |
| `/api/comments/*`, `/api/properties/{id}/comments` | comment-service | Comment threads |

Import [docs/postman/](docs/postman/) into Postman for a ready-to-use collection, or browse each service's Swagger UI at `http://localhost:<port>/swagger-ui.html`.

## 📚 Documentation

| Doc | Contents |
|-----|----------|
| [DEPLOYMENT_ARCHITECTURE.md](docs/DEPLOYMENT_ARCHITECTURE.md) | Full system diagrams, request flows, route tables |
| [LOCAL_DEVELOPMENT_GUIDE.md](docs/LOCAL_DEVELOPMENT_GUIDE.md) | Day-to-day development workflow |
| [DEPLOYMENT.md](docs/DEPLOYMENT.md) / [K8S_DEPLOYMENT_GUIDE.md](docs/K8S_DEPLOYMENT_GUIDE.md) | Production / Kubernetes deployment |
| [SECURITY_CHECKLIST.md](docs/SECURITY_CHECKLIST.md) | Security posture |
| `docs/*_PRP.md`, `docs/*_PRD.md` | Feature proposals and their implementation status |
| [jenkins/CI_CD_GUIDE.md](jenkins/CI_CD_GUIDE.md) | CI/CD pipelines |
| [terraform/README.md](terraform/README.md) | AWS infrastructure |

## 🧪 Testing

```bash
# Any Java service
cd post-service && mvn test

# Frontend type-check / lint / build
cd frontend && npx tsc --noEmit && npm run lint && npm run build
```

## 📄 License

Private project
