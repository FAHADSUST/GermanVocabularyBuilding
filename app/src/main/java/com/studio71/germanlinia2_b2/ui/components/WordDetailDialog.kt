package com.studio71.germanlinia2_b2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity

/**
 * Quick popup details for a word reached from a synonym/antonym chip.
 * Shows a condensed view; the user can also speak it.
 */
@Composable
fun WordDetailDialog(
    word: VocabularyEntity,
    onDismiss: () -> Unit,
    onSpeak: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        },
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(word.displayWord, style = MaterialTheme.typography.titleLarge)
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
                LabeledLine("EN", word.english)
                LabeledLine("DE", word.germanMeaning)
                if (word.exampleDe.isNotBlank()) {
                    Text("„${word.exampleDe}“", style = MaterialTheme.typography.bodyMedium)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text(word.level) })
                    if (word.pos.isNotBlank()) AssistChip(onClick = {}, label = { Text(word.pos) })
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

