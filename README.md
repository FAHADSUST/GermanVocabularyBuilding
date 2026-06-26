# German Vocabulary Trainer (A2 → B2 Beruf)

A simple, fast Android app to learn German vocabulary from *die neue Linie A2/B1*
and *Linie B2 Beruf*, with spaced repetition and progress tracking.

Built with **Jetpack Compose + Room** (AGP 9 built-in Kotlin).

## Run
Open in Android Studio and press Run, or:
```
./gradlew :app:assembleDebug
```
On first launch the app imports `app/src/main/assets/vocabulary.csv` into Room.

## Firebase Spark (manual cloud sync)
The app now includes manual cloud sync (Settings -> Cloud-Sync) for:
- SRS progress (`progress` table)
- word markers (`word_mark`)
- word comments (`word_comment`)
- TTS playback history

Meaning-image URLs are intentionally **local-only** and are not part of cloud sync.

Setup:
1. Create a Firebase project (Spark plan is enough for early usage).
2. Add an Android app with package `com.studio71.germanlinia2_b2`.
3. Download `google-services.json` and place it in `app/google-services.json`.
4. In Firebase Console -> Authentication, enable **Email/Password**.
5. In Firestore, create a database (production or test mode) and add rules for per-user data.

Without `app/google-services.json`, the project still builds but cloud sync remains disabled in the UI.

### Troubleshooting: "Failed to get service from broker / Unknown calling package name 'com.google.android.gms'"
This red logcat line is emitted **inside** the Google Play Services process during internal
service binding. It is benign noise and does **not** stop Firebase Auth or Firestore.
To confirm the real status, open Settings -> Cloud-Sync:
- tap **Diagnose anzeigen** to see Firebase project, package name and Play-Services status, and
- after signing in, tap **Verbindung testen** for a real Auth + Firestore round-trip (PASS/FAIL).

If sign-in genuinely fails (not just the log line), check:
1. `app/google-services.json` package is exactly `com.studio71.germanlinia2_b2`.
2. The debug **SHA-1** is added to the Firebase Android app.
3. **Email/Password** is enabled in Firebase Authentication.
4. The device/emulator has up-to-date Google Play Services (use a Google Play system image).

### Troubleshooting: "Failed to get document because the client is offline"
If auth works but reads fail as "offline", check logcat. A `FAILED_PRECONDITION` with
`The Cloud Firestore API is not available for Firestore in Datastore Mode database ...`
means the project's `(default)` database was created in **Datastore Mode**, which the
Firestore SDK cannot use (and it cannot be converted to Native mode). Fix options:

- **Option A (recommended):** Create a new Firebase project, enable **Firestore Database**
  in **Native mode**, add the Android app (`com.studio71.germanlinia2_b2` + debug SHA-1),
  download the new `google-services.json`, and replace `app/google-services.json`.
- **Option B (same project):** In Google Cloud Console, create an additional Firestore
  database in **Native mode** with a name (e.g. `germansync`), then set
  `FIRESTORE_DATABASE_ID = "germansync"` in
  `app/.../data/sync/CloudSyncManager.kt`.

The diagnostics panel (Settings -> Cloud-Sync -> Diagnose anzeigen) shows the active
`Firestore-DB` id so you can confirm which database the app targets.

## Features implemented
- **List view** with search (word / EN / DE) and filters: Niveau, Buch, Kapitel,
  Wortart, Grammatik-Gruppe. Sort by A–Z (no article), frequency, or chapter.
- **Card view** (tap a list item): full details + forward/back paging through the
  current filtered list. Shows verb forms (3rd person, Präteritum, Perfekt),
  adjective comparative/superlative, grammar group + preposition + case,
  memory trick, synonyms/antonyms as chips.
- **Meaning image per word**: the app looks up the first Google Image result via
  Google Custom Search Image API (German-first query), stores the URL in Room,
  and caches image bytes on-device via Coil. Failed/no-result lookups can be
  retried manually.

### Google image API setup
Add these properties (for local dev) to your global Gradle properties file:

`%USERPROFILE%\\.gradle\\gradle.properties`

```
googleImageApiKey=YOUR_API_KEY
googleImageSearchCx=YOUR_SEARCH_ENGINE_ID
```
- **Synonym/antonym popup**: tap a chip to open a quick detail dialog for that word.
- **Text-to-speech** (German) on list items, cards, and the popup.
- **Spaced repetition**: schedule Day 1 → 2 → 3 → 7 → 15 → 30 (`SrsScheduler`).
  "Als gelernt markieren", "Gewusst", "Nochmal" update the schedule; the card shows
  last-reviewed and next-due dates.
- **Stats screen** (bar-chart icon, top-right): bar chart of learned + reviewed per
  day, switchable Woche / Monat / Jahr, total learned, and current streak.

## Project layout (`com.studio71.germanlinia2_b2`)
```
GermanApp.kt              Application + manual service locator; seeds DB
MainActivity.kt           Compose entry point
data/
  local/                  Room: VocabularyEntity, ProgressEntity, DailyStatEntity,
                          DAOs, AppDatabase
  csv/                    CsvVocabularyImporter (quoted-CSV parser)
  repo/                   VocabularyRepository, VocabularyFilter, SortMode
domain/srs/               SrsScheduler (1/2/3/7/15/30-day spacing)
ui/
  list/                   VocabularyListScreen + ViewModel
  card/                   WordCardScreen + ViewModel + CardDeck (paging)
  stats/                  StatsScreen + ViewModel (bar chart, streak)
  components/             FilterDropdown, WordDetailDialog
  tts/                    GermanSpeaker (TextToSpeech)
  theme/                  GermanVocabTheme
  nav/                    AppNav (NavHost: list / card / stats)
```

## Adding vocabulary
1. Edit `data/vocabulary_template.csv` (schema in `data/README_VOCAB_SCHEMA.md`).
2. Copy the finished CSV to `app/src/main/assets/vocabulary.csv`.
3. Reinstall (or clear app data) so the seed re-imports. Stable `id`s keep your
   learning progress intact across re-imports.

> The bundled CSV currently contains 10 sample rows covering every word type and
> B2-Beruf grammar group, so the app runs out of the box. Replace them with your
> real chapter lists as you go.

