# Stage 1: Build code (Sử dụng JDK 21 và Maven)
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy file cấu hình Maven để tải thư viện trước (tối ưu cache layer)
COPY pom.xml .
RUN for attempt in 1 2 3; do \
      mvn -B -Dmaven.wagon.http.retryHandler.count=5 dependency:go-offline && break; \
      if [ "$attempt" = "3" ]; then exit 1; fi; \
      echo "Maven download failed (attempt $attempt/3), retrying..."; \
      sleep 5; \
    done

# Copy code và thực hiện build file .jar
COPY src ./src
RUN mvn -B -Dmaven.wagon.http.retryHandler.count=5 clean package -DskipTests

# Stage 2: Run (Chỉ giữ lại JRE để giảm dung lượng image)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Lấy file jar vừa build ở Stage 1 sang Stage 2
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]
