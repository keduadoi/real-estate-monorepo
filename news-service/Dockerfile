# Multi-stage build for news-service
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml
COPY news-service/pom.xml ./news-service/pom.xml

# Download dependencies (cached layer)
RUN cd news-service && mvn dependency:go-offline -B

# Copy source code
COPY news-service/src ./news-service/src

# Build the application
RUN cd news-service && mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre

# Add non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

WORKDIR /app

# Copy the built jar
COPY --from=build /app/news-service/target/*.jar app.jar

# Expose REST port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8085/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
