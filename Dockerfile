FROM eclipse-temurin:17-jdk AS builder

# Install Maven and curl
RUN apt-get update && apt-get install -y maven curl ca-certificates

# Set the working directory in the container
WORKDIR /app

# Copy the Maven project file
COPY pom.xml .

# Download dependencies
RUN mvn dependency:go-offline -B

# Copy the rest of the application source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# ------------------------------------
# Final image
# ------------------------------------
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Install certificate tools
RUN apt-get update && apt-get install -y ca-certificates

# Create a folder for SSL cert
RUN mkdir -p /etc/ssl/certs/custom

# Copy JAR from builder
COPY --from=builder /app/target/DataquadRequirementsApi-0.0.1-SNAPSHOT.jar app.jar

# Copy SSL certificate (GitHub Actions will ensure this exists)
COPY nginx/ssl/mymulya.crt /etc/ssl/certs/custom/mymulya.crt

# Import cert into Java truststore
RUN keytool -import -trustcacerts -alias mymulya_cert \
    -file /etc/ssl/certs/custom/mymulya.crt \
    -keystore $JAVA_HOME/lib/security/cacerts \
    -storepass changeit -noprompt

# Expose app port
EXPOSE 8111

# Start the application
ENTRYPOINT ["java", "-jar", "app.jar"]
