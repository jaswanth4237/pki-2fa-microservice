# ============================
# 1. Build Stage
# ============================
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

# Copy all source files
COPY . .

# Compile Java files
RUN javac -cp "nanohttpd-2.3.1.jar:gson-2.10.1.jar:bcprov-jdk15on-1.70.jar" *.java

# ============================
# 2. Runtime Stage
# ============================
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install cron
RUN apt-get update && apt-get install -y cron tzdata && rm -rf /var/lib/apt/lists/*

# Create persistent directories
RUN mkdir -p /data && mkdir -p /cron

# Copy compiled classes
COPY --from=builder /app/*.class /app/

# Copy required JARs
COPY nanohttpd-2.3.1.jar .
COPY gson-2.10.1.jar .
COPY bcprov-jdk15on-1.70.jar .

# Copy key files
COPY student_private.pem .
COPY student_public.pem .
COPY instructor_public.pem .

# Copy cron job
COPY cron/2fa-cron /etc/cron.d/2fa-cron
RUN chmod 0644 /etc/cron.d/2fa-cron
RUN crontab /etc/cron.d/2fa-cron

# ============================
# CMD — Start cron AND server
# ============================
CMD /usr/sbin/cron && \
    java -cp ".:nanohttpd-2.3.1.jar:gson-2.10.1.jar:bcprov-jdk15on-1.70.jar" Server
