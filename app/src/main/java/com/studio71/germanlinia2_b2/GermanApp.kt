package com.studio71.germanlinia2_b2

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import com.studio71.germanlinia2_b2.data.settings.SettingsStore
import com.studio71.germanlinia2_b2.data.sync.CloudSyncManager
import com.studio71.germanlinia2_b2.notify.NotificationHelper
import com.studio71.germanlinia2_b2.notify.ReminderScheduler
import com.studio71.germanlinia2_b2.notify.WordImagePrefetchScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Holds the app-wide [VocabularyRepository] and seeds the database on first launch.
 * A tiny manual service locator keeps the project dependency-free (no Hilt needed).
 */
class GermanApp : Application(), ImageLoaderFactory {

    val repository: VocabularyRepository by lazy { VocabularyRepository(this) }

    val settings: SettingsStore by lazy { SettingsStore(this) }

    val cloudSync: CloudSyncManager by lazy { CloudSyncManager(this, settings) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch { repository.seedIfEmpty() }
        NotificationHelper.ensureChannel(this)
        ReminderScheduler.apply(this, settings.state.value)
        WordImagePrefetchScheduler.apply(this)
    }

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components {
                add(SvgDecoder.Factory())
            }
            .build()

    companion object {
        lateinit var instance: GermanApp
            private set
    }
}

