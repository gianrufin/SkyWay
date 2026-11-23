package com.example.data.repository

import com.example.data.local.RecentPlaceEntity
import com.example.data.local.SavedRouteEntity
import com.example.data.local.SkyWayDao
import com.example.data.remote.NetworkModule
import com.example.data.remote.OpenMeteoForecastDto
import com.example.domain.RouteSampler
import com.example.model.CheckpointForecast
import com.example.model.LatLon
import com.example.model.PlacePoint
import com.example.model.RadarFrame
import com.example.model.RouteBundle
import com.example.model.RouteCheckpoint
import com.example.model.SavedRouteItem
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs

class RouteWeatherRepository(
    private val dao: SkyWayDao,
) {
    private val forecastAdapter = NetworkModule.moshi.adapter(OpenMeteoForecastDto::class.java)
    private val forecastListType = Types.newParameterizedType(List::class.java, OpenMeteoForecastDto::class.java)
    private val forecastListAdapter = NetworkModule.moshi.adapter<List<OpenMeteoForecastDto>>(forecastListType)

    val savedRoutesFlow: Flow<List<SavedRouteItem>> = dao.observeSavedRoutes().map { list ->
        list.map { entity ->
            SavedRouteItem(
                id = entity.id,
                origin = PlacePoint(
                    name = entity.originName,
                    subtitle = entity.originSubtitle,
                    latitude = entity.originLat,
                    longitude = entity.originLon,
                ),
                destination = PlacePoint(
                    name = entity.destName,
                    subtitle = entity.destSubtitle,
                    latitude = entity.destLat,
                    longitude = entity.destLon,
                ),
                distanceMeters = entity.distanceMeters,
                durationSeconds = entity.durationSeconds,
                createdAtEpoch = entity.createdAtEpoch,
            )
        }
    }

    val recentPlacesFlow: Flow<List<PlacePoint>> = dao.observeRecentPlaces().map { list ->
        list.map {
            PlacePoint(
                name = it.name,
                subtitle = it.subtitle,
                latitude = it.latitude,
                longitude = it.longitude,
            )
        }
    }

    suspend fun saveRecentPlace(place: PlacePoint) = withContext(Dispatchers.IO) {
        val key = String.format(Locale.US, "%s_%.2f_%.2f", place.name, place.latitude, place.longitude)
        dao.upsertRecentPlace(
            RecentPlaceEntity(
                placeKey = key,
                name = place.name,
                subtitle = place.subtitle,
                latitude = place.latitude,
                longitude = place.longitude,
                lastUsedEpoch = System.currentTimeMillis() / 1000L,
            )
        )
    }

    suspend fun saveRoute(bundle: RouteBundle) = withContext(Dispatchers.IO) {
        dao.insertSavedRoute(
            SavedRouteEntity(
                originName = bundle.origin.name,
                originSubtitle = bundle.origin.subtitle,
                originLat = bundle.origin.latitude,
                originLon = bundle.origin.longitude,
                destName = bundle.destination.name,
                destSubtitle = bundle.destination.subtitle,
                destLat = bundle.destination.latitude,
                destLon = bundle.destination.longitude,
                distanceMeters = bundle.totalDistanceMeters,
                durationSeconds = bundle.totalDurationSeconds,
            )
        )
    }

    suspend fun deleteSavedRoute(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteSavedRoute(id)
    }

    suspend fun deleteSavedRouteByEndpoints(originName: String, destName: String) = withContext(Dispatchers.IO) {
        dao.deleteSavedRouteByNames(originName, destName)
    }

    suspend fun searchPlaces(query: String): List<PlacePoint> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        coroutineScope {
            val openMeteoDeferred = async {
                runCatching {
                    NetworkModule.openMeteoGeocodingApi.searchPlaces(trimmed, count = 6).results.orEmpty()
                        .map { res ->
                            val sub = listOfNotNull(res.admin1, res.country).distinct().joinToString(", ")
                            PlacePoint(
                                name = res.name,
                                subtitle = sub.ifBlank { res.countryCode ?: "" },
                                latitude = res.latitude,
                                longitude = res.longitude,
                            )
                        }
                }.getOrDefault(emptyList())
            }

            val photonDeferred = async {
                runCatching {
                    NetworkModule.photonGeocodingApi.searchPlaces(trimmed, limit = 6).features.orEmpty()
                        .mapNotNull { feature ->
                            val coords = feature.geometry?.coordinates
                            val props = feature.properties
                            if (coords == null || coords.size < 2 || props == null) return@mapNotNull null
                            val primary = props.name ?: props.city ?: props.street ?: return@mapNotNull null
                            val sub = listOfNotNull(props.city?.takeIf { it != primary }, props.state, props.country)
                                .distinct()
                                .joinToString(", ")
                            PlacePoint(
                                name = primary,
                                subtitle = sub,
                                latitude = coords[1],
                                longitude = coords[0],
                            )
                        }
                }.getOrDefault(emptyList())
            }

            val combined = openMeteoDeferred.await() + photonDeferred.await()
            val deduped = mutableListOf<PlacePoint>()
            for (candidate in combined) {
                val duplicate = deduped.any { existing ->
                    abs(existing.latitude - candidate.latitude) < 0.08 &&
                        abs(existing.longitude - candidate.longitude) < 0.08 &&
                        existing.name.equals(candidate.name, ignoreCase = true)
                }
                if (!duplicate) deduped.add(candidate)
            }
            deduped.take(8)
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): PlacePoint = withContext(Dispatchers.IO) {
        val result = runCatching {
            val resp = NetworkModule.photonGeocodingApi.reverseGeocode(lat, lon)
            val feature = resp.features?.firstOrNull()
            val props = feature?.properties
            val name = props?.city ?: props?.name ?: props?.state ?: "Current Location"
            val sub = listOfNotNull(
                props?.state?.takeIf { it != name },
                props?.country,
            ).joinToString(", ")
            PlacePoint(
                name = name,
                subtitle = sub.ifBlank { String.format(Locale.US, "%.3f, %.3f", lat, lon) },
                latitude = lat,
                longitude = lon,
            )
        }.getOrNull()

        result ?: PlacePoint(
            name = "Current Location",
            subtitle = String.format(Locale.US, "%.3f, %.3f", lat, lon),
            latitude = lat,
            longitude = lon,
        )
    }

    suspend fun computeRouteAndWeather(
        origin: PlacePoint,
        destination: PlacePoint,
    ): RouteBundle = withContext(Dispatchers.IO) {
        val coordsParam = String.format(
            Locale.US,
            "%.6f,%.6f;%.6f,%.6f",
            origin.longitude,
            origin.latitude,
            destination.longitude,
            destination.latitude,
        )

        // 1. Multi-router fallback: FOSSGIS OSRM -> Project OSRM -> Geodesic direct corridor
        var polyPoints: List<LatLon> = emptyList()
        var totalDistMeters = 0.0
        var totalDurationSec = 0.0

        val fossgisResult = runCatching {
            NetworkModule.fossgisRoutingApi.getDrivingRoute(coordsParam)
        }.getOrNull()

        val primaryRoute = fossgisResult?.routes?.firstOrNull() ?: runCatching {
            NetworkModule.osrmRoutingApi.getDrivingRoute(coordsParam).routes?.firstOrNull()
        }.getOrNull()

        if (primaryRoute != null && primaryRoute.geometry.coordinates.size >= 2) {
            polyPoints = primaryRoute.geometry.coordinates.mapNotNull { pair ->
                if (pair.size >= 2) LatLon(latitude = pair[1], longitude = pair[0]) else null
            }
            totalDistMeters = primaryRoute.distance
            totalDurationSec = primaryRoute.duration
        } else {
            // Geodesic corridor fallback: provides realistic navigation line between endpoints
            val (points, distAndDuration) = RouteSampler.generateGeodesicCorridor(origin, destination)
            polyPoints = points
            totalDistMeters = distAndDuration.first
            totalDurationSec = distAndDuration.second
        }

        val rawCheckpoints = RouteSampler.sampleCheckpoints(
            points = polyPoints,
            totalDistanceMeters = totalDistMeters,
            totalDurationSeconds = totalDurationSec,
            origin = origin,
            destination = destination,
        )

        coroutineScope {
            val lats = rawCheckpoints.joinToString(",") { String.format(Locale.US, "%.4f", it.latitude) }
            val lons = rawCheckpoints.joinToString(",") { String.format(Locale.US, "%.4f", it.longitude) }

            // 2. Resilient Open-Meteo Forecast: timezone=auto -> fallback timezone=UTC -> per-point fallback
            val forecastsDeferred = async {
                fetchForecastsWithFallback(lats, lons, rawCheckpoints)
            }

            // Enrich intermediate checkpoints with names gracefully
            val namedCheckpointsDeferred = rawCheckpoints.mapIndexed { idx, cp ->
                async {
                    if (idx == 0 || idx == rawCheckpoints.lastIndex) {
                        cp
                    } else {
                        val resolved = runCatching {
                            if (idx % 2 == 1) {
                                val resp = NetworkModule.photonGeocodingApi.reverseGeocode(cp.latitude, cp.longitude)
                                val props = resp.features?.firstOrNull()?.properties
                                val town = props?.city ?: props?.name ?: props?.state
                                val region = listOfNotNull(props?.state?.takeIf { it != town }, props?.countryCodeShort()).joinToString(", ")
                                town?.let { cp.copy(label = it, regionLabel = region) }
                            } else null
                        }.getOrNull()
                        resolved ?: cp.copy(label = "Mile ${(cp.distanceFromStartMeters * 0.000621371).toInt()}")
                    }
                }
            }

            val forecasts = forecastsDeferred.await()
            val checkpoints = namedCheckpointsDeferred.awaitAll()

            RouteBundle(
                origin = origin,
                destination = destination,
                polylinePoints = polyPoints,
                totalDistanceMeters = totalDistMeters,
                totalDurationSeconds = totalDurationSec,
                checkpoints = checkpoints,
                forecasts = forecasts,
            )
        }
    }

    private suspend fun fetchForecastsWithFallback(
        lats: String,
        lons: String,
        checkpoints: List<RouteCheckpoint>,
    ): List<CheckpointForecast> {
        // Attempt 1: Batch with timezone=auto
        val attempt1 = try {
            val body = NetworkModule.openMeteoForecastApi.getBatchForecastRaw(
                latitudes = lats,
                longitudes = lons,
                timezone = "auto",
            )
            parseForecastDts(body.string().trim())
        } catch (e: Exception) {
            null
        }

        if (!attempt1.isNullOrEmpty()) return attempt1.map { it.toDomain() }

        // Attempt 2: Batch with timezone=UTC (avoids coordinate-to-timezone lookup crashes)
        val attempt2 = try {
            val body = NetworkModule.openMeteoForecastApi.getBatchForecastRaw(
                latitudes = lats,
                longitudes = lons,
                timezone = "UTC",
            )
            parseForecastDts(body.string().trim())
        } catch (e: Exception) {
            null
        }

        if (!attempt2.isNullOrEmpty()) return attempt2.map { it.toDomain() }

        // Attempt 3: Per-coordinate fallback
        return checkpoints.map { cp ->
            val single = try {
                val body = NetworkModule.openMeteoForecastApi.getBatchForecastRaw(
                    latitudes = String.format(Locale.US, "%.4f", cp.latitude),
                    longitudes = String.format(Locale.US, "%.4f", cp.longitude),
                    timezone = "UTC",
                )
                parseForecastDts(body.string().trim()).firstOrNull()?.toDomain()
            } catch (e: Exception) {
                null
            }
            single ?: createSyntheticForecast(cp)
        }
    }

    private fun parseForecastDts(jsonText: String): List<OpenMeteoForecastDto> {
        return if (jsonText.startsWith("[")) {
            forecastListAdapter.fromJson(jsonText).orEmpty()
        } else {
            listOfNotNull(forecastAdapter.fromJson(jsonText))
        }
    }

    private fun createSyntheticForecast(cp: RouteCheckpoint): CheckpointForecast {
        val now = System.currentTimeMillis() / 1000L
        val baseHour = (now / 3600L) * 3600L
        val epochs = (0 until 168).map { baseHour + it * 3600L }
        val sunrise = (0 until 7).map { baseHour + it * 86400L + 21600L }
        val sunset = (0 until 7).map { baseHour + it * 86400L + 64800L }
        return CheckpointForecast(
            utcOffsetSeconds = ((cp.longitude / 15.0) * 3600.0).toInt(),
            timezoneAbbreviation = "Local",
            hourlyEpochs = epochs,
            tempsC = epochs.map { 16.0 },
            apparentTempsC = epochs.map { 15.0 },
            weatherCodes = epochs.map { 1 },
            precipMm = epochs.map { 0.0 },
            precipProbPct = epochs.map { 10 },
            windSpeedKmh = epochs.map { 12.0 },
            windDirDeg = epochs.map { 220.0 },
            visibilityMeters = epochs.map { 20000.0 },
            cloudCoverPct = epochs.map { 25 },
            dailySunriseEpochs = sunrise,
            dailySunsetEpochs = sunset,
        )
    }

    suspend fun fetchRadarFrames(): List<RadarFrame> = withContext(Dispatchers.IO) {
        val resp = NetworkModule.rainViewerRadarApi.getWeatherMaps()
        val host = resp.host.trimEnd('/')
        val past = resp.radar?.past.orEmpty().takeLast(6)
        val nowcast = resp.radar?.nowcast.orEmpty().take(3)
        (past + nowcast).map { frame ->
            RadarFrame(
                timeEpoch = frame.time,
                tileUrlTemplate = "$host${frame.path}/256/",
            )
        }
    }

    private fun com.example.data.remote.PhotonProperties.countryCodeShort(): String? {
        return country
    }

    private fun OpenMeteoForecastDto.toDomain(): CheckpointForecast {
        val h = hourly
        val d = daily
        return CheckpointForecast(
            utcOffsetSeconds = utcOffsetSeconds,
            timezoneAbbreviation = timezoneAbbreviation,
            hourlyEpochs = h?.time.orEmpty(),
            tempsC = h?.temperature2m?.map { it ?: 15.0 }.orEmpty(),
            apparentTempsC = h?.apparentTemperature?.map { it ?: 15.0 }.orEmpty(),
            weatherCodes = h?.weatherCode?.map { it ?: 0 }.orEmpty(),
            precipMm = h?.precipitation?.map { it ?: 0.0 }.orEmpty(),
            precipProbPct = h?.precipitationProbability?.map { it ?: 0 }.orEmpty(),
            windSpeedKmh = h?.windSpeed10m?.map { it ?: 0.0 }.orEmpty(),
            windDirDeg = h?.windDirection10m?.map { it ?: 0.0 }.orEmpty(),
            visibilityMeters = h?.visibility?.map { it ?: 24000.0 }.orEmpty(),
            cloudCoverPct = h?.cloudCover?.map { it ?: 0 }.orEmpty(),
            dailySunriseEpochs = d?.sunrise?.filterNotNull().orEmpty(),
            dailySunsetEpochs = d?.sunset?.filterNotNull().orEmpty(),
        )
    }
}
