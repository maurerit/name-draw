# Quickstart Guide: Name Draw Application

**Date**: September 8, 2025  
**Feature**: Name Draw Application with Social Login  
**Purpose**: Step-by-step guide to set up and validate the application

## Prerequisites

### Development Environment
- Java 17 or higher
- Node.js 18 or higher
- Docker and Docker Compose
- Git
- Maven 3.8+ (or use Maven wrapper)

### External Services
- Facebook Developer Account (for OAuth app registration)
- Google Cloud Console access (for OAuth app registration)

## Environment Setup

### 1. Clone and Setup Repository
```bash
# Clone the repository
git clone <repository-url>
cd name-draw

# Checkout feature branch
git checkout 001-create-an-application
```

### 2. OAuth Provider Setup

#### Facebook OAuth Setup
1. Go to [Facebook Developers](https://developers.facebook.com/)
2. Create a new app for "Consumer" use case
3. Add "Facebook Login" product
4. Configure Valid OAuth Redirect URIs:
   - Development: `http://localhost:8080/api/v1/auth/callback/facebook`
   - Production: `https://yourdomain.com/api/v1/auth/callback/facebook`
5. Note down App ID and App Secret

#### Google OAuth Setup
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Google+ API
4. Create OAuth 2.0 Client ID credentials
5. Configure Authorized redirect URIs:
   - Development: `http://localhost:8080/api/v1/auth/callback/google`
   - Production: `https://yourdomain.com/api/v1/auth/callback/google`
6. Note down Client ID and Client Secret

### 3. Environment Configuration

#### Backend Configuration
Create `backend/src/main/resources/application-dev.yml`:
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/namedraw_dev;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  
  h2:
    console:
      enabled: true
      path: /h2-console
      settings:
        web-allow-others: false
  
  security:
    oauth2:
      client:
        registration:
          facebook:
            client-id: ${FACEBOOK_CLIENT_ID}
            client-secret: ${FACEBOOK_CLIENT_SECRET}
            scope: email,public_profile
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: profile,email

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

logging:
  level:
    com.namedraw: DEBUG
    org.springframework.security: DEBUG

jwt:
  secret: ${JWT_SECRET:your-256-bit-secret-key-here}
  expiration: 86400000  # 24 hours
```

#### Frontend Configuration
Create `frontend/.env.development`:
```bash
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_APP_NAME=Name Draw
```

#### Environment Variables
Create `.env` file in project root:
```bash
# Database (H2 - no additional setup required)
DATABASE_URL=jdbc:h2:file:./data/namedraw_dev;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
DATABASE_USERNAME=sa
DATABASE_PASSWORD=

# OAuth Providers
FACEBOOK_CLIENT_ID=your-facebook-app-id
FACEBOOK_CLIENT_SECRET=your-facebook-app-secret
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# JWT
JWT_SECRET=your-secure-256-bit-secret-key-change-this-in-production

# Development
SPRING_PROFILES_ACTIVE=dev
```

## Database Setup

### H2 Database (Zero Configuration)
H2 database requires no additional setup! The database will be automatically created when the application starts.

```bash
# Create data directory (optional - will be created automatically)
mkdir -p data

# Database file will be created at: ./data/namedraw_dev.mv.db
# H2 Console will be available at: http://localhost:8080/h2-console
```

#### H2 Console Access (Development)
1. Start the backend application
2. Open browser to `http://localhost:8080/h2-console`
3. Use connection settings:
   - JDBC URL: `jdbc:h2:file:./data/namedraw_dev`
   - User Name: `sa`
   - Password: (leave empty)

#### Database Migrations
```bash
cd backend

# Run database migrations (H2 tables will be created automatically)
./mvnw flyway:migrate -Pdevelopment
```

## Application Startup

### 1. Start Backend
```bash
cd backend

# Run tests first (TDD approach)
./mvnw test

# Start Spring Boot application
./mvnw spring-boot:run -Pdevelopment
```

Backend will be available at: `http://localhost:8080`

### 2. Start Frontend
```bash
cd frontend

# Install dependencies
npm install

# Run tests
npm test

# Start development server
npm run dev
```

Frontend will be available at: `http://localhost:5173`

### 3. Verify Setup
1. Open browser to `http://localhost:5173`
2. Should see Name Draw application homepage
3. Click "Login with Google" or "Login with Facebook"
4. Should redirect to OAuth provider
5. After authentication, should return to application logged in

## Development Workflow

### 1. TDD Process
```bash
# Always start with failing tests
cd backend
./mvnw test  # Should show failing tests

# Implement feature to make tests pass
./mvnw test  # Should show passing tests

# Refactor if needed
./mvnw test  # Should still pass
```

### 2. API Testing
```bash
# Install API testing tools
npm install -g @apidevtools/swagger-parser

# Validate OpenAPI spec
swagger-parser validate specs/001-create-an-application/contracts/api.yaml

# Test API endpoints (after starting backend)
curl -X GET http://localhost:8080/api/v1/draws \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### 3. Frontend Testing
```bash
cd frontend

# Unit tests
npm run test:unit

# Integration tests
npm run test:integration

# E2E tests (requires backend running)
npm run test:e2e
```

## User Story Validation

### Test Scenario 1: Draw Creator Flow
1. **Login**: Visit app, click "Login with Google", authenticate
2. **Create Draw**: Click "Create New Draw", fill form:
   - Title: "Family Gift Exchange"
   - Description: "Christmas 2025 gift exchange"
   - Draw Date: 2025-12-20
   - Max Participants: 10
3. **Invite Participants**: Share draw link with family members
4. **Monitor Joining**: Watch participants join the draw
5. **Open Draw**: When ready, click "Open Draw for Drawing"
6. **Monitor Results**: Watch as participants draw names
7. **View Completion**: See final results when all have drawn

### Test Scenario 2: Participant Flow
1. **Access Draw**: Click shared draw link (must be logged in)
2. **Join Draw**: Click "Join Draw" button
3. **Wait for Opening**: See "Joining" status, wait for organizer to open
4. **Draw Name**: When open, click "Draw Name" button
5. **View Result**: See drawn name and save for reference
6. **Return Later**: Come back to app, still see drawn name

### Test Scenario 3: Edge Cases
1. **Self-Draw Prevention**: Create 2-person draw, verify system re-draws automatically
2. **Concurrent Drawing**: Have multiple users try to draw simultaneously
3. **Full Draw Auto-Start**: Fill draw to 30 participants, verify auto-transition
4. **Single Participant**: Create draw with 1 participant, verify auto-archive on draw date

## Troubleshooting

### Common Issues

#### Backend Won't Start
```bash
# Check Java version
java -version  # Should be 17+

# Check H2 database file permissions
ls -la data/
ls -la data/namedraw_dev.mv.db

# Check environment variables
echo $FACEBOOK_CLIENT_ID
echo $GOOGLE_CLIENT_ID
```

#### Frontend Build Errors
```bash
# Clear node modules and reinstall
rm -rf node_modules package-lock.json
npm install

# Check Node version
node --version  # Should be 18+
```

#### OAuth Redirect Issues
1. Verify redirect URIs in OAuth provider settings
2. Check that URLs match exactly (including http/https)
3. Ensure OAuth apps are in "Live" mode (not sandbox)

#### Database Issues
```bash
# Reset H2 database
rm -rf data/namedraw_dev.mv.db
rm -rf data/namedraw_dev.trace.db

# Re-run migrations
cd backend
./mvnw flyway:clean flyway:migrate -Pdevelopment
```

### Debug Logging
Enable debug logging by adding to `application-dev.yml`:
```yaml
logging:
  level:
    root: INFO
    com.namedraw: DEBUG
    org.springframework.security.oauth2: DEBUG
    org.springframework.web: DEBUG
```

## Performance Validation

### Load Testing
```bash
# Install load testing tool
npm install -g artillery

# Run load tests (with backend running)
artillery run loadtest/api-load-test.yml
```

### Memory Usage Monitoring
```bash
# Monitor backend memory usage
jstat -gc <java-process-id> 1s

# Monitor H2 database size
ls -lh data/namedraw_dev.mv.db

# Check H2 console for active connections
# Open http://localhost:8080/h2-console and run:
# SELECT * FROM INFORMATION_SCHEMA.SESSIONS;
```

## Production Deployment

### Let's Encrypt SSL Setup

#### 1. Domain Configuration
First, ensure your domain is pointing to your server:
```bash
# Verify domain DNS
dig yourdomain.com A
ping yourdomain.com
```

#### 2. Production Docker Compose with Let's Encrypt
Create `docker-compose.prod.yml`:
```yaml
version: '3.8'

services:
  nginx-proxy:
    image: nginxproxy/nginx-proxy:alpine
    container_name: nginx-proxy
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - conf:/etc/nginx/conf.d
      - vhost:/etc/nginx/vhost.d
      - html:/usr/share/nginx/html
      - certs:/etc/nginx/certs:ro
      - /var/run/docker.sock:/tmp/docker.sock:ro
    networks:
      - proxy
    restart: unless-stopped

  nginx-proxy-acme:
    image: nginxproxy/acme-companion
    container_name: nginx-proxy-acme
    environment:
      - DEFAULT_EMAIL=your-email@domain.com
    volumes_from:
      - nginx-proxy
    volumes:
      - certs:/etc/nginx/certs:rw
      - acme:/etc/acme.sh
      - /var/run/docker.sock:/var/run/docker.sock:ro
    networks:
      - proxy
    restart: unless-stopped

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile.prod
    container_name: namedraw-backend
    environment:
      - VIRTUAL_HOST=api.yourdomain.com
      - VIRTUAL_PORT=8080
      - LETSENCRYPT_HOST=api.yourdomain.com
      - LETSENCRYPT_EMAIL=your-email@domain.com
      - SPRING_PROFILES_ACTIVE=prod
      - DATABASE_URL=jdbc:h2:file:/app/data/namedraw;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
      - FACEBOOK_CLIENT_ID=${FACEBOOK_CLIENT_ID}
      - FACEBOOK_CLIENT_SECRET=${FACEBOOK_CLIENT_SECRET}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - JWT_SECRET=${JWT_SECRET}
    volumes:
      - h2_data:/app/data
    networks:
      - proxy
    restart: unless-stopped

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile.prod
    container_name: namedraw-frontend
    environment:
      - VIRTUAL_HOST=yourdomain.com,www.yourdomain.com
      - VIRTUAL_PORT=80
      - LETSENCRYPT_HOST=yourdomain.com,www.yourdomain.com
      - LETSENCRYPT_EMAIL=your-email@domain.com
    networks:
      - proxy
    restart: unless-stopped

volumes:
  conf:
  vhost:
  html:
  certs:
  acme:
  h2_data:

networks:
  proxy:
    external: true
```

#### 3. Deploy with SSL
```bash
# Create external network
docker network create proxy

# Create production environment file
cp .env .env.prod
# Edit .env.prod with production values

# Deploy with Let's Encrypt
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Monitor certificate generation
docker logs nginx-proxy-acme -f
```

#### 4. Verify SSL Setup
```bash
# Check certificate status
curl -I https://yourdomain.com
curl -I https://api.yourdomain.com

# Test SSL rating
curl -s "https://api.ssllabs.com/api/v3/analyze?host=yourdomain.com" | jq '.status'
```

### Docker Deployment
```bash
# Build production images
docker-compose -f docker-compose.prod.yml build

# Deploy with production environment
docker-compose -f docker-compose.prod.yml up -d
```

### Environment Checklist
- [ ] Domain DNS configured and propagated
- [ ] OAuth redirect URIs updated for production domain (HTTPS)
- [ ] JWT secret changed from default
- [ ] Database credentials secured
- [ ] Let's Encrypt certificates configured and validated
- [ ] SSL/TLS security headers configured
- [ ] Error logging configured
- [ ] Performance monitoring enabled
- [ ] Backup strategy implemented

## Next Steps

After successful quickstart:
1. Review API documentation at `/specs/001-create-an-application/contracts/api.yaml`
2. Examine data model at `/specs/001-create-an-application/data-model.md`
3. Follow task list in `/specs/001-create-an-application/tasks.md` (when generated)
4. Set up CI/CD pipeline for automated testing and deployment

## Support

For issues:
1. Check troubleshooting section above
2. Review application logs
3. Consult OpenAPI documentation
4. Check decision records in `/specs/001-create-an-application/memory/`
