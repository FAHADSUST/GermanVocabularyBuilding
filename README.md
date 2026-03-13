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

## Features implemented
- **List view** with search (word / EN / DE) and filters: Niveau, Buch, Kapitel,
  Wortart, Grammatik-Gruppe. Sort by A–Z (no article), frequency, or chapter.
- **Card view** (tap a list item): full details + forward/back paging through the
  current filtered list. Shows verb forms (3rd person, Präteritum, Perfekt),
  adjective comparative/superlative, grammar group + preposition + case,
  memory trick, synonyms/antonyms as chips.
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

