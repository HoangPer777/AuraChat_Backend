# Production deploy: CI đã chạy `mvn package`, chỉ copy JAR (không build lại trên EC2).
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
