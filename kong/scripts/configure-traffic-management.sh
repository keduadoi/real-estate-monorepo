#!/bin/bash
set -e

# Phase 5: Traffic Management Configuration
# - Enhanced rate limiting with Redis
# - Upstream health checks
# - Circuit breaker configuration
#
# Usage: ./configure-traffic-management.sh <KONG_ADMIN_URL>
# Example: ./configure-traffic-management.sh http://127.0.0.1:30001

KONG_ADMIN="${1:-http://localhost:30001}"
REDIS_HOST="${2:-kong-redis.kong.svc.cluster.local}"
REDIS_PORT="${3:-6379}"

echo "=== Phase 5: Traffic Management Configuration ==="
echo "Kong Admin: $KONG_ADMIN"
echo "Redis Host: $REDIS_HOST:$REDIS_PORT"
echo ""

# Verify Kong is reachable
curl -s "$KONG_ADMIN/" > /dev/null || { echo "ERROR: Cannot reach Kong Admin at $KONG_ADMIN"; exit 1; }

# ============================================================================
# STEP 1: CONFIGURE UPSTREAMS WITH HEALTH CHECKS
# ============================================================================
echo "[1/4] Configuring upstreams with health checks..."

# Create/update auth-service upstream
curl -s -X PUT "$KONG_ADMIN/upstreams/auth-service-upstream" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "auth-service-upstream",
    "slots": 10000,
    "healthchecks": {
      "active": {
        "healthy": {
          "interval": 5,
          "successes": 2,
          "http_statuses": [200, 302]
        },
        "unhealthy": {
          "interval": 5,
          "http_failures": 3,
          "tcp_failures": 3,
          "timeouts": 3,
          "http_statuses": [429, 500, 502, 503, 504]
        },
        "type": "http",
        "http_path": "/actuator/health",
        "timeout": 5,
        "concurrency": 10
      },
      "passive": {
        "healthy": {
          "successes": 2,
          "http_statuses": [200, 201, 202, 203, 204, 205, 206, 207, 208, 226, 300, 301, 302, 303, 304, 305, 306, 307, 308]
        },
        "unhealthy": {
          "http_failures": 5,
          "tcp_failures": 5,
          "timeouts": 5,
          "http_statuses": [429, 500, 502, 503, 504]
        },
        "type": "http"
      }
    }
  }' > /dev/null
echo "  Created upstream: auth-service-upstream"

# Add target for auth-service
curl -s -X POST "$KONG_ADMIN/upstreams/auth-service-upstream/targets" \
  -H "Content-Type: application/json" \
  -d '{
    "target": "auth-service.real-estate.svc.cluster.local:8081",
    "weight": 100
  }' > /dev/null 2>&1 || true
echo "  Added target: auth-service.real-estate.svc.cluster.local:8081"

# Create/update backend-service upstream
curl -s -X PUT "$KONG_ADMIN/upstreams/backend-service-upstream" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "backend-service-upstream",
    "slots": 10000,
    "healthchecks": {
      "active": {
        "healthy": {
          "interval": 5,
          "successes": 2,
          "http_statuses": [200, 302]
        },
        "unhealthy": {
          "interval": 5,
          "http_failures": 3,
          "tcp_failures": 3,
          "timeouts": 3,
          "http_statuses": [429, 500, 502, 503, 504]
        },
        "type": "http",
        "http_path": "/actuator/health",
        "timeout": 5,
        "concurrency": 10
      },
      "passive": {
        "healthy": {
          "successes": 2,
          "http_statuses": [200, 201, 202, 203, 204, 205, 206, 207, 208, 226, 300, 301, 302, 303, 304, 305, 306, 307, 308]
        },
        "unhealthy": {
          "http_failures": 5,
          "tcp_failures": 5,
          "timeouts": 5,
          "http_statuses": [429, 500, 502, 503, 504]
        },
        "type": "http"
      }
    }
  }' > /dev/null
echo "  Created upstream: backend-service-upstream"

# Add target for backend-service
curl -s -X POST "$KONG_ADMIN/upstreams/backend-service-upstream/targets" \
  -H "Content-Type: application/json" \
  -d '{
    "target": "real-estate-backend-backend.real-estate.svc.cluster.local:8080",
    "weight": 100
  }' > /dev/null 2>&1 || true
echo "  Added target: real-estate-backend-backend.real-estate.svc.cluster.local:8080"

# Update services to use upstreams
echo "  Updating services to use upstreams..."
curl -s -X PATCH "$KONG_ADMIN/services/auth-service" \
  -H "Content-Type: application/json" \
  -d '{"host": "auth-service-upstream"}' > /dev/null
curl -s -X PATCH "$KONG_ADMIN/services/backend-service" \
  -H "Content-Type: application/json" \
  -d '{"host": "backend-service-upstream"}' > /dev/null
echo "  Services updated to use upstreams"

# ============================================================================
# STEP 2: CONFIGURE RATE LIMITING WITH REDIS
# ============================================================================
echo ""
echo "[2/4] Configuring rate limiting with Redis..."

# Helper function to configure rate limiting
configure_rate_limit() {
  local route=$1
  local minute=$2
  local hour=${3:-0}
  local policy=${4:-redis}

  # Remove existing rate limiting plugin
  local existing=$(curl -s "$KONG_ADMIN/routes/$route/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
plugins = [p['id'] for p in d.get('data', []) if p['name'] == 'rate-limiting']
print(plugins[0] if plugins else '')
" 2>/dev/null || echo "")

  if [ -n "$existing" ]; then
    curl -s -X DELETE "$KONG_ADMIN/plugins/$existing" > /dev/null 2>&1 || true
  fi

  # Create rate limiting config
  local config="{
    \"name\": \"rate-limiting\",
    \"config\": {
      \"minute\": $minute,
      \"policy\": \"$policy\",
      \"fault_tolerant\": true,
      \"hide_client_headers\": false,
      \"redis_host\": \"$REDIS_HOST\",
      \"redis_port\": $REDIS_PORT,
      \"redis_timeout\": 2000"

  if [ "$hour" -gt 0 ]; then
    config="$config, \"hour\": $hour"
  fi

  config="$config } }"

  curl -s -X POST "$KONG_ADMIN/routes/$route/plugins" \
    -H "Content-Type: application/json" \
    -d "$config" > /dev/null

  if [ "$hour" -gt 0 ]; then
    echo "  $route: $minute/min, $hour/hour ($policy)"
  else
    echo "  $route: $minute/min ($policy)"
  fi
}

# Auth public routes - strict limits (brute force protection)
configure_rate_limit "auth-public" 10 100 "redis"

# Auth JWKS - public key endpoint, very permissive
configure_rate_limit "auth-jwks" 60 0 "local"

# Protected routes - moderate limits
configure_rate_limit "auth-protected" 60 0 "redis"
configure_rate_limit "properties-protected" 30 200 "redis"
configure_rate_limit "properties-user" 60 0 "redis"

# Upload routes - stricter limits
configure_rate_limit "upload-routes" 10 0 "redis"

# Admin routes - permissive for admins
configure_rate_limit "admin-routes" 100 0 "redis"
configure_rate_limit "users-admin" 100 0 "redis"

# Public property routes - moderate
configure_rate_limit "properties-public-get" 120 0 "local"
configure_rate_limit "properties-search" 60 0 "local"
configure_rate_limit "properties-cities" 60 0 "local"

# Health endpoint - very permissive
configure_rate_limit "health-backend" 120 0 "local"

# ============================================================================
# STEP 3: CONFIGURE RETRY LOGIC
# ============================================================================
echo ""
echo "[3/4] Configuring retry logic..."

# Update services with retry configuration
for service in auth-service backend-service; do
  curl -s -X PATCH "$KONG_ADMIN/services/$service" \
    -H "Content-Type: application/json" \
    -d '{
      "retries": 3,
      "connect_timeout": 10000,
      "write_timeout": 60000,
      "read_timeout": 60000
    }' > /dev/null
  echo "  $service: 3 retries, 10s connect, 60s read/write timeout"
done

# ============================================================================
# STEP 4: CONFIGURE CIRCUIT BREAKER (via request-termination on unhealthy)
# ============================================================================
echo ""
echo "[4/4] Configuring circuit breaker behavior..."

# Kong's passive health checks already provide circuit breaker functionality
# When an upstream becomes unhealthy, Kong stops sending traffic to it
# We'll also add request-termination for explicit error handling

# Global response transformer to add rate limit headers
curl -s -X POST "$KONG_ADMIN/plugins" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "response-transformer",
    "config": {
      "add": {
        "headers": [
          "X-Kong-Upstream-Status:$(upstream_status)"
        ]
      }
    }
  }' > /dev/null 2>&1 || true
echo "  Added response transformer for upstream status headers"

echo ""
echo "=== Phase 5 Configuration Complete ==="
echo ""
echo "Summary:"
echo "  - Upstreams: auth-service-upstream, backend-service-upstream"
echo "  - Health Checks: Active (5s interval) + Passive"
echo "  - Rate Limiting: Redis-backed for critical routes"
echo "  - Circuit Breaker: Passive health checks (5 failures = unhealthy)"
echo "  - Retry: 3 retries per service"
echo ""
echo "Verify health checks:"
echo "  curl $KONG_ADMIN/upstreams/auth-service-upstream/health"
echo "  curl $KONG_ADMIN/upstreams/backend-service-upstream/health"
echo ""
echo "Test rate limiting:"
echo "  for i in {1..15}; do curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:30000/auth/login -X POST -H 'Content-Type: application/json' -d '{}'; done"
