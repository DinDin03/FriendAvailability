#!/bin/bash

echo "Starting Spring Boot Backend..."
echo "Backend will be available at: http://localhost:8080"
echo "Press Ctrl+C to stop"
echo ""

cd backend
./mvnw spring-boot:run
