package com.studio71.germanlinia2_b2

import android.app.Application
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import com.studio71.germanlinia2_b2.data.settings.SettingsStore
import com.studio71.germanlinia2_b2.notify.NotificationHelper
import com.studio71.germanlinia2_b2.notify.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Holds the app-wide [VocabularyRepository] and seeds the database on first launch.
 * A tiny manual service locator keeps the project dependency-free (no Hilt needed).
 */
class GermanApp : Application() {

    val repository: VocabularyRepository by lazy { VocabularyRepository(this) }

    val settings: SettingsStore by lazy { SettingsStore(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch { repository.seedIfEmpty() }
        NotificationHelper.ensureChannel(this)
        ReminderScheduler.apply(this, settings.state.value)
    }

    companion object {
        lateinit var instance: GermanApp
            private set
    }
}

