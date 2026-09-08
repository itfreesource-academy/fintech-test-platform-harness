# Multi-stage Dockerfile for containerized FinTech test harness execution
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy sources and compile
COPY src ./src
RUN mvn clean test-compile -B

# Execution stage
FROM maven:3.9.6-eclipse-temurin-17
WORKDIR /app

COPY --from=build /root/.m2 /root/.m2
COPY --from=build /app /app

ENTRYPOINT ["mvn", "test", "--batch-mode"]
