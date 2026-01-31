#!/bin/bash
# Script to check frontend status

echo "📊 Frontend Status"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Check if Next.js dev server is running
echo "🌐 Next.js Dev Server:"
if pgrep -f "next dev" > /dev/null 2>&1; then
    echo -e "   ${GREEN}✅ Running${NC}"
    PID=$(pgrep -f "next dev")
    echo "   PID: $PID"
else
    echo -e "   ${YELLOW}⏸️  Not running${NC}"
    echo "   Start with: ./start-dev.sh"
fi
echo ""

# Check port 3000
echo "🔌 Port 3000:"
if lsof -i :3000 > /dev/null 2>&1; then
    echo -e "   ${GREEN}✅ In use${NC}"
    lsof -i :3000 | grep LISTEN | awk '{print "   Process: " $1 " (PID: " $2 ")"}'
else
    echo -e "   ${YELLOW}⚠️  Available${NC}"
fi
echo ""

# Check frontend health
echo "🏥 Health Check:"
if curl -s -o /dev/null -w "%{http_code}" http://localhost:3000 2>/dev/null | grep -qE "200|500"; then
    RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3000)
    if [ "$RESPONSE" == "200" ]; then
        echo -e "   ${GREEN}✅ Frontend is healthy (HTTP 200)${NC}"
    else
        echo -e "   ${YELLOW}⚠️  Frontend responding with HTTP $RESPONSE${NC}"
        echo "   This might be normal if backend is not running"
    fi
else
    echo -e "   ${RED}❌ Frontend not accessible${NC}"
fi
echo ""

# Check backend connectivity
echo "🔗 Backend Connectivity:"
if curl -s http://localhost:8000/api/properties > /dev/null 2>&1; then
    echo -e "   Kong Gateway: ${GREEN}✅ Available${NC}"
else
    echo -e "   Kong Gateway: ${RED}❌ Not available${NC}"
fi

if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "   Backend API:  ${GREEN}✅ Available${NC}"
else
    echo -e "   Backend API:  ${RED}❌ Not available${NC}"
fi
echo ""

# Check .env.local
echo "📄 Configuration:"
if [ -f ".env.local" ]; then
    echo -e "   .env.local: ${GREEN}✅ Present${NC}"
    echo "   API URL: $(grep NEXT_PUBLIC_API_URL .env.local | cut -d'=' -f2)"
else
    echo -e "   .env.local: ${RED}❌ Missing${NC}"
fi
echo ""

# Check node_modules
echo "📦 Dependencies:"
if [ -d "node_modules" ]; then
    echo -e "   node_modules: ${GREEN}✅ Installed${NC}"
else
    echo -e "   node_modules: ${YELLOW}⚠️  Not installed${NC}"
    echo "   Run: npm install"
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📍 URLs:"
echo "   Frontend:  http://localhost:3000"
echo "   API:       http://localhost:8000"
echo ""
echo "🔧 Commands:"
echo "   Start:     ./start-dev.sh"
echo "   Stop:      ./stop.sh"
echo "   Build:     npm run build"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
