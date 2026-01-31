# Real Estate Application - Complete Deployment Architecture

## Table of Contents

1. [System Architecture Overview](#system-architecture-overview)
2. [Request Flow Navigation](#request-flow-navigation)
   - [Login Flow](#1-login-flow-public-route)
   - [Fetch Properties (Authenticated)](#2-fetch-all-properties-after-login-authenticated)
   - [Create Property](#3-create-new-property-protected-route)
   - [Social Feed Flow](#4-social-feed-post-service-flow)
3. [URL Mapping Reference](#url-mapping-reference)
4. [Kong API Gateway Architecture](#kong-api-gateway-architecture)
5. [Service Details](#auth-service-architecture)

---

## System Architecture Overview

```
                                    INTERNET
                                        │
                                        ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND (Vercel/Local)                          │
│                         Next.js 14 + React + NextAuth                         │
│                              http://localhost:3000                            │
└─────────────────────────────────────┬─────────────────────────────────────────┘
                                      │ HTTP/HTTPS
                                      ▼
┌───────────────────────────────────────────────────────────────────────────────┐
│                           KUBERNETES CLUSTER (EKS/Minikube)                   │
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                        KONG API GATEWAY                                  │ │
│  │                http://localhost:8000 (port-forward)                      │ │
│  │                                                                          │ │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐ │ │
│  │  │     JWT      │  │     Rate     │  │    CORS      │  │  Prometheus │ │ │
│  │  │  Validation  │  │   Limiting   │  │   Plugin     │  │   Metrics   │ │ │
│  │  └──────────────┘  └──────────────┘  └──────────────┘  └─────────────┘ │ │
│  │                                                                          │ │
│  │  Routes:                                                                 │ │
│  │  • /auth/*       → Auth Service (login, register, refresh, etc.)        │ │
│  │  • /api/*        → Backend/Post Service (properties, posts, etc.)       │ │
│  │  • /.well-known/jwks.json → Auth Service (public keys for JWT)          │ │
│  └──────────────────────────────┬───────────────────────────────────────────┘ │
│                                 │                                             │
│     ┌───────────────────────────┼───────────────────────────┐                 │
│     │                           │                           │                 │
│     ▼                           ▼                           ▼                 │
│  ┌───────────────┐      ┌───────────────┐      ┌───────────────┐             │
│  │ AUTH SERVICE  │      │    BACKEND    │      │ POST SERVICE  │             │
│  │ (Spring Boot) │      │ (Spring Boot) │      │ (Spring Boot) │             │
│  │               │      │               │      │               │             │
│  │ • User Mgmt   │      │ • Properties  │      │ • Posts/Feed  │             │
│  │ • JWT Issue   │      │ • File Upload │      │ • Comments    │             │
│  │ • Key Rotate  │      │ • Search      │      │ • Likes       │             │
│  │ • JWKS        │      │ • Admin       │      │               │             │
│  │               │      │               │      │               │             │
│  │ Port: 8081    │      │ Port: 8080    │      │ Port: 8082    │             │
│  └───────┬───────┘      └───────┬───────┘      └───────┬───────┘             │
│          │                      │                      │                      │
│          ▼                      ▼                      ▼                      │
│  ┌───────────────┐      ┌───────────────┐      ┌───────────────┐             │
│  │   Auth DB     │      │  Property DB  │      │   Post DB     │             │
│  │  PostgreSQL   │      │  PostgreSQL   │      │  PostgreSQL   │             │
│  │  Port: 5433   │      │  Port: 5432   │      │  Port: 5434   │             │
│  └───────────────┘      └───────────────┘      └───────────────┘             │
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐ │
│  │                          MONITORING STACK                                │ │
│  │           Prometheus (9090) │ Grafana (3001) │ AlertManager              │ │
│  └─────────────────────────────────────────────────────────────────────────┘ │
│                                                                               │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## Request Flow Navigation

This section provides detailed explanation of how requests navigate from the frontend through the system. Understanding these flows is essential for debugging and development.

### 1. Login Flow (Public Route)

**Scenario**: User submits login form with email and password

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Browser    │    │   Next.js    │    │     Kong     │    │ Auth Service │    │   Auth DB    │
│              │    │   Server     │    │   Gateway    │    │              │    │  PostgreSQL  │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │                   │                   │
       │ 1. Submit Login   │                   │                   │                   │
       │   Form            │                   │                   │                   │
       │──────────────────▶│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 2. POST /auth/login                   │                   │
       │                   │   {email, password}                   │                   │
       │                   │──────────────────▶│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 3. Rate Limit Check                   │
       │                   │                   │    (10 req/min)   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 4. CORS Validation│                   │
       │                   │                   │                   │                   │
       │                   │                   │ 5. Forward to     │                   │
       │                   │                   │    auth-service   │                   │
       │                   │                   │──────────────────▶│                   │
       │                   │                   │                   │                   │
       │                   │                   │                   │ 6. Validate       │
       │                   │                   │                   │    Credentials    │
       │                   │                   │                   │──────────────────▶│
       │                   │                   │                   │                   │
       │                   │                   │                   │◀──────────────────│
       │                   │                   │                   │    User Data      │
       │                   │                   │                   │                   │
       │                   │                   │                   │ 7. Generate JWT   │
       │                   │                   │                   │    (RS256)        │
       │                   │                   │                   │                   │
       │                   │                   │◀──────────────────│                   │
       │                   │                   │  {accessToken,    │                   │
       │                   │                   │   refreshToken,   │                   │
       │                   │                   │   user}           │                   │
       │                   │                   │                   │                   │
       │                   │◀──────────────────│                   │                   │
       │                   │                   │                   │                   │
       │                   │ 8. Store tokens   │                   │                   │
       │                   │    in NextAuth    │                   │                   │
       │                   │    session        │                   │                   │
       │                   │                   │                   │                   │
       │◀──────────────────│                   │                   │                   │
       │  Set session      │                   │                   │                   │
       │  cookie           │                   │                   │                   │
       │                   │                   │                   │                   │
```

**URL Mapping for Login:**

| Step | Component | URL | Method | Description |
|------|-----------|-----|--------|-------------|
| 1 | Browser → Next.js | `http://localhost:3000/api/auth/callback/credentials` | POST | NextAuth credentials callback |
| 2 | Next.js → Kong | `http://localhost:8000/auth/login` | POST | Kong public auth route |
| 3-5 | Kong (internal) | N/A | - | Rate limiting, CORS, routing |
| 6 | Kong → Auth Service | `http://auth-service.real-estate.svc:8081/auth/login` | POST | Internal K8s service call |

**Request/Response Example:**

```bash
# Request (Next.js → Kong)
POST http://localhost:8000/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123"
}

# Response (Auth Service → Kong → Next.js)
HTTP/1.1 200 OK
Content-Type: application/json

{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjEyMzQ1In0...",
  "refreshToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "a63d005d-cc68-40d5-ae60-a603264b8d78",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "roles": ["USER"]
  }
}
```

---

### 2. Fetch All Properties After Login (Authenticated)

**Scenario**: Authenticated user visits homepage, frontend fetches all properties

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Browser    │    │   Next.js    │    │     Kong     │    │   Backend    │    │ Property DB  │
│              │    │   (SSR)      │    │   Gateway    │    │   Service    │    │  PostgreSQL  │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │                   │                   │
       │ 1. GET /          │                   │                   │                   │
       │   (with session   │                   │                   │                   │
       │    cookie)        │                   │                   │                   │
       │──────────────────▶│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 2. SSR: Fetch     │                   │                   │
       │                   │    properties     │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 3. GET /api/properties                │                   │
       │                   │   (Public route - no JWT needed)      │                   │
       │                   │──────────────────▶│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 4. Rate Limit     │                   │
       │                   │                   │    (100 req/min)  │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 5. Route to       │                   │
       │                   │                   │    backend-service│                   │
       │                   │                   │──────────────────▶│                   │
       │                   │                   │                   │                   │
       │                   │                   │                   │ 6. Query DB      │
       │                   │                   │                   │──────────────────▶│
       │                   │                   │                   │                   │
       │                   │                   │                   │◀──────────────────│
       │                   │                   │                   │   Properties      │
       │                   │                   │                   │                   │
       │                   │                   │◀──────────────────│                   │
       │                   │                   │  {data: [...],    │                   │
       │                   │                   │   total, page}    │                   │
       │                   │                   │                   │                   │
       │                   │◀──────────────────│                   │                   │
       │                   │                   │                   │                   │
       │                   │ 7. Render HTML    │                   │                   │
       │                   │    with data      │                   │                   │
       │                   │                   │                   │                   │
       │◀──────────────────│                   │                   │                   │
       │  HTML Response    │                   │                   │                   │
       │                   │                   │                   │                   │
```

**URL Mapping for Fetch Properties (Public):**

| Step | Component | URL | Method | Auth Required |
|------|-----------|-----|--------|---------------|
| 1 | Browser → Next.js | `http://localhost:3000/` | GET | Session cookie |
| 3 | Next.js → Kong | `http://localhost:8000/api/properties?page=0&size=20` | GET | No |
| 5 | Kong → Backend | `http://real-estate-backend.real-estate.svc:8080/api/properties` | GET | No |

**Request/Response Example:**

```bash
# Request (Next.js SSR → Kong)
GET http://localhost:8000/api/properties?page=0&size=20&sortBy=createdAt&sortDirection=desc
X-Request-ID: 550e8400-e29b-41d4-a716-446655440000

# Response
HTTP/1.1 200 OK
Content-Type: application/json
X-Kong-Proxy-Latency: 5
X-Kong-Upstream-Latency: 45

{
  "data": [
    {
      "id": 8,
      "title": "Modern Apartment in District 1",
      "price": 500000000,
      "city": "Hồ Chí Minh",
      "propertyType": "APARTMENT",
      "status": "FOR_SALE",
      "images": ["http://localhost:8080/uploads/temp/image1.jpg"],
      "userId": "a63d005d-cc68-40d5-ae60-a603264b8d78",
      "createdAt": "2026-01-30T14:57:43.597798"
    }
  ],
  "total": 6,
  "page": 0,
  "perPage": 20,
  "totalPages": 1
}
```

---

### 3. Create New Property (Protected Route)

**Scenario**: Authenticated user creates a new property listing

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Browser    │    │   Next.js    │    │     Kong     │    │   Backend    │    │ Property DB  │
│              │    │   Client     │    │   Gateway    │    │   Service    │    │  PostgreSQL  │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │                   │                   │
       │ 1. Submit         │                   │                   │                   │
       │    Property Form  │                   │                   │                   │
       │──────────────────▶│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 2. Get accessToken│                   │                   │
       │                   │    from session   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 3. POST /api/properties               │                   │
       │                   │    Authorization: Bearer <JWT>        │                   │
       │                   │──────────────────▶│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 4. Extract JWT    │                   │
       │                   │                   │    from header    │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 5. Fetch JWKS     │                   │
       │                   │                   │    (cached)       │                   │
       │                   │                   │ ┌────────────────┐│                   │
       │                   │                   │ │  Auth Service  ││                   │
       │                   │                   │ │ /.well-known/  ││                   │
       │                   │                   │ │  jwks.json     ││                   │
       │                   │                   │ └────────────────┘│                   │
       │                   │                   │                   │                   │
       │                   │                   │ 6. Validate JWT   │                   │
       │                   │                   │    signature      │                   │
       │                   │                   │    (RS256)        │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 7. Check expiry   │                   │
       │                   │                   │    and claims     │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 8. Add headers:   │                   │
       │                   │                   │  X-Consumer-ID    │                   │
       │                   │                   │  X-Consumer-Username                  │
       │                   │                   │                   │                   │
       │                   │                   │ 9. Forward to     │                   │
       │                   │                   │    backend        │                   │
       │                   │                   │──────────────────▶│                   │
       │                   │                   │                   │                   │
       │                   │                   │                   │ 10. Extract user │
       │                   │                   │                   │     from JWT     │
       │                   │                   │                   │                   │
       │                   │                   │                   │ 11. Create       │
       │                   │                   │                   │     property     │
       │                   │                   │                   │──────────────────▶│
       │                   │                   │                   │                   │
       │                   │                   │                   │◀──────────────────│
       │                   │                   │                   │   Saved          │
       │                   │                   │                   │                   │
       │                   │                   │◀──────────────────│                   │
       │                   │                   │   201 Created     │                   │
       │                   │                   │   {property}      │                   │
       │                   │                   │                   │                   │
       │                   │◀──────────────────│                   │                   │
       │◀──────────────────│                   │                   │                   │
       │   Success         │                   │                   │                   │
```

**URL Mapping for Create Property (Protected):**

| Step | Component | URL | Method | Auth Required |
|------|-----------|-----|--------|---------------|
| 3 | Next.js → Kong | `http://localhost:8000/api/properties` | POST | Yes (JWT) |
| 5 | Kong → Auth (JWKS) | `http://auth-service.real-estate.svc:8081/.well-known/jwks.json` | GET | No (cached) |
| 9 | Kong → Backend | `http://real-estate-backend.real-estate.svc:8080/api/properties` | POST | Headers injected |

**Request/Response Example:**

```bash
# Request (Frontend → Kong)
POST http://localhost:8000/api/properties
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6IjEyMzQ1In0...

{
  "title": "Beautiful Villa in District 2",
  "description": "A modern 4-bedroom villa with garden",
  "price": 15000000000,
  "address": "123 Thao Dien",
  "city": "Hồ Chí Minh",
  "bedrooms": 4,
  "bathrooms": 3,
  "area": 300,
  "propertyType": "VILLA",
  "status": "FOR_SALE",
  "features": ["Swimming Pool", "Garden", "Garage"]
}

# Kong adds headers before forwarding:
X-Consumer-ID: a63d005d-cc68-40d5-ae60-a603264b8d78
X-Consumer-Username: user@example.com
X-Request-ID: 550e8400-e29b-41d4-a716-446655440001

# Response
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 9,
  "title": "Beautiful Villa in District 2",
  "userId": "a63d005d-cc68-40d5-ae60-a603264b8d78",
  "createdAt": "2026-01-31T10:30:00.000000",
  ...
}
```

---

### 4. Social Feed (Post Service) Flow

**Scenario**: User views social feed and likes a post

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│   Browser    │    │   Next.js    │    │     Kong     │    │ Post Service │    │   Post DB    │
│              │    │              │    │   Gateway    │    │              │    │  PostgreSQL  │
└──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘    └──────┬───────┘
       │                   │                   │                   │                   │
       │ 1. Visit /feed    │                   │                   │                   │
       │──────────────────▶│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 2. GET /api/posts │                   │                   │
       │                   │    (Public)       │                   │                   │
       │                   │──────────────────▶│                   │                   │
       │                   │                   │──────────────────▶│                   │
       │                   │                   │                   │──────────────────▶│
       │                   │                   │                   │◀──────────────────│
       │                   │                   │◀──────────────────│                   │
       │                   │◀──────────────────│                   │                   │
       │◀──────────────────│  Feed Data        │                   │                   │
       │                   │                   │                   │                   │
       │ 3. Click Like     │                   │                   │                   │
       │──────────────────▶│                   │                   │                   │
       │                   │                   │                   │                   │
       │                   │ 4. POST /api/posts/{id}/like          │                   │
       │                   │    Authorization: Bearer <JWT>        │                   │
       │                   │──────────────────▶│                   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 5. Validate JWT   │                   │
       │                   │                   │                   │                   │
       │                   │                   │ 6. Forward        │                   │
       │                   │                   │──────────────────▶│                   │
       │                   │                   │                   │──────────────────▶│
       │                   │                   │                   │◀──────────────────│
       │                   │                   │◀──────────────────│                   │
       │                   │◀──────────────────│                   │                   │
       │◀──────────────────│  Like Updated     │                   │                   │
```

**URL Mapping for Post Service:**

| Action | Frontend URL | Kong Route | Backend URL | Auth |
|--------|--------------|------------|-------------|------|
| Get Feed | `/feed` | `GET /api/posts` | `post-service:8082/api/posts` | No |
| Get My Posts | `/feed/me` | `GET /api/posts/me` | `post-service:8082/api/posts/me` | Yes |
| Create Post | `/feed` | `POST /api/posts` | `post-service:8082/api/posts` | Yes |
| Like Post | `/feed` | `POST /api/posts/{id}/like` | `post-service:8082/api/posts/{id}/like` | Yes |
| Search Posts | `/feed/search` | `GET /api/posts/search` | `post-service:8082/api/posts/search` | No |

---

## URL Mapping Reference

### Complete Route Table

| Route Name | Path Pattern | Methods | Service | Port | Auth | Rate Limit |
|------------|--------------|---------|---------|------|------|------------|
| **Authentication** |
| auth-public | `/auth/login` | POST | auth-service | 8081 | No | 10/min |
| auth-public | `/auth/register` | POST | auth-service | 8081 | No | 10/min |
| auth-public | `/auth/refresh` | POST | auth-service | 8081 | No | 10/min |
| auth-public | `/auth/forgot-password` | POST | auth-service | 8081 | No | 10/min |
| auth-public | `/auth/reset-password` | POST | auth-service | 8081 | No | 10/min |
| auth-protected | `/auth/logout` | POST | auth-service | 8081 | JWT | 100/min |
| auth-protected | `/auth/me` | GET | auth-service | 8081 | JWT | 100/min |
| auth-protected | `/auth/change-password` | POST | auth-service | 8081 | JWT | 100/min |
| auth-jwks | `/.well-known/jwks.json` | GET | auth-service | 8081 | No | 1000/min |
| **Properties** |
| properties-public-get | `/api/properties` | GET | backend-service | 8080 | No | 100/min |
| properties-protected | `/api/properties` | POST,PUT,DELETE | backend-service | 8080 | JWT | 100/min |
| properties-search | `/api/properties/search` | POST | backend-service | 8080 | No | 100/min |
| properties-cities | `/api/properties/cities` | GET | backend-service | 8080 | No | 100/min |
| properties-user | `/api/properties/user` | GET | backend-service | 8080 | JWT | 100/min |
| **Posts (Social Feed)** |
| posts-public-get | `/api/posts` | GET | post-service | 8082 | No | 100/min |
| posts-protected | `/api/posts` | POST,PUT,DELETE | post-service | 8082 | JWT | 100/min |
| posts-me | `/api/posts/me` | GET | post-service | 8082 | JWT | 100/min |
| posts-user | `/api/posts/user` | GET | post-service | 8082 | No | 100/min |
| posts-search | `/api/posts/search` | GET | post-service | 8082 | No | 100/min |
| posts-like | `/api/posts/{id}/like` | POST | post-service | 8082 | JWT | 100/min |
| **File Upload** |
| upload-routes | `/api/upload/*` | POST | backend-service | 8080 | JWT | 50/min |
| uploads-static | `/uploads/*` | GET | backend-service | 8080 | No | 200/min |
| **Admin** |
| admin-routes | `/api/admin/*` | ALL | backend-service | 8080 | JWT+ACL | 100/min |
| users-admin | `/users/*` | ALL | auth-service | 8081 | JWT+ACL | 100/min |

### Service Endpoints (Internal K8s DNS)

| Service | Internal DNS | Port | Namespace |
|---------|--------------|------|-----------|
| Kong Proxy | `kong-proxy.kong.svc.cluster.local` | 8000 | kong |
| Kong Admin | `kong-admin.kong.svc.cluster.local` | 8001 | kong |
| Auth Service | `auth-service.real-estate.svc.cluster.local` | 8081 | real-estate |
| Backend Service | `real-estate-backend-backend.real-estate.svc.cluster.local` | 8080 | real-estate |
| Post Service | `post-service.real-estate.svc.cluster.local` | 8082 | real-estate |

### Local Development Port Mapping

| Service | Local URL | K8s Port-Forward Command |
|---------|-----------|-------------------------|
| Frontend | `http://localhost:3000` | N/A (npm run dev) |
| Kong Gateway | `http://localhost:8000` | `kubectl port-forward svc/kong-proxy 8000:80 -n kong` |
| Kong Admin | `http://localhost:8001` | `kubectl port-forward svc/kong-admin 8001:8001 -n kong` |
| Backend | `http://localhost:8080` | `kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate` |
| Auth Service | `http://localhost:8081` | `kubectl port-forward svc/auth-service 8081:8081 -n real-estate` |
| Post Service | `http://localhost:8082` | `kubectl port-forward svc/post-service 8082:8082 -n real-estate` |

### Database Connections (Docker)

| Database | Container Name | Host Port | Internal Port | Database Name |
|----------|---------------|-----------|---------------|---------------|
| Property DB | real-estate-postgres-local | 5432 | 5432 | realestatedb |
| Auth DB | auth-db | 5433 | 5432 | authdb |
| Post DB | post-db | 5434 | 5432 | postdb |

---

### JWT Token Structure

The JWT tokens issued by Auth Service contain:

```json
{
  "header": {
    "alg": "RS256",
    "typ": "JWT",
    "kid": "key-id-12345"  // Key ID for JWKS lookup
  },
  "payload": {
    "sub": "a63d005d-cc68-40d5-ae60-a603264b8d78",  // User ID
    "email": "user@example.com",
    "roles": ["USER"],
    "iat": 1706698200,      // Issued at
    "exp": 1706699100,      // Expires (15 min for access token)
    "iss": "auth-service"   // Issuer
  }
}
```

### Headers Added by Kong

When Kong validates a JWT and forwards the request, it adds these headers:

| Header | Value | Description |
|--------|-------|-------------|
| `X-Consumer-ID` | User UUID | Extracted from JWT `sub` claim |
| `X-Consumer-Username` | Email | Extracted from JWT `email` claim |
| `X-Request-ID` | UUID | Correlation ID for tracing |
| `X-Forwarded-For` | Client IP | Original client IP |
| `X-Forwarded-Proto` | http/https | Original protocol |

---

## Kong API Gateway Architecture

### Overview

Kong Gateway serves as the central entry point for all API traffic, providing:

- **Stateless JWT Validation**: Validates tokens using JWKS from Auth Service
- **Rate Limiting**: Protects against abuse (10 req/min for auth, 100 req/min for API)
- **CORS Handling**: Centralized cross-origin configuration
- **Request/Response Transformation**: Header manipulation, path rewriting
- **Observability**: Prometheus metrics, request logging, correlation IDs

### Kong Configuration

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            KONG SERVICES                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  Service            │ Host                                    │ Port │      │
├─────────────────────┼─────────────────────────────────────────┼──────┼──────┤
│  auth-service       │ auth-service.real-estate.svc            │ 8081 │ HTTP │
│  backend-service    │ real-estate-backend-backend.real-estate │ 8080 │ HTTP │
│  post-service       │ post-service.real-estate.svc            │ 8082 │ HTTP │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              KONG ROUTES                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│  Route                  │ Paths                           │ Auth Required   │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  auth-public            │ /auth/login, /auth/register,    │ No              │
│                         │ /auth/refresh, /auth/forgot-    │                 │
│                         │ password, /auth/reset-password  │                 │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  auth-protected         │ /auth/logout, /auth/me,         │ Yes (JWT)       │
│                         │ /auth/change-password           │                 │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  auth-jwks              │ /.well-known/jwks.json          │ No              │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  properties-public-get  │ /api/properties (GET)           │ No              │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  properties-protected   │ /api/properties (POST,PUT,DEL)  │ Yes (JWT)       │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  properties-user        │ /api/properties/user            │ Yes (JWT)       │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  properties-search      │ /api/properties/search          │ No              │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  properties-cities      │ /api/properties/cities          │ No              │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  posts-public-get       │ /api/posts (GET)                │ No              │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  posts-protected        │ /api/posts (POST,PUT,DEL)       │ Yes (JWT)       │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  posts-me               │ /api/posts/me                   │ Yes (JWT)       │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  posts-user             │ /api/posts/user                 │ No              │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  posts-search           │ /api/posts/search               │ No              │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  posts-like             │ /api/posts/{id}/like            │ Yes (JWT)       │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  admin-routes           │ /api/admin/*                    │ Yes (JWT+Admin) │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  upload-routes          │ /api/upload/*                   │ Yes (JWT)       │
├─────────────────────────┼─────────────────────────────────┼─────────────────┤
│  uploads-static         │ /uploads/*                      │ No              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                            KONG PLUGINS                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│  Plugin               │ Scope    │ Configuration                            │
├───────────────────────┼──────────┼──────────────────────────────────────────┤
│  jwt                  │ Route    │ JWKS URI: auth-service/.well-known/jwks  │
│                       │          │ Algorithm: RS256                         │
├───────────────────────┼──────────┼──────────────────────────────────────────┤
│  rate-limiting        │ Global   │ Auth: 10/min, API: 100/min               │
│                       │          │ Policy: redis (cluster-aware)            │
├───────────────────────┼──────────┼──────────────────────────────────────────┤
│  cors                 │ Global   │ Origins: localhost:3000, vercel.app      │
│                       │          │ Methods: GET,POST,PUT,DELETE,OPTIONS     │
│                       │          │ Headers: Authorization, Content-Type     │
├───────────────────────┼──────────┼──────────────────────────────────────────┤
│  prometheus           │ Global   │ Metrics endpoint: /metrics               │
├───────────────────────┼──────────┼──────────────────────────────────────────┤
│  correlation-id       │ Global   │ Header: X-Request-ID                     │
├───────────────────────┼──────────┼──────────────────────────────────────────┤
│  request-size-limiting│ Global   │ Max payload: 10MB                        │
├───────────────────────┼──────────┼──────────────────────────────────────────┤
│  acl                  │ Route    │ Admin routes: require 'admin' group      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### JWT Validation Flow

```
┌─────────┐     ┌──────────┐     ┌─────────────┐     ┌─────────────┐
│ Client  │────▶│   Kong   │────▶│ Auth Service│     │   Backend   │
└─────────┘     └──────────┘     └─────────────┘     └─────────────┘
     │               │                  │                   │
     │  1. Request   │                  │                   │
     │  with JWT     │                  │                   │
     │──────────────▶│                  │                   │
     │               │                  │                   │
     │               │  2. Fetch JWKS   │                   │
     │               │  (cached)        │                   │
     │               │─────────────────▶│                   │
     │               │◀─────────────────│                   │
     │               │                  │                   │
     │               │  3. Validate JWT │                   │
     │               │  locally         │                   │
     │               │                  │                   │
     │               │  4. Forward with │                   │
     │               │  X-Consumer-*    │                   │
     │               │─────────────────────────────────────▶│
     │               │                  │                   │
     │               │◀─────────────────────────────────────│
     │◀──────────────│                  │                   │
     │  5. Response  │                  │                   │
```

---

## Auth Service Architecture

### Overview

The Auth Service is a dedicated Spring Boot application handling:

- User registration and authentication
- JWT token issuance (access + refresh tokens)
- JWKS endpoint for Kong JWT validation
- Automatic key rotation (30-day cycle)
- Password reset flows
- Account security (lockout, rate limiting)

### Key Components

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           AUTH SERVICE                                       │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌───────────────────┐   ┌───────────────────┐   ┌───────────────────────┐ │
│  │   Controllers     │   │     Services      │   │    Repositories       │ │
│  │                   │   │                   │   │                       │ │
│  │ • AuthController  │──▶│ • AuthService     │──▶│ • UserRepository      │ │
│  │ • AdminController │   │ • JwtService      │   │ • RsaKeyRepository    │ │
│  │                   │   │ • KeyRotation     │   │ • RefreshToken        │ │
│  │                   │   │   Scheduler       │   │   Repository          │ │
│  └───────────────────┘   └───────────────────┘   └───────────────────────┘ │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                        Security Configuration                          │  │
│  │  • BCrypt password encoding                                           │  │
│  │  • RS256 JWT signing                                                  │  │
│  │  • Configurable token expiration (15min access, 7day refresh)        │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                           Endpoints                                    │  │
│  │                                                                        │  │
│  │  POST /auth/register      - User registration                         │  │
│  │  POST /auth/login         - User authentication                       │  │
│  │  POST /auth/refresh       - Token refresh                             │  │
│  │  POST /auth/logout        - Token invalidation                        │  │
│  │  GET  /auth/me            - Current user info                         │  │
│  │  POST /auth/forgot-password - Password reset request                  │  │
│  │  POST /auth/reset-password  - Password reset confirmation             │  │
│  │  POST /auth/change-password - Authenticated password change           │  │
│  │  GET  /.well-known/jwks.json - Public keys for JWT validation        │  │
│  │  GET  /admin/keys/stats   - Key rotation statistics (admin only)     │  │
│  │  POST /admin/keys/rotate  - Manual key rotation (admin only)         │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Key Rotation

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         KEY ROTATION LIFECYCLE                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Day 0           Day 23          Day 30          Day 37                     │
│    │               │               │               │                         │
│    ▼               ▼               ▼               ▼                         │
│  ┌─────┐        ┌─────┐        ┌─────┐        ┌─────┐                       │
│  │Key A│        │Key A│        │Key B│        │Key B│                       │
│  │ACTIVE│        │ACTIVE│        │ACTIVE│        │ACTIVE│                    │
│  └─────┘        └──┬──┘        └─────┘        └─────┘                       │
│                    │              ▲                                          │
│                    │   ┌─────┐   │                                          │
│                    └──▶│Key B│───┘                                          │
│                        │GRACE│                                              │
│                        └─────┘                                              │
│                                                                              │
│  • Max key age: 30 days                                                     │
│  • Grace period: 7 days (old key still valid for existing tokens)          │
│  • Rotation check: Daily at 2 AM                                            │
│  • JWKS exposes all active keys                                             │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## Monitoring & Observability Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         MONITORING ARCHITECTURE                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                          PROMETHEUS                                    │  │
│  │                      (Metrics Collection)                              │  │
│  │                                                                        │  │
│  │  Scrape Targets:                                                       │  │
│  │  • Kong Gateway    (/metrics)    - Request counts, latencies          │  │
│  │  • Auth Service    (/actuator/prometheus) - Login/register metrics    │  │
│  │  • Backend Service (/actuator/prometheus) - Business metrics          │  │
│  │                                                                        │  │
│  └───────────────────────────────────┬───────────────────────────────────┘  │
│                                      │                                       │
│                                      ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                           GRAFANA                                      │  │
│  │                       (Visualization)                                  │  │
│  │                                                                        │  │
│  │  Dashboards:                                                           │  │
│  │  • Kong Gateway Overview    - Traffic, errors, latency                │  │
│  │  • Auth Service Dashboard   - Logins, registrations, failures         │  │
│  │  • Backend Service Dashboard - API calls, DB queries                  │  │
│  │  • Infrastructure Overview  - CPU, memory, pods                       │  │
│  │                                                                        │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                      │                                       │
│                                      ▼                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                        ALERTMANAGER                                    │  │
│  │                         (Alerting)                                     │  │
│  │                                                                        │  │
│  │  Alert Rules:                                                          │  │
│  │  • High error rate (5xx > 5%)                                         │  │
│  │  • Service down                                                        │  │
│  │  • High latency (p95 > 2s)                                            │  │
│  │  • Rate limit exceeded frequently                                      │  │
│  │  • Failed login spike                                                  │  │
│  │                                                                        │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Custom Auth Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `auth_login_total` | Counter | Total login attempts |
| `auth_login_success_total` | Counter | Successful logins |
| `auth_login_failure_total` | Counter | Failed logins |
| `auth_registration_total` | Counter | Total registrations |
| `auth_token_refresh_total` | Counter | Token refresh attempts |
| `auth_password_reset_total` | Counter | Password reset requests |
| `auth_login_latency_seconds` | Histogram | Login request latency |

---

## CI/CD Pipeline Architecture

```
┌──────────────┐
│   Developer  │
│ Commits Code │
└──────┬───────┘
       │
       ▼
┌──────────────┐         ┌─────────────────┐
│    GitHub    │ ◀──────▶│  GitHub Webhook │
│  Repository  │         └─────────┬───────┘
└──────────────┘                   │
       │                           │ Triggers
       │ Webhook/Poll              │
       ▼                           ▼
┌──────────────────────────────────────────────────────────┐
│                    JENKINS SERVER                         │
│                                                            │
│  ┌─────────────────────────────────────────────────────┐ │
│  │  Stage 1: Build & Test                              │ │
│  │  - Checkout code                                    │ │
│  │  - Maven build (Backend + Auth Service)             │ │
│  │  - Unit tests + Integration tests                   │ │
│  └────────────────┬────────────────────────────────────┘ │
│                   │                                       │
│  ┌────────────────▼────────────────────────────────────┐ │
│  │  Stage 2: Code Quality & Security                   │ │
│  │  - SonarQube analysis                               │ │
│  │  - OWASP dependency check                           │ │
│  │  - Trivy vulnerability scan                         │ │
│  └────────────────┬────────────────────────────────────┘ │
│                   │                                       │
│  ┌────────────────▼────────────────────────────────────┐ │
│  │  Stage 3: Build Docker Images                       │ │
│  │  - real-estate-backend:tag                          │ │
│  │  - auth-service:tag                                 │ │
│  │  - Trivy image scan                                 │ │
│  └────────────────┬────────────────────────────────────┘ │
│                   │                                       │
│  ┌────────────────▼────────────────────────────────────┐ │
│  │  Stage 4: Push to ECR                               │ │
│  │  - Push backend and auth-service images             │ │
│  └────────────────┬────────────────────────────────────┘ │
│                   │                                       │
│  ┌────────────────▼────────────────────────────────────┐ │
│  │  Stage 5: Deploy to EKS                             │ │
│  │  - Helm upgrade kong                                │ │
│  │  - Helm upgrade auth-service                        │ │
│  │  - Helm upgrade backend                             │ │
│  │  - Helm upgrade monitoring                          │ │
│  └────────────────┬────────────────────────────────────┘ │
│                   │                                       │
│  ┌────────────────▼────────────────────────────────────┐ │
│  │  Stage 6: Integration Tests                         │ │
│  │  - Kong integration tests                           │ │
│  │  - Auth flow tests                                  │ │
│  │  - API smoke tests                                  │ │
│  └─────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────────┘
```

---

## Security Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    SECURITY LAYERS                           │
│                                                               │
│  1. Network Layer                                            │
│     - VPC with private/public subnets                       │
│     - Security groups (least privilege)                     │
│     - Network policies (K8s)                                │
│     - Kong Admin API internal only                          │
│                                                               │
│  2. API Gateway Layer (Kong)                                 │
│     - TLS termination (cert-manager)                        │
│     - Rate limiting (abuse protection)                      │
│     - Request size limiting (10MB max)                      │
│     - CORS enforcement                                      │
│     - Security headers (HSTS, X-Frame-Options, etc.)        │
│                                                               │
│  3. Authentication Layer                                     │
│     - JWT tokens (RS256 signed)                             │
│     - Automatic key rotation (30-day)                       │
│     - Refresh token rotation                                │
│     - Account lockout (5 failed attempts)                   │
│     - Password requirements (12+ chars production)          │
│                                                               │
│  4. Authorization Layer                                      │
│     - Role-based access control (USER, ADMIN)               │
│     - Resource ownership validation                         │
│     - ACL plugin for admin routes                           │
│                                                               │
│  5. Database Layer                                          │
│     - RDS in private subnet                                 │
│     - Encryption at rest (AES-256)                          │
│     - Encryption in transit (TLS)                           │
│     - Secrets Manager for credentials                       │
│                                                               │
│  6. Container Security                                       │
│     - Non-root user                                         │
│     - Read-only root filesystem                             │
│     - Pod security policies                                 │
│     - Image vulnerability scanning                          │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## High Availability Configuration

### Production Deployment

| Component | Replicas | HPA | PDB |
|-----------|----------|-----|-----|
| Kong Gateway | 3 | 3-10 (CPU 70%) | minAvailable: 2 |
| Auth Service | 3 | 3-10 (CPU 70%) | minAvailable: 2 |
| Backend Service | 3 | 3-15 (CPU 70%) | minAvailable: 2 |
| Post Service | 3 | 3-10 (CPU 70%) | minAvailable: 2 |
| PostgreSQL (Auth) | Managed (RDS Multi-AZ) | N/A | N/A |
| PostgreSQL (Property) | Managed (RDS Multi-AZ) | N/A | N/A |
| PostgreSQL (Post) | Managed (RDS Multi-AZ) | N/A | N/A |

### Anti-Affinity Rules

All services configured with pod anti-affinity to spread across nodes:

```yaml
affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
      - labelSelector:
          matchExpressions:
            - key: app
              operator: In
              values:
                - <service-name>
        topologyKey: kubernetes.io/hostname
```

### Topology Spread

Zone redundancy for production:

```yaml
topologySpreadConstraints:
  - maxSkew: 1
    topologyKey: topology.kubernetes.io/zone
    whenUnsatisfiable: ScheduleAnyway
```

---

## Infrastructure as Code

```
project-root/
├── kong/
│   └── helm/
│       └── kong/
│           ├── Chart.yaml
│           ├── values.yaml           # Development
│           ├── values-production.yaml # Production
│           └── templates/
│               ├── deployment.yaml
│               ├── service.yaml
│               ├── configmap.yaml
│               ├── hpa.yaml
│               ├── pdb.yaml
│               ├── tls-certificate.yaml
│               └── cluster-issuer.yaml
│
├── auth-service/
│   └── helm/
│       └── auth-service/
│           ├── Chart.yaml
│           ├── values.yaml
│           ├── values-production.yaml
│           └── templates/
│               ├── deployment.yaml
│               ├── service.yaml
│               ├── configmap.yaml
│               ├── hpa.yaml
│               ├── pdb.yaml
│               ├── networkpolicy.yaml
│               └── key-rotation-cronjob.yaml
│
├── backend/
│   └── helm/
│       └── real-estate-backend/
│           ├── Chart.yaml
│           ├── values.yaml
│           ├── values-local.yaml
│           ├── values-production.yaml
│           └── templates/
│               ├── deployment.yaml
│               ├── service.yaml
│               ├── configmap.yaml
│               ├── hpa.yaml
│               ├── pdb.yaml
│               └── networkpolicy.yaml
│
├── post-service/
│   └── helm/
│       └── post-service/
│           ├── Chart.yaml
│           ├── values.yaml
│           ├── values-local.yaml
│           ├── values-production.yaml
│           └── templates/
│               ├── deployment.yaml
│               ├── service.yaml
│               ├── configmap.yaml
│               ├── secret.yaml
│               ├── hpa.yaml
│               ├── pdb.yaml
│               └── postgres.yaml
│
├── monitoring/
│   └── helm/
│       └── monitoring/
│           ├── Chart.yaml
│           ├── values.yaml
│           └── templates/
│               ├── prometheus/
│               ├── grafana/
│               └── alertmanager/
│
└── terraform/
    └── environments/
        ├── dev/
        ├── staging/
        └── prod/
```

---

## Cost Breakdown (Monthly)

### Development Environment (Minikube/Local)
| Service | Configuration | Cost |
|---------|---------------|------|
| Local Development | Minikube | $0 |
| PostgreSQL | Local/Docker | $0 |
| **Total** | | **$0** |

### Production Environment (AWS)
| Service | Configuration | Cost |
|---------|---------------|------|
| EKS Control Plane | 1 cluster | $72 |
| EC2 Nodes | 4× t3.large | $250 |
| RDS PostgreSQL | db.t3.small Multi-AZ × 2 | $160 |
| ECR | Image storage | $5 |
| Load Balancer | ALB | $25 |
| ElastiCache Redis | cache.t3.micro | $15 |
| CloudWatch | Logs & metrics | $20 |
| Secrets Manager | 10 secrets | $5 |
| **Total** | | **~$552** |

---

## Quick Start (Local Development)

### Prerequisites
- Docker Desktop
- Minikube
- kubectl
- Helm 3

### Deploy Stack

```bash
# Start Minikube
minikube start --memory 8192 --cpus 4

# Deploy Kong
helm install kong ./kong/helm/kong -n kong --create-namespace

# Deploy Auth Service
helm install auth-service ./auth-service/helm/auth-service -n real-estate --create-namespace

# Deploy Backend
helm install backend ./backend/helm/real-estate-backend -n real-estate

# Deploy Monitoring
helm install monitoring ./monitoring/helm/monitoring -n monitoring --create-namespace

# Port forward Kong
kubectl port-forward -n kong svc/kong-proxy 8000:80 &
kubectl port-forward -n kong svc/kong-admin 8001:8001 &

# Run integration tests
./kong/scripts/test-kong-integration.sh
```

### Frontend Configuration

```bash
# .env.local
NEXT_PUBLIC_KONG_URL=http://127.0.0.1:8000
NEXT_PUBLIC_API_URL=http://127.0.0.1:8000/api
NEXT_PUBLIC_AUTH_API_URL=http://127.0.0.1:8000
```

---

## Summary

The Real Estate application now has:

- **Kong API Gateway**: Centralized entry point with JWT validation, rate limiting, CORS
- **Auth Service**: Dedicated authentication microservice with key rotation
- **Backend Service**: Property management, file uploads, and search
- **Post Service**: Social feed functionality (posts, comments, likes)
- **Monitoring Stack**: Prometheus, Grafana, AlertManager
- **Production-ready Infrastructure**: HPA, PDB, network policies, TLS
- **CI/CD Pipeline**: Automated build, test, and deployment
- **Security**: Multi-layer security with encryption, access control, audit logging

### Architecture Highlights

| Feature | Implementation |
|---------|----------------|
| API Gateway | Kong with JWT plugin (RS256) |
| Authentication | Stateless JWT with automatic key rotation |
| Database per Service | Separate PostgreSQL for each microservice |
| Service Discovery | Kubernetes DNS (*.svc.cluster.local) |
| Rate Limiting | Redis-backed distributed rate limiting |
| Observability | Prometheus metrics + Grafana dashboards |

---

## Quick Reference Card

### Start Local Development Environment

```bash
# 1. Start databases (Docker)
cd backend && docker-compose -f docker-compose-db.yml up -d
cd ../auth-service && docker-compose -f docker-compose-db.yml up -d
cd ../post-service && docker-compose -f docker-compose-db.yml up -d

# 2. Start Kubernetes services
kubectl port-forward svc/kong-proxy 8000:80 -n kong &
kubectl port-forward svc/real-estate-backend-backend 8080:8080 -n real-estate &
kubectl port-forward svc/auth-service 8081:8081 -n real-estate &
kubectl port-forward svc/post-service 8082:8082 -n real-estate &

# 3. Start frontend
cd frontend && npm run dev
```

### Test API Endpoints

```bash
# Health checks
curl http://localhost:8000/api/properties          # Backend via Kong
curl http://localhost:8000/auth/login              # Auth via Kong (POST)
curl http://localhost:8000/api/posts               # Posts via Kong

# Direct service access (bypass Kong)
curl http://localhost:8080/actuator/health         # Backend direct
curl http://localhost:8081/actuator/health         # Auth direct
curl http://localhost:8082/actuator/health         # Posts direct

# Get JWT token
TOKEN=$(curl -s -X POST http://localhost:8000/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password"}' \
  | jq -r '.accessToken')

# Authenticated request
curl http://localhost:8000/api/properties/user \
  -H "Authorization: Bearer $TOKEN"
```

### Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| `fetch failed` on frontend | Backend not accessible | Check port-forward is running |
| `401 Unauthorized` | JWT expired or invalid | Login again to get new token |
| `502 Bad Gateway` from Kong | Backend service down | Check pod status: `kubectl get pods -n real-estate` |
| `Connection refused :8000` | Kong port-forward not running | Run: `kubectl port-forward svc/kong-proxy 8000:80 -n kong` |
| Database connection error | Docker DB not running | Run: `docker-compose -f docker-compose-db.yml up -d` |

---

**Version:** 3.0.0
**Last Updated:** 2026-01-31
**Changes in v3.0.0:**
- Added detailed request flow navigation diagrams
- Added complete URL mapping reference tables
- Added Post Service documentation
- Added JWT token structure explanation
- Added quick reference card for development
