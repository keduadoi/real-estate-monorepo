#!/bin/bash
set -e

# Configure Kong post-function plugin to forward JWT claims as headers
# This enables the backend to receive user info without parsing JWT
# Usage: ./configure-header-transformer.sh <KONG_ADMIN_URL>

KONG_ADMIN="${1:-http://localhost:30001}"

echo "=== Configuring JWT Claims Header Transformer ==="
echo "Admin URL: $KONG_ADMIN"
echo ""

# Verify Kong is reachable
curl -s "$KONG_ADMIN/" > /dev/null || { echo "ERROR: Cannot reach Kong Admin at $KONG_ADMIN"; exit 1; }

# The Lua function to extract JWT claims and set as headers
# This runs in the access phase after JWT validation
LUA_CODE='
local jwt_claims = kong.ctx.shared.authenticated_jwt_token
if jwt_claims then
  local cjson = require "cjson"

  -- Try to decode the JWT payload (middle part)
  local token = kong.request.get_header("Authorization")
  if token and token:sub(1, 7):lower() == "bearer " then
    local jwt_token = token:sub(8)
    local parts = {}
    for part in jwt_token:gmatch("[^%.]+") do
      table.insert(parts, part)
    end

    if #parts >= 2 then
      -- Decode base64url payload
      local payload_b64 = parts[2]
      -- Convert base64url to base64
      payload_b64 = payload_b64:gsub("-", "+"):gsub("_", "/")
      -- Add padding if needed
      local padding = 4 - (#payload_b64 % 4)
      if padding < 4 then
        payload_b64 = payload_b64 .. string.rep("=", padding)
      end

      local ok, payload_json = pcall(function()
        return ngx.decode_base64(payload_b64)
      end)

      if ok and payload_json then
        local ok2, claims = pcall(cjson.decode, payload_json)
        if ok2 and claims then
          -- Set headers from JWT claims
          if claims.sub then
            kong.service.request.set_header("X-User-Id", claims.sub)
          end
          if claims.email then
            kong.service.request.set_header("X-User-Email", claims.email)
          end
          if claims.roles then
            if type(claims.roles) == "table" then
              kong.service.request.set_header("X-User-Roles", table.concat(claims.roles, ","))
            else
              kong.service.request.set_header("X-User-Roles", tostring(claims.roles))
            end
          end
          if claims.iss then
            kong.service.request.set_header("X-Token-Issuer", claims.iss)
          end
        end
      end
    end
  end
end
'

# Apply the post-function plugin globally
echo "Configuring post-function plugin for JWT claims extraction..."

# First, check if plugin already exists and delete it
EXISTING_PLUGIN=$(curl -s "$KONG_ADMIN/plugins" | python3 -c "
import sys, json
d = json.load(sys.stdin)
plugins = [p['id'] for p in d.get('data', []) if p['name'] == 'post-function' and p.get('service') is None and p.get('route') is None]
print(plugins[0] if plugins else '')
" 2>/dev/null || echo "")

if [ -n "$EXISTING_PLUGIN" ]; then
  echo "  Removing existing global post-function plugin..."
  curl -s -X DELETE "$KONG_ADMIN/plugins/$EXISTING_PLUGIN" > /dev/null
fi

# Create the plugin with Lua code
curl -s -X POST "$KONG_ADMIN/plugins" \
  -H "Content-Type: application/json" \
  -d "$(cat << 'EOFPYTHON' | python3
import json
lua_code = '''
local jwt_claims = kong.ctx.shared.authenticated_jwt_token
if jwt_claims then
  local cjson = require "cjson"
  local token = kong.request.get_header("Authorization")
  if token and token:sub(1, 7):lower() == "bearer " then
    local jwt_token = token:sub(8)
    local parts = {}
    for part in jwt_token:gmatch("[^%.]+") do
      table.insert(parts, part)
    end
    if #parts >= 2 then
      local payload_b64 = parts[2]
      payload_b64 = payload_b64:gsub("-", "+"):gsub("_", "/")
      local padding = 4 - (#payload_b64 % 4)
      if padding < 4 then
        payload_b64 = payload_b64 .. string.rep("=", padding)
      end
      local ok, payload_json = pcall(function()
        return ngx.decode_base64(payload_b64)
      end)
      if ok and payload_json then
        local ok2, claims = pcall(cjson.decode, payload_json)
        if ok2 and claims then
          if claims.sub then
            kong.service.request.set_header("X-User-Id", claims.sub)
          end
          if claims.email then
            kong.service.request.set_header("X-User-Email", claims.email)
          end
          if claims.roles then
            if type(claims.roles) == "table" then
              kong.service.request.set_header("X-User-Roles", table.concat(claims.roles, ","))
            else
              kong.service.request.set_header("X-User-Roles", tostring(claims.roles))
            end
          end
          if claims.iss then
            kong.service.request.set_header("X-Token-Issuer", claims.iss)
          end
        end
      end
    end
  end
end
'''
print(json.dumps({
    "name": "post-function",
    "config": {
        "access": [lua_code]
    }
}))
EOFPYTHON
)" > /dev/null 2>&1

RESULT=$?
if [ $RESULT -eq 0 ]; then
  echo "  Post-function plugin configured successfully"
  echo ""
  echo "The following headers will be added to upstream requests:"
  echo "  - X-User-Id: JWT 'sub' claim (user UUID)"
  echo "  - X-User-Email: JWT 'email' claim"
  echo "  - X-User-Roles: JWT 'roles' claim (comma-separated)"
  echo "  - X-Token-Issuer: JWT 'iss' claim"
else
  echo "  WARNING: post-function plugin may not be available in Kong OSS"
  echo "  The backend will need to parse JWT directly for user info"
fi

echo ""
echo "=== Header Transformer Configuration Complete ==="
