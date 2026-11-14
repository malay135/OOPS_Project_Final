# How the cloud (Render) builds and runs the backend.
# Stage 1 builds the .jar with Maven. Stage 2 runs it on a small Java 21 image.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run on our laptops (they need a local database), so they are skipped here
RUN mvn -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Cloud servers use UTC time. Our app shows India time, so set it here.
ENV TZ=Asia/Kolkata

# Render gives the port in the PORT variable (application.properties reads it)
EXPOSE 8080

# Memory and startup settings that fit the free plan (512 MB)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss512k", "-Duser.timezone=Asia/Kolkata", "-jar", "app.jar"]
