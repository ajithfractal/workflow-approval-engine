# Workflow Engine - Standalone Microservice Mode

## Quick Start

### Prerequisites
- Java 17+
- PostgreSQL 12+
- Maven 3.6+

### Building Standalone JAR

```bash
mvn clean package -Pstandalone
```

This creates an executable JAR: `target/workflow-core-starter-1.0.0-SNAPSHOT.jar`

### Running Standalone

#### Using Java
```bash
java -jar target/workflow-core-starter-1.0.0-SNAPSHOT.jar
```

#### Using Maven
```bash
mvn spring-boot:run -Pstandalone
```

#### Using Environment Variables
```bash
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=workflow_db
export DB_USERNAME=workflow_user
export DB_PASSWORD=workflow_pass
export SERVER_PORT=8080

java -jar target/workflow-core-starter-1.0.0-SNAPSHOT.jar
```

## Configuration

### Database Setup

1. Create PostgreSQL database:
```sql
CREATE DATABASE workflow_db;
CREATE USER workflow_user WITH PASSWORD 'workflow_pass';
GRANT ALL PRIVILEGES ON DATABASE workflow_db TO workflow_user;
```

2. The application will automatically create tables on first run (if `ddl-auto=update`)

### Application Properties

Create `application.yml` or use environment variables:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/workflow_db
    username: workflow_user
    password: workflow_pass
  
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8080
```

## API Endpoints

Once running, access:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs

### Key Endpoints

- `POST /api/applications/register` - Register new application
- `GET /api/applications` - List applications (admin)
- `GET /api/users` - List users (requires schema header)
- `GET /api/workflow-definitions` - List workflows
- `GET /api/tasks` - List tasks

## Keycloak Integration

The standalone mode requires the Keycloak dependency (`fractalhive-spring-boot-starter-keycloak`).

### Add Dependency

For standalone mode, include in `pom.xml`:

```xml
<dependency>
    <groupId>com.fractalhive</groupId>
    <artifactId>fractalhive-spring-boot-starter-keycloak</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Keycloak Dependency Responsibilities

The Keycloak dependency should:

1. Handle authentication
2. Set request attributes:
   - `roles` (List<String>) - User roles
   - `schemaName` (String) - Tenant schema name

See `docs/KEYCLOAK_ADMIN_CLAIMS.md` for details.

## Docker Deployment

### Dockerfile Example

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/workflow-core-starter-*.jar app.jar

EXPOSE 8080

ENV DB_HOST=localhost
ENV DB_PORT=5432
ENV DB_NAME=workflow_db
ENV SERVER_PORT=8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build and Run

```bash
docker build -t workflow-engine .
docker run -p 8080:8080 \
  -e DB_HOST=postgres \
  -e DB_NAME=workflow_db \
  -e DB_USERNAME=workflow_user \
  -e DB_PASSWORD=workflow_pass \
  workflow-engine
```

## Health Check

The application includes Spring Boot Actuator endpoints (if added):

- `/actuator/health` - Health check
- `/actuator/info` - Application info

## Logging

Logs are written to console by default. Configure in `application.yml`:

```yaml
logging:
  level:
    com.fractalhive.workflowcore: INFO
    org.springframework.statemachine: WARN
```

## Troubleshooting

### Port Already in Use
```bash
# Change port
export SERVER_PORT=8081
java -jar app.jar
```

### Database Connection Failed
- Verify PostgreSQL is running
- Check database credentials
- Ensure database exists

### Schema Not Found
- Ensure Keycloak dependency sets `schemaName` attribute
- Check `X-Schema-Name` header is set
- Verify application is registered
