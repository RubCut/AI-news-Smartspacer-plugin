# AI News — Smartspacer plugin

Smartspacer plugin that publishes a news **target** on your smartspace:

* the target shows the **short headline**;
* the line underneath (the complication line) always reads **"Tap to view full"**;
* tapping opens a dialog activity with the **full headline**, source/date and article text;
* at the bottom there are two buttons — **Close & dismiss** (left, closes and removes the
  target from Smartspacer) and **Close** (just closes the window).

Dismissed stories are remembered, so a removed target does not come back.

## Project layout

```
app/src/main/java/com/rubcut/ainews/
├── data/NewsItem.kt          # news model
├── data/NewsRepository.kt    # SharedPreferences-backed store + dismiss list
├── providers/NewsTargetProvider.kt  # SmartspacerTargetProvider
└── ui/NewsActivity.kt        # full-article dialog with the two buttons
    ui/MainActivity.kt        # helper screen (add test story / restore dismissed)
```

## Build

```bash
gradle assembleDebug          # or ./gradlew assembleDebug
```

Requires JDK 17 and the Android SDK (compileSdk 34). A GitHub Actions workflow in
`.github/workflows/build.yml` builds a debug APK on every push.

## Install / use

1. Install this APK alongside [Smartspacer](https://github.com/KieronQuinn/Smartspacer).
2. In Smartspacer → Targets → add **AI News**.
3. Use the app's "Add test story" button to push a story and check the target.

## Next steps

Real news fetching (RSS / API + background refresh) is not wired up yet —
`NewsRepository.add()` is the single entry point where a fetcher should insert items,
followed by `SmartspacerTargetProvider.notifyChange(context, NewsTargetProvider::class.java)`.
