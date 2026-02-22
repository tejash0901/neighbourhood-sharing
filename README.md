# Neighborhood Asset Sharing Platform - Backend

Production-grade backend for apartment residents to share and rent items.

## Tech Stack

- **Java 21** + Spring Boot 3.2
- **PostgreSQL 14+** - Main database
- **Redis** - Caching layer
- **AWS S3** - Image storage
- **Stripe** - Payment processing
- **JWT** - Authentication
- **Docker** - Containerization

## Project Structure

```
src/main/java/com/neighborshare/
├── config/              # Configuration classes (Security, Cache, JWT, etc)
├── domain/
│   ├── entity/         # JPA entities (User, Item, Booking, etc)
│   ├── repository/     # Repository interfaces
│   └── valueobject/    # Value objects (BookingStatus, etc)
├── service/            # Business logic services
├── controller/         # REST controllers
├── dto/                # Data Transfer Objects
├── exception/          # Custom exceptions
├── security/           # Security utilities
├── integration/        # External integrations (Stripe, AWS, etc)
├── event/              # Event definitions
├── listener/           # Event listeners
└── util/               # Utility classes
```

## Quick Start

### Prerequisites

- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Option 1: Docker Compose (Recommended for Development)

```bash
# Clone the repository
cd neighborhood-sharing

# Create .env file from example
cp .env.example .env

# Start all services
docker-compose up

# The application will be available at http://localhost:8080
# PostgreSQL: localhost:5432
# Redis: localhost:6379
# LocalStack: localhost:4566
```

### Option 2: Local Development Setup

```bash
# 1. Install PostgreSQL 14+
# 2. Create database
createdb neighborshare

# 3. Install Redis
# macOS: brew install redis
# Ubuntu: sudo apt-get install redis-server

# 4. Set environment variables
export DB_HOST=localhost
export REDIS_HOST=localhost
export JWT_SECRET=your-super-secret-key-min-256-bits

# 5. Build and run
mvn clean install
mvn spring-boot:run
```

## API Endpoints (Phase 0 Foundation)

The foundation is set up. Endpoints will be built in Phase 1:

- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/send-otp` - Send OTP
- `POST /api/v1/auth/verify-otp` - Verify OTP

(Full endpoint list in architecture plan)

## Database Migrations

Migrations are automatically applied via Flyway:

```sql
-- Schema is in: src/main/resources/db/migration/V1__initial_schema.sql
```

New migrations should be added as:
- `V2__add_fraud_tables.sql`
- `V3__add_audit_logging.sql`
- etc.

## Configuration

### Application Profiles

- **dev** - Development (very verbose logging, hot reload)
- **prod** - Production (optimized, security hardened)

Switch profiles:
```bash
export SPRING_PROFILES_ACTIVE=prod
mvn spring-boot:run
```

### JWT Configuration

```yaml
jwt:
  secret: ${JWT_SECRET}  # Min 256-bit hex string
  expiration: 900        # 15 minutes
  refresh-token-expiration: 604800  # 7 days
```

## Build & Deploy

### Docker Build

```bash
docker build -t neighborshare-api:latest .

docker run -p 8080:8080 \
  -e DB_HOST=postgres \
  -e REDIS_HOST=redis \
  neighborshare-api:latest
```

### Maven Build

```bash
mvn clean package

java -jar target/neighborhood-sharing-0.1.0.jar
```

## Testing

```bash
# Unit tests only
mvn test

# Integration tests
mvn verify

# With coverage
mvn test jacoco:report
```

## Security Checklist

- [ ] JWT_SECRET configured (min 256 bits)
- [ ] Database credentials in environment variables
- [ ] CORS configured for your frontend domain
- [ ] HTTPS/TLS enabled in production
- [ ] Database backups configured
- [ ] Redis persistence enabled
- [ ] Stripe keys in production environment
- [ ] AWS credentials for S3 rotated

## Monitoring & Debugging

### Health Check

```bash
curl http://localhost:8080/api/actuator/health
```

### Metrics

```bash
curl http://localhost:8080/api/actuator/metrics
```

### Logs

```bash
# Docker
docker logs neighborshare-app -f

# Local
tail -f logs/application.log
```

## Common Issues

### Port Already in Use

```bash
# Find and kill process on port 8080
lsof -ti:8080 | xargs kill -9

# Use different port
export SERVER_PORT=8081
```

### Database Connection Failed

```bash
# Check PostgreSQL is running
psql -U postgres -d neighborshare -c "SELECT 1"

# Reset migrations (careful!)
DELETE FROM flyway_schema_history;
```

### Redis Connection Failed

```bash
# Check Redis is running and accessible
redis-cli ping
```

## Next Steps (Phase 1-3)

- [ ] Implement User Service
- [ ] Implement Item Service
- [ ] Create REST Controllers
- [ ] Add Stripe Integration
- [ ] Implement Booking Service
- [ ] Add Review System
- [ ] Create Admin Dashboard
- [ ] Add Image Upload/Processing
- [ ] Set up CI/CD Pipeline

## Contributing

1. Follow the existing code structure
2. Write tests for new features
3. Run `mvn clean verify` before committing
4. Follow Java naming conventions

## License

Proprietary - NeighborhoodShare Inc.

## Support

For issues or questions, refer to the architecture documentation in `/docs/ARCHITECTURE.md`
