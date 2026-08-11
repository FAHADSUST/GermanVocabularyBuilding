package com.studio71.germanlinia2_b2.ui.settings

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import com.studio71.germanlinia2_b2.data.settings.SettingsStore
import com.studio71.germanlinia2_b2.data.settings.TtsHistoryEntry
import com.studio71.germanlinia2_b2.data.settings.ThemePreset
import com.studio71.germanlinia2_b2.data.sync.CloudSyncManager
import com.studio71.germanlinia2_b2.notify.ReminderScheduler
import com.studio71.germanlinia2_b2.ui.tts.TtsController
import com.studio71.germanlinia2_b2.ui.tts.TtsSessionSeed
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsStore,
    repository: VocabularyRepository,
    cloudSync: CloudSyncManager,
    onBack: () -> Unit,
    onOpenExtraNotes: () -> Unit
) {
    val state by settings.state.collectAsStateWithLifecycle()
    val syncState by cloudSync.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showThemeSubSettings by rememberSaveable { mutableStateOf(false) }
    var cloudEmail by rememberSaveable { mutableStateOf("") }
    var cloudPassword by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showThemeSubSettings) "Design" else "Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showThemeSubSettings) showThemeSubSettings = false else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        if (showThemeSubSettings) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Themes", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        ThemePreset.entries.forEach { preset ->
                            ThemeOptionRow(
                                title = preset.uiTitle(),
                                subtitle = preset.uiSubtitle(),
                                selected = state.themePreset == preset,
                                onClick = { settings.setThemePreset(preset) }
                            )
                        }

                        if (state.themePreset == ThemePreset.CUSTOM) {
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            CustomThemeEditor(
                                initialPrimary = state.themeCustomPrimary,
                                initialSecondary = state.themeCustomSecondary,
                                initialTertiary = state.themeCustomTertiary,
                                onApply = { p, s, t -> settings.setCustomThemeColors(p, s, t) }
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- Appearance ---
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Darstellung", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        SettingNavigationRow(
                            title = "Theme auswählen",
                            subtitle = "Hell, Eye Friendly, Dark, Custom und weitere",
                            onClick = { showThemeSubSettings = true }
                        )
                    }
                }

                // --- Extra Notes ---
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Extra Notizen & Texte", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        SettingNavigationRow(
                            title = "Meine Notizen & TTS",
                            subtitle = "Texte einfügen, speichern, bearbeiten und vorlesen lassen",
                            onClick = onOpenExtraNotes
                        )
                    }
                }

                // --- Cloud sync ---
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cloud-Sync (Firebase)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "Manueller Sync fur Kommentare, Markierungen, SRS-Fortschritt und TTS-Verlauf.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!syncState.configured) {
                            Text(
                                "Firebase ist noch nicht aktiv. Lege eine Datei app/google-services.json ab und starte neu.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else if (syncState.isSignedIn) {
                            Text(
                                "Konto: ${syncState.email ?: syncState.uid.orEmpty()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            FilledTonalButton(
                                onClick = { scope.launch { cloudSync.syncNow() } },
                                enabled = !syncState.isBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Jetzt synchronisieren")
                            }
                            FilledTonalButton(
                                onClick = { scope.launch { cloudSync.testConnection() } },
                                enabled = !syncState.isBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Verbindung testen")
                            }
                            FilledTonalButton(
                                onClick = { cloudSync.signOut() },
                                enabled = !syncState.isBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Abmelden")
                            }
                        } else {
                            OutlinedTextField(
                                value = cloudEmail,
                                onValueChange = { cloudEmail = it },
                                label = { Text("E-Mail") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = cloudPassword,
                                onValueChange = { cloudPassword = it },
                                label = { Text("Passwort") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                modifier = Modifier.fillMaxWidth()
                            )
                            FilledTonalButton(
                                onClick = { scope.launch { cloudSync.signIn(cloudEmail, cloudPassword) } },
                                enabled = !syncState.isBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Anmelden")
                            }
                            FilledTonalButton(
                                onClick = { scope.launch { cloudSync.register(cloudEmail, cloudPassword) } },
                                enabled = !syncState.isBusy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Konto erstellen")
                            }
                        }

                        syncState.lastSyncedAtEpochMs?.let { syncedAt ->
                            Text(
                                "Letzter Sync: ${formatHistoryTime(syncedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            syncState.statusMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        syncState.errorMessage?.let { error ->
                            Text(
                                error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        if (syncState.configured) {
                            HorizontalDivider()
                            var showDiagnostics by rememberSaveable { mutableStateOf(false) }
                            Text(
                                if (showDiagnostics) "Diagnose ausblenden" else "Diagnose anzeigen",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDiagnostics = !showDiagnostics }
                                    .padding(vertical = 4.dp)
                            )
                            if (showDiagnostics) {
                                val diag = remember(syncState.isSignedIn, syncState.email) { cloudSync.diagnostics() }
                                DiagnosticRow("Firebase aktiv", if (diag.firebaseConfigured) "Ja" else "Nein")
                                DiagnosticRow("Projekt", diag.projectId ?: "-")
                                DiagnosticRow("App-ID (Firebase)", diag.applicationId ?: "-")
                                DiagnosticRow("Paketname", diag.packageName)
                                DiagnosticRow("Firestore-DB", diag.firestoreDatabaseId)
                                DiagnosticRow(
                                    "Play-Dienste",
                                    if (diag.playServicesOk) "OK" else "${diag.playServicesStatusText} (${diag.playServicesStatusCode})"
                                )
                                DiagnosticRow("Angemeldet", if (diag.isSignedIn) (diag.email ?: "Ja") else "Nein")
                                Text(
                                    "Hinweis: Die Logzeile \"Failed to get service from broker / Unknown calling package name 'com.google.android.gms'\" " +
                                        "ist ein interner Play-Dienste-Hinweis und stoppt die Synchronisierung nicht. " +
                                        "Nutze \"Verbindung testen\", um den echten Status zu prufen.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

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
                                        settings.setReminderWindowStart(hour, minute)
                                        ReminderScheduler.apply(context, settings.state.value)
                                    },
                                    state.reminderWindowStartHour,
                                    state.reminderWindowStartMinute,
                                    true
                                ).show()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Zeitfenster Start",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (timeEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Erste erlaubte Lernzeit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "%02d:%02d".format(state.reminderWindowStartHour, state.reminderWindowStartMinute),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (timeEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(enabled = timeEnabled) {
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        settings.setReminderWindowEnd(hour, minute)
                                        ReminderScheduler.apply(context, settings.state.value)
                                    },
                                    state.reminderWindowEndHour,
                                    state.reminderWindowEndMinute,
                                    true
                                ).show()
                            }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Zeitfenster Ende",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (timeEnabled) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Spateste erlaubte Lernzeit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "%02d:%02d".format(state.reminderWindowEndHour, state.reminderWindowEndMinute),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (timeEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider()

                    StepperRow(
                        title = "Intervall min (Stunden)",
                        subtitle = "Kleinster Abstand zwischen Erinnerungen",
                        value = state.reminderIntervalMinHours,
                        onChange = {
                            settings.setReminderIntervalMinHours(it)
                            ReminderScheduler.apply(context, settings.state.value)
                        },
                        min = 1
                    )
                    StepperRow(
                        title = "Intervall max (Stunden)",
                        subtitle = "Grosster Abstand zwischen Erinnerungen",
                        value = state.reminderIntervalMaxHours,
                        onChange = {
                            settings.setReminderIntervalMaxHours(it)
                            ReminderScheduler.apply(context, settings.state.value)
                        },
                        min = 1
                    )
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
                    HorizontalDivider()
                    SettingSwitchRow(
                        title = "Beim Öffnen zur Wiederholung hinzufügen",
                        subtitle = "Öffnet man Wortdetails, kommt das Wort automatisch in den 2/3/7/15/30-Tage-Plan",
                        checked = state.autoAddSeenToReview,
                        onCheckedChange = { settings.setAutoAddSeenToReview(it) }
                    )
                    HorizontalDivider()
                    SettingSwitchRow(
                        title = "Reverse Recall aktivieren",
                        subtitle = "Optionales Spiel: Englisch sehen, deutsches Wort erinnern",
                        checked = state.gameReverseRecallEnabled,
                        onCheckedChange = { settings.setGameReverseRecallEnabled(it) }
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

                    HorizontalDivider()

                    SettingSwitchRow(
                        title = "Erinnerungsrunde aktivieren",
                        subtitle = "Nach jedem Intervall werden diese Wörter nur als Kopfwort wiederholt",
                        checked = state.ttsRecallPauseEnabled,
                        onCheckedChange = settings::setTtsRecallPauseEnabled
                    )
                    StepperRow(
                        title = "Intervall (Wörter)",
                        subtitle = "Nach wie vielen detaillierten Wörtern die Erinnerungsrunde startet",
                        value = state.ttsRecallPauseEveryWords,
                        onChange = settings::setTtsRecallPauseEveryWords,
                        min = 1
                    )
                    StepperRow(
                        title = "Pause je Wort (Sek.)",
                        subtitle = "In der Erinnerungsrunde: Wort sprechen, dann diese Pause",
                        value = (state.ttsRecallPauseMs / 1000).coerceAtLeast(1),
                        onChange = { seconds -> settings.setTtsRecallPauseMs(seconds * 1000) },
                        min = 1
                    )
                    SettingSwitchRow(
                        title = "Bedeutung nach Pause",
                        subtitle = "Nach der Pause wird die Bedeutung gesprochen, damit du dich prüfen kannst",
                        checked = state.ttsRecallSpeakMeaning,
                        onCheckedChange = settings::setTtsRecallSpeakMeaning
                    )
                }
            }

            // --- Background playback / battery ---
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Vorlese-Verlauf", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Zeigt Filter und bereits gespielten Bereich. Tippen startet wieder ab der letzten Position.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (state.ttsHistory.isEmpty()) {
                        Text(
                            "Noch kein Verlauf vorhanden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.ttsHistory.forEachIndexed { idx, entry ->
                            TtsHistoryRow(
                                entry = entry,
                                onResume = {
                                    scope.launch {
                                        val filter = entry.toFilter()
                                        val sortMode = entry.toSortMode()
                                        val words = repository.getFilteredSnapshot(
                                            filter = filter,
                                            query = entry.query,
                                            sortMode = sortMode
                                        )
                                        if (words.isNotEmpty()) {
                                            val resumeIndex = entry.resumeIndex.coerceIn(0, words.lastIndex)
                                            TtsController.start(
                                                context = context,
                                                words = words,
                                                startIndex = resumeIndex,
                                                sessionSeed = TtsSessionSeed(
                                                    filter = filter,
                                                    query = entry.query,
                                                    sortMode = sortMode
                                                )
                                            )
                                        }
                                    }
                                }
                            )
                            if (idx < state.ttsHistory.lastIndex) HorizontalDivider()
                        }

                        FilledTonalButton(
                            onClick = { settings.clearTtsHistory() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Verlauf leeren")
                        }
                    }
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
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TtsHistoryRow(
    entry: TtsHistoryEntry,
    onResume: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onResume).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                entry.filterSummary(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
            Text(
                "Bereich: ${entry.rangeSummary()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            formatHistoryTime(entry.playedAtEpochMs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CustomThemeEditor(
    initialPrimary: String,
    initialSecondary: String,
    initialTertiary: String,
    onApply: (String, String, String) -> Unit
) {
    var primaryInput by rememberSaveable(initialPrimary) { mutableStateOf(initialPrimary) }
    var secondaryInput by rememberSaveable(initialSecondary) { mutableStateOf(initialSecondary) }
    var tertiaryInput by rememberSaveable(initialTertiary) { mutableStateOf(initialTertiary) }

    val primaryValid = isValidColorHex(primaryInput)
    val secondaryValid = isValidColorHex(secondaryInput)
    val tertiaryValid = isValidColorHex(tertiaryInput)
    val canApply = primaryValid && secondaryValid && tertiaryValid

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Eigene Farben (#RRGGBB oder #AARRGGBB)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = primaryInput,
            onValueChange = { primaryInput = it },
            label = { Text("Primärfarbe") },
            singleLine = true,
            isError = !primaryValid,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = secondaryInput,
            onValueChange = { secondaryInput = it },
            label = { Text("Sekundärfarbe") },
            singleLine = true,
            isError = !secondaryValid,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = tertiaryInput,
            onValueChange = { tertiaryInput = it },
            label = { Text("Tertiärfarbe") },
            singleLine = true,
            isError = !tertiaryValid,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeColorPreview(
                modifier = Modifier.weight(1f),
                color = parseColorOrNull(primaryInput),
                fallback = MaterialTheme.colorScheme.primary
            )
            ThemeColorPreview(
                modifier = Modifier.weight(1f),
                color = parseColorOrNull(secondaryInput),
                fallback = MaterialTheme.colorScheme.secondary
            )
            ThemeColorPreview(
                modifier = Modifier.weight(1f),
                color = parseColorOrNull(tertiaryInput),
                fallback = MaterialTheme.colorScheme.tertiary
            )
        }

        FilledTonalButton(
            onClick = {
                onApply(
                    normalizeColorHex(primaryInput),
                    normalizeColorHex(secondaryInput),
                    normalizeColorHex(tertiaryInput)
                )
            },
            enabled = canApply,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Eigene Farben übernehmen")
        }
    }
}

@Composable
private fun ThemeColorPreview(modifier: Modifier = Modifier, color: Color?, fallback: Color) {
    Box(
        modifier
            .height(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color ?: fallback)
    )
}

@Composable
private fun ThemeOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}

@Composable
private fun SettingNavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("Öffnen", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
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

private fun ThemePreset.uiTitle(): String = when (this) {
    ThemePreset.SYSTEM -> "System"
    ThemePreset.LIGHT -> "Hell"
    ThemePreset.DARK -> "Dunkel"
    ThemePreset.LIGHT_EYE_CARE -> "Eye Friendly (Light)"
    ThemePreset.LIGHT_MINT -> "Light Mint"
    ThemePreset.LIGHT_LAVENDER -> "Light Lavender"
    ThemePreset.LIGHT_PEACH -> "Light Peach"
    ThemePreset.OCEAN -> "Ocean"
    ThemePreset.SUNSET -> "Sunset"
    ThemePreset.FOREST -> "Forest"
    ThemePreset.AMOLED -> "AMOLED"
    ThemePreset.CUSTOM -> "Custom"
}

private fun ThemePreset.uiSubtitle(): String = when (this) {
    ThemePreset.SYSTEM -> "Folgt automatisch den Geräteeinstellungen"
    ThemePreset.LIGHT -> "Immer im hellen Standard-Design"
    ThemePreset.DARK -> "Immer im dunklen Standard-Design"
    ThemePreset.LIGHT_EYE_CARE -> "Helles, augenschonendes warmes Design"
    ThemePreset.LIGHT_MINT -> "Heller Mint-Look"
    ThemePreset.LIGHT_LAVENDER -> "Heller Violett-Look"
    ThemePreset.LIGHT_PEACH -> "Heller warmer Peach-Look"
    ThemePreset.OCEAN -> "Eigene Blau-/Türkisfarben (hell/dunkel automatisch)"
    ThemePreset.SUNSET -> "Eigene warme Farben (hell/dunkel automatisch)"
    ThemePreset.FOREST -> "Natürlicher Grün-Look (hell/dunkel automatisch)"
    ThemePreset.AMOLED -> "Sehr dunkles Schwarz für OLED-Displays"
    ThemePreset.CUSTOM -> "Definiere Primär-/Sekundär-/Tertiärfarben selbst"
}

private fun isValidColorHex(raw: String): Boolean {
    val value = raw.trim().removePrefix("#")
    return (value.length == 6 || value.length == 8) && value.all { it.uppercaseChar() in "0123456789ABCDEF" }
}

private fun normalizeColorHex(raw: String): String {
    return "#${raw.trim().removePrefix("#").uppercase()}"
}

private fun parseColorOrNull(raw: String): Color? {
    val normalized = raw.trim().removePrefix("#")
    val argb = when (normalized.length) {
        6 -> "FF$normalized"
        8 -> normalized
        else -> return null
    }
    val parsed = argb.toLongOrNull(16) ?: return null
    return Color(parsed.toInt())
}

private fun formatHistoryTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    return DateTimeFormatter.ofPattern("dd.MM HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))
}

