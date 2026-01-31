#!/bin/bash
set -e

# Configure Kong services and routes via Admin API
# Usage: ./configure-routes.sh <KONG_ADMIN_URL>
# Example: ./configure-routes.sh http://127.0.0.1:52345

KONG_ADMIN="${1:-http://localhost:30001}"

echo "=== Configuring Kong routes via Admin API ==="
echo "Admin URL: $KONG_ADMIN"

# Verify Kong is reachable
curl -s "$KONG_ADMIN/" > /dev/null || { echo "ERROR: Cannot reach Kong Admin at $KONG_ADMIN"; exit 1; }

# ============================================================================
# SERVICES
# ============================================================================
echo ""
echo "[1/2] Creating services..."

curl -s -X PUT "$KONG_ADMIN/services/auth-service" \
  -d url=http://auth-service.real-estate.svc.cluster.local:8081 \
  -d connect_timeout=10000 \
  -d read_timeout=60000 \
  -d write_timeout=60000 \
  -d retries=3 | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'  auth-service -> {d.get(\"host\",\"ERROR\")}')" 2>/dev/null || echo "  auth-service created"

curl -s -X PUT "$KONG_ADMIN/services/backend-service" \
  -d url=http://real-estate-backend-backend.real-estate.svc.cluster.local:8080 \
  -d connect_timeout=10000 \
  -d read_timeout=60000 \
  -d write_timeout=60000 \
  -d retries=3 | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'  backend-service -> {d.get(\"host\",\"ERROR\")}')" 2>/dev/null || echo "  backend-service created"

curl -s -X PUT "$KONG_ADMIN/services/post-service" \
  -d url=http://post-service.real-estate.svc.cluster.local:8082 \
  -d connect_timeout=10000 \
  -d read_timeout=60000 \
  -d write_timeout=60000 \
  -d retries=3 | python3 -c "import sys,json; d=json.load(sys.stdin); print(f'  post-service -> {d.get(\"host\",\"ERROR\")}')" 2>/dev/null || echo "  post-service created"

# ============================================================================
# ROUTES
# ============================================================================
echo ""
echo "[2/2] Creating routes..."

# --- Auth public routes ---
curl -s -X PUT "$KONG_ADMIN/services/auth-service/routes/auth-public" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/auth/login", "/auth/register", "/auth/refresh", "/auth/forgot-password", "/auth/reset-password"],
    "methods": ["POST"],
    "strip_path": false
  }' > /dev/null
echo "  auth-public (POST /auth/login,register,refresh,...)"

# --- JWKS ---
curl -s -X PUT "$KONG_ADMIN/services/auth-service/routes/auth-jwks" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/.well-known/jwks.json"],
    "methods": ["GET"],
    "strip_path": false
  }' > /dev/null
echo "  auth-jwks (GET /.well-known/jwks.json)"

# --- Auth protected routes ---
curl -s -X PUT "$KONG_ADMIN/services/auth-service/routes/auth-protected" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/auth/logout", "/auth/change-password", "/auth/me"],
    "strip_path": false
  }' > /dev/null
echo "  auth-protected (/auth/logout,change-password,me)"

# --- User admin routes ---
curl -s -X PUT "$KONG_ADMIN/services/auth-service/routes/users-admin" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/users"],
    "strip_path": false
  }' > /dev/null
echo "  users-admin (/users)"

# --- Backend public GET ---
curl -s -X PUT "$KONG_ADMIN/services/backend-service/routes/properties-public-get" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/properties"],
    "methods": ["GET"],
    "strip_path": false
  }' > /dev/null
echo "  properties-public-get (GET /api/properties)"

# --- Search ---
curl -s -X PUT "$KONG_ADMIN/services/backend-service/routes/properties-search" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/properties/search"],
    "methods": ["POST"],
    "strip_path": false
  }' > /dev/null
echo "  properties-search (POST /api/properties/search)"

# --- Cities ---
curl -s -X PUT "$KONG_ADMIN/services/backend-service/routes/properties-cities" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/properties/cities"],
    "methods": ["GET"],
    "strip_path": false
  }' > /dev/null
echo "  properties-cities (GET /api/properties/cities)"

# --- Properties protected ---
curl -s -X PUT "$KONG_ADMIN/services/backend-service/routes/properties-protected" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/properties"],
    "methods": ["POST", "PUT", "DELETE"],
    "strip_path": false
  }' > /dev/null
echo "  properties-protected (POST,PUT,DELETE /api/properties)"

# --- User properties ---
curl -s -X PUT "$KONG_ADMIN/services/backend-service/routes/properties-user" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/properties/user"],
    "strip_path": false
  }' > /dev/null
echo "  properties-user (/api/properties/user)"

# --- Upload ---
curl -s -X PUT "$KONG_ADMIN/services/backend-service/routes/upload-routes" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/upload"],
    "strip_path": false
  }' > /dev/null
echo "  upload-routes (/api/upload)"

# --- Admin ---
curl -s -X PUT "$KONG_ADMIN/services/backend-service/routes/admin-routes" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/admin"],
    "strip_path": false
  }' > /dev/null
echo "  admin-routes (/api/admin)"

# --- Health ---
curl -s -X PUT "$KONG_ADMIN/services/backend-service/routes/health-backend" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/actuator/health"],
    "methods": ["GET"],
    "strip_path": false
  }' > /dev/null
echo "  health-backend (GET /actuator/health)"

# ============================================================================
# POST SERVICE ROUTES
# ============================================================================
echo ""
echo "[3/3] Creating post-service routes..."

# --- Posts public GET ---
curl -s -X PUT "$KONG_ADMIN/services/post-service/routes/posts-public-get" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/posts"],
    "methods": ["GET"],
    "strip_path": false
  }' > /dev/null
echo "  posts-public-get (GET /api/posts)"

# --- Posts search ---
curl -s -X PUT "$KONG_ADMIN/services/post-service/routes/posts-search" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/posts/search"],
    "methods": ["GET"],
    "strip_path": false
  }' > /dev/null
echo "  posts-search (GET /api/posts/search)"

# --- Posts by user ---
curl -s -X PUT "$KONG_ADMIN/services/post-service/routes/posts-user" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/posts/user"],
    "methods": ["GET"],
    "strip_path": false
  }' > /dev/null
echo "  posts-user (GET /api/posts/user)"

# --- Posts protected (create, update, delete) ---
curl -s -X PUT "$KONG_ADMIN/services/post-service/routes/posts-protected" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/posts"],
    "methods": ["POST", "PUT", "DELETE"],
    "strip_path": false
  }' > /dev/null
echo "  posts-protected (POST,PUT,DELETE /api/posts)"

# --- Posts like toggle ---
curl -s -X PUT "$KONG_ADMIN/services/post-service/routes/posts-like" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["~/api/posts/[^/]+/like$"],
    "methods": ["POST"],
    "strip_path": false
  }' > /dev/null
echo "  posts-like (POST /api/posts/{id}/like)"

# --- My posts ---
curl -s -X PUT "$KONG_ADMIN/services/post-service/routes/posts-me" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/posts/me"],
    "strip_path": false
  }' > /dev/null
echo "  posts-me (/api/posts/me)"

# --- Post service health ---
curl -s -X PUT "$KONG_ADMIN/services/post-service/routes/health-post-service" \
  -H "Content-Type: application/json" \
  -d '{
    "paths": ["/api/posts/actuator/health"],
    "methods": ["GET"],
    "strip_path": false
  }' > /dev/null
echo "  health-post-service (GET /api/posts/actuator/health)"

echo ""
echo "=== Done! Routes configured ==="
echo ""
echo "Verify with: curl $KONG_ADMIN/routes | python3 -m json.tool"
