package com.studio71.germanlinia2_b2.ui.tts

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** High-level playback status shared between the foreground service and the UI. */
enum class TtsPlaybackState { IDLE, PLAYING, PAUSED }

/**
 * Process-wide bridge between the UI (Compose) and the [TtsPlaybackService].
 *
 * The UI observes the [playbackState] / [currentWordId] flows to render the in-app
 * playback bar, and issues commands (play / pause / next / …) which are delivered to
 * the running foreground service via intents. Because both live in the same process,
 * the (potentially large) word list is shared in-memory here rather than through the
 * intent, which keeps things cheap.
 */
object TtsController {

    const val ACTION_START = "com.studio71.germanlinia2_b2.tts.START"
    const val ACTION_JUMP = "com.studio71.germanlinia2_b2.tts.JUMP"
    const val ACTION_TOGGLE = "com.studio71.germanlinia2_b2.tts.TOGGLE"
    const val ACTION_PAUSE = "com.studio71.germanlinia2_b2.tts.PAUSE"
    const val ACTION_RESUME = "com.studio71.germanlinia2_b2.tts.RESUME"
    const val ACTION_NEXT = "com.studio71.germanlinia2_b2.tts.NEXT"
    const val ACTION_PREV = "com.studio71.germanlinia2_b2.tts.PREV"
    const val ACTION_STOP = "com.studio71.germanlinia2_b2.tts.STOP"
    const val EXTRA_START_INDEX = "start_index"

    /** The list currently queued for playback (shared in-memory with the service). */
    @Volatile
    var playlist: List<VocabularyEntity> = emptyList()
        private set

    private val _playbackState = MutableStateFlow(TtsPlaybackState.IDLE)
    val playbackState: StateFlow<TtsPlaybackState> = _playbackState.asStateFlow()

    private val _currentWordId = MutableStateFlow<String?>(null)
    val currentWordId: StateFlow<String?> = _currentWordId.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // ---- Commands from the UI ------------------------------------------------

    /** Start (or restart) playback of [words], beginning at [startIndex]. */
    fun start(context: Context, words: List<VocabularyEntity>, startIndex: Int) {
        if (words.isEmpty()) return
        playlist = words
        send(context, ACTION_START, startIndex.coerceIn(0, words.lastIndex))
    }

    /**
     * Jump within the already-loaded [playlist] to [index] and start speaking from there.
     * Keeps the current queue intact (unlike [start], which replaces it).
     */
    fun jumpTo(context: Context, index: Int) {
        if (index !in playlist.indices) return
        send(context, ACTION_JUMP, index)
    }

    fun togglePlayPause(context: Context) = send(context, ACTION_TOGGLE)
    fun next(context: Context) = send(context, ACTION_NEXT)
    fun previous(context: Context) = send(context, ACTION_PREV)
    fun stop(context: Context) = send(context, ACTION_STOP)

    private fun send(context: Context, action: String, startIndex: Int = -1) {
        val intent = Intent(context, TtsPlaybackService::class.java).apply {
            this.action = action
            if (startIndex >= 0) putExtra(EXTRA_START_INDEX, startIndex)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    // ---- Updates from the service -------------------------------------------

    internal fun publishState(state: TtsPlaybackState) { _playbackState.value = state }

    internal fun publishCurrent(index: Int, wordId: String?) {
        _currentIndex.value = index
        _currentWordId.value = wordId
    }

    internal fun reset() {
        _playbackState.value = TtsPlaybackState.IDLE
        _currentIndex.value = -1
        _currentWordId.value = null
    }
}

