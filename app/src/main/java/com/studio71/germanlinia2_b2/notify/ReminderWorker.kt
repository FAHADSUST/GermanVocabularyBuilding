package com.studio71.germanlinia2_b2.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.studio71.germanlinia2_b2.data.repo.VocabularyRepository
import com.studio71.germanlinia2_b2.data.settings.SettingsStore
import com.studio71.germanlinia2_b2.domain.game.GameSessionPlanner

/**
 * Runs on each scheduled interval, posts a game reminder, and schedules the next run.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val settingsStore = SettingsStore(applicationContext)
            val settings = settingsStore.state.value
            val due = VocabularyRepository(applicationContext).dueCountNow()
            val (mode, nextIndex) = GameSessionPlanner.chooseNextMode(settings)

            NotificationHelper.showGameReminder(
                context = applicationContext,
                dueCount = due,
                modeRoute = mode.routeValue,
                modeTitle = mode.title
            )
            settingsStore.setNextGameModeIndex(nextIndex)

            // Chain the next one-time interval reminder.
            ReminderScheduler.apply(applicationContext, settingsStore.state.value)
            if (!settings.reminderEnabled) {
                ReminderScheduler.cancel(applicationContext)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

