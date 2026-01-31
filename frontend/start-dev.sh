#!/bin/bash
# Script to start frontend development server

echo "🚀 Starting Frontend (Next.js)"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Check Node.js
echo "🔍 Checking prerequisites..."
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js is not installed${NC}"
    echo "   Install from: https://nodejs.org/"
    exit 1
fi

NODE_VERSION=$(node -v)
echo -e "   Node.js: ${GREEN}${NODE_VERSION}${NC}"

if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ npm is not installed${NC}"
    exit 1
fi

NPM_VERSION=$(npm -v)
echo -e "   npm: ${GREEN}${NPM_VERSION}${NC}"
echo ""

# Check if node_modules exists
if [ ! -d "$SCRIPT_DIR/node_modules" ]; then
    echo -e "${YELLOW}📦 Installing dependencies...${NC}"
    cd "$SCRIPT_DIR" && npm install
    echo ""
fi

# Check .env.local
if [ ! -f "$SCRIPT_DIR/.env.local" ]; then
    echo -e "${YELLOW}⚠️  .env.local not found. Creating default...${NC}"
    cat > "$SCRIPT_DIR/.env.local" << 'EOF'
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=your-secret-key-here-change-in-production-min-32-characters-long

# Kong Gateway URL - All API traffic routes through Kong
NEXT_PUBLIC_KONG_URL=http://127.0.0.1:8000

# Backend API URL (via Kong Gateway)
NEXT_PUBLIC_API_URL=http://127.0.0.1:8000/api

# Auth Service URL (via Kong Gateway)
NEXT_PUBLIC_AUTH_API_URL=http://127.0.0.1:8000

# Image Upload Configuration
NEXT_PUBLIC_MAX_IMAGE_SIZE=10485760
NEXT_PUBLIC_MAX_IMAGES_PER_PROPERTY=10
EOF
    echo -e "${GREEN}✅ Created .env.local with default configuration${NC}"
    echo ""
fi

# Check backend connectivity
echo "🔍 Checking backend connectivity..."
if curl -s http://localhost:8000/api/properties > /dev/null 2>&1; then
    echo -e "   Kong Gateway: ${GREEN}✅ Available${NC}"
else
    echo -e "   Kong Gateway: ${YELLOW}⚠️  Not available at localhost:8000${NC}"
    echo "   Run: ./scripts/start-all-services.sh (from project root)"
    echo ""
fi

# Start the development server
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${BLUE}🌐 Starting Next.js development server...${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📍 Access URLs:"
echo "   Frontend:     http://localhost:3000"
echo "   API Gateway:  http://localhost:8000"
echo ""
echo "🔧 Commands:"
echo "   Stop server:  Ctrl+C"
echo "   Build:        npm run build"
echo "   Lint:         npm run lint"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

cd "$SCRIPT_DIR" && npm run dev
