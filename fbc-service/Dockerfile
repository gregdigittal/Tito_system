FROM eclipse-temurin:21-jre-alpine
ADD target/fbc-service.jar /fbc-service.jar
EXPOSE 8288
ENTRYPOINT ["java", "-jar", "fbc-service.jar"]
