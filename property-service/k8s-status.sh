#!/bin/bash
# Script to check Kubernetes backend status

echo "📊 Kubernetes Backend Status"
echo "=============================="
echo ""

# Check minikube
echo "🖥️  Minikube:"
minikube status
echo ""

# Check helm release
echo "📦 Helm Release:"
helm list
echo ""

# Check pods
echo "🐳 Pods:"
kubectl get pods -n real-estate-dev
echo ""

# Check services
echo "🌐 Services:"
kubectl get svc -n real-estate-dev
echo ""

# Check persistent volumes
echo "💾 Persistent Volumes:"
kubectl get pvc -n real-estate-dev
echo ""

# Check port-forward
echo "🔌 Port Forwarding:"
if lsof -i:8080 | grep -q LISTEN; then
    echo "✅ Port 8080 is forwarded"
    lsof -i:8080 | grep LISTEN
else
    echo "❌ Port 8080 is not forwarded"
    echo "   Run: ./k8s-start.sh"
fi
echo ""

# Test health endpoint
echo "🏥 Health Check:"
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo "✅ Backend is healthy"
    curl -s http://localhost:8080/actuator/health | jq -r '.status'
else
    echo "❌ Backend is not accessible"
    echo "   Check if port-forward is running: ./k8s-start.sh"
fi
echo ""

echo "=============================="
echo "Quick Commands:"
echo "  Start:  ./k8s-start.sh"
echo "  Stop:   ./k8s-stop.sh"
echo "  Logs:   kubectl logs -n real-estate-dev -l app.kubernetes.io/component=backend -f"
