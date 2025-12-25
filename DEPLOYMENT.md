# Zappa Deployment Guide

## Externalized Configuration

The application uses externalized configuration for flexible deployments across different environments.

## Environment Files

### `.env` (Development)
Default configuration for local development with Docker Compose.

### `.env.example`
Template showing all available configuration options.

### `.env.production`
Production-ready configuration template.

## Quick Start

### 1. Local Development
```bash
# Uses .env file automatically
docker-compose up -d
```

### 2. Develop Environment
```bash
# Copy and customize develop config
cp .env.example .env.develop
# Edit .env.develop with your develop values

# Use develop environment file
docker-compose --env-file .env.develop up -d
```

### 3. Production Deployment
```bash
# Copy and customize production config
cp .env.example .env.production
# Edit .env.production with your production values

# Use specific environment file
docker-compose --env-file .env.production up -d
```

## Configuration Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `POSTGRES_DB` | Database name | `zappa`, `zappa_prod` |
| `POSTGRES_USER` | Database username | `zappa_user` |
| `POSTGRES_PASSWORD` | Database password | `secure_password` |
| `DB_HOST` | Database host | `postgresql`, `external-db.com` |
| `DB_PORT` | Database port | `5432` |
| `SERVER_PORT` | Application port | `6886`, `80` |

## Security Notes

- **Never commit** `.env` files to version control
- **Use strong passwords** in production
- **Rotate credentials** regularly
- **Use external secret management** for production (AWS Secrets Manager, etc.)

## External Database

To use an external database instead of the containerized PostgreSQL:

1. **Update .env file:**
   ```bash
   DB_HOST=your-external-db.com
   POSTGRES_PASSWORD=your_external_db_password
   ```

2. **Remove PostgreSQL service:**
   ```bash
   # Comment out or remove postgresql service from docker-compose.yml
   # Only run the application
   docker-compose up -d zappa-app
   ```

## Health Monitoring

Check application health:
```bash
curl http://localhost:6886/actuator/health
```

View container status:
```bash
docker-compose ps
```

## Logs

View application logs:
```bash
docker-compose logs -f zappa-app
```

View database logs:
```bash
docker-compose logs -f postgresql
```