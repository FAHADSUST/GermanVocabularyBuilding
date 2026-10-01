package com.studio71.germanlinia2_b2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        VocabularyEntity::class,
        ProgressEntity::class,
        DailyStatEntity::class,
        WordMarkEntity::class,
        SeenWordEntity::class,
        SeenEventEntity::class,
        WordCommentEntity::class,
        WordImageEntity::class,
        ActiveGameEntity::class,
        ExtraNoteEntity::class
    ],
    version = 21,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun progressDao(): ProgressDao
    abstract fun statsDao(): StatsDao
    abstract fun wordMarkDao(): WordMarkDao
    abstract fun seenWordDao(): SeenWordDao
    abstract fun seenEventDao(): SeenEventDao
    abstract fun wordCommentDao(): WordCommentDao
    abstract fun wordImageDao(): WordImageDao
    abstract fun activeGameDao(): ActiveGameDao
    abstract fun extraNoteDao(): ExtraNoteDao

    companion object {
        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_image` (
                        `wordId` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        `query` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastTriedAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`wordId`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds the learner-facing memory tip column without touching existing data. */
        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE vocabulary ADD COLUMN memoryTips TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /** Adds append-only seen-event history for "last 50 events" review + sync. */
        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `seen_event` (
                        `eventId` TEXT NOT NULL,
                        `wordId` TEXT NOT NULL,
                        `seenAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`eventId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_seen_event_wordId` ON `seen_event` (`wordId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_seen_event_seenAtEpochMs` ON `seen_event` (`seenAtEpochMs`)")
            }
        }

        /**
         * Repairs installs that were already on v18 but with an older Room identity hash.
         * All operations are additive/idempotent so existing user data remains intact.
         */
        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                if (!db.hasColumn("vocabulary", "memoryTips")) {
                    db.execSQL("ALTER TABLE `vocabulary` ADD COLUMN `memoryTips` TEXT NOT NULL DEFAULT ''")
                }

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_image` (
                        `wordId` TEXT NOT NULL,
                        `imageUrl` TEXT,
                        `query` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `lastTriedAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`wordId`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `seen_event` (
                        `eventId` TEXT NOT NULL,
                        `wordId` TEXT NOT NULL,
                        `seenAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`eventId`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_seen_event_wordId` ON `seen_event` (`wordId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_seen_event_seenAtEpochMs` ON `seen_event` (`seenAtEpochMs`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `word_comment` (
                        `wordId` TEXT NOT NULL,
                        `comment` TEXT NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL,
                        PRIMARY KEY(`wordId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `active_game` (
                        `id` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `wordIds` TEXT NOT NULL,
                        `progressIndex` INTEGER NOT NULL,
                        `knownCount` INTEGER NOT NULL,
                        `againCount` INTEGER NOT NULL,
                        `extraData` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `extra_notes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `createdAtEpochMs` INTEGER NOT NULL,
                        `updatedAtEpochMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        /** Supports app downgrades by removing the v21-only table. */
        val MIGRATION_21_20 = object : Migration(21, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `extra_notes`")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "german_vocab.db"
                )
                    .addMigrations(MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_20)
                    .build()
                    .also { INSTANCE = it }
            }

        private fun SupportSQLiteDatabase.hasColumn(tableName: String, columnName: String): Boolean {
            query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                if (nameIndex < 0) return false
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == columnName) return true
                }
            }
            return false
        }
    }
}

