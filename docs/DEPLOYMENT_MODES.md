# Workflow Engine Deployment Modes

## Overview

The workflow engine supports two deployment modes:

1. **Dependency Mode** - Library that can be included in other Spring Boot applications
2. **Standalone Microservice Mode** - Independent microservice that runs on its own

## Mode Selection

The mode is determined automatically based on how the application is built and run:

- **Dependency Mode**: Default Maven build (library JAR)
- **Standalone Mode**: Build with `-Pstandalone` profile (executable JAR)

## Dependency Mode

### Usage

Include the workflow engine as a dependency in your Spring Boot application:

```xml
<dependency>
    <groupId>com.fractalhive</groupId>
    <artifactId>workflow-core-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Building

```bash
mvn clean install
# Produces: workflow-core-starter-1.0.0-SNAPSHOT.jar (library)
```

### Configuration

The consuming application must provide:
- Database configuration (PostgreSQL)
- Keycloak integration (external dependency)
- Application properties

### Auto-Configuration

All workflow engine components are auto-configured via `WorkflowCoreAutoConfiguration` when the dependency is included.

### Example Consumer Application

```java
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

The workflow engine APIs are automatically available at `/api/*` endpoints.

## Standalone Microservice Mode

### Building Standalone JAR

```bash
mvn clean package -Pstandalone
# Produces: workflow-core-starter-1.0.0-SNAPSHOT.jar (executable)
```

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

### Required Dependencies

For standalone mode, include in `pom.xml`:

```xml
<dependency>
    <groupId>com.fractalhive</groupId>
    <artifactId>fractalhive-spring-boot-starter-keycloak</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Configuration

Standalone mode uses `application-standalone.properties` which can be overridden:

1. **Environment Variables** (recommended for production)
2. **application.yml** or **application.properties** in working directory
3. **Command line arguments**: `java -jar app.jar --server.port=8081`

### Default Configuration

- **Port**: 8080
- **Database**: PostgreSQL (localhost:5432/workflow_db)
- **Swagger UI**: Enabled at `/swagger-ui.html`
- **API Docs**: Available at `/api-docs`

## Differences Between Modes

| Feature | Dependency Mode | Standalone Mode |
|---------|----------------|-----------------|
| **Main Class** | Not included | `WorkflowEngineApplication` |
| **Packaging** | JAR (library) | JAR (executable) |
| **Configuration** | Provided by consumer | Self-contained |
| **Database** | Consumer's database | Own database |
| **Port** | Consumer's port | Configurable (default: 8080) |
| **Swagger UI** | Optional | Enabled by default |
| **Build Profile** | `dependency` (default) | `standalone` |
| **Spring Boot Plugin** | Skipped | Enabled |

## Maven Profiles

### Dependency Profile (Default)
```bash
mvn clean install
# or explicitly
mvn clean install -Pdependency
```

- Skips Spring Boot repackaging
- Produces library JAR
- Used when including as dependency

### Standalone Profile
```bash
mvn clean package -Pstandalone
```

- Enables Spring Boot repackaging
- Produces executable JAR
- Includes main class
- Used for standalone deployment

## Configuration Files

### Dependency Mode
- Uses `application.properties` (library defaults)
- Consumer provides actual configuration

### Standalone Mode
- Uses `application-standalone.properties` (defaults)
- Can override with `application.yml` or environment variables

## API Endpoints

Both modes expose the same REST APIs:

- `POST /api/applications/register` - Register new application
- `GET /api/applications` - List applications (admin)
- `GET /api/applications/{appId}` - Get application (admin)
- `PUT /api/applications/{appId}` - Update application (admin)
- `DELETE /api/applications/{appId}` - Deactivate application (admin)
- `GET /api/users` - List users (requires schema header)
- `GET /api/workflow-definitions` - List workflows
- `POST /api/workflow-definitions` - Create workflow
- `GET /api/work-items` - List work items
- `GET /api/tasks` - List tasks
- `GET /swagger-ui.html` - Swagger UI (standalone mode)

## Keycloak Integration

Both modes require the Keycloak dependency (`fractalhive-spring-boot-starter-keycloak`) to:

1. Handle authentication
2. Set request attributes:
   - `roles` (List<String>) - User roles (for admin check)
   - `schemaName` (String) - Tenant schema name

See `docs/KEYCLOAK_ADMIN_CLAIMS.md` for details.

## Docker Deployment (Standalone Mode)

### Dockerfile Example

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/workflow-core-starter-*.jar app.jar

EXPOSE 8080

ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=workflow_db
ENV SERVER_PORT=8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Build and Run

```bash
# Build standalone JAR
mvn clean package -Pstandalone

# Build Docker image
docker build -t workflow-engine .

# Run container
docker run -p 8080:8080 \
  -e DB_HOST=postgres \
  -e DB_NAME=workflow_db \
  -e DB_USERNAME=workflow_user \
  -e DB_PASSWORD=workflow_pass \
  workflow-engine
```

## Environment Variables (Standalone Mode)

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `workflow_db` |
| `DB_USERNAME` | Database username | `workflow_user` |
| `DB_PASSWORD` | Database password | `workflow_pass` |
| `SERVER_PORT` | Server port | `8080` |
| `DDL_AUTO` | Hibernate DDL mode | `update` |
| `SHOW_SQL` | Show SQL queries | `false` |
| `FORMAT_SQL` | Format SQL queries | `false` |
| `STATEMACHINE_LOG_LEVEL` | State machine log level | `WARN` |

## Troubleshooting

### Dependency Mode Issues

**Problem:** Components not auto-configured

**Solution:**
- Ensure `WorkflowCoreAutoConfiguration` is in classpath
- Check `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Verify Spring Boot auto-configuration is enabled

### Standalone Mode Issues

**Problem:** JAR not executable

**Solution:**
- Build with `-Pstandalone` profile
- Verify Spring Boot Maven plugin is configured
- Check main class is set correctly

**Problem:** Port already in use

**Solution:**
```bash
export SERVER_PORT=8081
java -jar app.jar
```

**Problem:** Database connection failed

**Solution:**
- Verify PostgreSQL is running
- Check database credentials
- Ensure database exists
- Verify connection URL format

## Migration Guide

### From Dependency to Standalone

1. Build standalone JAR:
   ```bash
   mvn clean package -Pstandalone
   ```

2. Create `application.yml` with your configuration

3. Run the JAR:
   ```bash
   java -jar target/workflow-core-starter-*.jar
   ```

### From Standalone to Dependency

1. Include dependency in your `pom.xml`

2. Remove standalone-specific configuration

3. Configure database in your application's properties

4. Ensure Keycloak dependency is included

## Best Practices

### Dependency Mode
- Use when integrating into existing applications
- Share database with host application
- Leverage host application's security/authentication

### Standalone Mode
- Use for dedicated workflow engine deployments
- Isolated database for better security
- Easier to scale independently
- Better for microservices architecture
