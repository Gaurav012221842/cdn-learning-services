FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY learning-common ./learning-common
RUN cd learning-common && mvn clean install -DskipTests
COPY course-service ./course-service
WORKDIR /app/course-service
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/course-service/target/course-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
