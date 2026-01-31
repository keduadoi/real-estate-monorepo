#!/bin/bash
set -e

# Phase 6: Observability Configuration
# - Prometheus metrics plugin
# - HTTP logging plugin
# - Request ID correlation
#
# Usage: ./configure-observability.sh <KONG_ADMIN_URL>
# Example: ./configure-observability.sh http://127.0.0.1:30001

KONG_ADMIN="${1:-http://localhost:30001}"

echo "=== Phase 6: Observability Configuration ==="
echo "Kong Admin: $KONG_ADMIN"
echo ""

# Verify Kong is reachable
curl -s "$KONG_ADMIN/" > /dev/null || { echo "ERROR: Cannot reach Kong Admin at $KONG_ADMIN"; exit 1; }

# ============================================================================
# STEP 1: PROMETHEUS METRICS PLUGIN
# ============================================================================
echo "[1/3] Configuring Prometheus metrics plugin..."

# Check if prometheus plugin exists globally
EXISTING=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for p in d.get('data', []):
    if p['name'] == 'prometheus' and not p.get('service') and not p.get('route'):
        print(p['id'])
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING" ]; then
  echo "  Updating existing Prometheus plugin..."
  curl -s -X PATCH "$KONG_ADMIN/plugins/$EXISTING" \
    -H "Content-Type: application/json" \
    -d '{
      "config": {
        "per_consumer": true,
        "status_code_metrics": true,
        "latency_metrics": true,
        "bandwidth_metrics": true,
        "upstream_health_metrics": true
      }
    }' > /dev/null
else
  echo "  Creating Prometheus plugin..."
  curl -s -X POST "$KONG_ADMIN/plugins" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "prometheus",
      "config": {
        "per_consumer": true,
        "status_code_metrics": true,
        "latency_metrics": true,
        "bandwidth_metrics": true,
        "upstream_health_metrics": true
      }
    }' > /dev/null
fi
echo "  Prometheus metrics enabled at /metrics"

# ============================================================================
# STEP 2: HTTP LOGGING PLUGIN
# ============================================================================
echo ""
echo "[2/3] Configuring HTTP logging plugin..."

# For local development, we'll use file-log instead of http-log
# In production, you would configure http-log to send to a log aggregator

# Check if file-log plugin exists globally
EXISTING=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for p in d.get('data', []):
    if p['name'] == 'file-log' and not p.get('service') and not p.get('route'):
        print(p['id'])
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING" ]; then
  echo "  File-log plugin already exists"
else
  curl -s -X POST "$KONG_ADMIN/plugins" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "file-log",
      "config": {
        "path": "/dev/stdout",
        "reopen": false
      }
    }' > /dev/null
  echo "  File-log plugin enabled (logs to stdout)"
fi

# ============================================================================
# STEP 3: REQUEST TERMINATION FOR ERRORS
# ============================================================================
echo ""
echo "[3/3] Verifying correlation-id plugin..."

EXISTING=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for p in d.get('data', []):
    if p['name'] == 'correlation-id' and not p.get('service') and not p.get('route'):
        print(p['id'])
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING" ]; then
  echo "  Correlation-id plugin already configured"
else
  curl -s -X POST "$KONG_ADMIN/plugins" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "correlation-id",
      "config": {
        "header_name": "X-Request-ID",
        "generator": "uuid",
        "echo_downstream": true
      }
    }' > /dev/null
  echo "  Correlation-id plugin enabled"
fi

echo ""
echo "=== Observability Configuration Complete ==="
echo ""
echo "Prometheus metrics available at:"
echo "  curl $KONG_ADMIN/metrics"
echo ""
echo "Request logs available via:"
echo "  kubectl logs -f deployment/kong -n kong"
echo ""
echo "Each request will have X-Request-ID header for correlation"
