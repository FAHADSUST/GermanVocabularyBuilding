package com.studio71.germanlinia2_b2.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

data class AppSettings(
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 19,
    val reminderMinute: Int = 0,
    val autoSpeakOnReveal: Boolean = false,
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
    /** Playback speech rate (1.0 = normal). */
    val ttsSpeechRate: Float = 1.0f,
    /** Active app theme preset. */
    val themePreset: ThemePreset = ThemePreset.SYSTEM,
    /** Custom primary theme color as #RRGGBB or #AARRGGBB. */
    val themeCustomPrimary: String = DEFAULT_CUSTOM_PRIMARY,
    /** Custom secondary theme color as #RRGGBB or #AARRGGBB. */
    val themeCustomSecondary: String = DEFAULT_CUSTOM_SECONDARY,
    /** Custom tertiary theme color as #RRGGBB or #AARRGGBB. */
    val themeCustomTertiary: String = DEFAULT_CUSTOM_TERTIARY
) {
    val reminderTimeLabel: String
        get() = "%02d:%02d".format(reminderHour, reminderMinute)
}

/** Simple SharedPreferences-backed settings, exposed reactively for Compose. */
class SettingsStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    private fun load() = AppSettings(
        reminderEnabled = prefs.getBoolean(KEY_ENABLED, true),
        reminderHour = prefs.getInt(KEY_HOUR, 19),
        reminderMinute = prefs.getInt(KEY_MINUTE, 0),
        autoSpeakOnReveal = prefs.getBoolean(KEY_AUTOSPEAK, false),
        ttsWordRepeat = prefs.getInt(KEY_TTS_WORD, 1),
        ttsMeaningRepeat = prefs.getInt(KEY_TTS_MEANING, 1),
        ttsExampleRepeat = prefs.getInt(KEY_TTS_EXAMPLE, 1),
        ttsVerbFormRepeat = prefs.getInt(KEY_TTS_VERB, 1),
        ttsLoopCount = prefs.getInt(KEY_TTS_LOOP, 2),
        ttsGapMs = prefs.getInt(KEY_TTS_GAP, 350),
        ttsSpeechRate = prefs.getFloat(KEY_TTS_RATE, 1.0f),
        themePreset = ThemePreset.fromStorage(prefs.getString(KEY_THEME, ThemePreset.SYSTEM.storageValue)),
        themeCustomPrimary = prefs.getString(KEY_THEME_CUSTOM_PRIMARY, DEFAULT_CUSTOM_PRIMARY) ?: DEFAULT_CUSTOM_PRIMARY,
        themeCustomSecondary = prefs.getString(KEY_THEME_CUSTOM_SECONDARY, DEFAULT_CUSTOM_SECONDARY) ?: DEFAULT_CUSTOM_SECONDARY,
        themeCustomTertiary = prefs.getString(KEY_THEME_CUSTOM_TERTIARY, DEFAULT_CUSTOM_TERTIARY) ?: DEFAULT_CUSTOM_TERTIARY
    )

    fun setReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        _state.value = _state.value.copy(reminderEnabled = enabled)
    }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
        _state.value = _state.value.copy(reminderHour = hour, reminderMinute = minute)
    }

    fun setAutoSpeakOnReveal(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOSPEAK, enabled).apply()
        _state.value = _state.value.copy(autoSpeakOnReveal = enabled)
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

    private fun normalizeColorHex(input: String, fallback: String): String {
        val cleaned = input.trim().removePrefix("#").uppercase()
        val isValid = (cleaned.length == 6 || cleaned.length == 8) && cleaned.all { it in "0123456789ABCDEF" }
        return if (isValid) "#$cleaned" else fallback
    }

    private companion object {
        const val KEY_ENABLED = "reminder_enabled"
        const val KEY_HOUR = "reminder_hour"
        const val KEY_MINUTE = "reminder_minute"
        const val KEY_AUTOSPEAK = "auto_speak_on_reveal"
        const val KEY_TTS_WORD = "tts_word_repeat"
        const val KEY_TTS_MEANING = "tts_meaning_repeat"
        const val KEY_TTS_EXAMPLE = "tts_example_repeat"
        const val KEY_TTS_VERB = "tts_verb_repeat"
        const val KEY_TTS_LOOP = "tts_loop_count"
        const val KEY_TTS_GAP = "tts_gap_ms"
        const val KEY_TTS_RATE = "tts_speech_rate"
        const val KEY_THEME = "theme_preset"
        const val KEY_THEME_CUSTOM_PRIMARY = "theme_custom_primary"
        const val KEY_THEME_CUSTOM_SECONDARY = "theme_custom_secondary"
        const val KEY_THEME_CUSTOM_TERTIARY = "theme_custom_tertiary"
    }
}

