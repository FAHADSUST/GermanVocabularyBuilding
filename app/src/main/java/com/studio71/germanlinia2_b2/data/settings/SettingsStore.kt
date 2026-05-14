package com.studio71.germanlinia2_b2.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    val ttsSpeechRate: Float = 1.0f
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
        ttsSpeechRate = prefs.getFloat(KEY_TTS_RATE, 1.0f)
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
    }
}

