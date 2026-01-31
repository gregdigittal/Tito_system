FROM eclipse-temurin:21-jre-alpine
ADD target/fee-service.jar /fee-service.jar
EXPOSE 8283
ENTRYPOINT ["java", "-jar", "fee-service.jar"]
