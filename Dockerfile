# ── Stage 1: Build ────────────────────────────────────────────────────────────
# Use Eclipse Temurin JDK 17 to build the JAR
# Multi-stage build — builder image is discarded after compilation
# Final image only contains the JRE and the JAR — no build tools
FROM eclipse-temurin:17-jdk-alpine AS builder

# Set working directory inside the container
WORKDIR /app

# Copy Maven wrapper and pom.xml first — Docker caches this layer
# If only source code changes, Maven dependencies are NOT re-downloaded
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies — cached as a separate layer
# This layer only rebuilds when pom.xml changes
RUN ./mvnw dependency:resolve -q

# Copy source code — this layer rebuilds when code changes
COPY src src

# Build the JAR — skip tests for faster Docker build
RUN ./mvnw clean package -DskipTests -q

# ── Stage 2: Runtime ──────────────────────────────────────────────────────────
# Use JRE only — smaller image, no compiler or build tools
# alpine = minimal Linux base — reduces image size significantly
FROM eclipse-temurin:17-jre-alpine

# Create non-root user — security best practice
# Running as root inside containers is a security risk
RUN addgroup -S logshield && adduser -S logshield -G logshield

# Set working directory
WORKDIR /app

# Create data directory for log persistence
# Owned by logshield user — not root
RUN mkdir -p data && chown logshield:logshield data

# Copy only the JAR from the builder stage — not the entire build environment
COPY --from=builder /app/target/logshield-v2-0.0.1-SNAPSHOT.jar app.jar

# Switch to non-root user
USER logshield

# Expose port 8080 — documents which port the container listens on
# Does not publish the port — docker run -p 8080:8080 does that
EXPOSE 8080

# Health check — Docker monitors this every 30 seconds
# If the endpoint fails 3 times the container is marked unhealthy
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# JVM tuning for containerized environments
# -XX:+UseContainerSupport — JVM reads container CPU/memory limits not host
# -XX:MaxRAMPercentage=75.0 — use 75% of container memory for heap
# -Djava.security.egd — faster startup by using /dev/urandom for randomness
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]