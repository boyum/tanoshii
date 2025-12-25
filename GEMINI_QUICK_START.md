# Quick Start with Gemini (5 minutes)

Get the app running with Google's free Gemini API - no GPU or powerful computer needed!

## Step 1: Get API Key (1 minute)

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with your Google account
3. Click "**Create API Key**"
4. Copy the key (starts with `AIza...`)

## Step 2: Configure App (1 minute)

Edit `src/main/resources/application.yml`:

```yaml
llm:
  provider: gemini  # ← Change this from "ollama" to "gemini"
  model: gemini-3-flash-preview  # ← Gemini model name
  api-key: ${GOOGLE_AI_API_KEY}  # ← Will read from environment

  # Note: base-url is not used with official Gemini SDK
```

## Step 3: Set API Key

**Mac/Linux:**
```bash
export GOOGLE_AI_API_KEY="your-key-here"
```

**Windows PowerShell:**
```powershell
$env:GOOGLE_AI_API_KEY="your-key-here"
```

## Step 4: Run the App

```bash
./gradlew run
```

Or if using Docker:

```bash
docker-compose up -d
```

## That's It!

Open **http://localhost:8080** in your browser.

The app now uses Gemini instead of Ollama - no local AI model needed!

---

## What's Different?

| Feature | Ollama (Before) | Gemini (Now) |
|---------|----------------|--------------|
| Setup Time | 30+ minutes | 5 minutes |
| Download Size | ~5 GB | 0 GB |
| RAM Needed | 8+ GB | 2 GB |
| GPU Needed | Optional | No |
| Cost | $0 | $0 (free tier) |
| Speed | Medium | Fast |
| Japanese Quality | Good | Excellent ⭐ |

## Free Tier Details

- **15 requests per minute**
- **1,500 requests per day**
- **1 million tokens per day**

This is enough for:
- ✅ Personal use (unlimited)
- ✅ 10-20 active users
- ✅ Development and testing

## Switching Back to Ollama

Just change the config back:

```yaml
llm:
  provider: ollama
  base-url: http://localhost:11434
  model: qwen2.5:7b
  api-key: ""
```

## Need Help?

See [GEMINI_SETUP.md](GEMINI_SETUP.md) for detailed docs or open an issue!
