# LLM Request Optimization

This document explains how LLM requests have been optimized to reduce costs and improve performance.

## Before Optimization ❌

For a 10-task Easy session:
- **10 calls** for sentence generation
- **10 calls** for word-by-word translations
- **1 call** for topic suggestions (every page load)
- **~10 calls** for answer checking

**Total: ~31 LLM calls per session**

### Cost Impact (Gemini)
- Free tier: 1500 requests/day = ~48 sessions/day
- Paid tier: $0.075 per 1M tokens ≈ $0.002 per request
- **Cost per session**: ~$0.062 ($6.20 per 100 sessions)

## After Optimization ✅

For a 10-task Easy session:
- **10 calls** for sentence generation (includes word translations)
- **0 calls** for word translations (included in sentence generation)
- **1 call** for topic suggestions (cached for 5 minutes)
- **~10 calls** for answer checking

**Total: ~21 LLM calls per session** (32% reduction!)

### Cost Impact (Gemini)
- Free tier: 1500 requests/day = ~71 sessions/day (48% more)
- **Cost per session**: ~$0.042 ($4.20 per 100 sessions)

**Savings: ~$2 per 100 sessions (32% reduction)**

---

## Optimizations Applied

### 1. Combined Sentence + Word Translation (50% reduction)

**Before:**
```kotlin
// Call 1: Generate sentence
val content = ollamaService.generateSentence(words, topic)

// Call 2: Generate word translations
val translations = ollamaService.generateWordTranslations(content.japanese)
```

**After:**
```kotlin
// Single call returns both
val content = ollamaService.generateSentence(words, topic)
// content.wordTranslations is already included!
```

**How it works:**
The LLM prompt now asks for both in one response:
```json
{
  "japanese": "猫が好きです。",
  "english": "I like cats.",
  "wordTranslations": {
    "猫": "cat",
    "が": "subject marker (ga)",
    "好き": "like, favorite",
    "です": "to be (polite)"
  }
}
```

---

### 2. Topic Suggestion Caching (100% reduction on cache hits)

**Before:**
- Every user opening the home page triggers an LLM call
- 100 users = 100 LLM calls

**After:**
- First user triggers LLM call
- Next 99 users (within 5 minutes) get cached results
- 100 users = 1 LLM call

**Cache duration:** 5 minutes (configurable in `OllamaService.kt`)

**Implementation:**
```kotlin
@Volatile
private var cachedTopics: List<String>? = null
@Volatile
private var topicsCacheTime: Long = 0
private val TOPIC_CACHE_DURATION_MS = 5 * 60 * 1000L
```

---

## Future Optimization Ideas

### 3. Batch Task Generation (Not Implemented)

Instead of generating tasks one-by-one, generate multiple in a single call:
```json
{
  "tasks": [
    {"japanese": "...", "english": "...", "wordTranslations": {...}},
    {"japanese": "...", "english": "...", "wordTranslations": {...}},
    {"japanese": "...", "english": "...", "wordTranslations": {...}}
  ]
}
```

**Potential savings:** Up to 90% for task generation
**Trade-off:** Slower response time, harder error handling

### 4. Pre-computed Vocabulary Translations (Not Implemented)

For common JLPT N5 words, pre-compute translations and store in database.

**Potential savings:** 30-50% on word translations
**Trade-off:** Less context-aware translations

### 5. Answer Checking Optimization (Not Implemented)

Use simpler/faster model for answer checking:
- Gemini Flash for task generation (quality)
- Gemini Nano or local model for answer checking (speed)

**Potential savings:** 20-30% on answer checking
**Trade-off:** Slightly lower feedback quality

---

## Monitoring LLM Usage

To track actual usage, check logs:

```bash
# Count LLM calls
grep "generate" logs/application.log | wc -l

# See cache hit rate
grep "Returning cached topics" logs/application.log | wc -l
```

Or add monitoring code:

```kotlin
companion object {
    @Volatile
    private var requestCount = 0

    fun getRequestCount(): Int = requestCount
    fun resetRequestCount() { requestCount = 0 }
}

// In generate():
requestCount++
logger.info("LLM request count: $requestCount")
```

---

## Configuration

### Topic Cache Duration

Edit `OllamaService.kt`:
```kotlin
private val TOPIC_CACHE_DURATION_MS = 10 * 60 * 1000L // 10 minutes
```

### Disable Caching (for development)

Set cache duration to 0:
```kotlin
private val TOPIC_CACHE_DURATION_MS = 0L
```

---

## Comparison by Provider

| Provider | Before Optimization | After Optimization | Savings |
|----------|---------------------|-------------------|---------|
| **Ollama (local)** | Free (hardware cost) | Free (hardware cost) | Less CPU/GPU usage |
| **Gemini (free tier)** | ~48 sessions/day | ~71 sessions/day | 48% more capacity |
| **Gemini (paid)** | $6.20/100 sessions | $4.20/100 sessions | $2.00 saved (32%) |

---

## Summary

✅ **32% fewer LLM calls** per session
✅ **48% more users** can use free tier
✅ **$2 saved** per 100 sessions on paid tier
✅ **Faster response times** (fewer sequential calls)
✅ **Better user experience** (cached topics load instantly)

The optimizations are **fully backward compatible** - if the LLM doesn't return word translations, the app falls back to a separate call.
