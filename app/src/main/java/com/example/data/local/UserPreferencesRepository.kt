package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.MapStyle
import com.example.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.skyWayDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "skyway_preferences"
)

data class UserPreferences(
    val unitSystem: UnitSystem = defaultLocaleUnits(),
    val mapStyle: MapStyle = MapStyle.DARK,
    val radarEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
)

private fun defaultLocaleUnits(): UnitSystem {
    val country = Locale.getDefault().country.uppercase(Locale.US)
    return if (country == "US" || country == "LR" || country == "MM") {
        UnitSystem.IMPERIAL
    } else {
        UnitSystem.METRIC
    }
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val MAP_STYLE = stringPreferencesKey("map_style")
        val RADAR_ENABLED = booleanPreferencesKey("radar_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
    }

    val preferencesFlow: Flow<UserPreferences> = context.skyWayDataStore.data.map { prefs ->
        val unitStr = prefs[Keys.UNIT_SYSTEM]
        val units = when (unitStr) {
            UnitSystem.METRIC.name -> UnitSystem.METRIC
            UnitSystem.IMPERIAL.name -> UnitSystem.IMPERIAL
            else -> defaultLocaleUnits()
        }
        val mapStyleStr = prefs[Keys.MAP_STYLE]
        val style = when (mapStyleStr) {
            MapStyle.DARK.name -> MapStyle.DARK
            MapStyle.TOPO.name -> MapStyle.TOPO
            MapStyle.SATELLITE.name -> MapStyle.SATELLITE
            MapStyle.STREET.name -> MapStyle.STREET
            else -> MapStyle.DARK
        }
        UserPreferences(
            unitSystem = units,
            mapStyle = style,
            radarEnabled = prefs[Keys.RADAR_ENABLED] ?: false,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
        )
    }

    suspend fun setUnitSystem(unitSystem: UnitSystem) {
        context.skyWayDataStore.edit { it[Keys.UNIT_SYSTEM] = unitSystem.name }
    }

    suspend fun setMapStyle(mapStyle: MapStyle) {
        context.skyWayDataStore.edit { it[Keys.MAP_STYLE] = mapStyle.name }
    }

    suspend fun setRadarEnabled(enabled: Boolean) {
        context.skyWayDataStore.edit { it[Keys.RADAR_ENABLED] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.skyWayDataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }
}
