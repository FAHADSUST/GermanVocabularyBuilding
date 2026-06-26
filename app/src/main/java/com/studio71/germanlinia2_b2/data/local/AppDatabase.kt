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
        WordCommentEntity::class,
        WordImageEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun progressDao(): ProgressDao
    abstract fun statsDao(): StatsDao
    abstract fun wordMarkDao(): WordMarkDao
    abstract fun seenWordDao(): SeenWordDao
    abstract fun wordCommentDao(): WordCommentDao
    abstract fun wordImageDao(): WordImageDao

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

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "german_vocab.db"
                )
                    .addMigrations(MIGRATION_15_16)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}

