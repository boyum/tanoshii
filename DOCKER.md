# Docker Quick Reference

This guide provides detailed Docker instructions for running Tanoshii.

## Prerequisites

- Docker Desktop (or Docker Engine + Docker Compose)
- At least 8 GB free disk space
- 4 GB RAM minimum (8 GB recommended)

## Quick Start

```bash
# Clone the repository
git clone https://github.com/boyum/tanoshii
cd tanoshii

# Start everything
docker-compose up -d

# View logs
docker-compose logs -f
```

Open http://localhost:8080 in your browser once the services are ready.

## Understanding the Setup

The Docker setup consists of three services:

1. **ollama**: The LLM server (Ollama with Qwen 2.5 model)
2. **ollama-init**: One-time service to download the LLM model
3. **app**: The Tanoshii application

### First Startup

The first startup takes several minutes because:

1. Docker builds the application (~2-3 minutes)
2. Ollama downloads the Qwen 2.5 model (~4.7 GB)
3. The application starts and initializes the database

You can monitor the progress:

```bash
# Watch all services
docker-compose logs -f

# Watch just Ollama (to see model download)
docker-compose logs -f ollama

# Watch just the app
docker-compose logs -f app
```

### Subsequent Startups

After the first run, startups are much faster (~10-20 seconds) because:

- The application image is built
- The LLM model is cached in a Docker volume
- The database is persisted

## Common Commands

```bash
# Start services in background
docker-compose up -d

# Stop services
docker-compose down

# View logs (all services)
docker-compose logs -f

# View logs (specific service)
docker-compose logs -f app
docker-compose logs -f ollama

# Restart a service
docker-compose restart app

# Rebuild after code changes
docker-compose build
docker-compose up -d

# Check service status
docker-compose ps
```

## Data Persistence

Data is stored in Docker volumes:

- `ollama-data`: LLM model (~4.7 GB)
- `app-data`: SQLite database with sessions and vocabulary
- `audio-cache`: Generated audio files

### Backing Up Your Data

```bash
# Backup the database
docker cp tanoshii-app:/app/data/database.sqlite ./backup-database.sqlite

# Restore the database
docker cp ./backup-database.sqlite tanoshii-app:/app/data/database.sqlite
docker-compose restart app
```

### Fresh Start

To completely reset and remove all data:

```bash
docker-compose down -v
docker-compose up -d
```

**Warning**: This deletes the LLM model (~4.7 GB) and will need to re-download it.

## Custom Vocabulary

Place your CSV files in the `known-words/` directory:

```bash
# Create the directory
mkdir -p known-words

# Add your CSV files (semicolon-separated)
echo "猫;neko;cat" > known-words/my-words.csv
echo "犬;inu;dog" >> known-words/my-words.csv

# Restart the app to load new words
docker-compose restart app
```

The `known-words/` directory is automatically mounted into the container.

## Troubleshooting

### Services Won't Start

Check if ports are already in use:

```bash
# Check port 8080
lsof -i :8080

# Check port 11434
lsof -i :11434
```

If ports are in use, either stop the conflicting service or change the ports in `docker-compose.yml`:

```yaml
services:
  app:
    ports:
      - "8081:8080" # Change to 8081
```

### App Can't Connect to Ollama

The app may start before the LLM model finishes downloading. Wait for the model download to complete:

```bash
# Monitor Ollama logs
docker-compose logs -f ollama

# Once download is complete, restart app
docker-compose restart app
```

### Out of Disk Space

The LLM model requires ~4.7 GB. Check available space:

```bash
docker system df
```

Clean up unused Docker resources:

```bash
# Remove unused images
docker image prune -a

# Remove unused volumes (be careful!)
docker volume prune
```

### Container Keeps Restarting

Check the logs:

```bash
docker-compose logs --tail=100 app
```

Common issues:

- Ollama not ready: Wait for model download
- Port already in use: Change port in docker-compose.yml
- Out of memory: Increase Docker memory limit in Docker Desktop settings

### Health Check Failing

The app includes health checks. View status:

```bash
docker-compose ps
```

If unhealthy, check logs:

```bash
docker-compose logs --tail=50 app
```

## Development with Docker

To rebuild after code changes:

```bash
# Rebuild the app
docker-compose build app

# Restart with new image
docker-compose up -d
```

For faster development, consider mounting the source code:

```yaml
services:
  app:
    volumes:
      - ./src:/app/src
```

Then use Gradle's continuous build inside the container.

## Performance Tuning

### Memory

Adjust JVM memory in `docker-compose.yml`:

```yaml
services:
  app:
    environment:
      - JAVA_OPTS=-Xmx1024m -Xms512m
```

### CPU

Limit CPU usage:

```yaml
services:
  ollama:
    deploy:
      resources:
        limits:
          cpus: "2.0"
```

## Accessing Ollama Directly

Ollama is exposed on port 11434:

```bash
# List models
curl http://localhost:11434/api/tags

# Generate text
curl http://localhost:11434/api/generate -d '{
  "model": "qwen2.5:7b",
  "prompt": "Translate to Japanese: Hello"
}'
```

## Uninstalling

```bash
# Stop and remove containers
docker-compose down

# Remove volumes (deletes all data)
docker-compose down -v

# Remove images
docker rmi tanoshii-app ollama/ollama

# Remove the project directory
cd ..
rm -rf tanoshii
```

## Architecture

```txt
┌─────────────────┐
│   Browser       │
│  localhost:8080 │
└────────┬────────┘
         │
         │ HTTP
         │
┌────────▼────────┐     ┌──────────────┐
│   app           │────→│   ollama     │
│  (Tanoshii)     │ LLM │ (Qwen 2.5)   │
│                 │     │              │
│ • Kotlin/Java   │     │ Port: 11434  │
│ • Edge TTS      │     └──────────────┘
│ • Whisper       │
│ • SQLite        │
│                 │
│ Port: 8080      │
└─────────────────┘
```

## Support

For issues specific to Docker deployment, check:

1. Docker logs: `docker-compose logs -f`
2. Container status: `docker-compose ps`
3. Resource usage: `docker stats`

For application issues, see the main README.md.
