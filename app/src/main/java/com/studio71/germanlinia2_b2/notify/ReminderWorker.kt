package com.studio71.germanlinia2_b2.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository

/**
 * Runs daily, checks how many words are due, and posts a reminder if there are any.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val due = VocabularyRepository(applicationContext).dueCountNow()
            if (due > 0) {
                NotificationHelper.showDueReminder(applicationContext, due)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

