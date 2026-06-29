package com.studio71.germanlinia2_b2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.repo.WordImageState

/**
 * Quick popup details for a word reached from a synonym/antonym chip.
 * Shows a condensed view; the user can also speak it.
 */
@Composable
fun WordDetailDialog(
    word: VocabularyEntity,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit,
    imageState: WordImageState = WordImageState(),
    onRetryImage: (() -> Unit)? = null
) {
    var imageLoadFailed by remember(imageState.imageUrl) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        },
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(word.displayWord, style = MaterialTheme.typography.titleLarge)
                    if (word.posTinyLabel.isNotBlank()) {
                        Text(
                            word.posTinyLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { onSpeak(word.word) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Vorlesen")
                }
            }
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                when {
                    imageState.imageUrl != null && !imageLoadFailed -> {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = imageState.imageUrl,
                                contentDescription = "Bedeutungsbild",
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                contentScale = ContentScale.Crop,
                                onError = { imageLoadFailed = true }
                            )
                        }
                    }

                    imageLoadFailed -> {
                        Text(
                            "Bedeutungsbild konnte nicht dargestellt werden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (onRetryImage != null) {
                            TextButton(onClick = onRetryImage) {
                                Text("Bild erneut suchen")
                            }
                        }
                    }

                    imageState.isLoading -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                            Text(
                                "Bedeutungsbild wird geladen …",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    imageState.message.isNotBlank() -> {
                        Text(
                            imageState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (imageState.canRetry && onRetryImage != null) {
                            TextButton(onClick = onRetryImage) {
                                Text("Bild erneut suchen")
                            }
                        }
                    }
                }

                LabeledLine("EN", word.english)
                LabeledLine("DE", word.germanMeaning)
                if (word.exampleDe.isNotBlank()) {
                    Text("„${word.exampleDe}“", style = MaterialTheme.typography.bodyMedium)
                }
                LabeledLine("Merkhilfe", word.memoryTips)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text(word.level) })
                    if (word.posTinyLabel.isNotBlank()) {
                        AssistChip(onClick = {}, label = { Text(word.posTinyLabel) })
                    }
                }
            }
        }
    )
}

@Composable
private fun LabeledLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.padding(top = 2.dp)) {
        Text(
            "$label: ",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

