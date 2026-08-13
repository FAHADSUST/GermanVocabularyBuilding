# German Vocabulary Trainer (A2-B2 Beruf)

Android app for learning German vocabulary from A2 through B2 Beruf with spaced repetition, progress tracking, and optional cloud sync.

Built with Jetpack Compose, Room, WorkManager, and Firebase.

## What this app does

- Learn vocabulary from curated A2/B1/B2 chapter lists.
- Review with a Day 1 -> 2 -> 3 -> 7 -> 15 -> 30 SRS schedule.
- Filter and search by level, book, chapter, part of speech, and grammar group.
- Open detailed cards with verb/adjective forms, memory tips, and synonym/antonym chips.
- Play German TTS and track playback history.
- Optionally sync personal learning data via Firebase (manual sync).

## Tech stack

- Kotlin + Android Gradle Plugin 9
- Jetpack Compose + Navigation Compose
- Room (with KSP schema export)
- Kotlin Coroutines + WorkManager
- Retrofit + Moshi
- Coil
- Firebase Auth + Firestore (optional)

## Requirements

- Android Studio (recent stable)
- JDK 11+ for app compilation (project includes Java 11 compile target)
- Android SDK 36
- minSdk 30, targetSdk 36

## Quick start

1. Open this project in Android Studio.
2. Build and run the `app` module.
3. On first launch, the app imports `app/src/main/assets/vocabulary.csv` into Room.

Build from terminal:

```powershell
./gradlew :app:assembleDebug
```

Run tests:

```powershell
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

## Optional setup: Google image lookup

To enable per-word meaning images using Google Custom Search Image API, add these to:

`%USERPROFILE%\.gradle\gradle.properties`

```properties
googleImageApiKey=YOUR_API_KEY
googleImageSearchCx=YOUR_SEARCH_ENGINE_ID
```

If not set, the app still builds; image lookup simply stays inactive.

## Optional setup: Firebase cloud sync

Cloud sync (Settings -> Cloud-Sync) syncs:

- SRS progress (`progress`)
- Word markers (`word_mark`)
- Word comments (`word_comment`)
- TTS playback history

Meaning-image URLs are intentionally local-only and are not synced.

Setup steps:

1. Create a Firebase project.
2. Add Android app package `com.studio71.germanlinia2_b2`.
3. Place `google-services.json` at `app/google-services.json`.
4. Enable Email/Password auth.
5. Create Firestore in Native mode.

Without `app/google-services.json`, the project still builds and runs; sync features remain disabled.

## Firebase troubleshooting

### "Unknown calling package name 'com.google.android.gms'"

This log line is often benign Play Services noise. Verify real status from Settings -> Cloud-Sync using diagnostics and connection test.

If sign-in truly fails, check:

1. `google-services.json` package exactly matches `com.studio71.germanlinia2_b2`.
2. Debug SHA-1 is registered in Firebase.
3. Email/Password auth is enabled.
4. Device/emulator has current Google Play Services.

### "The client is offline" with `FAILED_PRECONDITION`

If logs mention Firestore in Datastore Mode, the active DB is incompatible with Firestore SDK.

- Recommended: create a Firebase project with Firestore Native mode and replace `app/google-services.json`.
- Alternative: create a named Native mode Firestore DB and set `FIRESTORE_DATABASE_ID` in `CloudSyncManager`.

## Data pipeline (A2/B1/B2 vocabulary)

The canonical data pipeline lives in `data/`.

- Source text files: `data/a2`, `data/b1`, and `data/b2_ch*.txt`
- Main generator: `data/build_vocab_csv.py`
- Output CSV: `data/vocabulary_all_generated.csv`
- App seed file: `app/src/main/assets/vocabulary.csv`

Generate and copy vocabulary:

```powershell
py data/build_vocab_csv.py
Copy-Item data/vocabulary_all_generated.csv app/src/main/assets/vocabulary.csv -Force
```

Schema and column rules are documented in `data/README_VOCAB_SCHEMA.md`.

Supporting scripts:

- `data/enrich.py` for inferred forms and cleanup
- `data/add_memory_tips.py` for memory tip augmentation
- `data/add_b2_mnemonics.py` for mnemonic updates

## Project structure (high level)

`app/src/main/java/com/studio71/germanlinia2_b2/`

- `GermanApp.kt` app initialization and startup tasks
- `MainActivity.kt` Compose entry point + navigation
- `data/` local DB, repositories, importers, sync
- `domain/` scheduling and use-case logic
- `ui/` screens, viewmodels, and reusable UI components

## Notes for contributors

- Keep vocabulary `id` values stable; progress is keyed by ID.
- Room schemas are exported under `app/schemas/`.
- Cloud sync and image lookup are optional and should not break local app startup.

## License

This project is licensed under the MIT License. See `LICENSE`.

