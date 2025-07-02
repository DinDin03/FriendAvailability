#!/bin/bash

echo "Setting up FriendAvailability Local Development Environment..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "Docker is not running. Please start Docker Desktop and try again."
    exit 1
fi

# Check if .env.local exists, create if not
if [ ! -f ".env.local" ]; then
    echo ".env.local file not found. Creating a basic one..."
    cat > .env.local << EOF
# Environment variables for local development
SPRING_PROFILES_ACTIVE=local
MYSQL_ROOT_PASSWORD=16October2003@
MYSQL_DATABASE=linkup_db
MYSQL_USER=linkup_user
MYSQL_PASSWORD=16October2003@
EOF
fi

echo "Starting Docker services..."
docker-compose up -d

echo "Waiting for MySQL to be ready..."
until docker exec linkup-mysql mysqladmin ping -h"localhost" --silent; do
    echo "   Waiting for database connection..."
    sleep 2
done

echo "MySQL is ready!"

echo "Setting up application-local.properties..."
# This will be created automatically by Spring Boot using our .env.local values

echo "Development Environment Status:"
echo "Database: http://localhost:3306"
echo "phpMyAdmin: http://localhost:8081"
echo "Application: http://localhost:8080 (when started)"
echo ""
echo "To start your Spring Boot application:"
echo "   ./mvnw spring-boot:run"
echo ""
echo "Development environment is ready!"