package com.studio71.germanlinia2_b2.ui.tts

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Holds a German TextToSpeech engine and exposes a simple speak() call. */
class GermanSpeaker(
    private val tts: TextToSpeech
) {
    fun speak(text: String) {
        if (text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun stop() {
        tts.stop()
    }
}

/**
 * Remembers a [GermanSpeaker] bound to the current composition and releases the
 * underlying engine when it leaves the composition.
 */
@Composable
fun rememberGermanSpeaker(): GermanSpeaker {
    val context = LocalContext.current
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.GERMAN
            }
        }
        engine
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    return remember { GermanSpeaker(tts) }
}

