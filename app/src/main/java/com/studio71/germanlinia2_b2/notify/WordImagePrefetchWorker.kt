package com.studio71.germanlinia2_b2.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository

/**
 * Background prefetch for missing word meaning images.
 * Runs under strict constraints (charging + unmetered network + battery not low).
 */
class WordImagePrefetchWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            VocabularyRepository(applicationContext).prefetchMissingImages(limit = 20)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

