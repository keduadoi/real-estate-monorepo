# Real Estate Backend API

Backend REST API for the Real Estate Application built with Spring Boot 3.

## Tech Stack

- **Java**: 17
- **Spring Boot**: 3.2.1
- **Build Tool**: Maven
- **Database**: PostgreSQL 18
- **ORM**: Spring Data JPA / Hibernate
- **Migration**: Flyway
- **Lombok**: Code generation
- **DevTools**: Hot reload
- **Containerization**: Docker
- **Orchestration**: Kubernetes with Helm

## Prerequisites

Choose your deployment method:

### Option 1: Docker Compose (Recommended for Quick Start)
- Docker Desktop or Docker Engine
- Docker Compose

### Option 2: Kubernetes with Helm (Production-like Environment)
- Docker Desktop or Docker Engine
- Minikube (v1.37.0+)
- Helm (v4.1.0+)
- kubectl

### Option 3: Local Development
- Java 17 or higher
- PostgreSQL 18
- Maven (optional - project includes Maven Wrapper)

## Getting Started

### Quick Start with Docker Compose

The fastest way to get started:

```bash
cd backend
docker-compose up -d
```

Access the application at `http://localhost:8080`

Health check:
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {"status": "UP"},
    "livenessState": {"status": "UP"},
    "ping": {"status": "UP"},
    "readinessState": {"status": "UP"}
  }
}
```

To stop:
```bash
docker-compose down
```

### Kubernetes Deployment

For a production-like local environment with Kubernetes:

**Quick Start:**
See [KUBERNETES_QUICKSTART.md](docs/KUBERNETES_QUICKSTART.md) for step-by-step instructions.

**Full Migration Guide:**
See [KUBERNETES_MIGRATION.md](docs/KUBERNETES_MIGRATION.md) for complete migration documentation.

**Basic Commands:**
```bash
# Start Minikube
minikube start --memory=3500 --cpus=2

# Build and load image
eval $(minikube docker-env)
docker build -t real-estate-backend:latest .

# Deploy with Helm
helm install real-estate ./helm/real-estate-backend \
  -f ./helm/real-estate-backend/values-dev.yaml

# Access the application
kubectl port-forward -n real-estate-dev \
  svc/real-estate-real-estate-backend-backend 8080:8080
```

### Local Development

For development without containers:

1. **Start PostgreSQL:**
```bash
# Using Docker
docker run -d --name postgres \
  -e POSTGRES_DB=realestatedb \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:18-alpine
```

2. **Build and Run:**
```bash
cd backend
./mvnw clean install
./mvnw spring-boot:run
```

Or use the startup script:
```bash
./run.sh
```

The application will start on `http://localhost:8080`

## Project Structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/realestate/backend/
│   │   │   ├── BackendApplication.java           # Main application
│   │   │   ├── config/                          # Configuration classes
│   │   │   ├── controller/                      # REST controllers
│   │   │   ├── dto/                            # Data Transfer Objects
│   │   │   ├── entity/                         # JPA entities
│   │   │   ├── exception/                      # Exception handling
│   │   │   ├── repository/                     # Data repositories
│   │   │   └── service/                        # Business logic
│   │   └── resources/
│   │       ├── application.yml                 # Main configuration
│   │       ├── application-dev.yml            # Dev environment
│   │       ├── application-prod.yml           # Production environment
│   │       └── db/migration/                  # Flyway migrations
│   └── test/
│       └── java/                              # Unit and integration tests
├── helm/                                       # Helm charts for Kubernetes
│   └── real-estate-backend/
│       ├── Chart.yaml                         # Chart metadata
│       ├── values.yaml                        # Default values
│       ├── values-dev.yaml                    # Development overrides
│       ├── templates/                         # Kubernetes manifests
│       └── README.md                          # Chart documentation
├── docs/                                       # Documentation
│   ├── KUBERNETES_MIGRATION.md               # Migration guide
│   └── KUBERNETES_QUICKSTART.md              # Quick start guide
├── Dockerfile                                 # Docker image definition
├── docker-compose.yml                         # Docker Compose setup
├── pom.xml                                    # Maven dependencies
└── README.md                                  # This file
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

## API Endpoints

### Health Check
```bash
GET /actuator/health
```

### Properties
```bash
GET    /api/properties              # List all properties
GET    /api/properties/{id}         # Get property by ID
POST   /api/properties              # Create new property
PUT    /api/properties/{id}         # Update property
DELETE /api/properties/{id}         # Delete property
```

### Property Images
```bash
POST   /api/properties/{id}/images  # Upload images
GET    /api/properties/{id}/images  # Get property images
DELETE /api/images/{id}             # Delete image
```

### File Access
```bash
GET /uploads/{filename}              # Access uploaded files
```

## Database

The application uses PostgreSQL with Flyway for database migrations.

### Migrations

Database schema is managed by Flyway migrations located in `src/main/resources/db/migration/`:
- `V1__initial_schema.sql` - Initial schema with properties and images tables
- `V2__add_image_constraints.sql` - Image constraints and triggers

### Connect to Database

**Docker Compose:**
```bash
docker exec -it real-estate-postgres psql -U postgres -d realestatedb
```

**Kubernetes:**
```bash
kubectl exec -it -n real-estate-dev \
  real-estate-real-estate-backend-postgres-0 -- \
  psql -U postgres -d realestatedb
```

**Inside psql:**
```sql
\dt              -- List tables
\d properties    -- Describe properties table
SELECT * FROM properties;
\q               -- Quit
```

## Configuration

Application configuration is managed through Spring profiles:

- `application.yml` - Base configuration
- `application-dev.yml` - Development settings
- `application-prod.yml` - Production settings
- `application-docker.yml` - Docker environment settings

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `dev` |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `realestatedb` |
| `DB_USER` | Database user | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `SERVER_PORT` | Application port | `8080` |
| `CORS_ALLOWED_ORIGINS` | Allowed CORS origins | `http://localhost:3000` |

## File Uploads

The application stores uploaded property images in the `/app/uploads` directory (in containers) or `./uploads` (local development).

**Supported formats:** JPEG, PNG, WebP
**Max file size:** 10MB
**Max files per property:** 10

## Deployment Options Comparison

| Feature | Docker Compose | Kubernetes + Helm |
|---------|----------------|-------------------|
| Setup Time | 1 minute | 5 minutes |
| Learning Curve | Low | Medium |
| Production Ready | No | Yes |
| Scalability | Limited | High |
| High Availability | No | Yes |
| Rolling Updates | No | Yes |
| Rollback Support | Manual | Automatic |
| Best For | Local dev | Production-like env |

## Documentation

- **[Kubernetes Quick Start](docs/KUBERNETES_QUICKSTART.md)** - Get started with Kubernetes in minutes
- **[Kubernetes Migration Guide](docs/KUBERNETES_MIGRATION.md)** - Complete migration documentation
- **[Helm Chart README](helm/real-estate-backend/README.md)** - Helm chart configuration details

## Monitoring & Observability

### Actuator Endpoints

Available at `/actuator`:
- `/actuator/health` - Application health status
- `/actuator/info` - Application information
- `/actuator/metrics` - Application metrics

In development mode (`dev` profile), all actuator endpoints are exposed.

### Logs

**Docker Compose:**
```bash
# Backend logs
docker-compose logs -f backend

# Database logs
docker-compose logs -f postgres
```

**Kubernetes:**
```bash
# Backend logs
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=backend -f

# Database logs
kubectl logs -n real-estate-dev -l app.kubernetes.io/component=database -f
```

## Troubleshooting

### Common Issues

**Port already in use:**
```bash
# Find process using port 8080
lsof -i :8080
# Kill the process
kill -9 <PID>
```

**Database connection refused:**
- Ensure PostgreSQL is running
- Check database credentials in environment variables
- Verify database host and port

**Docker build fails:**
- Ensure you have enough disk space
- Clear Docker cache: `docker system prune -a`

**Kubernetes pods not starting:**
- Check pod logs: `kubectl logs -n real-estate-dev <pod-name>`
- Describe pod: `kubectl describe pod -n real-estate-dev <pod-name>`
- Ensure minikube has enough resources

For more troubleshooting help, see the [Kubernetes Migration Guide](docs/KUBERNETES_MIGRATION.md#troubleshooting).

## Contributing

1. Create a feature branch
2. Make your changes
3. Write/update tests
4. Ensure all tests pass: `./mvnw test`
5. Submit a pull request

## License

This project is part of the Real Estate Application monorepo.
