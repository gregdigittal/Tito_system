FROM eclipse-temurin:21-jre-alpine
WORKDIR /home/app
COPY classes /home/app/classes
COPY dependency/* /home/app/libs/
EXPOSE 8293
ENTRYPOINT ["java", "-cp", "/home/app/libs/*:/home/app/classes/", "cash.ice.posb.PosbApplication"]
