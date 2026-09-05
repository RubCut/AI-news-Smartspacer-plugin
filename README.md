**English** · [Русский](README_RU.md)

<div align="center">

<img src="docs/icon.svg" width="112" alt="AI News Smartspacer icon">

# AI News for Smartspacer

**Pick a topic — a neural network writes the news right inside Smartspacer**

[![Build APK](https://github.com/RubCut/AI-news-Smartspacer-plugin/actions/workflows/build.yml/badge.svg)](https://github.com/RubCut/AI-news-Smartspacer-plugin/actions/workflows/build.yml)
![Android 10+](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-7F52FF?logo=kotlin&logoColor=white)
![Material 3 Expressive](https://img.shields.io/badge/Material-3_Expressive-6750A4)
![Gemini](https://img.shields.io/badge/Google-Gemini-4285F4?logo=googlegemini&logoColor=white)

</div>

The plugin adds one Target provider to [Smartspacer](https://github.com/KieronQuinn/Smartspacer)
that shows AI-written news:

- 📝 you type a topic — anything from `artificial intelligence` to `Formula 1`;
- 🤖 an AI provider of your choice writes a short headline, a full headline and the article;
- 📰 the smartspace shows the short headline with a `Tap to view full` line;
- 📖 tapping opens a full screen reader you can swipe between articles in.

One target covers everything: a single story shows its own short headline, while
several show a `5 stories to read` summary instead.

## Features

- Settings screen in Material 3 Expressive, with a large collapsing title;
- Full screen reader with a swipeable pager, a dots overlay and an `x of y` counter;
- Articles in GitHub flavoured Markdown: **bold**, *italics*, ~~strikethrough~~,
  headings, bulleted and numbered lists, quotes, code and links;
- Article length of your choice — short, medium or long;
- Model list fetched straight from the API, plus a one-tap API key test;
- Many AI providers: **Gemini**, **Claude**, **OpenAI**, **DeepSeek**, **OpenRouter**,
  **Groq**, **Mistral**, **xAI Grok**, **Qwen**, **Together**, **Perplexity**,
  **Cerebras**, **Ollama** and any custom OpenAI-, Anthropic- or Gemini-compatible endpoint;
- Keys, models and base URLs are remembered per provider, so switching back and
  forth loses nothing;
- An **About** section with the plugin description and a link to the repository;
- Interface in English, Russian, Ukrainian, German, French, Spanish and Portuguese;
- Every build is signed with the same key, so updates install over each other;
- Dynamic colors on Android 12+;
- Light and dark themes;
- Per-target settings: each target keeps its own topic, key, model and schedule;
- Multiple targets can run side by side with different topics;
- Automatic refresh with an individual interval per target — from 15 to 480 minutes;
- `Close & dismiss` hides just the story you are reading, and the target
  disappears once the last one is gone;
- `Restore dismissed stories` brings them all back;
- No launcher icon and no background location or tracking of any kind.

## Requirements

- Android 10 or newer;
- Smartspacer installed;
- An API key for one of the supported providers — or a local server such as
  [Ollama](https://ollama.com/), which needs no key at all.

> Most providers have a free tier with rate limits and paid tiers above it.
> Check the current pricing and quotas of your provider before use.

### Supported providers

| Provider | API dialect | Key |
|---|---|---|
| Google Gemini | Gemini | [AI Studio](https://aistudio.google.com/app/apikey) |
| Anthropic Claude | Anthropic | [Console](https://console.anthropic.com/settings/keys) |
| OpenAI | OpenAI | [Platform](https://platform.openai.com/api-keys) |
| DeepSeek | OpenAI | [Platform](https://platform.deepseek.com/api_keys) |
| OpenRouter | OpenAI | [Keys](https://openrouter.ai/keys) |
| Groq | OpenAI | [Console](https://console.groq.com/keys) |
| Mistral AI | OpenAI | [Console](https://console.mistral.ai/api-keys) |
| xAI Grok | OpenAI | [Console](https://console.x.ai/) |
| Alibaba Qwen | OpenAI | [Bailian](https://bailian.console.alibabacloud.com/) |
| Together AI | OpenAI | [Settings](https://api.together.xyz/settings/api-keys) |
| Perplexity | OpenAI | [Settings](https://www.perplexity.ai/settings/api) |
| Cerebras | OpenAI | [Cloud](https://cloud.cerebras.ai/) |
| Ollama / local server | OpenAI | not needed |
| Custom endpoint | OpenAI, Anthropic or Gemini | depends on the server |

## Getting an API key

1. Pick a provider in the settings and tap **Get a … key** — it opens that
   provider's console.
2. Create a key there.
3. Paste it into the plugin settings.
4. Tap **Test key** to verify it, then **Fetch models** to load the models it can use.

For **Ollama** or a **custom** endpoint, enter the base URL instead
(for example `http://192.168.1.10:11434/v1`); the key can be left empty.

## Installation

### Prebuilt build

1. Open releases.
2. Install/Update plugin

### Building from sources

Requires JDK 17, Android SDK 36, and Gradle 8.13.

```bash
git clone https://github.com/RubCut/AI-news-Smartspacer-plugin.git
cd AI-news-Smartspacer-plugin
gradle assembleDebug
```

The APK will be in:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

1. In Smartspacer, add the **AI News** target.
2. The settings open automatically; later they are available through **More settings**.
3. Enter the **topic** the model should write about and the **language** of the stories.
4. Choose the **AI provider**, paste its **API key** (and a **base URL** for local
   or custom endpoints), then:
   - **Test key** — checks it against the API and reports how many models it can reach;
   - **Fetch models** — loads the real model list for that key into the dropdown.
5. Choose the behaviour of this target:
   - **Article length** — short, medium or long;
   - **Update interval** — from 15 to 480 minutes;
   - **Stories on the smartspace** — from 1 to 5 at a time.
6. Tap **"Save and generate"**.

The plugin intentionally has no launcher icon: settings open from Smartspacer
through **More settings**.

## How refresh works

```text
Smartspacer
    │ periodic request (individual interval, 15–480 min)
    ▼
NewsUpdateReceiver
    │ topic, language, length, model
    ▼
Gemini API (structured JSON response)
    │ short headline + full headline + Markdown body
    ▼
SharedPreferences → Target
```

`getSmartspaceTargets()` does not perform network calls. The target provider only
reads the latest cached stories, so Smartspacer gets its response fast. Generation
happens in the background receiver, or on demand when you save the settings.

## Privacy

- the plugin does not track location and collects no analytics;
- the API key, topic, settings and generated stories are stored locally in
  `SharedPreferences` — per target instance;
- only the topic, language and length are sent to the provider you picked;
- removing a target wipes its settings and its stories.

## Project structure

```text
app/src/main/java/com/rubcut/ainews/
├── Constants.kt                # authority, defaults and limits
├── NewsItem.kt                 # story model
├── AiProvider.kt               # the list of supported backends
├── ApiFlavor.kt                # Gemini / Anthropic / OpenAI dialects
├── AiClient.kt                 # one entry point for every backend
├── GeminiClient.kt             # Gemini generateContent
├── AnthropicClient.kt          # Claude /messages
├── OpenAiClient.kt             # OpenAI-compatible /chat/completions
├── NewsPrompt.kt               # the shared prompt
├── NewsJsonParser.kt           # tolerant JSON → stories
├── StoryLength.kt              # prompt wording and token budget per length
├── MarkdownRenderer.kt         # GitHub flavoured Markdown → spans
├── NewsUpdater.kt              # generate and store for one target
├── NewsUpdateReceiver.kt       # Smartspacer update requests
├── SettingsRepository.kt       # local settings and story cache
├── targets/
│   └── NewsTarget.kt           # the Smartspacer target provider
└── ui/
    ├── SettingsActivity.kt     # settings screen
    ├── NewsActivity.kt         # full screen reader
    └── StoryPagerAdapter.kt    # one page per story
```

## Limitations

- the model writes from what it knows, so stories are plausible rather than
  verified breaking news — treat them as a generated digest, not a news wire;
- generation needs a network connection; on failure the target keeps the previous
  stories and the error is shown in the settings;
- before configuration the target shows `Tap to set up your topic`;
- the smartspace headline is trimmed to 42 characters, so a short format is used;
- every refresh costs one API request per target — keep that in mind when
  choosing an interval and a plan.

## Signing

Every build — CI, local, debug and release — is signed with the shared key in
[`signing/`](signing/README.md), so a new APK always installs over the previous
one instead of failing with a signature mismatch. Use your own key by creating
`signing.properties` in the repository root; see the folder's README.

## Disclaimer

This project is not an official product of Smartspacer or any AI provider. Names
and trademarks belong to their respective owners. By using a provider's API you
agree to its current terms of service and the limits of your plan. Generated
stories may contain inaccuracies.
