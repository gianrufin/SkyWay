package com.example.domain

import com.example.model.LatLon
import com.example.model.PlacePoint
import com.example.model.RouteCheckpoint
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object RouteSampler {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun haversineMeters(a: LatLon, b: LatLon): Double {
        val dLat = Math.toRadians(b.latitude - a.latitude)
        val dLon = Math.toRadians(b.longitude - a.longitude)
        val lat1 = Math.toRadians(a.latitude)
        val lat2 = Math.toRadians(b.latitude)
        val sinLat = sin(dLat / 2.0)
        val sinLon = sin(dLon / 2.0)
        val h = sinLat * sinLat + cos(lat1) * cos(lat2) * sinLon * sinLon
        return 2.0 * EARTH_RADIUS_METERS * atan2(sqrt(h), sqrt(1.0 - h))
    }

    fun bearingDegrees(from: LatLon, to: LatLon): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val deg = Math.toDegrees(atan2(y, x))
        return ((deg % 360.0) + 360.0) % 360.0
    }

    /**
     * Samples 6 to 11 evenly-spaced checkpoints along the polyline based on trip duration,
     * preserving exact Origin (index 0) and Destination (last index).
     */
    fun sampleCheckpoints(
        points: List<LatLon>,
        totalDistanceMeters: Double,
        totalDurationSeconds: Double,
        origin: PlacePoint,
        destination: PlacePoint,
    ): List<RouteCheckpoint> {
        if (points.size < 2) {
            val bearing = bearingDegrees(
                LatLon(origin.latitude, origin.longitude),
                LatLon(destination.latitude, destination.longitude),
            )
            return listOf(
                RouteCheckpoint(0, origin.latitude, origin.longitude, 0.0, 0.0, bearing, origin.name, origin.subtitle),
                RouteCheckpoint(1, destination.latitude, destination.longitude, totalDistanceMeters, totalDurationSeconds, bearing, destination.name, destination.subtitle),
            )
        }

        val cumulativeDistances = DoubleArray(points.size)
        var acc = 0.0
        for (i in 1 until points.size) {
            acc += haversineMeters(points[i - 1], points[i])
            cumulativeDistances[i] = acc
        }
        val polyTotalDist = acc.coerceAtLeast(1.0)

        // Target ~30 min per leg, clamped between 6 and 11 checkpoints total
        val segmentCount = (totalDurationSeconds / 1800.0).roundToInt().coerceIn(5, 10)
        val checkpoints = mutableListOf<RouteCheckpoint>()

        for (step in 0..segmentCount) {
            val fraction = step.toDouble() / segmentCount.toDouble()
            val targetPolyDist = polyTotalDist * fraction
            val idx = findIndexAtDistance(cumulativeDistances, targetPolyDist)
            val point = points[idx]

            val lookAheadIdx = (idx + 3).coerceAtMost(points.lastIndex)
            val lookBackIdx = (idx - 3).coerceAtLeast(0)
            val bearing = if (lookAheadIdx > lookBackIdx) {
                bearingDegrees(points[lookBackIdx], points[lookAheadIdx])
            } else {
                bearingDegrees(points.first(), points.last())
            }

            val label = when (step) {
                0 -> origin.name
                segmentCount -> destination.name
                else -> "Waypoint $step"
            }
            val region = when (step) {
                0 -> origin.subtitle
                segmentCount -> destination.subtitle
                else -> ""
            }

            checkpoints.add(
                RouteCheckpoint(
                    index = step,
                    latitude = point.latitude,
                    longitude = point.longitude,
                    distanceFromStartMeters = totalDistanceMeters * fraction,
                    durationFromStartSeconds = totalDurationSeconds * fraction,
                    bearingDegrees = bearing,
                    label = label,
                    regionLabel = region,
                )
            )
        }

        return checkpoints
    }

    private fun findIndexAtDistance(cumDist: DoubleArray, target: Double): Int {
        var low = 0
        var high = cumDist.lastIndex
        while (low < high) {
            val mid = (low + high) / 2
            if (cumDist[mid] < target) {
                low = mid + 1
            } else {
                high = mid
            }
        }
        return low
    }

    /**
     * Generates a realistic geodesic direct transit corridor between two points
     * as a reliable fallback when public road routing servers are unreachable or no road connects them.
     */
    fun generateGeodesicCorridor(
        origin: PlacePoint,
        destination: PlacePoint,
    ): Pair<List<LatLon>, Pair<Double, Double>> {
        val start = LatLon(origin.latitude, origin.longitude)
        val end = LatLon(destination.latitude, destination.longitude)
        val directDist = haversineMeters(start, end).coerceAtLeast(1000.0)
        val roadDist = directDist * 1.22
        val estDurationSec = (roadDist / 24.0).coerceAtLeast(600.0) // ~86 km/h avg

        val steps = 36
        val points = mutableListOf<LatLon>()
        for (i in 0..steps) {
            val f = i.toDouble() / steps.toDouble()
            val lat = origin.latitude + (destination.latitude - origin.latitude) * f
            val lon = origin.longitude + (destination.longitude - origin.longitude) * f
            points.add(LatLon(lat, lon))
        }
        return points to (roadDist to estDurationSec)
    }
}
