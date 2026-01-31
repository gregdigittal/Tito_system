FROM eclipse-temurin:21-jre-alpine
ADD target/zim-api-service.jar /zim-api-service.jar
EXPOSE 8292
ENTRYPOINT ["java", "-jar", "zim-api-service.jar"]
