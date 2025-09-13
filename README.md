# Name Draw Application

A Spring Boot backend with React frontend application for managing name draws with social login (Google, Facebook).

## Phase 3.1: Project Setup ✅ COMPLETED

### Backend (Spring Boot)
- ✅ Maven project with Spring Boot 3.3.3
- ✅ Dependencies: Spring Web, Data JPA, Security, OAuth2 Client, Validation
- ✅ H2 Database support
- ✅ Flyway database migrations
- ✅ JWT token support
- ✅ Google Java Format and Checkstyle
- ✅ Maven wrapper

### Frontend (React + Vite)
- ✅ React 18 + TypeScript
- ✅ Vite build system
- ✅ ESLint and Prettier configuration
- ✅ Testing dependencies
- ✅ React Router

### Docker Configuration
- ✅ Backend Dockerfile with multi-stage build
- ✅ Frontend Dockerfile with Nginx
- ✅ Development docker-compose.yml
- ✅ Production docker-compose.prod.yml with Let's Encrypt
- ✅ Health checks and security configurations

## Project Structure

```
.
├── backend/                 # Spring Boot application
│   ├── src/main/java/       # Java source code
│   ├── src/main/resources/  # Application configuration
│   ├── src/test/java/       # Test source code
│   ├── Dockerfile          # Backend container
│   └── pom.xml             # Maven configuration
├── frontend/               # React application
│   ├── src/                # TypeScript source code
│   ├── tests/              # Test files
│   ├── Dockerfile          # Frontend container
│   ├── nginx.conf          # Nginx configuration
│   └── package.json        # Node.js configuration
├── docker-compose.yml      # Development environment
├── docker-compose.prod.yml # Production with Let's Encrypt
└── specs/                  # Project specifications
```

## Quick Start

### Development
```bash
# Build backend
cd backend && ./mvnw clean compile

# Build frontend
cd frontend && npm install && npm run build

# Run with Docker
docker-compose up --build
```

### Production
```bash
# Set environment variables
export DOMAIN_NAME=your-domain.com
export LETSENCRYPT_EMAIL=your-email@example.com
export DB_PASSWORD=your-secure-password
export JWT_SECRET=your-jwt-secret
export OAUTH_GOOGLE_CLIENT_ID=your-google-client-id
export OAUTH_GOOGLE_CLIENT_SECRET=your-google-client-secret
export OAUTH_FACEBOOK_CLIENT_ID=your-facebook-client-id
export OAUTH_FACEBOOK_CLIENT_SECRET=your-facebook-client-secret

# Deploy
docker network create proxy
docker-compose -f docker-compose.prod.yml up -d
```

## Technology Stack

- **Backend**: Java 17, Spring Boot 3.3.3, H2 Database, Flyway, JWT
- **Frontend**: React 18, TypeScript, Vite, React Router
- **Authentication**: OAuth2 (Google, Facebook), JWT tokens
- **Database**: H2 (in-memory for development, file-based for production)
- **Deployment**: Docker, Docker Compose, Let's Encrypt SSL
- **Testing**: JUnit 5, Mockito, React Testing Library
- **Code Quality**: Checkstyle, Google Java Format, ESLint, Prettier

## Next Steps

Phase 3.2: Database and Configuration
Phase 3.3: Contract Tests First (TDD)
Phase 3.4: Entity Models
... (see specs/001-create-an-application/tasks.md)

## Documentation

See `specs/001-create-an-application/` for detailed specifications:
- `spec.md` - Feature specification and requirements
- `plan.md` - Implementation plan and architecture
- `tasks.md` - Detailed task breakdown (72 tasks)
- `research.md` - Technology decisions and architecture
- `data-model.md` - Entity definitions and relationships
- `contracts/api.yaml` - OpenAPI 3.0 specification
- `quickstart.md` - Setup and testing guide
