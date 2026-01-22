# Real Estate Backend - Kubernetes Deployment with Helm

This Helm chart deploys the Real Estate Backend application with PostgreSQL database to Kubernetes for local development using Minikube.

## Prerequisites

- Docker Desktop or Docker Engine
- Minikube (v1.37.0+)
- Helm (v4.1.0+)
- kubectl

## Quick Start

### 1. Start Minikube

```bash
minikube start --memory=3500 --cpus=2
```

### 2. Build Docker Image

Build the Docker image and load it into Minikube's Docker environment:

```bash
# Set Docker environment to use Minikube's Docker daemon
eval $(minikube docker-env)

# Build the image
cd backend
docker build -t real-estate-backend:latest .
```

### 3. Install the Helm Chart

Install the chart with development values:

```bash
helm install real-estate ./backend/helm/real-estate-backend \
  -f ./backend/helm/real-estate-backend/values-dev.yaml
```

### 4. Verify Deployment

Check that all pods are running:

```bash
kubectl get pods -n real-estate-dev
```

Expected output:
```
NAME                                                       READY   STATUS    RESTARTS   AGE
real-estate-real-estate-backend-backend-xxxxxxxxxx-xxxxx   1/1     Running   0          2m
real-estate-real-estate-backend-postgres-0                 1/1     Running   0          2m
```

## Accessing the Application

### Option 1: Port Forwarding (Recommended for Development)

```bash
kubectl port-forward -n real-estate-dev \
  svc/real-estate-real-estate-backend-backend 8080:8080
```

Then access the application at `http://localhost:8080`

### Option 2: Minikube Service

```bash
minikube service real-estate-real-estate-backend-backend -n real-estate-dev
```

This will automatically open the application in your default browser.

### Option 3: NodePort Access

```bash
export NODE_IP=$(minikube ip)
export NODE_PORT=$(kubectl get -n real-estate-dev -o jsonpath="{.spec.ports[0].nodePort}" \
  services real-estate-real-estate-backend-backend)
echo "Application URL: http://$NODE_IP:$NODE_PORT"
```

## Testing the Application

Test the health endpoint:

```bash
# Using port-forward
curl http://localhost:8080/actuator/health

# Or using NodePort
curl http://$(minikube ip):30080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    }
  }
}
```

## Common Operations

### View Application Logs

```bash
# Backend logs
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=backend -f

# PostgreSQL logs
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=database -f
```

### Connect to PostgreSQL

```bash
kubectl exec -it -n real-estate-dev \
  real-estate-real-estate-backend-postgres-0 -- \
  psql -U postgres -d realestatedb
```

### Upgrade the Chart

After making changes to the chart or values:

```bash
helm upgrade real-estate ./backend/helm/real-estate-backend \
  -f ./backend/helm/real-estate-backend/values-dev.yaml
```

### Rollback to Previous Version

```bash
helm rollback real-estate
```

### Uninstall the Chart

```bash
helm uninstall real-estate
kubectl delete namespace real-estate-dev
```

## Configuration

### Values Files

- `values.yaml` - Default configuration values
- `values-dev.yaml` - Development environment overrides

### Key Configuration Options

| Parameter | Description | Default |
|-----------|-------------|---------|
| `namespace.name` | Kubernetes namespace | `real-estate-dev` |
| `backend.replicaCount` | Number of backend replicas | `1` |
| `backend.image.tag` | Backend image tag | `latest` |
| `backend.service.nodePort` | NodePort for backend service | `30080` |
| `backend.persistence.size` | Uploads storage size | `2Gi` (dev) |
| `postgresql.persistence.size` | Database storage size | `2Gi` (dev) |
| `postgresql.auth.database` | Database name | `realestatedb` |
| `postgresql.auth.username` | Database user | `postgres` |
| `postgresql.auth.password` | Database password | `postgres` |

### Customizing Values

Create a custom values file or override values during installation:

```bash
helm install real-estate ./backend/helm/real-estate-backend \
  --set backend.replicaCount=2 \
  --set postgresql.auth.password=mypassword
```

## Troubleshooting

### Pods Not Starting

Check pod events:
```bash
kubectl describe pod -n real-estate-dev <pod-name>
```

Check pod logs:
```bash
kubectl logs -n real-estate-dev <pod-name>
```

### Database Connection Issues

Verify PostgreSQL is running:
```bash
kubectl get pods -n real-estate-dev -l app.kubernetes.io/component=database
```

Check database service:
```bash
kubectl get svc -n real-estate-dev real-estate-real-estate-backend-postgres
```

### Image Pull Errors

Ensure you're using Minikube's Docker daemon:
```bash
eval $(minikube docker-env)
docker images | grep real-estate-backend
```

If the image is missing, rebuild it:
```bash
cd backend
docker build -t real-estate-backend:latest .
```

### Persistent Volume Issues

Delete and recreate PVCs:
```bash
helm uninstall real-estate
kubectl delete pvc --all -n real-estate-dev
helm install real-estate ./backend/helm/real-estate-backend \
  -f ./backend/helm/real-estate-backend/values-dev.yaml
```

## Architecture

```
┌─────────────────────────────────────────────┐
│         Kubernetes Cluster (Minikube)       │
│                                             │
│  ┌──────────────────────────────────────┐  │
│  │  Namespace: real-estate-dev          │  │
│  │                                      │  │
│  │  ┌─────────────────────────────┐    │  │
│  │  │  Backend Deployment         │    │  │
│  │  │  - Spring Boot App          │    │  │
│  │  │  - Port: 8080               │    │  │
│  │  │  - Persistent uploads       │    │  │
│  │  └─────────────────────────────┘    │  │
│  │            │                         │  │
│  │            ▼                         │  │
│  │  ┌─────────────────────────────┐    │  │
│  │  │  Backend Service            │    │  │
│  │  │  - Type: NodePort           │    │  │
│  │  │  - NodePort: 30080          │    │  │
│  │  └─────────────────────────────┘    │  │
│  │                                      │  │
│  │  ┌─────────────────────────────┐    │  │
│  │  │  PostgreSQL StatefulSet     │    │  │
│  │  │  - PostgreSQL 18-alpine     │    │  │
│  │  │  - Port: 5432               │    │  │
│  │  │  - Persistent data          │    │  │
│  │  └─────────────────────────────┘    │  │
│  │            │                         │  │
│  │            ▼                         │  │
│  │  ┌─────────────────────────────┐    │  │
│  │  │  PostgreSQL Service         │    │  │
│  │  │  - Type: ClusterIP          │    │  │
│  │  └─────────────────────────────┘    │  │
│  │                                      │  │
│  │  ┌─────────────────────────────┐    │  │
│  │  │  ConfigMap & Secrets        │    │  │
│  │  │  - App configuration        │    │  │
│  │  │  - DB credentials           │    │  │
│  │  └─────────────────────────────┘    │  │
│  │                                      │  │
│  │  ┌─────────────────────────────┐    │  │
│  │  │  Persistent Volumes         │    │  │
│  │  │  - Backend uploads: 2Gi     │    │  │
│  │  │  - PostgreSQL data: 2Gi     │    │  │
│  │  └─────────────────────────────┘    │  │
│  └──────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
```

## Resources

- [Helm Documentation](https://helm.sh/docs/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Minikube Documentation](https://minikube.sigs.k8s.io/docs/)

## Support

For issues and questions, please refer to the main project README or open an issue in the repository.
