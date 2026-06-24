package com.studio71.germanlinia2_b2.data.sync

import android.content.Context

/** Tracks local changes vs last successful cloud sync. */
object SyncStateTracker {

    private const val PREFS = "cloud_sync_meta"
    private const val KEY_LOCAL_MUTATED_AT = "local_mutated_at"
    private const val KEY_LAST_SYNCED_AT = "last_synced_at"

    fun markLocalMutation(context: Context, atEpochMs: Long = System.currentTimeMillis()) {
        prefs(context).edit().putLong(KEY_LOCAL_MUTATED_AT, atEpochMs).apply()
    }

    fun markSynced(context: Context, atEpochMs: Long = System.currentTimeMillis()) {
        prefs(context).edit()
            .putLong(KEY_LAST_SYNCED_AT, atEpochMs)
            .putLong(KEY_LOCAL_MUTATED_AT, atEpochMs)
            .apply()
    }

    fun localMutatedAt(context: Context): Long =
        prefs(context).getLong(KEY_LOCAL_MUTATED_AT, 0L)

    fun lastSyncedAt(context: Context): Long =
        prefs(context).getLong(KEY_LAST_SYNCED_AT, 0L)

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

