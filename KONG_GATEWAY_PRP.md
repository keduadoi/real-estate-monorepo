# Product Requirements Proposal (PRP)
# Kong API Gateway Integration for Real Estate Application

**Version**: 1.0
**Date**: 2026-01-26
**Author**: Engineering Team
**Status**: Draft - Pending Review

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Goals and Objectives](#3-goals-and-objectives)
4. [Proposed Solution](#4-proposed-solution)
5. [Technical Architecture](#5-technical-architecture)
6. [Feature Requirements](#6-feature-requirements)
7. [Implementation Plan](#7-implementation-plan)
8. [Configuration Specifications](#8-configuration-specifications)
9. [Security Considerations](#9-security-considerations)
10. [Monitoring and Observability](#10-monitoring-and-observability)
11. [Performance Requirements](#11-performance-requirements)
12. [Deployment Strategy](#12-deployment-strategy)
13. [Risk Assessment](#13-risk-assessment)
14. [Success Metrics](#14-success-metrics)
15. [Dependencies and Prerequisites](#15-dependencies-and-prerequisites)
16. [Future Considerations](#16-future-considerations)

---

## 1. Executive Summary

### 1.1 Overview

This PRP outlines the integration of Kong API Gateway into the Real Estate application infrastructure. Kong will serve as the centralized entry point for all API traffic, providing authentication, rate limiting, request routing, and observability capabilities.

### 1.2 Current State

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│    Frontend     │  HTTP   │    Backend      │  JDBC   │   PostgreSQL    │
│   (Next.js)     │────────▶│  (Spring Boot)  │────────▶│    Database     │
│    Vercel       │         │    Port 8080    │         │                 │
└─────────────────┘         └─────────────────┘         └─────────────────┘
                                    │
                            No Authentication
                            No Rate Limiting
                            No Centralized Logging
```

### 1.3 Proposed State

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│    Frontend     │  HTTPS  │   Kong Gateway  │  HTTP   │    Backend      │
│   (Next.js)     │────────▶│   Port 443/80   │────────▶│  (Spring Boot)  │
│    Vercel       │         │                 │         │    Port 8080    │
└─────────────────┘         │  - Auth/JWT     │         └────────┬────────┘
                            │  - Rate Limit   │                  │
                            │  - Logging      │                  │
                            │  - Metrics      │                  ▼
                            └─────────────────┘         ┌─────────────────┐
                                    │                   │   PostgreSQL    │
                                    ▼                   │    Database     │
                            ┌─────────────────┐         └─────────────────┘
                            │   PostgreSQL    │
                            │  (Kong Config)  │
                            └─────────────────┘
```

### 1.4 Key Benefits

| Benefit | Description |
|---------|-------------|
| **Security** | Centralized authentication, authorization, and threat protection |
| **Scalability** | Horizontal scaling of API layer independent of backend |
| **Observability** | Unified logging, metrics, and distributed tracing |
| **Rate Limiting** | Protection against abuse and DDoS attacks |
| **Flexibility** | Easy addition of new services without backend changes |

---

## 2. Problem Statement

### 2.1 Current Challenges

#### 2.1.1 Security Gaps
- **No Backend Authentication**: All API endpoints (`/api/*`) are publicly accessible without authentication
- **Unprotected Admin Endpoints**: Critical endpoints like `/api/admin/clear-database` are exposed
- **No Rate Limiting**: APIs vulnerable to abuse, scraping, and DDoS attacks
- **Inconsistent CORS**: CORS handled at application level, not at edge

#### 2.1.2 Operational Challenges
- **Lack of Centralized Logging**: No unified view of API traffic and errors
- **No API Versioning**: No strategy for managing API versions
- **Missing Metrics**: Limited visibility into API performance and usage
- **Direct Backend Exposure**: Backend service directly accessible

#### 2.1.3 Scalability Concerns
- **Single Point of Entry**: No load balancing or failover capability
- **No Circuit Breaking**: Cascading failures possible
- **Limited Traffic Management**: Cannot shape or control traffic patterns

### 2.2 Business Impact

| Impact Area | Current Risk Level | Description |
|-------------|-------------------|-------------|
| Security | **CRITICAL** | Data breach possible via unprotected endpoints |
| Availability | HIGH | No protection against traffic spikes or attacks |
| Compliance | HIGH | Missing audit trails and access controls |
| Operations | MEDIUM | Limited visibility into system behavior |

---

## 3. Goals and Objectives

### 3.1 Primary Goals

1. **Implement Centralized Security**
   - JWT-based authentication for all protected endpoints
   - Role-based access control (RBAC)
   - API key management for service-to-service communication

2. **Enable Traffic Management**
   - Rate limiting per user, IP, and API endpoint
   - Request/response transformation
   - Load balancing across backend instances

3. **Establish Observability**
   - Centralized request logging
   - Prometheus metrics export
   - Distributed tracing support

4. **Improve Operational Efficiency**
   - Zero-downtime deployments
   - Declarative configuration management
   - Self-service API management

### 3.2 Success Criteria

| Objective | Metric | Target |
|-----------|--------|--------|
| Security | Authentication coverage | 100% of protected endpoints |
| Performance | P99 latency overhead | < 10ms added latency |
| Availability | Gateway uptime | 99.9% |
| Observability | Log coverage | 100% of requests logged |

---

## 4. Proposed Solution

### 4.1 Why Kong Gateway?

#### 4.1.1 Comparison Matrix

| Feature | Kong OSS | Kong Enterprise | NGINX | Traefik | AWS API Gateway |
|---------|----------|-----------------|-------|---------|-----------------|
| Open Source | ✅ | Partial | ✅ | ✅ | ❌ |
| Kubernetes Native | ✅ | ✅ | Partial | ✅ | ❌ |
| Plugin Ecosystem | 100+ | 150+ | Limited | Limited | AWS Native |
| DB-less Mode | ✅ | ✅ | N/A | ✅ | N/A |
| Admin API | ✅ | ✅ | ❌ | ✅ | ✅ |
| Declarative Config | ✅ | ✅ | ✅ | ✅ | Partial |
| Rate Limiting | ✅ | ✅ | ✅ | ✅ | ✅ |
| JWT/OAuth | ✅ | ✅ | Lua | Plugin | ✅ |
| Prometheus Metrics | ✅ | ✅ | ✅ | ✅ | CloudWatch |
| Cost | Free | $$ | Free | Free | Pay-per-request |

#### 4.1.2 Selected Option: Kong OSS (Open Source)

**Rationale:**
- Rich plugin ecosystem covers all our requirements
- Native Kubernetes support via Kong Ingress Controller
- Declarative configuration via YAML (GitOps friendly)
- Large community and extensive documentation
- Zero licensing cost
- Easy upgrade path to Enterprise if needed

### 4.2 Kong Deployment Mode

#### Option A: Traditional Mode (with PostgreSQL) - **RECOMMENDED for Production**
```
Pros:
- Persistent configuration
- Multi-node clustering
- Full Admin API support
- Configuration backup/restore

Cons:
- Additional database dependency
- Slightly more complex setup
```

#### Option B: DB-less Mode (Declarative)
```
Pros:
- Simpler architecture
- GitOps native
- No database dependency
- Immutable configuration

Cons:
- No dynamic configuration via Admin API
- Configuration changes require restart
- No clustering state sync
```

**Decision**: Use **Traditional Mode with PostgreSQL** for production environments and **DB-less Mode** for local development.

---

## 5. Technical Architecture

### 5.1 High-Level Architecture

```
                                    ┌──────────────────────────────────────────────┐
                                    │              Kubernetes Cluster              │
                                    │                                              │
┌──────────────┐                    │  ┌────────────────────────────────────────┐ │
│              │                    │  │           Kong Namespace               │ │
│   Frontend   │                    │  │                                        │ │
│  (Next.js)   │    HTTPS/443       │  │  ┌──────────────────────────────────┐ │ │
│   Vercel     │───────────────────▶│  │  │        Kong Gateway              │ │ │
│              │                    │  │  │     (2+ replicas, HA)            │ │ │
└──────────────┘                    │  │  │                                  │ │ │
                                    │  │  │  Plugins:                        │ │ │
                                    │  │  │  ├─ JWT Authentication           │ │ │
                                    │  │  │  ├─ Rate Limiting               │ │ │
                                    │  │  │  ├─ Request Transformer         │ │ │
┌──────────────┐                    │  │  │  ├─ Prometheus Metrics          │ │ │
│   External   │                    │  │  │  ├─ File Log                    │ │ │
│   Clients    │───────────────────▶│  │  │  ├─ CORS                        │ │ │
│  (Mobile/3P) │    HTTPS/443       │  │  │  └─ IP Restriction (optional)  │ │ │
└──────────────┘                    │  │  └──────────────┬───────────────────┘ │ │
                                    │  │                 │                      │ │
                                    │  │                 ▼                      │ │
                                    │  │  ┌──────────────────────────────────┐ │ │
                                    │  │  │      Kong PostgreSQL             │ │ │
                                    │  │  │      (Configuration Store)       │ │ │
                                    │  │  └──────────────────────────────────┘ │ │
                                    │  └────────────────────────────────────────┘ │
                                    │                    │                        │
                                    │                    │ Internal Network       │
                                    │                    ▼                        │
                                    │  ┌────────────────────────────────────────┐ │
                                    │  │        Real Estate Namespace          │ │
                                    │  │                                        │ │
                                    │  │  ┌──────────────────────────────────┐ │ │
                                    │  │  │    Backend Service               │ │ │
                                    │  │  │    (Spring Boot - Port 8080)     │ │ │
                                    │  │  │                                  │ │ │
                                    │  │  │    /api/properties/*             │ │ │
                                    │  │  │    /api/upload/*                 │ │ │
                                    │  │  │    /api/admin/* (protected)      │ │ │
                                    │  │  │    /actuator/*                   │ │ │
                                    │  │  └──────────────┬───────────────────┘ │ │
                                    │  │                 │                      │ │
                                    │  │                 ▼                      │ │
                                    │  │  ┌──────────────────────────────────┐ │ │
                                    │  │  │      Application PostgreSQL      │ │ │
                                    │  │  │      (Application Data)          │ │ │
                                    │  │  └──────────────────────────────────┘ │ │
                                    │  └────────────────────────────────────────┘ │
                                    └──────────────────────────────────────────────┘
```

### 5.2 Network Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              REQUEST FLOW                                       │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  1. Client Request                                                              │
│     │                                                                           │
│     ▼                                                                           │
│  2. Kong Ingress (LoadBalancer/NodePort)                                        │
│     │                                                                           │
│     ▼                                                                           │
│  3. Kong Gateway                                                                │
│     ├─── Route Matching (path, host, headers)                                   │
│     ├─── Plugin Execution Chain:                                                │
│     │    ├─── CORS Plugin (preflight handling)                                  │
│     │    ├─── IP Restriction (if configured)                                    │
│     │    ├─── Rate Limiting (check limits)                                      │
│     │    ├─── JWT Plugin (validate token)                                       │
│     │    ├─── ACL Plugin (check permissions)                                    │
│     │    ├─── Request Transformer (add headers)                                 │
│     │    └─── Prometheus Plugin (record metrics)                                │
│     │                                                                           │
│     ▼                                                                           │
│  4. Upstream Service (Backend)                                                  │
│     │                                                                           │
│     ▼                                                                           │
│  5. Response                                                                    │
│     ├─── Response Transformer (if configured)                                   │
│     ├─── Prometheus Plugin (response metrics)                                   │
│     └─── File Log Plugin (access log)                                           │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 Kong Resource Model

```yaml
# Kong Entity Hierarchy
Kong:
  Services:                          # Backend services to proxy to
    - name: real-estate-backend
      url: http://backend-service.real-estate.svc:8080

  Routes:                            # How requests reach services
    - name: api-properties
      paths: ["/api/properties"]
      service: real-estate-backend

    - name: api-upload
      paths: ["/api/upload"]
      service: real-estate-backend

    - name: api-admin
      paths: ["/api/admin"]
      service: real-estate-backend

    - name: health-check
      paths: ["/actuator/health"]
      service: real-estate-backend

  Consumers:                         # API clients
    - username: frontend-app
      credentials:
        - type: jwt
          key: frontend-jwt-key

    - username: admin-user
      credentials:
        - type: jwt
          key: admin-jwt-key
      groups: [admin]

  Plugins:                           # Cross-cutting concerns
    - name: jwt                      # Authentication
    - name: acl                      # Authorization
    - name: rate-limiting            # Traffic control
    - name: prometheus               # Metrics
    - name: cors                     # CORS handling
    - name: file-log                 # Logging
```

---

## 6. Feature Requirements

### 6.1 Authentication & Authorization

#### 6.1.1 JWT Authentication

| Requirement | Description | Priority |
|-------------|-------------|----------|
| JWT Validation | Validate JWT tokens on protected routes | P0 |
| Multiple Issuers | Support tokens from NextAuth and future issuers | P1 |
| Token Claims | Extract and forward user claims to backend | P0 |
| Anonymous Access | Allow unauthenticated access to public endpoints | P0 |

**JWT Configuration:**
```yaml
plugins:
  - name: jwt
    config:
      claims_to_verify:
        - exp                        # Token expiration
      key_claim_name: iss            # Issuer claim
      secret_is_base64: false
      run_on_preflight: false        # Skip for OPTIONS requests
```

#### 6.1.2 Access Control Lists (ACL)

| Consumer Group | Allowed Routes | Description |
|----------------|----------------|-------------|
| `public` | `/api/properties` (GET), `/api/properties/search`, `/api/properties/cities` | Public property browsing |
| `authenticated` | All `/api/properties/*`, `/api/upload/*` | Logged-in users |
| `admin` | `/api/admin/*` | Administrative functions |

#### 6.1.3 API Key Authentication (Service-to-Service)

| Requirement | Description | Priority |
|-------------|-------------|----------|
| API Key Header | Accept `X-API-Key` header for service auth | P1 |
| Key Rotation | Support multiple active keys per consumer | P2 |
| Key Scoping | Limit keys to specific routes | P2 |

### 6.2 Rate Limiting

#### 6.2.1 Rate Limit Tiers

| Tier | Requests/Minute | Requests/Hour | Applied To |
|------|-----------------|---------------|------------|
| Anonymous | 30 | 500 | Unauthenticated requests |
| Authenticated | 100 | 3000 | Regular users |
| Premium | 500 | 15000 | Premium accounts (future) |
| Admin | 1000 | 30000 | Admin users |
| Service | 10000 | 300000 | Service-to-service |

#### 6.2.2 Rate Limit Configuration

```yaml
plugins:
  - name: rate-limiting
    config:
      minute: 100
      hour: 3000
      policy: redis                  # Use Redis for distributed limiting
      fault_tolerant: true           # Continue if Redis unavailable
      hide_client_headers: false     # Include X-RateLimit-* headers
      redis_host: redis-service
      redis_port: 6379
```

#### 6.2.3 Endpoint-Specific Limits

| Endpoint | Limit | Rationale |
|----------|-------|-----------|
| `POST /api/upload/*` | 10/minute | Prevent upload abuse |
| `POST /api/properties` | 20/hour | Prevent spam listings |
| `DELETE /api/admin/*` | 5/minute | Protect destructive operations |

### 6.3 Request/Response Transformation

#### 6.3.1 Request Headers

| Header | Value | Purpose |
|--------|-------|---------|
| `X-Request-ID` | `$(uuid)` | Distributed tracing |
| `X-Forwarded-For` | `$(client_ip)` | Original client IP |
| `X-Consumer-Username` | `$(consumer.username)` | User identification |
| `X-Consumer-Groups` | `$(consumer.groups)` | User roles |

#### 6.3.2 Response Headers

| Header | Value | Purpose |
|--------|-------|---------|
| `X-Kong-Request-Id` | Request ID | Tracing |
| `X-Response-Time` | Latency in ms | Performance monitoring |
| `X-RateLimit-Remaining` | Remaining quota | Client awareness |

### 6.4 CORS Configuration

```yaml
plugins:
  - name: cors
    config:
      origins:
        - https://your-frontend.vercel.app
        - http://localhost:3000
        - http://localhost:3001
      methods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
        - OPTIONS
      headers:
        - Authorization
        - Content-Type
        - X-Requested-With
        - X-API-Key
      exposed_headers:
        - X-RateLimit-Remaining
        - X-RateLimit-Limit
        - X-Kong-Request-Id
      credentials: true
      max_age: 3600
      preflight_continue: false
```

### 6.5 Health Checks

#### 6.5.1 Active Health Checks

```yaml
upstreams:
  - name: backend-upstream
    healthchecks:
      active:
        healthy:
          interval: 5              # Check every 5 seconds
          successes: 2             # 2 successes = healthy
          http_statuses: [200, 302]
        unhealthy:
          interval: 5
          http_failures: 3         # 3 failures = unhealthy
          http_statuses: [500, 502, 503]
        http_path: /actuator/health
        timeout: 3
```

#### 6.5.2 Passive Health Checks (Circuit Breaker)

```yaml
upstreams:
  - name: backend-upstream
    healthchecks:
      passive:
        healthy:
          successes: 5
          http_statuses: [200, 201, 204, 302]
        unhealthy:
          http_failures: 5
          http_statuses: [500, 502, 503]
          timeouts: 3
```

---

## 7. Implementation Plan

### 7.1 Phase Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         IMPLEMENTATION PHASES                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Phase 1: Foundation                                                            │
│  ├─── Kong Helm chart setup                                                     │
│  ├─── Basic routing configuration                                               │
│  ├─── Health check endpoints                                                    │
│  └─── Local development environment                                             │
│                                                                                 │
│  Phase 2: Security                                                              │
│  ├─── JWT authentication plugin                                                 │
│  ├─── ACL plugin configuration                                                  │
│  ├─── CORS plugin setup                                                         │
│  └─── Admin endpoint protection                                                 │
│                                                                                 │
│  Phase 3: Traffic Management                                                    │
│  ├─── Rate limiting plugin                                                      │
│  ├─── Redis deployment for rate limiting                                        │
│  ├─── Request/response transformation                                           │
│  └─── Upstream health checks                                                    │
│                                                                                 │
│  Phase 4: Observability                                                         │
│  ├─── Prometheus metrics plugin                                                 │
│  ├─── File/HTTP logging plugin                                                  │
│  ├─── Grafana dashboard setup                                                   │
│  └─── AlertManager rules                                                        │
│                                                                                 │
│  Phase 5: Production Hardening                                                  │
│  ├─── High availability setup (multi-replica)                                   │
│  ├─── SSL/TLS termination                                                       │
│  ├─── Performance tuning                                                        │
│  └─── Disaster recovery procedures                                              │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Detailed Phase Breakdown

#### Phase 1: Foundation

**Deliverables:**
1. Kong Helm chart with custom values
2. Kong namespace and RBAC
3. PostgreSQL database for Kong
4. Service and route definitions for backend
5. Basic proxy functionality working

**Files to Create:**
```
kong/
├── helm/
│   └── kong/
│       ├── Chart.yaml
│       ├── values.yaml
│       ├── values-local.yaml
│       ├── values-minikube.yaml
│       ├── values-staging.yaml
│       └── values-production.yaml
├── config/
│   ├── kong.yaml              # Declarative config
│   ├── services.yaml
│   └── routes.yaml
└── scripts/
    ├── install-kong.sh
    └── configure-kong.sh
```

#### Phase 2: Security

**Deliverables:**
1. JWT plugin configuration
2. ACL groups and permissions
3. Consumer definitions
4. CORS plugin configuration
5. Protected admin endpoints

**Configuration Files:**
```
kong/config/
├── plugins/
│   ├── jwt.yaml
│   ├── acl.yaml
│   └── cors.yaml
├── consumers/
│   ├── frontend-app.yaml
│   └── admin-user.yaml
└── credentials/
    └── jwt-secrets.yaml
```

#### Phase 3: Traffic Management

**Deliverables:**
1. Rate limiting plugin
2. Redis deployment for distributed rate limiting
3. Request transformer plugin
4. Upstream definitions with health checks
5. Circuit breaker configuration

**Configuration Files:**
```
kong/config/
├── plugins/
│   ├── rate-limiting.yaml
│   └── request-transformer.yaml
├── upstreams/
│   └── backend-upstream.yaml
└── redis/
    └── deployment.yaml
```

#### Phase 4: Observability

**Deliverables:**
1. Prometheus plugin configuration
2. File logging plugin
3. Grafana dashboards for Kong
4. AlertManager rules
5. Log aggregation setup

**Configuration Files:**
```
kong/config/
├── plugins/
│   ├── prometheus.yaml
│   └── file-log.yaml
└── monitoring/
    ├── grafana-dashboard.json
    └── alertmanager-rules.yaml
```

#### Phase 5: Production Hardening

**Deliverables:**
1. Multi-replica Kong deployment
2. SSL/TLS certificates configuration
3. Performance tuning (worker processes, connections)
4. Backup and restore procedures
5. Runbook documentation

---

## 8. Configuration Specifications

### 8.1 Kong Helm Values (Production)

```yaml
# kong/helm/kong/values-production.yaml

image:
  repository: kong
  tag: "3.5"
  pullPolicy: IfNotPresent

replicaCount: 3

resources:
  requests:
    cpu: 500m
    memory: 512Mi
  limits:
    cpu: 2000m
    memory: 2Gi

env:
  database: postgres
  pg_host: kong-postgresql
  pg_port: 5432
  pg_database: kong
  pg_user: kong
  # pg_password from secret

  nginx_worker_processes: auto
  nginx_proxy_read_timeout: 60s
  nginx_proxy_connect_timeout: 10s

  plugins: bundled,prometheus

proxy:
  enabled: true
  type: LoadBalancer
  http:
    enabled: true
    containerPort: 8000
    servicePort: 80
  tls:
    enabled: true
    containerPort: 8443
    servicePort: 443

admin:
  enabled: true
  type: ClusterIP
  http:
    enabled: true
    containerPort: 8001
    servicePort: 8001

postgresql:
  enabled: true
  auth:
    username: kong
    database: kong
    existingSecret: kong-postgresql-secret
  primary:
    persistence:
      enabled: true
      size: 10Gi

ingressController:
  enabled: true
  installCRDs: true

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

### 8.2 Kong Declarative Configuration

```yaml
# kong/config/kong.yaml

_format_version: "3.0"
_transform: true

services:
  - name: real-estate-backend
    url: http://backend-service.real-estate.svc.cluster.local:8080
    connect_timeout: 10000
    write_timeout: 60000
    read_timeout: 60000
    retries: 3

routes:
  # Public property endpoints
  - name: properties-public
    service: real-estate-backend
    paths:
      - /api/properties
    methods:
      - GET
    strip_path: false

  - name: properties-search
    service: real-estate-backend
    paths:
      - /api/properties/search
    methods:
      - POST
    strip_path: false

  - name: properties-cities
    service: real-estate-backend
    paths:
      - /api/properties/cities
    methods:
      - GET
    strip_path: false

  # Authenticated property endpoints
  - name: properties-authenticated
    service: real-estate-backend
    paths:
      - /api/properties
    methods:
      - POST
      - PUT
      - DELETE
    strip_path: false

  # Upload endpoints
  - name: upload
    service: real-estate-backend
    paths:
      - /api/upload
    strip_path: false

  # Admin endpoints
  - name: admin
    service: real-estate-backend
    paths:
      - /api/admin
    strip_path: false

  # Health check
  - name: health
    service: real-estate-backend
    paths:
      - /actuator/health
    methods:
      - GET
    strip_path: false

consumers:
  - username: frontend-app
    custom_id: frontend-nextjs

  - username: admin-user
    custom_id: admin-001

plugins:
  # Global CORS
  - name: cors
    config:
      origins:
        - https://your-frontend.vercel.app
        - http://localhost:3000
      methods:
        - GET
        - POST
        - PUT
        - DELETE
        - PATCH
        - OPTIONS
      headers:
        - Authorization
        - Content-Type
      credentials: true
      max_age: 3600

  # Global Prometheus metrics
  - name: prometheus
    config:
      per_consumer: true
      status_code_metrics: true
      latency_metrics: true
      bandwidth_metrics: true
      upstream_health_metrics: true

  # Global request ID
  - name: correlation-id
    config:
      header_name: X-Request-ID
      generator: uuid
      echo_downstream: true

  # JWT on authenticated routes
  - name: jwt
    route: properties-authenticated
    config:
      claims_to_verify:
        - exp
      key_claim_name: iss

  - name: jwt
    route: upload
    config:
      claims_to_verify:
        - exp

  - name: jwt
    route: admin
    config:
      claims_to_verify:
        - exp

  # ACL for admin routes
  - name: acl
    route: admin
    config:
      allow:
        - admin

  # Rate limiting
  - name: rate-limiting
    route: properties-public
    config:
      minute: 60
      policy: local

  - name: rate-limiting
    route: properties-authenticated
    config:
      minute: 100
      policy: local

  - name: rate-limiting
    route: upload
    config:
      minute: 10
      policy: local

  - name: rate-limiting
    route: admin
    config:
      minute: 30
      policy: local
```

### 8.3 Route Access Matrix

| Route | Methods | Auth Required | ACL Groups | Rate Limit |
|-------|---------|---------------|------------|------------|
| `/api/properties` | GET | No | - | 60/min |
| `/api/properties/search` | POST | No | - | 60/min |
| `/api/properties/cities` | GET | No | - | 60/min |
| `/api/properties` | POST, PUT, DELETE | Yes (JWT) | authenticated | 100/min |
| `/api/properties/{id}` | GET | No | - | 60/min |
| `/api/properties/user/{userId}` | GET | Yes (JWT) | authenticated | 100/min |
| `/api/upload/*` | POST | Yes (JWT) | authenticated | 10/min |
| `/api/admin/*` | ALL | Yes (JWT) | admin | 30/min |
| `/actuator/health` | GET | No | - | 120/min |

---

## 9. Security Considerations

### 9.1 Security Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         SECURITY LAYERS                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Layer 1: Network Security                                                      │
│  ├─── Kubernetes Network Policies                                               │
│  ├─── Namespace isolation                                                       │
│  └─── Service mesh (future: Istio/Linkerd)                                      │
│                                                                                 │
│  Layer 2: Edge Security (Kong)                                                  │
│  ├─── TLS termination                                                           │
│  ├─── IP restriction (optional)                                                 │
│  ├─── Rate limiting                                                             │
│  └─── Request validation                                                        │
│                                                                                 │
│  Layer 3: Authentication (Kong)                                                 │
│  ├─── JWT validation                                                            │
│  ├─── API key validation                                                        │
│  └─── Token claim extraction                                                    │
│                                                                                 │
│  Layer 4: Authorization (Kong + Backend)                                        │
│  ├─── ACL groups at Kong                                                        │
│  └─── Fine-grained RBAC at backend (future)                                     │
│                                                                                 │
│  Layer 5: Application Security (Backend)                                        │
│  ├─── Input validation                                                          │
│  ├─── SQL injection prevention (JPA)                                            │
│  └─── Output encoding                                                           │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 9.2 Secret Management

| Secret | Storage | Rotation |
|--------|---------|----------|
| Kong DB Password | Kubernetes Secret + External Secrets | 90 days |
| JWT Signing Keys | Kubernetes Secret + External Secrets | 30 days |
| API Keys | Kong Database (encrypted) | On-demand |
| TLS Certificates | cert-manager / AWS ACM | Auto-renewal |

### 9.3 Security Checklist

- [ ] Enable TLS 1.2+ only
- [ ] Configure HSTS headers
- [ ] Disable Kong Admin API external access in production
- [ ] Enable request body size limits
- [ ] Configure IP allowlisting for admin routes
- [ ] Set up audit logging for security events
- [ ] Implement JWT key rotation strategy
- [ ] Configure network policies to restrict pod communication
- [ ] Enable PodSecurityPolicy/PodSecurityStandards
- [ ] Regular security scanning of Kong image

### 9.4 Threat Model

| Threat | Mitigation | Kong Plugin |
|--------|------------|-------------|
| DDoS | Rate limiting, IP restriction | rate-limiting, ip-restriction |
| Unauthorized Access | JWT validation, ACL | jwt, acl |
| API Abuse | Rate limiting per consumer | rate-limiting |
| Injection Attacks | Request validation, WAF (future) | request-validator |
| Data Leakage | Response transformation | response-transformer |
| Replay Attacks | JWT expiration, nonce (future) | jwt |

---

## 10. Monitoring and Observability

### 10.1 Metrics

#### 10.1.1 Kong Prometheus Metrics

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `kong_http_requests_total` | Total requests | - |
| `kong_request_latency_ms` | Request latency | P99 > 500ms |
| `kong_upstream_latency_ms` | Backend latency | P99 > 300ms |
| `kong_bandwidth_bytes` | Bandwidth usage | - |
| `kong_http_status` | Status code counts | 5xx > 1% |
| `kong_nginx_connections_active` | Active connections | > 80% capacity |

#### 10.1.2 Business Metrics

| Metric | Description | Dashboard |
|--------|-------------|-----------|
| Requests per route | Traffic distribution | Kong Overview |
| Auth failures | Security monitoring | Security Dashboard |
| Rate limit hits | Abuse detection | Traffic Dashboard |
| Consumer usage | API usage per client | Consumer Dashboard |

### 10.2 Logging

#### 10.2.1 Log Format (JSON)

```json
{
  "timestamp": "2026-01-26T10:30:00.000Z",
  "request_id": "uuid-here",
  "client_ip": "192.168.1.100",
  "method": "POST",
  "path": "/api/properties",
  "status": 201,
  "latency_ms": 45,
  "upstream_latency_ms": 30,
  "consumer": "frontend-app",
  "route": "properties-authenticated",
  "service": "real-estate-backend",
  "request_size": 1024,
  "response_size": 512
}
```

#### 10.2.2 Log Destinations

| Environment | Destination | Retention |
|-------------|-------------|-----------|
| Local | stdout/file | Session |
| Development | CloudWatch Logs | 7 days |
| Staging | CloudWatch Logs | 14 days |
| Production | CloudWatch Logs + S3 | 90 days (S3: 1 year) |

### 10.3 Alerting Rules

```yaml
# AlertManager rules
groups:
  - name: kong-alerts
    rules:
      - alert: KongHighErrorRate
        expr: sum(rate(kong_http_status{code=~"5.."}[5m])) / sum(rate(kong_http_requests_total[5m])) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "Kong error rate is above 1%"

      - alert: KongHighLatency
        expr: histogram_quantile(0.99, sum(rate(kong_request_latency_ms_bucket[5m])) by (le)) > 500
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Kong P99 latency is above 500ms"

      - alert: KongRateLimitHits
        expr: sum(rate(kong_rate_limiting_exceeded_total[5m])) > 10
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High rate of rate limit hits detected"
```

### 10.4 Grafana Dashboards

1. **Kong Overview Dashboard**
   - Request rate, latency, error rate
   - Upstream health status
   - Active connections

2. **Kong Security Dashboard**
   - Auth success/failure rate
   - Rate limit hits per consumer
   - Blocked requests by IP/route

3. **Kong Consumer Dashboard**
   - Per-consumer request rates
   - Per-consumer error rates
   - Top consumers by request volume

---

## 11. Performance Requirements

### 11.1 Performance Targets

| Metric | Target | Maximum |
|--------|--------|---------|
| Added Latency (P50) | < 2ms | 5ms |
| Added Latency (P99) | < 10ms | 20ms |
| Throughput | 10,000 RPS | - |
| Connection Overhead | < 1ms | 2ms |
| Memory per Request | < 10KB | 50KB |

### 11.2 Performance Tuning

```yaml
# Kong environment variables for performance
env:
  # Worker processes
  nginx_worker_processes: auto

  # Connections
  nginx_worker_connections: 16384

  # Keep-alive
  nginx_http_keepalive_timeout: 65
  nginx_upstream_keepalive: 100
  nginx_upstream_keepalive_requests: 1000

  # Buffers
  nginx_proxy_buffer_size: 16k
  nginx_proxy_buffers: 4 32k
  nginx_proxy_busy_buffers_size: 64k

  # Timeouts
  nginx_proxy_connect_timeout: 10s
  nginx_proxy_send_timeout: 60s
  nginx_proxy_read_timeout: 60s

  # Caching
  db_cache_ttl: 3600
  db_resurrect_ttl: 30
```

### 11.3 Load Testing Plan

| Test Type | Target | Duration | Tool |
|-----------|--------|----------|------|
| Smoke Test | 100 RPS | 5 min | k6 |
| Load Test | 1000 RPS | 30 min | k6 |
| Stress Test | Ramp to 5000 RPS | 1 hour | k6 |
| Soak Test | 500 RPS | 24 hours | k6 |

---

## 12. Deployment Strategy

### 12.1 Environment Configuration

| Environment | Replicas | Resources | Database |
|-------------|----------|-----------|----------|
| Local | 1 | 256Mi/250m | In-memory (DB-less) |
| Development | 1 | 512Mi/500m | PostgreSQL (shared) |
| Staging | 2 | 1Gi/1000m | PostgreSQL (dedicated) |
| Production | 3+ | 2Gi/2000m | PostgreSQL (HA) |

### 12.2 Deployment Process

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         KONG DEPLOYMENT PROCESS                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  1. Pre-Deployment                                                              │
│     ├─── Run configuration validation (deck validate)                           │
│     ├─── Backup current configuration (deck dump)                               │
│     └─── Review changes (deck diff)                                             │
│                                                                                 │
│  2. Database Migration (if Traditional mode)                                    │
│     ├─── Run kong migrations up                                                 │
│     └─── Verify migration status                                                │
│                                                                                 │
│  3. Rolling Deployment                                                          │
│     ├─── Update one replica at a time                                           │
│     ├─── Wait for health checks to pass                                         │
│     └─── Monitor error rates during rollout                                     │
│                                                                                 │
│  4. Configuration Sync                                                          │
│     ├─── Apply new configuration (deck sync)                                    │
│     └─── Verify routes and plugins                                              │
│                                                                                 │
│  5. Post-Deployment                                                             │
│     ├─── Run smoke tests                                                        │
│     ├─── Monitor metrics for anomalies                                          │
│     └─── Verify all routes accessible                                           │
│                                                                                 │
│  6. Rollback (if needed)                                                        │
│     ├─── Restore previous configuration                                         │
│     ├─── Rollback Helm release                                                  │
│     └─── Rollback migrations (if applicable)                                    │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 12.3 Blue-Green Deployment (Optional)

For zero-downtime updates with configuration changes:

```
                    ┌──────────────────┐
                    │  Load Balancer   │
                    └────────┬─────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
              ▼                             ▼
     ┌────────────────┐           ┌────────────────┐
     │   Kong Blue    │           │   Kong Green   │
     │  (Current)     │           │  (New Version) │
     │    100%        │           │    0%          │
     └────────────────┘           └────────────────┘
              │                             │
              └──────────────┬──────────────┘
                             │
                             ▼
                    ┌────────────────┐
                    │    Backend     │
                    └────────────────┘
```

---

## 13. Risk Assessment

### 13.1 Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Kong becomes SPOF | Medium | High | Multi-replica, health checks, failover |
| Performance degradation | Low | Medium | Performance testing, monitoring, auto-scaling |
| Configuration errors | Medium | High | Validation, staging testing, rollback procedures |
| JWT key compromise | Low | Critical | Key rotation, short expiration, revocation |
| Database corruption | Low | High | Regular backups, replication |
| Plugin compatibility | Medium | Medium | Version pinning, testing in staging |

### 13.2 Mitigation Strategies

#### Single Point of Failure (SPOF)
- Deploy minimum 2 replicas in production
- Configure pod anti-affinity for spread across nodes
- Use health checks for automatic recovery
- Consider multi-zone/multi-region deployment

#### Performance
- Pre-production load testing
- Auto-scaling based on CPU/memory
- Caching configuration
- Regular performance benchmarking

#### Configuration
- GitOps workflow with PR reviews
- Automated validation in CI pipeline
- Staging environment for testing
- Automated rollback on failures

---

## 14. Success Metrics

### 14.1 Technical Metrics

| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| API Gateway Uptime | N/A (new) | 99.9% | Prometheus/CloudWatch |
| Auth Success Rate | N/A | > 99% | Kong metrics |
| P99 Latency (gateway) | N/A | < 10ms | Kong metrics |
| Config Deployment Time | N/A | < 5 min | CI/CD metrics |
| MTTR (Mean Time to Recovery) | N/A | < 15 min | Incident tracking |

### 14.2 Security Metrics

| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| Unprotected Endpoints | 100% | 0% | Route audit |
| Rate Limit Coverage | 0% | 100% | Kong config review |
| Security Incidents | Unknown | 0 critical | Security monitoring |
| Failed Auth Attempts | Unknown | < 5% | Kong metrics |

### 14.3 Operational Metrics

| Metric | Baseline | Target | Measurement |
|--------|----------|--------|-------------|
| Deployment Frequency | Manual | Daily capable | CI/CD metrics |
| Change Failure Rate | Unknown | < 5% | Deployment tracking |
| Log Coverage | 0% | 100% | Log audit |
| Alert Noise | Unknown | < 5 false positives/week | Alert review |

---

## 15. Dependencies and Prerequisites

### 15.1 Infrastructure Prerequisites

| Requirement | Status | Notes |
|-------------|--------|-------|
| Kubernetes Cluster | ✅ Available | EKS/Minikube |
| Helm 3.x | ✅ Available | v3.x required |
| PostgreSQL | ✅ Available | For Kong config (or use existing) |
| Redis | ❌ Required | For distributed rate limiting |
| External Secrets Operator | ❌ Recommended | For secret management |
| cert-manager | ❌ Recommended | For TLS certificates |

### 15.2 Application Prerequisites

| Requirement | Status | Notes |
|-------------|--------|-------|
| Backend JWT Validation | ❌ Required | Backend should validate forwarded claims |
| Health Check Endpoint | ✅ Available | /actuator/health |
| Remove Backend CORS | ⚠️ Recommended | After Kong CORS is enabled |
| Update Frontend API URL | ⚠️ Required | Point to Kong instead of backend |

### 15.3 Team Prerequisites

| Requirement | Description |
|-------------|-------------|
| Kubernetes Knowledge | Team familiarity with K8s operations |
| Kong Training | Basic Kong administration knowledge |
| Runbook Documentation | Operational procedures documentation |

---

## 16. Future Considerations

### 16.1 Short-term Enhancements

| Enhancement | Priority | Description |
|-------------|----------|-------------|
| OAuth2/OIDC Plugin | P1 | Support for OAuth2 providers (Google, Auth0) |
| Request Validation | P1 | OpenAPI spec-based request validation |
| Response Caching | P2 | Cache GET responses at gateway |
| GraphQL Support | P2 | If GraphQL API is added |

### 16.2 Medium-term Enhancements

| Enhancement | Priority | Description |
|-------------|----------|-------------|
| Kong Enterprise | P2 | For advanced features (Dev Portal, Analytics) |
| Service Mesh Integration | P2 | Integrate with Istio/Linkerd |
| API Versioning | P2 | Path-based or header-based versioning |
| Multi-region Deployment | P3 | Geo-distributed Kong instances |

### 16.3 Long-term Vision

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         FUTURE ARCHITECTURE VISION                              │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────┐                                                               │
│  │   Clients   │                                                               │
│  └──────┬──────┘                                                               │
│         │                                                                       │
│         ▼                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐           │
│  │                    Kong Gateway (Multi-Region)                  │           │
│  │  ├─ Global Load Balancing                                      │           │
│  │  ├─ Geo-based Routing                                          │           │
│  │  └─ Edge Caching                                               │           │
│  └─────────────────────────────────────────────────────────────────┘           │
│         │                                                                       │
│         ▼                                                                       │
│  ┌─────────────────────────────────────────────────────────────────┐           │
│  │                    Service Mesh (Istio/Linkerd)                 │           │
│  │  ├─ mTLS between services                                      │           │
│  │  ├─ Advanced traffic management                                │           │
│  │  └─ Observability (distributed tracing)                        │           │
│  └─────────────────────────────────────────────────────────────────┘           │
│         │                                                                       │
│         ├────────────────┬────────────────┬───────────────┐                    │
│         ▼                ▼                ▼               ▼                    │
│  ┌────────────┐   ┌────────────┐   ┌────────────┐  ┌────────────┐             │
│  │  Property  │   │   User     │   │  Payment   │  │   Search   │             │
│  │  Service   │   │  Service   │   │  Service   │  │  Service   │             │
│  └────────────┘   └────────────┘   └────────────┘  └────────────┘             │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Appendix A: Reference Documentation

### Kong Documentation
- [Kong Gateway Documentation](https://docs.konghq.com/gateway/)
- [Kong Helm Chart](https://github.com/Kong/charts)
- [Kong Plugin Hub](https://docs.konghq.com/hub/)

### Related Tools
- [decK - Kong declarative configuration](https://docs.konghq.com/deck/)
- [Insomnia - API testing](https://insomnia.rest/)
- [k6 - Load testing](https://k6.io/)

### Internal Documentation
- [DEPLOYMENT_ARCHITECTURE.md](./DEPLOYMENT_ARCHITECTURE.md)
- [HELM_VS_PLAIN_K8S.md](./HELM_VS_PLAIN_K8S.md)
- [CI_CD_GUIDE.md](./CI_CD_GUIDE.md)

---

## Appendix B: Glossary

| Term | Definition |
|------|------------|
| **Service** | Kong entity representing an upstream service |
| **Route** | Kong entity mapping requests to services |
| **Consumer** | Kong entity representing an API client |
| **Plugin** | Kong extension providing additional functionality |
| **Upstream** | Kong entity for load balancing |
| **decK** | Kong's declarative configuration tool |
| **DB-less Mode** | Kong running without a database |
| **Traditional Mode** | Kong running with PostgreSQL/Cassandra |

---

## Approval

| Role | Name | Signature | Date |
|------|------|-----------|------|
| Engineering Lead | | | |
| DevOps Lead | | | |
| Security Lead | | | |
| Product Owner | | | |

---

**Document Version History**

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-26 | Engineering Team | Initial draft |
