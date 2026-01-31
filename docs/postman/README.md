# Postman Collection for Real Estate Kong API

This folder contains Postman collection and environment files for testing the Real Estate API via Kong Gateway.

## Files

| File | Description |
|------|-------------|
| `Real_Estate_Kong_API.postman_collection.json` | Main API collection with all endpoints |
| `Real_Estate_Kong_Environment.postman_environment.json` | Local development environment |
| `Real_Estate_Kong_Production.postman_environment.json` | Production environment template |

## Quick Start

### 1. Import into Postman

1. Open Postman
2. Click **Import** button (top left)
3. Drag and drop both files:
   - `Real_Estate_Kong_API.postman_collection.json`
   - `Real_Estate_Kong_Environment.postman_environment.json`
4. Click **Import**

### 2. Select Environment

1. Click the environment dropdown (top right)
2. Select **Real Estate Kong (Local)**

### 3. Start Kong Gateway

Make sure Kong is running and port-forwarded:

```bash
# Port forward Kong proxy
kubectl port-forward -n kong svc/kong-proxy 8000:80 &

# Port forward Kong admin (optional, for debugging)
kubectl port-forward -n kong svc/kong-admin 8001:8001 &
```

### 4. Test the API

1. **Health Checks** - Start here to verify connectivity
2. **Authentication > Register User** - Create a test account
3. **Authentication > Login** - Get JWT tokens (automatically saved)
4. **Properties** - Test CRUD operations

## Collection Structure

```
Real Estate API (Kong Gateway)
├── Health Checks
│   ├── Kong Health
│   ├── Auth Service Health
│   └── JWKS Endpoint
├── Authentication
│   ├── Register User
│   ├── Login
│   ├── Refresh Token
│   ├── Get Current User
│   ├── Change Password
│   ├── Forgot Password
│   ├── Reset Password
│   └── Logout
├── Properties
│   ├── List Properties (Public)
│   ├── Get Property by ID (Public)
│   ├── Search Properties (Public)
│   ├── Get Cities (Public)
│   ├── Get My Properties
│   ├── Create Property
│   ├── Update Property
│   └── Delete Property
├── File Upload
│   ├── Upload Property Image
│   └── Delete Property Image
├── Admin
│   ├── Get Key Statistics
│   ├── Trigger Key Rotation
│   ├── Get All Users (Admin)
│   └── Get All Properties (Admin)
└── Testing
    ├── Test Rate Limiting
    ├── Test CORS (Preflight)
    ├── Test Invalid Token
    ├── Test Missing Token
    └── Test Request ID Header
```

## Automatic Token Management

The collection includes scripts that automatically:
- Save `access_token` after login/register
- Save `refresh_token` after login/register
- Save `property_id` after creating a property

Protected endpoints automatically use `{{access_token}}` from the environment.

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `kong_url` | Kong Gateway URL | `http://127.0.0.1:8000` |
| `kong_admin_url` | Kong Admin API URL | `http://127.0.0.1:8001` |
| `access_token` | JWT access token | Auto-populated |
| `refresh_token` | JWT refresh token | Auto-populated |
| `property_id` | Last created property ID | Auto-populated |
| `test_email` | Test user email | `testuser@example.com` |
| `test_password` | Test user password | `TestPassword123` |

## Rate Limits

| Endpoint Type | Limit |
|---------------|-------|
| Auth endpoints (`/auth/*`) | 10 requests/minute |
| API endpoints (`/api/*`) | 100 requests/minute |
| Upload endpoints (`/api/upload/*`) | 10 requests/minute |

## Running Collection Tests

### Via Postman UI

1. Click on the collection name
2. Click **Run** button
3. Select requests to run
4. Click **Run Real Estate API**

### Via Newman (CLI)

```bash
# Install Newman
npm install -g newman

# Run collection
newman run Real_Estate_Kong_API.postman_collection.json \
  -e Real_Estate_Kong_Environment.postman_environment.json \
  --reporters cli,json

# Run with HTML report
newman run Real_Estate_Kong_API.postman_collection.json \
  -e Real_Estate_Kong_Environment.postman_environment.json \
  --reporters cli,htmlextra \
  --reporter-htmlextra-export ./report.html
```

## Troubleshooting

### 401 Unauthorized

1. Check if token has expired (15 minutes for access token)
2. Run **Login** request to get new tokens
3. Check if Kong is properly forwarding to Auth Service

### 429 Too Many Requests

Rate limit exceeded. Wait for the limit to reset (check `RateLimit-Reset` header).

### Connection Refused

1. Check if Kong is running: `kubectl get pods -n kong`
2. Check port forwarding: `ps aux | grep port-forward`
3. Restart port forwarding if needed

### CORS Errors

If testing from a web browser:
1. Ensure origin is in Kong's CORS allowed list
2. Check `Access-Control-Allow-Origin` header in response

## Production Usage

For production:
1. Import `Real_Estate_Kong_Production.postman_environment.json`
2. Update `kong_url` to your production API URL
3. Remove or disable admin endpoints (Admin API not exposed in production)
