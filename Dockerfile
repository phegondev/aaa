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
# Changed from alpine to jammy
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Install Maven (apt for Debian/Ubuntu)
# Add --no-install-recommends to keep image size down
RUN apt-get update && apt-get install -y --no-install-recommends maven && rm -rf /var/lib/apt/lists/*

# Copy the Maven build files (pom.xml) first to leverage Docker cache
COPY pom.xml .

# Download dependencies - separate step to cache dependencies
RUN mvn dependency:go-offline -B

# Copy source code.
COPY src ./src

# Build the Spring Boot application
RUN mvn clean package -Dmaven.test.skip=true

# Stage 2: Create the final production image
# Changed from alpine to jammy
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# No need to install libstdc++ or glibc compatibility, they are included by default
# in glibc-based distributions like Ubuntu/Debian (Jammy Jellyfish).

# Copy the JAR from the 'builder' stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8085

# Run the jar
ENTRYPOINT ["java","-jar","app.jar"]
