# Multi-stage build for price-service
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy grpc-proto dependency first
COPY grpc-proto/pom.xml ./grpc-proto/pom.xml
COPY grpc-proto/src ./grpc-proto/src
RUN cd grpc-proto && mvn clean install -DskipTests -B

# Copy pom.xml
COPY price-service/pom.xml ./price-service/pom.xml

# Download dependencies (cached layer)
RUN cd price-service && mvn dependency:go-offline -B

# Copy source code
COPY price-service/src ./price-service/src

# Build the application
RUN cd price-service && mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre

# Add non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

WORKDIR /app

# Copy the built jar
COPY --from=build /app/price-service/target/*.jar app.jar

# Expose REST and gRPC ports
EXPOSE 8084 9090

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8084/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
