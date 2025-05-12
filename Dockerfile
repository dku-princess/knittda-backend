# Base image
FROM openjdk:21-jdk

# App directory
WORKDIR /app

# Copy the JAR file
COPY build/libs/*SNAPSHOT.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
