.PHONY: start stop start-ollama start-app dev clean setup init-mac help

JAVA_HOME := /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export JAVA_HOME

# Default target
help:
	@echo "Tanoshii - Japanese Learning App"
	@echo ""
	@echo "Usage:"
	@echo "  make init-mac   - Full setup for macOS (installs all dependencies)"
	@echo "  make setup      - Install Python deps and pull Ollama model"
	@echo "  make start      - Start Ollama and the app"
	@echo "  make stop       - Stop all services"
	@echo "  make dev        - Start in dev mode (auto-restart on code changes)"
	@echo "  make clean      - Clean build artifacts and database"
	@echo ""
	@echo "Individual services:"
	@echo "  make start-ollama  - Start Ollama server"
	@echo "  make start-app     - Start the Micronaut app"

# Full macOS setup - installs everything from scratch
init-mac:
	@echo "=========================================="
	@echo "Tanoshii - macOS Setup"
	@echo "=========================================="
	@echo ""
	@# Check for Homebrew
	@if ! command -v brew &> /dev/null; then \
		echo "Installing Homebrew..."; \
		/bin/bash -c "$$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"; \
	else \
		echo "✓ Homebrew already installed"; \
	fi
	@echo ""
	@# Install Java 17
	@if ! brew list openjdk@17 &> /dev/null; then \
		echo "Installing Java 17..."; \
		brew install openjdk@17; \
	else \
		echo "✓ Java 17 already installed"; \
	fi
	@echo ""
	@# Install Python
	@if ! command -v python3 &> /dev/null; then \
		echo "Installing Python 3..."; \
		brew install python@3.11; \
	else \
		echo "✓ Python 3 already installed"; \
	fi
	@echo ""
	@# Install Ollama
	@if ! command -v ollama &> /dev/null; then \
		echo "Installing Ollama..."; \
		brew install ollama; \
	else \
		echo "✓ Ollama already installed"; \
	fi
	@echo ""
	@# Install ffmpeg (needed for audio conversion in STT)
	@if ! command -v ffmpeg &> /dev/null; then \
		echo "Installing ffmpeg..."; \
		brew install ffmpeg; \
	else \
		echo "✓ ffmpeg already installed"; \
	fi
	@echo ""
	@# Setup Python venv and dependencies
	@echo "Setting up Python virtual environment..."
	@python3 -m venv .venv
	@.venv/bin/pip install --upgrade pip --quiet
	@.venv/bin/pip install -r scripts/requirements.txt --quiet
	@echo "✓ Python dependencies installed"
	@echo ""
	@# Start Ollama and pull model
	@echo "Starting Ollama and pulling qwen2.5 model..."
	@if ! pgrep -x "ollama" > /dev/null; then \
		ollama serve > /dev/null 2>&1 & \
		sleep 3; \
	fi
	@ollama pull qwen2.5:7b
	@echo "✓ Ollama model ready"
	@echo ""
	@echo "=========================================="
	@echo "Setup complete!"
	@echo "=========================================="
	@echo ""
	@echo "To start the app, run:"
	@echo "  make start"
	@echo ""
	@echo "Then open http://localhost:8080 in your browser"
	@echo ""

# Setup dependencies (without Homebrew installs)
setup:
	@echo "Setting up Python virtual environment..."
	python3 -m venv .venv
	.venv/bin/pip install --upgrade pip
	.venv/bin/pip install -r scripts/requirements.txt
	@echo ""
	@echo "Pulling qwen2.5 model for Ollama..."
	ollama pull qwen2.5:7b
	@echo ""
	@echo "Setup complete!"

# Start everything
start: start-ollama
	@sleep 2
	@echo "Starting Tanoshii..."
	./gradlew run

# Start Ollama in background
start-ollama:
	@echo "Starting Ollama server..."
	@if pgrep -x "ollama" > /dev/null; then \
		echo "Ollama is already running"; \
	else \
		ollama serve > /dev/null 2>&1 & \
		echo "Ollama started (PID: $$!)"; \
		sleep 2; \
	fi
	make pull-qwen

# Fetch qwen2.5 model
pull-qwen:
	@echo "Pulling qwen2.5 model for Ollama..."
	ollama pull qwen2.5:7b

# Start just the app (assumes Ollama is running)
start-app:
	@echo "Starting Tanoshii on http://localhost:8080"
	./gradlew run

# Development mode with continuous build
dev: start-ollama
	@sleep 2
	@echo "Starting in development mode..."
	./gradlew run -t

# Stop all services
stop:
	@echo "Stopping services..."
	@-pkill -f "gradlew run" 2>/dev/null || true
	@-pkill -f "tanoshii" 2>/dev/null || true
	@echo "Services stopped"

# Clean build artifacts
clean:
	@echo "Cleaning..."
	./gradlew clean
	rm -f database.sqlite
	rm -rf audio-cache/*
	@echo "Clean complete"

# Build without running
build:
	./gradlew build -x test

# Run tests
test:
	./gradlew test
