package com.studio71.germanlinia2_b2.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fortschritt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    value = state.totalLearned.toString(),
                    label = "Wörter gelernt"
                )
                StreakCard(modifier = Modifier.weight(1f), streak = state.currentStreak)
            }

            // Range selector
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { viewModel.setRange(range) },
                        label = { Text(range.label) }
                    )
                }
            }

            // Bar chart
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Gelernt + Wiederholt pro Tag", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    BarChart(
                        bars = state.bars,
                        learnedColor = primary,
                        reviewedColor = secondary,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(primary); Text("  Gelernt    ", style = MaterialTheme.typography.labelMedium)
                        LegendDot(secondary); Text("  Wiederholt", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(modifier: Modifier, value: String, label: String) {
    ElevatedCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun StreakCard(modifier: Modifier, streak: Int) {
    ElevatedCard(modifier) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Column(Modifier.padding(start = 8.dp)) {
                Text("$streak Tage", style = MaterialTheme.typography.headlineSmall)
                Text("Serie (Streak)", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color) {
    Canvas(Modifier.height(12.dp).fillMaxWidth(0.03f)) {
        drawCircle(color)
    }
}

@Composable
private fun BarChart(
    bars: List<StatBar>,
    learnedColor: Color,
    reviewedColor: Color,
    modifier: Modifier = Modifier
) {
    val maxValue = (bars.maxOfOrNull { it.learned + it.reviewed } ?: 0).coerceAtLeast(1)
    Canvas(modifier) {
        if (bars.isEmpty()) return@Canvas
        val gap = size.width * 0.01f
        val slot = size.width / bars.size
        val barWidth = (slot - gap).coerceAtLeast(1f)
        bars.forEachIndexed { i, bar ->
            val x = i * slot + gap / 2
            val learnedH = size.height * (bar.learned.toFloat() / maxValue)
            val reviewedH = size.height * (bar.reviewed.toFloat() / maxValue)
            // learned (bottom)
            drawRect(
                color = learnedColor,
                topLeft = Offset(x, size.height - learnedH),
                size = Size(barWidth, learnedH)
            )
            // reviewed (stacked on top)
            drawRect(
                color = reviewedColor,
                topLeft = Offset(x, size.height - learnedH - reviewedH),
                size = Size(barWidth, reviewedH)
            )
        }
    }
}

