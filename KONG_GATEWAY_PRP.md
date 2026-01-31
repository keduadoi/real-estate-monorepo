# Product Requirements Proposal (PRP)
# Kong API Gateway & Auth Service Integration for Real Estate Application

**Version**: 2.0
**Date**: 2026-01-26
**Author**: Engineering Team
**Status**: Draft - Pending Review

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Problem Statement](#2-problem-statement)
3. [Goals and Objectives](#3-goals-and-objectives)
4. [Proposed Solution](#4-proposed-solution)
5. [Authentication Architecture](#5-authentication-architecture)
6. [Technical Architecture](#6-technical-architecture)
7. [Auth Service Specification](#7-auth-service-specification)
8. [Feature Requirements](#8-feature-requirements)
9. [Implementation Plan](#9-implementation-plan)
10. [Configuration Specifications](#10-configuration-specifications)
11. [Security Considerations](#11-security-considerations)
12. [Monitoring and Observability](#12-monitoring-and-observability)
13. [Performance Requirements](#13-performance-requirements)
14. [Deployment Strategy](#14-deployment-strategy)
15. [Risk Assessment](#15-risk-assessment)
16. [Success Metrics](#16-success-metrics)
17. [Dependencies and Prerequisites](#17-dependencies-and-prerequisites)
18. [Future Considerations](#18-future-considerations)

---

## 1. Executive Summary

### 1.1 Overview

This PRP outlines the integration of **Kong API Gateway** and a dedicated **Auth Service** (Spring Security) into the Real Estate application infrastructure. This hybrid approach provides:
- **Kong Gateway**: Centralized entry point with stateless JWT validation, rate limiting, and observability
- **Auth Service**: User management, token issuance, and authentication flows

### 1.2 Current State

```
┌─────────────────┐         ┌─────────────────┐         ┌─────────────────┐
│    Frontend     │  HTTP   │    Backend      │  JDBC   │   PostgreSQL    │
│   (Next.js)     │────────▶│  (Spring Boot)  │────────▶│    Database     │
│    Vercel       │         │    Port 8080    │         │                 │
└─────────────────┘         └─────────────────┘         └─────────────────┘
                                    │
                        ┌───────────┴───────────┐
                        │     CRITICAL GAPS     │
                        ├───────────────────────┤
                        │ ✗ No Authentication   │
                        │ ✗ No User Management  │
                        │ ✗ No Rate Limiting    │
                        │ ✗ No Centralized Logs │
                        │ ✗ Admin Unprotected   │
                        └───────────────────────┘
```

### 1.3 Proposed State (Hybrid Architecture)

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         HYBRID AUTHENTICATION ARCHITECTURE                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────┐                                                              │
│  │   Frontend   │                                                              │
│  │  (Next.js)   │                                                              │
│  │   Vercel     │                                                              │
│  └──────┬───────┘                                                              │
│         │                                                                       │
│         │ 1. Login/Register ─────────────────────┐                             │
│         │                                        │                             │
│         │ 2. API Calls + JWT Token               ▼                             │
│         │                               ┌─────────────────┐                    │
│         ▼                               │  Auth Service   │                    │
│  ┌─────────────────┐                    │ (Spring Security)│                   │
│  │  Kong Gateway   │                    │                 │                    │
│  │                 │   JWKS Endpoint    │ • User CRUD     │                    │
│  │ • JWT Validate  │◄───────────────────│ • Login/Register│                    │
│  │   (stateless)   │   (public keys)    │ • Token Issue   │                    │
│  │ • Rate Limit    │                    │ • Password Reset│                    │
│  │ • CORS          │                    └────────┬────────┘                    │
│  │ • Logging       │                             │                             │
│  └────────┬────────┘                             │                             │
│           │                                      ▼                             │
│           │                             ┌─────────────────┐                    │
│           │                             │    User DB      │                    │
│           │                             │  (PostgreSQL)   │                    │
│           │                             └─────────────────┘                    │
│           │                                                                    │
│           │  3. Forward + User Headers                                         │
│           │     X-User-Id, X-User-Roles                                        │
│           ▼                                                                    │
│  ┌─────────────────┐         ┌─────────────────┐                              │
│  │ Backend Service │  JDBC   │  Application DB │                              │
│  │  (Spring Boot)  │────────▶│  (PostgreSQL)   │                              │
│  │                 │         │                 │                              │
│  │ • Business Logic│         │ • Properties    │                              │
│  │ • Trust Headers │         │ • Images        │                              │
│  └─────────────────┘         └─────────────────┘                              │
│                                                                                │
└─────────────────────────────────────────────────────────────────────────────────┘

KEY POINTS:
• Auth Service issues JWT tokens (login/register)
• Kong validates tokens using Auth Service's public keys (NO call to Auth Service per request)
• Kong forwards user claims as headers to Backend
• Backend trusts Kong's headers (no JWT validation needed)
```

### 1.4 Key Benefits

| Benefit | Description |
|---------|-------------|
| **Security** | Centralized authentication with dedicated Auth Service |
| **Scalability** | Stateless JWT validation - no auth bottleneck |
| **User Management** | Full control over user database and authentication flows |
| **Performance** | Kong validates tokens locally using public keys |
| **Flexibility** | Easy to add OAuth2/OIDC providers in future |
| **Observability** | Unified logging, metrics, and distributed tracing |

### 1.5 Component Responsibilities

| Component | Responsibility |
|-----------|---------------|
| **Auth Service** | User registration, login, JWT issuance, password reset, user management |
| **Kong Gateway** | JWT validation (stateless), rate limiting, CORS, logging, routing |
| **Backend API** | Business logic only, trusts Kong headers, no auth logic |
| **Frontend** | Calls Auth Service for login, includes JWT in API requests |

---

## 2. Problem Statement

### 2.1 Current Challenges

#### 2.1.1 Security Gaps
- **No Backend Authentication**: All API endpoints (`/api/*`) are publicly accessible without authentication
- **No User Management**: No user database, registration, or login functionality
- **Unprotected Admin Endpoints**: Critical endpoints like `/api/admin/clear-database` are exposed
- **No Rate Limiting**: APIs vulnerable to abuse, scraping, and DDoS attacks
- **Mock Authentication**: NextAuth frontend uses mock user validation (NOT production-ready)

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
| User Management | **CRITICAL** | No real user accounts or authentication |
| Availability | HIGH | No protection against traffic spikes or attacks |
| Compliance | HIGH | Missing audit trails and access controls |
| Operations | MEDIUM | Limited visibility into system behavior |

---

## 3. Goals and Objectives

### 3.1 Primary Goals

1. **Implement User Management System**
   - User registration and login
   - Password management (reset, change)
   - Role-based user accounts (user, admin)
   - Secure token-based authentication

2. **Implement Centralized API Gateway**
   - JWT-based authentication validation
   - Role-based access control (RBAC)
   - API key management for service-to-service communication

3. **Enable Traffic Management**
   - Rate limiting per user, IP, and API endpoint
   - Request/response transformation
   - Load balancing across backend instances

4. **Establish Observability**
   - Centralized request logging
   - Prometheus metrics export
   - Distributed tracing support

### 3.2 Success Criteria

| Objective | Metric | Target |
|-----------|--------|--------|
| User Management | Registration/Login available | Fully functional |
| Security | Authentication coverage | 100% of protected endpoints |
| Performance | P99 latency overhead | < 10ms added latency (gateway) |
| Availability | Gateway + Auth Service uptime | 99.9% |
| Observability | Log coverage | 100% of requests logged |

---

## 4. Proposed Solution

### 4.1 Solution Overview: Hybrid Authentication

We will implement a **Hybrid Authentication Architecture** that combines:

1. **Auth Service** (Spring Security) - Handles user management and token issuance
2. **Kong Gateway** (OSS) - Handles stateless JWT validation and API management

This approach provides the best balance of:
- Full control over user management
- High performance (no auth service call per request)
- Scalability (stateless token validation)
- Security (dedicated authentication service)

### 4.2 Why Hybrid Over Alternatives?

#### Comparison of Authentication Approaches

| Aspect | Kong Only | Auth Service Only | Hybrid (Selected) |
|--------|-----------|-------------------|-------------------|
| **Latency** | ✅ Lowest | ❌ High (call/request) | ✅ Low (stateless) |
| **User Management** | ❌ External only | ✅ Full control | ✅ Full control |
| **Scalability** | ✅ Excellent | ⚠️ Bottleneck | ✅ Excellent |
| **Token Refresh** | ❌ Limited | ✅ Full control | ✅ Full control |
| **Custom Auth Flows** | ❌ Limited | ✅ Full flexibility | ✅ Full flexibility |
| **Offline Validation** | ✅ Yes | ❌ No | ✅ Yes |
| **Complexity** | ✅ Simple | ⚠️ Medium | ⚠️ Medium |

### 4.3 Why Kong Gateway?

| Feature | Kong OSS | Kong Enterprise | NGINX | Traefik | AWS API Gateway |
|---------|----------|-----------------|-------|---------|-----------------|
| Open Source | ✅ | Partial | ✅ | ✅ | ❌ |
| Kubernetes Native | ✅ | ✅ | Partial | ✅ | ❌ |
| Plugin Ecosystem | 100+ | 150+ | Limited | Limited | AWS Native |
| JWT Validation | ✅ | ✅ | Lua | Plugin | ✅ |
| JWKS Support | ✅ | ✅ | Manual | Plugin | ✅ |
| Rate Limiting | ✅ | ✅ | ✅ | ✅ | ✅ |
| Cost | Free | $$ | Free | Free | Pay-per-request |

**Decision**: Use **Kong OSS** with **Traditional Mode (PostgreSQL)** for production.

---

## 5. Authentication Architecture

### 5.1 Authentication Flow Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         AUTHENTICATION FLOW                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      1. USER LOGIN/REGISTRATION                         │   │
│  │                                                                         │   │
│  │   Frontend                        Auth Service                          │   │
│  │      │                                │                                 │   │
│  │      │  POST /auth/login              │                                 │   │
│  │      │  {email, password}             │                                 │   │
│  │      │───────────────────────────────▶│                                 │   │
│  │      │                                │  • Validate credentials         │   │
│  │      │                                │  • Generate JWT (RS256)         │   │
│  │      │                                │  • Generate Refresh Token       │   │
│  │      │◀───────────────────────────────│                                 │   │
│  │      │  {accessToken, refreshToken,   │                                 │   │
│  │      │   expiresIn, user}             │                                 │   │
│  │      │                                │                                 │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      2. API REQUEST WITH JWT                            │   │
│  │                                                                         │   │
│  │   Frontend              Kong Gateway              Backend               │   │
│  │      │                      │                        │                  │   │
│  │      │  GET /api/properties │                        │                  │   │
│  │      │  Authorization:      │                        │                  │   │
│  │      │  Bearer <JWT>        │                        │                  │   │
│  │      │─────────────────────▶│                        │                  │   │
│  │      │                      │                        │                  │   │
│  │      │                      │  JWT Validation:       │                  │   │
│  │      │                      │  • Verify signature    │                  │   │
│  │      │                      │    (using JWKS)        │                  │   │
│  │      │                      │  • Check expiration    │                  │   │
│  │      │                      │  • Extract claims      │                  │   │
│  │      │                      │                        │                  │   │
│  │      │                      │  Forward with headers: │                  │   │
│  │      │                      │  X-User-Id: 123        │                  │   │
│  │      │                      │  X-User-Email: a@b.com │                  │   │
│  │      │                      │  X-User-Roles: user    │                  │   │
│  │      │                      │───────────────────────▶│                  │   │
│  │      │                      │                        │  Process         │   │
│  │      │                      │                        │  request         │   │
│  │      │                      │◀───────────────────────│                  │   │
│  │      │◀─────────────────────│                        │                  │   │
│  │      │  Response            │                        │                  │   │
│  │                                                                         │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      3. TOKEN REFRESH                                   │   │
│  │                                                                         │   │
│  │   Frontend                        Auth Service                          │   │
│  │      │                                │                                 │   │
│  │      │  POST /auth/refresh            │                                 │   │
│  │      │  {refreshToken}                │                                 │   │
│  │      │───────────────────────────────▶│                                 │   │
│  │      │                                │  • Validate refresh token       │   │
│  │      │                                │  • Generate new access token    │   │
│  │      │◀───────────────────────────────│                                 │   │
│  │      │  {accessToken, expiresIn}      │                                 │   │
│  │                                                                         │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 5.2 Token Strategy

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              TOKEN STRATEGY                                     │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ACCESS TOKEN (JWT)                                                            │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  Algorithm:     RS256 (asymmetric - public/private key pair)            │   │
│  │  Expiration:    15 minutes                                              │   │
│  │  Issuer:        auth-service                                            │   │
│  │  Audience:      real-estate-api                                         │   │
│  │                                                                         │   │
│  │  Claims:                                                                │   │
│  │  {                                                                      │   │
│  │    "sub": "user-uuid-123",           // Subject (user ID)              │   │
│  │    "email": "user@example.com",      // User email                     │   │
│  │    "roles": ["user", "admin"],       // User roles                     │   │
│  │    "iss": "auth-service",            // Issuer                         │   │
│  │    "aud": "real-estate-api",         // Audience                       │   │
│  │    "iat": 1706270400,                // Issued at                      │   │
│  │    "exp": 1706271300                 // Expiration (15 min)            │   │
│  │  }                                                                      │   │
│  │                                                                         │   │
│  │  Validated by:  Kong Gateway (using JWKS public keys)                   │   │
│  │  Storage:       Frontend memory (NOT localStorage for XSS protection)   │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  REFRESH TOKEN                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  Type:          Opaque token (random UUID)                              │   │
│  │  Expiration:    7 days                                                  │   │
│  │  Storage:       Database (can be revoked)                               │   │
│  │  Purpose:       Obtain new access tokens without re-login               │   │
│  │                                                                         │   │
│  │  Validated by:  Auth Service only (database lookup)                     │   │
│  │  Client Storage: HttpOnly cookie (secure, sameSite=strict)              │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  KEY ROTATION                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │  • RSA key pairs rotated every 30 days                                  │   │
│  │  • JWKS endpoint always serves current + previous key (overlap period)  │   │
│  │  • Kong caches JWKS with configurable TTL (default: 1 hour)            │   │
│  │  • Old tokens remain valid until expiration during rotation             │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 5.3 JWKS (JSON Web Key Set) Integration

Kong validates JWTs using the Auth Service's JWKS endpoint, enabling stateless validation:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         JWKS INTEGRATION                                        │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│   Auth Service                                    Kong Gateway                  │
│       │                                               │                         │
│       │                                               │                         │
│       │  GET /.well-known/jwks.json                   │                         │
│       │◀──────────────────────────────────────────────│  (on startup +          │
│       │                                               │   periodic refresh)     │
│       │  {                                            │                         │
│       │    "keys": [                                  │                         │
│       │      {                                        │                         │
│       │        "kty": "RSA",                          │                         │
│       │        "kid": "key-2026-01",     ◄────────────│  Key ID for matching    │
│       │        "use": "sig",                          │                         │
│       │        "alg": "RS256",                        │                         │
│       │        "n": "public-key-modulus...",          │                         │
│       │        "e": "AQAB"                            │                         │
│       │      },                                       │                         │
│       │      {                                        │                         │
│       │        "kty": "RSA",                          │                         │
│       │        "kid": "key-2025-12",     ◄────────────│  Previous key           │
│       │        "use": "sig",                          │   (rotation overlap)    │
│       │        "alg": "RS256",                        │                         │
│       │        "n": "old-public-key...",              │                         │
│       │        "e": "AQAB"                            │                         │
│       │      }                                        │                         │
│       │    ]                                          │                         │
│       │  }                                            │                         │
│       │──────────────────────────────────────────────▶│                         │
│       │                                               │  Cache keys locally     │
│       │                                               │  (TTL: 1 hour)          │
│                                                                                 │
│   VALIDATION PROCESS:                                                          │
│   1. Kong receives request with JWT                                            │
│   2. Kong extracts 'kid' from JWT header                                       │
│   3. Kong looks up matching public key from cached JWKS                        │
│   4. Kong verifies JWT signature using public key                              │
│   5. Kong checks exp, iss, aud claims                                          │
│   6. If valid, Kong extracts claims and forwards to backend                    │
│                                                                                 │
│   NO CALL TO AUTH SERVICE FOR EACH REQUEST!                                    │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 5.4 Token Revocation Strategy

Since JWTs are stateless, immediate revocation requires additional measures:

| Strategy | Implementation | Use Case |
|----------|---------------|----------|
| **Short Expiration** | 15-minute access tokens | Primary defense |
| **Refresh Token Revocation** | Delete from database | Logout, password change |
| **Token Blacklist (Optional)** | Redis-based blacklist | Immediate revocation needs |
| **Key Rotation** | Rotate signing keys | Compromise recovery |

```yaml
# Token Revocation Scenarios
scenarios:
  user_logout:
    action: Delete refresh token from database
    effect: User must re-login after access token expires (max 15 min)

  password_change:
    action: Delete all user's refresh tokens
    effect: All sessions invalidated after access token expires

  account_compromise:
    action: Add access token to Redis blacklist + delete refresh tokens
    effect: Immediate invalidation (requires Kong to check blacklist)

  security_incident:
    action: Rotate RSA signing keys
    effect: All tokens invalid after JWKS cache refresh
```

---

## 6. Technical Architecture

### 6.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              KUBERNETES CLUSTER                                 │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                           KONG NAMESPACE                                │   │
│  │                                                                         │   │
│  │  ┌───────────────────────────────────────────────────────────────────┐ │   │
│  │  │                      Kong Gateway                                 │ │   │
│  │  │                   (2+ replicas, HA)                               │ │   │
│  │  │                                                                   │ │   │
│  │  │  Routes:                         Plugins:                         │ │   │
│  │  │  ├─ /auth/* → Auth Service       ├─ JWT (JWKS validation)        │ │   │
│  │  │  ├─ /api/*  → Backend Service    ├─ ACL (role-based access)      │ │   │
│  │  │  └─ /actuator/* → Backend        ├─ Rate Limiting                │ │   │
│  │  │                                  ├─ CORS                         │ │   │
│  │  │                                  ├─ Request Transformer          │ │   │
│  │  │                                  ├─ Prometheus                   │ │   │
│  │  │                                  └─ File Log                     │ │   │
│  │  └───────────────────────────────────────────────────────────────────┘ │   │
│  │                              │                                         │   │
│  │                              ▼                                         │   │
│  │  ┌───────────────────────────────────────────────────────────────────┐ │   │
│  │  │                    Kong PostgreSQL                                │ │   │
│  │  │                  (Configuration Store)                            │ │   │
│  │  └───────────────────────────────────────────────────────────────────┘ │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                              │                                                  │
│          ┌───────────────────┼───────────────────┐                             │
│          │                   │                   │                             │
│          ▼                   ▼                   ▼                             │
│  ┌───────────────┐   ┌───────────────┐   ┌───────────────┐                    │
│  │ AUTH NAMESPACE │   │BACKEND NAMESPACE│   │REDIS NAMESPACE│                   │
│  │               │   │               │   │               │                    │
│  │┌─────────────┐│   │┌─────────────┐│   │┌─────────────┐│                    │
│  ││Auth Service ││   ││Backend API  ││   ││   Redis     ││                    │
│  ││(Spring Sec) ││   ││(Spring Boot)││   ││(Rate Limit) ││                    │
│  ││             ││   ││             ││   │└─────────────┘│                    │
│  ││• Login      ││   ││• Properties ││   └───────────────┘                    │
│  ││• Register   ││   ││• Upload     ││                                        │
│  ││• Refresh    ││   ││• Admin      ││                                        │
│  ││• JWKS       ││   │└──────┬──────┘│                                        │
│  │└──────┬──────┘│   │       │       │                                        │
│  │       │       │   │       ▼       │                                        │
│  │       ▼       │   │┌─────────────┐│                                        │
│  │┌─────────────┐│   ││ App DB      ││                                        │
│  ││  User DB    ││   ││(PostgreSQL) ││                                        │
│  ││(PostgreSQL) ││   │└─────────────┘│                                        │
│  │└─────────────┘│   └───────────────┘                                        │
│  └───────────────┘                                                             │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Network Flow

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              REQUEST FLOWS                                      │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  FLOW 1: AUTHENTICATION (Login/Register)                                       │
│  ════════════════════════════════════════                                       │
│                                                                                 │
│  Client ──▶ Kong ──▶ Auth Service ──▶ User DB                                  │
│     │         │           │              │                                     │
│     │  POST   │  Forward  │   Validate   │                                     │
│     │ /auth/  │  (no JWT  │   & Issue    │                                     │
│     │ login   │  check)   │   Tokens     │                                     │
│     │         │           │              │                                     │
│     │◀────────│◀──────────│◀─────────────│                                     │
│           {accessToken, refreshToken}                                          │
│                                                                                 │
│  FLOW 2: PROTECTED API REQUEST                                                 │
│  ═══════════════════════════════                                               │
│                                                                                 │
│  Client ──▶ Kong ──▶ Backend ──▶ App DB                                        │
│     │         │         │          │                                           │
│     │  GET    │ Validate │ Process  │                                          │
│     │ /api/   │ JWT via  │ with     │                                          │
│     │         │ JWKS     │ user     │                                          │
│     │         │          │ context  │                                          │
│     │         │ Forward  │          │                                          │
│     │         │ + Headers│          │                                          │
│     │◀────────│◀─────────│◀─────────│                                          │
│           Response                                                              │
│                                                                                 │
│  FLOW 3: TOKEN REFRESH                                                         │
│  ══════════════════════                                                        │
│                                                                                 │
│  Client ──▶ Kong ──▶ Auth Service ──▶ User DB                                  │
│     │         │           │              │                                     │
│     │  POST   │  Forward  │  Validate    │                                     │
│     │ /auth/  │  (no JWT  │  Refresh     │                                     │
│     │ refresh │  check)   │  Token       │                                     │
│     │         │           │              │                                     │
│     │◀────────│◀──────────│◀─────────────│                                     │
│           {accessToken}                                                         │
│                                                                                 │
│  FLOW 4: PUBLIC ENDPOINT                                                       │
│  ════════════════════════                                                      │
│                                                                                 │
│  Client ──▶ Kong ──▶ Backend ──▶ App DB                                        │
│     │         │         │          │                                           │
│     │  GET    │ Rate     │ Process  │                                          │
│     │ /api/   │ Limit    │          │                                          │
│     │ props   │ Only     │          │                                          │
│     │         │ (no JWT) │          │                                          │
│     │◀────────│◀─────────│◀─────────│                                          │
│           Response                                                              │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 Service Communication Matrix

| Source | Destination | Protocol | Port | Auth | Purpose |
|--------|-------------|----------|------|------|---------|
| Frontend | Kong | HTTPS | 443 | JWT Header | API calls |
| Kong | Auth Service | HTTP | 8081 | None (internal) | Auth routes, JWKS |
| Kong | Backend | HTTP | 8080 | Headers | API routes |
| Kong | Redis | TCP | 6379 | Password | Rate limiting |
| Auth Service | User DB | TCP | 5432 | Password | User data |
| Backend | App DB | TCP | 5432 | Password | Business data |

---

## 7. Auth Service Specification

### 7.1 Service Overview

| Attribute | Value |
|-----------|-------|
| **Name** | auth-service |
| **Type** | Spring Boot 3.x with Spring Security |
| **Port** | 8081 |
| **Database** | PostgreSQL (dedicated) |
| **Language** | Java 17 |

### 7.2 API Endpoints

#### 7.2.1 Authentication Endpoints

| Endpoint | Method | Auth | Request Body | Response | Description |
|----------|--------|------|--------------|----------|-------------|
| `/auth/register` | POST | No | RegisterRequest | AuthResponse | User registration |
| `/auth/login` | POST | No | LoginRequest | AuthResponse | User login |
| `/auth/refresh` | POST | No | RefreshRequest | TokenResponse | Refresh access token |
| `/auth/logout` | POST | JWT | - | 200 OK | Invalidate refresh token |
| `/auth/forgot-password` | POST | No | ForgotPasswordRequest | 200 OK | Request password reset |
| `/auth/reset-password` | POST | No | ResetPasswordRequest | 200 OK | Reset password with token |
| `/auth/change-password` | POST | JWT | ChangePasswordRequest | 200 OK | Change password |
| `/auth/me` | GET | JWT | - | UserResponse | Get current user profile |

#### 7.2.2 JWKS Endpoint (Public)

| Endpoint | Method | Auth | Response | Description |
|----------|--------|------|----------|-------------|
| `/.well-known/jwks.json` | GET | No | JWKS | Public keys for JWT verification |

#### 7.2.3 User Management Endpoints (Admin Only)

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/users` | GET | JWT + Admin | List all users (paginated) |
| `/users/{id}` | GET | JWT + Admin | Get user by ID |
| `/users/{id}` | PUT | JWT + Admin | Update user |
| `/users/{id}` | DELETE | JWT + Admin | Delete user |
| `/users/{id}/roles` | PUT | JWT + Admin | Update user roles |
| `/users/{id}/status` | PUT | JWT + Admin | Enable/disable user |

#### 7.2.4 Health Endpoints

| Endpoint | Method | Auth | Description |
|----------|--------|------|-------------|
| `/actuator/health` | GET | No | Health check |
| `/actuator/health/liveness` | GET | No | Kubernetes liveness probe |
| `/actuator/health/readiness` | GET | No | Kubernetes readiness probe |

### 7.3 Data Transfer Objects (DTOs)

```java
// Request DTOs
public record RegisterRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank String firstName,
    @NotBlank String lastName,
    String phone
) {}

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {}

public record RefreshRequest(
    @NotBlank String refreshToken
) {}

public record ForgotPasswordRequest(
    @NotBlank @Email String email
) {}

public record ResetPasswordRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8) String newPassword
) {}

public record ChangePasswordRequest(
    @NotBlank String currentPassword,
    @NotBlank @Size(min = 8) String newPassword
) {}

// Response DTOs
public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType,
    UserResponse user
) {}

public record TokenResponse(
    String accessToken,
    long expiresIn,
    String tokenType
) {}

public record UserResponse(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String phone,
    List<String> roles,
    LocalDateTime createdAt
) {}
```

### 7.4 Database Schema

```sql
-- Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    email_verified BOOLEAN DEFAULT FALSE,
    enabled BOOLEAN DEFAULT TRUE,
    account_locked BOOLEAN DEFAULT FALSE,
    failed_login_attempts INT DEFAULT 0,
    lock_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- Roles Table
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- User Roles (Many-to-Many)
CREATE TABLE user_roles (
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role_id INT REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Refresh Tokens Table
CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP,
    device_info VARCHAR(255),
    ip_address VARCHAR(45)
);

-- Password Reset Tokens
CREATE TABLE password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- RSA Keys for JWT Signing
CREATE TABLE rsa_keys (
    id VARCHAR(50) PRIMARY KEY,  -- Key ID (kid)
    public_key TEXT NOT NULL,
    private_key TEXT NOT NULL,   -- Encrypted
    algorithm VARCHAR(10) DEFAULT 'RS256',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
CREATE INDEX idx_password_reset_tokens_token ON password_reset_tokens(token);

-- Initial Data
INSERT INTO roles (name, description) VALUES
    ('ROLE_USER', 'Standard user role'),
    ('ROLE_ADMIN', 'Administrator role');
```

### 7.5 Project Structure

```
auth-service/
├── src/main/java/com/realestate/auth/
│   ├── AuthServiceApplication.java
│   ├── controller/
│   │   ├── AuthController.java          # Login, register, refresh
│   │   ├── UserController.java          # User management (admin)
│   │   └── JwksController.java          # JWKS endpoint
│   ├── service/
│   │   ├── AuthService.java             # Authentication logic
│   │   ├── JwtService.java              # JWT generation/validation
│   │   ├── UserService.java             # User CRUD operations
│   │   ├── RefreshTokenService.java     # Refresh token management
│   │   └── PasswordResetService.java    # Password reset logic
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── RoleRepository.java
│   │   ├── RefreshTokenRepository.java
│   │   ├── PasswordResetTokenRepository.java
│   │   └── RsaKeyRepository.java
│   ├── model/
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── RefreshToken.java
│   │   ├── PasswordResetToken.java
│   │   └── RsaKey.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── LoginRequest.java
│   │   │   ├── RegisterRequest.java
│   │   │   ├── RefreshRequest.java
│   │   │   └── ...
│   │   └── response/
│   │       ├── AuthResponse.java
│   │       ├── TokenResponse.java
│   │       ├── UserResponse.java
│   │       └── JwksResponse.java
│   ├── config/
│   │   ├── SecurityConfig.java          # Spring Security config
│   │   ├── JwtConfig.java               # JWT properties
│   │   └── CorsConfig.java              # CORS settings
│   ├── security/
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── CustomUserDetailsService.java
│   │   └── JwtTokenProvider.java
│   ├── exception/
│   │   ├── AuthException.java
│   │   ├── UserNotFoundException.java
│   │   ├── TokenExpiredException.java
│   │   └── GlobalExceptionHandler.java
│   └── util/
│       ├── KeyGeneratorUtil.java        # RSA key generation
│       └── PasswordEncoderUtil.java
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/
│       ├── V1__create_users_table.sql
│       ├── V2__create_roles_table.sql
│       ├── V3__create_refresh_tokens_table.sql
│       └── V4__create_rsa_keys_table.sql
├── src/test/java/
│   └── ...
├── Dockerfile
├── pom.xml
└── helm/
    └── auth-service/
        ├── Chart.yaml
        ├── values.yaml
        └── templates/
```

### 7.6 Configuration

```yaml
# application.yml
spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:authdb}
    username: ${DB_USER:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  flyway:
    enabled: true
    locations: classpath:db/migration

server:
  port: 8081

jwt:
  access-token:
    expiration: 900000          # 15 minutes in milliseconds
  refresh-token:
    expiration: 604800000       # 7 days in milliseconds
  issuer: auth-service
  audience: real-estate-api

security:
  password:
    min-length: 8
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special: false
  account:
    max-failed-attempts: 5
    lock-duration-minutes: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    health:
      probes:
        enabled: true
```

### 7.7 Security Configuration

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/auth/register", "/auth/login", "/auth/refresh").permitAll()
                .requestMatchers("/auth/forgot-password", "/auth/reset-password").permitAll()
                .requestMatchers("/.well-known/jwks.json").permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                // Admin endpoints
                .requestMatchers("/users/**").hasRole("ADMIN")
                // Authenticated endpoints
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
```

---

## 8. Feature Requirements

### 8.1 Authentication & Authorization

#### 8.1.1 Kong JWT Plugin Configuration

```yaml
plugins:
  - name: jwt
    config:
      # JWKS URL for key fetching
      key_claim_name: kid
      claims_to_verify:
        - exp                        # Token expiration
        - iss                        # Issuer claim
      anonymous: null                # Reject if no valid JWT
      run_on_preflight: false        # Skip for OPTIONS requests

      # JWKS Configuration
      # Kong will fetch keys from: http://auth-service:8081/.well-known/jwks.json
```

#### 8.1.2 Access Control Lists (ACL)

| Consumer Group | Allowed Routes | Description |
|----------------|----------------|-------------|
| `public` | `/api/properties` (GET), `/api/properties/search`, `/api/properties/cities` | Public property browsing |
| `authenticated` | All `/api/properties/*`, `/api/upload/*` | Logged-in users |
| `admin` | `/api/admin/*`, `/users/*` | Administrative functions |

#### 8.1.3 Request Header Transformation

Kong extracts JWT claims and forwards them as headers:

```yaml
plugins:
  - name: request-transformer
    config:
      add:
        headers:
          - "X-User-Id:$(jwt.claims.sub)"
          - "X-User-Email:$(jwt.claims.email)"
          - "X-User-Roles:$(jwt.claims.roles)"
```

### 8.2 Rate Limiting

#### 8.2.1 Rate Limit Tiers

| Tier | Requests/Minute | Requests/Hour | Applied To |
|------|-----------------|---------------|------------|
| Anonymous | 30 | 500 | Unauthenticated requests |
| Authenticated | 100 | 3000 | Regular users |
| Admin | 1000 | 30000 | Admin users |
| Auth Endpoints | 10 | 100 | /auth/login, /auth/register |

#### 8.2.2 Endpoint-Specific Limits

| Endpoint | Limit | Rationale |
|----------|-------|-----------|
| `POST /auth/login` | 10/minute | Prevent brute force |
| `POST /auth/register` | 5/minute | Prevent spam accounts |
| `POST /auth/forgot-password` | 3/minute | Prevent email spam |
| `POST /api/upload/*` | 10/minute | Prevent upload abuse |
| `POST /api/properties` | 20/hour | Prevent spam listings |

### 8.3 Route Configuration

#### 8.3.1 Kong Routes

```yaml
services:
  - name: auth-service
    url: http://auth-service.auth.svc.cluster.local:8081

  - name: backend-service
    url: http://backend-service.real-estate.svc.cluster.local:8080

routes:
  # Auth routes (no JWT validation)
  - name: auth-public
    service: auth-service
    paths:
      - /auth/login
      - /auth/register
      - /auth/refresh
      - /auth/forgot-password
      - /auth/reset-password
    methods:
      - POST
    strip_path: false

  - name: auth-jwks
    service: auth-service
    paths:
      - /.well-known/jwks.json
    methods:
      - GET
    strip_path: false

  # Auth routes (JWT required)
  - name: auth-protected
    service: auth-service
    paths:
      - /auth/logout
      - /auth/change-password
      - /auth/me
    strip_path: false

  # User management (Admin only)
  - name: users-admin
    service: auth-service
    paths:
      - /users
    strip_path: false

  # Backend public routes
  - name: properties-public
    service: backend-service
    paths:
      - /api/properties
    methods:
      - GET
    strip_path: false

  # Backend protected routes
  - name: properties-protected
    service: backend-service
    paths:
      - /api/properties
    methods:
      - POST
      - PUT
      - DELETE
    strip_path: false

  # Admin routes
  - name: admin-routes
    service: backend-service
    paths:
      - /api/admin
    strip_path: false
```

### 8.4 Complete Route Access Matrix

| Route | Methods | Auth | ACL Group | Rate Limit | JWT Claims Forwarded |
|-------|---------|------|-----------|------------|---------------------|
| `/auth/register` | POST | No | - | 5/min | - |
| `/auth/login` | POST | No | - | 10/min | - |
| `/auth/refresh` | POST | No | - | 30/min | - |
| `/auth/logout` | POST | JWT | authenticated | 100/min | sub, email |
| `/auth/me` | GET | JWT | authenticated | 100/min | sub, email |
| `/.well-known/jwks.json` | GET | No | - | 120/min | - |
| `/users/*` | ALL | JWT | admin | 100/min | sub, email, roles |
| `/api/properties` | GET | No | - | 60/min | - |
| `/api/properties/search` | POST | No | - | 60/min | - |
| `/api/properties` | POST, PUT, DELETE | JWT | authenticated | 100/min | sub, email, roles |
| `/api/properties/user/{userId}` | GET | JWT | authenticated | 100/min | sub, email |
| `/api/upload/*` | POST | JWT | authenticated | 10/min | sub, email |
| `/api/admin/*` | ALL | JWT | admin | 30/min | sub, email, roles |
| `/actuator/health` | GET | No | - | 120/min | - |

---

## 9. Implementation Plan

### 9.1 Phase Overview

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         IMPLEMENTATION PHASES                                   │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  PHASE 1: Auth Service Development                                             │
│  ├─── Project setup (Spring Boot + Security)                                   │
│  ├─── User entity and repository                                               │
│  ├─── Registration and login endpoints                                         │
│  ├─── JWT generation with RS256                                                │
│  ├─── JWKS endpoint                                                            │
│  ├─── Refresh token mechanism                                                  │
│  ├─── Database migrations                                                      │
│  └─── Unit and integration tests                                               │
│                                                                                 │
│  PHASE 2: Kong Gateway Foundation                                              │
│  ├─── Kong Helm chart setup                                                    │
│  ├─── Basic routing configuration                                              │
│  ├─── Auth service and backend service definitions                             │
│  ├─── Health check endpoints                                                   │
│  └─── Local development environment                                            │
│                                                                                 │
│  PHASE 3: Kong-Auth Integration                                                │
│  ├─── JWT plugin with JWKS                                                     │
│  ├─── ACL plugin configuration                                                 │
│  ├─── Request transformer (header forwarding)                                  │
│  ├─── CORS plugin setup                                                        │
│  └─── Route-level plugin configuration                                         │
│                                                                                 │
│  PHASE 4: Backend Integration                                                  │
│  ├─── Update backend to trust Kong headers                                     │
│  ├─── Remove backend authentication logic (if any)                             │
│  ├─── Add user context from headers                                            │
│  ├─── Ownership validation using X-User-Id                                     │
│  └─── Update frontend API client                                               │
│                                                                                 │
│  PHASE 5: Traffic Management                                                   │
│  ├─── Rate limiting plugin                                                     │
│  ├─── Redis deployment                                                         │
│  ├─── Upstream health checks                                                   │
│  └─── Circuit breaker configuration                                            │
│                                                                                 │
│  PHASE 6: Observability                                                        │
│  ├─── Prometheus metrics plugin                                                │
│  ├─── Logging plugin                                                           │
│  ├─── Auth service metrics                                                     │
│  ├─── Grafana dashboards                                                       │
│  └─── AlertManager rules                                                       │
│                                                                                 │
│  PHASE 7: Production Hardening                                                 │
│  ├─── High availability (multi-replica)                                        │
│  ├─── SSL/TLS termination                                                      │
│  ├─── Key rotation automation                                                  │
│  ├─── Performance tuning                                                       │
│  └─── Security audit                                                           │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 9.2 Detailed Phase Breakdown

#### Phase 1: Auth Service Development

**Deliverables:**
1. Spring Boot project with Spring Security
2. User registration and login functionality
3. JWT token generation (RS256)
4. JWKS endpoint for public key distribution
5. Refresh token mechanism
6. Password reset functionality
7. Database schema and migrations
8. API documentation (OpenAPI/Swagger)
9. Unit and integration tests

**Project Structure:**
```
auth-service/
├── src/main/java/com/realestate/auth/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── model/
│   ├── dto/
│   ├── config/
│   ├── security/
│   └── exception/
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
├── src/test/
├── Dockerfile
├── pom.xml
└── helm/auth-service/
```

#### Phase 2: Kong Gateway Foundation

**Deliverables:**
1. Kong Helm chart with custom values
2. Kong namespace and RBAC
3. PostgreSQL for Kong configuration
4. Service definitions (auth-service, backend-service)
5. Basic route definitions
6. Local development environment with minikube

**Files Structure:**
```
kong/
├── helm/
│   └── kong/
│       ├── Chart.yaml
│       ├── values.yaml
│       ├── values-local.yaml
│       └── values-production.yaml
├── config/
│   ├── services.yaml
│   └── routes.yaml
└── scripts/
    ├── install-kong.sh
    └── configure-kong.sh
```

#### Phase 3: Kong-Auth Integration

**Deliverables:**
1. JWT plugin configuration with JWKS URL
2. ACL groups and consumer mapping
3. Request transformer for header forwarding
4. CORS plugin (remove from backend)
5. Route-specific plugin assignments

**Configuration Files:**
```
kong/config/
├── plugins/
│   ├── jwt.yaml
│   ├── acl.yaml
│   ├── cors.yaml
│   └── request-transformer.yaml
├── consumers/
│   └── acl-groups.yaml
└── routes/
    ├── auth-routes.yaml
    ├── api-routes.yaml
    └── admin-routes.yaml
```

#### Phase 4: Backend Integration

**Deliverables:**
1. Backend filter to extract user from headers
2. UserContext class for request-scoped user info
3. Ownership validation in services
4. Remove CORS configuration from backend
5. Updated frontend authentication flow

**Backend Changes:**
```java
// UserContextFilter.java - Extract user from Kong headers
@Component
public class UserContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                        FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String userId = httpRequest.getHeader("X-User-Id");
        String userEmail = httpRequest.getHeader("X-User-Email");
        String userRoles = httpRequest.getHeader("X-User-Roles");

        if (userId != null) {
            UserContext.setCurrentUser(new UserInfo(userId, userEmail, userRoles));
        }

        try {
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
```

#### Phase 5-7: Traffic Management, Observability, Production Hardening

(Detailed in subsequent sections)

### 9.3 File Structure Overview

```
real-estate-ui/
├── auth-service/                    # NEW: Authentication microservice
│   ├── src/
│   ├── Dockerfile
│   ├── pom.xml
│   └── helm/
│       └── auth-service/
│           ├── Chart.yaml
│           ├── values.yaml
│           └── templates/
│
├── kong/                            # NEW: Kong Gateway configuration
│   ├── helm/
│   │   └── kong/
│   │       ├── Chart.yaml
│   │       ├── values.yaml
│   │       ├── values-local.yaml
│   │       └── values-production.yaml
│   ├── config/
│   │   ├── kong.yaml               # Declarative config
│   │   ├── services.yaml
│   │   ├── routes.yaml
│   │   └── plugins/
│   └── scripts/
│
├── backend/                         # EXISTING: Updated
│   ├── src/                        # Add UserContext filter
│   ├── helm/
│   └── ...
│
├── frontend/                        # EXISTING: Updated
│   ├── lib/
│   │   ├── auth/                   # Updated auth client
│   │   └── api/                    # Updated API client
│   └── ...
│
├── terraform/
│   ├── modules/
│   │   ├── rds/
│   │   ├── eks/
│   │   └── redis/                  # NEW: For rate limiting
│   └── ...
│
└── docs/
    ├── KONG_GATEWAY_PRP.md         # This document
    └── AUTH_SERVICE_GUIDE.md       # NEW: Auth service documentation
```

---

## 10. Configuration Specifications

### 10.1 Kong Helm Values (Production)

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

autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
```

### 10.2 Kong Declarative Configuration

```yaml
# kong/config/kong.yaml

_format_version: "3.0"
_transform: true

# ============================================================================
# SERVICES
# ============================================================================
services:
  - name: auth-service
    url: http://auth-service.auth.svc.cluster.local:8081
    connect_timeout: 10000
    write_timeout: 60000
    read_timeout: 60000
    retries: 3

  - name: backend-service
    url: http://backend-service.real-estate.svc.cluster.local:8080
    connect_timeout: 10000
    write_timeout: 60000
    read_timeout: 60000
    retries: 3

# ============================================================================
# ROUTES
# ============================================================================
routes:
  # --- Auth Service Routes (Public - No JWT) ---
  - name: auth-public
    service: auth-service
    paths:
      - /auth/login
      - /auth/register
      - /auth/refresh
      - /auth/forgot-password
      - /auth/reset-password
    methods:
      - POST
    strip_path: false

  - name: auth-jwks
    service: auth-service
    paths:
      - /.well-known/jwks.json
    methods:
      - GET
    strip_path: false

  # --- Auth Service Routes (Protected - JWT Required) ---
  - name: auth-protected
    service: auth-service
    paths:
      - /auth/logout
      - /auth/change-password
      - /auth/me
    strip_path: false

  # --- User Management (Admin Only) ---
  - name: users-admin
    service: auth-service
    paths:
      - /users
    strip_path: false

  # --- Backend Public Routes ---
  - name: properties-public-get
    service: backend-service
    paths:
      - /api/properties
    methods:
      - GET
    strip_path: false

  - name: properties-search
    service: backend-service
    paths:
      - /api/properties/search
    methods:
      - POST
    strip_path: false

  - name: properties-cities
    service: backend-service
    paths:
      - /api/properties/cities
    methods:
      - GET
    strip_path: false

  # --- Backend Protected Routes ---
  - name: properties-protected
    service: backend-service
    paths:
      - /api/properties
    methods:
      - POST
      - PUT
      - DELETE
    strip_path: false

  - name: properties-user
    service: backend-service
    paths:
      - /api/properties/user
    strip_path: false

  - name: upload-routes
    service: backend-service
    paths:
      - /api/upload
    strip_path: false

  # --- Admin Routes ---
  - name: admin-routes
    service: backend-service
    paths:
      - /api/admin
    strip_path: false

  # --- Health Check ---
  - name: health-backend
    service: backend-service
    paths:
      - /actuator/health
    methods:
      - GET
    strip_path: false

  - name: health-auth
    service: auth-service
    paths:
      - /auth/actuator/health
    methods:
      - GET
    strip_path: false

# ============================================================================
# PLUGINS - GLOBAL
# ============================================================================
plugins:
  # --- CORS (Global) ---
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
      exposed_headers:
        - X-RateLimit-Remaining
        - X-RateLimit-Limit
        - X-Request-ID
      credentials: true
      max_age: 3600

  # --- Prometheus Metrics (Global) ---
  - name: prometheus
    config:
      per_consumer: true
      status_code_metrics: true
      latency_metrics: true
      bandwidth_metrics: true

  # --- Request ID (Global) ---
  - name: correlation-id
    config:
      header_name: X-Request-ID
      generator: uuid
      echo_downstream: true

# ============================================================================
# PLUGINS - ROUTE SPECIFIC
# ============================================================================

  # --- JWT Plugin for Protected Routes ---
  - name: jwt
    route: auth-protected
    config:
      key_claim_name: kid
      claims_to_verify:
        - exp

  - name: jwt
    route: users-admin
    config:
      key_claim_name: kid
      claims_to_verify:
        - exp

  - name: jwt
    route: properties-protected
    config:
      key_claim_name: kid
      claims_to_verify:
        - exp

  - name: jwt
    route: properties-user
    config:
      key_claim_name: kid
      claims_to_verify:
        - exp

  - name: jwt
    route: upload-routes
    config:
      key_claim_name: kid
      claims_to_verify:
        - exp

  - name: jwt
    route: admin-routes
    config:
      key_claim_name: kid
      claims_to_verify:
        - exp

  # --- ACL for Admin Routes ---
  - name: acl
    route: admin-routes
    config:
      allow:
        - admin

  - name: acl
    route: users-admin
    config:
      allow:
        - admin

  # --- Rate Limiting ---
  - name: rate-limiting
    route: auth-public
    config:
      minute: 10
      hour: 100
      policy: redis
      redis_host: redis.redis.svc.cluster.local
      redis_port: 6379

  - name: rate-limiting
    route: properties-public-get
    config:
      minute: 60
      policy: local

  - name: rate-limiting
    route: properties-protected
    config:
      minute: 100
      policy: local

  - name: rate-limiting
    route: upload-routes
    config:
      minute: 10
      policy: local

  - name: rate-limiting
    route: admin-routes
    config:
      minute: 30
      policy: local

# ============================================================================
# UPSTREAMS (for load balancing)
# ============================================================================
upstreams:
  - name: auth-service-upstream
    targets:
      - target: auth-service.auth.svc.cluster.local:8081
        weight: 100
    healthchecks:
      active:
        healthy:
          interval: 5
          successes: 2
        unhealthy:
          interval: 5
          http_failures: 3
        http_path: /actuator/health

  - name: backend-service-upstream
    targets:
      - target: backend-service.real-estate.svc.cluster.local:8080
        weight: 100
    healthchecks:
      active:
        healthy:
          interval: 5
          successes: 2
        unhealthy:
          interval: 5
          http_failures: 3
        http_path: /actuator/health
```

### 10.3 Auth Service Helm Values

```yaml
# auth-service/helm/auth-service/values.yaml

replicaCount: 2

image:
  repository: your-registry/auth-service
  tag: latest
  pullPolicy: IfNotPresent

service:
  type: ClusterIP
  port: 8081

resources:
  requests:
    cpu: 250m
    memory: 512Mi
  limits:
    cpu: 1000m
    memory: 1Gi

env:
  SPRING_PROFILES_ACTIVE: prod
  DB_HOST: auth-postgresql
  DB_PORT: "5432"
  DB_NAME: authdb
  DB_USER: authuser
  JWT_ACCESS_TOKEN_EXPIRATION: "900000"
  JWT_REFRESH_TOKEN_EXPIRATION: "604800000"

secrets:
  - name: DB_PASSWORD
    secretName: auth-db-secret
    secretKey: password
  - name: JWT_PRIVATE_KEY
    secretName: auth-jwt-secret
    secretKey: private-key

postgresql:
  enabled: true
  auth:
    username: authuser
    database: authdb
    existingSecret: auth-db-secret
  primary:
    persistence:
      enabled: true
      size: 5Gi

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8081
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8081
  initialDelaySeconds: 20
  periodSeconds: 5
```

---

## 11. Security Considerations

### 11.1 Security Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         SECURITY LAYERS                                         │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  Layer 1: Network Security                                                      │
│  ├─── Kubernetes Network Policies                                               │
│  ├─── Namespace isolation                                                       │
│  ├─── Internal services not exposed externally                                  │
│  └─── TLS for all external traffic                                             │
│                                                                                 │
│  Layer 2: Edge Security (Kong)                                                  │
│  ├─── TLS termination                                                           │
│  ├─── Rate limiting (DDoS protection)                                           │
│  ├─── IP restriction (optional)                                                 │
│  └─── Request size limits                                                       │
│                                                                                 │
│  Layer 3: Authentication (Auth Service + Kong)                                  │
│  ├─── Password hashing (BCrypt, cost factor 12)                                │
│  ├─── JWT with RS256 (asymmetric signing)                                      │
│  ├─── Short-lived access tokens (15 min)                                       │
│  ├─── Secure refresh token storage                                             │
│  └─── Account lockout after failed attempts                                    │
│                                                                                 │
│  Layer 4: Authorization (Kong + Backend)                                        │
│  ├─── ACL groups at Kong level                                                 │
│  ├─── Role-based access control                                                │
│  ├─── Resource ownership validation                                            │
│  └─── Admin endpoint protection                                                │
│                                                                                 │
│  Layer 5: Application Security                                                  │
│  ├─── Input validation                                                          │
│  ├─── SQL injection prevention (JPA/Hibernate)                                 │
│  ├─── XSS prevention                                                           │
│  └─── CSRF protection (for web forms)                                          │
│                                                                                 │
│  Layer 6: Data Security                                                         │
│  ├─── Encryption at rest (database)                                            │
│  ├─── Encryption in transit (TLS)                                              │
│  ├─── Sensitive data masking in logs                                           │
│  └─── PII handling compliance                                                  │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 11.2 Secret Management

| Secret | Storage | Rotation | Access |
|--------|---------|----------|--------|
| Kong DB Password | K8s Secret + External Secrets | 90 days | Kong pods only |
| Auth Service DB Password | K8s Secret + External Secrets | 90 days | Auth pods only |
| JWT Private Key | K8s Secret + External Secrets | 30 days | Auth pods only |
| Redis Password | K8s Secret | 90 days | Kong pods only |
| TLS Certificates | cert-manager / AWS ACM | Auto-renewal | Kong pods only |

### 11.3 Security Checklist

#### Infrastructure
- [ ] Enable TLS 1.2+ only
- [ ] Configure HSTS headers
- [ ] Disable Kong Admin API external access
- [ ] Enable request body size limits
- [ ] Configure network policies
- [ ] Enable pod security standards

#### Authentication
- [ ] Implement password complexity requirements
- [ ] Enable account lockout after 5 failed attempts
- [ ] Implement JWT key rotation (30 days)
- [ ] Use secure cookie settings for refresh tokens
- [ ] Implement rate limiting on auth endpoints

#### Authorization
- [ ] Protect all admin endpoints with ACL
- [ ] Validate resource ownership in backend
- [ ] Implement role hierarchy
- [ ] Audit all authorization decisions

#### Monitoring
- [ ] Log all authentication events
- [ ] Alert on suspicious patterns (multiple failed logins)
- [ ] Monitor token revocation events
- [ ] Track API abuse patterns

### 11.4 Threat Model

| Threat | Mitigation | Component |
|--------|------------|-----------|
| Brute Force | Account lockout, rate limiting | Auth Service, Kong |
| Token Theft | Short expiration, secure storage | Auth Service, Frontend |
| Session Hijacking | Refresh token rotation, secure cookies | Auth Service |
| DDoS | Rate limiting, IP restriction | Kong |
| SQL Injection | Parameterized queries, JPA | Auth Service, Backend |
| XSS | Content Security Policy, output encoding | Frontend |
| CSRF | SameSite cookies, CSRF tokens | Auth Service |
| Key Compromise | Key rotation, HSM (future) | Auth Service |

---

## 12. Monitoring and Observability

### 12.1 Metrics

#### 12.1.1 Auth Service Metrics

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `auth_login_total` | Total login attempts | - |
| `auth_login_success` | Successful logins | - |
| `auth_login_failure` | Failed logins | > 100/min |
| `auth_registration_total` | Registration attempts | - |
| `auth_token_refresh_total` | Token refreshes | - |
| `auth_token_validation_latency` | JWT validation time | P99 > 10ms |
| `auth_active_sessions` | Active refresh tokens | - |

#### 12.1.2 Kong Metrics

| Metric | Description | Alert Threshold |
|--------|-------------|-----------------|
| `kong_http_requests_total` | Total requests | - |
| `kong_request_latency_ms` | Request latency | P99 > 500ms |
| `kong_http_status` | Status code counts | 5xx > 1% |
| `kong_jwt_auth_success` | JWT validations | - |
| `kong_jwt_auth_failure` | JWT failures | > 5% |
| `kong_rate_limiting_exceeded` | Rate limit hits | > 10/min |

### 12.2 Logging

#### 12.2.1 Auth Service Log Events

```json
{
  "timestamp": "2026-01-26T10:30:00.000Z",
  "level": "INFO",
  "service": "auth-service",
  "event": "USER_LOGIN",
  "user_id": "uuid",
  "email": "user@example.com",
  "ip_address": "192.168.1.100",
  "user_agent": "Mozilla/5.0...",
  "success": true,
  "request_id": "kong-request-id"
}
```

#### 12.2.2 Security Events to Log

| Event | Log Level | Details |
|-------|-----------|---------|
| Login Success | INFO | user_id, ip, timestamp |
| Login Failure | WARN | email (masked), ip, reason |
| Account Locked | WARN | user_id, ip, failed_attempts |
| Password Changed | INFO | user_id, ip |
| Token Revoked | INFO | user_id, reason |
| Admin Action | INFO | admin_id, action, target_user |

### 12.3 Grafana Dashboards

1. **Auth Service Dashboard**
   - Login success/failure rates
   - Registration trends
   - Token refresh patterns
   - Active sessions

2. **Kong Security Dashboard**
   - JWT validation success/failure
   - Rate limit hits
   - Blocked requests
   - Auth endpoint latency

3. **User Activity Dashboard**
   - Daily active users
   - New registrations
   - Failed login patterns
   - Session duration

---

## 13. Performance Requirements

### 13.1 Performance Targets

| Component | Metric | Target | Maximum |
|-----------|--------|--------|---------|
| Kong (gateway latency) | P50 | < 2ms | 5ms |
| Kong (gateway latency) | P99 | < 10ms | 20ms |
| Auth Service (login) | P50 | < 100ms | 200ms |
| Auth Service (login) | P99 | < 300ms | 500ms |
| Auth Service (JWT validation) | P99 | < 5ms | 10ms |
| JWKS endpoint | Response time | < 50ms | 100ms |

### 13.2 Scalability

| Component | Min Replicas | Max Replicas | Scaling Trigger |
|-----------|--------------|--------------|-----------------|
| Kong Gateway | 2 | 10 | CPU > 70% |
| Auth Service | 2 | 5 | CPU > 70% |
| Backend Service | 2 | 10 | CPU > 70% |

### 13.3 Load Testing Scenarios

| Scenario | Target | Duration |
|----------|--------|----------|
| Auth endpoint load | 100 logins/sec | 30 min |
| JWT validation load | 10,000 requests/sec | 30 min |
| Mixed traffic | 5,000 requests/sec | 1 hour |
| Stress test (auth) | Ramp to 500 logins/sec | 1 hour |

---

## 14. Deployment Strategy

### 14.1 Environment Configuration

| Environment | Kong | Auth Service | Backend | Database |
|-------------|------|--------------|---------|----------|
| Local | 1 replica, DB-less | 1 replica | 1 replica | Docker |
| Development | 1 replica | 1 replica | 1 replica | RDS (shared) |
| Staging | 2 replicas | 2 replicas | 2 replicas | RDS (dedicated) |
| Production | 3+ replicas | 2+ replicas | 3+ replicas | RDS (HA) |

### 14.2 Deployment Order

```
1. Deploy Auth Service Database
   └── Run migrations
   └── Create initial admin user

2. Deploy Auth Service
   └── Generate RSA keys
   └── Verify JWKS endpoint

3. Deploy Kong Gateway
   └── Configure services and routes
   └── Configure JWT plugin with JWKS URL
   └── Verify JWT validation working

4. Update Backend Service
   └── Deploy with header extraction
   └── Verify user context propagation

5. Update Frontend
   └── Point to Kong endpoint
   └── Implement new auth flow
```

### 14.3 Rollback Procedures

| Component | Rollback Strategy |
|-----------|-------------------|
| Kong | Helm rollback, restore previous config |
| Auth Service | Helm rollback, database compatible |
| Backend | Helm rollback |
| Database | Point-in-time recovery |

---

## 15. Risk Assessment

### 15.1 Risk Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Auth Service becomes SPOF | Medium | Critical | Multi-replica, health checks |
| JWT key compromise | Low | Critical | Key rotation, monitoring |
| JWKS endpoint unavailable | Low | High | Kong caches keys, multiple replicas |
| Database corruption | Low | Critical | Regular backups, replication |
| Migration breaks auth | Medium | High | Staged rollout, feature flags |
| Performance degradation | Medium | Medium | Load testing, auto-scaling |

### 15.2 Mitigation Strategies

#### Auth Service Availability
- Deploy minimum 2 replicas
- Implement health checks and readiness probes
- Configure pod disruption budgets
- Kong caches JWKS (continues working if auth service briefly unavailable)

#### Security Incidents
- Implement key rotation automation
- Enable audit logging
- Set up security alerts
- Document incident response procedures

---

## 16. Success Metrics

### 16.1 Technical Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Auth Service uptime | 99.9% | Prometheus |
| Login success rate | > 99% (excluding invalid credentials) | Auth metrics |
| JWT validation latency | < 5ms P99 | Kong metrics |
| Token refresh success | > 99.9% | Auth metrics |

### 16.2 Security Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Unprotected endpoints | 0 | Route audit |
| Failed auth attempts | < 5% | Auth metrics |
| Security incidents | 0 critical | Incident tracking |
| Key rotation compliance | 100% | Automation logs |

### 16.3 User Experience Metrics

| Metric | Target | Measurement |
|--------|--------|-------------|
| Login time | < 500ms | Frontend metrics |
| Token refresh transparent | 100% | Frontend metrics |
| Session persistence | 7 days (with activity) | Auth metrics |

---

## 17. Dependencies and Prerequisites

### 17.1 Infrastructure Prerequisites

| Requirement | Status | Notes |
|-------------|--------|-------|
| Kubernetes Cluster | ✅ Available | EKS/Minikube |
| Helm 3.x | ✅ Available | Required |
| PostgreSQL (Auth DB) | ❌ Required | New database for auth |
| PostgreSQL (Kong DB) | ❌ Required | For Kong config |
| Redis | ❌ Required | For rate limiting |
| cert-manager | ❌ Recommended | For TLS certificates |

### 17.2 Application Prerequisites

| Requirement | Status | Notes |
|-------------|--------|-------|
| Auth Service | ❌ Required | New microservice to build |
| Backend header extraction | ❌ Required | Modify backend |
| Frontend auth update | ❌ Required | Update auth flow |
| Remove backend CORS | ⚠️ After Kong | After Kong CORS works |

### 17.3 Team Prerequisites

| Requirement | Description |
|-------------|-------------|
| Spring Security knowledge | For auth service development |
| Kong administration | For gateway configuration |
| JWT/OAuth understanding | For token implementation |

---

## 18. Future Considerations

### 18.1 Short-term Enhancements

| Enhancement | Priority | Description |
|-------------|----------|-------------|
| OAuth2/OIDC | P1 | Social login (Google, GitHub) |
| MFA/2FA | P1 | Two-factor authentication |
| Email verification | P1 | Verify user emails |
| Password policies | P2 | Configurable password rules |

### 18.2 Medium-term Enhancements

| Enhancement | Priority | Description |
|-------------|----------|-------------|
| Session management UI | P2 | Users can view/revoke sessions |
| API key management | P2 | Self-service API keys |
| Audit log UI | P2 | Admin view of security events |
| Rate limit tiers | P3 | Premium user limits |

### 18.3 Long-term Vision

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                         FUTURE IDENTITY PLATFORM                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      Identity Provider (Auth Service v2)                │   │
│  │                                                                         │   │
│  │  ├─ OAuth2/OIDC Server                                                 │   │
│  │  ├─ Social Login (Google, Facebook, Apple)                             │   │
│  │  ├─ Enterprise SSO (SAML, LDAP)                                        │   │
│  │  ├─ Multi-factor Authentication                                        │   │
│  │  ├─ Passwordless Authentication                                        │   │
│  │  └─ Device Trust / Risk-based Authentication                           │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                      API Gateway (Kong Enterprise)                      │   │
│  │                                                                         │   │
│  │  ├─ Developer Portal                                                   │   │
│  │  ├─ API Analytics                                                      │   │
│  │  ├─ API Monetization                                                   │   │
│  │  └─ Advanced Rate Limiting                                             │   │
│  └─────────────────────────────────────────────────────────────────────────┘   │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

---

## Appendix A: Reference Documentation

### Kong Documentation
- [Kong Gateway Documentation](https://docs.konghq.com/gateway/)
- [Kong JWT Plugin](https://docs.konghq.com/hub/kong-inc/jwt/)
- [Kong Helm Chart](https://github.com/Kong/charts)

### Spring Security Documentation
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Boot OAuth2](https://docs.spring.io/spring-security/reference/servlet/oauth2/index.html)

### JWT/JWKS Standards
- [RFC 7519 - JSON Web Token](https://tools.ietf.org/html/rfc7519)
- [RFC 7517 - JSON Web Key](https://tools.ietf.org/html/rfc7517)

---

## Appendix B: Glossary

| Term | Definition |
|------|------------|
| **JWT** | JSON Web Token - compact, URL-safe token format |
| **JWKS** | JSON Web Key Set - set of public keys for JWT verification |
| **RS256** | RSA Signature with SHA-256 - asymmetric signing algorithm |
| **Access Token** | Short-lived JWT for API authentication |
| **Refresh Token** | Long-lived token for obtaining new access tokens |
| **ACL** | Access Control List - permission groups |
| **Kong Consumer** | Entity representing an API client |
| **Upstream** | Backend service that Kong proxies to |

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
| 2.0 | 2026-01-26 | Engineering Team | Added Hybrid Auth Architecture with Auth Service |
