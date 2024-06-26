# Step 1: Build the application
FROM maven:3.8.6-openjdk-11 AS build

WORKDIR /app

# Copy the pom.xml and fetch all dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the application
COPY src/ /app/src/
RUN mvn clean package

# Step 2: Use a smaller base image to run the app
FROM openjdk:11-jre-slim

WORKDIR /app

# This ensures the DATABASE_URL can be injected during the build phase
ARG DATABASE_URL

# This sets the DATABASE_URL as an environment variable in the running container
ENV DATABASE_URL=$DATABASE_URL

# Copy the built JAR from the build stage
COPY --from=build /app/target/TokenBot_1_maven-1.0-SNAPSHOT-jar-with-dependencies.jar /app/

# Command to run the application
CMD ["java", "-jar", "/app/TokenBot_1_maven-1.0-SNAPSHOT-jar-with-dependencies.jar"]