# Build stage: Maven builds the api-service JAR (use Maven image; repo has no .mvn/wrapper)
FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

COPY . .

RUN mvn -pl api-service -am -DskipTests -q package -B

# Run stage: use non-Alpine JRE for MongoDB Atlas TLS (Alpine can cause "Received fatal alert: internal_error")
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/api-service/target/api-service.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=dev-k8s,tidb-cloud"]
