package com.studio71.germanlinia2_b2.data.settings

import android.content.Context
import com.studio71.germanlinia2_b2.data.local.WordMarker
import com.studio71.germanlinia2_b2.data.repo.SortMode
import com.studio71.germanlinia2_b2.data.repo.VocabularyFilter
import com.studio71.germanlinia2_b2.data.sync.SyncStateTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

private const val DEFAULT_CUSTOM_PRIMARY = "#3F51B5"
private const val DEFAULT_CUSTOM_SECONDARY = "#00897B"
private const val DEFAULT_CUSTOM_TERTIARY = "#EF6C00"

enum class ThemePreset(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    LIGHT_EYE_CARE("light_eye_care"),
    LIGHT_MINT("light_mint"),
    LIGHT_LAVENDER("light_lavender"),
    LIGHT_PEACH("light_peach"),
    OCEAN("ocean"),
    SUNSET("sunset"),
    FOREST("forest"),
    AMOLED("amoled"),
    CUSTOM("custom");

    companion object {
        fun fromStorage(value: String?): ThemePreset {
            return entries.firstOrNull { it.storageValue == value } ?: SYSTEM
        }
    }
}

data class TtsHistoryEntry(
    val id: Long,
    val playedAtEpochMs: Long,
    val level: String? = null,
    val book: String? = null,
    val chapter: String? = null,
    val pos: String? = null,
    val grammarGroup: String? = null,
    val markerValue: Int? = null,
    val query: String = "",
    val sortModeKey: String = SortMode.SOURCE.key,
    val startIndex: Int = 0,
    val lastIndex: Int = 0,
    val totalCount: Int = 0
) {
    val fromPosition: Int get() = (startIndex + 1).coerceAtLeast(1)
    val toPosition: Int get() = (lastIndex + 1).coerceAtLeast(fromPosition)
    val resumeIndex: Int get() = lastIndex.coerceAtLeast(startIndex)

    fun toFilter(): VocabularyFilter = VocabularyFilter(
        level = level,
        book = book,
        chapter = chapter,
        pos = pos,
        grammarGroup = grammarGroup,
        marker = WordMarker.fromValue(markerValue).takeIf { it != WordMarker.NONE }
    )

    fun toSortMode(): SortMode = SortMode.entries.firstOrNull { it.key == sortModeKey } ?: SortMode.SOURCE

    fun filterSummary(): String {
        val parts = listOfNotNull(
            level,
            book,
            chapter,
            pos?.let { "POS $it" },
            grammarGroup,
            markerValue?.let { "Marker ${WordMarker.fromValue(it).name}" },
            query.takeIf { it.isNotBlank() }?.let { "Suche \"$it\"" },
            toSortMode().label
        )
        return parts.joinToString(" | ").ifBlank { "Keine Filter" }
    }

    fun rangeSummary(): String {
        val total = if (totalCount > 0) " / $totalCount" else ""
        return "$fromPosition -> $toPosition$total"
    }
}

data class AppSettings(
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val reminderWindowStartHour: Int = 6,
    val reminderWindowStartMinute: Int = 0,
    val reminderWindowEndHour: Int = 23,
    val reminderWindowEndMinute: Int = 0,
    val reminderIntervalMinHours: Int = 2,
    val reminderIntervalMaxHours: Int = 3,
    val autoSpeakOnReveal: Boolean = false,
    val autoAddSeenToReview: Boolean = false,
    val gameMatchingEnabled: Boolean = true,
    val gameClozeEnabled: Boolean = true,
    val gameRecentEnabled: Boolean = true,
    val gameReverseRecallEnabled: Boolean = false,
    val nextGameModeIndex: Int = 0,
    val gameCoverageCursor: Int = 0,
    /** How often the headword itself is spoken within one loop. */
    val ttsWordRepeat: Int = 1,
    /** How often the (English) meaning is spoken within one loop. */
    val ttsMeaningRepeat: Int = 1,
    /** How often the German example sentence is spoken within one loop. */
    val ttsExampleRepeat: Int = 1,
    /** How often the verb / adjective forms are spoken within one loop. */
    val ttsVerbFormRepeat: Int = 1,
    /** How many times the whole [word → meaning → example → forms] block repeats per word. */
    val ttsLoopCount: Int = 2,
    /** Pause (milliseconds) inserted between two spoken parts. */
    val ttsGapMs: Int = 350,
    /** Insert a longer recall pause after every N spoken words. */
    val ttsRecallPauseEnabled: Boolean = false,
    /** Number of words between recall pauses. */
    val ttsRecallPauseEveryWords: Int = 10,
    /** Recall pause duration in milliseconds. */
    val ttsRecallPauseMs: Int = 5000,
    /** Playback speech rate (1.0 = normal). */
    val ttsSpeechRate: Float = 1.0f,
    /** Active app theme preset. */
    val themePreset: ThemePreset = ThemePreset.SYSTEM,
    /** Custom primary theme color as #RRGGBB or #AARRGGBB. */
    val themeCustomPrimary: String = DEFAULT_CUSTOM_PRIMARY,
    /** Custom secondary theme color as #RRGGBB or #AARRGGBB. */
    val themeCustomSecondary: String = DEFAULT_CUSTOM_SECONDARY,
    /** Custom tertiary theme color as #RRGGBB or #AARRGGBB. */
    val themeCustomTertiary: String = DEFAULT_CUSTOM_TERTIARY,
    /** Recently played TTS sessions (newest first). */
    val ttsHistory: List<TtsHistoryEntry> = emptyList(),
    /** Active review screen view mode (false = card, true = list). */
    val reviewAsList: Boolean = false
) {
    val reminderTimeLabel: String
        get() = "%02d:%02d".format(reminderHour, reminderMinute)

    val reminderWindowLabel: String
        get() = "%02d:%02d-%02d:%02d".format(
            reminderWindowStartHour,
            reminderWindowStartMinute,
            reminderWindowEndHour,
            reminderWindowEndMinute
        )
}

/** Simple SharedPreferences-backed settings, exposed reactively for Compose. */
class SettingsStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs =
        appContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun load() = AppSettings(
        reminderEnabled = prefs.getBoolean(KEY_ENABLED, true),
        reminderHour = prefs.getInt(KEY_HOUR, 19),
        reminderMinute = prefs.getInt(KEY_MINUTE, 0),
        reminderWindowStartHour = prefs.getInt(KEY_REMINDER_WINDOW_START_HOUR, 6),
        reminderWindowStartMinute = prefs.getInt(KEY_REMINDER_WINDOW_START_MINUTE, 0),
        reminderWindowEndHour = prefs.getInt(KEY_REMINDER_WINDOW_END_HOUR, 23),
        reminderWindowEndMinute = prefs.getInt(KEY_REMINDER_WINDOW_END_MINUTE, 0),
        reminderIntervalMinHours = prefs.getInt(KEY_REMINDER_INTERVAL_MIN_HOURS, 2).coerceIn(1, 12),
        reminderIntervalMaxHours = prefs.getInt(KEY_REMINDER_INTERVAL_MAX_HOURS, 3).coerceIn(1, 12),
        autoSpeakOnReveal = prefs.getBoolean(KEY_AUTOSPEAK, false),
        autoAddSeenToReview = prefs.getBoolean(KEY_AUTO_ADD_SEEN, false),
        gameMatchingEnabled = prefs.getBoolean(KEY_GAME_MATCHING_ENABLED, true),
        gameClozeEnabled = prefs.getBoolean(KEY_GAME_CLOZE_ENABLED, true),
        gameRecentEnabled = prefs.getBoolean(KEY_GAME_RECENT_ENABLED, true),
        gameReverseRecallEnabled = prefs.getBoolean(KEY_GAME_REVERSE_RECALL_ENABLED, false),
        nextGameModeIndex = prefs.getInt(KEY_NEXT_GAME_MODE_INDEX, 0).coerceAtLeast(0),
        gameCoverageCursor = prefs.getInt(KEY_GAME_COVERAGE_CURSOR, 0).coerceAtLeast(0),
        ttsWordRepeat = prefs.getInt(KEY_TTS_WORD, 1),
        ttsMeaningRepeat = prefs.getInt(KEY_TTS_MEANING, 1),
        ttsExampleRepeat = prefs.getInt(KEY_TTS_EXAMPLE, 1),
        ttsVerbFormRepeat = prefs.getInt(KEY_TTS_VERB, 1),
        ttsLoopCount = prefs.getInt(KEY_TTS_LOOP, 2),
        ttsGapMs = prefs.getInt(KEY_TTS_GAP, 350),
        ttsRecallPauseEnabled = prefs.getBoolean(KEY_TTS_RECALL_PAUSE_ENABLED, false),
        ttsRecallPauseEveryWords = prefs.getInt(KEY_TTS_RECALL_PAUSE_EVERY_WORDS, 10),
        ttsRecallPauseMs = prefs.getInt(KEY_TTS_RECALL_PAUSE_MS, 5000),
        ttsSpeechRate = prefs.getFloat(KEY_TTS_RATE, 1.0f),
        themePreset = ThemePreset.fromStorage(prefs.getString(KEY_THEME, ThemePreset.SYSTEM.storageValue)),
        themeCustomPrimary = prefs.getString(KEY_THEME_CUSTOM_PRIMARY, DEFAULT_CUSTOM_PRIMARY) ?: DEFAULT_CUSTOM_PRIMARY,
        themeCustomSecondary = prefs.getString(KEY_THEME_CUSTOM_SECONDARY, DEFAULT_CUSTOM_SECONDARY) ?: DEFAULT_CUSTOM_SECONDARY,
        themeCustomTertiary = prefs.getString(KEY_THEME_CUSTOM_TERTIARY, DEFAULT_CUSTOM_TERTIARY) ?: DEFAULT_CUSTOM_TERTIARY,
        ttsHistory = parseHistory(prefs.getString(KEY_TTS_HISTORY, "[]")),
        reviewAsList = prefs.getBoolean(KEY_REVIEW_AS_LIST, false)
    )

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = _state.value.copy(reminderEnabled = enabled)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
        _state.value = _state.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    fun setReminderWindowStart(hour: Int, minute: Int) {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        prefs.edit()
            .putInt(KEY_REMINDER_WINDOW_START_HOUR, h)
            .putInt(KEY_REMINDER_WINDOW_START_MINUTE, m)
            .apply()
        _state.value = _state.value.copy(
            reminderWindowStartHour = h,
            reminderWindowStartMinute = m
        )
    }

    fun setReminderWindowEnd(hour: Int, minute: Int) {
        val h = hour.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        prefs.edit()
            .putInt(KEY_REMINDER_WINDOW_END_HOUR, h)
            .putInt(KEY_REMINDER_WINDOW_END_MINUTE, m)
            .apply()
        _state.value = _state.value.copy(
            reminderWindowEndHour = h,
            reminderWindowEndMinute = m
        )
    }

    fun setReminderIntervalMinHours(hours: Int) {
        val min = hours.coerceIn(1, 12)
        val max = _state.value.reminderIntervalMaxHours.coerceAtLeast(min)
        prefs.edit()
            .putInt(KEY_REMINDER_INTERVAL_MIN_HOURS, min)
            .putInt(KEY_REMINDER_INTERVAL_MAX_HOURS, max)
            .apply()
        _state.value = _state.value.copy(
            reminderIntervalMinHours = min,
            reminderIntervalMaxHours = max
        )
    }

    fun setReminderIntervalMaxHours(hours: Int) {
        val max = hours.coerceIn(1, 12)
        val min = _state.value.reminderIntervalMinHours.coerceAtMost(max)
        prefs.edit()
            .putInt(KEY_REMINDER_INTERVAL_MIN_HOURS, min)
            .putInt(KEY_REMINDER_INTERVAL_MAX_HOURS, max)
            .apply()
        _state.value = _state.value.copy(
            reminderIntervalMinHours = min,
            reminderIntervalMaxHours = max
        )
    }

    fun setAutoSpeakOnReveal(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOSPEAK, enabled).apply()
        _state.value = _state.value.copy(autoSpeakOnReveal = enabled)
    }

    fun setAutoAddSeenToReview(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ADD_SEEN, enabled).apply()
        _state.value = _state.value.copy(autoAddSeenToReview = enabled)
    }

    fun setGameMatchingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAME_MATCHING_ENABLED, enabled).apply()
        _state.value = _state.value.copy(gameMatchingEnabled = enabled)
    }

    fun setGameClozeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAME_CLOZE_ENABLED, enabled).apply()
        _state.value = _state.value.copy(gameClozeEnabled = enabled)
    }

    fun setGameRecentEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAME_RECENT_ENABLED, enabled).apply()
        _state.value = _state.value.copy(gameRecentEnabled = enabled)
    }

    fun setGameReverseRecallEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GAME_REVERSE_RECALL_ENABLED, enabled).apply()
        _state.value = _state.value.copy(gameReverseRecallEnabled = enabled)
    }

    fun setNextGameModeIndex(index: Int) {
        val value = index.coerceAtLeast(0)
        prefs.edit().putInt(KEY_NEXT_GAME_MODE_INDEX, value).apply()
        _state.value = _state.value.copy(nextGameModeIndex = value)
    }

    fun setGameCoverageCursor(cursor: Int) {
        val value = cursor.coerceAtLeast(0)
        prefs.edit().putInt(KEY_GAME_COVERAGE_CURSOR, value).apply()
        _state.value = _state.value.copy(gameCoverageCursor = value)
    }

    fun setReviewAsList(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REVIEW_AS_LIST, enabled).apply()
        _state.value = _state.value.copy(reviewAsList = enabled)
    }

    fun setTtsWordRepeat(value: Int) {
        val v = value.coerceIn(0, 10)
        prefs.edit().putInt(KEY_TTS_WORD, v).apply()
        _state.value = _state.value.copy(ttsWordRepeat = v)
    }

    fun setTtsMeaningRepeat(value: Int) {
        val v = value.coerceIn(0, 10)
        prefs.edit().putInt(KEY_TTS_MEANING, v).apply()
        _state.value = _state.value.copy(ttsMeaningRepeat = v)
    }

    fun setTtsExampleRepeat(value: Int) {
        val v = value.coerceIn(0, 10)
        prefs.edit().putInt(KEY_TTS_EXAMPLE, v).apply()
        _state.value = _state.value.copy(ttsExampleRepeat = v)
    }

    fun setTtsVerbFormRepeat(value: Int) {
        val v = value.coerceIn(0, 10)
        prefs.edit().putInt(KEY_TTS_VERB, v).apply()
        _state.value = _state.value.copy(ttsVerbFormRepeat = v)
    }

    fun setTtsLoopCount(value: Int) {
        val v = value.coerceIn(1, 20)
        prefs.edit().putInt(KEY_TTS_LOOP, v).apply()
        _state.value = _state.value.copy(ttsLoopCount = v)
    }

    fun setTtsGapMs(value: Int) {
        val v = value.coerceIn(0, 5000)
        prefs.edit().putInt(KEY_TTS_GAP, v).apply()
        _state.value = _state.value.copy(ttsGapMs = v)
    }

    fun setTtsRecallPauseEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TTS_RECALL_PAUSE_ENABLED, enabled).apply()
        _state.value = _state.value.copy(ttsRecallPauseEnabled = enabled)
    }

    fun setTtsRecallPauseEveryWords(value: Int) {
        val v = value.coerceIn(1, 200)
        prefs.edit().putInt(KEY_TTS_RECALL_PAUSE_EVERY_WORDS, v).apply()
        _state.value = _state.value.copy(ttsRecallPauseEveryWords = v)
    }

    fun setTtsRecallPauseMs(value: Int) {
        val v = value.coerceIn(1000, 60000)
        prefs.edit().putInt(KEY_TTS_RECALL_PAUSE_MS, v).apply()
        _state.value = _state.value.copy(ttsRecallPauseMs = v)
    }

    fun setTtsSpeechRate(value: Float) {
        val v = value.coerceIn(0.5f, 2.0f)
        prefs.edit().putFloat(KEY_TTS_RATE, v).apply()
        _state.value = _state.value.copy(ttsSpeechRate = v)
    }

    fun setThemePreset(value: ThemePreset) {
        prefs.edit().putString(KEY_THEME, value.storageValue).apply()
        _state.value = _state.value.copy(themePreset = value)
    }

    fun setCustomThemeColors(primary: String, secondary: String, tertiary: String) {
        val current = _state.value
        val normalizedPrimary = normalizeColorHex(primary, current.themeCustomPrimary)
        val normalizedSecondary = normalizeColorHex(secondary, current.themeCustomSecondary)
        val normalizedTertiary = normalizeColorHex(tertiary, current.themeCustomTertiary)

        prefs.edit()
            .putString(KEY_THEME_CUSTOM_PRIMARY, normalizedPrimary)
            .putString(KEY_THEME_CUSTOM_SECONDARY, normalizedSecondary)
            .putString(KEY_THEME_CUSTOM_TERTIARY, normalizedTertiary)
            .apply()

        _state.value = current.copy(
            themeCustomPrimary = normalizedPrimary,
            themeCustomSecondary = normalizedSecondary,
            themeCustomTertiary = normalizedTertiary
        )
    }

    fun addTtsHistory(entry: TtsHistoryEntry) {
        val updated = listOf(entry) + _state.value.ttsHistory.filterNot { it.id == entry.id }
        persistHistory(updated.take(HISTORY_LIMIT), markDirty = true)
    }

    fun clearTtsHistory() {
        persistHistory(emptyList(), markDirty = true)
    }

    fun replaceTtsHistoryFromSync(entries: List<TtsHistoryEntry>) {
        persistHistory(entries.take(HISTORY_LIMIT), markDirty = false)
    }

    private fun persistHistory(entries: List<TtsHistoryEntry>, markDirty: Boolean) {
        prefs.edit().putString(KEY_TTS_HISTORY, historyToJson(entries)).apply()
        _state.value = _state.value.copy(ttsHistory = entries)
        if (markDirty) SyncStateTracker.markLocalMutation(appContext)
    }

    private fun parseHistory(raw: String?): List<TtsHistoryEntry> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(
                        TtsHistoryEntry(
                            id = obj.optLong("id", 0L),
                            playedAtEpochMs = obj.optLong("playedAtEpochMs", 0L),
                            level = obj.optNullableString("level"),
                            book = obj.optNullableString("book"),
                            chapter = obj.optNullableString("chapter"),
                            pos = obj.optNullableString("pos"),
                            grammarGroup = obj.optNullableString("grammarGroup"),
                            markerValue = obj.optNullableInt("markerValue"),
                            query = obj.optString("query", ""),
                            sortModeKey = obj.optString("sortModeKey", SortMode.SOURCE.key),
                            startIndex = obj.optInt("startIndex", 0),
                            lastIndex = obj.optInt("lastIndex", 0),
                            totalCount = obj.optInt("totalCount", 0)
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
            .filter { it.id > 0L }
            .sortedByDescending { it.playedAtEpochMs }
            .take(HISTORY_LIMIT)
    }

    private fun historyToJson(entries: List<TtsHistoryEntry>): String {
        val array = JSONArray()
        entries.forEach { e ->
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("playedAtEpochMs", e.playedAtEpochMs)
                    .putOpt("level", e.level)
                    .putOpt("book", e.book)
                    .putOpt("chapter", e.chapter)
                    .putOpt("pos", e.pos)
                    .putOpt("grammarGroup", e.grammarGroup)
                    .putOpt("markerValue", e.markerValue)
                    .put("query", e.query)
                    .put("sortModeKey", e.sortModeKey)
                    .put("startIndex", e.startIndex)
                    .put("lastIndex", e.lastIndex)
                    .put("totalCount", e.totalCount)
            )
        }
        return array.toString()
    }

    private fun normalizeColorHex(input: String, fallback: String): String {
        val cleaned = input.trim().removePrefix("#").uppercase()
        val isValid = (cleaned.length == 6 || cleaned.length == 8) && cleaned.all { it in "0123456789ABCDEF" }
        return if (isValid) "#$cleaned" else fallback
    }

    private companion object {
        const val KEY_ENABLED = "reminder_enabled"
        const val KEY_HOUR = "reminder_hour"
        const val KEY_MINUTE = "reminder_minute"
        const val KEY_REMINDER_WINDOW_START_HOUR = "reminder_window_start_hour"
        const val KEY_REMINDER_WINDOW_START_MINUTE = "reminder_window_start_minute"
        const val KEY_REMINDER_WINDOW_END_HOUR = "reminder_window_end_hour"
        const val KEY_REMINDER_WINDOW_END_MINUTE = "reminder_window_end_minute"
        const val KEY_REMINDER_INTERVAL_MIN_HOURS = "reminder_interval_min_hours"
        const val KEY_REMINDER_INTERVAL_MAX_HOURS = "reminder_interval_max_hours"
        const val KEY_AUTOSPEAK = "auto_speak_on_reveal"
        const val KEY_AUTO_ADD_SEEN = "auto_add_seen_to_review"
        const val KEY_GAME_MATCHING_ENABLED = "game_matching_enabled"
        const val KEY_GAME_CLOZE_ENABLED = "game_cloze_enabled"
        const val KEY_GAME_RECENT_ENABLED = "game_recent_enabled"
        const val KEY_GAME_REVERSE_RECALL_ENABLED = "game_reverse_recall_enabled"
        const val KEY_NEXT_GAME_MODE_INDEX = "next_game_mode_index"
        const val KEY_GAME_COVERAGE_CURSOR = "game_coverage_cursor"
        const val KEY_TTS_WORD = "tts_word_repeat"
        const val KEY_TTS_MEANING = "tts_meaning_repeat"
        const val KEY_TTS_EXAMPLE = "tts_example_repeat"
        const val KEY_TTS_VERB = "tts_verb_repeat"
        const val KEY_TTS_LOOP = "tts_loop_count"
        const val KEY_TTS_GAP = "tts_gap_ms"
        const val KEY_TTS_RECALL_PAUSE_ENABLED = "tts_recall_pause_enabled"
        const val KEY_TTS_RECALL_PAUSE_EVERY_WORDS = "tts_recall_pause_every_words"
        const val KEY_TTS_RECALL_PAUSE_MS = "tts_recall_pause_ms"
        const val KEY_TTS_RATE = "tts_speech_rate"
        const val KEY_THEME = "theme_preset"
        const val KEY_THEME_CUSTOM_PRIMARY = "theme_custom_primary"
        const val KEY_THEME_CUSTOM_SECONDARY = "theme_custom_secondary"
        const val KEY_THEME_CUSTOM_TERTIARY = "theme_custom_tertiary"
        const val KEY_TTS_HISTORY = "tts_history"
        const val KEY_REVIEW_AS_LIST = "review_as_list"
        const val HISTORY_LIMIT = 20
    }
}

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else optString(key).ifBlank { null }

private fun JSONObject.optNullableInt(key: String): Int? =
    if (isNull(key)) null else optInt(key)

