@echo off
echo Building AuraChat Backend with Docker...
echo.

docker run --rm -v "%cd%":/app -w /app maven:3.9.6-eclipse-temurin-21 mvn clean install -U

echo.
echo Build completed!
pause
