# Member Profile Service

Backend REST API service for the Dallas Makerspace member portal, built with Kotlin and Ktor.

## Overview

The Member Profile Service provides RESTful APIs for member data management, integrations with external systems (Active Directory, Discourse, WHMCS), and automated synchronization workflows.

**Port:** 8081 (default)

**Tech Stack:**
- Ktor 3.0.1
- Kotlin 2.0.21
- Exposed ORM (database access)
- Dagger (dependency injection)
- MariaDB

## Architecture

### Entry Point

`src/main/kotlin/org/dallasmakerspace/Application.kt`

### Key Packages

- **`di/`** - Dagger dependency injection setup with `@Singleton` scoped `AppComponent`
- **`plugins/`** - Ktor plugin configuration (Routing, Database, HTTP, Serialization)
- **`routing/`** - REST API routes using Ktor Resources
- **`members/`** - Business logic with Observer pattern for member changes
- **`repositories/`** - Data access layer using Exposed ORM
- **`activedirectory/`** - LDAP/AD integration for user synchronization
- **`discourse/`** - Discourse API client for forum integration
- **`core/`** - Database connections (MariaDB), AppConfig, logging
- **`cron/`** - Scheduled jobs (member refresh, sync tasks)
- **`webhook/`** - Webhook routing and handlers
- **`dataviz/`** - Data visualization reports

### Key Design Patterns

1. **Dependency Injection (Dagger)**
   - `@Singleton` scoped components
   - Lazy initialization in routing to avoid circular dependencies

2. **Observer Pattern**
   - `IMemberPropChangeObserver` for tracking member property changes
   - Enables decoupled event handling across the system

3. **Repository + Service Pattern**
   - Clear separation between data access and business logic
   - Exposed ORM with suspend transactions for async operations

4. **Conditional Mocking**
   - `app.use-mock-services` configuration flag
   - Enables testing without external dependencies

5. **Router Pattern**
   - Name-based handler registration for webhooks and data visualization
   - Extensible plugin architecture

## API Authentication

Requests to the API must include:
- `X-Api-Key` - API key for authentication
- `X-Api-Client` - Client identifier

## Configuration

Configuration file: `src/main/resources/application.conf`

Key configuration sections:
- Database connection (MariaDB)
- LDAP/Active Directory settings
- Discourse API credentials
- API keys and client identifiers
- Mock services toggle (`app.use-mock-services`)

## Running the Service

### Start the Service

```bash
./gradlew :service:run
```

The service will start on port 8081 by default.

### Run Tests

```bash
# All tests
./gradlew :service:test

# Specific test class
./gradlew :service:test --tests "org.dallasmakerspace.YourTestClass"
```

### Build

```bash
./gradlew :service:build
```

## Development

### Code Quality

```bash
# Run static analysis
./gradlew :service:detekt

# Generate code coverage report
./gradlew :service:koverHtmlReport
```

Coverage reports are generated in `build/reports/kover/html/index.html`

### IntelliJ IDEA Setup

1. Open the root `member-portal` project in IntelliJ IDEA
2. The service module will be automatically recognized
3. Run configurations can be created for `Application.kt`

### Database Setup

The service requires a MariaDB instance. Configure connection details in `application.conf`:

```
database {
    host = "localhost"
    port = 3306
    name = "member_portal"
    user = "your_user"
    password = "your_password"
}
```

## External Integrations

### Active Directory/LDAP
- User authentication and synchronization
- Group membership management
- Configuration in `application.conf` under `activedirectory`

### Discourse
- Forum SSO integration
- User profile synchronization
- API client in `discourse/` package

### WHMCS/MakerManager
- External member data sources
- Webhook handlers for data updates

## Scheduled Jobs

The service includes cron jobs for:
- Member data refresh
- Active Directory synchronization
- Discourse user sync

Jobs are configured in the `cron/` package.

## API Endpoints

API routes are defined using Ktor Resources in the `routing/` package. Key endpoints include:

- Member CRUD operations
- Group management
- Data visualization reports
- Webhook handlers

See the routing configuration in `plugins/Routing.kt` for the complete API surface.

## Troubleshooting

### Common Issues

**Database connection errors:**
- Verify MariaDB is running
- Check credentials in `application.conf`
- Ensure database exists and migrations are applied

**LDAP/AD connection issues:**
- Verify network connectivity to AD server
- Check credentials and LDAP DN configuration
- Review SSL/TLS settings if applicable

**Mock services not working:**
- Set `app.use-mock-services = true` in `application.conf`
- Restart the service after configuration changes

