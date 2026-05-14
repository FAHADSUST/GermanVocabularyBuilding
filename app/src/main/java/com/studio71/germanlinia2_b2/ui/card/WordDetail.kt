package com.studio71.germanlinia2_b2.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity

/**
 * Reusable compact word body (header + meaning/forms/grammar + synonyms/antonyms).
 * Used by both the browse card and the review session. The caller supplies scrolling.
 *
 * When [revealed] is false, only the headword is shown (active-recall mode); the meaning
 * and other details stay hidden until the user reveals them.
 */
@Composable
fun WordDetail(
    word: VocabularyEntity,
    onSpeak: (String) -> Unit,
    onRelationClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    revealed: Boolean = true
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HeaderCard(word, onSpeak = { onSpeak(word.word) })

        if (!revealed) {
            InfoCard {
                Text(
                    "Bedeutung verdeckt — denke nach und tippe „Antwort zeigen“.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

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

        if (word.synonymList.isNotEmpty() || word.antonymList.isNotEmpty()) {
            InfoCard {
                RelationRow("Syn.", word.synonymList, onRelationClick)
                RelationRow("Ant.", word.antonymList, onRelationClick)
            }
        }
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

/** Compact card container without per-section dividers. */
@Composable
internal fun InfoCard(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) { content() }
    }
}

/** One-line "Label value" with the label emphasized; hidden when value is blank. */
@Composable
internal fun Labeled(label: String, value: String) {
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
    val parts = listOf(word.verbPresent3rd, word.verbPast, word.verbPerfect).filter { it.isNotBlank() }
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

