package com.example

import com.example.domain.RouteEvaluator
import com.example.domain.RouteSampler
import com.example.domain.SolarCalculator
import com.example.model.CheckpointForecast
import com.example.model.GlareRisk
import com.example.model.LatLon
import com.example.model.PlacePoint
import com.example.model.RouteBundle
import com.example.model.SolarEventType
import com.example.model.WeatherSeverity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteWeatherEngineTest {

    @Test
    fun solarCalculator_detectsSevereGlareWhenDrivingIntoLowMorningSun() {
        // March 20 equinox ~07:00 local in San Francisco (UTC-7 -> 14:00 UTC)
        // 2026-03-20T14:30:00Z = 1774017000
        val epoch = 1774017000L
        val lat = 37.7749
        val lon = -122.4194

        val eastbound = SolarCalculator.computeSolarState(
            epochSeconds = epoch,
            latitudeDeg = lat,
            longitudeDeg = lon,
            travelBearingDeg = 95.0, // Heading East toward rising sun
            cloudCoverPct = 10,
        )
        assertTrue("Expected daylight elevation > 0, got ${eastbound.elevationDeg}", eastbound.elevationDeg > 0.0)
        assertEquals(GlareRisk.SEVERE, eastbound.glareRisk)

        val westbound = SolarCalculator.computeSolarState(
            epochSeconds = epoch,
            latitudeDeg = lat,
            longitudeDeg = lon,
            travelBearingDeg = 275.0, // Heading West away from rising sun
            cloudCoverPct = 10,
        )
        assertEquals(GlareRisk.NONE, westbound.glareRisk)
    }

    @Test
    fun routeSampler_samplesOriginIntermediateAndDestinationCheckpoints() {
        val origin = PlacePoint("San Francisco", "CA", 37.7749, -122.4194)
        val dest = PlacePoint("Sacramento", "CA", 38.5816, -121.4944)
        val poly = listOf(
            LatLon(37.7749, -122.4194),
            LatLon(38.0194, -122.1341),
            LatLon(38.2494, -122.0399),
            LatLon(38.5816, -121.4944),
        )
        val checkpoints = RouteSampler.sampleCheckpoints(
            points = poly,
            totalDistanceMeters = 140_000.0,
            totalDurationSeconds = 5_400.0, // 1.5h
            origin = origin,
            destination = dest,
        )
        assertTrue(checkpoints.size >= 6)
        assertEquals("San Francisco", checkpoints.first().label)
        assertEquals("Sacramento", checkpoints.last().label)
        assertEquals(0.0, checkpoints.first().durationFromStartSeconds, 0.1)
        assertEquals(5400.0, checkpoints.last().durationFromStartSeconds, 0.1)
    }

    @Test
    fun routeEvaluator_interpolatesPassByWeatherAndFindsBestClearWindow() {
        val origin = PlacePoint("Origin", "Test", 37.77, -122.42)
        val dest = PlacePoint("Destination", "Test", 38.58, -121.49)
        val poly = listOf(LatLon(37.77, -122.42), LatLon(38.58, -121.49))
        val cps = RouteSampler.sampleCheckpoints(poly, 120_000.0, 3_600.0, origin, dest)

        val baseEpoch = 1774000000L
        val epochs = (0 until 72).map { baseEpoch + it * 3600L }
        // Heavy rain for first 4 hours, clear sky from hour 6 onward
        val forecasts = cps.map {
            CheckpointForecast(
                utcOffsetSeconds = -25200,
                timezoneAbbreviation = "PDT",
                hourlyEpochs = epochs,
                tempsC = epochs.indices.map { idx -> 12.0 + idx * 0.1 },
                apparentTempsC = epochs.indices.map { idx -> 11.0 + idx * 0.1 },
                weatherCodes = epochs.indices.map { idx -> if (idx < 6) 65 else 0 },
                precipMm = epochs.indices.map { idx -> if (idx < 6) 6.0 else 0.0 },
                precipProbPct = epochs.indices.map { idx -> if (idx < 6) 90 else 5 },
                windSpeedKmh = epochs.indices.map { idx -> if (idx < 6) 40.0 else 10.0 },
                windDirDeg = epochs.indices.map { 270.0 },
                visibilityMeters = epochs.indices.map { idx -> if (idx < 6) 1200.0 else 24000.0 },
                cloudCoverPct = epochs.indices.map { idx -> if (idx < 6) 95 else 10 },
                dailySunriseEpochs = listOf(baseEpoch + 1800L),
                dailySunsetEpochs = listOf(baseEpoch + 45000L),
            )
        }

        val bundle = RouteBundle(origin, dest, poly, 120_000.0, 3_600.0, cps, forecasts)

        val immediate = RouteEvaluator.evaluateRoute(bundle, baseEpoch, baseEpoch)
        assertEquals(WeatherSeverity.HAZARD, immediate.overallSeverity)
        assertEquals(1, immediate.solarMarkers.size)
        assertEquals(SolarEventType.SUNRISE, immediate.solarMarkers.first().type)

        val bestOffset = immediate.bestWindowOffsetMinutes
        assertNotNull(bestOffset)
        assertTrue("Expected best clear window >= 360 min, got $bestOffset", bestOffset!! >= 360)

        val delayed = RouteEvaluator.evaluateRoute(bundle, baseEpoch + bestOffset * 60L, baseEpoch)
        assertEquals(WeatherSeverity.CLEAR, delayed.overallSeverity)
    }
}
