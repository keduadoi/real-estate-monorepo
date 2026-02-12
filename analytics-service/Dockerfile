# Multi-stage build for analytics-service
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre

RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/*

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
