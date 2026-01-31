FROM eclipse-temurin:21-jre-alpine
ADD target/api-service.jar /api-service.jar
EXPOSE 8281
ENTRYPOINT ["java", "-jar", "api-service.jar"]
