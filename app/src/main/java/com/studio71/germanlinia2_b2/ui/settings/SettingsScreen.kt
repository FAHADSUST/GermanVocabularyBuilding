package com.studio71.germanlinia2_b2.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.data.settings.SettingsStore
import com.studio71.germanlinia2_b2.notify.ReminderScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    onBack: () -> Unit
) {
    val state by settings.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // --- Reminder ---
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Erinnerung", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)

                    SettingSwitchRow(
                        title = "Tägliche Erinnerung",
                        subtitle = "Benachrichtigung, wenn Wörter fällig sind",
                        checked = state.reminderEnabled,
                        onCheckedChange = { enabled ->
                            settings.setReminderEnabled(enabled)
                            ReminderScheduler.apply(context, settings.state.value)
                        }
                    )

                    HorizontalDivider()

                    val timeEnabled = state.reminderEnabled
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = timeEnabled) {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        settings.setReminderTime(hour, minute)
                                        ReminderScheduler.apply(context, settings.state.value)
                                    },
                                    state.reminderHour,
                                    state.reminderMinute,
                                    true
                                ).show()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Uhrzeit",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (timeEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Erinnerung um diese Zeit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            state.reminderTimeLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (timeEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- Review ---
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Wiederholung", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    SettingSwitchRow(
                        title = "Beim Aufdecken vorlesen",
                        subtitle = "Das Wort automatisch sprechen, wenn die Antwort gezeigt wird",
                        checked = state.autoSpeakOnReveal,
                        onCheckedChange = { settings.setAutoSpeakOnReveal(it) }
                    )
                }
            }

            // --- Text-to-speech playback ---
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sprachausgabe (Vorlesen der Liste)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Reihenfolge je Wort: Wort → Bedeutung → Beispiel → Formen. " +
                            "Dieser Block wird so oft wiederholt, wie unter „Durchläufe“ eingestellt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    StepperRow(
                        title = "Wort",
                        subtitle = "Wie oft das Wort gesprochen wird",
                        value = state.ttsWordRepeat,
                        onChange = settings::setTtsWordRepeat,
                        min = 0
                    )
                    StepperRow(
                        title = "Bedeutung",
                        subtitle = "Wie oft die Bedeutung gesprochen wird",
                        value = state.ttsMeaningRepeat,
                        onChange = settings::setTtsMeaningRepeat,
                        min = 0
                    )
                    StepperRow(
                        title = "Beispiel",
                        subtitle = "Wie oft der Beispielsatz gesprochen wird",
                        value = state.ttsExampleRepeat,
                        onChange = settings::setTtsExampleRepeat,
                        min = 0
                    )
                    StepperRow(
                        title = "Verb-/Adjektivformen",
                        subtitle = "Wie oft die Formen gesprochen werden",
                        value = state.ttsVerbFormRepeat,
                        onChange = settings::setTtsVerbFormRepeat,
                        min = 0
                    )

                    HorizontalDivider()

                    StepperRow(
                        title = "Durchläufe",
                        subtitle = "Wie oft der ganze Block je Wort wiederholt wird",
                        value = state.ttsLoopCount,
                        onChange = settings::setTtsLoopCount,
                        min = 1
                    )
                    StepperRow(
                        title = "Pause (ms)",
                        subtitle = "Pause zwischen zwei gesprochenen Teilen",
                        value = state.ttsGapMs,
                        onChange = { settings.setTtsGapMs(it) },
                        min = 0,
                        step = 50
                    )
                }
            }

            // --- Background playback / battery ---
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Hintergrundwiedergabe", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Damit das Vorlesen bei ausgeschaltetem Bildschirm weiterläuft, sollte die " +
                            "Akku-Optimierung für diese App deaktiviert werden.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val ignoring = isIgnoringBatteryOptimizations(context)
                    Text(
                        if (ignoring) "Status: Akku-Optimierung ist deaktiviert ✓"
                        else "Status: Akku-Optimierung ist aktiv",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ignoring) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    FilledTonalButton(
                        onClick = { requestIgnoreBatteryOptimizations(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.BatteryStd, contentDescription = null, Modifier.size(18.dp))
                        Text("  Akku-Optimierung verwalten")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StepperRow(
    title: String,
    subtitle: String,
    value: Int,
    onChange: (Int) -> Unit,
    min: Int = 0,
    step: Int = 1
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = { onChange((value - step).coerceAtLeast(min)) }, enabled = value > min) {
            Icon(Icons.Default.Remove, contentDescription = "Weniger")
        }
        Text(
            value.toString(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.size(width = 44.dp, height = 24.dp)
        )
        IconButton(onClick = { onChange(value + step) }) {
            Icon(Icons.Default.Add, contentDescription = "Mehr")
        }
    }
}

private fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun requestIgnoreBatteryOptimizations(context: Context) {
    val intent = if (isIgnoringBatteryOptimizations(context)) {
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    } else {
        Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            "package:${context.packageName}".toUri()
        )
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fall back to the general battery optimization list.
        runCatching {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
}

