#!/bin/bash

# Jenkins Setup Script for Real Estate Backend CI/CD
# This script helps set up Jenkins with all required plugins and configurations

set -e

echo "=========================================="
echo "Jenkins Setup for Real Estate Backend"
echo "=========================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Jenkins is running
check_jenkins() {
    echo "Checking Jenkins..."
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|403"; then
        echo -e "${GREEN}✓${NC} Jenkins is running"
        return 0
    else
        echo -e "${RED}✗${NC} Jenkins is not running"
        return 1
    fi
}

# Install required Jenkins plugins
install_plugins() {
    echo ""
    echo "=========================================="
    echo "Installing Required Jenkins Plugins"
    echo "=========================================="

    PLUGINS=(
        "git"
        "github"
        "workflow-aggregator"          # Pipeline
        "docker-workflow"
        "kubernetes"
        "kubernetes-cli"
        "aws-credentials"
        "amazon-ecr"
        "slack"
        "email-ext"
        "sonar"
        "owasp-dependency-check"
        "junit"
        "htmlpublisher"
        "ansicolor"
        "timestamper"
        "build-timeout"
        "credentials-binding"
        "ws-cleanup"
    )

    echo "Required plugins:"
    for plugin in "${PLUGINS[@]}"; do
        echo "  - $plugin"
    done

    echo ""
    read -p "Install these plugins via Jenkins CLI? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        for plugin in "${PLUGINS[@]}"; do
            echo "Installing $plugin..."
            java -jar jenkins-cli.jar -s http://localhost:8080/ install-plugin "$plugin" || true
        done

        echo ""
        echo -e "${GREEN}✓${NC} Plugins installation completed"
        echo "Please restart Jenkins to activate plugins"
    fi
}

# Configure AWS credentials
configure_aws() {
    echo ""
    echo "=========================================="
    echo "AWS Configuration"
    echo "=========================================="
    echo ""
    echo "You need to configure AWS credentials in Jenkins:"
    echo "1. Go to: Manage Jenkins → Manage Credentials"
    echo "2. Click on 'global' → 'Add Credentials'"
    echo "3. Add the following credentials:"
    echo ""
    echo "   a) AWS Credentials (for ECR and EKS)"
    echo "      - Kind: AWS Credentials"
    echo "      - ID: aws-credentials"
    echo "      - Access Key ID: [Your AWS Access Key]"
    echo "      - Secret Access Key: [Your AWS Secret Key]"
    echo ""
    echo "   b) AWS Account ID"
    echo "      - Kind: Secret text"
    echo "      - ID: aws-account-id"
    echo "      - Secret: [Your 12-digit AWS Account ID]"
    echo ""
}

# Configure Slack
configure_slack() {
    echo ""
    echo "=========================================="
    echo "Slack Configuration (Optional)"
    echo "=========================================="
    echo ""
    echo "To enable Slack notifications:"
    echo "1. Create a Slack Incoming Webhook"
    echo "   - Go to: https://api.slack.com/messaging/webhooks"
    echo "   - Create webhook for your channel"
    echo ""
    echo "2. Add webhook URL to Jenkins credentials:"
    echo "   - Go to: Manage Jenkins → Manage Credentials"
    echo "   - Kind: Secret text"
    echo "   - ID: slack-webhook-url"
    echo "   - Secret: [Your Slack Webhook URL]"
    echo ""
}

# Configure SonarQube
configure_sonarqube() {
    echo ""
    echo "=========================================="
    echo "SonarQube Configuration (Optional)"
    echo "=========================================="
    echo ""
    echo "To enable code quality scanning:"
    echo "1. Install and run SonarQube"
    echo "   - Docker: docker run -d --name sonarqube -p 9000:9000 sonarqube:lts"
    echo "   - Or use SonarCloud: https://sonarcloud.io/"
    echo ""
    echo "2. Create authentication token in SonarQube"
    echo "   - Go to: My Account → Security → Generate Tokens"
    echo ""
    echo "3. Add to Jenkins:"
    echo "   - Go to: Manage Jenkins → Configure System"
    echo "   - Find 'SonarQube servers'"
    echo "   - Add server: Name: SonarQube, URL: http://localhost:9000"
    echo ""
    echo "4. Add token to Jenkins credentials:"
    echo "   - Kind: Secret text"
    echo "   - ID: sonarqube-token"
    echo "   - Secret: [Your SonarQube Token]"
    echo ""
}

# Install AWS CLI
install_aws_cli() {
    echo ""
    echo "=========================================="
    echo "Installing AWS CLI"
    echo "=========================================="

    if command -v aws &> /dev/null; then
        echo -e "${GREEN}✓${NC} AWS CLI already installed"
        aws --version
    else
        echo "Installing AWS CLI..."
        curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
        unzip awscliv2.zip
        sudo ./aws/install
        rm -rf aws awscliv2.zip
        echo -e "${GREEN}✓${NC} AWS CLI installed"
    fi
}

# Install kubectl
install_kubectl() {
    echo ""
    echo "=========================================="
    echo "Installing kubectl"
    echo "=========================================="

    if command -v kubectl &> /dev/null; then
        echo -e "${GREEN}✓${NC} kubectl already installed"
        kubectl version --client
    else
        echo "Installing kubectl..."
        curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
        sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
        rm kubectl
        echo -e "${GREEN}✓${NC} kubectl installed"
    fi
}

# Install Helm
install_helm() {
    echo ""
    echo "=========================================="
    echo "Installing Helm"
    echo "=========================================="

    if command -v helm &> /dev/null; then
        echo -e "${GREEN}✓${NC} Helm already installed"
        helm version
    else
        echo "Installing Helm..."
        curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash
        echo -e "${GREEN}✓${NC} Helm installed"
    fi
}

# Install Trivy
install_trivy() {
    echo ""
    echo "=========================================="
    echo "Installing Trivy (Security Scanner)"
    echo "=========================================="

    if command -v trivy &> /dev/null; then
        echo -e "${GREEN}✓${NC} Trivy already installed"
        trivy --version
    else
        echo "Installing Trivy..."
        sudo apt-get install wget apt-transport-https gnupg lsb-release -y
        wget -qO - https://aquasecurity.github.io/trivy-repo/deb/public.key | sudo apt-key add -
        echo "deb https://aquasecurity.github.io/trivy-repo/deb $(lsb_release -sc) main" | sudo tee -a /etc/apt/sources.list.d/trivy.list
        sudo apt-get update
        sudo apt-get install trivy -y
        echo -e "${GREEN}✓${NC} Trivy installed"
    fi
}

# Create Jenkins pipeline
create_pipeline() {
    echo ""
    echo "=========================================="
    echo "Creating Jenkins Pipeline"
    echo "=========================================="
    echo ""
    echo "To create the pipeline in Jenkins:"
    echo "1. Go to Jenkins Dashboard"
    echo "2. Click 'New Item'"
    echo "3. Enter name: 'real-estate-backend'"
    echo "4. Select 'Pipeline'"
    echo "5. In Pipeline section:"
    echo "   - Definition: Pipeline script from SCM"
    echo "   - SCM: Git"
    echo "   - Repository URL: [Your Git repository]"
    echo "   - Script Path: Jenkinsfile"
    echo "6. Save and run!"
    echo ""
}

# Display summary
display_summary() {
    echo ""
    echo "=========================================="
    echo "Setup Summary"
    echo "=========================================="
    echo ""
    echo "Next steps:"
    echo "1. Configure AWS credentials in Jenkins"
    echo "2. Configure Slack webhook (optional)"
    echo "3. Configure SonarQube (optional)"
    echo "4. Create ECR repository:"
    echo "   aws ecr create-repository --repository-name real-estate-backend"
    echo ""
    echo "5. Create EKS cluster (if not exists):"
    echo "   eksctl create cluster --name real-estate-cluster --region us-east-1"
    echo ""
    echo "6. Deploy RDS with Terraform:"
    echo "   cd terraform/environments/dev && terraform apply"
    echo ""
    echo "7. Create Jenkins pipeline"
    echo ""
    echo "For detailed instructions, see: jenkins/README.md"
    echo ""
}

# Main execution
main() {
    check_jenkins

    echo ""
    read -p "Install Jenkins plugins? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        install_plugins
    fi

    echo ""
    read -p "Install required CLI tools? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        install_aws_cli
        install_kubectl
        install_helm
        install_trivy
    fi

    configure_aws
    configure_slack
    configure_sonarqube
    create_pipeline
    display_summary

    echo -e "${GREEN}Setup completed!${NC}"
}

# Run main function
main
