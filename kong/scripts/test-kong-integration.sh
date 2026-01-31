#!/bin/bash
set -e

# Kong Integration Test Script
# Tests Kong Gateway integration with Auth Service and Backend
#
# Usage: ./test-kong-integration.sh [MINIKUBE_IP]
# Example: ./test-kong-integration.sh 192.168.49.2

MINIKUBE_IP="${1:-$(minikube ip 2>/dev/null || echo '192.168.49.2')}"
KONG_PROXY="http://${MINIKUBE_IP}:30000"
KONG_ADMIN="http://${MINIKUBE_IP}:30001"
AUTH_DIRECT="http://${MINIKUBE_IP}:30081"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PASSED=0
FAILED=0
WARNINGS=0

# Test result tracking
declare -a RESULTS

print_header() {
    echo ""
    echo -e "${BLUE}============================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}============================================================${NC}"
}

print_test() {
    echo -e "\n${YELLOW}[TEST]${NC} $1"
}

pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
    PASSED=$((PASSED + 1))
    RESULTS+=("PASS: $1")
}

fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    FAILED=$((FAILED + 1))
    RESULTS+=("FAIL: $1")
}

warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
    WARNINGS=$((WARNINGS + 1))
    RESULTS+=("WARN: $1")
}

info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

# ============================================================================
# SETUP
# ============================================================================
print_header "Kong Integration Test Suite"
echo "Minikube IP: $MINIKUBE_IP"
echo "Kong Proxy: $KONG_PROXY"
echo "Kong Admin: $KONG_ADMIN"
echo "Auth Service (direct): $AUTH_DIRECT"
echo ""

# ============================================================================
# TEST 1: Kong Admin API Connectivity
# ============================================================================
print_header "1. Kong Admin API Connectivity"

print_test "Checking Kong Admin API is reachable..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$KONG_ADMIN/" 2>/dev/null || echo "000")
if [ "$RESPONSE" = "200" ]; then
    pass "Kong Admin API is reachable (HTTP $RESPONSE)"
else
    fail "Kong Admin API is not reachable (HTTP $RESPONSE)"
    echo "Cannot continue without Kong Admin API. Exiting."
    exit 1
fi

print_test "Getting Kong version..."
VERSION=$(curl -s "$KONG_ADMIN/" | python3 -c "import sys,json; print(json.load(sys.stdin).get('version', 'unknown'))" 2>/dev/null || echo "unknown")
info "Kong version: $VERSION"

# ============================================================================
# TEST 2: Kong Proxy Connectivity
# ============================================================================
print_header "2. Kong Proxy Connectivity"

print_test "Checking Kong Proxy is reachable..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$KONG_PROXY/" 2>/dev/null || echo "000")
if [ "$RESPONSE" = "404" ] || [ "$RESPONSE" = "200" ] || [ "$RESPONSE" = "503" ]; then
    pass "Kong Proxy is reachable (HTTP $RESPONSE - expected, no default route)"
else
    fail "Kong Proxy is not reachable (HTTP $RESPONSE)"
fi

# ============================================================================
# TEST 3: Services Configuration
# ============================================================================
print_header "3. Kong Services Configuration"

print_test "Checking configured services..."
SERVICES=$(curl -s "$KONG_ADMIN/services" | python3 -c "
import sys, json
d = json.load(sys.stdin)
services = d.get('data', [])
for s in services:
    print(f\"  - {s['name']}: {s.get('host', 'N/A')}:{s.get('port', 'N/A')}\")
print(len(services))
" 2>/dev/null)

SERVICE_COUNT=$(echo "$SERVICES" | tail -1)
if [ "$SERVICE_COUNT" -gt 0 ] 2>/dev/null; then
    pass "Found $SERVICE_COUNT service(s) configured"
    echo "$SERVICES" | head -n -1
else
    warn "No services configured in Kong"
fi

# ============================================================================
# TEST 4: Routes Configuration
# ============================================================================
print_header "4. Kong Routes Configuration"

print_test "Checking configured routes..."
ROUTES=$(curl -s "$KONG_ADMIN/routes" | python3 -c "
import sys, json
d = json.load(sys.stdin)
routes = d.get('data', [])
for r in routes:
    paths = r.get('paths', [])
    methods = r.get('methods', ['*'])
    print(f\"  - {r['name']}: {paths} [{', '.join(methods or ['*'])}]\")
print(len(routes))
" 2>/dev/null)

ROUTE_COUNT=$(echo "$ROUTES" | tail -1)
if [ "$ROUTE_COUNT" -gt 0 ] 2>/dev/null; then
    pass "Found $ROUTE_COUNT route(s) configured"
    echo "$ROUTES" | head -n -1
else
    warn "No routes configured in Kong"
fi

# ============================================================================
# TEST 5: Plugins Configuration
# ============================================================================
print_header "5. Kong Plugins Configuration"

print_test "Checking enabled plugins..."
PLUGINS=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
plugins = d.get('data', [])
plugin_names = {}
for p in plugins:
    name = p['name']
    scope = 'global' if not p.get('service') and not p.get('route') else 'scoped'
    if name not in plugin_names:
        plugin_names[name] = []
    plugin_names[name].append(scope)
for name, scopes in sorted(plugin_names.items()):
    print(f\"  - {name}: {', '.join(set(scopes))}\")
print(len(plugins))
" 2>/dev/null)

PLUGIN_COUNT=$(echo "$PLUGINS" | tail -1)
if [ "$PLUGIN_COUNT" -gt 0 ] 2>/dev/null; then
    pass "Found $PLUGIN_COUNT plugin instance(s)"
    echo "$PLUGINS" | head -n -1
else
    warn "No plugins configured"
fi

# Check for essential plugins
print_test "Checking essential plugins..."
ESSENTIAL_PLUGINS=("jwt" "rate-limiting" "cors" "prometheus")
for plugin in "${ESSENTIAL_PLUGINS[@]}"; do
    EXISTS=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
for p in d.get('data', []):
    if p['name'] == '$plugin':
        print('yes')
        break
" 2>/dev/null)
    if [ "$EXISTS" = "yes" ]; then
        pass "Plugin '$plugin' is configured"
    else
        warn "Plugin '$plugin' is NOT configured"
    fi
done

# ============================================================================
# TEST 6: Auth Service Integration (Direct)
# ============================================================================
print_header "6. Auth Service Integration (Direct Access)"

print_test "Checking Auth Service health (direct)..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$AUTH_DIRECT/actuator/health" 2>/dev/null || echo "000")
if [ "$RESPONSE" = "200" ]; then
    pass "Auth Service is healthy (HTTP $RESPONSE)"
else
    fail "Auth Service health check failed (HTTP $RESPONSE)"
fi

print_test "Checking JWKS endpoint (direct)..."
JWKS=$(curl -s "$AUTH_DIRECT/.well-known/jwks.json" 2>/dev/null)
KEY_COUNT=$(echo "$JWKS" | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('keys', [])))" 2>/dev/null || echo "0")
if [ "$KEY_COUNT" -gt 0 ]; then
    pass "JWKS endpoint returns $KEY_COUNT key(s)"
else
    fail "JWKS endpoint is not working"
fi

# ============================================================================
# TEST 7: Auth Service via Kong
# ============================================================================
print_header "7. Auth Service via Kong Gateway"

print_test "Checking Auth Service health via Kong..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$KONG_PROXY/auth/actuator/health" 2>/dev/null || echo "000")
if [ "$RESPONSE" = "200" ]; then
    pass "Auth Service via Kong is healthy (HTTP $RESPONSE)"
elif [ "$RESPONSE" = "404" ]; then
    warn "Auth health route not configured in Kong (HTTP $RESPONSE)"
else
    fail "Auth Service via Kong failed (HTTP $RESPONSE)"
fi

print_test "Checking JWKS via Kong..."
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$KONG_PROXY/.well-known/jwks.json" 2>/dev/null || echo "000")
if [ "$RESPONSE" = "200" ]; then
    pass "JWKS via Kong is accessible (HTTP $RESPONSE)"
elif [ "$RESPONSE" = "404" ]; then
    warn "JWKS route not configured in Kong (HTTP $RESPONSE)"
else
    fail "JWKS via Kong failed (HTTP $RESPONSE)"
fi

# ============================================================================
# TEST 8: Authentication Flow
# ============================================================================
print_header "8. Authentication Flow Tests"

# Test user registration (may fail if user exists)
print_test "Testing user registration..."
REGISTER_RESPONSE=$(curl -s -X POST "$KONG_PROXY/auth/register" \
    -H "Content-Type: application/json" \
    -d '{
        "email": "test-kong@example.com",
        "password": "TestPassword123!",
        "firstName": "Kong",
        "lastName": "Test"
    }' 2>/dev/null)

REGISTER_STATUS=$(echo "$REGISTER_RESPONSE" | python3 -c "
import sys,json
try:
    d = json.load(sys.stdin)
    if 'accessToken' in d:
        print('SUCCESS')
    elif 'error' in d or 'message' in d:
        print('EXISTS')
    else:
        print('UNKNOWN')
except:
    print('ERROR')
" 2>/dev/null)

if [ "$REGISTER_STATUS" = "SUCCESS" ]; then
    pass "User registration successful"
elif [ "$REGISTER_STATUS" = "EXISTS" ]; then
    info "User already exists (expected for repeat tests)"
else
    warn "User registration returned unexpected response"
fi

# Test user login
print_test "Testing user login..."
LOGIN_RESPONSE=$(curl -s -X POST "$KONG_PROXY/auth/login" \
    -H "Content-Type: application/json" \
    -d '{
        "email": "test-kong@example.com",
        "password": "TestPassword123!"
    }' 2>/dev/null)

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "
import sys,json
try:
    d = json.load(sys.stdin)
    print(d.get('accessToken', ''))
except:
    print('')
" 2>/dev/null)

if [ -n "$ACCESS_TOKEN" ]; then
    pass "User login successful, received access token"
    # Decode JWT header to verify structure
    TOKEN_PARTS=$(echo "$ACCESS_TOKEN" | tr '.' '\n' | wc -l)
    if [ "$TOKEN_PARTS" -eq 3 ]; then
        info "Token has valid JWT structure (3 parts)"
    fi
else
    fail "User login failed - no access token received"
    echo "Response: $LOGIN_RESPONSE"
fi

# ============================================================================
# TEST 9: JWT Validation
# ============================================================================
print_header "9. JWT Validation Tests"

if [ -n "$ACCESS_TOKEN" ]; then
    # Test accessing protected endpoint with valid token
    print_test "Accessing protected endpoint with valid token..."
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$KONG_PROXY/auth/me" \
        -H "Authorization: Bearer $ACCESS_TOKEN" 2>/dev/null || echo "000")
    if [ "$RESPONSE" = "200" ]; then
        pass "Protected endpoint accessible with valid token (HTTP $RESPONSE)"
    elif [ "$RESPONSE" = "404" ]; then
        warn "/auth/me route not configured (HTTP $RESPONSE)"
    else
        fail "Protected endpoint access failed (HTTP $RESPONSE)"
    fi

    # Test accessing protected endpoint without token
    print_test "Accessing protected endpoint without token..."
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$KONG_PROXY/auth/me" 2>/dev/null || echo "000")
    if [ "$RESPONSE" = "401" ]; then
        pass "Protected endpoint correctly requires authentication (HTTP $RESPONSE)"
    elif [ "$RESPONSE" = "404" ]; then
        warn "/auth/me route not configured (HTTP $RESPONSE)"
    else
        warn "Protected endpoint returned unexpected code (HTTP $RESPONSE)"
    fi

    # Test with invalid token
    print_test "Accessing protected endpoint with invalid token..."
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$KONG_PROXY/auth/me" \
        -H "Authorization: Bearer invalid.token.here" 2>/dev/null || echo "000")
    if [ "$RESPONSE" = "401" ]; then
        pass "Invalid token correctly rejected (HTTP $RESPONSE)"
    elif [ "$RESPONSE" = "404" ]; then
        warn "/auth/me route not configured (HTTP $RESPONSE)"
    else
        warn "Invalid token returned unexpected code (HTTP $RESPONSE)"
    fi
else
    warn "Skipping JWT validation tests - no valid token available"
fi

# ============================================================================
# TEST 10: Rate Limiting
# ============================================================================
print_header "10. Rate Limiting Tests"

print_test "Testing rate limiting on auth endpoints..."
RATE_LIMITED=false
for i in {1..15}; do
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$KONG_PROXY/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"email":"rate-test@example.com","password":"wrong"}' 2>/dev/null || echo "000")
    if [ "$RESPONSE" = "429" ]; then
        RATE_LIMITED=true
        break
    fi
done

if [ "$RATE_LIMITED" = true ]; then
    pass "Rate limiting is working (received HTTP 429 after $i requests)"
else
    warn "Rate limiting may not be configured (no 429 after 15 requests)"
fi

# ============================================================================
# TEST 11: CORS Headers
# ============================================================================
print_header "11. CORS Configuration"

print_test "Checking CORS headers..."
CORS_HEADERS=$(curl -s -I -X OPTIONS "$KONG_PROXY/auth/login" \
    -H "Origin: http://localhost:3000" \
    -H "Access-Control-Request-Method: POST" 2>/dev/null | grep -i "access-control" || echo "")

if [ -n "$CORS_HEADERS" ]; then
    pass "CORS headers present"
    echo "$CORS_HEADERS" | head -5
else
    warn "No CORS headers found"
fi

# ============================================================================
# TEST 12: Prometheus Metrics
# ============================================================================
print_header "12. Prometheus Metrics"

print_test "Checking Kong Prometheus metrics..."
METRICS=$(curl -s "$KONG_ADMIN/metrics" 2>/dev/null | head -20)
if [ -n "$METRICS" ]; then
    pass "Prometheus metrics endpoint is accessible"
    METRIC_COUNT=$(curl -s "$KONG_ADMIN/metrics" 2>/dev/null | grep -c "^kong_" || echo "0")
    info "Found $METRIC_COUNT Kong-specific metrics"
else
    warn "Prometheus metrics not available"
fi

# ============================================================================
# TEST 13: Request ID / Correlation
# ============================================================================
print_header "13. Request Correlation"

print_test "Checking X-Request-ID header..."
REQUEST_ID=$(curl -s -I "$KONG_PROXY/auth/login" 2>/dev/null | grep -i "x-request-id" || echo "")
if [ -n "$REQUEST_ID" ]; then
    pass "X-Request-ID header is present"
    echo "  $REQUEST_ID"
else
    warn "X-Request-ID header not found"
fi

# ============================================================================
# SUMMARY
# ============================================================================
print_header "TEST SUMMARY"

echo ""
echo -e "${GREEN}Passed:${NC}   $PASSED"
echo -e "${RED}Failed:${NC}   $FAILED"
echo -e "${YELLOW}Warnings:${NC} $WARNINGS"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All critical tests passed!${NC}"
    EXIT_CODE=0
else
    echo -e "${RED}Some tests failed. Please review the output above.${NC}"
    EXIT_CODE=1
fi

echo ""
echo "Detailed Results:"
echo "-----------------"
for result in "${RESULTS[@]}"; do
    if [[ $result == PASS* ]]; then
        echo -e "${GREEN}$result${NC}"
    elif [[ $result == FAIL* ]]; then
        echo -e "${RED}$result${NC}"
    else
        echo -e "${YELLOW}$result${NC}"
    fi
done

exit $EXIT_CODE
