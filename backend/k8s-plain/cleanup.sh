#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}Cleaning up Real Estate Backend${NC}"
echo -e "${YELLOW}========================================${NC}"

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}Error: kubectl is not installed${NC}"
    exit 1
fi

echo -e "${YELLOW}This will delete all resources in the 'real-estate' namespace.${NC}"
read -p "Are you sure you want to continue? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo ""
echo -e "${YELLOW}Deleting resources...${NC}"

echo "1. Deleting Backend resources..."
kubectl delete -f base/backend-service.yaml --ignore-not-found=true
kubectl delete -f base/backend-deployment.yaml --ignore-not-found=true

echo "2. Deleting PostgreSQL resources..."
kubectl delete -f base/postgres-service.yaml --ignore-not-found=true
kubectl delete -f base/postgres-statefulset.yaml --ignore-not-found=true

echo "3. Deleting PersistentVolumeClaims..."
read -p "Do you want to delete PVCs (this will delete all data)? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    kubectl delete -f base/backend-pvc.yaml --ignore-not-found=true
    kubectl delete -f base/postgres-pvc.yaml --ignore-not-found=true
    echo -e "${YELLOW}PVCs deleted${NC}"
else
    echo -e "${YELLOW}PVCs preserved${NC}"
fi

echo "4. Deleting ConfigMaps and Secrets..."
kubectl delete -f base/secret.yaml --ignore-not-found=true
kubectl delete -f base/configmap.yaml --ignore-not-found=true

echo "5. Deleting Namespace..."
kubectl delete -f base/namespace.yaml --ignore-not-found=true

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Cleanup completed!${NC}"
echo -e "${GREEN}========================================${NC}"

echo ""
echo "To redeploy: ./deploy.sh [local|minikube]"
