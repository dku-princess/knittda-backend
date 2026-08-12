# Base image
FROM eclipse-temurin:21-jdk

# HEIC/HEIF 디코딩용 네이티브 libheif CLI(heif-convert) 설치.
# libheif-examples 가 /usr/bin/heif-convert 와 네이티브 libheif 를 함께 제공한다.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libheif-examples \
    && rm -rf /var/lib/apt/lists/* \
    && command -v heif-convert

# App directory
WORKDIR /app

# Copy the JAR file
COPY build/libs/*SNAPSHOT.jar app.jar

# Expose the port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
