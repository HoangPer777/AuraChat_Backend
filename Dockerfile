# Stage 1: Build code (Sử dụng JDK 21 và Maven)
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy file cấu hình Maven để tải thư viện trước (tối ưu cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy code và thực hiện build file .jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Run (Chỉ giữ lại JRE để giảm dung lượng image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Lấy file jar vừa build ở Stage 1 sang Stage 2
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]