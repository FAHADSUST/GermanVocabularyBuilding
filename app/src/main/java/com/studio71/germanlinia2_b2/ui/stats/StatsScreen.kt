package com.studio71.germanlinia2_b2.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val outline = MaterialTheme.colorScheme.outline

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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    value = state.totalSeen.toString(),
                    label = "Wörter gesehen"
                )
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
                    Text("Gesehen + Gelernt + Wiederholt pro Tag", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(12.dp))
                    BarChart(
                        bars = state.bars,
                        selectedDate = state.selectedDate,
                        seenColor = tertiary,
                        learnedColor = primary,
                        reviewedColor = secondary,
                        outlineColor = outline,
                        onBarClick = viewModel::selectDate,
                        modifier = Modifier.fillMaxWidth().height(220.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LegendDot(tertiary); Text("  Gesehen    ", style = MaterialTheme.typography.labelMedium)
                        LegendDot(primary); Text("  Gelernt    ", style = MaterialTheme.typography.labelMedium)
                        LegendDot(secondary); Text("  Wiederholt", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Clickable table fallback (same behavior as tapping bars)
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tagesübersicht", style = MaterialTheme.typography.titleSmall)
                    val activeBars = state.bars.filter { (it.seen + it.learned + it.reviewed) > 0 }
                    if (activeBars.isEmpty()) {
                        Text(
                            "Noch keine Aktivität in diesem Zeitraum.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        activeBars.reversed().forEachIndexed { idx, bar ->
                            DayRow(
                                bar = bar,
                                selected = bar.epochDay == state.selectedDate,
                                onClick = { viewModel.selectDate(bar.epochDay) }
                            )
                            if (idx < activeBars.lastIndex) HorizontalDivider()
                        }
                    }
                }
            }

            // Seen-word history for selected day
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Gesehene Wörter am ${formatEpochDay(state.selectedDate)}",
                        style = MaterialTheme.typography.titleSmall
                    )
                    if (state.seenWordsForSelectedDate.isEmpty()) {
                        Text(
                            "Keine geöffneten Wortdetails an diesem Tag.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.seenWordsForSelectedDate.forEachIndexed { idx, item ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(item.displayWord, style = MaterialTheme.typography.bodyLarge)
                                if (item.english.isNotBlank()) {
                                    Text(
                                        item.english,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (idx < state.seenWordsForSelectedDate.lastIndex) HorizontalDivider()
                        }
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
    selectedDate: Long,
    seenColor: Color,
    learnedColor: Color,
    reviewedColor: Color,
    outlineColor: Color,
    onBarClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxValue = (bars.maxOfOrNull { it.seen + it.learned + it.reviewed } ?: 0).coerceAtLeast(1)
    Canvas(
        modifier.pointerInput(bars) {
            detectTapGestures { tapOffset ->
                if (bars.isEmpty()) return@detectTapGestures
                val slot = size.width.toFloat() / bars.size
                val index = (tapOffset.x / slot).toInt().coerceIn(0, bars.lastIndex)
                onBarClick(bars[index].epochDay)
            }
        }
    ) {
        if (bars.isEmpty()) return@Canvas
        val gap = size.width * 0.01f
        val slot = size.width / bars.size
        val barWidth = (slot - gap).coerceAtLeast(1f)
        bars.forEachIndexed { i, bar ->
            val x = i * slot + gap / 2
            val seenH = size.height * (bar.seen.toFloat() / maxValue)
            val learnedH = size.height * (bar.learned.toFloat() / maxValue)
            val reviewedH = size.height * (bar.reviewed.toFloat() / maxValue)
            // seen (bottom)
            drawRect(
                color = seenColor,
                topLeft = Offset(x, size.height - seenH),
                size = Size(barWidth, seenH)
            )
            // learned (middle)
            drawRect(
                color = learnedColor,
                topLeft = Offset(x, size.height - seenH - learnedH),
                size = Size(barWidth, learnedH)
            )
            // reviewed (stacked on top)
            drawRect(
                color = reviewedColor,
                topLeft = Offset(x, size.height - seenH - learnedH - reviewedH),
                size = Size(barWidth, reviewedH)
            )
            if (bar.epochDay == selectedDate) {
                drawRect(
                    color = outlineColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun DayRow(
    bar: StatBar,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            bar.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            "S:${bar.seen}  L:${bar.learned}  R:${bar.reviewed}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatEpochDay(epochDay: Long): String {
    val d = LocalDate.ofEpochDay(epochDay)
    return "%02d.%02d.%d".format(d.dayOfMonth, d.monthValue, d.year)
}

