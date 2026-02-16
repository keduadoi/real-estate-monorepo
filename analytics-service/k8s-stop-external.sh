#!/bin/bash
# Script to stop analytics-service with external MongoDB + Kafka

echo "🛑 Stopping Analytics Service"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Ask for confirmation
echo -e "${YELLOW}What do you want to stop?${NC}"
echo "1) Stop analytics-service in Kubernetes only (keep MongoDB + Kafka running)"
echo "2) Stop analytics-service, MongoDB, and Kafka"
echo "3) Full cleanup (uninstall Helm release, stop infra, delete data volumes)"
echo ""
read -p "Choose option (1-3): " -n 1 -r
echo ""
echo ""

case $REPLY in
    1)
        echo "🔄 Scaling down analytics-service deployment..."
        kubectl scale deployment/real-estate-analytics-analytics --replicas=0 -n real-estate 2>/dev/null
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Analytics service scaled down to 0 replicas${NC}"
            echo "   MongoDB and Kafka are still running in Docker"
            echo ""
            echo "To restart: kubectl scale deployment/real-estate-analytics-analytics --replicas=1 -n real-estate"
        else
            echo -e "${RED}❌ Failed to scale down analytics service${NC}"
        fi
        ;;
    2)
        echo "🛑 Stopping analytics-service, MongoDB, and Kafka..."

        # Scale down analytics
        echo "   Scaling down analytics-service..."
        kubectl scale deployment/real-estate-analytics-analytics --replicas=0 -n real-estate 2>/dev/null

        # Stop MongoDB and Kafka WITHOUT removing volumes
        echo "   Stopping MongoDB (preserving data)..."
        docker-compose -f docker-compose.yml stop mongo
        echo "   Stopping Kafka (preserving data)..."
        docker-compose -f docker-compose.yml stop kafka

        echo ""
        echo -e "${GREEN}✅ Services stopped${NC}"
        echo "   Analytics:  Scaled to 0 replicas"
        echo "   MongoDB:    Stopped (container stopped, volume preserved)"
        echo "   Kafka:      Stopped (container stopped, volume preserved)"
        echo ""
        echo "ℹ️  Data is preserved:"
        echo "   - MongoDB volume still exists (analytics data) ✅"
        echo "   - Kafka volume still exists (event data) ✅"
        echo ""
        echo "To restart: ./k8s-start-external.sh"
        ;;
    3)
        echo -e "${YELLOW}⚠️  Full cleanup will remove:${NC}"
        echo "   - Helm release"
        echo "   - Kubernetes resources for analytics"
        echo "   - MongoDB container and data volume"
        echo "   - Kafka container and data volume"
        echo ""
        read -p "Are you sure? (y/N): " -n 1 -r
        echo ""

        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo ""
            echo "🗑️  Performing full cleanup..."

            # Uninstall Helm release
            echo "   Uninstalling Helm release..."
            helm uninstall real-estate-analytics -n real-estate 2>/dev/null

            # Delete PVCs
            echo "   Deleting analytics PVCs..."
            kubectl delete pvc -l app.kubernetes.io/component=analytics -n real-estate 2>/dev/null

            # Stop and remove MongoDB + Kafka (including volumes)
            echo "   Removing MongoDB and Kafka..."
            docker-compose -f docker-compose.yml down -v

            echo ""
            echo -e "${GREEN}✅ Full cleanup completed${NC}"
            echo ""
            echo -e "${RED}⚠️  All analytics data has been deleted!${NC}"
            echo "   - Kubernetes resources removed"
            echo "   - MongoDB data volume removed"
            echo "   - Kafka data volume removed"
            echo ""
            echo "To start fresh: ./k8s-start-external.sh"
        else
            echo ""
            echo "❌ Cleanup cancelled"
        fi
        ;;
    *)
        echo "❌ Invalid option"
        exit 1
        ;;
esac

echo ""

# Show current status
echo "📊 Current Status:"
echo ""

echo "🍃 MongoDB (Docker):"
if docker ps | grep -q "analytics-mongo"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}" | grep analytics-mongo
else
    if docker ps -a | grep -q "analytics-mongo"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
    else
        echo -e "   ${RED}❌ Not found${NC}"
    fi
fi
echo ""

echo "📨 Kafka (Docker):"
if docker ps | grep -q "analytics-kafka"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}" | grep analytics-kafka
else
    if docker ps -a | grep -q "analytics-kafka"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
    else
        echo -e "   ${RED}❌ Not found${NC}"
    fi
fi
echo ""

echo "☸️  Analytics (Kubernetes):"
kubectl get pods -n real-estate -l app.kubernetes.io/component=analytics 2>/dev/null
if [ $? -ne 0 ]; then
    echo "   Namespace not found"
fi
echo ""

echo "💾 Data Volumes:"
echo "   Docker:"
docker volume ls | grep -E "mongo-data|kafka-data"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Quick Commands:"
echo "  Start:   ./k8s-start-external.sh"
echo "  Status:  ./k8s-status-external.sh"
echo "  Logs:    kubectl logs -f deployment/real-estate-analytics-analytics -n real-estate"
echo "           docker logs -f analytics-mongo"
echo "           docker logs -f analytics-kafka"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
