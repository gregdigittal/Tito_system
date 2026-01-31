FROM eclipse-temurin:21-jre-alpine
ADD target/ecocash-service.jar /ecocash-service.jar
EXPOSE 8286
ENTRYPOINT ["java", "-jar", "ecocash-service.jar"]
