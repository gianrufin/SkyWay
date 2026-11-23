package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedRouteEntity::class, RecentPlaceEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class SkyWayDatabase : RoomDatabase() {
    abstract fun skyWayDao(): SkyWayDao

    companion object {
        @Volatile
        private var INSTANCE: SkyWayDatabase? = null

        fun getInstance(context: Context): SkyWayDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SkyWayDatabase::class.java,
                    "skyway_db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
