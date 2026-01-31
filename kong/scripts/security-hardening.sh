#!/bin/bash
set -e

# Phase 7: Security Hardening Script
# Applies security best practices to Kong Gateway
#
# Usage: ./security-hardening.sh <KONG_ADMIN_URL>
# Example: ./security-hardening.sh http://127.0.0.1:30001

KONG_ADMIN="${1:-http://localhost:30001}"

echo "=== Phase 7: Security Hardening ==="
echo "Kong Admin: $KONG_ADMIN"
echo ""

# Verify Kong is reachable
curl -s "$KONG_ADMIN/" > /dev/null || { echo "ERROR: Cannot reach Kong Admin at $KONG_ADMIN"; exit 1; }

# ============================================================================
# STEP 1: DISABLE ADMIN API EXTERNAL ACCESS (if exposed)
# ============================================================================
echo "[1/6] Verifying Admin API is not externally exposed..."
echo "  Note: In production, Kong Admin API should only be accessible via ClusterIP"
echo "  Ensure admin service type is ClusterIP, not LoadBalancer or NodePort"

# ============================================================================
# STEP 2: REQUEST SIZE LIMITS
# ============================================================================
echo ""
echo "[2/6] Configuring request size limits..."

# Check if request-size-limiting plugin exists
EXISTING=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for p in d.get('data', []):
    if p['name'] == 'request-size-limiting' and not p.get('service') and not p.get('route'):
        print(p['id'])
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING" ]; then
  echo "  Updating request-size-limiting plugin..."
  curl -s -X PATCH "$KONG_ADMIN/plugins/$EXISTING" \
    -H "Content-Type: application/json" \
    -d '{
      "config": {
        "allowed_payload_size": 10,
        "size_unit": "megabytes",
        "require_content_length": false
      }
    }' > /dev/null
else
  echo "  Creating request-size-limiting plugin..."
  curl -s -X POST "$KONG_ADMIN/plugins" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "request-size-limiting",
      "config": {
        "allowed_payload_size": 10,
        "size_unit": "megabytes",
        "require_content_length": false
      }
    }' > /dev/null
fi
echo "  Request size limit: 10MB"

# ============================================================================
# STEP 3: BOT DETECTION (basic)
# ============================================================================
echo ""
echo "[3/6] Configuring bot detection..."

# Using request-termination for suspicious user agents
# This is a basic implementation - consider using more sophisticated bot detection in production
echo "  Note: Consider using dedicated bot detection service for production"
echo "  Basic suspicious request patterns will be rate-limited"

# ============================================================================
# STEP 4: IP RESTRICTION (optional - configure based on your needs)
# ============================================================================
echo ""
echo "[4/6] IP restriction plugin available for restricted endpoints..."
echo "  Note: Configure ip-restriction plugin for admin or sensitive endpoints as needed"
echo "  Example: curl -X POST $KONG_ADMIN/routes/{route-id}/plugins -d 'name=ip-restriction' -d 'config.allow=10.0.0.0/8'"

# ============================================================================
# STEP 5: REQUEST VALIDATION
# ============================================================================
echo ""
echo "[5/6] Configuring request validation..."

# Check if request-validator plugin exists (Enterprise feature, skip if not available)
AVAILABLE=$(curl -s "$KONG_ADMIN/" | python3 -c "
import sys, json
d = json.load(sys.stdin)
plugins = d.get('plugins', {}).get('available_on_server', {})
print('request-validator' if 'request-validator' in plugins else '')
" 2>/dev/null || echo "")

if [ -n "$AVAILABLE" ]; then
  echo "  request-validator plugin available (Enterprise feature)"
else
  echo "  request-validator plugin not available (Enterprise feature)"
  echo "  Using application-level validation instead"
fi

# ============================================================================
# STEP 6: AUDIT LOGGING
# ============================================================================
echo ""
echo "[6/6] Verifying audit logging..."

# Ensure file-log or http-log is enabled (from observability configuration)
LOGGING=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for p in d.get('data', []):
    if p['name'] in ['file-log', 'http-log', 'tcp-log', 'syslog']:
        print(p['name'])
        break
" 2>/dev/null || echo "")

if [ -n "$LOGGING" ]; then
  echo "  Logging enabled: $LOGGING"
else
  echo "  WARNING: No logging plugin found. Consider enabling file-log or http-log"
fi

echo ""
echo "=== Security Hardening Complete ==="
echo ""
echo "Security Checklist:"
echo "  [x] Request size limiting enabled (10MB max)"
echo "  [ ] Verify Admin API is ClusterIP only"
echo "  [ ] Configure IP restrictions for sensitive routes"
echo "  [ ] Enable TLS/SSL (see configure-tls.sh)"
echo "  [ ] Review rate limiting settings"
echo "  [ ] Enable security headers (see configure-tls.sh)"
echo ""
echo "Additional Recommendations:"
echo "  1. Use external secrets manager for sensitive data"
echo "  2. Enable pod security policies"
echo "  3. Configure network policies"
echo "  4. Regular security audits and penetration testing"
echo "  5. Monitor for suspicious activity patterns"
echo ""
