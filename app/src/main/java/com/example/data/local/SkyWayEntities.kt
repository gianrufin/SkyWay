package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_routes")
data class SavedRouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originName: String,
    val originSubtitle: String,
    val originLat: Double,
    val originLon: Double,
    val destName: String,
    val destSubtitle: String,
    val destLat: Double,
    val destLon: Double,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val createdAtEpoch: Long = System.currentTimeMillis() / 1000L,
)

@Entity(tableName = "recent_places")
data class RecentPlaceEntity(
    @PrimaryKey val placeKey: String,
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
    val lastUsedEpoch: Long = System.currentTimeMillis() / 1000L,
)
