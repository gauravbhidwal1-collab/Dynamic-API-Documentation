
# Use Java 8 base image
FROM openjdk:8-jdk-alpine

# Set working directory
WORKDIR /app

# Copy jar file
COPY target/apiDocBackendApis.jar app.jar

# Expose port
EXPOSE 8080

# Run application
ENTRYPOINT ["java","-jar","/app/app.jar"]
