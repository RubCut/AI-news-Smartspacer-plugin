# AI News — Smartspacer plugin

Smartspacer plugin that publishes an **AI-written news target** on your smartspace:
you type a topic, and a model (Gemini for now) writes a short headline, a full
headline and the story itself.

* The target shows the **short headline**.
* The line underneath (the complication line) always reads **"Tap to view full"**.
* Tapping opens a **full screen** article with a large collapsing headline,
  source, date and the story text.
* Bottom bar: **Close & dismiss** (left — closes and removes the target from
  Smartspacer) and **Close** (right — just closes the window). Dismissing every
  story hides the target completely until the next generation. If the feed gave a
  link, an "Open in browser" button is shown too.

There is **no launcher app**: everything is configured from inside the target
(Smartspacer → Targets → AI News → settings), exactly like the 2GIS plugin.

## Settings (per target instance)

* **Topic** — what the model should write about, e.g. "AI and robotics".
* **AI provider** — Google Gemini (only backend for now).
* **Model** — a starter list, or **Fetch models** to pull the real list of
  models your key can use.
* **Gemini API key** — with a shortcut to Google AI Studio, plus **Test key**
  which verifies it against the API before you generate anything.
* **Language of the stories** — defaults to the device language.
* **Update interval** — 15 minutes to 8 hours (`refreshPeriodMinutes`).
* **Stories on the smartspace** — 1 to 5 targets at a time.
* **Restore dismissed stories** — brings back stories removed with dismiss.

Several "AI News" targets can be added at once (each with its own topic); each keeps its own feed,
topic, key, model, interval and dismissed list, and its settings are wiped on `onProviderRemoved`.

## Project layout

```
app/src/main/java/com/rubcut/ainews/
├── Constants.kt              # authority, defaults, limits
├── NewsItem.kt               # story model
├── AiProvider.kt             # AI backends (Gemini for now)
├── GeminiClient.kt           # Gemini REST call with a JSON response schema
├── NewsUpdater.kt            # generate + store for one target instance
├── NewsUpdateReceiver.kt     # periodic refresh broadcast from Smartspacer
├── SettingsRepository.kt     # per-smartspacerId settings + cached stories
├── targets/NewsTarget.kt     # SmartspacerTargetProvider
└── ui/NewsActivity.kt        # article dialog with the two buttons
    ui/SettingsActivity.kt    # target settings (configActivity)
```

## Build

```bash
gradle assembleDebug
```

JDK 17, Android SDK 36, AGP 8.11.1 / Kotlin 2.2.20, Smartspacer SDK 1.1.
GitHub Actions (`.github/workflows/build.yml`) builds the debug APK on push.
