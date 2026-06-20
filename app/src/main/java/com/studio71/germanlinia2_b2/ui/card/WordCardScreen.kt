package com.studio71.germanlinia2_b2.ui.card

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.data.local.ProgressEntity
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.ui.components.MarkerStarsRow
import com.studio71.germanlinia2_b2.ui.components.WordDetailDialog
import com.studio71.germanlinia2_b2.ui.tts.rememberGermanSpeaker
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordCardScreen(
    viewModel: WordCardViewModel,
    autoAddSeenToReview: Boolean,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val marker by viewModel.marker.collectAsStateWithLifecycle()
    val comment by viewModel.comment.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val speaker = rememberGermanSpeaker()
    val scope = rememberCoroutineScope()
    var detailWord by remember { mutableStateOf<VocabularyEntity?>(null) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var commentDraft by remember { mutableStateOf("") }

    val word = state.word

    LaunchedEffect(word?.id, autoAddSeenToReview) {
        if (word != null) {
            viewModel.onWordSeen(autoAddSeenToReview)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.total > 0) "${state.index + 1} / ${state.total}" else "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück zur Liste")
                    }
                },
                actions = {
                    MarkerStarsRow(current = marker, onSelect = viewModel::setMarker)
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
            WordDetail(
                word = word,
                onSpeak = { speaker.speak(it) },
                onRelationClick = { value ->
                    scope.launch { detailWord = viewModel.lookup(value) ?: detailWord }
                },
                onCopyTextRequested = { text ->
                    copyTextToClipboard(context, word.displayWord, text)
                },
                onTranslateTextRequested = { text ->
                    openTranslateIntent(context, text)
                },
                onCommentRequested = {
                    commentDraft = comment
                    showCommentDialog = true
                },
                comment = comment
            )
            ReviewCard(progress, viewModel)
        }
    }

    if (showCommentDialog && word != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCommentDialog = false },
            title = { Text("Kommentar") },
            text = {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dein Kommentar") },
                    minLines = 3
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setComment(commentDraft)
                        showCommentDialog = false
                    }
                ) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
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

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "Text kopiert", Toast.LENGTH_SHORT).show()
}

private fun openTranslateIntent(context: Context, text: String) {
    val processIntent = Intent(Intent.ACTION_PROCESS_TEXT).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_PROCESS_TEXT, text)
        putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    val launchIntent = if (processIntent.resolveActivity(context.packageManager) != null) {
        processIntent
    } else {
        Intent.createChooser(shareIntent, "Ubersetzen mit")
    }

    if (context !is Activity) launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(launchIntent) }
        .onFailure {
            Toast.makeText(context, "Keine App fur Ubersetzung gefunden", Toast.LENGTH_SHORT).show()
        }
}

