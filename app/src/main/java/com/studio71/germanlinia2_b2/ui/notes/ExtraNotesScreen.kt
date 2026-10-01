package com.studio71.germanlinia2_b2.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.data.local.ExtraNoteEntity
import com.studio71.germanlinia2_b2.ui.tts.rememberGermanSpeaker
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraNotesScreen(
    viewModel: ExtraNotesViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val speaker = rememberGermanSpeaker()
    val clipboardManager = LocalClipboardManager.current

    var nowSpeakingText by remember { mutableStateOf<String?>(null) }
    var quickText by rememberSaveable { mutableStateOf("") }

    // Dialog state for create/edit
    var showEditDialog by remember { mutableStateOf(false) }
    var editingNoteId by remember { mutableStateOf(0) }
    var editingNoteTitle by remember { mutableStateOf("") }
    var editingNoteContent by remember { mutableStateOf("") }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extra Notizen & TTS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingNoteId = 0
                    editingNoteTitle = ""
                    editingNoteContent = ""
                    showEditDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Notiz hinzufügen")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // --- Quick TTS Section ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Schnelles Vorlesen (TTS)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Füge hier einen Text ein, um ihn sofort auf Deutsch anzuhören.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = quickText,
                        onValueChange = { quickText = it },
                        placeholder = { Text("Hier Text einfügen oder schreiben...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        maxLines = 5
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (quickText.isNotBlank()) {
                                    nowSpeakingText = quickText
                                    speaker.speak(quickText)
                                }
                            },
                            enabled = quickText.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Anhören",
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Anhören")
                        }

                        if (nowSpeakingText != null) {
                            OutlinedButton(
                                onClick = {
                                    speaker.stop()
                                    nowSpeakingText = null
                                }
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stopp")
                                Text("Stop")
                            }
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.getText()?.let {
                                    quickText = it.text
                                }
                            }
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Einfügen")
                        }
                    }

                    if (quickText.isNotBlank()) {
                        FilledTonalButton(
                            onClick = {
                                editingNoteId = 0
                                editingNoteTitle = "Schnelle Notiz"
                                editingNoteContent = quickText
                                showEditDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Als Notiz speichern",
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Als Notiz speichern")
                        }
                    }
                }
            }

            // --- Saved Notes List Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gespeicherte Notizen",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${uiState.notes.size} Notiz(en)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Keine Notizen vorhanden. Erstelle eine neue mit dem + Button.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                uiState.notes.forEach { note ->
                    NoteItemCard(
                        note = note,
                        dateText = dateFormatter.format(Date(note.updatedAtEpochMs)),
                        onSpeak = {
                            nowSpeakingText = note.content
                            speaker.speak(note.content)
                        },
                        onStop = {
                            speaker.stop()
                            if (nowSpeakingText == note.content) {
                                nowSpeakingText = null
                            }
                        },
                        isSpeaking = nowSpeakingText == note.content,
                        onEdit = {
                            editingNoteId = note.id
                            editingNoteTitle = note.title
                            editingNoteContent = note.content
                            showEditDialog = true
                        },
                        onDelete = {
                            viewModel.deleteNote(note.id)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
        }
    }

    // --- Create / Edit Note Dialog ---
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(if (editingNoteId > 0) "Notiz bearbeiten" else "Neue Notiz")
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = editingNoteTitle,
                        onValueChange = { editingNoteTitle = it },
                        label = { Text("Titel") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editingNoteContent,
                        onValueChange = { editingNoteContent = it },
                        label = { Text("Inhalt / Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = editingNoteTitle.trim().ifEmpty { "Unbenannt" }
                        viewModel.saveNote(editingNoteId, title, editingNoteContent)
                        showEditDialog = false
                    },
                    enabled = editingNoteContent.isNotBlank()
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun NoteItemCard(
    note: ExtraNoteEntity,
    dateText: String,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    isSpeaking: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isSpeaking) onStop() else onSpeak()
                        }
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Vorlesen / Stoppen",
                            tint = if (isSpeaking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Bearbeiten",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Löschen",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!expanded && note.content.length > 120) {
                Text(
                    text = "Mehr anzeigen...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

