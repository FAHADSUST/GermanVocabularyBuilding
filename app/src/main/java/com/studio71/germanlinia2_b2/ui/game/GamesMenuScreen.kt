package com.studio71.germanlinia2_b2.ui.game

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import com.studio71.germanlinia2_b2.data.local.ActiveGameEntity
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamesMenuScreen(
    repository: VocabularyRepository,
    onBack: () -> Unit,
    onStartGame: (mode: String, sessionId: String?) -> Unit
) {
    val activeGames by repository.observeActiveGames().collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lernspiele") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Standard Spielstufen (New Games)
            item {
                Text(
                    text = "Neues Spiel starten",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                GameModeCard(
                    title = "Matching-Spiel",
                    description = "Finde die passenden deutschen und englischen Paare.",
                    onClick = { onStartGame("matching", null) }
                )
            }

            item {
                GameModeCard(
                    title = "Lückentext-Spiel",
                    description = "Vervollständige die Lücke mit dem korrekten Wort.",
                    onClick = { onStartGame("cloze", null) }
                )
            }

            item {
                GameModeCard(
                    title = "Bedeutung erraten",
                    description = "Sieh dir das Wort an und teste dein Gedächtnis.",
                    onClick = { onStartGame("reverse", null) }
                )
            }

            item {
                GameModeCard(
                    title = "Recent 50",
                    description = "Wiederhole deine zuletzt angesehenen Wörter.",
                    onClick = { onStartGame("recent", null) }
                )
            }

            // Section 2: Active / Unfinished Games
            if (activeGames.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Nicht beendete Spiele fortsetzen",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(activeGames, key = { it.id }) { game ->
                    ActiveGameRow(
                        game = game,
                        onResume = { onStartGame(game.mode, game.id) },
                        onDelete = {
                            scope.launch {
                                repository.deleteActiveGame(game.id)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GameModeCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ActiveGameRow(
    game: ActiveGameEntity,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val modeDisplay = when (game.mode) {
        "matching" -> "Matching-Spiel"
        "cloze" -> "Lückentext-Spiel"
        "reverse" -> "Bedeutung erraten"
        else -> "Spiel (${game.mode})"
    }

    val totalWords = remember(game.wordIds) {
        game.wordIds.split(",").filter { it.isNotBlank() }.size
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onResume)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(modeDisplay, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Gestartet am: ${formatTimestamp(game.createdAtEpochMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Fortschritt: ${game.progressIndex} von $totalWords Wörtern" +
                            if (game.knownCount > 0 || game.againCount > 0) "   (Gewusst: ${game.knownCount}, Nochmal: ${game.againCount})" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Spiel löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatTimestamp(epochMs: Long): String {
    return try {
        val d = Instant.ofEpochMilli(epochMs)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        "%02d.%02d.%d %02d:%02d".format(d.dayOfMonth, d.monthValue, d.year, d.hour, d.minute)
    } catch (e: Exception) {
        ""
    }
}

