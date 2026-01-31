# Helm vs Plain Kubernetes: A Comprehensive Comparison

This document provides a detailed comparison between deploying applications using Helm charts versus plain Kubernetes manifests, using the Real Estate Backend application as a practical example.

## 📋 Table of Contents

1. [Overview](#overview)
2. [Side-by-Side Comparison](#side-by-side-comparison)
3. [Detailed Analysis](#detailed-analysis)
4. [When to Use What](#when-to-use-what)
5. [Hands-On Exercises](#hands-on-exercises)
6. [Conclusion](#conclusion)

## 🎯 Overview

### Plain Kubernetes Manifests
Plain Kubernetes manifests are YAML files that directly describe Kubernetes resources. They are the fundamental building blocks of Kubernetes deployments.

**Location:** `backend/k8s-plain/`

### Helm Charts
Helm is a package manager for Kubernetes that uses templates to generate Kubernetes manifests dynamically. It adds a layer of abstraction and configuration management.

**Location:** `backend/helm/real-estate-backend/`

## 🔄 Side-by-Side Comparison

### 1. Directory Structure

#### Plain Kubernetes
```
k8s-plain/
├── base/
│   ├── namespace.yaml              # 7 lines
│   ├── configmap.yaml              # 13 lines
│   ├── secret.yaml                 # 19 lines
│   ├── backend-deployment.yaml     # 104 lines
│   ├── backend-service.yaml        # 15 lines
│   ├── backend-pvc.yaml            # 13 lines
│   ├── postgres-statefulset.yaml   # 84 lines
│   ├── postgres-service.yaml       # 14 lines
│   ├── postgres-pvc.yaml           # 13 lines
│   └── kustomization.yaml          # 13 lines
├── overlays/
│   ├── local/
│   │   ├── kustomization.yaml      # 11 lines
│   │   └── configmap-patch.yaml    # 7 lines
│   └── minikube/
│       ├── kustomization.yaml      # 11 lines
│       └── configmap-patch.yaml    # 7 lines
└── scripts/
    ├── deploy.sh
    ├── status.sh
    └── cleanup.sh

Total: ~331 lines of YAML + 3 bash scripts
```

#### Helm
```
helm/real-estate-backend/
├── Chart.yaml                       # 16 lines (metadata)
├── values.yaml                      # 146 lines (all config)
├── values-dev.yaml                  # 23 lines
├── values-local.yaml                # 54 lines
├── values-minikube.yaml             # 58 lines
└── templates/
    ├── _helpers.tpl                 # 74 lines (template functions)
    ├── namespace.yaml               # 9 lines
    ├── configmap.yaml               # 21 lines
    ├── secret.yaml                  # 20 lines
    ├── backend-deployment.yaml      # 101 lines
    ├── backend-service.yaml         # 21 lines
    ├── backend-pvc.yaml             # 19 lines
    ├── postgres-statefulset.yaml    # 84 lines
    ├── postgres-service.yaml        # 19 lines
    ├── postgres-pvc.yaml            # 19 lines
    └── NOTES.txt                    # 81 lines (deployment notes)

Total: ~765 lines (more lines but more flexible)
```

### 2. Configuration Management

#### Plain Kubernetes - ConfigMap

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: real-estate-backend-config
  namespace: real-estate
data:
  SPRING_PROFILES_ACTIVE: "dev"
  SERVER_PORT: "8080"
  DB_HOST: "real-estate-backend-postgres"
  DB_PORT: "5432"
  DB_NAME: "realestatedb"
  CORS_ALLOWED_ORIGINS: "http://localhost:3000"
```

**Characteristics:**
- ✅ Simple and direct
- ✅ Easy to understand
- ❌ Hard-coded values
- ❌ Requires file editing to change
- ❌ Need separate files for each environment

#### Helm - ConfigMap Template

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: {{ include "real-estate-backend.fullname" . }}-config
  namespace: {{ .Values.namespace.name }}
data:
  SPRING_PROFILES_ACTIVE: {{ .Values.backend.env.springProfile | quote }}
  SERVER_PORT: {{ .Values.backend.env.serverPort | quote }}
  {{- if .Values.postgresql.enabled }}
  DB_HOST: {{ include "real-estate-backend.postgresql.serviceName" . }}
  DB_PORT: {{ .Values.postgresql.service.port | quote }}
  {{- else }}
  DB_HOST: {{ .Values.backend.env.dbHost | quote }}
  {{- end }}
```

**With values.yaml:**
```yaml
backend:
  env:
    springProfile: dev
    serverPort: 8080
    corsAllowedOrigins: "http://localhost:3000"
```

**Characteristics:**
- ✅ Templated and reusable
- ✅ Single source of truth (values.yaml)
- ✅ Easy to override per environment
- ✅ Conditional logic support
- ❌ More complex to understand initially
- ❌ Template syntax learning curve

### 3. Deployment Process

#### Plain Kubernetes

```bash
# Build image
docker build -t real-estate-backend:latest -f Dockerfile .

# Apply manifests one by one (order matters!)
kubectl apply -f base/namespace.yaml
kubectl apply -f base/configmap.yaml
kubectl apply -f base/secret.yaml
kubectl apply -f base/postgres-pvc.yaml
kubectl apply -f base/backend-pvc.yaml
kubectl apply -f base/postgres-statefulset.yaml
kubectl apply -f base/postgres-service.yaml
kubectl apply -f base/backend-deployment.yaml
kubectl apply -f base/backend-service.yaml

# Or use kustomize
kubectl apply -k overlays/local/
```

**Pros:**
- Direct control over what gets deployed
- Clear order of operations
- No additional tools needed (except kubectl)

**Cons:**
- Manual ordering required
- No rollback mechanism
- No release management
- Deployment history not tracked

#### Helm

```bash
# Build image
docker build -t real-estate-backend:latest -f Dockerfile .

# Install or upgrade
helm upgrade --install real-estate ./helm/real-estate-backend \
  --namespace real-estate \
  --create-namespace \
  --values ./helm/real-estate-backend/values-local.yaml

# That's it!
```

**Pros:**
- Single command deployment
- Automatic ordering of resources
- Built-in rollback capability
- Release history tracking
- Hooks for lifecycle management

**Cons:**
- Requires Helm to be installed
- Abstraction can hide issues
- More complex initial setup

### 4. Environment Management

#### Plain Kubernetes - Using Kustomize

**Base Configuration:**
```yaml
# base/configmap.yaml
data:
  SPRING_PROFILES_ACTIVE: "dev"
```

**Local Override:**
```yaml
# overlays/local/configmap-patch.yaml
data:
  SPRING_PROFILES_ACTIVE: "local"
```

**Usage:**
```bash
kubectl apply -k overlays/local/
kubectl apply -k overlays/minikube/
```

**Pros:**
- Native Kubernetes approach (built into kubectl)
- Clear inheritance model
- Good for small variations

**Cons:**
- Limited templating capabilities
- Can become complex with many overlays
- No package management

#### Helm - Multiple Values Files

**Base Configuration:**
```yaml
# values.yaml (defaults)
backend:
  env:
    springProfile: dev
    corsAllowedOrigins: "http://localhost:3000"
```

**Local Override:**
```yaml
# values-local.yaml
backend:
  env:
    springProfile: local
    corsAllowedOrigins: "http://localhost:3000,http://localhost:3001"
```

**Usage:**
```bash
helm upgrade --install real-estate ./helm/real-estate-backend \
  --values ./helm/real-estate-backend/values-local.yaml

helm upgrade --install real-estate ./helm/real-estate-backend \
  --values ./helm/real-estate-backend/values-minikube.yaml
```

**Pros:**
- Powerful templating
- Easy to override specific values
- Can combine multiple values files
- Can override from command line

**Cons:**
- Values file can become large
- Template complexity can grow

### 5. Updates and Rollbacks

#### Plain Kubernetes

**Update Deployment:**
```bash
# Edit the manifest file
vim base/backend-deployment.yaml

# Apply changes
kubectl apply -f base/backend-deployment.yaml

# Check rollout
kubectl rollout status deployment/real-estate-backend-backend -n real-estate

# Rollback (limited history)
kubectl rollout undo deployment/real-estate-backend-backend -n real-estate
```

**Characteristics:**
- Manual file editing
- Limited rollback history (by default: 10 revisions)
- No version tracking of configuration
- No easy way to see what changed

#### Helm

**Update Deployment:**
```bash
# Edit values file
vim helm/real-estate-backend/values-local.yaml

# Upgrade release
helm upgrade real-estate ./helm/real-estate-backend \
  --values ./helm/real-estate-backend/values-local.yaml

# Or override specific values
helm upgrade real-estate ./helm/real-estate-backend \
  --set backend.replicaCount=3

# Check status
helm status real-estate

# Rollback to previous version
helm rollback real-estate

# Rollback to specific revision
helm rollback real-estate 2
```

**Characteristics:**
- Configuration-driven updates
- Full revision history
- Easy rollback to any version
- Can see what changed between revisions
- Can test upgrades with --dry-run

### 6. Practical Examples

#### Example 1: Scaling the Backend

**Plain Kubernetes:**
```bash
# Method 1: Edit manifest
vim base/backend-deployment.yaml
# Change replicas: 1 to replicas: 3
kubectl apply -f base/backend-deployment.yaml

# Method 2: Direct scaling
kubectl scale deployment/real-estate-backend-backend --replicas=3 -n real-estate

# Note: This doesn't update your manifest file!
```

**Helm:**
```bash
# Method 1: Edit values
vim helm/real-estate-backend/values.yaml
# Change replicaCount: 1 to replicaCount: 3
helm upgrade real-estate ./helm/real-estate-backend

# Method 2: Override at deployment
helm upgrade real-estate ./helm/real-estate-backend \
  --set backend.replicaCount=3

# Configuration is tracked in Helm release
```

#### Example 2: Changing Environment Variables

**Plain Kubernetes:**
```bash
# Edit ConfigMap
vim base/configmap.yaml
# Add new variable: NEW_VAR: "value"

# Apply changes
kubectl apply -f base/configmap.yaml

# Restart pods to pick up changes
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate
```

**Helm:**
```bash
# Edit values
vim helm/real-estate-backend/values.yaml
# Add under backend.env: newVar: "value"

# Update template to use it
vim helm/real-estate-backend/templates/configmap.yaml
# Add: NEW_VAR: {{ .Values.backend.env.newVar | quote }}

# Upgrade
helm upgrade real-estate ./helm/real-estate-backend

# Helm automatically handles the rollout
```

#### Example 3: Deploying to Multiple Environments

**Plain Kubernetes:**
```bash
# Create environment-specific overlays
mkdir -p overlays/production

# Create production kustomization
cat > overlays/production/kustomization.yaml <<EOF
bases:
  - ../../base
patchesStrategicMerge:
  - configmap-patch.yaml
commonLabels:
  environment: production
EOF

# Create production patches
cat > overlays/production/configmap-patch.yaml <<EOF
data:
  SPRING_PROFILES_ACTIVE: "prod"
EOF

# Deploy
kubectl apply -k overlays/production/
```

**Helm:**
```bash
# Create production values file
cat > values-production.yaml <<EOF
backend:
  env:
    springProfile: prod
  replicaCount: 3
EOF

# Deploy
helm upgrade --install real-estate-prod ./helm/real-estate-backend \
  --values ./helm/real-estate-backend/values-production.yaml \
  --namespace real-estate-prod \
  --create-namespace
```

## 🎯 Detailed Analysis

### Complexity

| Aspect | Plain K8s | Helm |
|--------|-----------|------|
| Initial Setup | ⭐⭐ Simple | ⭐⭐⭐⭐ Complex |
| Understanding | ⭐⭐ Easy | ⭐⭐⭐⭐ Harder |
| Maintenance | ⭐⭐⭐ Moderate | ⭐⭐ Easier |
| Debugging | ⭐⭐ Straightforward | ⭐⭐⭐ Can be tricky |

### Flexibility

| Aspect | Plain K8s | Helm |
|--------|-----------|------|
| Customization | Limited | Extensive |
| Reusability | Low | High |
| Templating | Via Kustomize | Built-in |
| Conditional Logic | Limited | Extensive |

### Operations

| Aspect | Plain K8s | Helm |
|--------|-----------|------|
| Deployment | Manual/Scripted | Automated |
| Updates | Manual | Automated |
| Rollbacks | Limited | Full |
| Version Control | File-based | Release-based |
| Multi-env Support | Kustomize overlays | Values files |

### Learning Curve

**Plain Kubernetes:**
1. Learn Kubernetes resources (Pods, Deployments, Services, etc.)
2. Learn kubectl commands
3. Optionally learn Kustomize

**Estimated time:** 1-2 weeks to be productive

**Helm:**
1. Learn everything in Plain Kubernetes
2. Learn Helm concepts (Charts, Releases, Repositories)
3. Learn Helm template syntax (Go templates)
4. Learn Helm commands
5. Learn helper functions and best practices

**Estimated time:** 3-4 weeks to be productive

## 🤔 When to Use What

### Use Plain Kubernetes When:

1. **Learning Kubernetes**
   - Best way to understand how Kubernetes works
   - Direct mapping between what you write and what gets deployed
   - No abstraction layers hiding details

2. **Simple Applications**
   - Single-environment deployments
   - Few configuration variations
   - Straightforward deployments without complex dependencies

3. **Full Control Required**
   - Need to understand exactly what's deployed
   - Debugging requires seeing actual manifests
   - Organization policies prefer plain YAML

4. **Team Preference**
   - Team is not familiar with Helm
   - Team prefers simpler tooling
   - Want to avoid additional dependencies

5. **CI/CD Integration**
   - CI/CD system doesn't support Helm well
   - Want standard kubectl commands
   - Kustomize meets your needs

### Use Helm When:

1. **Multi-Environment Deployments**
   - Deploy same application to dev, staging, production
   - Need environment-specific configurations
   - Want consistent deployments across environments

2. **Complex Applications**
   - Many microservices
   - Complex dependencies between services
   - Need conditional resource creation

3. **Package Distribution**
   - Sharing applications with others
   - Installing third-party applications
   - Creating reusable components

4. **Lifecycle Management**
   - Need rollback capabilities
   - Want deployment history
   - Need hooks for pre/post deployment tasks

5. **Configuration Management**
   - Many configuration options
   - Need to override values easily
   - Want centralized configuration

6. **Team Scalability**
   - Large teams working on same application
   - Different teams managing different environments
   - Need consistent deployment processes

### Hybrid Approach: Kustomize

Kustomize offers a middle ground:
- Native to Kubernetes (built into kubectl)
- Provides overlays for environment-specific configs
- No templating - uses patches instead
- Less complex than Helm but more flexible than plain manifests

**Use Kustomize when:**
- You want more flexibility than plain K8s
- You don't need Helm's full feature set
- You want to avoid template complexity
- Your team prefers kubectl-native tools

## 🎓 Hands-On Exercises

### Exercise 1: Deploy Both Versions

**Goal:** Deploy the application using both methods and compare.

```bash
# 1. Deploy with plain Kubernetes
cd backend/k8s-plain
./deploy.sh local

# 2. Check what was deployed
./status.sh

# 3. Access the application
curl http://localhost:30080/actuator/health

# 4. Cleanup
./cleanup.sh

# 5. Deploy with Helm
cd ../helm
helm upgrade --install real-estate ./real-estate-backend \
  --values ./real-estate-backend/values-local.yaml \
  --namespace real-estate \
  --create-namespace

# 6. Check what was deployed
helm status real-estate
kubectl get all -n real-estate

# 7. Cleanup
helm uninstall real-estate -n real-estate
```

**Questions to Consider:**
- Which deployment method felt more intuitive?
- Which one gave you more visibility into what was deployed?
- Which one would be easier to repeat?

### Exercise 2: Make a Configuration Change

**Plain Kubernetes:**
```bash
# 1. Edit ConfigMap
vim backend/k8s-plain/base/configmap.yaml
# Add: TEST_VAR: "hello"

# 2. Apply changes
kubectl apply -f backend/k8s-plain/base/configmap.yaml

# 3. Restart deployment
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate

# 4. Verify
kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- env | grep TEST_VAR
```

**Helm:**
```bash
# 1. Edit values
vim backend/helm/real-estate-backend/values.yaml
# Add under backend.env: testVar: "hello"

# 2. Edit template
vim backend/helm/real-estate-backend/templates/configmap.yaml
# Add: TEST_VAR: {{ .Values.backend.env.testVar | quote }}

# 3. Upgrade
helm upgrade real-estate ./backend/helm/real-estate-backend

# 4. Verify
kubectl exec -it deployment/real-estate-backend-backend -n real-estate -- env | grep TEST_VAR
```

**Questions:**
- Which method required more steps?
- Which method is more maintainable across environments?
- How would you propagate this change to production in each approach?

### Exercise 3: Rollback a Deployment

**Plain Kubernetes:**
```bash
# 1. Make a bad change
kubectl set image deployment/real-estate-backend-backend \
  backend=real-estate-backend:broken -n real-estate

# 2. Watch it fail
kubectl get pods -n real-estate -w

# 3. Rollback
kubectl rollout undo deployment/real-estate-backend-backend -n real-estate

# 4. Verify
kubectl rollout status deployment/real-estate-backend-backend -n real-estate
```

**Helm:**
```bash
# 1. Make a bad change
helm upgrade real-estate ./backend/helm/real-estate-backend \
  --set backend.image.tag=broken

# 2. Watch it fail
kubectl get pods -n real-estate -w

# 3. Check history
helm history real-estate

# 4. Rollback
helm rollback real-estate

# 5. Verify
helm status real-estate
```

**Questions:**
- Which rollback was easier?
- Which one gave you better visibility into what changed?
- How would you prevent this issue in each approach?

### Exercise 4: Deploy to Multiple Environments

**Plain Kubernetes:**
```bash
# Create production namespace and deploy
kubectl apply -k backend/k8s-plain/overlays/minikube/

# Deploy to another namespace for staging
kubectl create namespace real-estate-staging
kubectl apply -f backend/k8s-plain/base/ -n real-estate-staging
```

**Helm:**
```bash
# Deploy to production
helm upgrade --install real-estate-prod ./backend/helm/real-estate-backend \
  --values ./backend/helm/real-estate-backend/values-prod.yaml \
  --namespace real-estate-prod \
  --create-namespace

# Deploy to staging
helm upgrade --install real-estate-staging ./backend/helm/real-estate-backend \
  --values ./backend/helm/real-estate-backend/values-staging.yaml \
  --namespace real-estate-staging \
  --create-namespace
```

**Questions:**
- Which approach makes environment separation clearer?
- How would you ensure consistency across environments?
- Which would be easier to automate in CI/CD?

## 📊 Feature Comparison Matrix

| Feature | Plain K8s | Plain K8s + Kustomize | Helm |
|---------|-----------|----------------------|------|
| Learning Curve | Low | Medium | High |
| Configuration Management | ❌ | ✅ | ✅✅ |
| Templating | ❌ | ⚠️ Limited | ✅✅ |
| Package Management | ❌ | ❌ | ✅✅ |
| Version Control | File-based | File-based | Release-based |
| Rollback Support | ⚠️ Limited | ⚠️ Limited | ✅✅ |
| Multi-Environment | ❌ | ✅ | ✅✅ |
| Dependency Management | ❌ | ❌ | ✅✅ |
| Hooks/Lifecycle | ❌ | ❌ | ✅✅ |
| Community Charts | ❌ | ❌ | ✅✅ |
| Debugging Ease | ✅✅ | ✅ | ⚠️ Harder |
| IDE Support | ✅✅ | ✅ | ⚠️ Limited |
| CI/CD Integration | ✅ | ✅✅ | ✅ |

Legend:
- ✅✅ = Excellent
- ✅ = Good
- ⚠️ = Limited/Partial
- ❌ = Not supported

## 🎯 Conclusion

### For Learning Kubernetes
**Start with Plain Kubernetes**, then move to Kustomize, and finally Helm. This progression helps you understand:
1. How Kubernetes resources work
2. How to manage configuration variations
3. How to package and template applications

### For Production Use
The choice depends on your specific needs:

- **Small teams, simple apps**: Plain Kubernetes or Kustomize
- **Large teams, complex apps**: Helm
- **Multi-environment deployments**: Helm or Kustomize
- **Package distribution**: Helm
- **Maximum control and simplicity**: Plain Kubernetes

### Best Practice Recommendation
1. **Learn**: Start with plain Kubernetes manifests
2. **Grow**: Add Kustomize for environment management
3. **Scale**: Adopt Helm when complexity increases

### Real-World Usage
- **Plain K8s**: ~20% of production deployments
- **Kustomize**: ~30% of production deployments
- **Helm**: ~50% of production deployments

Both approaches are valid, and many organizations use a combination based on their specific needs.

## 📚 Additional Resources

### Plain Kubernetes
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [kubectl Cheat Sheet](https://kubernetes.io/docs/reference/kubectl/cheatsheet/)
- [Kubernetes Best Practices](https://kubernetes.io/docs/concepts/configuration/overview/)

### Kustomize
- [Kustomize Documentation](https://kubernetes.io/docs/tasks/manage-kubernetes-objects/kustomization/)
- [Kustomize GitHub](https://github.com/kubernetes-sigs/kustomize)

### Helm
- [Helm Documentation](https://helm.sh/docs/)
- [Helm Best Practices](https://helm.sh/docs/chart_best_practices/)
- [Artifact Hub](https://artifacthub.io/) - Browse public Helm charts

### Comparisons
- [GitOps with Kustomize vs Helm](https://www.weave.works/blog/gitops-with-kustomize)
- [Kustomize vs Helm](https://www.youtube.com/watch?v=ZMFYSm0ldQ0)
- [CNCF Survey on Kubernetes Deployments](https://www.cncf.io/reports/)
