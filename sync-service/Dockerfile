FROM eclipse-temurin:21-jre-alpine
ADD target/sync-service.jar /sync-service.jar
EXPOSE 8282
ENTRYPOINT ["java", "-jar", "sync-service.jar"]
