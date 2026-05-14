package com.studio71.germanlinia2_b2.notify

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.studio71.germanlinia2_b2.data.settings.AppSettings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Schedules (or cancels) a once-per-day reminder at the user's chosen time. */
object ReminderScheduler {

    private const val WORK_NAME = "daily_review_reminder"

    /** Apply the current settings: schedule when enabled, cancel when disabled. */
    fun apply(context: Context, settings: AppSettings) {
        if (settings.reminderEnabled) {
            scheduleDaily(context, settings.reminderHour, settings.reminderMinute)
        } else {
            cancel(context)
        }
    }

    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(initialDelayMillis(LocalTime.of(hour, minute)), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Milliseconds until the next occurrence of [time]. */
    private fun initialDelayMillis(time: LocalTime): Long {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(time)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis()
    }
}

