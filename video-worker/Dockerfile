FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY learning-common ./learning-common
RUN cd learning-common && mvn clean install -DskipTests
COPY video-worker ./video-worker
WORKDIR /app/video-worker
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /app/video-worker/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
