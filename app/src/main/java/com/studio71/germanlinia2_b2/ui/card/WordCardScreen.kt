package com.studio71.germanlinia2_b2.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.data.local.ProgressEntity
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.ui.components.WordDetailDialog
import com.studio71.germanlinia2_b2.ui.tts.rememberGermanSpeaker
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun WordCardScreen(
    viewModel: WordCardViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val speaker = rememberGermanSpeaker()
    val scope = rememberCoroutineScope()
    var detailWord by remember { mutableStateOf<VocabularyEntity?>(null) }

    val word = state.word

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.total > 0) "${state.index + 1} / ${state.total}" else "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück zur Liste")
                    }
                }
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { viewModel.previous() }, enabled = state.hasPrevious) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Text("Zurück")
                }
                Button(
                    onClick = { viewModel.next() },
                    enabled = state.hasNext,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Weiter")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    ) { padding ->
        if (word == null) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                Text("Lädt …", Modifier.padding(16.dp))
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderCard(word, onSpeak = { speaker.speak(word.word) })

            // Meaning + example + forms + grammar + tip — all in one compact card.
            InfoCard {
                Labeled("EN", word.english)
                Labeled("DE", word.germanMeaning)
                if (word.exampleDe.isNotBlank()) {
                    Text(
                        "„${word.exampleDe}“",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                verbLine(word)?.let { Labeled("Formen", it) }
                adjLine(word)?.let { Labeled("Steigerung", it) }
                grammarLine(word)?.let { Labeled("Grammatik", it) }
                if (word.memoryTrick.isNotBlank()) Labeled("Tipp", word.memoryTrick)
            }

            // Synonyms / antonyms together.
            if (word.synonymList.isNotEmpty() || word.antonymList.isNotEmpty()) {
                InfoCard {
                    RelationRow("Syn.", word.synonymList) { value ->
                        scope.launch { detailWord = viewModel.lookup(value) ?: detailWord }
                    }
                    RelationRow("Ant.", word.antonymList) { value ->
                        scope.launch { detailWord = viewModel.lookup(value) ?: detailWord }
                    }
                }
            }

            ReviewCard(progress, viewModel)
        }
    }

    detailWord?.let { dw ->
        WordDetailDialog(
            word = dw,
            onDismiss = { detailWord = null },
            onSpeak = { speaker.speak(it) }
        )
    }
}

@Composable
private fun HeaderCard(word: VocabularyEntity, onSpeak: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    buildAnnotatedString {
                        append(word.displayWord)
                        if (word.plural.isNotBlank()) {
                            withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                append("  (Pl. ${word.plural})")
                            }
                        }
                    },
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "${word.level} · ${word.chapter}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSpeak) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Vorlesen")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RelationRow(label: String, words: List<String>, onClick: (String) -> Unit) {
    if (words.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "$label ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 4.dp, top = 2.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            words.forEach { w ->
                SuggestionChip(
                    onClick = { onClick(w) },
                    label = { Text(w, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }
    }
}

@Composable
private fun ReviewCard(progress: ProgressEntity?, viewModel: WordCardViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (progress == null) {
                FilledTonalButton(
                    onClick = { viewModel.markLearned() },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Als gelernt markieren") }
            } else {
                val last = progress.lastReviewed?.let { LocalDate.ofEpochDay(it).toString() } ?: "—"
                val due = LocalDate.ofEpochDay(progress.nextDue).toString()
                Text(
                    "Zuletzt: $last   ·   Nächste: $due",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.reviewFail() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, Modifier.size(18.dp))
                        Text(" Nochmal")
                    }
                    Button(
                        onClick = { viewModel.reviewSuccess() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp))
                        Text(" Gewusst")
                    }
                }
            }
        }
    }
}

/** Compact card container without per-section dividers. */
@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) { content() }
    }
}

/** One-line "Label value" with the label emphasized; hidden when value is blank. */
@Composable
private fun Labeled(label: String, value: String) {
    if (value.isBlank()) return
    Text(
        buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            ) { append("$label  ") }
            append(value)
        },
        style = MaterialTheme.typography.bodyMedium
    )
}

/** "spricht · sprach · hat gesprochen" or null if no verb forms. */
private fun verbLine(word: VocabularyEntity): String? {
    if (!word.isVerb) return null
    val parts = listOf(word.verbPresent3rd, word.verbPast, word.verbPerfect)
        .filter { it.isNotBlank() }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** "schneller · am schnellsten" or null. */
private fun adjLine(word: VocabularyEntity): String? {
    if (!word.isAdjective) return null
    val parts = listOf(word.adjComparative, word.adjSuperlative).filter { it.isNotBlank() }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** "Verben mit Präposition · an + Akkusativ" or null. */
private fun grammarLine(word: VocabularyEntity): String? {
    val prep = listOf(word.preposition, word.governCase).filter { it.isNotBlank() }.joinToString(" + ")
    val parts = listOf(word.grammarGroup, prep).filter { it.isNotBlank() }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
