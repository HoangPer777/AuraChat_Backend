FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy file JAR đã được build từ bước GitHub Actions
COPY target/*.jar app.jar

# Copy file Firebase (vì Stage 2 không thấy được file ở ngoài nếu không mount)
COPY serviceAccountKey.json serviceAccountKey.json

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]