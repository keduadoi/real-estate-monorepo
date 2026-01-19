# Real Estate Backend API

Backend REST API for the Real Estate Application built with Spring Boot 3.

## Tech Stack

- **Java**: 17
- **Spring Boot**: 3.2.1
- **Build Tool**: Maven
- **Database**: H2 (in-memory, for development)
- **ORM**: Spring Data JPA
- **Lombok**: Code generation
- **DevTools**: Hot reload

## Prerequisites

- Java 17 or higher
- **No Maven installation needed** - Project includes Maven Wrapper (mvnw)

## Getting Started

### 1. Build the Project

```bash
cd backend
./mvnw clean install
```

### 2. Run the Application

```bash
./mvnw spring-boot:run
```

Or use the startup script:

```bash
./run.sh
```

The application will start on `http://localhost:8080`

### 3. Test the Application

Health check endpoint:
```bash
curl http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "UP",
  "timestamp": "2024-01-18T...",
  "application": "Real Estate Backend",
  "version": "0.0.1-SNAPSHOT"
}
```

## H2 Database Console

Access the H2 console at: `http://localhost:8080/h2-console`

- **JDBC URL**: `jdbc:h2:mem:realestatedb`
- **Username**: `sa`
- **Password**: (leave empty)

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/realestate/backend/
│   │   │   ├── BackendApplication.java      # Main application
│   │   │   └── controller/
│   │   │       └── HealthController.java    # Health check endpoint
│   │   └── resources/
│   │       └── application.properties       # Configuration
│   └── test/
│       └── java/
├── pom.xml                                   # Maven dependencies
└── README.md
```

## Development

### Hot Reload

Spring Boot DevTools is included, so changes will automatically reload during development.

### Logging

Logs are configured to show DEBUG level for the application package.
Check console output for detailed information.

## CORS Configuration

CORS is configured to allow requests from:
- `http://localhost:3000` (Next.js frontend)

## Maven Wrapper

This project uses Maven Wrapper (`mvnw`), so you **don't need to install Maven** globally. The wrapper ensures everyone uses the same Maven version (3.9.12).

**Windows users**: Use `mvnw.cmd` instead of `./mvnw`

## Common Commands

```bash
# Build the project
./mvnw clean install

# Run the application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Package as JAR
./mvnw package

# Skip tests during build
./mvnw clean install -DskipTests
```

## Next Steps

- Add entity models
- Create repositories
- Implement service layer
- Add REST controllers
- Configure PostgreSQL for production
- Add authentication/authorization
- Write unit and integration tests
