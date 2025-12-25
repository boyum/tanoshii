# Changelog

## [Unreleased] - 2024

### Added
- **Google Gemini Integration** using official `com.google.ai.client.generativeai` SDK
  - Seamless switching between Ollama (local) and Gemini (cloud)
  - Smart provider selection in Makefile with `make start`
  - Environment variable configuration support
  - Official SDK provides better error handling and retry logic

- **LLM Request Optimization** (32% reduction in API calls)
  - Combined sentence generation and word translation into single call
  - Topic suggestion caching (5-minute cache duration)
  - Reduced from ~31 to ~21 LLM calls per 10-task session

- **Keyboard Shortcuts** for better UX
  - `W` - Toggle show/hide words
  - `A` - Toggle auto-play audio
  - `F` - Toggle furigana
  - `R` - Toggle romaji
  - `T` - Toggle translations
  - `Space` - Play/pause audio
  - `←/→` - Navigate tasks
  - `S` - Show translation
  - `Enter` - Check answer
  - Visual keyboard hints (hidden on touch devices)

- **View Transitions API** for smooth animations
  - Topic loader transitions
  - Splash screen animations
  - Staggered entrance effects for better UX

- **Enhanced Copy/Paste** functionality
  - Fixed line break issues when copying Japanese text
  - Romaji and furigana excluded from clipboard
  - Clean, continuous text when pasting

- **135 Diverse Topic Options** (expanded from 24)
  - Nature & Weather, Urban Life, Activities & Hobbies
  - Culture & Arts, Places & Locations, Daily Life
  - Abstract & Emotional, Food & Drink
  - Better topic generation priming for LLM

- **Audio Hint Improvements**
  - "Show words" toggle for pure listening exercises
  - Better audio control visibility
  - Space bar shortcut for audio playback

### Changed
- **Migrated from raw HTTP to Google Gen AI SDK**
  - Replaced manual HTTP requests with official `com.google.genai.Client`
  - Better error handling and automatic retries
  - Cleaner, simpler synchronous API
  - Removed manual JSON parsing for Gemini responses

- **Improved Makefile** with smart provider detection
  - `make start` now prompts for provider selection if both available
  - `make start-with-ollama` - Force Ollama
  - `make start-with-gemini` - Force Gemini with validation
  - `make init-mac` creates `.env` file automatically
  - Loads environment variables from `.env` file

- **Configuration Simplification**
  - `base-url` no longer needed for Gemini (handled by SDK)
  - Single `api-key` field for both providers
  - Environment variable priority: CLI > Shell > .env > Default

### Fixed
- **Punctuation Cleanup** for Japanese text
  - Removes duplicate punctuation (！。 → ！)
  - Handles all Japanese punctuation marks correctly
  - Improved LLM prompts to prevent double punctuation

- **Word Translation Tokenization**
  - LLM now provides individual token translations
  - Better matching for particles (は, が, を, etc.)
  - Improved tooltip accuracy with multiple lookup strategies

### Documentation
- Added `GEMINI_SETUP.md` - Complete Gemini setup guide
- Added `GEMINI_QUICK_START.md` - 5-minute quickstart
- Added `MAKEFILE_USAGE.md` - Comprehensive Makefile guide
- Added `OPTIMIZATION.md` - LLM request optimization details
- Updated `README.md` with provider switching instructions
- Added `.env.example` with configuration examples

### Technical Details
- Dependencies:
  - Added `com.google.genai:google-genai:1.32.0` (Google Gen AI SDK)
  - Removed `kotlinx-coroutines` dependency (no longer needed with new SDK)
- Lazy initialization of Gemini client to avoid unnecessary instantiation
- Simple synchronous API calls using `Client.models.generateContent()`

## Cost Impact

### Before Optimization
- ~31 LLM calls per 10-task session
- Gemini free tier: ~48 sessions/day
- Cost: $6.20 per 100 sessions (paid tier)

### After Optimization
- ~21 LLM calls per 10-task session (32% reduction)
- Gemini free tier: ~71 sessions/day (48% increase)
- Cost: $4.20 per 100 sessions (32% savings)

## Breaking Changes
None - all changes are backward compatible with existing Ollama setups.

## Migration Guide

### Switching to Gemini

**Option 1: Using Makefile (Recommended)**
```bash
export GOOGLE_AI_API_KEY="your-key-here"
make start  # Choose option 2 for Gemini
```

**Option 2: Manual Configuration**
```yaml
# application.yml
llm:
  provider: gemini
  model: gemini-3-flash-preview
  api-key: ${GOOGLE_AI_API_KEY}
```

### Staying with Ollama
No changes needed - works exactly as before!

## Future Plans
- [ ] Batch task generation (90% potential reduction)
- [ ] Pre-computed vocabulary translations for N5 words
- [ ] Faster model for answer checking
- [ ] Streaming responses for better UX
- [ ] Progressive Web App (PWA) support
- [ ] Mobile-specific keyboard shortcuts
