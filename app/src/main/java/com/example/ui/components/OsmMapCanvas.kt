package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.Formatters
import com.example.domain.RouteSampler
import com.example.model.EvaluatedCheckpoint
import com.example.model.EvaluatedRoute
import com.example.model.GlareRisk
import com.example.model.LatLon
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.example.model.MapStyle
import com.example.model.RadarFrame
import com.example.model.UnitSystem
import com.example.model.WeatherCondition
import com.example.model.WeatherSeverity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.tileprovider.modules.IFilesystemCache
import org.osmdroid.tileprovider.modules.TileWriter
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import java.io.File
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay
import kotlin.math.roundToInt
import android.graphics.Color as AndroidColor

@Composable
fun OsmMapCanvas(
    evaluatedRoute: EvaluatedRoute?,
    selectedCheckpointIndex: Int?,
    unitSystem: UnitSystem,
    mapStyle: MapStyle,
    activeRadarFrame: RadarFrame?,
    recenterNonce: Int,
    onCheckpointClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    val fileSystemCache: IFilesystemCache = remember(context) {
        val basePath = File(context.cacheDir, "osmdroid")
        val tileCache = File(basePath, "tiles")
        if (!tileCache.exists()) {
            tileCache.mkdirs()
        }
        val config = Configuration.getInstance()
        config.osmdroidBasePath = basePath
        config.osmdroidTileCache = tileCache
        config.load(
            context,
            context.applicationContext.getSharedPreferences("osmdroid_skyway", Context.MODE_PRIVATE),
        )
        config.userAgentValue = context.packageName

        // Configure osmdroid's FileSystemCache capacities & retention for offline resiliency:
        config.tileFileSystemCacheMaxBytes = 500L * 1024 * 1024 // 500 MB max disk quota
        config.tileFileSystemCacheTrimBytes = 420L * 1024 * 1024 // 420 MB trim threshold
        config.tileFileSystemThreads = 4
        config.expirationExtendedDuration = 1000L * 60 * 60 * 24 * 30L // 30-day extended retention for offline trips

        TileWriter()
    }

    val esriSatelliteTileSource = remember {
        object : OnlineTileSourceBase(
            "EsriSatellite",
            0,
            18,
            256,
            ".jpg",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val z = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "$baseUrl$z/$y/$x"
            }
        }
    }

    val darkCockpitColorFilter = remember {
        val matrix = ColorMatrix(
            floatArrayOf(
                -0.78f, 0f, 0f, 0f, 212f,
                0f, -0.76f, 0f, 0f, 222f,
                0f, 0f, -0.64f, 0f, 242f,
                0f, 0f, 0f, 1.0f, 0f,
            )
        )
        ColorMatrixColorFilter(matrix)
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var radarOverlayRef by remember { mutableStateOf<TilesOverlay?>(null) }
    var lastFittedRouteKey by remember { mutableStateOf<String?>(null) }
    var lastHandledRecenter by remember { mutableStateOf(-1) }

    DisposableEffect(lifecycleOwner, mapViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapViewRef?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapViewRef?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Pan to selected checkpoint smoothly
    LaunchedEffect(selectedCheckpointIndex, evaluatedRoute) {
        val map = mapViewRef ?: return@LaunchedEffect
        val route = evaluatedRoute ?: return@LaunchedEffect
        val idx = selectedCheckpointIndex ?: return@LaunchedEffect
        val cp = route.checkpoints.getOrNull(idx)?.checkpoint ?: return@LaunchedEffect
        map.controller.animateTo(GeoPoint(cp.latitude, cp.longitude))
    }

    // Pre-cache corridor tiles using osmdroid's CacheManager & FileSystemCache
    LaunchedEffect(evaluatedRoute?.bundle?.origin, evaluatedRoute?.bundle?.destination) {
        val map = mapViewRef ?: return@LaunchedEffect
        val route = evaluatedRoute ?: return@LaunchedEffect
        val pts = route.bundle.polylinePoints
        if (pts.size >= 2) {
            val geoPoints = ArrayList(pts.map { GeoPoint(it.latitude, it.longitude) })
            runCatching {
                val cacheManager = CacheManager(map, fileSystemCache)
                cacheManager.downloadAreaAsyncNoUI(
                    context,
                    geoPoints,
                    /* minZoom = */ 5,
                    /* maxZoom = */ 10,
                    object : CacheManager.CacheManagerCallback {
                        override fun onTaskComplete() {}
                        override fun updateProgress(progress: Int, current: Int, total: Int, errors: Int) {}
                        override fun downloadStarted() {}
                        override fun setPossibleTilesInArea(total: Int) {}
                        override fun onTaskFailed(errors: Int) {}
                    }
                )
            }
        }
    }

    // Fit full route bounding box when route loads or recenter pressed
    LaunchedEffect(evaluatedRoute?.bundle?.origin, evaluatedRoute?.bundle?.destination, recenterNonce) {
        val map = mapViewRef ?: return@LaunchedEffect
        val route = evaluatedRoute ?: return@LaunchedEffect
        val key = "${route.bundle.origin.name}_${route.bundle.destination.name}"
        if (key != lastFittedRouteKey || recenterNonce != lastHandledRecenter) {
            lastFittedRouteKey = key
            lastHandledRecenter = recenterNonce
            val pts = route.bundle.polylinePoints
            if (pts.size >= 2) {
                val minLat = pts.minOf { it.latitude }
                val maxLat = pts.maxOf { it.latitude }
                val minLon = pts.minOf { it.longitude }
                val maxLon = pts.maxOf { it.longitude }
                val latPad = ((maxLat - minLat) * 0.25).coerceAtLeast(0.08)
                val lonPad = ((maxLon - minLon) * 0.20).coerceAtLeast(0.08)
                val bbox = BoundingBox(
                    (maxLat + latPad).coerceAtMost(85.0),
                    (maxLon + lonPad).coerceAtMost(180.0),
                    (minLat - latPad * 1.45).coerceAtLeast(-85.0), // Extra bottom padding for glass console
                    (minLon - lonPad).coerceAtLeast(-180.0),
                )
                map.post {
                    runCatching { map.zoomToBoundingBox(bbox, true, 64) }
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            // Construct tile provider backed by osmdroid's FileSystemCache (TileWriter)
            val tileProvider = MapTileProviderBasic(ctx, TileSourceFactory.MAPNIK, fileSystemCache).apply {
                // Ensure offline-first: query filesystem disk cache first before hitting network
                setOfflineFirst(true)
            }
            MapView(ctx, tileProvider).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                overlayManager.tilesOverlay.setColorFilter(darkCockpitColorFilter)
                setMultiTouchControls(true)
                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                isTilesScaledToDpi = true
                minZoomLevel = 3.0
                maxZoomLevel = 18.0
                controller.setZoom(5.5)
                controller.setCenter(GeoPoint(38.5, -120.5))
                mapViewRef = this
            }
        },
        update = { mapView ->
            // Update base map tile source & styling without API keys
            when (mapStyle) {
                MapStyle.DARK -> {
                    if (mapView.tileProvider.tileSource != TileSourceFactory.MAPNIK) {
                        mapView.setTileSource(TileSourceFactory.MAPNIK)
                    }
                    mapView.overlayManager.tilesOverlay.setColorFilter(darkCockpitColorFilter)
                }
                MapStyle.TOPO -> {
                    if (mapView.tileProvider.tileSource != TileSourceFactory.OpenTopo) {
                        mapView.setTileSource(TileSourceFactory.OpenTopo)
                    }
                    mapView.overlayManager.tilesOverlay.setColorFilter(null)
                }
                MapStyle.SATELLITE -> {
                    if (mapView.tileProvider.tileSource != esriSatelliteTileSource) {
                        mapView.setTileSource(esriSatelliteTileSource)
                    }
                    mapView.overlayManager.tilesOverlay.setColorFilter(null)
                }
                MapStyle.STREET -> {
                    if (mapView.tileProvider.tileSource != TileSourceFactory.MAPNIK) {
                        mapView.setTileSource(TileSourceFactory.MAPNIK)
                    }
                    mapView.overlayManager.tilesOverlay.setColorFilter(null)
                }
            }

            // Clear existing secondary route overlays while keeping/rebuilding radar cleanly
            mapView.overlays.clear()

            // 1. Live RainViewer Doppler Radar Layer
            if (activeRadarFrame != null) {
                val radarSource = object : OnlineTileSourceBase(
                    "RainViewer_${activeRadarFrame.timeEpoch}",
                    0,
                    12,
                    256,
                    ".png",
                    arrayOf(activeRadarFrame.tileUrlTemplate),
                ) {
                    override fun getTileURLString(pMapTileIndex: Long): String {
                        val z = MapTileIndex.getZoom(pMapTileIndex)
                        val x = MapTileIndex.getX(pMapTileIndex)
                        val y = MapTileIndex.getY(pMapTileIndex)
                        return "${activeRadarFrame.tileUrlTemplate}$z/$x/$y/2/1_1.png"
                    }
                }
                val provider = MapTileProviderBasic(context, radarSource)
                val tilesOverlay = TilesOverlay(provider, context).apply {
                    loadingBackgroundColor = AndroidColor.TRANSPARENT
                    loadingLineColor = AndroidColor.TRANSPARENT
                }
                radarOverlayRef = tilesOverlay
                mapView.overlays.add(tilesOverlay)
            } else {
                radarOverlayRef = null
            }

            // 2. Multi-segment weather-colored route polyline + glass checkpoint pins
            if (evaluatedRoute != null) {
                val polyPts = evaluatedRoute.bundle.polylinePoints
                if (polyPts.size >= 2) {
                    // Ambient outer glow underlay
                    val haloLine = Polyline(mapView).apply {
                        setPoints(polyPts.map { GeoPoint(it.latitude, it.longitude) })
                        outlinePaint.color = AndroidColor.argb(95, 56, 189, 248)
                        outlinePaint.strokeWidth = 20f
                        outlinePaint.strokeCap = Paint.Cap.ROUND
                        outlinePaint.strokeJoin = Paint.Join.ROUND
                    }
                    mapView.overlays.add(haloLine)

                    // Colored segments mapped to nearest checkpoint severity
                    val segments = splitPolylineByCheckpoints(polyPts, evaluatedRoute.checkpoints)
                    for ((segmentPoints, severity) in segments) {
                        val segLine = Polyline(mapView).apply {
                            setPoints(segmentPoints.map { GeoPoint(it.latitude, it.longitude) })
                            outlinePaint.color = severityToColor(severity)
                            outlinePaint.strokeWidth = 10f
                            outlinePaint.strokeCap = Paint.Cap.ROUND
                            outlinePaint.strokeJoin = Paint.Join.ROUND
                        }
                        mapView.overlays.add(segLine)
                    }
                }

                // Checkpoint glass weather badge markers
                evaluatedRoute.checkpoints.forEachIndexed { idx, evalCp ->
                    val isSelected = selectedCheckpointIndex == idx
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(evalCp.checkpoint.latitude, evalCp.checkpoint.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        icon = createWeatherPinDrawable(
                            context = context,
                            evalCp = evalCp,
                            unitSystem = unitSystem,
                            selected = isSelected,
                        )
                        setOnMarkerClickListener { _, _ ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCheckpointClick(idx)
                            true
                        }
                    }
                    mapView.overlays.add(marker)
                }
            }

            mapView.invalidate()
        },
    )
}

private fun splitPolylineByCheckpoints(
    points: List<LatLon>,
    checkpoints: List<EvaluatedCheckpoint>,
): List<Pair<List<LatLon>, WeatherSeverity>> {
    if (checkpoints.isEmpty() || points.size < 2) return emptyList()
    val result = mutableListOf<Pair<List<LatLon>, WeatherSeverity>>()
    // Downsample slightly for fast rendering if huge
    val stride = (points.size / 220).coerceAtLeast(1)
    val sampledPts = points.filterIndexed { index, _ -> index % stride == 0 || index == points.lastIndex }

    var currentSeverity = nearestSeverity(sampledPts.first(), checkpoints)
    var bucket = mutableListOf(sampledPts.first())

    for (i in 1 until sampledPts.size) {
        val pt = sampledPts[i]
        val sev = nearestSeverity(pt, checkpoints)
        bucket.add(pt)
        if (sev != currentSeverity) {
            result.add(bucket to currentSeverity)
            currentSeverity = sev
            bucket = mutableListOf(pt)
        }
    }
    if (bucket.size >= 2) {
        result.add(bucket to currentSeverity)
    }
    return result
}

private fun nearestSeverity(pt: LatLon, checkpoints: List<EvaluatedCheckpoint>): WeatherSeverity {
    var bestDist = Double.MAX_VALUE
    var bestSev = WeatherSeverity.CLEAR
    for (cp in checkpoints) {
        val d = RouteSampler.haversineMeters(pt, LatLon(cp.checkpoint.latitude, cp.checkpoint.longitude))
        if (d < bestDist) {
            bestDist = d
            bestSev = cp.severity
        }
    }
    return bestSev
}

private fun severityToColor(severity: WeatherSeverity): Int = when (severity) {
    WeatherSeverity.CLEAR -> AndroidColor.rgb(56, 189, 248)
    WeatherSeverity.CAUTION -> AndroidColor.rgb(245, 158, 11)
    WeatherSeverity.HAZARD -> AndroidColor.rgb(251, 113, 133)
}

private fun createWeatherPinDrawable(
    context: Context,
    evalCp: EvaluatedCheckpoint,
    unitSystem: UnitSystem,
    selected: Boolean,
): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val w = (if (selected) 88 else 74) * density
    val h = (if (selected) 46 else 38) * density
    val bmp = Bitmap.createBitmap(w.roundToInt(), h.roundToInt(), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val pad = 3f * density
    val rect = RectF(pad, pad, w - pad, h - pad)
    val radius = (h - pad * 2f) / 2f

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) AndroidColor.argb(242, 24, 44, 72) else AndroidColor.argb(226, 15, 25, 42)
        style = Paint.Style.FILL
    }
    canvas.drawRoundRect(rect, radius, radius, bgPaint)

    val accentColor = when {
        evalCp.solarState.glareRisk == GlareRisk.SEVERE -> AndroidColor.rgb(251, 113, 133)
        evalCp.severity == WeatherSeverity.HAZARD -> AndroidColor.rgb(59, 130, 246)
        evalCp.severity == WeatherSeverity.CAUTION -> AndroidColor.rgb(245, 158, 11)
        else -> AndroidColor.rgb(56, 189, 248)
    }

    val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (selected) accentColor else AndroidColor.argb(175, 255, 255, 255)
        strokeWidth = if (selected) 2.2f * density else 1.2f * density
        style = Paint.Style.STROKE
    }
    canvas.drawRoundRect(rect, radius, radius, rimPaint)

    // Condition glyph dot
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = when (evalCp.condition) {
            WeatherCondition.CLEAR -> AndroidColor.rgb(245, 158, 11)
            WeatherCondition.RAIN, WeatherCondition.HEAVY_RAIN, WeatherCondition.DRIZZLE -> AndroidColor.rgb(59, 130, 246)
            WeatherCondition.THUNDERSTORM -> AndroidColor.rgb(129, 140, 248)
            WeatherCondition.SNOW -> AndroidColor.rgb(224, 242, 254)
            else -> AndroidColor.rgb(148, 163, 184)
        }
    }
    val dotR = 4.5f * density
    canvas.drawCircle(pad + 11f * density, h * 0.36f, dotR, dotPaint)

    val tempText = Formatters.formatTemp(evalCp.temperatureC, unitSystem)
    val tempPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textSize = (if (selected) 13f else 11.5f) * density
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
    canvas.drawText(tempText, pad + 19f * density, h * 0.44f, tempPaint)

    val timeText = Formatters.formatLocalTime(evalCp.passByEpochSeconds, evalCp.utcOffsetSeconds)
    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(179, 196, 220)
        textSize = (if (selected) 9.5f else 8.5f) * density
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }
    canvas.drawText(timeText, pad + 10f * density, h * 0.77f, timePaint)

    return BitmapDrawable(context.resources, bmp)
}
