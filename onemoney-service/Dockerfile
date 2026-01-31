FROM eclipse-temurin:21-jre-alpine
ADD target/onemoney-service.jar /onemoney-service.jar
EXPOSE 8287
ENTRYPOINT ["java", "-jar", "onemoney-service.jar"]
