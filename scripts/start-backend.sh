#!/bin/bash

echo "Starting Spring Boot Backend..."
echo "Backend will be available at: http://localhost:8080"
echo "Press Ctrl+C to stop"
echo ""

cd ..
cd backend
mvn clean
./mvnw spring-boot:run
# shellcheck disable=SC2103
