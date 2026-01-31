FROM eclipse-temurin:21-jre-alpine
ADD target/ledger-service.jar /ledger-service.jar
EXPOSE 8284
ENTRYPOINT ["java", "-jar", "ledger-service.jar"]
