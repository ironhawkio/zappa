#!/bin/bash

# Zappa Deployment Script
# Usage: ./scripts/deploy.sh [dev|prod]

set -e

ENVIRONMENT=${1:-dev}
COMPOSE_FILE=""

echo "🚀 Deploying Zappa to $ENVIRONMENT environment..."

# Determine which compose file to use
case $ENVIRONMENT in
  "dev")
    COMPOSE_FILE="docker-compose.dev.yml"
    ENV_FILE=".env.dev"
    ;;
  "prod")
    COMPOSE_FILE="docker-compose.prod.yml"
    ENV_FILE=".env.prod"
    ;;
  *)
    echo "❌ Error: Environment must be 'dev' or 'prod'"
    echo "Usage: $0 [dev|prod]"
    exit 1
    ;;
esac

# Check if environment file exists
if [[ ! -f "$ENV_FILE" ]]; then
    echo "❌ Error: Environment file $ENV_FILE not found"
    echo "💡 Create it from: cp .env.example $ENV_FILE"
    exit 1
fi

echo "📁 Using compose file: $COMPOSE_FILE"
echo "🔧 Using environment file: $ENV_FILE"

# Load environment variables
export $(cat $ENV_FILE | grep -v '^#' | xargs)

# Set Docker Compose variables
export REGISTRY="ghcr.io"
export IMAGE_NAME="your-username/zappa"  # Replace with your GitHub username/repo

case $ENVIRONMENT in
  "dev")
    export IMAGE_TAG="dev-latest"  # Or specific SHA for exact version
    ;;
  "prod")
    export IMAGE_TAG="latest"
    ;;
esac

echo "🐳 Using image: $REGISTRY/$IMAGE_NAME:$IMAGE_TAG"

# Pull latest images (if using registry)
if [[ "$ENVIRONMENT" == "prod" ]]; then
    echo "🐳 Pulling latest images from registry..."
    docker compose -f $COMPOSE_FILE pull || echo "⚠️  Pull failed, continuing with local images"
fi

# Stop existing containers
echo "🛑 Stopping existing containers..."
docker compose -f $COMPOSE_FILE down || true

# Start services
echo "▶️  Starting services..."
docker compose -f $COMPOSE_FILE up -d

# Wait for health check
echo "🔍 Waiting for application to be healthy..."
timeout 120 bash -c "
    while ! curl -sf http://localhost:${SERVER_PORT:-6886}/actuator/health > /dev/null; do
        echo '⏳ Waiting for application...'
        sleep 5
    done
"

# Verify deployment
echo "✅ Deployment verification..."
HEALTH_STATUS=$(curl -s http://localhost:${SERVER_PORT:-6886}/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)

if [[ "$HEALTH_STATUS" == "UP" ]]; then
    echo "🎉 Deployment successful!"
    echo "🌐 Application is available at: http://localhost:${SERVER_PORT:-6886}"
    echo "🩺 Health check: http://localhost:${SERVER_PORT:-6886}/actuator/health"
else
    echo "❌ Deployment failed - health check returned: $HEALTH_STATUS"
    echo "📋 Check logs: docker-compose -f $COMPOSE_FILE logs"
    exit 1
fi