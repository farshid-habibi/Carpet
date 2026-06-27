package com.farsh.carpetmapreader.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MapProject::class, MapCell::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mapDao(): MapDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "carpet_reader_db"
                )
                .fallbackToDestructiveMigration() // safe for early dynamic dev
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
