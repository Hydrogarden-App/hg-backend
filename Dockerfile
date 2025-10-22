FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/hydrogarden-backend.jar app.jar

ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "app.jar"]
