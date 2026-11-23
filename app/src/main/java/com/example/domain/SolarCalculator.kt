package com.example.domain

import com.example.model.GlareRisk
import com.example.model.SolarEventType
import com.example.model.SolarState
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object SolarCalculator {

    private fun toRadians(deg: Double): Double = deg * PI / 180.0
    private fun toDegrees(rad: Double): Double = rad * 180.0 / PI

    fun computeSolarState(
        epochSeconds: Long,
        latitudeDeg: Double,
        longitudeDeg: Double,
        travelBearingDeg: Double,
        cloudCoverPct: Int,
    ): SolarState {
        val jd = epochSeconds / 86400.0 + 2440587.5
        val n = jd - 2451545.0

        val meanLonDeg = normalizeDeg(280.460 + 0.9856474 * n)
        val meanAnomalyRad = toRadians(normalizeDeg(357.528 + 0.9856003 * n))
        val eclipticLonRad = toRadians(
            meanLonDeg + 1.915 * sin(meanAnomalyRad) + 0.020 * sin(2.0 * meanAnomalyRad)
        )
        val obliquityRad = toRadians(23.439 - 0.0000004 * n)

        val declinationRad = asin(sin(obliquityRad) * sin(eclipticLonRad))
        val raRad = atan2(cos(obliquityRad) * sin(eclipticLonRad), cos(eclipticLonRad))
        val raDeg = normalizeDeg(toDegrees(raRad))

        val gmstDeg = normalizeDeg(280.46061837 + 360.98564736629 * n)
        val lhaRad = toRadians(gmstDeg + longitudeDeg - raDeg)

        val latRad = toRadians(latitudeDeg.coerceIn(-89.9, 89.9))
        val sinAlt = (sin(latRad) * sin(declinationRad) +
            cos(latRad) * cos(declinationRad) * cos(lhaRad)).coerceIn(-1.0, 1.0)
        val altRad = asin(sinAlt)
        val elevationDeg = toDegrees(altRad)

        val cosAlt = cos(altRad).coerceAtLeast(1e-6)
        val cosAz = ((sin(declinationRad) - sin(latRad) * sinAlt) / (cos(latRad) * cosAlt))
            .coerceIn(-1.0, 1.0)
        val sinAz = (-cos(declinationRad) * sin(lhaRad) / cosAlt).coerceIn(-1.0, 1.0)
        val azimuthDeg = normalizeDeg(toDegrees(atan2(sinAz, cosAz)))

        val relAngle = angularDifferenceDeg(azimuthDeg, travelBearingDeg)
        val isDaylight = elevationDeg > -0.8

        val glareRisk = when {
            elevationDeg < 0.5 || elevationDeg > 24.0 -> GlareRisk.NONE
            cloudCoverPct >= 85 -> GlareRisk.NONE
            relAngle <= 25.0 && elevationDeg <= 18.0 && cloudCoverPct < 60 -> GlareRisk.SEVERE
            relAngle <= 42.0 && elevationDeg <= 24.0 -> GlareRisk.MODERATE
            else -> GlareRisk.NONE
        }

        return SolarState(
            elevationDeg = elevationDeg,
            azimuthDeg = azimuthDeg,
            relativeAngleDeg = relAngle,
            glareRisk = glareRisk,
            isDaylight = isDaylight,
        )
    }

    fun findSolarBoundaryCrossing(
        startEpoch: Long,
        endEpoch: Long,
        sunriseEpochs: List<Long>,
        sunsetEpochs: List<Long>,
    ): Pair<SolarEventType, Long>? {
        if (endEpoch <= startEpoch) return null
        for (sr in sunriseEpochs) {
            if (sr in (startEpoch + 1)..endEpoch) {
                return SolarEventType.SUNRISE to sr
            }
        }
        for (ss in sunsetEpochs) {
            if (ss in (startEpoch + 1)..endEpoch) {
                return SolarEventType.SUNSET to ss
            }
        }
        return null
    }

    fun normalizeDeg(deg: Double): Double {
        val m = deg % 360.0
        return if (m < 0.0) m + 360.0 else m
    }

    fun angularDifferenceDeg(a: Double, b: Double): Double {
        val diff = abs(normalizeDeg(a) - normalizeDeg(b))
        return if (diff > 180.0) 360.0 - diff else diff
    }
}
