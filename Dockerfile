# ==============================================================================
# Multi-Stage Dockerfile for DentalCare Clinic Spring Boot Application
# Base Image: Eclipse Temurin OpenJDK 17
# ==============================================================================

# ------------------------------------------------------------------------------
# Stage 1: Build Application Artifact
# ------------------------------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /workspace

# 1. Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B || true

# 2. Copy source code and compile fat JAR
COPY src ./src
RUN mvn clean package -DskipTests

# ------------------------------------------------------------------------------
# Stage 2: Runtime Production Container
# ------------------------------------------------------------------------------
FROM eclipse-temurin:17-jre

LABEL maintainer="DentalCare DevOps Team <devops@dentalcare.vn>"
LABEL description="DentalCare Clinic Enterprise Management System"

WORKDIR /app

# 1. Install curl for healthchecks and diagnostics
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 2. Create persistent directories for H2 database and media uploads
RUN mkdir -p /app/data /app/uploads/dental-images

# 3. Security Hardening: Create non-root user (UID 10001)
RUN groupadd -r -g 10001 dentalcare \
    && useradd -r -u 10001 -g dentalcare -d /app dentalcare \
    && chown -R dentalcare:dentalcare /app

# Switch to non-root user
USER dentalcare:dentalcare

# 4. Copy executable JAR from builder stage
COPY --from=builder --chown=dentalcare:dentalcare /workspace/target/*.jar /app/app.jar

# 5. Environment & JVM Configuration
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=default \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

EXPOSE 8080

# 6. Container Healthcheck via Spring Boot Actuator
HEALTHCHECK --interval=10s --timeout=5s --start-period=25s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 7. Optimized Entrypoint with shell variable expansion
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT} -jar /app/app.jar"]
