#!/bin/bash

# Real Estate Backend Startup Script

echo "🚀 Starting Real Estate Backend..."
echo ""

# Check Java version
echo "☕ Checking Java version..."
java -version

echo ""
echo "📦 Building and running the application with Maven Wrapper..."
echo ""

# Run the application using Maven Wrapper
./mvnw spring-boot:run
