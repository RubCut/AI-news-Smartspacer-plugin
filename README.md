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
- 🤖 Google Gemini writes a short headline, a full headline and the article;
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
- A Google Gemini API key with access to the
  [Generative Language API](https://ai.google.dev/gemini-api/docs).

> The Gemini API has a free tier with rate limits, and paid tiers above it.
> Check the current [pricing and quotas](https://ai.google.dev/pricing) before use.

## Getting an API key

1. Open [Google AI Studio](https://aistudio.google.com/app/apikey).
2. Create a project and an API key.
3. Paste the key into the plugin settings.
4. Tap **Test key** to verify it, then **Fetch models** to load the models it can use.

A shortcut to AI Studio is also available directly from the settings screen.

## Installation

### Prebuilt debug build

1. Open the latest successful run in
   [Actions](https://github.com/RubCut/AI-news-Smartspacer-plugin/actions/workflows/build.yml).
2. Download the `ai-news-debug-apk` artifact.
3. Unpack the archive and install the APK on your phone.

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
4. Paste your **Gemini API key**, then:
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
- only the topic, language and length are sent to the Gemini API;
- removing a target wipes its settings and its stories.

## Project structure

```text
app/src/main/java/com/rubcut/ainews/
├── Constants.kt                # authority, defaults and limits
├── NewsItem.kt                 # story model
├── AiProvider.kt               # AI backends (Gemini for now)
├── StoryLength.kt              # prompt wording and token budget per length
├── GeminiClient.kt             # generation and model listing
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

## Disclaimer

This project is not an official product of Google or Smartspacer. Names and
trademarks belong to their respective owners. By using the Gemini API you agree
to the current terms of service and the limits of your plan. Generated stories
may contain inaccuracies.
