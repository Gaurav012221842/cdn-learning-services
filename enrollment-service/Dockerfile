FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY learning-common ./learning-common
RUN cd learning-common && mvn clean install -DskipTests
COPY enrollment-service ./enrollment-service
WORKDIR /app/enrollment-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/enrollment-service/target/enrollment-service-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "app.jar"]
