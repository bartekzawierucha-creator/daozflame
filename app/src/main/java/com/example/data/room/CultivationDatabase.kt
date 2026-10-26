package com.example.data.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Główna baza danych Room dla persystencji stanu kultywacji adeptów sekty.
 */
@Database(
    entities = [DiscipleCultivationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CultivationDatabase : RoomDatabase() {

    abstract fun discipleCultivationDao(): DiscipleCultivationDao

    companion object {
        @Volatile
        private var INSTANCE: CultivationDatabase? = null

        fun getDatabase(context: Context): CultivationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CultivationDatabase::class.java,
                    "wuxia_cultivation.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
