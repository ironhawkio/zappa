# GitHub Actions CI/CD Setup Guide

This guide helps you set up automatic building and deployment using GitHub Container Registry with separate develop and production environments.

## Step-by-Step Setup

### 1. Enable GitHub Container Registry

1. **Go to your GitHub repository**
2. **Settings** → **Actions** → **General**
3. **Workflow permissions**: Select "Read and write permissions"
4. **Allow GitHub Actions to create and approve pull requests**: ✅ Check this

### 2. Create GitHub Environments

1. **Repository Settings** → **Environments**
2. **New environment** → Create `develop`
3. **New environment** → Create `production`

### 3. Configure Environment Variables

#### For `develop` environment:
**Variables (non-sensitive):**
```
POSTGRES_DB=zappa_develop
POSTGRES_USER=zappa_dev_user
DB_HOST=postgresql
DB_PORT=5432
SERVER_PORT=6886
```

**Secrets (sensitive):**
```
POSTGRES_PASSWORD=your_develop_password_here
```

#### For `production` environment:
**Variables (non-sensitive):**
```
POSTGRES_DB=zappa_production
POSTGRES_USER=zappa_prod_user
DB_HOST=your-external-db-host.com
DB_PORT=5432
SERVER_PORT=6886
```

**Secrets (sensitive):**
```
POSTGRES_PASSWORD=your_super_secure_production_password
```

### 4. Workflow Files

The setup includes 3 separate workflow files:
- `.github/workflows/develop.yml` - Develop environment deployment
- `.github/workflows/main.yml` - Production environment deployment
- `.github/workflows/pull-request.yml` - PR testing only

### 5. Workflow Triggers

**The workflows run on:**
- **Push to `main`** → Builds and deploys to production
- **Push to `develop`** → Builds and deploys to develop environment
- **Pull Requests** → Builds and tests only (no deployment)

### 6. Image Tagging Strategy

**Develop Branch (`develop.yml`):**
- Creates: `ghcr.io/your-username/zappa:develop-{commit-sha}`
- Example: `ghcr.io/your-username/zappa:develop-abc123`
- Purpose: Version-specific develop images

**Production Branch (`main.yml`):**
- Creates: `ghcr.io/your-username/zappa:latest`
- Purpose: Production-ready image

### 7. What Happens During Deployment

**CI Job (both environments):**
1. ✅ Runs tests with Gradle
2. 🐳 Builds Docker image
3. 📦 Pushes to GitHub Container Registry
4. 🏷️ Tags appropriately (develop-sha or latest)

**CD Job:**
1. 📥 Pulls specific image from registry
2. 🔧 Creates environment-specific `.env` file
3. 🐳 Creates environment-specific docker-compose file
4. 🚀 Deploys using `docker-compose up -d`
5. 🩺 Runs health checks and smoke tests

### 8. Manual Deployment

You can also deploy manually using the scripts:

```bash
# Deploy to develop environment
./scripts/deploy.sh develop

# Deploy to production environment
./scripts/deploy.sh production
```

### 9. Monitoring Deployments

**GitHub Actions:**
- View workflow runs in **Actions** tab
- Check build logs and deployment status
- View container registry in **Packages** tab

**Container Registry:**
- Production images: `ghcr.io/your-username/zappa:latest`
- Develop images: `ghcr.io/your-username/zappa:develop-abc123`

### 10. Environment Setup

**Develop Environment:**
- Uses containerized PostgreSQL
- Debug logging enabled
- Automatic deployment on `develop` branch push

**Production Environment:**
- Can use external database (recommended)
- Production logging levels
- Automatic deployment on `main` branch push

### 11. Security & Best Practices

**Secrets Management:**
- Use GitHub Secrets for passwords
- Different credentials for develop vs production
- Rotate credentials regularly

**External Database (Production):**
- Update `DB_HOST` to your external database
- Remove `postgresql` service from production compose
- Ensure network connectivity between container and database

**Monitoring:**
- Set up application monitoring (logs, metrics)
- Configure alerting for deployment failures
- Monitor container health and resource usage