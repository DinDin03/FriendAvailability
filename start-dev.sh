#!/bin/bash

echo "Starting Friend Availability Development Environment"
echo ""

echo "Starting Spring Boot Backend..."
gnome-terminal --title="Spring Boot Backend" -- bash -c "./mvnw spring-boot:run; exec bash" &

echo "Waiting 10 seconds for backend to start..."
sleep 10

echo "Starting React Frontend..."
gnome-terminal --title="React Frontend" -- bash -c "cd frontend && npm run dev; exec bash" &

echo ""
echo "Development servers starting:"
echo "- Backend: http://localhost:8080"
echo "- Frontend: http://localhost:5173"
echo ""
echo "Press Ctrl+C to exit..."

# Keep script running
while true; do
    sleep 1
done
