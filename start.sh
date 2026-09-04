#!/bin/bash

# AWS Elastic Beanstalk Java SE startup script
# The app is extracted to /var/app/current/ by EB

APP_DIR=/var/app/current
JAR_NAME=swari-sewa-backend-0.0.1-SNAPSHOT.jar

echo "=== Swari Sadhan startup ==="
echo "APP_DIR: $APP_DIR"
echo "JAR: $JAR_NAME"

# Verify JAR exists
if [ ! -f "$APP_DIR/$JAR_NAME" ]; then
    echo "ERROR: JAR not found at $APP_DIR/$JAR_NAME"
    echo "Contents of $APP_DIR:"
    ls -la "$APP_DIR" 2>&1
    exit 1
fi

# Decode truststore from base64 env var if provided
if [ -n "$TRUSTSTORE_BASE64" ]; then
    echo "Writing truststore from TRUSTSTORE_BASE64..."
    printf '%s' "$TRUSTSTORE_BASE64" | tr -d '\r\n' | base64 -di > /tmp/truststore.jks 2>&1
    if [ $? -eq 0 ]; then
        export TRUSTSTORE_PATH=/tmp/truststore.jks
        echo "Truststore written to /tmp/truststore.jks"
    else
        echo "WARNING: Failed to decode TRUSTSTORE_BASE64, continuing without truststore"
    fi
else
    echo "TRUSTSTORE_BASE64 not set, skipping truststore"
fi

echo "SPRING_PROFILES_ACTIVE: $SPRING_PROFILES_ACTIVE"
echo "PORT: $PORT"
echo "Starting Java application..."

# Start Spring Boot — redirect stderr to stdout so all output goes to web.stdout.log
cd "$APP_DIR"
exec java -jar "$JAR_NAME" 2>&1
