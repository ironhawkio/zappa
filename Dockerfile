# Multi-stage build for optimized image
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Gradle wrapper and Gradle build files
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle/ gradle/

# Make gradlew executable
RUN chmod +x gradlew

# Copy source code
COPY src/ src/

# Build the application
RUN ./gradlew build -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Install curl for health checks
RUN apk add --no-cache curl

# Create non-root user for security (Alpine syntax)
RUN addgroup -g 1000 appuser && adduser -u 1000 -G appuser -s /bin/sh -D appuser

# Set working directory
WORKDIR /app

# Copy built JAR from builder stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Change ownership to appuser
RUN chown appuser:appuser app.jar

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 6886

# JVM tuning for containers
# -XX:MaxRAMPercentage=75.0 -> Use maximum 75% of whatever memory limit Docker gives the container for heap memory. Leaves 20% for  non-heap memory usage (caches, direct buffers, etc.)
# -XX:+UseContainerSuppor -> Asks Java uses container limits instead of host machine limits for memory/thread allocations
# -XX:+UseG1G ->  Modern Garbage Collector that's better for containers with limited memory
# -XX:+HeapDumpOnOutOfMemoryError ->  Creates heap dump when app runs out of memory. Essential for troubleshooting memory leaks in production
# -XX:HeapDumpPath=/tmp/heapdump.hprof ->  Heap dump location that can be mounted volume for persistence
# Djava.security.egd=file:/dev/./urandom ->  Uses non-blocking random number generation to prevent startup delays waiting for entropy
ENV JAVA_OPTS="-XX:+UseContainerSupport \
      -XX:MaxRAMPercentage=75.0 \
      -XX:+UseG1GC \
      -XX:+HeapDumpOnOutOfMemoryError \
      -XX:HeapDumpPath=/tmp/heapdump.hprof \
      -Djava.security.egd=file:/dev/./urandom"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]