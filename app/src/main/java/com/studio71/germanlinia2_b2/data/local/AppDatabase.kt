package com.studio71.germanlinia2_b2.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VocabularyEntity::class,
        ProgressEntity::class,
        DailyStatEntity::class,
        WordMarkEntity::class,
        SeenWordEntity::class
    ],
    version = 14,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao
    abstract fun progressDao(): ProgressDao
    abstract fun statsDao(): StatsDao
    abstract fun wordMarkDao(): WordMarkDao
    abstract fun seenWordDao(): SeenWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "german_vocab.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
    }
}

