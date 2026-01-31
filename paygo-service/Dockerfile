FROM eclipse-temurin:21-jre-alpine
ADD target/paygo-service.jar /paygo-service.jar
EXPOSE 8285
ENTRYPOINT ["java", "-jar", "paygo-service.jar"]
