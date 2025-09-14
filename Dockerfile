FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/hydrogarden-backend.jar app.jar
VOLUME /app/logs
ENTRYPOINT ["java", "-jar", "app.jar"]

