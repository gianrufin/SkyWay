package com.example.model

import androidx.compose.runtime.Immutable

enum class UnitSystem {
    METRIC,
    IMPERIAL,
}

enum class MapStyle(val label: String, val shortLabel: String) {
    DARK("Dark Cockpit", "Dark"),
    TOPO("Topographic", "Topo"),
    SATELLITE("Satellite Imagery", "Satellite"),
    STREET("Light Street", "Street"),
}

enum class SearchTarget {
    ORIGIN,
    DESTINATION,
}

enum class WeatherCondition(val label: String) {
    CLEAR("Clear"),
    PARTLY_CLOUDY("Partly Cloudy"),
    CLOUDY("Overcast"),
    FOG("Fog & Mist"),
    DRIZZLE("Drizzle"),
    RAIN("Rain"),
    HEAVY_RAIN("Heavy Rain"),
    SNOW("Snow"),
    THUNDERSTORM("Thunderstorm");

    companion object {
        fun fromWmoCode(code: Int): WeatherCondition = when (code) {
            0 -> CLEAR
            1, 2 -> PARTLY_CLOUDY
            3 -> CLOUDY
            45, 48 -> FOG
            51, 53, 55, 56, 57 -> DRIZZLE
            61, 63, 80, 81 -> RAIN
            65, 66, 67, 82 -> HEAVY_RAIN
            71, 73, 75, 77, 85, 86 -> SNOW
            95, 96, 99 -> THUNDERSTORM
            else -> PARTLY_CLOUDY
        }
    }
}

enum class WeatherSeverity {
    CLEAR,
    CAUTION,
    HAZARD,
}

enum class GlareRisk(val label: String) {
    NONE("No Glare"),
    MODERATE("Low Sun"),
    SEVERE("Head-On Glare"),
}

enum class SolarEventType(val label: String) {
    SUNRISE("Sunrise"),
    SUNSET("Sunset"),
}

@Immutable
data class LatLon(
    val latitude: Double,
    val longitude: Double,
)

@Immutable
data class PlacePoint(
    val name: String,
    val subtitle: String,
    val latitude: Double,
    val longitude: Double,
)

@Immutable
data class RouteCheckpoint(
    val index: Int,
    val latitude: Double,
    val longitude: Double,
    val distanceFromStartMeters: Double,
    val durationFromStartSeconds: Double,
    val bearingDegrees: Double,
    val label: String,
    val regionLabel: String = "",
)

@Immutable
data class RouteBundle(
    val origin: PlacePoint,
    val destination: PlacePoint,
    val polylinePoints: List<LatLon>,
    val totalDistanceMeters: Double,
    val totalDurationSeconds: Double,
    val checkpoints: List<RouteCheckpoint>,
    val forecasts: List<CheckpointForecast>,
)

@Immutable
data class CheckpointForecast(
    val utcOffsetSeconds: Int,
    val timezoneAbbreviation: String,
    val hourlyEpochs: List<Long>,
    val tempsC: List<Double>,
    val apparentTempsC: List<Double>,
    val weatherCodes: List<Int>,
    val precipMm: List<Double>,
    val precipProbPct: List<Int>,
    val windSpeedKmh: List<Double>,
    val windDirDeg: List<Double>,
    val visibilityMeters: List<Double>,
    val cloudCoverPct: List<Int>,
    val dailySunriseEpochs: List<Long>,
    val dailySunsetEpochs: List<Long>,
)

@Immutable
data class SolarState(
    val elevationDeg: Double,
    val azimuthDeg: Double,
    val relativeAngleDeg: Double,
    val glareRisk: GlareRisk,
    val isDaylight: Boolean,
)

@Immutable
data class SolarBoundaryMarker(
    val type: SolarEventType,
    val eventEpochSeconds: Long,
    val utcOffsetSeconds: Int,
    val afterCheckpointIndex: Int,
    val locationHint: String,
)

@Immutable
data class EvaluatedCheckpoint(
    val checkpoint: RouteCheckpoint,
    val passByEpochSeconds: Long,
    val utcOffsetSeconds: Int,
    val timezoneAbbreviation: String,
    val temperatureC: Double,
    val apparentTempC: Double,
    val condition: WeatherCondition,
    val precipMm: Double,
    val precipProbPct: Int,
    val windSpeedKmh: Double,
    val windDirDeg: Double,
    val visibilityMeters: Double,
    val cloudCoverPct: Int,
    val solarState: SolarState,
    val severity: WeatherSeverity,
    val hazardSummary: String,
)

@Immutable
data class EvaluatedRoute(
    val bundle: RouteBundle,
    val departureEpochSeconds: Long,
    val arrivalEpochSeconds: Long,
    val checkpoints: List<EvaluatedCheckpoint>,
    val solarMarkers: List<SolarBoundaryMarker>,
    val overallSeverity: WeatherSeverity,
    val bestWindowOffsetMinutes: Int?,
    val bestWindowScore: Double,
)

@Immutable
data class DepartureWindowSlot(
    val offsetMinutes: Int,
    val severity: WeatherSeverity,
    val score: Double,
)

@Immutable
data class SavedRouteItem(
    val id: Long,
    val origin: PlacePoint,
    val destination: PlacePoint,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val createdAtEpoch: Long,
)

@Immutable
data class RadarFrame(
    val timeEpoch: Long,
    val tileUrlTemplate: String,
)

sealed interface RouteLoadState {
    data object Idle : RouteLoadState
    data class Loading(val stepLabel: String) : RouteLoadState
    data object Ready : RouteLoadState
    data class Error(val message: String) : RouteLoadState
}
