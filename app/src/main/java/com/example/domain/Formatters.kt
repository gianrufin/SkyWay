package com.example.domain

import com.example.model.UnitSystem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

object Formatters {

    fun formatTemp(tempC: Double, units: UnitSystem): String {
        val value = when (units) {
            UnitSystem.METRIC -> tempC.roundToInt()
            UnitSystem.IMPERIAL -> ((tempC * 9.0 / 5.0) + 32.0).roundToInt()
        }
        return "${value}°"
    }

    fun formatTempWithUnit(tempC: Double, units: UnitSystem): String {
        val value = when (units) {
            UnitSystem.METRIC -> tempC.roundToInt()
            UnitSystem.IMPERIAL -> ((tempC * 9.0 / 5.0) + 32.0).roundToInt()
        }
        val suffix = if (units == UnitSystem.METRIC) "C" else "F"
        return "${value}°$suffix"
    }

    fun formatWind(windKmh: Double, units: UnitSystem): String {
        return when (units) {
            UnitSystem.METRIC -> "${windKmh.roundToInt()} km/h"
            UnitSystem.IMPERIAL -> "${(windKmh * 0.621371).roundToInt()} mph"
        }
    }

    fun formatDistance(meters: Double, units: UnitSystem): String {
        return when (units) {
            UnitSystem.METRIC -> {
                val km = meters / 1000.0
                if (km >= 100) "${km.roundToInt()} km" else String.format(Locale.US, "%.1f km", km)
            }
            UnitSystem.IMPERIAL -> {
                val mi = meters * 0.000621371
                if (mi >= 100) "${mi.roundToInt()} mi" else String.format(Locale.US, "%.1f mi", mi)
            }
        }
    }

    fun formatVisibility(meters: Double, units: UnitSystem): String {
        return when (units) {
            UnitSystem.METRIC -> {
                val km = (meters / 1000.0).coerceAtLeast(0.1)
                if (km >= 10.0) "${km.roundToInt()} km" else String.format(Locale.US, "%.1f km", km)
            }
            UnitSystem.IMPERIAL -> {
                val mi = (meters * 0.000621371).coerceAtLeast(0.1)
                if (mi >= 10.0) "${mi.roundToInt()} mi" else String.format(Locale.US, "%.1f mi", mi)
            }
        }
    }

    fun formatPrecip(precipMm: Double, units: UnitSystem): String {
        return when (units) {
            UnitSystem.METRIC -> String.format(Locale.US, "%.1f mm", precipMm)
            UnitSystem.IMPERIAL -> String.format(Locale.US, "%.2f in", precipMm * 0.0393701)
        }
    }

    fun formatDuration(seconds: Double): String {
        val totalMinutes = (seconds / 60.0).roundToInt().coerceAtLeast(1)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun formatLocalTime(epochSeconds: Long, utcOffsetSeconds: Int? = null): String {
        val sdf = SimpleDateFormat("h:mm a", Locale.US)
        if (utcOffsetSeconds != null) {
            val shiftedMillis = (epochSeconds + utcOffsetSeconds) * 1000L
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date(shiftedMillis))
        }
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(epochSeconds * 1000L))
    }

    fun formatDayAndTime(epochSeconds: Long, utcOffsetSeconds: Int? = null): String {
        val sdf = SimpleDateFormat("EEE, h:mm a", Locale.US)
        if (utcOffsetSeconds != null) {
            val shiftedMillis = (epochSeconds + utcOffsetSeconds) * 1000L
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date(shiftedMillis))
        }
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(epochSeconds * 1000L))
    }

    fun formatCardinalDirection(degrees: Double): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val idx = (((degrees % 360.0) + 360.0) % 360.0 / 45.0).roundToInt() % 8
        return dirs[idx]
    }
}
