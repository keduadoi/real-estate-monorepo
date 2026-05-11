# Multi-stage build for comment-service
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

COPY comment-service/pom.xml ./comment-service/pom.xml
RUN cd comment-service && mvn dependency:go-offline -B

COPY comment-service/src ./comment-service/src
RUN cd comment-service && mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring
WORKDIR /app

COPY --from=build /app/comment-service/target/*.jar app.jar

EXPOSE 8087

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8087/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
