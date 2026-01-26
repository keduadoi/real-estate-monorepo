#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Real Estate Backend - Status${NC}"
echo -e "${GREEN}========================================${NC}"

echo ""
echo -e "${BLUE}=== Namespace ===${NC}"
kubectl get namespace real-estate

echo ""
echo -e "${BLUE}=== Pods ===${NC}"
kubectl get pods -n real-estate -o wide

echo ""
echo -e "${BLUE}=== Services ===${NC}"
kubectl get services -n real-estate

echo ""
echo -e "${BLUE}=== Deployments ===${NC}"
kubectl get deployments -n real-estate

echo ""
echo -e "${BLUE}=== StatefulSets ===${NC}"
kubectl get statefulsets -n real-estate

echo ""
echo -e "${BLUE}=== PersistentVolumeClaims ===${NC}"
kubectl get pvc -n real-estate

echo ""
echo -e "${BLUE}=== ConfigMaps ===${NC}"
kubectl get configmaps -n real-estate

echo ""
echo -e "${BLUE}=== Secrets ===${NC}"
kubectl get secrets -n real-estate

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${YELLOW}Detailed Pod Information:${NC}"
echo -e "${GREEN}========================================${NC}"

# Get pod details
BACKEND_POD=$(kubectl get pods -n real-estate -l component=backend -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)
POSTGRES_POD=$(kubectl get pods -n real-estate -l component=database -o jsonpath='{.items[0].metadata.name}' 2>/dev/null)

if [ -n "$BACKEND_POD" ]; then
    echo ""
    echo -e "${BLUE}Backend Pod Status:${NC}"
    kubectl describe pod $BACKEND_POD -n real-estate | grep -E "Status:|Conditions:|Ready:"
fi

if [ -n "$POSTGRES_POD" ]; then
    echo ""
    echo -e "${BLUE}PostgreSQL Pod Status:${NC}"
    kubectl describe pod $POSTGRES_POD -n real-estate | grep -E "Status:|Conditions:|Ready:"
fi

echo ""
echo -e "${YELLOW}To view logs:${NC}"
echo "  Backend: kubectl logs -f deployment/real-estate-backend-backend -n real-estate"
echo "  PostgreSQL: kubectl logs -f statefulset/real-estate-backend-postgres -n real-estate"
