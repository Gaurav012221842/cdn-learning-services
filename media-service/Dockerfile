FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY learning-common ./learning-common
RUN cd learning-common && mvn clean install -DskipTests
COPY media-service ./media-service
WORKDIR /app/media-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/media-service/target/media-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
