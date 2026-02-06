#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default environment
ENVIRONMENT=${1:-local}

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deploying Real Estate Backend - Plain K8s${NC}"
echo -e "${GREEN}Environment: ${ENVIRONMENT}${NC}"
echo -e "${GREEN}========================================${NC}"

# Validate environment
if [[ "$ENVIRONMENT" != "local" && "$ENVIRONMENT" != "minikube" ]]; then
    echo -e "${RED}Error: Invalid environment. Use 'local' or 'minikube'${NC}"
    echo "Usage: ./deploy.sh [local|minikube]"
    exit 1
fi

# Check if kubectl is available
if ! command -v kubectl &> /dev/null; then
    echo -e "${RED}Error: kubectl is not installed${NC}"
    exit 1
fi

# Check if cluster is accessible
if ! kubectl cluster-info &> /dev/null; then
    echo -e "${RED}Error: Cannot connect to Kubernetes cluster${NC}"
    exit 1
fi

echo -e "${YELLOW}Building backend Docker image...${NC}"
cd ../
docker build -t real-estate-backend:latest -f Dockerfile .
cd k8s-plain

# For minikube, load the image
if [[ "$ENVIRONMENT" == "minikube" ]]; then
    echo -e "${YELLOW}Loading image into minikube...${NC}"
    minikube image load real-estate-backend:latest
fi

echo -e "${YELLOW}Applying Kubernetes manifests...${NC}"

# Apply base resources in order
echo "1. Creating namespace..."
kubectl apply -f base/namespace.yaml

echo "2. Creating ConfigMap..."
kubectl apply -f base/configmap.yaml
# Note: ConfigMap overlays are not applied to avoid overriding base values
# To customize for different environments, edit base/configmap.yaml directly
# or use: kubectl patch configmap real-estate-backend-config -n real-estate --patch-file overlays/local/configmap-patch.yaml

echo "3. Creating Secret..."
kubectl apply -f base/secret.yaml

echo "4. Creating PersistentVolumeClaims..."
kubectl apply -f base/postgres-pvc.yaml
kubectl apply -f base/backend-pvc.yaml

echo "5. Deploying PostgreSQL..."
kubectl apply -f base/postgres-statefulset.yaml
kubectl apply -f base/postgres-service.yaml

echo "6. Deploying Backend Application..."
kubectl apply -f base/backend-deployment.yaml
kubectl apply -f base/backend-service.yaml

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment initiated successfully!${NC}"
echo -e "${GREEN}========================================${NC}"

echo ""
echo -e "${YELLOW}Waiting for PostgreSQL to be ready...${NC}"
kubectl wait --for=condition=ready pod -l component=database -n real-estate --timeout=120s

echo ""
echo -e "${YELLOW}Waiting for Backend to be ready...${NC}"
kubectl wait --for=condition=ready pod -l component=backend -n real-estate --timeout=180s

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}Deployment completed successfully!${NC}"
echo -e "${GREEN}========================================${NC}"

echo ""
echo "Access the application:"
if [[ "$ENVIRONMENT" == "minikube" ]]; then
    MINIKUBE_IP=$(minikube ip)
    echo -e "  Backend API: ${GREEN}http://${MINIKUBE_IP}:30080${NC}"
else
    echo -e "  Backend API: ${GREEN}http://localhost:30080${NC}"
fi

echo ""
echo "Useful commands:"
echo "  Check status: ./status.sh"
echo "  View logs: kubectl logs -f deployment/real-estate-backend-backend -n real-estate"
echo "  Clean up: ./cleanup.sh"
