#!/bin/sh
set -e

# If a base64-encoded truststore (JKS) is provided, write it to disk before startup.
# This is the safest way to ship a TiDB Cloud CA cert without committing it to Git.
if [ -n "$TRUSTSTORE_BASE64" ]; then
    echo "Writing truststore from TRUSTSTORE_BASE64..."
    echo "$TRUSTSTORE_BASE64" | base64 -d > /app/truststore.jks
fi

exec java -jar /app/app.jar
