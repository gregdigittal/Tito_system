FROM eclipse-temurin:21-jre-alpine
ADD target/emola-service.jar /emola-service.jar
EXPOSE 8290
ENTRYPOINT ["java", "-jar", "emola-service.jar"]
