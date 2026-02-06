# Kubernetes Deployment Comparison Guide

This project includes TWO different Kubernetes deployment approaches for learning and comparison purposes.

## 🎯 Overview

| Approach | Location | Best For | Complexity |
|----------|----------|----------|------------|
| **Plain Kubernetes** | `backend/k8s-plain/` | Learning K8s fundamentals | ⭐⭐ Low |
| **Helm Charts** | `backend/helm/real-estate-backend/` | Production deployments | ⭐⭐⭐⭐ High |

## 🚀 Quick Start

### Option 1: Plain Kubernetes (Recommended for Learning)

```bash
cd backend/k8s-plain
./deploy.sh local
./status.sh
```

**What you'll learn:**
- How Kubernetes resources work directly
- ConfigMaps, Secrets, Deployments, Services
- StatefulSets for databases
- PersistentVolumeClaims for storage
- Health probes and resource management

### Option 2: Helm (Recommended for Production)

```bash
cd backend/helm
helm upgrade --install real-estate ./real-estate-backend \
  --values ./real-estate-backend/values-local.yaml \
  --namespace real-estate \
  --create-namespace
```

**What you'll learn:**
- Package management for Kubernetes
- Templating and configuration management
- Release management and rollbacks
- Multi-environment deployments
- Lifecycle hooks

## 📊 Key Differences

### 1. File Structure

**Plain K8s:**
- Direct YAML files with actual values
- One file per resource type
- Kustomize for environment variations
- ~331 lines of YAML

**Helm:**
- Template files with variables
- Single values.yaml for configuration
- Multiple environment values files
- ~765 lines (templates + values)

### 2. Deployment Commands

**Plain K8s:**
```bash
# Deploy
kubectl apply -f base/namespace.yaml
kubectl apply -f base/configmap.yaml
kubectl apply -f base/secret.yaml
# ... (8 more files)

# Or with script
./deploy.sh local

# Or with Kustomize
kubectl apply -k overlays/local/
```

**Helm:**
```bash
# Deploy or update - single command!
helm upgrade --install real-estate ./real-estate-backend \
  --values values-local.yaml

# That's it!
```

### 3. Configuration Changes

**Plain K8s:**
```bash
# Edit the manifest file
vim base/configmap.yaml

# Apply changes
kubectl apply -f base/configmap.yaml

# Restart to pick up changes
kubectl rollout restart deployment/real-estate-backend-backend -n real-estate
```

**Helm:**
```bash
# Edit the values file
vim values-local.yaml

# Upgrade (automatically handles restart)
helm upgrade real-estate ./real-estate-backend
```

### 4. Rollback

**Plain K8s:**
```bash
# Limited rollback (only deployment changes)
kubectl rollout undo deployment/real-estate-backend-backend -n real-estate

# No history of ConfigMap/Secret changes
```

**Helm:**
```bash
# Full rollback of entire release
helm rollback real-estate

# With history
helm history real-estate
```

### 5. Multi-Environment Support

**Plain K8s:**
```bash
# Create overlay for each environment
kubectl apply -k overlays/local/
kubectl apply -k overlays/minikube/
kubectl apply -k overlays/production/
```

**Helm:**
```bash
# Use different values files
helm install real-estate-dev ./chart -f values-dev.yaml
helm install real-estate-staging ./chart -f values-staging.yaml
helm install real-estate-prod ./chart -f values-prod.yaml
```

## 🎓 Learning Path

### Day 1-2: Start with Plain Kubernetes
1. Deploy using `k8s-plain/deploy.sh`
2. Explore each manifest file in `k8s-plain/base/`
3. Understand what each resource does
4. Try making changes and redeploying
5. Use `kubectl` commands to inspect resources

**Why start here?**
- See exactly what Kubernetes does
- Understand the building blocks
- No abstraction layers
- Direct mapping of concepts to resources

### Day 3-5: Explore Kustomize
1. Look at `k8s-plain/overlays/`
2. Understand how patches work
3. Deploy to different environments
4. Learn about base + overlay pattern

**Why Kustomize?**
- Native Kubernetes approach
- Simple environment management
- No templating complexity
- Good middle ground

### Week 2: Move to Helm
1. Deploy using Helm
2. Study `helm/real-estate-backend/templates/`
3. Compare templates with plain manifests
4. Understand template syntax and functions
5. Experiment with values files

**Why Helm?**
- Industry standard for complex apps
- Powerful package management
- Great for production use
- Extensive ecosystem

## 🔍 Side-by-Side Example

### Scaling the Backend Application

**Plain Kubernetes:**
```bash
# Edit the file
vim k8s-plain/base/backend-deployment.yaml
# Change: replicas: 1 → replicas: 3

# Apply
kubectl apply -f k8s-plain/base/backend-deployment.yaml
```

**Helm:**
```bash
# Option 1: Edit values
vim helm/real-estate-backend/values.yaml
# Change: replicaCount: 1 → replicaCount: 3
helm upgrade real-estate ./helm/real-estate-backend

# Option 2: Command line override
helm upgrade real-estate ./helm/real-estate-backend \
  --set backend.replicaCount=3
```

**Result:** Both achieve the same outcome, but Helm tracks the change in release history!

## 📈 Pros and Cons

### Plain Kubernetes

**Pros:**
- ✅ Easy to understand
- ✅ No additional tools needed
- ✅ Direct control
- ✅ Great for learning
- ✅ Straightforward debugging

**Cons:**
- ❌ Manual file management
- ❌ Repetitive configurations
- ❌ Limited rollback capability
- ❌ Hard to manage multiple environments
- ❌ No package management

### Helm

**Pros:**
- ✅ Configuration management
- ✅ Package distribution
- ✅ Release management
- ✅ Easy rollbacks
- ✅ Multi-environment support
- ✅ Large ecosystem (Artifact Hub)
- ✅ Dependency management

**Cons:**
- ❌ Steeper learning curve
- ❌ Template complexity
- ❌ Harder to debug
- ❌ Additional tool dependency
- ❌ Can hide Kubernetes details

## 🎯 When to Use What?

### Use Plain Kubernetes When:
- 🎓 Learning Kubernetes fundamentals
- 🏠 Simple, single-environment deployments
- 🔍 Need full visibility and control
- 👥 Small team with simple needs
- 📝 Kubernetes-native approach preferred

### Use Helm When:
- 🏭 Production deployments
- 🌍 Multiple environments (dev/staging/prod)
- 📦 Distributing applications
- 👥 Large teams
- 🔄 Need sophisticated lifecycle management
- 🎯 Using third-party applications

### Use Both!
This project gives you both options so you can:
- Learn Kubernetes with plain manifests
- Understand the benefits of Helm
- Make informed decisions for your own projects
- Compare approaches side-by-side

## 🧪 Hands-On Exercises

### Exercise 1: Deploy Both and Compare
```bash
# 1. Deploy with plain K8s
cd backend/k8s-plain
./deploy.sh local
kubectl get all -n real-estate

# 2. Cleanup
./cleanup.sh

# 3. Deploy with Helm
cd ../helm
helm upgrade --install real-estate ./real-estate-backend \
  --values ./real-estate-backend/values-local.yaml \
  --namespace real-estate \
  --create-namespace
kubectl get all -n real-estate

# Compare the output - they should be similar!
```

### Exercise 2: Make Configuration Changes
```bash
# Try changing the Spring profile
# - In plain K8s: edit base/configmap.yaml
# - In Helm: edit values-local.yaml

# Which was easier? Which is more maintainable?
```

### Exercise 3: Rollback a Change
```bash
# Try making a bad change and rolling back
# - In plain K8s: kubectl rollout undo
# - In Helm: helm rollback

# Which gives you more confidence? Why?
```

## 📚 Documentation

- **Plain Kubernetes:** See `backend/k8s-plain/README.md`
- **Helm Charts:** See `backend/helm/real-estate-backend/README.md`
- **Comprehensive Comparison:** See `/HELM_VS_PLAIN_K8S.md`

## 🤝 Recommendations

1. **If you're new to Kubernetes:**
   - Start with plain Kubernetes manifests
   - Get comfortable with kubectl commands
   - Understand the resources and their relationships
   - Then explore Helm

2. **If you're building a production application:**
   - Use Helm from the start
   - Leverage its configuration management
   - Benefit from the rollback capabilities
   - Use the ecosystem of charts

3. **If you're in between:**
   - Try plain Kubernetes with Kustomize
   - Get the benefits of environment management
   - Keep the simplicity of plain YAML
   - No templating complexity

## 🌟 Key Takeaways

1. **Plain Kubernetes is transparent** - What you see is what you get
2. **Helm is powerful** - More features, more complexity
3. **Both are valid** - Choose based on your needs
4. **Learn both** - Understanding both makes you a better Kubernetes user
5. **Start simple, grow as needed** - Don't use Helm if you don't need it

## 💡 Tips

- Use plain K8s for learning and understanding
- Use Helm for production and complex deployments
- Don't skip learning plain Kubernetes - it's the foundation
- Helm templates are just Kubernetes YAML with variables
- Both approaches deploy the same resources
- The difference is in how you manage them

## 🔗 Next Steps

After exploring both approaches:
1. Read the detailed comparison: `/HELM_VS_PLAIN_K8S.md`
2. Practice with the hands-on exercises
3. Try deploying to different environments
4. Experiment with making changes
5. Choose the approach that fits your needs

Happy learning! 🎉
