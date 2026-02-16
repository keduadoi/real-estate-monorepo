# Multi-stage build for Spring Boot application

# Stage 1: Build the application
FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

# Copy Maven files for dependency caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY mvnw.cmd .

# Copy grpc-proto jar (local dependency not in Maven Central)
COPY grpc-proto-0.0.1-SNAPSHOT.jar /tmp/grpc-proto.jar
RUN mvn install:install-file -Dfile=/tmp/grpc-proto.jar \
    -DgroupId=com.realestate -DartifactId=grpc-proto \
    -Dversion=0.0.1-SNAPSHOT -Dpackaging=jar -q

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:17-jre

WORKDIR /app

# Create a non-root user to run the application
RUN groupadd -r spring && useradd -r -g spring spring

# Create uploads directory and set permissions
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# Copy the built JAR from the build stage
COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring:spring

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
