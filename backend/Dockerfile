# Build stage
FROM maven:3.8.5-openjdk-8 AS build

WORKDIR /app

COPY . .

RUN mvn clean package -DskipTests

# Run stage
FROM openjdk:8-jdk-alpine

WORKDIR /app

COPY --from=build /app/target/apiDocBackendApis.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
