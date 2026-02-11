# Multi-stage build: Java 21 for MongoDB Atlas TLS compatibility on Render.
# In Render: set "Dockerfile Path" to api-service/Dockerfile. Build context = repo root (default).
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

COPY . .
# Build only api-service and its dependencies (common, common-sqldb).
RUN apk add --no-cache maven && \
    mvn -B -pl api-service -am package -DskipTests -q && \
    cp api-service/target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/app.jar app.jar
EXPOSE 8281
ENTRYPOINT ["java", "-jar", "app.jar"]
