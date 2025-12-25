# Zappa Note Management System

A Spring Boot-based note management application with sophisticated note linking, categorization, and graph visualization capabilities.

## Features

- **Note Management**: Create, edit, view, and delete notes with markdown support
- **Group Categorization**: Organize notes into logical groups (Programming, Photography, Planting, General)
- **Note Linking**: Rich relationship system between notes with different link types
- **Tag System**: Tag notes for additional organization with AND/OR filtering
- **Graph Visualization**: Visual representation of note connections (group-specific or global)
- **Advanced Search**: Full-text search across notes with tag-based filtering

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 21 (for local development)
- PostgreSQL (handled by Docker)

### Running with Docker (Recommended)

1. **Start the application stack:**
   ```bash
   docker-compose up -d
   ```

2. **Access the application:**
   - Web UI: http://localhost:6886
   - Database: localhost:5432 (if needed)

3. **Stop the application:**
   ```bash
   docker-compose down
   ```

### Local Development

1. **Prerequisites:**
   ```bash
   # Start PostgreSQL only
   docker-compose up -d postgresql
   ```

2. **Run application locally:**
   ```bash
   ./gradlew bootRun
   ```


## Docker Configuration

### Services

- **zappa-app**: Main Spring Boot application
  - Port: 6886
  - Profile: docker
  - Health checks enabled

- **postgresql**: PostgreSQL 15 database
  - Port: 5432
  - Database: zappa
  - User: zappa_user
  - Password: zappa_password

### Environment Variables

The application supports the following environment variables:

```env
SPRING_PROFILES_ACTIVE=docker
SPRING_DATASOURCE_URL=jdbc:postgresql://postgresql:5432/zappa
SPRING_DATASOURCE_USERNAME=zappa_user
SPRING_DATASOURCE_PASSWORD=zappa_password
JAVA_OPTS=-Xmx512m -Xms256m
```

## Architecture

### Package Structure
```
io.ironhawk.zappa/
├── ZappaApplication.java
└── module/notemgmt/
    ├── entity/          # JPA entities (Note, Group, Tag, NoteLink)
    ├── repository/      # Spring Data repositories
    ├── service/         # Business logic layer
    ├── controller/      # REST API controllers
    └── web/             # Web controllers for Thymeleaf
```

### Database Schema

- **notes**: Main notes table with content and metadata
- **groups**: Hierarchical categorization system
- **tags**: Tagging system for cross-cutting organization
- **note_tags**: Many-to-many relationship between notes and tags
- **note_links**: Rich linking system between notes with metadata

## API Endpoints

### Notes
- `GET /notes` - List notes with filtering options
- `GET /notes/{id}` - View specific note
- `POST /notes` - Create new note
- `PUT /notes/{id}` - Update note

### Graph
- `GET /graph` - Graph visualization UI
- `GET /graph/data` - Graph data API (supports group filtering)

### Groups
- Notes can be filtered by group: `/notes?group={groupId}`
- Graph can be filtered by group: `/graph?group={groupId}`

## Development

### Building

```bash
# Compile
./gradlew compileJava

# Build JAR
./gradlew build

# Run tests
./gradlew test

# Build Docker image
docker-compose build zappa-app
```

### Database Migrations

The application uses Liquibase for database schema management. Migrations are located in `src/main/resources/db/changelog/`.

## Monitoring

Health checks are available at:
- Application health: http://localhost:6886/actuator/health
- Database connectivity is monitored automatically

## Security Features

- Non-root container execution
- Minimal Alpine-based images
- Resource limits configured
- Health checks for reliability