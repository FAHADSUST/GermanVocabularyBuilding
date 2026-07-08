package com.studio71.germanlinia2_b2.ui.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.studio71.germanlinia2_b2.ui.components.label
import com.studio71.germanlinia2_b2.ui.tts.TtsController
import com.studio71.germanlinia2_b2.ui.tts.TtsPlaybackState
import com.studio71.germanlinia2_b2.ui.tts.TtsSessionSeed
import com.studio71.germanlinia2_b2.ui.tts.rememberGermanSpeaker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VocabularyListScreen(
    viewModel: VocabularyListViewModel,
    onOpenCard: (String) -> Unit,
    onOpenStats: () -> Unit,
    onStartReview: () -> Unit,
    onStartGame: () -> Unit,
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
    val context = LocalContext.current

    val playbackState by TtsController.playbackState.collectAsStateWithLifecycle()
    val playingWordId by TtsController.currentWordId.collectAsStateWithLifecycle()
    var showJumpDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    val sessionSeed = remember(filter, query, sortMode) {
        TtsSessionSeed(filter = filter, query = query, sortMode = sortMode)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wortschatz · A2–B2") },
                actions = {
                    IconButton(
                        onClick = { TtsController.start(context, words, 0, sessionSeed) },
                        enabled = words.isNotEmpty()
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "Liste vorlesen")
                    }
                    IconButton(onClick = onStartReview, enabled = dueCount > 0) {
                        BadgedBox(
                            badge = {
                                if (dueCount > 0) Badge { Text("$dueCount") }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Wiederholen")
                        }
                    }
                    IconButton(onClick = onStartGame) {
                        Icon(Icons.Default.SportsEsports, contentDescription = "Lernspiel")
                    }
                    IconButton(onClick = onOpenStats) {
                        Icon(Icons.Default.BarChart, contentDescription = "Statistik")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        },
        bottomBar = {
            if (playbackState != TtsPlaybackState.IDLE) {
                PlaybackBar(
                    state = playbackState,
                    currentWord = words.firstOrNull { it.id == playingWordId },
                    onPrevious = { TtsController.previous(context) },
                    onToggle = { TtsController.togglePlayPause(context) },
                    onNext = { TtsController.next(context) },
                    onJump = { showJumpDialog = true },
                    onStop = { TtsController.stop(context) },
                    onWordTitleDoubleTap = if (playbackState == TtsPlaybackState.PLAYING && playingWordId != null) {
                        {
                            val index = words.indexOfFirst { it.id == playingWordId }
                            if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
                        }
                    } else {
                        null
                    }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Gelernt: $learnedCount   ·   Heute fällig: $dueCount",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = onStartReview,
                    enabled = dueCount > 0,
                    contentPadding = PaddingValues(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, Modifier.size(14.dp))
                    Text(" Wiederholen ($dueCount)", style = MaterialTheme.typography.labelSmall,)
                }
                /*IconButton(onClick = onStartGame) {
                    Icon(Icons.Default.SportsEsports, contentDescription = "Lernspiel")
                }*/
            }

            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(47.dp),
                placeholder = { Text("Suchen (Wort, EN, DE) …",
                        style = MaterialTheme.typography.labelSmall) },
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
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(words, key = { _, item -> item.id }) { index, word ->
                    WordListItem(
                        itemNumber = index + 1,
                        word = word,
                        marker = marks[word.id] ?: WordMarker.NONE,
                        isPlaying = word.id == playingWordId,
                        onClick = {
                            CardDeck.setDeck(words.map { it.id })
                            onOpenCard(word.id)
                        },
                        onLongClick = { TtsController.start(context, words, index, sessionSeed) },
                        onSpeak = { speaker.speak(word.word) },
                        onMarkerChange = { viewModel.setMarker(word.id, it) }
                    )
                }
            }
        }
    }

    if (showJumpDialog) {
        JumpToWordDialog(
            words = words,
            currentWordId = playingWordId,
            onJump = { index ->
                if (playbackState == TtsPlaybackState.IDLE) {
                    TtsController.start(context, words, index, sessionSeed)
                } else {
                    TtsController.jumpTo(context, index)
                }
                showJumpDialog = false
            },
            onDismiss = { showJumpDialog = false }
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun PlaybackBar(
    state: TtsPlaybackState,
    currentWord: VocabularyEntity?,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onJump: () -> Unit,
    onStop: () -> Unit,
    onWordTitleDoubleTap: (() -> Unit)?
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    currentWord?.displayWord ?: "Wiedergabe",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    modifier = Modifier.combinedClickable(
                        enabled = onWordTitleDoubleTap != null,
                        onClick = {},
                        onDoubleClick = { onWordTitleDoubleTap?.invoke() }
                    )
                )
                Text(
                    if (state == TtsPlaybackState.PAUSED) "Pausiert"
                    else currentWord?.english.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Zurück")
            }
            FilledIconButton(onClick = onToggle) {
                if (state == TtsPlaybackState.PLAYING) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause")
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Abspielen")
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Weiter")
            }
            IconButton(onClick = onJump) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Zu Wort springen")
            }
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Stop, contentDescription = "Stopp")
            }
        }
    }
}

@Composable
private fun JumpToWordDialog(
    words: List<VocabularyEntity>,
    currentWordId: String?,
    onJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val matches = remember(query, words) {
        val indexed = words.mapIndexed { index, word -> index to word }
        if (query.isBlank()) indexed
        else indexed.filter { (_, word) ->
            word.displayWord.contains(query, ignoreCase = true) ||
                word.english.contains(query, ignoreCase = true) ||
                word.germanMeaning.contains(query, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        },
        title = { Text("Zu Wort springen") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Suchen …") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                Spacer(Modifier.height(8.dp))
                if (matches.isEmpty()) {
                    Text(
                        "Keine Treffer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        items(matches, key = { it.first }) { (index, word) ->
                            val selected = word.id == currentWordId
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onJump(index) }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(40.dp)
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        word.displayWord,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (word.english.isNotBlank()) {
                                        Text(
                                            word.english,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                if (selected) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WordListItem(
    itemNumber: Int,
    word: VocabularyEntity,
    marker: WordMarker,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSpeak: () -> Unit,
    onMarkerChange: (WordMarker) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = if (isPlaying) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$itemNumber.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    if (isPlaying) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp).padding(end = 4.dp)
                        )
                    }
                    Text(word.displayWord, style = MaterialTheme.typography.titleMedium)
                    if (word.posTinyLabel.isNotBlank()) {
                        Text(
                            "  ${word.posTinyLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    MarkerMenuButton(
                        current = marker,
                        onSelect = onMarkerChange,
                        modifier = Modifier.padding(start = 6.dp)
                    )
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

@Composable
private fun MarkerMenuButton(
    current: WordMarker,
    onSelect: (WordMarker) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        if (current == WordMarker.NONE) {
            Icon(
                Icons.Outlined.StarOutline,
                contentDescription = "Markierung auswählen",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp).clickable { expanded = true }
            )
        } else {
            MarkerStar(
                marker = current,
                size = 16.dp,
                modifier = Modifier.size(16.dp).clickable { expanded = true }
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        Icons.Outlined.StarOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                text = { Text(WordMarker.NONE.label) },
                onClick = {
                    onSelect(WordMarker.NONE)
                    expanded = false
                }
            )
            listOf(WordMarker.HARD, WordMarker.MEDIUM, WordMarker.EASY).forEach { marker ->
                DropdownMenuItem(
                    leadingIcon = { MarkerStar(marker = marker, size = 18.dp) },
                    text = { Text(marker.label) },
                    onClick = {
                        onSelect(marker)
                        expanded = false
                    }
                )
            }
        }
    }
}

