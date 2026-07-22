package com.studio71.germanlinia2_b2.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.domain.game.GameMode
import com.studio71.germanlinia2_b2.ui.theme.getAnnotatedDisplayWord
import com.studio71.germanlinia2_b2.ui.theme.highlightGermanArticles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zuruck")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Ladt...", Modifier.padding(horizontal = 16.dp))
            }
            return@Scaffold
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            SummaryBar(known = state.knownCount, again = state.againCount)
            if (state.message.isNotBlank()) {
                Text(
                    state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            when (state.mode) {
                GameMode.MATCHING -> MatchingContent(viewModel)
                GameMode.CLOZE -> ClozeContent(viewModel)
                GameMode.RECENT -> RecentContent(viewModel)
                GameMode.REVERSE_RECALL -> ReverseRecallContent(viewModel)
            }

            if (state.finished) {
                Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text(
                        "Fertig! Gewusst ${state.knownCount}, Nochmal ${state.againCount}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryBar(known: Int, again: Int) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Gewusst: $known", color = MaterialTheme.colorScheme.primary)
        Text("Nochmal: $again", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun MatchingContent(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress = state.matchedLeftToRight.size.toFloat() / state.batch.size.coerceAtLeast(1)

    Column(Modifier.fillMaxSize()) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Row(
            Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.matchingLeftColumn, key = { it.id }) { word ->
                    val selected = state.selectedLeftId == word.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectMatchingLeft(word.id) }
                    ) {
                        Text(
                            word.getAnnotatedDisplayWord(),
                            modifier = Modifier.padding(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.matchingRightColumn, key = { it.id }) { word ->
                    val selected = state.selectedRightId == word.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectMatchingRight(word.id) }
                    ) {
                        Text(
                            word.english,
                            modifier = Modifier.padding(10.dp),
                            color = if (selected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ClozeContent(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = viewModel.currentClozeWord() ?: return
    val progress = state.clozeIndex.toFloat() / state.batch.size.coerceAtLeast(1)

    val maskedExample = remember(current.id, current.exampleDe) {
        maskExampleSentence(current)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text("${state.clozeIndex + 1} / ${state.batch.size}")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(maskedExample, style = MaterialTheme.typography.titleMedium)
                if (current.english.isNotBlank()) {
                    Text(current.english, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = state.clozeInput,
                    onValueChange = viewModel::onClozeInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fehlendes Wort") },
                    singleLine = true
                )
                Button(onClick = viewModel::submitCloze, modifier = Modifier.fillMaxWidth()) {
                    Text("Prufen")
                }
            }
        }

        if (state.clozeShowChoices) {
            Text("Hinweis: Mehrfachauswahl")
            state.clozeChoiceWords.forEach { option ->
                OutlinedButton(
                    onClick = { viewModel.chooseClozeOption(option.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(option.getAnnotatedDisplayWord())
                }
            }
        }
    }
}

@Composable
private fun ReverseRecallContent(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = viewModel.currentReverseWord() ?: return
    val progress = state.reverseIndex.toFloat() / state.batch.size.coerceAtLeast(1)

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text("${state.reverseIndex + 1} / ${state.batch.size}")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(current.english, style = MaterialTheme.typography.titleMedium)
                if (state.reverseRevealed) {
                    Text(current.getAnnotatedDisplayWord(), color = MaterialTheme.colorScheme.primary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { viewModel.gradeReverse(false) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Close, contentDescription = null)
                            Text(" Nochmal")
                        }
                        Button(onClick = { viewModel.gradeReverse(true) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Text(" Gewusst")
                        }
                    }
                } else {
                    Button(onClick = viewModel::revealReverse, modifier = Modifier.fillMaxWidth()) {
                        Text("Losung zeigen")
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentContent(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rows = remember(state.recentRows, state.recentFilter) { viewModel.filteredRecentRows() }

    Column(Modifier.fillMaxSize()) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { viewModel.setRecentFilter(RecentFilter.ALL) },
                label = { Text("All") }
            )
            AssistChip(
                onClick = { viewModel.setRecentFilter(RecentFilter.UNIQUE) },
                label = { Text("Unique") }
            )
            AssistChip(
                onClick = { viewModel.setRecentFilter(RecentFilter.EVENTS) },
                label = { Text("Events") }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(rows, key = { it.key }) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val label = if (row.source == RecentSource.UNIQUE) "Unique" else "Event"
                        val rowText = buildAnnotatedString {
                            append("[$label] ")
                            append(row.displayWord.highlightGermanArticles())
                        }
                        Text(rowText, style = MaterialTheme.typography.titleSmall)
                        if (row.english.isNotBlank()) {
                            Text(row.english, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.gradeRecent(row.key, false) },
                                enabled = !row.reviewed,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Nochmal")
                            }
                            Button(
                                onClick = { viewModel.gradeRecent(row.key, true) },
                                enabled = !row.reviewed,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (row.reviewed) "Erledigt" else "Gewusst")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun maskExampleSentence(word: com.studio71.germanlinia2_b2.data.local.VocabularyEntity): String {
    val sentence = word.exampleDe.ifBlank { return "____" }
    val target = word.word.trim()
    if (target.isBlank()) return sentence
    val regex = Regex("\\b${Regex.escape(target)}\\b", setOf(RegexOption.IGNORE_CASE))
    return if (regex.containsMatchIn(sentence)) {
        sentence.replaceFirst(regex, "____")
    } else {
        sentence
    }
}


