#!/bin/bash
# Script to stop Kubernetes backend port forwarding

echo "🛑 Stopping Kubernetes Backend..."
echo ""

# Kill port-forward processes
echo "🔌 Stopping port forwarding..."

# Stop backend port forward
if lsof -ti:8080 | xargs kill -9 2>/dev/null; then
    echo "✅ Backend port forward stopped (8080)"
else
    echo "ℹ️  No port forward was running on port 8080"
fi

# Stop PostgreSQL port forward
if lsof -ti:5432 | xargs kill -9 2>/dev/null; then
    echo "✅ PostgreSQL port forward stopped (5432)"
else
    echo "ℹ️  No port forward was running on port 5432"
fi
echo ""

# Show current status
echo "📊 Current pod status:"
kubectl get pods -n real-estate-dev
echo ""

echo "ℹ️  The Kubernetes pods are still running."
echo "   To completely stop everything:"
echo "   - Stop minikube:        minikube stop"
echo "   - Uninstall release:    helm uninstall real-estate"
echo ""
echo "💾 Note: Database data is preserved in PersistentVolumeClaims"
echo "   Data will persist across helm uninstall/reinstall cycles"
echo ""
echo "   To restart port-forward: ./k8s-start.sh"
