# Multi-stage build for Tanoshii

# Stage 1: Build the application
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Copy gradle files
COPY gradle gradle
COPY gradlew .
COPY gradle.properties .
COPY settings.gradle.kts .
COPY build.gradle.kts .

# Download dependencies
RUN ./gradlew dependencies --no-daemon

# Copy source code
COPY src src

# Build the application
RUN ./gradlew build -x test --no-daemon

# Stage 2: Runtime image
FROM eclipse-temurin:17-jre-jammy

# Install Python and required packages for Edge TTS and Whisper
RUN apt-get update && apt-get install -y \
    python3 \
    python3-pip \
    python3-venv \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy built application
COPY --from=builder /app/build/libs/*.jar app.jar

# Copy Python scripts
COPY scripts scripts

# Setup Python virtual environment and install dependencies
RUN python3 -m venv /app/.venv && \
    /app/.venv/bin/pip install --no-cache-dir -r scripts/requirements.txt

# Create directory for database and audio cache
RUN mkdir -p /app/data /app/audio-cache

# Set environment variables
ENV JAVA_OPTS="-Xmx512m -Xms256m"
ENV PATH="/app/.venv/bin:${PATH}"

# Expose application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/ || exit 1

# Run the application
CMD ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
