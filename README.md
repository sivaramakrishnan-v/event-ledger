# Event Ledger

Event Ledger is a Java 17, Spring Boot 3.5 multi-module Maven project with two services:

- `event-gateway-service`: accepts ledger events, stores event records, and forwards account transactions to the account service.
- `account-service`: applies credit and debit transactions, stores account state, and exposes account balances.

The project uses in-memory H2 databases for both services, Spring Boot Actuator for health and metrics endpoints, Resilience4j for account-service calls from the gateway, Micrometer tracing, and structured JSON logging.

## Project Structure

```text
event-ledger/
|-- pom.xml                         # Parent Maven project
|-- docker-compose.yml              # Runs both services together
|-- account-service/
|   |-- Dockerfile
|   |-- pom.xml
|   `-- src/
`-- event-gateway-service/
    |-- Dockerfile
    |-- pom.xml
    `-- src/
```

## Requirements

Install the following before running the project locally:

- Java 17
- Maven 3.9 or newer
- Docker Desktop or Docker Engine with Docker Compose

Verify your local tools:

```powershell
java -version
mvn -version
docker --version
docker compose version
```

This repository does not currently include Maven wrapper scripts such as `mvnw` or `mvnw.cmd`, so Maven must be installed and available on your `PATH`.

## Services and Ports

| Service | Port | Base URL | Description |
| --- | ---: | --- | --- |
| Event Gateway Service | 8080 | `http://localhost:8080` | Public event ingestion API |
| Account Service | 8081 | `http://localhost:8081` | Account balance and transaction API |

Both services expose actuator endpoints at the root management path:

- `GET /health`
- `GET /info`
- `GET /metrics`

## Configuration

### Account Service

Default configuration is in `account-service/src/main/resources/application.properties`.

Important values:

```properties
server.port=8081
spring.datasource.url=jdbc:h2:mem:account_service;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
spring.h2.console.path=/h2-console
management.endpoints.web.base-path=/
management.endpoints.web.exposure.include=health,info,metrics
```

### Event Gateway Service

Default configuration is in `event-gateway-service/src/main/resources/application.properties`.

Important values:

```properties
server.port=8080
account-service.base-url=http://localhost:8081
account-service.connect-timeout=2s
account-service.read-timeout=3s
management.endpoints.web.base-path=/
management.endpoints.web.exposure.include=health,info,metrics
```

When running through Docker Compose, `ACCOUNT_SERVICE_BASE_URL=http://account-service:8081` is passed to the gateway so it can reach the account service by container name.

## Run Tests with Maven

From the repository root, run all module tests:

```powershell
mvn test
```

Run tests for one module:

```powershell
mvn -pl account-service test
mvn -pl event-gateway-service test
```

Run a full clean build for all modules:

```powershell
mvn clean package
```

Skip tests while packaging:

```powershell
mvn clean package -DskipTests
```

Maven Surefire test reports are written under each module:

```text
account-service/target/surefire-reports/
event-gateway-service/target/surefire-reports/
```

## Run Locally Without Docker

Open two terminals from the repository root.

Terminal 1:

```powershell
mvn -pl account-service spring-boot:run
```

Terminal 2:

```powershell
mvn -pl event-gateway-service spring-boot:run
```

The account service must be running before submitting events through the gateway because the gateway forwards every new event to the account service.

## Run with Docker Compose

Build and start both services:

```powershell
docker compose up --build
```

Run in the background:

```powershell
docker compose up --build -d
```

View logs:

```powershell
docker compose logs -f
```

View logs for one service:

```powershell
docker compose logs -f account-service
docker compose logs -f event-gateway-service
```

Check container status:

```powershell
docker compose ps
```

Stop services:

```powershell
docker compose down
```

Stop services and remove anonymous volumes:

```powershell
docker compose down -v
```

## Docker Images

Each service Dockerfile uses a multi-stage build:

1. `maven:3.9.9-eclipse-temurin-17` builds the application.
2. `eclipse-temurin:17-jre-jammy` runs the packaged Spring Boot jar.

The Dockerfiles package the full parent Maven project so both modules are built during image creation.

Build a single image manually:

```powershell
docker build -f account-service/Dockerfile -t event-ledger-account-service .
docker build -f event-gateway-service/Dockerfile -t event-ledger-gateway-service .
```

## API Usage

### Health Checks

```powershell
curl http://localhost:8081/health
curl http://localhost:8080/health
```

### Submit an Event

Submit a credit event through the gateway:

```powershell
curl -X POST http://localhost:8080/events `
  -H "Content-Type: application/json" `
  -d '{
    "eventId": "evt-001",
    "accountId": "acct-001",
    "type": "CREDIT",
    "amount": 100.00,
    "currency": "USD",
    "eventTimestamp": "2026-07-15T12:00:00Z",
    "metadata": {
      "source": "manual-test"
    }
  }'
```

Submit a debit event:

```powershell
curl -X POST http://localhost:8080/events `
  -H "Content-Type: application/json" `
  -d '{
    "eventId": "evt-002",
    "accountId": "acct-001",
    "type": "DEBIT",
    "amount": 25.00,
    "currency": "USD",
    "eventTimestamp": "2026-07-15T12:05:00Z",
    "metadata": {
      "source": "manual-test"
    }
  }'
```

Valid event types are:

- `CREDIT`
- `DEBIT`

### Read Events

Get one event by event ID:

```powershell
curl http://localhost:8080/events/evt-001
```

Get all events for an account:

```powershell
curl "http://localhost:8080/events?account=acct-001"
```

### Read Account Data

Get account balance:

```powershell
curl http://localhost:8081/accounts/acct-001/balance
```

Get account details and transactions:

```powershell
curl http://localhost:8081/accounts/acct-001
```

### Apply a Transaction Directly

Most clients should submit events through the gateway. For direct account-service testing:

```powershell
curl -X POST http://localhost:8081/accounts/acct-002/transactions `
  -H "Content-Type: application/json" `
  -d '{
    "eventId": "evt-direct-001",
    "type": "CREDIT",
    "amount": 50.00,
    "currency": "USD",
    "eventTimestamp": "2026-07-15T12:10:00Z"
  }'
```

## Runtime Behavior

- Events are idempotent by `eventId`.
- Re-submitting the same `eventId` with the same event data returns the existing event.
- Re-submitting the same `eventId` with different data returns a conflict.
- Account transactions are also idempotent by `eventId`.
- Account balances are recalculated in chronological transaction order.
- Each service uses an in-memory H2 database, so data is lost when the service restarts.
- The gateway uses a circuit breaker around account-service calls.

## H2 Console

The H2 console is enabled for both services:

- Account service: `http://localhost:8081/h2-console`
- Event gateway service: `http://localhost:8080/h2-console`

Connection settings:

| Service | JDBC URL | User | Password |
| --- | --- | --- | --- |
| Account Service | `jdbc:h2:mem:account_service` | `sa` | blank |
| Event Gateway Service | `jdbc:h2:mem:event_gateway_service` | `sa` | blank |

## Troubleshooting

### `mvn` is not recognized

Install Maven and make sure its `bin` directory is on your `PATH`, then open a new terminal and run:

```powershell
mvn -version
```

### Docker build cannot download dependencies

Check internet access and Maven Central availability, then rebuild:

```powershell
docker compose build --no-cache
```

### Gateway cannot reach account service

If running locally without Docker, make sure the account service is running on port `8081`.

If running with Docker Compose, check service health and logs:

```powershell
docker compose ps
docker compose logs account-service
docker compose logs event-gateway-service
```

### Port already in use

Stop the process using port `8080` or `8081`, or change the service port in the relevant `application.properties` file.

### Health check fails in Docker

The compose file checks account service health at:

```text
http://localhost:8081/health
```

If the gateway does not start, inspect the account-service logs first:

```powershell
docker compose logs account-service
```

## Useful Commands

```powershell
# Clean all Maven build output
mvn clean

# Run all tests
mvn test

# Package all modules
mvn clean package

# Start both services with Docker
docker compose up --build

# Start both services in the background
docker compose up --build -d

# Stop Docker services
docker compose down

# Show Docker logs
docker compose logs -f
```
