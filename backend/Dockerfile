# Build stage
FROM maven:3.8.5-openjdk-8 AS build

WORKDIR /build

COPY backend/pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

COPY backend/src ./src
RUN mvn -q -DskipTests package

# Run stage
FROM eclipse-temurin:8-jdk

WORKDIR /app

COPY --from=build /build/target/apiDocBackendApis.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
