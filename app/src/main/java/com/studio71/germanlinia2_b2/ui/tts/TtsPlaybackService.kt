package com.studio71.germanlinia2_b2.ui.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.studio71.germanlinia2_b2.GermanApp
import com.studio71.germanlinia2_b2.MainActivity
import com.studio71.germanlinia2_b2.R
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.settings.AppSettings
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Foreground service that reads through the (filtered) vocabulary list using the
 * device's German/English TTS engine. It honours the per-part repeat counts and the
 * loop count configured in settings, posts a media-style notification with
 * play/pause/next/previous/stop controls, and keeps the current position so playback
 * can be paused and resumed from the same word.
 */
class TtsPlaybackService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private var pendingStartIndex: Int? = null

    private val paused = MutableStateFlow(false)
    private var playbackJob: Job? = null
    private var currentIndex = -1

    private val pending = ConcurrentHashMap<String, CancellableContinuation<Unit>>()
    private val idGen = AtomicInteger(0)

    private val playlist: List<VocabularyEntity> get() = TtsController.playlist
    private val settings get() = (application as GermanApp).settings.state.value

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        tts = TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.GERMAN
                tts.setOnUtteranceProgressListener(progressListener)
                ttsReady = true
                pendingStartIndex?.let { idx ->
                    pendingStartIndex = null
                    startForegroundSafely()
                    playFrom(idx)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            TtsController.ACTION_START -> {
                val idx = intent.getIntExtra(TtsController.EXTRA_START_INDEX, 0)
                startForegroundSafely()
                if (ttsReady) playFrom(idx) else pendingStartIndex = idx
            }
            TtsController.ACTION_TOGGLE -> {
                when (TtsController.playbackState.value) {
                    TtsPlaybackState.PLAYING -> pause()
                    TtsPlaybackState.PAUSED -> resume()
                    TtsPlaybackState.IDLE -> if (ttsReady) playFrom(0) else pendingStartIndex = 0
                }
            }
            TtsController.ACTION_PAUSE -> pause()
            TtsController.ACTION_RESUME -> resume()
            TtsController.ACTION_NEXT -> next()
            TtsController.ACTION_PREV -> previous()
            TtsController.ACTION_STOP -> stopPlayback()
        }
        return START_NOT_STICKY
    }

    // ---- Playback control ----------------------------------------------------

    private fun playFrom(startIndex: Int) {
        if (startIndex !in playlist.indices) {
            stopPlayback()
            return
        }
        playbackJob?.cancel()
        tts.setSpeechRate(settings.ttsSpeechRate)
        paused.value = false
        TtsController.publishState(TtsPlaybackState.PLAYING)
        playbackJob = scope.launch {
            var i = startIndex
            while (i in playlist.indices && isActive) {
                currentIndex = i
                playWord(playlist[i])
                i++
            }
            if (isActive) finishPlayback()
        }
    }

    private suspend fun playWord(word: VocabularyEntity) {
        currentIndex = playlist.indexOfFirst { it.id == word.id }.let { if (it >= 0) it else currentIndex }
        TtsController.publishCurrent(currentIndex, word.id)
        updateNotification()
        val s = settings
        val gap = s.ttsGapMs.toLong()
        for (step in buildSteps(word, s)) {
            awaitResumed()
            if (playbackJob?.isActive != true) return
            speakAndWait(step)
            if (gap > 0) delay(gap)
        }
    }

    private fun pause() {
        if (TtsController.playbackState.value != TtsPlaybackState.PLAYING) return
        paused.value = true
        tts.stop()
        TtsController.publishState(TtsPlaybackState.PAUSED)
        updateNotification()
    }

    private fun resume() {
        if (playbackJob?.isActive == true) {
            paused.value = false
            TtsController.publishState(TtsPlaybackState.PLAYING)
            updateNotification()
        } else {
            // Playback had finished (or never started) — restart at the last word.
            playFrom(currentIndex.coerceIn(0, (playlist.size - 1).coerceAtLeast(0)))
        }
    }

    private fun next() {
        if (playlist.isEmpty()) return
        val target = currentIndex + 1
        if (target in playlist.indices) playFrom(target) else stopPlayback()
    }

    private fun previous() {
        if (playlist.isEmpty()) return
        playFrom((currentIndex - 1).coerceAtLeast(0))
    }

    private fun finishPlayback() {
        TtsController.publishState(TtsPlaybackState.PAUSED)
        // Keep the notification but mark it as paused at the end of the list.
        paused.value = true
        updateNotification()
    }

    private fun stopPlayback() {
        playbackJob?.cancel()
        paused.value = false
        currentIndex = -1
        if (::tts.isInitialized) tts.stop()
        TtsController.reset()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun awaitResumed() {
        if (!paused.value) return
        paused.first { !it }
    }

    private suspend fun speakAndWait(step: Step): Unit = suspendCancellableCoroutine { cont ->
        val id = "u" + idGen.incrementAndGet()
        pending[id] = cont
        tts.language = step.locale
        val result = tts.speak(step.text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (result != TextToSpeech.SUCCESS) {
            pending.remove(id)
            if (cont.isActive) cont.resume(Unit)
        }
        cont.invokeOnCancellation { pending.remove(id) }
    }

    private val progressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) = finishUtterance(utteranceId)
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) = finishUtterance(utteranceId)
        override fun onError(utteranceId: String?, errorCode: Int) = finishUtterance(utteranceId)
        override fun onStop(utteranceId: String?, interrupted: Boolean) = finishUtterance(utteranceId)
    }

    private fun finishUtterance(utteranceId: String?) {
        val cont = utteranceId?.let { pending.remove(it) } ?: return
        if (cont.isActive) cont.resume(Unit)
    }

    // ---- Step building -------------------------------------------------------

    private data class Step(val text: String, val locale: Locale)

    private fun buildSteps(word: VocabularyEntity, s: AppSettings): List<Step> {
        val steps = mutableListOf<Step>()
        val meaning = word.english.ifBlank { word.germanMeaning }
        val meaningLocale = if (word.english.isNotBlank()) Locale.ENGLISH else Locale.GERMAN
        repeat(s.ttsLoopCount.coerceAtLeast(1)) {
            repeat(s.ttsWordRepeat) { steps += Step(word.displayWord, Locale.GERMAN) }
            if (meaning.isNotBlank()) repeat(s.ttsMeaningRepeat) { steps += Step(meaning, meaningLocale) }
            if (word.exampleDe.isNotBlank()) repeat(s.ttsExampleRepeat) { steps += Step(word.exampleDe, Locale.GERMAN) }
            verbForms(word)?.let { vf -> repeat(s.ttsVerbFormRepeat) { steps += Step(vf, Locale.GERMAN) } }
        }
        return steps
    }

    /** "spricht, sprach, hat gesprochen" / "schneller, am schnellsten" or null. */
    private fun verbForms(word: VocabularyEntity): String? {
        val parts = when {
            word.isVerb -> listOf(word.verbPresent3rd, word.verbPast, word.verbPerfect)
            word.isAdjective -> listOf(word.adjComparative, word.adjSuperlative)
            else -> emptyList()
        }.filter { it.isNotBlank() }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    // ---- Notification --------------------------------------------------------

    private fun startForegroundSafely() {
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val word = playlist.getOrNull(currentIndex)
        val state = TtsController.playbackState.value
        val isPlaying = state == TtsPlaybackState.PLAYING
        val title = word?.displayWord ?: "Wortschatz"
        val text = when (state) {
            TtsPlaybackState.PLAYING -> word?.english.orEmpty()
            TtsPlaybackState.PAUSED -> "Pausiert — ${word?.english.orEmpty()}".trim()
            TtsPlaybackState.IDLE -> ""
        }

        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevAction = NotificationCompat.Action(
            android.R.drawable.ic_media_previous, "Zurück", command(TtsController.ACTION_PREV)
        )
        val toggleAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause, "Pause", command(TtsController.ACTION_TOGGLE)
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play, "Abspielen", command(TtsController.ACTION_TOGGLE)
            )
        }
        val nextAction = NotificationCompat.Action(
            android.R.drawable.ic_media_next, "Weiter", command(TtsController.ACTION_NEXT)
        )
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel, "Stopp", command(TtsController.ACTION_STOP)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_review)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setDeleteIntent(command(TtsController.ACTION_STOP))
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(prevAction)
            .addAction(toggleAction)
            .addAction(nextAction)
            .addAction(stopAction)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun command(action: String): PendingIntent {
        val intent = Intent(this, TtsPlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this, action.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vokabel-Wiedergabe",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Steuerung für das Vorlesen der Wortliste"
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackJob?.cancel()
        scope.coroutineContext[Job]?.cancel()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        TtsController.reset()
    }

    companion object {
        private const val CHANNEL_ID = "tts_playback"
        private const val NOTIFICATION_ID = 2001

        fun stop(context: Context) {
            context.stopService(Intent(context, TtsPlaybackService::class.java))
        }
    }
}

