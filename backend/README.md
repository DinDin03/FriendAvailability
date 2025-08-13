# Friend Availability Backend

Spring Boot REST API for the Friend Availability application.

## Technology Stack

- **Framework**: Spring Boot 3.5.0
- **Language**: Java 17
- **Database**: H2 (development), MySQL (production)
- **Security**: Spring Security + OAuth2 (Google)
- **Build Tool**: Maven
- **Architecture**: Clean Architecture with Domain-Driven Design

## Project Structure

```
backend/
├── src/main/java/com/friendavailability/
│   ├── api/                    # Controllers and DTOs
│   │   ├── controller/v1/      # REST Controllers
│   │   └── dto/               # Data Transfer Objects
│   ├── domain/                # Business Logic
│   │   ├── entity/            # Domain Entities
│   │   ├── repository/        # Repository Interfaces
│   │   └── service/           # Business Services
│   ├── infrastructure/        # External Concerns
│   │   ├── config/            # Configuration Classes
│   │   └── repository/        # Repository Implementations
│   └── FriendAvailabilityAppApplication.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-local.properties
│   └── static/                # Static web resources
└── pom.xml
```

## Quick Start

### Prerequisites
- Java 17+
- Maven (or use included wrapper)

### Development

1. **Navigate to backend directory**
   ```bash
   cd backend
   ```

2. **Run the application**
   ```bash
   # Using Maven wrapper (recommended)
   ./mvnw spring-boot:run
   
   # Or using installed Maven
   mvn spring-boot:run
   ```

3. **Access the application**
   - API Base URL: http://localhost:8080/api
   - H2 Console: http://localhost:8080/h2-console
   - Actuator Health: http://localhost:8080/actuator/health

### Database Configuration

**Development (H2 in-memory):**
- URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (empty)
- Console: http://localhost:8080/h2-console

**Production (MySQL):**
- Configured via environment variables
- See `application.properties` for details

## API Endpoints

### Authentication (`/api/auth`)
- `POST /api/auth/login` - User login
- `POST /api/auth/register` - User registration
- `POST /api/auth/logout` - User logout
- `GET /api/auth/current-user` - Get current user
- `POST /api/auth/verify-email` - Verify email address
- `GET /oauth2/authorization/google` - Google OAuth login

### Users (`/api/users`)
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/profile` - Update user profile
- `DELETE /api/users/{id}` - Delete user account

### Friends (`/api/friends`)
- `GET /api/friends/{userId}` - Get user's friends
- `POST /api/friends/request` - Send friend request
- `POST /api/friends/{requestId}/accept` - Accept friend request
- `POST /api/friends/{requestId}/reject` - Reject friend request
- `DELETE /api/friends/remove` - Remove friend

### Calendar (`/api/calendar`)
- `GET /api/calendar/{userId}/availability` - Get user availability
- `GET /api/calendar/{userId}/events` - Get user events
- `POST /api/calendar/events` - Create event
- `PUT /api/calendar/events/{eventId}` - Update event
- `DELETE /api/calendar/events/{eventId}` - Delete event

### Chat (`/api/chat`)
- `GET /api/chat/rooms` - Get chat rooms
- `POST /api/chat/rooms` - Create chat room
- `GET /api/chat/rooms/{roomId}/messages` - Get messages
- `POST /api/chat/rooms/{roomId}/messages` - Send message
- WebSocket endpoint: `/ws` for real-time messaging

### Circles (`/api/circles`)
- `GET /api/circles` - Get user's circles
- `POST /api/circles` - Create circle
- `POST /api/circles/{circleId}/join` - Join circle
- `DELETE /api/circles/{circleId}/leave` - Leave circle

## Configuration

### Profiles
- `local` - Development profile (H2 database)
- `production` - Production profile (MySQL database)

### Environment Variables
- `DATABASE_URL` - Database connection URL
- `GOOGLE_CLIENT_ID` - Google OAuth client ID
- `GOOGLE_CLIENT_SECRET` - Google OAuth client secret
- `SMTP_USERNAME` - Email SMTP username
- `SMTP_PASSWORD` - Email SMTP password

## Development

### Building
```bash
./mvnw clean compile
```

### Testing
```bash
./mvnw test
```

### Running with specific profile
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### Debugging
Add JVM debug options:
```bash
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

## Security

- **Authentication**: JWT tokens + OAuth2 (Google)
- **Authorization**: Role-based access control
- **CORS**: Configured for frontend origins
- **CSRF**: Disabled for API endpoints
- **Password**: BCrypt hashing

## Monitoring

- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Info**: `/actuator/info`

## Troubleshooting

### Common Issues

1. **Database Connection Failed**
   - Check H2 console is enabled
   - Verify database URL in properties

2. **Compilation Errors**
   - Ensure Java 17 is being used
   - Clean and rebuild: `./mvnw clean compile`

3. **Lombok Issues**
   - IDE should have Lombok plugin installed
   - Annotation processing should be enabled

4. **Port Already in Use**
   - Change port in `application.properties`: `server.port=8081`
   - Or kill process using port 8080

### Logs
Application logs are output to console. For file logging, add to `application.properties`:
```properties
logging.file.name=logs/application.log
```
