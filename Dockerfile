# Use an OpenJDK runtime as a base image
FROM openjdk:17-jdk-alpine

# Set the working directory
WORKDIR /app

# Copy the executable JAR file from the target directory to the container
COPY target/TuringSec-0.0.1-SNAPSHOT.jar app.jar

# Expose the port your application runs on
EXPOSE 5000

# Specify the command to run on container startup
CMD ["java", "-jar", "app.jar"]