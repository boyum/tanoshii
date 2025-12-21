# Using Google Gemini Instead of Ollama

This guide shows you how to switch from Ollama (local) to Google Gemini (free hosted API).

## Why Gemini?

- ✅ **Free tier**: 15 requests/minute, 1 million tokens/day
- ✅ **No local GPU needed**: Runs on any server
- ✅ **Excellent Japanese support**: Better than most models
- ✅ **Fast responses**: ~1-2 seconds
- ✅ **Cost-effective**: ~$5-10/month after free tier

## Setup Steps

### 1. Get Gemini API Key

1. Go to [Google AI Studio](https://aistudio.google.com/app/apikey)
2. Sign in with Google account
3. Click "Create API Key"
4. Copy your API key

### 2. Update Configuration

Edit `src/main/resources/application.yml`:

```yaml
llm:
  provider: gemini  # Change from "ollama" to "gemini"
  base-url: https://generativelanguage.googleapis.com
  model: gemini-1.5-flash
  api-key: ${GOOGLE_AI_API_KEY}  # Set via environment variable
```

### 3. Set Environment Variable

**On Mac/Linux:**
```bash
export GOOGLE_AI_API_KEY="your-api-key-here"
```

**On Windows (PowerShell):**
```powershell
$env:GOOGLE_AI_API_KEY="your-api-key-here"
```

**Or add to `.env` file:**
```
GOOGLE_AI_API_KEY=your-api-key-here
```

### 4. Run the App

```bash
make run
```

That's it! The app will now use Gemini instead of Ollama.

## Docker Setup

If using Docker, update your `docker-compose.yml`:

```yaml
services:
  app:
    environment:
      - LLM_PROVIDER=gemini
      - LLM_BASE_URL=https://generativelanguage.googleapis.com
      - LLM_MODEL=gemini-1.5-flash
      - LLM_API_KEY=${GOOGLE_AI_API_KEY}
    # Remove ollama service dependency
```

## Switch Back to Ollama

Just change the provider back:

```yaml
llm:
  provider: ollama
  base-url: http://localhost:11434
  model: qwen2.5:7b
  api-key: ""
```

## Gemini Free Tier Limits

- **Requests**: 15 per minute, 1,500 per day
- **Tokens**: 1 million per day
- **Good for**: 10-50 active users

After free tier, costs are very low:
- **Input**: $0.075 per 1M tokens (~$0.000075 per request)
- **Output**: $0.30 per 1M tokens (~$0.0003 per request)

**Estimated cost for 1,000 sentences/day**: ~$1-2/month

## Available Gemini Models

| Model | Speed | Quality | Cost |
|-------|-------|---------|------|
| `gemini-1.5-flash` | Fast | Excellent | Free tier + cheap |
| `gemini-1.5-pro` | Medium | Best | Higher cost |
| `gemini-1.0-pro` | Fast | Good | Free tier |

**Recommendation**: Use `gemini-1.5-flash` (best balance)

## Troubleshooting

### "Failed to generate content from Gemini"

1. Check API key is set correctly
2. Verify you haven't exceeded rate limits
3. Check the logs for specific error messages

### Rate limit exceeded

Wait 1 minute or upgrade to paid tier.

### Invalid API key

Make sure you copied the full key from Google AI Studio.

## Comparison: Ollama vs Gemini

| Feature | Ollama (Local) | Gemini (Cloud) |
|---------|----------------|----------------|
| Setup | Complex | Easy |
| Cost | $0 (uses your hardware) | Free tier + $1-10/month |
| Speed | Medium (depends on GPU) | Fast |
| Japanese Quality | Good | Excellent |
| Privacy | 100% private | Data sent to Google |
| Reliability | Depends on your server | 99.9% uptime |
| Hardware Needed | 8GB+ RAM, optional GPU | None |

## Questions?

See the main README or open an issue on GitHub.
