#!/bin/bash
# Script to stop price-service with external PostgreSQL + Kafka

echo "🛑 Stopping Price Service"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Ask for confirmation
echo -e "${YELLOW}What do you want to stop?${NC}"
echo "1) Stop price-service in Kubernetes only (keep PostgreSQL + Kafka running)"
echo "2) Stop price-service and PostgreSQL (keep Kafka running for other services)"
echo "3) Stop price-service, PostgreSQL, and Kafka"
echo "4) Full cleanup (uninstall Helm release, stop infra, delete data volumes)"
echo ""
read -p "Choose option (1-4): " -n 1 -r
echo ""
echo ""

case $REPLY in
    1)
        echo "🔄 Scaling down price-service deployment..."
        kubectl scale deployment/real-estate-price-price --replicas=0 -n real-estate 2>/dev/null
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Price service scaled down to 0 replicas${NC}"
            echo "   PostgreSQL and Kafka are still running in Docker"
            echo ""
            echo "To restart: kubectl scale deployment/real-estate-price-price --replicas=1 -n real-estate"
        else
            echo -e "${RED}❌ Failed to scale down price service${NC}"
        fi
        ;;
    2)
        echo "🛑 Stopping price-service and PostgreSQL..."

        # Scale down price service
        echo "   Scaling down price-service..."
        kubectl scale deployment/real-estate-price-price --replicas=0 -n real-estate 2>/dev/null

        # Stop PostgreSQL WITHOUT removing volumes
        echo "   Stopping PostgreSQL (preserving data)..."
        docker-compose -f docker-compose-db.yml stop

        echo ""
        echo -e "${GREEN}✅ Services stopped${NC}"
        echo "   Price Service: Scaled to 0 replicas"
        echo "   PostgreSQL:    Stopped (container stopped, volume preserved)"
        echo "   Kafka:         Still running"
        echo ""
        echo "ℹ️  Data is preserved:"
        echo "   - PostgreSQL volume still exists (price data) ✅"
        echo ""
        echo "To restart: ./k8s-start-external.sh"
        ;;
    3)
        echo "🛑 Stopping price-service, PostgreSQL, and Kafka..."

        # Scale down price service
        echo "   Scaling down price-service..."
        kubectl scale deployment/real-estate-price-price --replicas=0 -n real-estate 2>/dev/null

        # Stop PostgreSQL WITHOUT removing volumes
        echo "   Stopping PostgreSQL (preserving data)..."
        docker-compose -f docker-compose-db.yml stop

        # Stop Kafka WITHOUT removing volumes
        echo "   Stopping Kafka (preserving data)..."
        docker-compose -f ../kafka/docker-compose.yml stop

        echo ""
        echo -e "${GREEN}✅ Services stopped${NC}"
        echo "   Price Service: Scaled to 0 replicas"
        echo "   PostgreSQL:    Stopped (container stopped, volume preserved)"
        echo "   Kafka:         Stopped (container stopped, volume preserved)"
        echo ""
        echo -e "${YELLOW}⚠️  Note: Stopping Kafka may affect other services (analytics-service)${NC}"
        echo ""
        echo "ℹ️  Data is preserved:"
        echo "   - PostgreSQL volume still exists (price data) ✅"
        echo "   - Kafka volume still exists (event data) ✅"
        echo ""
        echo "To restart: ./k8s-start-external.sh"
        ;;
    4)
        echo -e "${YELLOW}⚠️  Full cleanup will remove:${NC}"
        echo "   - Helm release"
        echo "   - Kubernetes resources for price-service"
        echo "   - PostgreSQL container and data volume"
        echo "   - Kafka container and data volume (shared!)"
        echo ""
        echo -e "${RED}⚠️  Warning: Removing Kafka will affect analytics-service too!${NC}"
        echo ""
        read -p "Are you sure? (y/N): " -n 1 -r
        echo ""

        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo ""
            echo "🗑️  Performing full cleanup..."

            # Uninstall Helm release
            echo "   Uninstalling Helm release..."
            helm uninstall real-estate-price -n real-estate 2>/dev/null

            # Delete PVCs
            echo "   Deleting price-service PVCs..."
            kubectl delete pvc -l app.kubernetes.io/component=price -n real-estate 2>/dev/null

            # Stop and remove PostgreSQL (including volume)
            echo "   Removing PostgreSQL..."
            docker-compose -f docker-compose-db.yml down -v

            # Stop and remove Kafka (including volume)
            echo "   Removing Kafka..."
            docker-compose -f ../kafka/docker-compose.yml down -v

            echo ""
            echo -e "${GREEN}✅ Full cleanup completed${NC}"
            echo ""
            echo -e "${RED}⚠️  All data has been deleted!${NC}"
            echo "   - Kubernetes resources removed"
            echo "   - PostgreSQL data volume removed"
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

echo "🐘 PostgreSQL (Docker):"
if docker ps | grep -q "price-db"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}" | grep price-db
else
    if docker ps -a | grep -q "price-db"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
    else
        echo -e "   ${RED}❌ Not found${NC}"
    fi
fi
echo ""

echo "📨 Kafka (Docker):"
if docker ps | grep -q "kafka"; then
    echo -e "   ${GREEN}✅ Running${NC}"
    docker ps --format "table {{.Names}}\t{{.Status}}" | grep kafka
else
    if docker ps -a | grep -q "kafka"; then
        echo -e "   ${YELLOW}⏸️  Stopped${NC}"
    else
        echo -e "   ${RED}❌ Not found${NC}"
    fi
fi
echo ""

echo "☸️  Price Service (Kubernetes):"
kubectl get pods -n real-estate -l app.kubernetes.io/component=price 2>/dev/null
if [ $? -ne 0 ]; then
    echo "   Namespace not found"
fi
echo ""

echo "💾 Data Volumes:"
echo "   Docker:"
docker volume ls | grep -E "price-db-data|kafka-data"
echo "   K8s PVCs:"
kubectl get pvc -l app.kubernetes.io/component=price -n real-estate 2>/dev/null || echo "   No PVCs found"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Quick Commands:"
echo "  Start:   ./k8s-start-external.sh"
echo "  Status:  ./k8s-status-external.sh"
echo "  Logs:    kubectl logs -f deployment/real-estate-price-price -n real-estate"
echo "           docker logs -f price-db"
echo "           docker logs -f kafka"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
