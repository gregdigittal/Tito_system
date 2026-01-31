FROM eclipse-temurin:21-jre-alpine
ADD target/mpesa-service.jar /mpesa-service.jar
EXPOSE 8289
ENTRYPOINT ["java", "-jar", "mpesa-service.jar"]
