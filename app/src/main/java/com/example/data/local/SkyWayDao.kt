package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SkyWayDao {

    @Query("SELECT * FROM saved_routes ORDER BY createdAtEpoch DESC")
    fun observeSavedRoutes(): Flow<List<SavedRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedRoute(route: SavedRouteEntity): Long

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteSavedRoute(id: Long)

    @Query(
        "DELETE FROM saved_routes WHERE originName = :originName AND destName = :destName"
    )
    suspend fun deleteSavedRouteByNames(originName: String, destName: String)

    @Query("SELECT * FROM recent_places ORDER BY lastUsedEpoch DESC LIMIT 10")
    fun observeRecentPlaces(): Flow<List<RecentPlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecentPlace(place: RecentPlaceEntity)
}
