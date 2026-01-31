#!/bin/bash
# Script to stop frontend development server

echo "🛑 Stopping Frontend"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Find and kill Next.js dev server
if pgrep -f "next dev" > /dev/null 2>&1; then
    echo "   Stopping Next.js dev server..."
    pkill -f "next dev"
    sleep 2

    if pgrep -f "next dev" > /dev/null 2>&1; then
        echo "   Force killing..."
        pkill -9 -f "next dev"
    fi

    echo -e "${GREEN}✅ Frontend stopped${NC}"
else
    echo -e "${YELLOW}ℹ️  Frontend is not running${NC}"
fi

# Also check for any node processes on port 3000
if lsof -i :3000 > /dev/null 2>&1; then
    echo "   Killing processes on port 3000..."
    lsof -ti :3000 | xargs kill -9 2>/dev/null
fi

echo ""
echo "To start again: ./start-dev.sh"
