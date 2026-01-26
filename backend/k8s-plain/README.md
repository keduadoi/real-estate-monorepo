# Plain Kubernetes Deployment

This directory contains plain Kubernetes manifests for deploying the Real Estate Backend application without Helm. This serves as a learning resource to understand Kubernetes fundamentals and compare with Helm deployments.

## 📁 Directory Structure

```
k8s-plain/
├── base/                           # Base Kubernetes manifests
│   ├── namespace.yaml              # Namespace definition
│   ├── configmap.yaml              # Application configuration
│   ├── secret.yaml                 # Sensitive data (credentials)
│   ├── postgres-pvc.yaml           # PostgreSQL storage claim
│   ├── postgres-statefulset.yaml  # PostgreSQL deployment
│   ├── postgres-service.yaml       # PostgreSQL service
│   ├── backend-pvc.yaml            # Backend storage claim
│   ├── backend-deployment.yaml     # Backend application deployment
│   ├── backend-service.yaml        # Backend service
│   └── kustomization.yaml          # Kustomize configuration
├── overlays/                       # Environment-specific overrides
│   ├── local/                      # Local development
│   │   ├── kustomization.yaml
│   │   └── configmap-patch.yaml
│   └── minikube/                   # Minikube environment
│       ├── kustomization.yaml
│       └── configmap-patch.yaml
├── deploy.sh                       # Deployment script
├── status.sh                       # Status check script
├── cleanup.sh                      # Cleanup script
└── README.md                       # This file
```

## 🚀 Quick Start

### Prerequisites

- Docker
- Kubernetes cluster (Docker Desktop, Minikube, or similar)
- kubectl

### Deploy to Local Kubernetes

```bash
# Deploy to local Kubernetes cluster
./deploy.sh local

# Or deploy to Minikube
./deploy.sh minikube
```

### Check Status

```bash
./status.sh
```

### Access the Application

**Local Kubernetes:**
```
Backend API: http://localhost:30080
```

**Minikube:**
```
Backend API: http://$(minikube ip):30080
```

### Cleanup

```bash
./cleanup.sh
```

## 📝 Manual Deployment (Step by Step)

If you want to understand each step:

```bash
# 1. Build the Docker image
cd ../
docker build -t real-estate-backend:latest -f Dockerfile .
cd k8s-plain

# 2. For Minikube, load the image
minikube image load real-estate-backend:latest  # Only for Minikube

# 3. Apply manifests in order
kubectl apply -f base/namespace.yaml
kubectl apply -f base/configmap.yaml
kubectl apply -f base/secret.yaml
kubectl apply -f base/postgres-pvc.yaml
kubectl apply -f base/backend-pvc.yaml
kubectl apply -f base/postgres-statefulset.yaml
kubectl apply -f base/postgres-service.yaml
kubectl apply -f base/backend-deployment.yaml
kubectl apply -f base/backend-service.yaml

# 4. Wait for pods to be ready
kubectl wait --for=condition=ready pod -l component=database -n real-estate --timeout=120s
kubectl wait --for=condition=ready pod -l component=backend -n real-estate --timeout=180s
```

## 🔍 Useful Commands

### View Resources

```bash
# View all resources in the namespace
kubectl get all -n real-estate

# View pods with details
kubectl get pods -n real-estate -o wide

# View services
kubectl get services -n real-estate

# View PVCs
kubectl get pvc -n real-estate
```

### View Logs

```bash
# Backend logs
kubectl logs -f deployment/real-estate-backend-backend -n real-estate

# PostgreSQL logs
kubectl logs -f statefulset/real-estate-backend-postgres -n real-estate

# View logs for a specific pod
kubectl logs -f <pod-name> -n real-estate
```

### Debug Pods

```bash
# Describe a pod to see events and status
kubectl describe pod <pod-name> -n real-estate

# Execute commands in a pod
kubectl exec -it <pod-name> -n real-estate -- /bin/sh

# Port forward for local access
kubectl port-forward service/real-estate-backend-backend 8080:8080 -n real-estate
```

### Update Deployment

```bash
# Update image
kubectl set image deployment/real-estate-backend-backend backend=real-estate-backend:v2 -n real-estate

# Restart deployment
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate

# Check rollout status
kubectl rollout status deployment/real-estate-backend-backend -n real-estate

# Rollback to previous version
kubectl rollout undo deployment/real-estate-backend-backend -n real-estate
```

### Scaling

```bash
# Scale backend replicas
kubectl scale deployment/real-estate-backend-backend --replicas=3 -n real-estate
```

## 🎯 Using Kustomize

Kustomize is built into kubectl and allows for environment-specific configurations:

```bash
# Apply with kustomize for local environment
kubectl apply -k overlays/local/

# Apply with kustomize for minikube environment
kubectl apply -k overlays/minikube/

# View the generated manifest without applying
kubectl kustomize overlays/local/
```

## 🔐 Security Notes

The `secret.yaml` file contains base64-encoded credentials:
- Default username: `postgres`
- Default password: `postgres`
- Default database: `realestatedb`

**⚠️ IMPORTANT:** Change these credentials for production deployments!

To create a new secret:
```bash
echo -n 'your-password' | base64
```

## 🏗️ Architecture

### Components

1. **Namespace**: `real-estate`
   - Isolates all resources in a dedicated namespace

2. **PostgreSQL Database**
   - StatefulSet with 1 replica
   - ClusterIP service (internal only)
   - PersistentVolumeClaim for data storage (5Gi)
   - Health checks (liveness and readiness probes)

3. **Backend Application**
   - Deployment with 1 replica
   - NodePort service (exposed on port 30080)
   - PersistentVolumeClaim for uploads (5Gi)
   - Health checks via /actuator/health endpoint

4. **Configuration**
   - ConfigMap: Non-sensitive configuration
   - Secret: Database credentials

### Resource Limits

**Backend:**
- Requests: 500m CPU, 512Mi Memory
- Limits: 1000m CPU, 1024Mi Memory

**PostgreSQL:**
- Requests: 250m CPU, 256Mi Memory
- Limits: 500m CPU, 512Mi Memory

## 🔄 Differences from Helm Deployment

See [HELM_VS_PLAIN_K8S.md](../../HELM_VS_PLAIN_K8S.md) for a comprehensive comparison.

### Quick Comparison

| Aspect | Plain K8s | Helm |
|--------|-----------|------|
| Configuration | Hard-coded in manifests | Templated with values.yaml |
| Deployment | kubectl apply | helm install/upgrade |
| Updates | Edit manifests + apply | Update values.yaml + upgrade |
| Rollback | Manual or kubectl rollout undo | helm rollback |
| Multi-environment | Multiple manifest files or Kustomize | Single chart with multiple values files |
| Package management | Manual file management | Packaged as charts |
| Version control | File-based | Helm release history |

## 🎓 Learning Resources

### Key Kubernetes Concepts Demonstrated

1. **Namespaces**: Resource isolation
2. **ConfigMaps**: Configuration management
3. **Secrets**: Sensitive data handling
4. **Deployments**: Stateless application management
5. **StatefulSets**: Stateful application management
6. **Services**: Network access to pods
7. **PersistentVolumeClaims**: Storage management
8. **Probes**: Health checking (liveness and readiness)
9. **Resource Limits**: Resource management
10. **Labels and Selectors**: Resource organization and selection

### Next Steps

1. Modify the ConfigMap and apply changes
2. Scale the backend deployment
3. Try rolling updates
4. Experiment with different service types
5. Add an Ingress resource
6. Implement Network Policies
7. Add resource quotas

## 🐛 Troubleshooting

### Pods not starting

```bash
# Check pod status
kubectl get pods -n real-estate

# Check pod events
kubectl describe pod <pod-name> -n real-estate

# Check logs
kubectl logs <pod-name> -n real-estate
```

### Image pull errors

```bash
# For local images, ensure ImagePullPolicy is IfNotPresent
# For Minikube, ensure image is loaded:
minikube image load real-estate-backend:latest
```

### Database connection issues

```bash
# Check if PostgreSQL is running
kubectl get pods -n real-estate -l component=database

# Check PostgreSQL logs
kubectl logs -f statefulset/real-estate-backend-postgres -n real-estate

# Verify service
kubectl get service real-estate-backend-postgres -n real-estate
```

### PVC issues

```bash
# Check PVC status
kubectl get pvc -n real-estate

# Check PV
kubectl get pv

# Describe PVC for events
kubectl describe pvc <pvc-name> -n real-estate
```

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Kustomize Documentation](https://kubernetes.io/docs/tasks/manage-kubernetes-objects/kustomization/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)
