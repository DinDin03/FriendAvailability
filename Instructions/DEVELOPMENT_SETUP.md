# Friend Availability - Development Setup

## Overview
This project uses a **proxy setup** for development, allowing the React frontend and Spring Boot backend to work together seamlessly while running on separate servers.

## Architecture
- **Backend**: Spring Boot (Java 17) in `backend/` folder running on `http://localhost:8080`
- **Frontend**: React + Vite in `frontend/` folder running on `http://localhost:5173`
- **Proxy**: Vite proxies all `/api/*` calls to the Spring Boot backend

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- MySQL database running
- Maven

### 1. Install Frontend Dependencies
```bash
cd frontend
npm install
```

### 2. Start Development Servers

#### Option A: Use the provided scripts
**Windows:**
```bash
start-dev.bat
```

**Linux/Mac:**
```bash
chmod +x start-dev.sh
./start-dev.sh
```

**WSL (Windows Subsystem for Linux):**
```bash
# Option 1: All-in-one script (runs both in background)
chmod +x start-dev-wsl.sh
./start-dev-wsl.sh

# Option 2: Separate terminals (recommended)
# Terminal 1:
chmod +x start-backend.sh
./start-backend.sh

# Terminal 2:
chmod +x start-frontend.sh
./start-frontend.sh
```

#### Option B: Manual startup
**Terminal 1 - Backend:**
```bash
# Windows
cd backend
mvnw.cmd spring-boot:run

# Linux/Mac
cd backend
./mvnw spring-boot:run
```

**Terminal 2 - Frontend:**
```bash
cd frontend
npm run dev
```

### 3. Access the Application
- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080/api
- **API Test Page**: http://localhost:5173 (scroll down to see the API test component)

## How the Proxy Works

1. **Frontend makes API call**: `fetch('/api/auth/login', ...)`
2. **Vite proxy intercepts**: Recognizes `/api/*` pattern
3. **Forwards to backend**: `http://localhost:8080/api/auth/login`
4. **Returns response**: Backend response sent back to frontend

## API Services

The frontend includes several service classes:

### AuthService (`frontend/src/services/authService.js`)
- Login/logout
- Registration
- Password reset
- Email verification
- OAuth integration

### UserService (`frontend/src/services/userService.js`)
- User management
- Profile updates
- User search

### Base API Service (`frontend/src/services/api.js`)
- HTTP request handling
- Error management
- Endpoint configuration

## Testing the Connection

1. Start both servers
2. Visit http://localhost:5173
3. Scroll down to the "API Connection Test" section
4. Click "Test Backend Connection" to verify the proxy is working
5. Try other test buttons to verify different endpoints

## Configuration Files

### Vite Proxy Configuration (`frontend/vite.config.js`)
```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
      secure: false
    }
  }
}
```

### CORS Configuration (Backend)
The backend allows requests from:
- `http://localhost:8080` (backend)
- `http://localhost:5173` (frontend dev server)
- Production domains

## Troubleshooting

### Common Issues

1. **CORS Errors**
   - Ensure backend CORS includes `http://localhost:5173`
   - Check that both servers are running

2. **Proxy Not Working**
   - Verify Vite config has correct proxy settings
   - Check browser network tab for request URLs

3. **Backend Connection Failed**
   - Ensure Spring Boot is running on port 8080
   - Check database connection
   - Verify application.properties configuration

4. **Frontend Build Issues**
   - Run `npm install` in frontend directory
   - Clear node_modules and reinstall if needed

### Debug Tips

1. **Check proxy logs**: Vite logs proxy requests in the terminal
2. **Browser DevTools**: Network tab shows actual request URLs
3. **Backend logs**: Spring Boot logs show incoming requests
4. **API Test Component**: Use the built-in test component to verify connections

## Next Steps

After verifying the proxy setup works:

1. **Remove test component** from Home.jsx
2. **Implement authentication UI** (login/register forms)
3. **Add protected routes** for authenticated users
4. **Build dashboard components** using the API services
5. **Implement real-time features** (WebSocket chat)

## Production Deployment

For production, you'll want to:
1. Build the React app: `npm run build`
2. Serve React from Spring Boot static resources
3. Remove proxy configuration
4. Update CORS for production domains
