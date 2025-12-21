.PHONY: start stop start-ollama start-app start-with-ollama start-with-gemini dev clean setup init-mac help

JAVA_HOME := /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export JAVA_HOME

# Load environment variables if .env exists
ifneq (,$(wildcard ./.env))
    include .env
    export
endif

# Default target
help:
	@echo "Tanoshii - Japanese Learning App"
	@echo ""
	@echo "Usage:"
	@echo "  make init-mac          - Full setup for macOS (installs all dependencies)"
	@echo "  make setup             - Install Python deps and pull Ollama model"
	@echo "  make start             - Start the app (with provider selection if both available)"
	@echo "  make start-with-ollama - Start with Ollama (local)"
	@echo "  make start-with-gemini - Start with Google Gemini (cloud)"
	@echo "  make stop              - Stop all services"
	@echo "  make dev               - Start in dev mode (auto-restart on code changes)"
	@echo "  make clean             - Clean build artifacts and database"
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
	@echo "Starting Ollama and pulling qwen model..."
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
	@# Create .env file with Ollama defaults
	@echo "Creating .env file with Ollama configuration..."
	@echo "# LLM Provider Configuration" > .env
	@echo "LLM_PROVIDER=ollama" >> .env
	@echo "LLM_BASE_URL=http://localhost:11434" >> .env
	@echo "LLM_MODEL=qwen2.5:7b" >> .env
	@echo "LLM_API_KEY=" >> .env
	@echo "" >> .env
	@echo "# To use Google Gemini instead:" >> .env
	@echo "# 1. Get API key from https://aistudio.google.com/app/apikey" >> .env
	@echo "# 2. Uncomment and set:" >> .env
	@echo "# LLM_PROVIDER=gemini" >> .env
	@echo "# LLM_BASE_URL=https://generativelanguage.googleapis.com" >> .env
	@echo "# LLM_MODEL=gemini-1.5-flash" >> .env
	@echo "# GOOGLE_AI_API_KEY=your-api-key-here" >> .env
	@echo "✓ .env file created"
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
	@echo "Pulling qwen model for Ollama..."
	ollama pull qwen2.5:7b
	@echo ""
	@echo "Setup complete!"

# Start everything (with provider selection if both available)
start:
	@# Check if both providers are available
	@OLLAMA_AVAILABLE=false; \
	GEMINI_AVAILABLE=false; \
	if pgrep -x "ollama" > /dev/null || command -v ollama &> /dev/null; then \
		OLLAMA_AVAILABLE=true; \
	fi; \
	if [ -n "$$GOOGLE_AI_API_KEY" ] && [ "$$GOOGLE_AI_API_KEY" != "" ]; then \
		GEMINI_AVAILABLE=true; \
	fi; \
	\
	if [ "$$OLLAMA_AVAILABLE" = true ] && [ "$$GEMINI_AVAILABLE" = true ]; then \
		echo "Both Ollama and Gemini are available!"; \
		echo ""; \
		echo "Choose your LLM provider:"; \
		echo "  1) Ollama (local, private, free)"; \
		echo "  2) Gemini (cloud, fast, free tier)"; \
		echo ""; \
		read -p "Enter choice (1 or 2): " choice; \
		case $$choice in \
			1) $(MAKE) start-with-ollama ;; \
			2) $(MAKE) start-with-gemini ;; \
			*) echo "Invalid choice. Defaulting to Ollama..."; $(MAKE) start-with-ollama ;; \
		esac; \
	elif [ "$$GEMINI_AVAILABLE" = true ]; then \
		echo "Starting with Gemini (Ollama not detected)..."; \
		$(MAKE) start-with-gemini; \
	else \
		echo "Starting with Ollama..."; \
		$(MAKE) start-with-ollama; \
	fi

# Start with Ollama
start-with-ollama: start-ollama
	@sleep 2
	@echo "Starting Tanoshii with Ollama..."
	@echo "Provider: Ollama (local)"
	@echo "Model: qwen2.5:7b"
	@echo ""
	LLM_PROVIDER=ollama LLM_BASE_URL=http://localhost:11434 LLM_MODEL=qwen2.5:7b ./gradlew run

# Start with Gemini
start-with-gemini:
	@echo "Starting Tanoshii with Google Gemini..."
	@echo "Provider: Gemini (cloud)"
	@echo "Model: gemini-1.5-flash"
	@echo ""
	@if [ -z "$$GOOGLE_AI_API_KEY" ]; then \
		echo "ERROR: GOOGLE_AI_API_KEY not set!"; \
		echo ""; \
		echo "To use Gemini:"; \
		echo "1. Get API key from https://aistudio.google.com/app/apikey"; \
		echo "2. Set environment variable:"; \
		echo "   export GOOGLE_AI_API_KEY='your-key-here'"; \
		echo "3. Or add to .env file:"; \
		echo "   GOOGLE_AI_API_KEY=your-key-here"; \
		echo ""; \
		exit 1; \
	fi
	LLM_PROVIDER=gemini LLM_BASE_URL=https://generativelanguage.googleapis.com LLM_MODEL=gemini-1.5-flash LLM_API_KEY=$$GOOGLE_AI_API_KEY ./gradlew run

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

# Fetch qwen model
pull-qwen:
	@echo "Pulling qwen model for Ollama..."
	ollama pull qwen2.5:7b

# Start just the app (assumes Ollama is running)
start-app:
	@echo "Starting Tanoshii on http://localhost:8080"
	./gradlew run

# Development mode with continuous build
dev:
	@# Check provider
	@if [ -n "$$GOOGLE_AI_API_KEY" ] && [ "$$GOOGLE_AI_API_KEY" != "" ]; then \
		echo "Starting in development mode with Gemini..."; \
		LLM_PROVIDER=gemini LLM_BASE_URL=https://generativelanguage.googleapis.com LLM_MODEL=gemini-1.5-flash LLM_API_KEY=$$GOOGLE_AI_API_KEY ./gradlew run -t; \
	else \
		$(MAKE) start-ollama; \
		sleep 2; \
		echo "Starting in development mode with Ollama..."; \
		LLM_PROVIDER=ollama ./gradlew run -t; \
	fi

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
