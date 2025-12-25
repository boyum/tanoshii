# Makefile Usage Guide

The Makefile provides convenient commands for running Tanoshii with different LLM providers.

## Quick Start

```bash
# Full setup for macOS (first time only)
make init-mac

# Start the app
make start
```

## Commands

### Setup Commands

#### `make init-mac`
Full macOS setup from scratch. Installs:
- Homebrew (if not installed)
- Java 17
- Python 3
- Ollama
- ffmpeg
- Python dependencies
- Qwen 2.5 7B model

Also creates a `.env` file with Ollama defaults.

#### `make setup`
Minimal setup (assumes Homebrew/Java/Python already installed):
- Creates Python virtual environment
- Installs Python dependencies
- Pulls Qwen 2.5 7B model

### Running the App

#### `make start`
Smart start with provider selection:
- If **only Ollama** available → starts with Ollama
- If **only Gemini** available → starts with Gemini
- If **both** available → prompts you to choose:

```
Both Ollama and Gemini are available!

Choose your LLM provider:
  1) Ollama (local, private, free)
  2) Gemini (cloud, fast, free tier)

Enter choice (1 or 2):
```

#### `make start-with-ollama`
Force start with Ollama (local):
- Starts Ollama server if not running
- Pulls qwen2.5:7b model if needed
- Runs app with Ollama

#### `make start-with-gemini`
Force start with Gemini (cloud):
- Requires `GOOGLE_AI_API_KEY` environment variable
- Runs app with Gemini API
- Shows helpful error if API key not set

### Development

#### `make dev`
Development mode with auto-restart:
- Automatically detects provider (Gemini if API key set, else Ollama)
- Watches for code changes
- Recompiles and restarts on save

### Other Commands

#### `make stop`
Stop all services (app and Ollama)

#### `make clean`
Clean build artifacts:
- Removes compiled classes
- Deletes database.sqlite
- Clears audio cache

#### `make build`
Build without running (skips tests)

#### `make test`
Run all tests

---

## Environment Variables

The Makefile reads from `.env` file if it exists. Variables can also be set in your shell.

### Ollama Configuration

```bash
export LLM_PROVIDER=ollama
export LLM_BASE_URL=http://localhost:11434
export LLM_MODEL=qwen2.5:7b
```

### Gemini Configuration

```bash
export LLM_PROVIDER=gemini
export LLM_BASE_URL=https://generativelanguage.googleapis.com
export LLM_MODEL=gemini-3-flash-preview
export GOOGLE_AI_API_KEY=your-key-here
```

---

## Examples

### Example 1: First Time Setup (Ollama)

```bash
# Install everything
make init-mac

# Start the app (will use Ollama by default)
make start
```

### Example 2: Switch to Gemini

```bash
# Get API key from https://aistudio.google.com/app/apikey

# Set the key
export GOOGLE_AI_API_KEY="AIza..."

# Start with Gemini
make start-with-gemini

# Or use interactive selection
make start
# Choose option 2 (Gemini)
```

### Example 3: Development with Gemini

```bash
# Set API key
export GOOGLE_AI_API_KEY="AIza..."

# Start in dev mode (auto-detects Gemini)
make dev
```

### Example 4: Back to Ollama

```bash
# Just unset the Gemini key
unset GOOGLE_AI_API_KEY

# Start (will use Ollama)
make start
```

---

## Using .env File

Instead of setting environment variables manually, edit the `.env` file:

```bash
# For Ollama (default after init-mac)
LLM_PROVIDER=ollama
LLM_BASE_URL=http://localhost:11434
LLM_MODEL=qwen2.5:7b
LLM_API_KEY=

# For Gemini (edit to these values)
LLM_PROVIDER=gemini
LLM_BASE_URL=https://generativelanguage.googleapis.com
LLM_MODEL=gemini-3-flash-preview
GOOGLE_AI_API_KEY=your-key-here
```

Then just run `make start` - no need to export variables!

---

## Troubleshooting

### "Ollama is not running"

```bash
# Start Ollama manually
make start-ollama

# Or restart everything
make stop
make start
```

### "GOOGLE_AI_API_KEY not set"

```bash
# Set the API key
export GOOGLE_AI_API_KEY="your-key-here"

# Or add to .env file
echo "GOOGLE_AI_API_KEY=your-key-here" >> .env
```

### "Model not found"

```bash
# Pull the model
make pull-qwen

# Or for full setup
make setup
```

### Port 8080 already in use

```bash
# Stop any running instances
make stop

# Check what's using the port
lsof -i :8080

# Kill the process if needed
kill -9 <PID>
```

---

## Advanced Usage

### Custom Model

```bash
# Use a different Ollama model
LLM_MODEL=llama3:8b make start-with-ollama

# Use a different Gemini model
LLM_MODEL=gemini-1.5-pro make start-with-gemini
```

### Custom Port

```bash
# Run on different port
SERVER_PORT=9090 make start
```

### Multiple Instances

```bash
# Terminal 1: Ollama instance
SERVER_PORT=8080 make start-with-ollama

# Terminal 2: Gemini instance
SERVER_PORT=8081 make start-with-gemini
```

---

## Tips

1. **Use `make help`** to see all available commands
2. **Check `.env.example`** for configuration examples
3. **Use `make dev`** during development for auto-reload
4. **Use `make start`** for interactive provider selection
5. **Keep `.env` in `.gitignore`** (never commit API keys!)

---

## Platform Support

The Makefile is designed for **macOS** with the following assumptions:
- Uses Homebrew for package management
- Uses `pgrep` and `pkill` for process management
- Java is at `/opt/homebrew/opt/openjdk@17`

For **Linux**, you may need to adjust:
- Package manager commands (apt/yum instead of brew)
- Java path in JAVA_HOME
- Process management commands

For **Windows**, consider using WSL or Docker instead.
