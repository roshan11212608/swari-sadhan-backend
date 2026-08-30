# ---- Build stage ----
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app

# Cache Maven dependencies by copying only pom.xml first
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw mvnw
COPY mvnw.cmd mvnw.cmd
COPY src src

# Build the production JAR (skip tests; they need a real DB)
RUN chmod +x mvnw && ./mvnw clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the generated executable JAR and the startup script
COPY --from=build /app/target/swari-sewa-backend-0.0.1-SNAPSHOT.jar app.jar
COPY entrypoint.sh entrypoint.sh
RUN chmod +x entrypoint.sh

# Render injects the PORT env var; the app uses it via server.port=${PORT:8081}
EXPOSE 8081

ENTRYPOINT ["/app/entrypoint.sh"]
