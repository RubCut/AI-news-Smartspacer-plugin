# AI News — Smartspacer plugin

Smartspacer plugin that publishes a **news target** on your smartspace.

* The target shows the **short headline**.
* The line underneath (the complication line) always reads **"Tap to view full"**.
* Tapping opens a dialog with the **full headline**, source, date and article text.
* Bottom bar: **Close & dismiss** (left — closes and removes the target from
  Smartspacer) and **Close** (right — just closes the window). If the feed gave a
  link, an "Open in browser" button is shown too.

There is **no launcher app**: everything is configured from inside the target
(Smartspacer → Targets → AI News → settings), exactly like the 2GIS plugin.

## Settings (per target instance)

* **RSS / Atom feed** — any feed; the default is a Google News AI search feed.
* **Update interval** — 15 minutes to 8 hours (`refreshPeriodMinutes`).
* **Stories on the smartspace** — 1 to 5 targets at a time.
* **Restore dismissed stories** — brings back stories removed with dismiss.

Several "AI News" targets can be added at once; each keeps its own feed,
interval and dismissed list, and its settings are wiped on `onProviderRemoved`.

## Project layout

```
app/src/main/java/com/rubcut/ainews/
├── Constants.kt              # authority, defaults, limits
├── NewsItem.kt               # story model
├── NewsFetcher.kt            # tiny RSS/Atom parser (no networking libs)
├── NewsUpdater.kt            # fetch + store for one target instance
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
