package com.studio71.germanlinia2_b2.ui.archive

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.data.local.ArchivedWordEntity
import com.studio71.germanlinia2_b2.data.local.ArchivedWordItem
import com.studio71.germanlinia2_b2.ui.theme.highlightGermanArticles
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    onBack: () -> Unit
) {
    val words by viewModel.words.collectAsStateWithLifecycle()
    var showRestoreAllDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archiv") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                "Zurückgesetzte und abgeschlossene Wörter. Beim Wiederaufnehmen startet der " +
                    "Plan neu, die erste Wiederholung ist morgen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (words.isEmpty()) {
                Text(
                    "Noch keine archivierten Wörter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                FilledTonalButton(
                    onClick = { showRestoreAllDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text("Alle ${words.size} Wörter wieder aufnehmen")
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(words, key = { it.wordId }) { item ->
                        ArchivedWordRow(
                            item = item,
                            onRestore = { viewModel.restore(item.wordId) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showRestoreAllDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreAllDialog = false },
            title = { Text("Alle ${words.size} Wörter wieder aufnehmen?") },
            text = {
                Text(
                    "Der Wiederholungsplan startet für diese Wörter neu. " +
                        "Bei vielen Wörtern ist der Plan danach wieder gut gefüllt."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreAllDialog = false
                    viewModel.restoreAll()
                }) {
                    Text("Aufnehmen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreAllDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun ArchivedWordRow(
    item: ArchivedWordItem,
    onRestore: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(item.displayWord.highlightGermanArticles(), style = MaterialTheme.typography.bodyLarge)
            if (item.english.isNotBlank()) {
                Text(
                    item.english,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${item.reasonLabel()} · ${formatArchiveDate(item.archivedAtEpochMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = onRestore) {
            Text("Aufnehmen")
        }
    }
}

private fun ArchivedWordItem.reasonLabel(): String =
    if (reason == ArchivedWordEntity.REASON_MASTERED) "Abgeschlossen" else "Zurückgesetzt"

private fun formatArchiveDate(epochMs: Long): String =
    DateTimeFormatter.ofPattern("dd.MM.yyyy")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))
