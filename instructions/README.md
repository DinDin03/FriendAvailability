# Friend Availability App

A full-stack application for managing friend availability and scheduling.

## Project Structure

```
FriendAvailability/
├── backend/                 # Spring Boot API (Java 17)
│   ├── src/
│   ├── pom.xml
│   └── mvnw*
├── frontend/                # React + Vite UI
│   ├── src/
│   ├── package.json
│   └── vite.config.js
├── sql/                     # Database scripts
├── data/                    # H2 database files (development)
└── start-*.sh/bat          # Development startup scripts
```

## Quick Start

### Prerequisites
- Java 17+
- Node.js 18+
- Git

### Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd FriendAvailability
   ```

2. **Install frontend dependencies**
   ```bash
   cd frontend
   npm install
   cd ..
   ```

3. **Start development servers**

   **Option A: Use startup scripts**
   ```bash
   # Windows
   start-dev.bat

   # Linux/Mac/WSL
   chmod +x start-dev-wsl.sh
   ./start-dev-wsl.sh
   ```

   **Option B: Manual startup**
   ```bash
   # Terminal 1 - Backend
   cd backend
   ./mvnw spring-boot:run

   # Terminal 2 - Frontend
   cd frontend
   npm run dev
   ```

4. **Access the application**
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080/api
   - H2 Database Console: http://localhost:8080/h2-console

## Technology Stack

### Backend
- **Framework**: Spring Boot 3.5.0
- **Language**: Java 17
- **Database**: H2 (development), MySQL (production)
- **Security**: Spring Security + OAuth2 (Google)
- **Build Tool**: Maven

### Frontend
- **Framework**: React 19.1.0
- **Build Tool**: Vite
- **Styling**: Tailwind CSS 4.1.11
- **Routing**: React Router DOM 7.7.0

## Features

- 🔐 **Authentication**: Google OAuth2 + Custom auth
- 👥 **User Management**: Registration, profiles, friends
- 📅 **Calendar Integration**: Availability scheduling
- 💬 **Real-time Chat**: WebSocket-based messaging
- 👥 **Social Circles**: Group management
- 📧 **Email Verification**: Automated email workflows

## Development

See [DEVELOPMENT_SETUP.md](DEVELOPMENT_SETUP.md) for detailed development instructions.

## API Documentation

The backend provides a REST API with the following main endpoints:

- `/api/auth/*` - Authentication endpoints
- `/api/users/*` - User management
- `/api/friends/*` - Friend relationships
- `/api/calendar/*` - Calendar and availability
- `/api/chat/*` - Chat and messaging
- `/api/circles/*` - Social circles

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## License

[Add your license information here]

# Packages

- react-router-dom
- tailwindcss/cli
- clsx
- tailwind-merge

# 4. Production Build and Deployment
- When ready to deploy, build the React app:
- Apply to SecurityConf...
- This creates a frontend/dist folder with static files.
- Copy the contents of frontend/dist to src/main/resources/static in your Spring Boot project:
- Apply to SecurityConf...
- Spring Boot will now serve your React app at /.