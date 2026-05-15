#!/bin/bash

echo "🔨 Building AuraChat Backend with Docker..."
echo ""

# Build using Docker with Maven image
docker run --rm \
  -v "$(pwd)":/app \
  -w /app \
  maven:3.9.6-eclipse-temurin-21 \
  mvn clean install -U

echo ""
echo "✅ Build completed!"
