package com.studio71.germanlinia2_b2.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.repo.WordImageState

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
    onCopyTextRequested: ((String) -> Unit)? = null,
    onTranslateTextRequested: ((String) -> Unit)? = null,
    onCommentRequested: (() -> Unit)? = null,
    comment: String = "",
    imageState: WordImageState = WordImageState(),
    onRetryImage: (() -> Unit)? = null,
    revealed: Boolean = true
) {
    var imageLoadFailed by remember(imageState.imageUrl) { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HeaderCard(
            word = word,
            onSpeak = { onSpeak(word.word) },
            onCopyTextRequested = onCopyTextRequested,
            onTranslateTextRequested = onTranslateTextRequested,
            onCommentRequested = onCommentRequested
        )

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

        when {
            /*imageState.imageUrl != null && !imageLoadFailed -> {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = imageState.imageUrl,
                        contentDescription = "Bedeutungsbild",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = ContentScale.Crop,
                        onError = { imageLoadFailed = true }
                    )
                }
            }

            imageLoadFailed -> {
                InfoCard {
                    Text(
                        "Bedeutungsbild konnte nicht dargestellt werden.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (onRetryImage != null) {
                        TextButton(onClick = onRetryImage) {
                            Text("Bild erneut suchen")
                        }
                    }
                }
            }

            imageState.isLoading -> {
                InfoCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                        Text(
                            "Bedeutungsbild wird geladen …",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }*/

            imageState.message.isNotBlank() -> {
                InfoCard {
                    Text(
                        imageState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (imageState.canRetry && onRetryImage != null) {
                        TextButton(onClick = onRetryImage) {
                            Text("Bild erneut suchen")
                        }
                    }
                }
            }
        }

        InfoCard {
            Labeled(
                label = "EN",
                value = word.english,
                onCopyTextRequested = onCopyTextRequested,
                onTranslateTextRequested = onTranslateTextRequested
            )
            Labeled(
                label = "DE",
                value = word.germanMeaning,
                onCopyTextRequested = onCopyTextRequested,
                onTranslateTextRequested = onTranslateTextRequested
            )
            if (word.exampleDe.isNotBlank()) {
                LongPressMenuHost(
                    textValue = word.exampleDe,
                    onCopyTextRequested = onCopyTextRequested,
                    onTranslateTextRequested = onTranslateTextRequested
                ) { pressModifier ->
                    Text(
                        "„${word.exampleDe}“",
                        modifier = pressModifier,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            verbLine(word)?.let {
                Labeled("Formen", it, onCopyTextRequested, onTranslateTextRequested)
            }
            adjLine(word)?.let {
                Labeled("Steigerung", it, onCopyTextRequested, onTranslateTextRequested)
            }
            grammarLine(word)?.let {
                Labeled("Grammatik", it, onCopyTextRequested, onTranslateTextRequested)
            }
            if (word.memoryTrick.isNotBlank()) {
                Labeled("Tipp", word.memoryTrick, onCopyTextRequested, onTranslateTextRequested)
            }
            if (word.memoryTips.isNotBlank()) {
                Labeled("Merkhilfe", word.memoryTips, onCopyTextRequested, onTranslateTextRequested)
            }
            if (comment.isNotBlank()) {
                Labeled("Kommentar", comment, onCopyTextRequested, onTranslateTextRequested)
            }
        }

        if (word.synonymList.isNotEmpty() || word.antonymList.isNotEmpty()) {
            InfoCard {
                RelationRow("Syn.", word.synonymList, onRelationClick)
                RelationRow("Ant.", word.antonymList, onRelationClick)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderCard(
    word: VocabularyEntity,
    onSpeak: () -> Unit,
    onCopyTextRequested: ((String) -> Unit)?,
    onTranslateTextRequested: ((String) -> Unit)?,
    onCommentRequested: (() -> Unit)?
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                LongPressMenuHost(
                    textValue = word.displayWord,
                    onCopyTextRequested = onCopyTextRequested,
                    onTranslateTextRequested = onTranslateTextRequested
                ) { pressModifier ->
                    Text(
                        buildAnnotatedString {
                            append(word.displayWord)
                            if (word.plural.isNotBlank()) {
                                withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
                                    append("  (Pl. ${word.plural})")
                                }
                            }
                        },
                        modifier = pressModifier,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Text(
                    listOfNotNull(
                        word.level,
                        word.chapter,
                        word.posTinyLabel.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (onCommentRequested != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        SuggestionChip(onClick = onCommentRequested, label = { Text("Kommentar") })
                    }
                }
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
    Labeled(
        label = label,
        value = value,
        onCopyTextRequested = null,
        onTranslateTextRequested = null
    )
}

@Composable
internal fun Labeled(
    label: String,
    value: String,
    onCopyTextRequested: ((String) -> Unit)? = null,
    onTranslateTextRequested: ((String) -> Unit)? = null
) {
    if (value.isBlank()) return
    LongPressMenuHost(
        textValue = value,
        onCopyTextRequested = onCopyTextRequested,
        onTranslateTextRequested = onTranslateTextRequested
    ) { pressModifier ->
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
            modifier = pressModifier,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LongPressMenuHost(
    textValue: String,
    onCopyTextRequested: ((String) -> Unit)?,
    onTranslateTextRequested: ((String) -> Unit)?,
    content: @Composable (Modifier) -> Unit
) {
    if (onCopyTextRequested == null && onTranslateTextRequested == null) {
        content(Modifier)
        return
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        content(
            Modifier.combinedClickable(
                onClick = {},
                onLongClick = { menuExpanded = true }
            )
        )
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            offset = DpOffset(x = 0.dp, y = (-40).dp)
        ) {
            if (onCopyTextRequested != null) {
                DropdownMenuItem(
                    text = { Text("Kopieren") },
                    onClick = {
                        menuExpanded = false
                        onCopyTextRequested(textValue)
                    }
                )
            }
            if (onTranslateTextRequested != null) {
                DropdownMenuItem(
                    text = { Text("Ubersetzen") },
                    onClick = {
                        menuExpanded = false
                        onTranslateTextRequested(textValue)
                    }
                )
            }
        }
    }
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

