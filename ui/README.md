# Member Profile UI

Web frontend for the Dallas Makerspace member portal with SSO authentication and member management interface, built with Kotlin, Ktor, and Thymeleaf.

## Current Status

WIP: This project is currently in development. The current version is a simple webapp with some static content and SSO Login. The next step is to allow users to link their discourse account.

## Overview

The Member Profile UI provides a web interface for members to view and manage their profiles, group memberships, and access various member portal features. It integrates with the Member Profile Service backend and provides OAuth/OIDC authentication.

**Port:** 8000 (default)

**Current Features:**
- List / Connect discourse (talk.dallasmakerspace.org) account

**Tech Stack:**
- Ktor 2.3.5
- Kotlin 1.9.23
- Thymeleaf (templating engine)
- Bootstrap (UI framework)
- Dagger (dependency injection)
- OAuth/OIDC (Keycloak)

## Architecture

### Entry Point

`src/main/java/org/dallasmakerspace/server/MemberProfileUiApplication.kt`

### Key Packages

- **`server/di/`** - Dagger dependency injection with route handler multi-binding via `@IntoMap`
- **`server/routes/`** - HTTP request handlers implementing `IRouteHandler`
- **`server/plugins/`** - Ktor plugins (Templating, Sessions, HTTP, Routing)
- **`server/auth/`** - OAuth/OIDC authentication flow
- **`server/memberservice/`** - Backend API client for Member Profile Service
- **`server/discourse/`** - Discourse SSO integration
- **`server/common/`** - AppConfig, DMSHttpClient, logging utilities
- **`server/models/`** - Domain models (DMSMember, DMSGroup)
- **`thymeleaf/`** - Template utilities and custom processors

### Key Design Patterns

1. **AuthRouteHandler Base Class**
   - Base class for protected routes
   - Automatic session validation
   - Redirects to login if session is invalid

2. **Route Factory Pattern**
   - Enum defining all application paths
   - Centralized route management with parameter templates
   - Type-safe route generation

3. **Session-based Authentication**
   - OAuth/OIDC integration with Keycloak
   - Server-side session storage
   - Automatic token refresh

4. **Dependency Injection with Multi-binding**
   - Route handlers registered via `@IntoMap`
   - Automatic route registration at startup
   - Extensible handler architecture

5. **Thymeleaf Template Resolution**
   - FileResolver for development (hot reload)
   - ClassLoaderResolver for production (embedded templates)
   - Fragment-based composition for reusability

## Templates

Templates are located in `src/main/resources/templates/`

### Main Templates

- **`index.html`** - Home page / dashboard
- **`profile.html`** - Member profile view
- **`group.html`** - Group details and member list
- **`reports.html`** - Data visualization and reports

### Fragments

Reusable template fragments in `fragments/`:
- Navigation bars
- Header/footer
- Common UI components

### Template Development

During development, templates can be edited and reloaded without restarting the server (when using FileResolver).

## Configuration

Configuration file: `src/main/resources/application.conf`

Key configuration sections:
- OAuth/OIDC provider settings (Keycloak)
- Backend service URL (Member Profile Service)
- Session configuration
- Discourse SSO credentials

## Running the UI

### Start the UI

```bash
./gradlew :ui:run
```

The UI will start on port 8000 by default. Navigate to `http://localhost:8000` in your browser.

### Run Tests

```bash
# All tests
./gradlew :ui:test

# Specific test class
./gradlew :ui:test --tests "org.dallasmakerspace.YourTestClass"
```

### Build

```bash
./gradlew :ui:build
```

## Development

### Code Quality

```bash
# Run static analysis
./gradlew :ui:detekt

# Generate code coverage report
./gradlew :ui:koverHtmlReport
```

Coverage reports are generated in `build/reports/kover/html/index.html`

### IntelliJ IDEA Setup

1. Open the root `member-portal` project in IntelliJ IDEA
2. The UI module will be automatically recognized
3. Run configurations can be created for `MemberProfileUiApplication.kt`

### Backend Service Dependency

The UI requires the Member Profile Service to be running. By default, it connects to `http://localhost:8081`.

To run both services locally:

1. Start the backend service:
   ```bash
   ./gradlew :service:run
   ```

2. In a separate terminal, start the UI:
   ```bash
   ./gradlew :ui:run
   ```

## Authentication

### OAuth/OIDC (Keycloak)

The UI uses OAuth/OIDC for authentication via Keycloak. Configuration in `application.conf`:

```
oauth {
    clientId = "your_client_id"
    clientSecret = "your_client_secret"
    authorizeUrl = "https://keycloak.example.com/auth"
    tokenUrl = "https://keycloak.example.com/token"
}
```

### Session Management

- Sessions are stored server-side
- Session timeout configurable in `application.conf`
- Automatic redirect to login on session expiration

### Discourse SSO

The UI handles Discourse SSO integration for seamless forum access. SSO endpoints are defined in `server/discourse/` package.

## Frontend Assets

Static assets (CSS, JavaScript, images) are served from:
- `src/main/resources/static/`

### Asset Organization

- `css/` - Stylesheets
- `js/` - JavaScript files
- `images/` - Images and icons

## Route Handlers

Route handlers are defined in `server/routes/` and automatically registered via Dagger multi-binding.

### Creating a New Route Handler

1. Implement `IRouteHandler` interface
2. Extend `AuthRouteHandler` if authentication is required
3. Annotate with `@IntoMap` and `@StringKey` for automatic registration

Example:

```kotlin
@Singleton
@IntoMap
@StringKey("/my-route")
class MyRouteHandler @Inject constructor(
    private val memberService: IMemberService
) : AuthRouteHandler() {
    override suspend fun handle(call: ApplicationCall, session: UserSession) {
        // Handler implementation
    }
}
```

## Troubleshooting

### Common Issues

**Cannot connect to backend service:**
- Verify the Member Profile Service is running on port 8081
- Check the service URL in `application.conf`
- Review network/firewall settings

**OAuth/OIDC authentication errors:**
- Verify Keycloak is running and accessible
- Check client ID and secret in configuration
- Ensure redirect URIs are properly configured in Keycloak

**Template rendering errors:**
- Check template file paths and names
- Verify template syntax (Thymeleaf)
- Review template fragment references

**Session issues:**
- Clear browser cookies
- Check session timeout configuration
- Verify session storage is properly configured
