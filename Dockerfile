# Build stage: Java 21 (Maven + Eclipse Temurin 21). Required for MongoDB Atlas TLS on Render.
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY . .

RUN mvn -pl api-service -am -DskipTests -q package -B

# Run stage: Java 21 JRE, non-Alpine for MongoDB Atlas TLS (Alpine can cause "Received fatal alert: internal_error")
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/api-service/target/api-service.jar app.jar

# api-service listens on 8281 (server.port in application.yml)
EXPOSE 8281

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=dev-k8s,tidb-cloud"]
