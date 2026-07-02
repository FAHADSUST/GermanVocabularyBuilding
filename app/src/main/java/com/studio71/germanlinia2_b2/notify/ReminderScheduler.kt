package com.studio71.germanlinia2_b2.notify

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.studio71.germanlinia2_b2.data.settings.AppSettings
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.random.Random
import java.util.concurrent.TimeUnit

/** Schedules (or cancels) interval reminders within the configured study window. */
object ReminderScheduler {

    private const val WORK_NAME = "interval_review_game_reminder"

    /** Apply the current settings: schedule when enabled, cancel when disabled. */
    fun apply(context: Context, settings: AppSettings) {
        if (settings.reminderEnabled) {
            scheduleNext(context, settings)
        } else {
            cancel(context)
        }
    }

    fun scheduleNext(context: Context, settings: AppSettings) {
        val delayMs = nextDelayMillis(settings)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    private fun nextDelayMillis(settings: AppSettings): Long {
        val now = LocalDateTime.now()
        val start = safeTime(settings.reminderWindowStartHour, settings.reminderWindowStartMinute, LocalTime.of(6, 0))
        val end = safeTime(settings.reminderWindowEndHour, settings.reminderWindowEndMinute, LocalTime.of(23, 0))
        val minHours = settings.reminderIntervalMinHours.coerceIn(1, 12)
        val maxHours = settings.reminderIntervalMaxHours.coerceIn(minHours, 12)
        val minMinutes = minHours * 60
        val maxMinutes = maxHours * 60
        val randomizedMinutes = Random.nextInt(from = minMinutes, until = maxMinutes + 1)

        val base = if (isWithinWindow(now.toLocalTime(), start, end)) now else nextWindowStart(now, start)
        val tentative = base.plusMinutes(randomizedMinutes.toLong())
        val next = shiftIntoWindowWithCarryover(tentative, start, end)

        return Duration.between(now, next).toMillis()
            .coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
    }

    /** Option B: keep interval overflow and carry it to the next valid study window. */
    private fun shiftIntoWindowWithCarryover(
        candidate: LocalDateTime,
        start: LocalTime,
        end: LocalTime
    ): LocalDateTime {
        var out = candidate
        var guard = 0
        while (guard < 8) {
            val time = out.toLocalTime()
            if (isWithinWindow(time, start, end)) return out
            out = if (time.isAfter(end)) {
                val overflow = Duration.between(end, time).toMinutes().coerceAtLeast(0)
                out.toLocalDate().plusDays(1).atTime(start).plusMinutes(overflow)
            } else {
                out.toLocalDate().atTime(start)
            }
            guard++
        }
        return out
    }

    private fun isWithinWindow(time: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        if (!end.isAfter(start)) return true
        return !time.isBefore(start) && !time.isAfter(end)
    }

    private fun nextWindowStart(now: LocalDateTime, start: LocalTime): LocalDateTime {
        val todayStart = now.toLocalDate().atTime(start)
        return if (now.isBefore(todayStart)) todayStart else todayStart.plusDays(1)
    }

    private fun safeTime(hour: Int, minute: Int, fallback: LocalTime): LocalTime {
        return runCatching { LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59)) }
            .getOrDefault(fallback)
    }
}

