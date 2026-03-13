package com.studio71.germanlinia2_b2.ui.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontStyle
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
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { viewModel.previous() },
                    enabled = state.hasPrevious
                ) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderCard(word, onSpeak = { speaker.speak(word.word) })

            Section("Bedeutung") {
                Labeled("EN", word.english)
                Labeled("DE", word.germanMeaning)
            }

            if (word.exampleDe.isNotBlank()) {
                Section("Beispiel") {
                    Text("„${word.exampleDe}“", fontStyle = FontStyle.Italic)
                }
            }

            VerbAdjectiveForms(word)

            GrammarSection(word)

            if (word.memoryTrick.isNotBlank()) {
                Section("Merkhilfe") { Text(word.memoryTrick) }
            }

            WordRelations(
                title = "Synonyme",
                words = word.synonymList,
                onClick = { value ->
                    scope.launch { detailWord = viewModel.lookup(value) ?: detailWord }
                }
            )
            WordRelations(
                title = "Antonyme",
                words = word.antonymList,
                onClick = { value ->
                    scope.launch { detailWord = viewModel.lookup(value) ?: detailWord }
                }
            )

            ReviewSection(progress, viewModel)
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
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(word.displayWord, style = MaterialTheme.typography.headlineSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (word.plural.isNotBlank()) {
                        Text("Pl.: ${word.plural}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(
                    "${word.level} · ${word.book} · ${word.chapter}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSpeak) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Vorlesen")
            }
        }
    }
}

@Composable
private fun VerbAdjectiveForms(word: VocabularyEntity) {
    if (word.isVerb && (word.verbPresent3rd.isNotBlank() || word.verbPast.isNotBlank() || word.verbPerfect.isNotBlank())) {
        Section("Verbformen") {
            Labeled("3. Person", word.verbPresent3rd)
            Labeled("Präteritum", word.verbPast)
            Labeled("Perfekt", word.verbPerfect)
        }
    }
    if (word.isAdjective && (word.adjComparative.isNotBlank() || word.adjSuperlative.isNotBlank())) {
        Section("Steigerung") {
            Labeled("Komparativ", word.adjComparative)
            Labeled("Superlativ", word.adjSuperlative)
        }
    }
}

@Composable
private fun GrammarSection(word: VocabularyEntity) {
    val hasGrammar = word.grammarGroup.isNotBlank() || word.preposition.isNotBlank() || word.governCase.isNotBlank()
    if (!hasGrammar) return
    Section("Grammatik") {
        if (word.grammarGroup.isNotBlank()) Labeled("Gruppe", word.grammarGroup)
        if (word.preposition.isNotBlank()) Labeled("Präposition", word.preposition)
        if (word.governCase.isNotBlank()) Labeled("Kasus", word.governCase)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordRelations(title: String, words: List<String>, onClick: (String) -> Unit) {
    if (words.isEmpty()) return
    Section(title) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            words.forEach { w ->
                SuggestionChip(onClick = { onClick(w) }, label = { Text(w) })
            }
        }
    }
}

@Composable
private fun ReviewSection(progress: ProgressEntity?, viewModel: WordCardViewModel) {
    Section("Wiederholung") {
        if (progress == null) {
            Text("Noch nicht gelernt.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { viewModel.markLearned() }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Als gelernt markieren")
            }
        } else {
            Labeled("Zuletzt wiederholt", progress.lastReviewed?.let { LocalDate.ofEpochDay(it).toString() } ?: "—")
            Labeled("Nächste Wiederholung", LocalDate.ofEpochDay(progress.nextDue).toString())
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { viewModel.reviewFail() }) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Text("Nochmal")
                }
                Button(onClick = { viewModel.reviewSuccess() }) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Text("Gewusst")
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            HorizontalDivider(Modifier.padding(vertical = 2.dp))
            content()
        }
    }
}

@Composable
private fun Labeled(label: String, value: String) {
    if (value.isBlank()) return
    Row {
        Text("$label: ", style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

