# FROM openjdk:21-jdk-slim
#
# WORKDIR /app
#
# COPY target/*.jar app.jar
#
# EXPOSE 8090
#
# ENTRYPOINT ["java", "-jar", "app.jar"]



# Stage 1: Build the application
# Use a full JDK image with a package manager and build tools for building the application.
# The `AS builder` alias gives this stage a name, so we can reference it later.
FROM eclipse-temurin:21-jdk-jammy AS builder

# Set the working directory inside the container for this stage.
WORKDIR /app

# Copy the Maven build files first to leverage Docker's build cache.
# This ensures that dependencies are only downloaded when the pom.xml changes.
COPY pom.xml .

# Download dependencies. This is a separate step to improve caching.
RUN mvn dependency:go-offline -B

# Copy the source code.
COPY src ./src

# Build the Spring Boot application, skipping tests for a faster build.
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Create the final production image
# Use a lean JRE image, which is much smaller than a JDK image.
FROM eclipse-temurin:21-jre-jammy

# Create a non-root user and group to run the application.
# Running as a non-root user is a security best practice to prevent privilege escalation.
RUN addgroup --system appgroup && adduser --system --group appgroup appuser

# Set the working directory for the final image.
WORKDIR /app

# Copy only the compiled JAR file from the "builder" stage.
# The --from=builder flag is the key to multi-stage builds.
COPY --from=builder --chown=appuser:appgroup /app/target/*.jar app.jar

# Expose the application's port.
EXPOSE 8090

# Switch to the non-root user.
USER appuser

# Define the command to run the application when the container starts.
ENTRYPOINT ["java", "-jar", "app.jar"]
