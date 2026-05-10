# Multi-stage build for ai-search-service
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY ai-search-service/pom.xml ./ai-search-service/pom.xml

RUN cd ai-search-service && mvn dependency:go-offline -B

COPY ai-search-service/src ./ai-search-service/src

RUN cd ai-search-service && mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

WORKDIR /app

COPY --from=build /app/ai-search-service/target/*.jar app.jar

EXPOSE 8086

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8086/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
