package com.example.domain

import com.example.model.CheckpointForecast
import com.example.model.DepartureWindowSlot
import com.example.model.EvaluatedCheckpoint
import com.example.model.EvaluatedRoute
import com.example.model.GlareRisk
import com.example.model.RouteBundle
import com.example.model.SolarBoundaryMarker
import com.example.model.WeatherCondition
import com.example.model.WeatherSeverity
import kotlin.math.roundToInt

object RouteEvaluator {

    fun evaluateRoute(
        bundle: RouteBundle,
        departureEpochSeconds: Long,
        baseNowEpochSeconds: Long,
    ): EvaluatedRoute {
        val evaluatedCheckpoints = mutableListOf<EvaluatedCheckpoint>()
        val solarMarkers = mutableListOf<SolarBoundaryMarker>()

        var prevPassByEpoch = departureEpochSeconds

        bundle.checkpoints.forEachIndexed { idx, cp ->
            val forecast = bundle.forecasts.getOrNull(idx) ?: bundle.forecasts.first()
            val passByEpoch = departureEpochSeconds + cp.durationFromStartSeconds.toLong()

            val sample = sampleForecastAt(forecast, passByEpoch)
            val condition = WeatherCondition.fromWmoCode(sample.weatherCode)

            val solarState = SolarCalculator.computeSolarState(
                epochSeconds = passByEpoch,
                latitudeDeg = cp.latitude,
                longitudeDeg = cp.longitude,
                travelBearingDeg = cp.bearingDegrees,
                cloudCoverPct = sample.cloudCoverPct,
            )

            val severity = computeSeverity(
                condition = condition,
                precipMm = sample.precipMm,
                precipProbPct = sample.precipProbPct,
                windSpeedKmh = sample.windSpeedKmh,
                visibilityMeters = sample.visibilityMeters,
                glareRisk = solarState.glareRisk,
            )

            val hazardSummary = buildHazardSummary(
                condition = condition,
                precipProbPct = sample.precipProbPct,
                windSpeedKmh = sample.windSpeedKmh,
                visibilityMeters = sample.visibilityMeters,
                glareRisk = solarState.glareRisk,
            )

            if (idx > 0) {
                val crossing = SolarCalculator.findSolarBoundaryCrossing(
                    startEpoch = prevPassByEpoch,
                    endEpoch = passByEpoch,
                    sunriseEpochs = forecast.dailySunriseEpochs,
                    sunsetEpochs = forecast.dailySunsetEpochs,
                )
                if (crossing != null) {
                    solarMarkers.add(
                        SolarBoundaryMarker(
                            type = crossing.first,
                            eventEpochSeconds = crossing.second,
                            utcOffsetSeconds = forecast.utcOffsetSeconds,
                            afterCheckpointIndex = idx - 1,
                            locationHint = cp.label,
                        )
                    )
                }
            }
            prevPassByEpoch = passByEpoch

            evaluatedCheckpoints.add(
                EvaluatedCheckpoint(
                    checkpoint = cp,
                    passByEpochSeconds = passByEpoch,
                    utcOffsetSeconds = forecast.utcOffsetSeconds,
                    timezoneAbbreviation = forecast.timezoneAbbreviation,
                    temperatureC = sample.tempC,
                    apparentTempC = sample.apparentTempC,
                    condition = condition,
                    precipMm = sample.precipMm,
                    precipProbPct = sample.precipProbPct,
                    windSpeedKmh = sample.windSpeedKmh,
                    windDirDeg = sample.windDirDeg,
                    visibilityMeters = sample.visibilityMeters,
                    cloudCoverPct = sample.cloudCoverPct,
                    solarState = solarState,
                    severity = severity,
                    hazardSummary = hazardSummary,
                )
            )
        }

        val overallSeverity = when {
            evaluatedCheckpoints.any { it.severity == WeatherSeverity.HAZARD } -> WeatherSeverity.HAZARD
            evaluatedCheckpoints.any { it.severity == WeatherSeverity.CAUTION } -> WeatherSeverity.CAUTION
            else -> WeatherSeverity.CLEAR
        }

        val (bestOffset, bestScore) = findBestClearWindow(bundle, baseNowEpochSeconds)

        return EvaluatedRoute(
            bundle = bundle,
            departureEpochSeconds = departureEpochSeconds,
            arrivalEpochSeconds = departureEpochSeconds + bundle.totalDurationSeconds.toLong(),
            checkpoints = evaluatedCheckpoints,
            solarMarkers = solarMarkers,
            overallSeverity = overallSeverity,
            bestWindowOffsetMinutes = bestOffset,
            bestWindowScore = bestScore,
        )
    }

    fun computeDepartureSlots(
        bundle: RouteBundle,
        baseNowEpochSeconds: Long,
        maxHours: Int = 72,
    ): List<DepartureWindowSlot> {
        val slots = mutableListOf<DepartureWindowSlot>()
        for (hour in 0..maxHours step 2) {
            val offsetMin = hour * 60
            val depEpoch = baseNowEpochSeconds + offsetMin * 60L
            val (severity, score) = scoreDeparture(bundle, depEpoch)
            slots.add(DepartureWindowSlot(offsetMinutes = offsetMin, severity = severity, score = score))
        }
        return slots
    }

    private fun findBestClearWindow(
        bundle: RouteBundle,
        baseNowEpochSeconds: Long,
    ): Pair<Int?, Double> {
        var bestOffset = 0
        var bestScore = Double.MAX_VALUE
        // Search next 48 hours in 1-hour steps
        for (hour in 0..48) {
            val offsetMin = hour * 60
            val depEpoch = baseNowEpochSeconds + offsetMin * 60L
            val (_, score) = scoreDeparture(bundle, depEpoch)
            if (score < bestScore - 0.15) {
                bestScore = score
                bestOffset = offsetMin
            }
        }
        return bestOffset to bestScore
    }

    private fun scoreDeparture(bundle: RouteBundle, depEpoch: Long): Pair<WeatherSeverity, Double> {
        var totalScore = 0.0
        var worst = WeatherSeverity.CLEAR

        bundle.checkpoints.forEachIndexed { idx, cp ->
            val forecast = bundle.forecasts.getOrNull(idx) ?: bundle.forecasts.first()
            val passBy = depEpoch + cp.durationFromStartSeconds.toLong()
            val s = sampleForecastAt(forecast, passBy)
            val cond = WeatherCondition.fromWmoCode(s.weatherCode)
            val solar = SolarCalculator.computeSolarState(
                epochSeconds = passBy,
                latitudeDeg = cp.latitude,
                longitudeDeg = cp.longitude,
                travelBearingDeg = cp.bearingDegrees,
                cloudCoverPct = s.cloudCoverPct,
            )
            val sev = computeSeverity(
                condition = cond,
                precipMm = s.precipMm,
                precipProbPct = s.precipProbPct,
                windSpeedKmh = s.windSpeedKmh,
                visibilityMeters = s.visibilityMeters,
                glareRisk = solar.glareRisk,
            )
            if (sev.ordinal > worst.ordinal) worst = sev

            val legPenalty = (s.precipMm * 14.0) +
                (s.precipProbPct * 0.25) +
                ((s.windSpeedKmh - 20.0).coerceAtLeast(0.0) * 0.6) +
                (if (s.visibilityMeters < 3000.0) 15.0 else 0.0) +
                when (solar.glareRisk) {
                    GlareRisk.SEVERE -> 9.0
                    GlareRisk.MODERATE -> 3.0
                    GlareRisk.NONE -> 0.0
                } +
                when (cond) {
                    WeatherCondition.THUNDERSTORM -> 35.0
                    WeatherCondition.HEAVY_RAIN, WeatherCondition.SNOW -> 25.0
                    WeatherCondition.FOG -> 18.0
                    WeatherCondition.RAIN -> 12.0
                    else -> 0.0
                }
            totalScore += legPenalty
        }
        val avgScore = totalScore / bundle.checkpoints.size.coerceAtLeast(1)
        return worst to avgScore
    }

    private data class InterpolatedSample(
        val tempC: Double,
        val apparentTempC: Double,
        val weatherCode: Int,
        val precipMm: Double,
        val precipProbPct: Int,
        val windSpeedKmh: Double,
        val windDirDeg: Double,
        val visibilityMeters: Double,
        val cloudCoverPct: Int,
    )

    private fun sampleForecastAt(forecast: CheckpointForecast, targetEpoch: Long): InterpolatedSample {
        val epochs = forecast.hourlyEpochs
        if (epochs.isEmpty()) {
            return InterpolatedSample(15.0, 15.0, 0, 0.0, 0, 10.0, 180.0, 24000.0, 10)
        }
        if (targetEpoch <= epochs.first()) {
            return indexSample(forecast, 0)
        }
        if (targetEpoch >= epochs.last()) {
            return indexSample(forecast, epochs.lastIndex)
        }

        // Binary search insertion point
        var lo = 0
        var hi = epochs.lastIndex
        while (lo + 1 < hi) {
            val mid = (lo + hi) / 2
            if (epochs[mid] <= targetEpoch) lo = mid else hi = mid
        }

        val t0 = epochs[lo]
        val t1 = epochs[hi]
        val span = (t1 - t0).coerceAtLeast(1L).toDouble()
        val f = ((targetEpoch - t0) / span).coerceIn(0.0, 1.0)

        fun lerp(list: List<Double>, default: Double): Double {
            val a = list.getOrElse(lo) { default }
            val b = list.getOrElse(hi) { a }
            return a + (b - a) * f
        }

        fun lerpInt(list: List<Int>, default: Int): Int {
            val a = list.getOrElse(lo) { default }
            val b = list.getOrElse(hi) { a }
            return (a + (b - a) * f).roundToInt()
        }

        // Interpolate circular wind direction smoothly
        val dirA = forecast.windDirDeg.getOrElse(lo) { 0.0 }
        val dirB = forecast.windDirDeg.getOrElse(hi) { dirA }
        var dirDelta = (dirB - dirA) % 360.0
        if (dirDelta > 180.0) dirDelta -= 360.0
        if (dirDelta < -180.0) dirDelta += 360.0
        val windDir = SolarCalculator.normalizeDeg(dirA + dirDelta * f)

        val codeIdx = if (f < 0.5) lo else hi
        return InterpolatedSample(
            tempC = lerp(forecast.tempsC, 15.0),
            apparentTempC = lerp(forecast.apparentTempsC, 15.0),
            weatherCode = forecast.weatherCodes.getOrElse(codeIdx) { 0 },
            precipMm = lerp(forecast.precipMm, 0.0),
            precipProbPct = lerpInt(forecast.precipProbPct, 0),
            windSpeedKmh = lerp(forecast.windSpeedKmh, 10.0),
            windDirDeg = windDir,
            visibilityMeters = lerp(forecast.visibilityMeters, 24000.0),
            cloudCoverPct = lerpInt(forecast.cloudCoverPct, 20),
        )
    }

    private fun indexSample(forecast: CheckpointForecast, idx: Int) = InterpolatedSample(
        tempC = forecast.tempsC.getOrElse(idx) { 15.0 },
        apparentTempC = forecast.apparentTempsC.getOrElse(idx) { 15.0 },
        weatherCode = forecast.weatherCodes.getOrElse(idx) { 0 },
        precipMm = forecast.precipMm.getOrElse(idx) { 0.0 },
        precipProbPct = forecast.precipProbPct.getOrElse(idx) { 0 },
        windSpeedKmh = forecast.windSpeedKmh.getOrElse(idx) { 10.0 },
        windDirDeg = forecast.windDirDeg.getOrElse(idx) { 180.0 },
        visibilityMeters = forecast.visibilityMeters.getOrElse(idx) { 24000.0 },
        cloudCoverPct = forecast.cloudCoverPct.getOrElse(idx) { 15 },
    )

    private fun computeSeverity(
        condition: WeatherCondition,
        precipMm: Double,
        precipProbPct: Int,
        windSpeedKmh: Double,
        visibilityMeters: Double,
        glareRisk: GlareRisk,
    ): WeatherSeverity {
        if (condition == WeatherCondition.THUNDERSTORM ||
            condition == WeatherCondition.HEAVY_RAIN ||
            condition == WeatherCondition.SNOW ||
            precipMm >= 2.5 ||
            windSpeedKmh >= 48.0 ||
            visibilityMeters < 1500.0
        ) {
            return WeatherSeverity.HAZARD
        }
        if (condition == WeatherCondition.RAIN ||
            condition == WeatherCondition.FOG ||
            condition == WeatherCondition.DRIZZLE ||
            precipProbPct >= 45 ||
            windSpeedKmh >= 32.0 ||
            visibilityMeters < 5000.0 ||
            glareRisk == GlareRisk.SEVERE
        ) {
            return WeatherSeverity.CAUTION
        }
        return WeatherSeverity.CLEAR
    }

    private fun buildHazardSummary(
        condition: WeatherCondition,
        precipProbPct: Int,
        windSpeedKmh: Double,
        visibilityMeters: Double,
        glareRisk: GlareRisk,
    ): String {
        val parts = mutableListOf<String>()
        if (glareRisk == GlareRisk.SEVERE) parts.add("Direct sun glare")
        else if (glareRisk == GlareRisk.MODERATE) parts.add("Low sun angle")
        if (visibilityMeters < 3000.0) parts.add("Low visibility")
        if (windSpeedKmh >= 35.0) parts.add("Gusty crosswinds")
        if (precipProbPct >= 40) parts.add("${precipProbPct}% precip")
        return if (parts.isEmpty()) condition.label else parts.joinToString(" · ")
    }
}
