#!/bin/bash

echo "Starting Friend Availability Development Environment (WSL)"
echo ""

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Start backend in background
echo "Starting Spring Boot Backend..."
cd backend
./mvnw spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend started with PID: $BACKEND_PID"

# Wait for backend to start
echo "Waiting 15 seconds for backend to start..."
sleep 15

# Start frontend in background
echo "Starting React Frontend..."
cd ../frontend
npm run dev > frontend.log 2>&1 &
FRONTEND_PID=$!
cd ..
echo "Frontend started with PID: $FRONTEND_PID"

echo ""
echo "Development servers started:"
echo "- Backend: http://localhost:8080 (PID: $BACKEND_PID)"
echo "- Frontend: http://localhost:5173 (PID: $FRONTEND_PID)"
echo ""
echo "Logs:"
echo "- Backend: tail -f backend/backend.log"
echo "- Frontend: tail -f frontend/frontend.log"
echo ""
echo "To stop servers:"
echo "- kill $BACKEND_PID $FRONTEND_PID"
echo "- Or press Ctrl+C and run: pkill -f 'spring-boot:run|vite'"
echo ""

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "Stopping servers..."
    kill $BACKEND_PID $FRONTEND_PID 2>/dev/null
    pkill -f "spring-boot:run" 2>/dev/null
    pkill -f "vite" 2>/dev/null
    echo "Servers stopped."
    exit 0
}

# Set trap to cleanup on script exit
trap cleanup SIGINT SIGTERM

# Keep script running and show logs
echo "Press Ctrl+C to stop all servers"
echo "Showing backend logs (press Ctrl+C to stop):"
echo "----------------------------------------"

# Follow backend logs
tail -f backend/backend.log 2>/dev/null &
TAIL_PID=$!

# Wait for user interrupt
wait $TAIL_PID 2>/dev/null

cleanup
