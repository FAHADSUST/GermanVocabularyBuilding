package com.studio71.germanlinia2_b2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studio71.germanlinia2_b2.notify.NotificationHelper
import com.studio71.germanlinia2_b2.ui.nav.AppNav
import com.studio71.germanlinia2_b2.ui.nav.Routes
import com.studio71.germanlinia2_b2.ui.theme.GermanVocabTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        val app = application as GermanApp
        val launchRoute = intent?.getStringExtra(NotificationHelper.EXTRA_OPEN_ROUTE)
            ?.takeIf { it.isNotBlank() }
            ?: Routes.LIST
        setContent {
            val settingsState by app.settings.state.collectAsStateWithLifecycle()
            GermanVocabTheme(
                themePreset = settingsState.themePreset,
                customPrimaryHex = settingsState.themeCustomPrimary,
                customSecondaryHex = settingsState.themeCustomSecondary,
                customTertiaryHex = settingsState.themeCustomTertiary
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav(
                        repository = app.repository,
                        settings = app.settings,
                        cloudSync = app.cloudSync,
                        startDestination = launchRoute
                    )
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

