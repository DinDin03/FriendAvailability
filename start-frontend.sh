#!/bin/bash

echo "Starting React Frontend..."
echo "Frontend will be available at: http://localhost:5173"
echo "Press Ctrl+C to stop"
echo ""

cd frontend

# Check if node_modules exists, if not install dependencies
if [ ! -d "node_modules" ]; then
    echo "Dependencies not found. Installing..."
    npm install
    echo "Dependencies installed successfully!"
    echo ""
fi

npm run dev
