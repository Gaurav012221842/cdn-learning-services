FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY learning-common ./learning-common
RUN cd learning-common && mvn clean install -DskipTests
COPY user-service ./user-service
WORKDIR /app/user-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/user-service/target/user-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
