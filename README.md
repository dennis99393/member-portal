# Dallas Makerspace Member Portal

A Gradle multi-module monorepo containing the Dallas Makerspace member portal applications, built with Kotlin and Ktor.

## Overview

This repository contains two interconnected applications:

- **[Member Profile Service](service/)** - Backend REST API service providing member data and integrations
- **[Member Profile UI](ui/)** - Web frontend with SSO authentication and member management interface

## Tech Stack

- **Language:** Kotlin
- **Framework:** Ktor
- **Build System:** Gradle (Multi-module)
- **Database:** MariaDB
- **DI Framework:** Dagger
- **Authentication:** OAuth/OIDC (Keycloak)
- **Logging:** Elasticsearch

## Quick Start

### Prerequisites

- JDK 21
- IntelliJ IDEA Community Edition (recommended for development)
- MariaDB (for local development)

### Development Setup

1. **Install IntelliJ IDEA Community Edition**
   - Download from https://www.jetbrains.com/idea/download/

2. **Open the Project**
   - Launch IntelliJ IDEA
   - File → Open → Select the `member-portal` directory
   - Import as Gradle project
   - Both modules (service and ui) will be automatically recognized

3. **Configure JDK**
   - The project uses JDK 21 (configured in `gradle.properties`)
   - IntelliJ will prompt you to download if not already installed

### Build All Projects

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Run Individual Modules

**Backend Service (Port 8081):**
```bash
./gradlew :service:run
```

**Frontend UI (Port 8000):**
```bash
./gradlew :ui:run
```

## Project Structure

```
member-portal/
├── service/           # Backend REST API
│   ├── src/
│   ├── build.gradle.kts
│   └── README.md     # Service-specific documentation
├── ui/               # Web frontend
│   ├── src/
│   ├── build.gradle.kts
│   └── README.md     # UI-specific documentation
├── config/           # Shared configuration
│   └── detekt/       # Static analysis rules
├── build.gradle.kts  # Root build configuration
└── settings.gradle.kts
```

## Module Documentation

For detailed information about each module:

- **[Service Documentation](service/README.md)** - API endpoints, backend architecture, data models
- **[UI Documentation](ui/README.md)** - Frontend architecture, templates, authentication

## Common Commands

### Building

```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew :service:build
./gradlew :ui:build

# Clean build artifacts
./gradlew clean
```

### Testing

```bash
# Run all tests
./gradlew test

# Run module-specific tests
./gradlew :service:test
./gradlew :ui:test

# Run specific test class
./gradlew :service:test --tests "org.dallasmakerspace.YourTestClass"

# Generate code coverage report
./gradlew :service:koverHtmlReport
./gradlew :ui:koverHtmlReport
```

### Code Quality

```bash
# Run static analysis (detekt)
./gradlew detekt

# Module-specific analysis
./gradlew :service:detekt
./gradlew :ui:detekt
```

### List Available Tasks

```bash
./gradlew tasks
```

## External Integrations

The member portal integrates with:

- **Active Directory/LDAP** - User authentication and group synchronization
- **Discourse** - Forum SSO integration
- **Keycloak** - OAuth/OIDC identity provider
- **MariaDB** - Primary database
- **Elasticsearch** - Centralized logging
- **WHMCS/MakerManager** - External member data sources

## Deployment

Production deployment is automated using GitHub Actions. Commits to the main branch trigger the CI/CD pipeline which builds, tests, and deploys the applications automatically.

## Contributing

Pull requests are welcome.
