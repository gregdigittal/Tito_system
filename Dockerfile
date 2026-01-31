# Build stage: Maven builds the api-service JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw -pl api-service -am -DskipTests -q package -B

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/api-service/target/api-service-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=dev-k8s,tidb-cloud"]
