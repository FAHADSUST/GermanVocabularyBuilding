package com.studio71.germanlinia2_b2.ui.review

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.repo.WordImageState
import com.studio71.germanlinia2_b2.ui.card.WordDetail
import com.studio71.germanlinia2_b2.ui.components.WordDetailDialog
import com.studio71.germanlinia2_b2.ui.tts.rememberGermanSpeaker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    autoSpeakOnReveal: Boolean,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val imageState by viewModel.imageState.collectAsStateWithLifecycle()
    val speaker = rememberGermanSpeaker()
    val scope = rememberCoroutineScope()
    var detailWord by remember { mutableStateOf<VocabularyEntity?>(null) }
    // Active recall: hide the answer until revealed. Resets whenever the card advances.
    var revealed by remember(state.index) { mutableStateOf(false) }

    // Optionally pronounce the word as soon as it is revealed.
    LaunchedEffect(state.index, revealed) {
        if (revealed && autoSpeakOnReveal) {
            state.word?.let { speaker.speak(it.word) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wiederholung") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        },
        bottomBar = {
            val word = state.word
            if (!state.finished && word != null) {
                if (!revealed) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Button(
                            onClick = { revealed = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Visibility, contentDescription = null, Modifier.size(18.dp))
                            Text(" Antwort zeigen")
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.grade(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, Modifier.size(18.dp))
                            Text(" Nochmal")
                        }
                        Button(
                            onClick = { viewModel.grade(true) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, Modifier.size(18.dp))
                            Text(" Gewusst")
                        }
                    }
                }
            }
        }
    ) { padding ->
        when {
            state.loading -> {
                Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                    Text("Lädt …", Modifier.padding(16.dp))
                }
            }

            state.finished -> SummaryContent(
                modifier = Modifier.fillMaxSize().padding(padding),
                knownCount = state.knownCount,
                againCount = state.againCount,
                total = state.total,
                onDone = onBack
            )

            else -> {
                val word = state.word!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Progress through the session.
                    Text(
                        "${state.index + 1} / ${state.total}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LinearProgressIndicator(
                        progress = { (state.index).toFloat() / state.total.coerceAtLeast(1) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    )
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        WordDetail(
                            word = word,
                            onSpeak = { speaker.speak(it) },
                            onRelationClick = { value ->
                                scope.launch { detailWord = viewModel.lookup(value) ?: detailWord }
                            },
                            modifier = Modifier.clickable(enabled = !revealed) { revealed = true },
                            imageState = imageState,
                            onRetryImage = viewModel::retryImage,
                            revealed = revealed
                        )
                    }
                }
            }
        }
    }

    detailWord?.let { dw ->
        val detailImageState by remember(dw.id) {
            viewModel.observeWordImage(dw.id)
        }.collectAsStateWithLifecycle(initialValue = WordImageState())

        LaunchedEffect(dw.id) {
            viewModel.ensureWordImage(dw.id)
        }

        WordDetailDialog(
            word = dw,
            onDismiss = { detailWord = null },
            onSpeak = { speaker.speak(it) },
            imageState = detailImageState,
            onRetryImage = { viewModel.retryWordImage(dw.id) }
        )
    }
}

@Composable
private fun SummaryContent(
    modifier: Modifier,
    knownCount: Int,
    againCount: Int,
    total: Int,
    onDone: () -> Unit
) {
    Column(
        modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Done,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            if (total == 0) "Nichts fällig 🎉" else "Wiederholung fertig! 🎉",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (total > 0) {
            ElevatedCard(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryRow("Wiederholt", (knownCount + againCount).toString())
                    SummaryRow("Gewusst", knownCount.toString())
                    SummaryRow("Nochmal", againCount.toString())
                }
            }
        } else {
            Text(
                "Du bist auf dem neuesten Stand. Lerne neue Wörter aus der Liste, " +
                    "dann erscheinen sie nach dem Wiederholungsplan (1·2·3·7·15·30 Tage).",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Button(onClick = onDone, modifier = Modifier.padding(top = 24.dp)) {
            Text("Zur Liste")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}

