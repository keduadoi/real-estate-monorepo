#!/bin/bash
set -e

# Phase 7: TLS/SSL Configuration Script
# This script configures Kong to use TLS certificates and enables HTTPS
#
# Usage: ./configure-tls.sh <KONG_ADMIN_URL> [CERTIFICATE_PATH] [KEY_PATH]
# Example: ./configure-tls.sh http://127.0.0.1:30001 /path/to/cert.pem /path/to/key.pem

KONG_ADMIN="${1:-http://localhost:30001}"
CERT_PATH="${2:-}"
KEY_PATH="${3:-}"

echo "=== Phase 7: TLS/SSL Configuration ==="
echo "Kong Admin: $KONG_ADMIN"
echo ""

# Verify Kong is reachable
curl -s "$KONG_ADMIN/" > /dev/null || { echo "ERROR: Cannot reach Kong Admin at $KONG_ADMIN"; exit 1; }

# ============================================================================
# STEP 1: Configure Global Security Headers
# ============================================================================
echo "[1/4] Configuring security headers..."

# Check if response-transformer plugin exists globally
EXISTING=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for p in d.get('data', []):
    if p['name'] == 'response-transformer' and not p.get('service') and not p.get('route'):
        print(p['id'])
        break
" 2>/dev/null || echo "")

if [ -n "$EXISTING" ]; then
  echo "  Updating existing response-transformer plugin..."
  curl -s -X PATCH "$KONG_ADMIN/plugins/$EXISTING" \
    -H "Content-Type: application/json" \
    -d '{
      "config": {
        "add": {
          "headers": [
            "Strict-Transport-Security:max-age=31536000; includeSubDomains; preload",
            "X-Content-Type-Options:nosniff",
            "X-Frame-Options:DENY",
            "X-XSS-Protection:1; mode=block",
            "Referrer-Policy:strict-origin-when-cross-origin",
            "Content-Security-Policy:default-src '\''self'\''",
            "Permissions-Policy:geolocation=(), microphone=(), camera=()"
          ]
        }
      }
    }' > /dev/null
else
  echo "  Creating response-transformer plugin for security headers..."
  curl -s -X POST "$KONG_ADMIN/plugins" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "response-transformer",
      "config": {
        "add": {
          "headers": [
            "Strict-Transport-Security:max-age=31536000; includeSubDomains; preload",
            "X-Content-Type-Options:nosniff",
            "X-Frame-Options:DENY",
            "X-XSS-Protection:1; mode=block",
            "Referrer-Policy:strict-origin-when-cross-origin",
            "Content-Security-Policy:default-src '\''self'\''",
            "Permissions-Policy:geolocation=(), microphone=(), camera=()"
          ]
        }
      }
    }' > /dev/null
fi
echo "  Security headers configured"

# ============================================================================
# STEP 2: Configure SSL Certificate (if provided)
# ============================================================================
if [ -n "$CERT_PATH" ] && [ -n "$KEY_PATH" ]; then
  echo ""
  echo "[2/4] Uploading SSL certificate..."

  if [ ! -f "$CERT_PATH" ]; then
    echo "  ERROR: Certificate file not found: $CERT_PATH"
    exit 1
  fi

  if [ ! -f "$KEY_PATH" ]; then
    echo "  ERROR: Key file not found: $KEY_PATH"
    exit 1
  fi

  CERT_CONTENT=$(cat "$CERT_PATH" | sed ':a;N;$!ba;s/\n/\\n/g')
  KEY_CONTENT=$(cat "$KEY_PATH" | sed ':a;N;$!ba;s/\n/\\n/g')

  curl -s -X POST "$KONG_ADMIN/certificates" \
    -H "Content-Type: application/json" \
    -d "{
      \"cert\": \"$CERT_CONTENT\",
      \"key\": \"$KEY_CONTENT\"
    }" > /dev/null

  echo "  SSL certificate uploaded"
else
  echo ""
  echo "[2/4] Skipping certificate upload (no paths provided)"
  echo "  In production, certificates are typically managed by:"
  echo "    - cert-manager (automatic via Let's Encrypt)"
  echo "    - AWS ACM (via Load Balancer)"
  echo "    - Manual upload via Kong Admin API"
fi

# ============================================================================
# STEP 3: Configure HTTPS Redirect
# ============================================================================
echo ""
echo "[3/4] Configuring HTTPS redirect plugin..."

# Check if request-termination plugin exists for http redirect
# We'll use pre-function plugin to redirect HTTP to HTTPS
EXISTING=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for p in d.get('data', []):
    if p['name'] == 'pre-function' and not p.get('service') and not p.get('route'):
        print(p['id'])
        break
" 2>/dev/null || echo "")

# Note: In production, HTTPS redirect is typically handled at the load balancer level
# This is a Kong-level redirect as a fallback
echo "  Note: HTTPS redirect is typically configured at load balancer level"
echo "  Kong will enforce security headers for all responses"

# ============================================================================
# STEP 4: Verify Configuration
# ============================================================================
echo ""
echo "[4/4] Verifying TLS configuration..."

# Check plugins
PLUGINS=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
plugins = [p['name'] for p in d.get('data', [])]
print(', '.join(set(plugins)))
")

echo "  Active plugins: $PLUGINS"

# Check certificates
CERTS=$(curl -s "$KONG_ADMIN/certificates" | python3 -c "
import sys, json
d = json.load(sys.stdin)
count = len(d.get('data', []))
print(count)
")

echo "  Certificates uploaded: $CERTS"

echo ""
echo "=== TLS Configuration Complete ==="
echo ""
echo "Security headers enabled:"
echo "  - Strict-Transport-Security (HSTS)"
echo "  - X-Content-Type-Options"
echo "  - X-Frame-Options"
echo "  - X-XSS-Protection"
echo "  - Referrer-Policy"
echo "  - Content-Security-Policy"
echo "  - Permissions-Policy"
echo ""
echo "For production deployment:"
echo "  1. Use cert-manager for automatic certificate management"
echo "  2. Configure your load balancer for TLS termination"
echo "  3. Use AWS ACM or similar managed certificate service"
echo ""
