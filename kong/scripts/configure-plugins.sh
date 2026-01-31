#!/bin/bash
set -e

# Configure Kong plugins for Phase 3: Kong-Auth Integration
# Usage: ./configure-plugins.sh <KONG_ADMIN_URL>
# Example: ./configure-plugins.sh http://127.0.0.1:30001

KONG_ADMIN="${1:-http://localhost:30001}"
AUTH_SERVICE_JWKS="http://auth-service.real-estate.svc.cluster.local:8081/.well-known/jwks.json"

echo "=== Phase 3: Kong-Auth Integration ==="
echo "Admin URL: $KONG_ADMIN"
echo "JWKS URL: $AUTH_SERVICE_JWKS"
echo ""

# Verify Kong is reachable
curl -s "$KONG_ADMIN/" > /dev/null || { echo "ERROR: Cannot reach Kong Admin at $KONG_ADMIN"; exit 1; }

# ============================================================================
# STEP 1: GLOBAL PLUGINS
# ============================================================================
echo "[1/6] Configuring global plugins..."

# --- CORS Plugin (Global) ---
echo "  Configuring CORS plugin..."
curl -s -X POST "$KONG_ADMIN/plugins" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "cors",
    "config": {
      "origins": ["*"],
      "methods": ["GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"],
      "headers": ["Accept", "Accept-Version", "Authorization", "Content-Type", "X-Requested-With", "X-Request-ID"],
      "exposed_headers": ["X-RateLimit-Remaining", "X-RateLimit-Limit", "X-Request-ID"],
      "credentials": true,
      "max_age": 3600
    }
  }' > /dev/null 2>&1 || echo "    (CORS may already exist)"
echo "    CORS: enabled globally"

# --- Request ID Plugin (Global) ---
echo "  Configuring correlation-id plugin..."
curl -s -X POST "$KONG_ADMIN/plugins" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "correlation-id",
    "config": {
      "header_name": "X-Request-ID",
      "generator": "uuid",
      "echo_downstream": true
    }
  }' > /dev/null 2>&1 || echo "    (correlation-id may already exist)"
echo "    Correlation-ID: enabled globally"

# ============================================================================
# STEP 2: CREATE JWT CONSUMER AND CREDENTIALS
# ============================================================================
echo ""
echo "[2/6] Creating JWT consumer..."

# Create a consumer for JWT validation
curl -s -X PUT "$KONG_ADMIN/consumers/jwt-auth" \
  -H "Content-Type: application/json" \
  -d '{"username": "jwt-auth"}' > /dev/null
echo "  Consumer 'jwt-auth' created"

# Fetch the current public key from auth-service JWKS
echo "  Fetching RSA public key from auth-service..."

# Try to get the JWKS from auth-service (via Kong proxy first, then direct)
JWKS_RESPONSE=$(curl -s "$KONG_ADMIN/../.well-known/jwks.json" 2>/dev/null || \
                curl -s "http://127.0.0.1:30000/.well-known/jwks.json" 2>/dev/null || \
                echo '{"keys":[]}')

# Extract the key ID and create JWT credential
KID=$(echo "$JWKS_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['keys'][0]['kid'] if d.get('keys') else '')" 2>/dev/null || echo "")

if [ -z "$KID" ]; then
  echo "  WARNING: Could not fetch JWKS. JWT credentials will need manual configuration."
  echo "  You may need to run this script again after auth-service is accessible through Kong."
else
  echo "  Found key ID: $KID"

  # Extract the RSA public key components and create PEM format
  # For Kong JWT plugin, we need the RSA public key in PEM format
  RSA_N=$(echo "$JWKS_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['keys'][0]['n'])" 2>/dev/null)
  RSA_E=$(echo "$JWKS_RESPONSE" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d['keys'][0]['e'])" 2>/dev/null)

  # Create JWT credential with the key
  # Note: Kong's jwt plugin needs the public key in PEM format for RS256
  # We'll use a workaround by setting up the credential and letting Kong fetch from JWKS

  # For Kong 3.4+, we can use the key directly with algorithm RS256
  # First, convert JWK to PEM using Python
  RSA_PEM=$(python3 << EOF
import json
import base64
import sys

def b64url_decode(data):
    padding = 4 - len(data) % 4
    if padding != 4:
        data += '=' * padding
    return base64.urlsafe_b64decode(data)

def int_from_bytes(b):
    return int.from_bytes(b, 'big')

jwks = json.loads('''$JWKS_RESPONSE''')
if not jwks.get('keys'):
    sys.exit(1)

key = jwks['keys'][0]
n = b64url_decode(key['n'])
e = b64url_decode(key['e'])

# Create RSA public key in DER format
def encode_length(length):
    if length < 128:
        return bytes([length])
    elif length < 256:
        return bytes([0x81, length])
    else:
        return bytes([0x82, (length >> 8) & 0xff, length & 0xff])

def encode_integer(value):
    if value[0] & 0x80:
        value = b'\x00' + value
    return b'\x02' + encode_length(len(value)) + value

n_encoded = encode_integer(n)
e_encoded = encode_integer(e)
seq_content = n_encoded + e_encoded
seq = b'\x30' + encode_length(len(seq_content)) + seq_content

# RSA public key OID
oid = bytes([0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00])

# Bit string wrapper
bit_string = b'\x03' + encode_length(len(seq) + 1) + b'\x00' + seq

# Final sequence
final_seq = oid + bit_string
der = b'\x30' + encode_length(len(final_seq)) + final_seq

# Base64 encode for PEM
import textwrap
b64 = base64.b64encode(der).decode('ascii')
pem_lines = textwrap.wrap(b64, 64)
pem = "-----BEGIN PUBLIC KEY-----\n" + "\n".join(pem_lines) + "\n-----END PUBLIC KEY-----"
print(pem)
EOF
)

  if [ -n "$RSA_PEM" ]; then
    echo "  Creating JWT credential with RS256..."

    # Delete existing credential if any
    curl -s -X DELETE "$KONG_ADMIN/consumers/jwt-auth/jwt/$KID" > /dev/null 2>&1 || true

    # Create the JWT credential
    curl -s -X POST "$KONG_ADMIN/consumers/jwt-auth/jwt" \
      -H "Content-Type: application/json" \
      -d "{
        \"key\": \"auth-service\",
        \"algorithm\": \"RS256\",
        \"rsa_public_key\": $(echo "$RSA_PEM" | python3 -c 'import sys,json; print(json.dumps(sys.stdin.read()))')
      }" > /dev/null
    echo "  JWT credential created with issuer 'auth-service'"
  fi
fi

# ============================================================================
# STEP 3: CREATE ACL GROUPS
# ============================================================================
echo ""
echo "[3/6] Configuring ACL groups..."

# Create admin consumer with ACL group
curl -s -X PUT "$KONG_ADMIN/consumers/admin-user" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin-user"}' > /dev/null

curl -s -X POST "$KONG_ADMIN/consumers/admin-user/acls" \
  -H "Content-Type: application/json" \
  -d '{"group": "admin"}' > /dev/null 2>&1 || true
echo "  ACL group 'admin' configured"

# ============================================================================
# STEP 4: JWT PLUGIN ON PROTECTED ROUTES
# ============================================================================
echo ""
echo "[4/6] Configuring JWT plugin on protected routes..."

PROTECTED_ROUTES="auth-protected users-admin properties-protected properties-user upload-routes admin-routes"

for ROUTE in $PROTECTED_ROUTES; do
  # Check if route exists
  ROUTE_EXISTS=$(curl -s "$KONG_ADMIN/routes/$ROUTE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if d.get('id') else 'no')" 2>/dev/null || echo "no")

  if [ "$ROUTE_EXISTS" = "yes" ]; then
    # Delete existing JWT plugin on this route
    EXISTING_PLUGIN=$(curl -s "$KONG_ADMIN/routes/$ROUTE/plugins" | python3 -c "import sys,json; d=json.load(sys.stdin); plugins=[p['id'] for p in d.get('data',[]) if p['name']=='jwt']; print(plugins[0] if plugins else '')" 2>/dev/null || echo "")

    if [ -n "$EXISTING_PLUGIN" ]; then
      curl -s -X DELETE "$KONG_ADMIN/plugins/$EXISTING_PLUGIN" > /dev/null 2>&1 || true
    fi

    # Add JWT plugin to route
    curl -s -X POST "$KONG_ADMIN/routes/$ROUTE/plugins" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "jwt",
        "config": {
          "key_claim_name": "iss",
          "claims_to_verify": ["exp"],
          "run_on_preflight": false
        }
      }' > /dev/null
    echo "  JWT plugin enabled on: $ROUTE"
  else
    echo "  Route not found: $ROUTE (skipping)"
  fi
done

# ============================================================================
# STEP 5: ACL PLUGIN ON ADMIN ROUTES
# ============================================================================
echo ""
echo "[5/6] Configuring ACL plugin on admin routes..."

ADMIN_ROUTES="users-admin admin-routes"

for ROUTE in $ADMIN_ROUTES; do
  ROUTE_EXISTS=$(curl -s "$KONG_ADMIN/routes/$ROUTE" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if d.get('id') else 'no')" 2>/dev/null || echo "no")

  if [ "$ROUTE_EXISTS" = "yes" ]; then
    # Delete existing ACL plugin on this route
    EXISTING_PLUGIN=$(curl -s "$KONG_ADMIN/routes/$ROUTE/plugins" | python3 -c "import sys,json; d=json.load(sys.stdin); plugins=[p['id'] for p in d.get('data',[]) if p['name']=='acl']; print(plugins[0] if plugins else '')" 2>/dev/null || echo "")

    if [ -n "$EXISTING_PLUGIN" ]; then
      curl -s -X DELETE "$KONG_ADMIN/plugins/$EXISTING_PLUGIN" > /dev/null 2>&1 || true
    fi

    # Add ACL plugin to route (requires admin group)
    curl -s -X POST "$KONG_ADMIN/routes/$ROUTE/plugins" \
      -H "Content-Type: application/json" \
      -d '{
        "name": "acl",
        "config": {
          "allow": ["admin"],
          "hide_groups_header": false
        }
      }' > /dev/null
    echo "  ACL plugin enabled on: $ROUTE (requires 'admin' group)"
  fi
done

# ============================================================================
# STEP 6: RATE LIMITING ON SPECIFIC ROUTES
# ============================================================================
echo ""
echo "[6/6] Configuring rate limiting..."

# Auth public routes - stricter limits
ROUTE_EXISTS=$(curl -s "$KONG_ADMIN/routes/auth-public" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if d.get('id') else 'no')" 2>/dev/null || echo "no")
if [ "$ROUTE_EXISTS" = "yes" ]; then
  curl -s -X POST "$KONG_ADMIN/routes/auth-public/plugins" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "rate-limiting",
      "config": {
        "minute": 10,
        "hour": 100,
        "policy": "local",
        "fault_tolerant": true,
        "hide_client_headers": false
      }
    }' > /dev/null 2>&1 || true
  echo "  Rate limiting on auth-public: 10/min, 100/hour"
fi

# Upload routes - moderate limits
ROUTE_EXISTS=$(curl -s "$KONG_ADMIN/routes/upload-routes" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if d.get('id') else 'no')" 2>/dev/null || echo "no")
if [ "$ROUTE_EXISTS" = "yes" ]; then
  curl -s -X POST "$KONG_ADMIN/routes/upload-routes/plugins" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "rate-limiting",
      "config": {
        "minute": 10,
        "policy": "local",
        "fault_tolerant": true
      }
    }' > /dev/null 2>&1 || true
  echo "  Rate limiting on upload-routes: 10/min"
fi

# Properties protected - moderate limits
ROUTE_EXISTS=$(curl -s "$KONG_ADMIN/routes/properties-protected" | python3 -c "import sys,json; d=json.load(sys.stdin); print('yes' if d.get('id') else 'no')" 2>/dev/null || echo "no")
if [ "$ROUTE_EXISTS" = "yes" ]; then
  curl -s -X POST "$KONG_ADMIN/routes/properties-protected/plugins" \
    -H "Content-Type: application/json" \
    -d '{
      "name": "rate-limiting",
      "config": {
        "minute": 30,
        "hour": 200,
        "policy": "local",
        "fault_tolerant": true
      }
    }' > /dev/null 2>&1 || true
  echo "  Rate limiting on properties-protected: 30/min, 200/hour"
fi

echo ""
echo "=== Phase 3 Configuration Complete ==="
echo ""
echo "Summary:"
echo "  - CORS: Enabled globally"
echo "  - Request ID: Enabled globally"
echo "  - JWT validation: Enabled on protected routes"
echo "  - ACL: Enabled on admin routes (requires 'admin' group)"
echo "  - Rate limiting: Enabled on auth, upload, and properties routes"
echo ""
echo "Verify with:"
echo "  curl $KONG_ADMIN/plugins | python3 -m json.tool"
echo ""
echo "Test JWT protected route (should return 401):"
echo "  curl -i http://127.0.0.1:30000/auth/me"
echo ""
echo "Test with valid token:"
echo "  TOKEN=\$(curl -s -X POST http://127.0.0.1:30000/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"user@test.com\",\"password\":\"password\"}' | jq -r '.accessToken')"
echo "  curl -H \"Authorization: Bearer \$TOKEN\" http://127.0.0.1:30000/auth/me"
