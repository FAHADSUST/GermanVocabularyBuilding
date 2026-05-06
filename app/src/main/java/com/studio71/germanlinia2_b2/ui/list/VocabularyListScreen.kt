package com.studio71.germanlinia2_b2.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.data.local.VocabularyEntity
import com.studio71.germanlinia2_b2.data.local.WordMarker
import com.studio71.germanlinia2_b2.data.repo.SortMode
import com.studio71.germanlinia2_b2.ui.card.CardDeck
import com.studio71.germanlinia2_b2.ui.components.FilterDropdown
import com.studio71.germanlinia2_b2.ui.components.MarkerFilterChip
import com.studio71.germanlinia2_b2.ui.components.MarkerStar
import com.studio71.germanlinia2_b2.ui.tts.rememberGermanSpeaker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyListScreen(
    viewModel: VocabularyListViewModel,
    onOpenCard: (String) -> Unit,
    onOpenStats: () -> Unit,
    onStartReview: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val words by viewModel.words.collectAsStateWithLifecycle()
    val options by viewModel.options.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()
    val dueCount by viewModel.dueCount.collectAsStateWithLifecycle()
    val learnedCount by viewModel.learnedCount.collectAsStateWithLifecycle()
    val marks by viewModel.marks.collectAsStateWithLifecycle()
    val speaker = rememberGermanSpeaker()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wortschatz · A2–B2") },
                actions = {
                    IconButton(onClick = onStartReview, enabled = dueCount > 0) {
                        BadgedBox(
                            badge = {
                                if (dueCount > 0) Badge { Text("$dueCount") }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Wiederholen")
                        }
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistik")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Gelernt: $learnedCount   ·   Heute fällig: $dueCount",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onStartReview,
                    enabled = dueCount > 0,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(18.dp))
                    Text(" Wiederholen ($dueCount)")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Suchen (Wort, EN, DE) …") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Löschen")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Filter chips
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortChip(sortMode, viewModel::setSortMode)
                MarkerFilterChip(filter.marker) {
                    viewModel.setFilter(filter.copy(marker = it))
                }
                FilterDropdown("Niveau", options.levels, filter.level) {
                    viewModel.setFilter(filter.copy(level = it))
                }
                FilterDropdown("Buch", options.books, filter.book) {
                    viewModel.setFilter(filter.copy(book = it))
                }
                FilterDropdown("Kapitel", options.chapters, filter.chapter) {
                    viewModel.setFilter(filter.copy(chapter = it))
                }
                FilterDropdown("Wortart", options.partsOfSpeech, filter.pos) {
                    viewModel.setFilter(filter.copy(pos = it))
                }
                FilterDropdown("Grammatik", options.grammarGroups, filter.grammarGroup) {
                    viewModel.setFilter(filter.copy(grammarGroup = it))
                }
            }

            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(words, key = { it.id }) { word ->
                    WordListItem(
                        word = word,
                        marker = marks[word.id] ?: WordMarker.NONE,
                        onClick = {
                            CardDeck.setDeck(words.map { it.id })
                            onOpenCard(word.id)
                        },
                        onSpeak = { speaker.speak(word.word) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortChip(current: SortMode, onSelect: (SortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.material3.AssistChip(
        onClick = { expanded = true },
        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null) },
        label = { Text(current.label) }
    )
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        SortMode.entries.forEach { mode ->
            DropdownMenuItem(
                text = { Text(mode.label) },
                onClick = { onSelect(mode); expanded = false }
            )
        }
    }
}

@Composable
private fun WordListItem(
    word: VocabularyEntity,
    marker: WordMarker,
    onClick: () -> Unit,
    onSpeak: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(word.displayWord, style = MaterialTheme.typography.titleMedium)
                    if (marker != WordMarker.NONE) {
                        MarkerStar(marker, modifier = Modifier.padding(start = 6.dp))
                    }
                }
                Text(
                    word.english,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (word.germanMeaning.isNotBlank()) {
                    Text(word.germanMeaning, style = MaterialTheme.typography.bodySmall)
                }
                if (word.exampleDe.isNotBlank()) {
                    Text(
                        "„${word.exampleDe}“",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onSpeak) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Vorlesen")
            }
        }
    }
}

