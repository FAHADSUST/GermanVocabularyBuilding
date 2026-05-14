package com.studio71.germanlinia2_b2.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studio71.germanlinia2_b2.data.local.WordMarker

/** Fixed colors for the three difficulty markers. */
val WordMarker.color: Color
    get() = when (this) {
        WordMarker.HARD -> Color(0xFFE53935)   // red
        WordMarker.MEDIUM -> Color(0xFFF9A825)  // amber/yellow
        WordMarker.EASY -> Color(0xFF1E88E5)    // blue
        WordMarker.NONE -> Color.Unspecified
    }

/** Short German label for a marker (used in the filter dropdown). */
val WordMarker.label: String
    get() = when (this) {
        WordMarker.HARD -> "Schwer"
        WordMarker.MEDIUM -> "Mittel"
        WordMarker.EASY -> "Leicht"
        WordMarker.NONE -> "Nicht markiert"
    }

/**
 * Three tappable stars (red = hard, yellow = medium, blue = easy) for marking how
 * well a word is remembered. Tapping the already-selected star clears the mark.
 */
@Composable
fun MarkerStarsRow(
    current: WordMarker,
    onSelect: (WordMarker) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(WordMarker.HARD, WordMarker.MEDIUM, WordMarker.EASY).forEach { marker ->
            val selected = current == marker
            IconButton(
                onClick = { onSelect(if (selected) WordMarker.NONE else marker) }
            ) {
                Icon(
                    imageVector = if (selected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = marker.label,
                    tint = if (selected) marker.color else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** Small solid star in the marker color; nothing is drawn for [WordMarker.NONE]. */
@Composable
fun MarkerStar(marker: WordMarker, size: Dp = 18.dp, modifier: Modifier = Modifier) {
    if (marker == WordMarker.NONE) return
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = marker.label,
        tint = marker.color,
        modifier = modifier.size(size)
    )
}

/**
 * Filter chip that opens a dropdown to filter the list by difficulty marker.
 * [selected] == null means "all words".
 */
@Composable
fun MarkerFilterChip(
    selected: WordMarker?,
    modifier: Modifier = Modifier,
    onSelected: (WordMarker?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        FilterChip(
            selected = selected != null,
            onClick = { expanded = true },
            leadingIcon = selected?.let { { MarkerStar(it, size = 18.dp) } },
            label = { Text(selected?.label ?: "Markierung") }
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Alle (Markierung)") },
                onClick = { onSelected(null); expanded = false }
            )
            listOf(WordMarker.HARD, WordMarker.MEDIUM, WordMarker.EASY).forEach { marker ->
                DropdownMenuItem(
                    leadingIcon = { MarkerStar(marker, size = 18.dp) },
                    text = { Text(marker.label) },
                    onClick = { onSelected(marker); expanded = false }
                )
            }
        }
    }
}

