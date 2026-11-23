package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeoResult>? = null,
)

@JsonClass(generateAdapter = true)
data class OpenMeteoGeoResult(
    val id: Long? = null,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
)

@JsonClass(generateAdapter = true)
data class PhotonResponse(
    val features: List<PhotonFeature>? = null,
)

@JsonClass(generateAdapter = true)
data class PhotonFeature(
    val geometry: PhotonGeometry? = null,
    val properties: PhotonProperties? = null,
)

@JsonClass(generateAdapter = true)
data class PhotonGeometry(
    val coordinates: List<Double>? = null, // [lon, lat]
)

@JsonClass(generateAdapter = true)
data class PhotonProperties(
    val name: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val street: String? = null,
    val housenumber: String? = null,
)

@JsonClass(generateAdapter = true)
data class OsrmRouteResponse(
    val code: String,
    val routes: List<OsrmRoute>? = null,
)

@JsonClass(generateAdapter = true)
data class OsrmRoute(
    val distance: Double,
    val duration: Double,
    val geometry: OsrmGeometry,
)

@JsonClass(generateAdapter = true)
data class OsrmGeometry(
    val type: String,
    val coordinates: List<List<Double>>, // [[lon, lat], ...]
)

@JsonClass(generateAdapter = true)
data class OpenMeteoForecastDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int = 0,
    @Json(name = "timezone_abbreviation") val timezoneAbbreviation: String = "UTC",
    val hourly: OpenMeteoHourlyDto? = null,
    val daily: OpenMeteoDailyDto? = null,
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourlyDto(
    val time: List<Long> = emptyList(),
    @Json(name = "temperature_2m") val temperature2m: List<Double?> = emptyList(),
    @Json(name = "apparent_temperature") val apparentTemperature: List<Double?> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
    val precipitation: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @Json(name = "wind_speed_10m") val windSpeed10m: List<Double?> = emptyList(),
    @Json(name = "wind_direction_10m") val windDirection10m: List<Double?> = emptyList(),
    val visibility: List<Double?> = emptyList(),
    @Json(name = "cloud_cover") val cloudCover: List<Int?> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class OpenMeteoDailyDto(
    val time: List<Long> = emptyList(),
    val sunrise: List<Long?> = emptyList(),
    val sunset: List<Long?> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RainViewerResponse(
    val host: String = "https://tilecache.rainviewer.com",
    val radar: RainViewerRadarBlock? = null,
)

@JsonClass(generateAdapter = true)
data class RainViewerRadarBlock(
    val past: List<RainViewerFrameDto> = emptyList(),
    val nowcast: List<RainViewerFrameDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class RainViewerFrameDto(
    val time: Long,
    val path: String,
)
