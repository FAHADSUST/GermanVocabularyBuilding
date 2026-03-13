package com.studio71.germanlinia2_b2

import android.app.Application
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        appScope.launch { repository.seedIfEmpty() }
    }

    companion object {
        lateinit var instance: GermanApp
            private set
    }
}

