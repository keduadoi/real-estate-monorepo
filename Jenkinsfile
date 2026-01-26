// ⚠️ DEPRECATED: This combined CI/CD pipeline is deprecated
// Please use the separated approach instead:
//   - CI: Jenkinsfile.ci (automated build/test/publish)
//   - CD: Jenkinsfile.cd (manual deployment)
// See CI_CD_GUIDE.md for details
//
// Jenkins CI/CD Pipeline for Real Estate Backend
// Production-ready pipeline with build, test, security scan, and deployment to EKS

pipeline {
    agent any

    // Environment variables
    environment {
        // AWS Configuration
        AWS_REGION = 'us-east-1'
        AWS_ACCOUNT_ID = credentials('aws-account-id')
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        ECR_REPOSITORY = 'real-estate-backend'

        // EKS Configuration
        EKS_CLUSTER_NAME = 'real-estate-cluster'

        // Application Configuration
        APP_NAME = 'real-estate-backend'

        // Helm Configuration
        HELM_CHART_PATH = './backend/helm/real-estate-backend'

        // Docker Configuration
        DOCKER_BUILDKIT = '1'

        // Image tag based on Git commit
        IMAGE_TAG = "${env.GIT_COMMIT.take(8)}"
        FULL_IMAGE_NAME = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

        // Credentials
        AWS_CREDENTIALS = credentials('aws-credentials')
        SONAR_TOKEN = credentials('sonarqube-token')
        SLACK_WEBHOOK = credentials('slack-webhook-url')
    }

    // Parameters for manual deployment
    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'prod'],
            description: 'Deployment environment'
        )
        booleanParam(
            name: 'RUN_TESTS',
            defaultValue: true,
            description: 'Run unit and integration tests'
        )
        booleanParam(
            name: 'RUN_SECURITY_SCAN',
            defaultValue: true,
            description: 'Run security vulnerability scanning'
        )
        booleanParam(
            name: 'SKIP_DB_MIGRATION',
            defaultValue: false,
            description: 'Skip database migration (use with caution)'
        )
        booleanParam(
            name: 'DEPLOY_TO_EKS',
            defaultValue: true,
            description: 'Deploy to EKS cluster'
        )
    }

    // Build triggers
    triggers {
        // Poll SCM every 5 minutes (H/5 * * * *)
        pollSCM('H/5 * * * *')

        // GitHub webhook trigger (configure in GitHub)
        githubPush()
    }

    options {
        // Keep last 10 builds
        buildDiscarder(logRotator(numToKeepStr: '10'))

        // Timeout for entire pipeline
        timeout(time: 30, unit: 'MINUTES')

        // Disable concurrent builds
        disableConcurrentBuilds()

        // Timestamps in console output
        timestamps()

        // ANSI color output
        ansiColor('xterm')
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    echo "=========================================="
                    echo "Real Estate Backend CI/CD Pipeline"
                    echo "=========================================="
                    echo "Environment: ${params.ENVIRONMENT}"
                    echo "Git Branch: ${env.GIT_BRANCH}"
                    echo "Git Commit: ${env.GIT_COMMIT}"
                    echo "Image Tag: ${IMAGE_TAG}"
                    echo "Full Image: ${FULL_IMAGE_NAME}"
                    echo "=========================================="

                    // Send Slack notification
                    slackSend(
                        color: '#0099CC',
                        message: """
                            *Pipeline Started*
                            Environment: ${params.ENVIRONMENT}
                            Branch: ${env.GIT_BRANCH}
                            Commit: ${env.GIT_COMMIT.take(8)}
                            Build: ${env.BUILD_URL}
                        """.stripIndent()
                    )
                }
            }
        }

        stage('Checkout') {
            steps {
                echo "Checking out source code..."
                checkout scm

                // Display commit info
                sh '''
                    echo "Current commit:"
                    git log -1 --pretty=format:"%h - %an, %ar : %s"
                '''
            }
        }

        stage('Build Maven Project') {
            steps {
                dir('backend') {
                    echo "Building Spring Boot application..."
                    sh '''
                        ./mvnw clean package -DskipTests
                    '''
                }
            }
            post {
                success {
                    echo "Maven build completed successfully"
                    archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
                }
            }
        }

        stage('Run Tests') {
            when {
                expression { params.RUN_TESTS == true }
            }
            parallel {
                stage('Unit Tests') {
                    steps {
                        dir('backend') {
                            echo "Running unit tests..."
                            sh '''
                                ./mvnw test
                            '''
                        }
                    }
                    post {
                        always {
                            junit 'backend/target/surefire-reports/*.xml'
                            publishHTML(target: [
                                reportDir: 'backend/target/surefire-reports',
                                reportFiles: 'index.html',
                                reportName: 'Unit Test Report'
                            ])
                        }
                    }
                }

                stage('Integration Tests') {
                    steps {
                        dir('backend') {
                            echo "Running integration tests..."
                            sh '''
                                ./mvnw verify -DskipUnitTests=true
                            '''
                        }
                    }
                    post {
                        always {
                            junit 'backend/target/failsafe-reports/*.xml'
                        }
                    }
                }
            }
        }

        stage('Code Quality Analysis') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                dir('backend') {
                    echo "Running SonarQube analysis..."
                    withSonarQubeEnv('SonarQube') {
                        sh '''
                            ./mvnw sonar:sonar \
                                -Dsonar.projectKey=real-estate-backend \
                                -Dsonar.projectName="Real Estate Backend" \
                                -Dsonar.host.url=$SONAR_HOST_URL \
                                -Dsonar.login=$SONAR_TOKEN
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Security Scan') {
            when {
                expression { params.RUN_SECURITY_SCAN == true }
            }
            parallel {
                stage('OWASP Dependency Check') {
                    steps {
                        dir('backend') {
                            echo "Running OWASP Dependency Check..."
                            sh '''
                                ./mvnw org.owasp:dependency-check-maven:check
                            '''
                        }
                    }
                    post {
                        always {
                            publishHTML(target: [
                                reportDir: 'backend/target',
                                reportFiles: 'dependency-check-report.html',
                                reportName: 'OWASP Dependency Check'
                            ])
                        }
                    }
                }

                stage('Trivy Vulnerability Scan') {
                    steps {
                        echo "Running Trivy filesystem scan..."
                        sh '''
                            trivy fs --severity HIGH,CRITICAL \
                                --format json \
                                --output trivy-report.json \
                                ./backend
                        '''
                    }
                    post {
                        always {
                            archiveArtifacts artifacts: 'trivy-report.json', fingerprint: true
                        }
                    }
                }
            }
        }

        stage('Build Docker Image') {
            steps {
                dir('backend') {
                    echo "Building Docker image..."
                    sh """
                        docker build \
                            --build-arg BUILD_DATE=\$(date -u +'%Y-%m-%dT%H:%M:%SZ') \
                            --build-arg VCS_REF=${env.GIT_COMMIT} \
                            --build-arg VERSION=${IMAGE_TAG} \
                            -t ${FULL_IMAGE_NAME} \
                            -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest \
                            -t ${ECR_REGISTRY}/${ECR_REPOSITORY}:${params.ENVIRONMENT} \
                            .
                    """
                }
            }
        }

        stage('Scan Docker Image') {
            when {
                expression { params.RUN_SECURITY_SCAN == true }
            }
            steps {
                echo "Scanning Docker image for vulnerabilities..."
                sh """
                    trivy image --severity HIGH,CRITICAL \
                        --format json \
                        --output trivy-image-report.json \
                        ${FULL_IMAGE_NAME}
                """

                // Fail if critical vulnerabilities found (optional)
                sh """
                    trivy image --severity CRITICAL \
                        --exit-code 1 \
                        ${FULL_IMAGE_NAME}
                """
            }
            post {
                always {
                    archiveArtifacts artifacts: 'trivy-image-report.json', fingerprint: true
                }
            }
        }

        stage('Push to ECR') {
            steps {
                echo "Logging in to AWS ECR..."
                sh """
                    aws ecr get-login-password --region ${AWS_REGION} | \
                        docker login --username AWS --password-stdin ${ECR_REGISTRY}
                """

                echo "Pushing Docker image to ECR..."
                sh """
                    docker push ${FULL_IMAGE_NAME}
                    docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:latest
                    docker push ${ECR_REGISTRY}/${ECR_REPOSITORY}:${params.ENVIRONMENT}
                """
            }
        }

        stage('Database Migration') {
            when {
                expression {
                    params.DEPLOY_TO_EKS == true &&
                    params.SKIP_DB_MIGRATION == false
                }
            }
            steps {
                script {
                    echo "Running database migrations..."

                    // Get RDS endpoint from Terraform or Secrets Manager
                    def dbHost = sh(
                        script: "cd terraform/environments/${params.ENVIRONMENT} && terraform output -raw rds_address",
                        returnStdout: true
                    ).trim()

                    // Run Flyway migrations using Docker
                    sh """
                        docker run --rm \
                            -v \${PWD}/backend/src/main/resources/db/migration:/flyway/sql \
                            -e FLYWAY_URL=jdbc:postgresql://${dbHost}:5432/realestatedb \
                            -e FLYWAY_USER=postgres \
                            -e FLYWAY_PASSWORD=\$(aws secretsmanager get-secret-value --secret-id real-estate-db-password --query SecretString --output text | jq -r .password) \
                            flyway/flyway:latest migrate
                    """
                }
            }
        }

        stage('Update kubeconfig') {
            when {
                expression { params.DEPLOY_TO_EKS == true }
            }
            steps {
                echo "Updating kubeconfig for EKS cluster..."
                sh """
                    aws eks update-kubeconfig \
                        --region ${AWS_REGION} \
                        --name ${EKS_CLUSTER_NAME}
                """

                // Verify connection
                sh "kubectl cluster-info"
                sh "kubectl get nodes"
            }
        }

        stage('Deploy to EKS') {
            when {
                expression { params.DEPLOY_TO_EKS == true }
            }
            steps {
                script {
                    echo "Deploying to EKS with Helm..."

                    // Get RDS endpoint from Terraform
                    def dbHost = sh(
                        script: "cd terraform/environments/${params.ENVIRONMENT} && terraform output -raw rds_address 2>/dev/null || echo 'localhost'",
                        returnStdout: true
                    ).trim()

                    // Determine values file
                    def valuesFile = fileExists("${HELM_CHART_PATH}/values-${params.ENVIRONMENT}.yaml") ?
                        "-f ${HELM_CHART_PATH}/values-${params.ENVIRONMENT}.yaml" : ""

                    // Deploy with Helm
                    sh """
                        helm upgrade --install ${APP_NAME} ${HELM_CHART_PATH} \
                            --namespace ${params.ENVIRONMENT} \
                            --create-namespace \
                            ${valuesFile} \
                            --set backend.image.repository=${ECR_REGISTRY}/${ECR_REPOSITORY} \
                            --set backend.image.tag=${IMAGE_TAG} \
                            --set postgresql.enabled=false \
                            --set backend.env.DB_HOST=${dbHost} \
                            --set backend.env.SPRING_PROFILES_ACTIVE=${params.ENVIRONMENT} \
                            --wait \
                            --timeout 5m \
                            --atomic
                    """
                }
            }
        }

        stage('Smoke Tests') {
            when {
                expression { params.DEPLOY_TO_EKS == true }
            }
            steps {
                script {
                    echo "Running smoke tests..."

                    // Get service endpoint
                    def serviceUrl = sh(
                        script: """
                            kubectl get service ${APP_NAME}-backend \
                                -n ${params.ENVIRONMENT} \
                                -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
                        """,
                        returnStdout: true
                    ).trim()

                    // Wait for service to be ready
                    sleep(time: 30, unit: 'SECONDS')

                    // Health check
                    sh """
                        curl -f http://${serviceUrl}:8080/actuator/health || exit 1
                    """

                    echo "Smoke tests passed!"
                }
            }
        }

        stage('Performance Tests') {
            when {
                expression {
                    params.ENVIRONMENT == 'staging' &&
                    params.DEPLOY_TO_EKS == true
                }
            }
            steps {
                echo "Running performance tests with JMeter..."
                sh '''
                    jmeter -n -t backend/src/test/jmeter/load-test.jmx \
                        -l results.jtl \
                        -e -o performance-report
                '''
            }
            post {
                always {
                    publishHTML(target: [
                        reportDir: 'performance-report',
                        reportFiles: 'index.html',
                        reportName: 'Performance Test Report'
                    ])
                }
            }
        }
    }

    post {
        success {
            script {
                echo "Pipeline completed successfully!"

                slackSend(
                    color: 'good',
                    message: """
                        *Pipeline Succeeded* ✅
                        Environment: ${params.ENVIRONMENT}
                        Branch: ${env.GIT_BRANCH}
                        Commit: ${env.GIT_COMMIT.take(8)}
                        Image: ${IMAGE_TAG}
                        Duration: ${currentBuild.durationString}
                        Build: ${env.BUILD_URL}
                    """.stripIndent()
                )
            }
        }

        failure {
            script {
                echo "Pipeline failed!"

                slackSend(
                    color: 'danger',
                    message: """
                        *Pipeline Failed* ❌
                        Environment: ${params.ENVIRONMENT}
                        Branch: ${env.GIT_BRANCH}
                        Commit: ${env.GIT_COMMIT.take(8)}
                        Stage: ${env.STAGE_NAME}
                        Duration: ${currentBuild.durationString}
                        Build: ${env.BUILD_URL}
                    """.stripIndent()
                )

                // Send email notification
                emailext(
                    subject: "Pipeline Failed: ${env.JOB_NAME} - ${env.BUILD_NUMBER}",
                    body: """
                        Pipeline failed at stage: ${env.STAGE_NAME}

                        See details: ${env.BUILD_URL}
                    """,
                    to: "${env.CHANGE_AUTHOR_EMAIL}"
                )
            }
        }

        always {
            echo "Cleaning up..."

            // Clean up Docker images
            sh """
                docker rmi ${FULL_IMAGE_NAME} || true
                docker system prune -f || true
            """

            // Archive build info
            sh """
                cat > build-info.json <<EOF
{
    "build_number": "${env.BUILD_NUMBER}",
    "git_commit": "${env.GIT_COMMIT}",
    "git_branch": "${env.GIT_BRANCH}",
    "image_tag": "${IMAGE_TAG}",
    "environment": "${params.ENVIRONMENT}",
    "timestamp": "\$(date -u +%Y-%m-%dT%H:%M:%SZ)"
}
EOF
            """

            archiveArtifacts artifacts: 'build-info.json', fingerprint: true

            // Clean workspace
            cleanWs()
        }
    }
}
