package com.studio71.germanlinia2_b2.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.studio71.germanlinia2_b2.MainActivity
import com.studio71.germanlinia2_b2.R

/** Creates the reminder channel and posts the "words due" notification. */
object NotificationHelper {

    const val CHANNEL_ID = "review_reminders"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Wiederholungs-Erinnerungen",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Tägliche Erinnerung an fällige Vokabeln"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showDueReminder(context: Context, dueCount: Int) {
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = if (dueCount == 1) {
            "1 Wort wartet auf deine Wiederholung."
        } else {
            "$dueCount Wörter warten auf deine Wiederholung."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_review)
            .setContentTitle("Zeit zum Wiederholen 📚")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // POST_NOTIFICATIONS is checked by the caller / handled gracefully by the system.
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission not granted yet; silently skip.
        }
    }
}

