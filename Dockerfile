# Stage 1: Build
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

COPY . .

# Compile Java sources with all required JARs (include BouncyCastle)
RUN javac -cp "nanohttpd-2.3.1.jar:gson-2.10.1.jar:bcprov-jdk15on-1.70.jar" *.java

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-jammy

ENV TZ=UTC

WORKDIR /app

# Install cron (for cron jobs)
RUN apt-get update && apt-get install -y cron && apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy compiled classes and jars from builder
COPY --from=builder /app /app

# Ensure persistent directories exist
RUN mkdir -p /data /cron

# Copy cron configuration (must be LF line endings)
COPY cron/2fa-cron /etc/cron.d/2fa-cron
RUN chmod 0644 /etc/cron.d/2fa-cron && crontab /etc/cron.d/2fa-cron

# Expose API port
EXPOSE 8080

# Start cron and then run the Java server in foreground
CMD service cron start && java -cp ".:nanohttpd-2.3.1.jar:gson-2.10.1.jar:bcprov-jdk15on-1.70.jar" Server
