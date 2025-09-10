#!/bin/bash

echo "Starting React Frontend..."
echo "Frontend will be available at: http://localhost:5173"
echo "Press Ctrl+C to stop"
echo ""

# shellcheck disable=SC2164
cd ..
cd frontend
npm run dev
# shellcheck disable=SC2103
