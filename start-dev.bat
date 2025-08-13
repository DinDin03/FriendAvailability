@echo off
echo Starting Friend Availability Development Environment
echo.

echo Starting Spring Boot Backend...
start "Spring Boot Backend" cmd /k "cd backend && mvnw.cmd spring-boot:run"

echo Waiting 10 seconds for backend to start...
timeout /t 10 /nobreak > nul

echo Starting React Frontend...
start "React Frontend" cmd /k "cd frontend && npm run dev"

echo.
echo Development servers starting:
echo - Backend: http://localhost:8080
echo - Frontend: http://localhost:5173
echo.
echo Press any key to exit...
pause > nul
