#!/bin/sh
set -e

# Activate the production profile unless explicitly overridden.
# This ensures the Docker image always runs with prod configuration
# while local `mvn spring-boot:run` uses the dev profile (default in application.properties).
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-prod}

# If a base64-encoded truststore (JKS) is provided, write it to disk before startup.
# -i ignores any non-base64 characters (line breaks, BOM, spaces, etc.)
if [ -n "$TRUSTSTORE_BASE64" ]; then
    echo "Writing truststore from TRUSTSTORE_BASE64..."
    printf '%s' "$TRUSTSTORE_BASE64" | tr -d '\r\n' | base64 -di > /app/truststore.jks
fi

exec java -jar /app/app.jar
