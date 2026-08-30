#!/bin/sh
set -e

# If a base64-encoded truststore (JKS) is provided, write it to disk before startup.
# -i ignores any non-base64 characters (line breaks, BOM, spaces, etc.)
if [ -n "$TRUSTSTORE_BASE64" ]; then
    echo "Writing truststore from TRUSTSTORE_BASE64..."
    printf '%s' "$TRUSTSTORE_BASE64" | tr -d '\r\n' | base64 -di > /app/truststore.jks
fi

exec java -jar /app/app.jar
