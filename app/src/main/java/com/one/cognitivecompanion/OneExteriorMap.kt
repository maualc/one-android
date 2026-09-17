package com.one.cognitivecompanion

import android.content.Context
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
import org.maplibre.android.style.layers.PropertyFactory.lineColor
import org.maplibre.android.style.layers.PropertyFactory.lineWidth
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import okhttp3.OkHttpClient
import org.maplibre.android.module.http.HttpRequestUtil
import kotlin.math.cos
import kotlin.math.sin

private const val ONE_HOME_ZONE_SOURCE = "one-home-zone"
private const val ONE_SAFE_ZONES_SOURCE = "one-safe-zones"
private const val ONE_ROUTE_SOURCE = "one-route"
private const val ONE_HOME_POINT_SOURCE = "one-home-point"
private const val ONE_SAFE_POINTS_SOURCE = "one-safe-points"
private const val ONE_CURRENT_POINT_SOURCE = "one-current-point"
private const val ONE_SEARCH_POINT_SOURCE = "one-search-point"

private const val OSM_STYLE_JSON = """
{
  "version": 8,
  "name": "ONE OpenStreetMap exterior demo",
  "sources": {
    "one-osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    { "id": "one-osm", "type": "raster", "source": "one-osm" }
  ]
}
"""

private const val DEFAULT_MAP_LATITUDE = 40.4168
private const val DEFAULT_MAP_LONGITUDE = -3.7038

private val oneExteriorHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "ONE Android exterior demo/1.0 (MapLibre; OpenStreetMap tiles)")
                .build()
            chain.proceed(request)
        }
        .build()
}

private var oneExteriorHttpClientConfigured = false

enum class OneExteriorMapCameraAction {
    ZOOM_IN,
    ZOOM_OUT,
    RECENTER,
    FOCUS
}

data class OneExteriorMapCameraCommand(
    val id: Long,
    val action: OneExteriorMapCameraAction,
    val target: OneExteriorPoint? = null
)

@Composable
fun OneExteriorMap(
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>,
    routePoints: List<OneExteriorPoint>,
    currentPoint: OneExteriorPoint?,
    onMapTap: (OneExteriorPoint) -> Unit,
    modifier: Modifier = Modifier,
    focusPoint: OneExteriorPoint? = null,
    searchPoint: OneExteriorPoint? = null,
    cameraCommand: OneExteriorMapCameraCommand? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val mapView = remember(context) { createOneExteriorMapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    var lastCameraCenter by remember { mutableStateOf<OneExteriorPoint?>(null) }

    DisposableEffect(mapView, lifecycleOwner) {
        var mapViewStarted = false
        var mapViewResumed = false
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    if (!mapViewStarted) {
                        mapView.onStart()
                        mapViewStarted = true
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    if (!mapViewResumed) {
                        mapView.onResume()
                        mapViewResumed = true
                    }
                }

                Lifecycle.Event.ON_PAUSE -> {
                    if (mapViewResumed) {
                        mapView.onPause()
                        mapViewResumed = false
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    if (mapViewStarted) {
                        mapView.onStop()
                        mapViewStarted = false
                    }
                }

                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        var disposed = false
        mapView.getMapAsync { loadedMap ->
            if (!disposed) {
                loadedMap.addOnMapClickListener { point ->
                    currentOnMapTap(OneExteriorPoint(point.latitude, point.longitude))
                    true
                }
                loadedMap.setStyle(Style.Builder().fromJson(OSM_STYLE_JSON)) {
                    if (!disposed) {
                        map = loadedMap
                        styleReady = true
                        addOneExteriorLayers(loadedMap)
                    }
                }
            }
        }
        onDispose {
            disposed = true
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (mapViewResumed) {
                mapView.onPause()
            }
            if (mapViewStarted) {
                mapView.onStop()
            }
            mapView.onDestroy()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )

    LaunchedEffect(styleReady, home, safePlaces, routePoints, currentPoint, searchPoint) {
        val loadedMap = map ?: return@LaunchedEffect
        val style = loadedMap.style ?: return@LaunchedEffect
        updateOneExteriorSources(style, home, safePlaces, routePoints, currentPoint, searchPoint)
    }

    LaunchedEffect(styleReady, home?.center, focusPoint) {
        val loadedMap = map ?: return@LaunchedEffect
        val center = focusPoint ?: home?.center ?: OneExteriorPoint(DEFAULT_MAP_LATITUDE, DEFAULT_MAP_LONGITUDE)
        if (center == lastCameraCenter) return@LaunchedEffect
        lastCameraCenter = center
        loadedMap.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder()
                    .target(LatLng(center.latitude, center.longitude))
                    .zoom(if (home == null) 12.0 else 16.0)
                    .build()
            )
        )
    }

    LaunchedEffect(styleReady, cameraCommand?.id) {
        val command = cameraCommand ?: return@LaunchedEffect
        val loadedMap = map ?: return@LaunchedEffect
        if (command.id == 0L) return@LaunchedEffect

        when (command.action) {
            OneExteriorMapCameraAction.ZOOM_IN,
            OneExteriorMapCameraAction.ZOOM_OUT -> {
                val currentCamera = loadedMap.cameraPosition
                val target = currentCamera.target ?: return@LaunchedEffect
                val delta = if (command.action == OneExteriorMapCameraAction.ZOOM_IN) 1.0 else -1.0
                val nextZoom = (currentCamera.zoom + delta).coerceIn(2.0, 19.0)
                loadedMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(target)
                            .zoom(nextZoom)
                            .bearing(currentCamera.bearing)
                            .tilt(currentCamera.tilt)
                            .build()
                    )
                )
            }

            OneExteriorMapCameraAction.RECENTER,
            OneExteriorMapCameraAction.FOCUS -> {
                val target = command.target ?: return@LaunchedEffect
                loadedMap.animateCamera(
                    CameraUpdateFactory.newCameraPosition(
                        CameraPosition.Builder()
                            .target(LatLng(target.latitude, target.longitude))
                            .zoom(if (command.action == OneExteriorMapCameraAction.RECENTER) 16.0 else 15.0)
                            .build()
                    )
                )
            }
        }
    }
}

private fun createOneExteriorMapView(context: Context): MapView {
    ensureOneExteriorMapRuntime(context)
    return MapView(context).also { it.onCreate(null) }
}

@Synchronized
private fun ensureOneExteriorMapRuntime(context: Context) {
    // MapLibre must be initialized before HttpRequestUtil is touched. The
    // latter eagerly creates MapLibre's HTTP identifier and otherwise throws a
    // MapLibreConfigurationException while the composable is being composed.
    MapLibre.getInstance(context)
    if (!oneExteriorHttpClientConfigured) {
        // OSM's tile policy asks clients to identify themselves. Reuse one
        // client for all map instances so opening/closing the card does not
        // create a new dispatcher and connection pool each time.
        HttpRequestUtil.setOkHttpClient(oneExteriorHttpClient)
        oneExteriorHttpClientConfigured = true
    }
}

private fun addOneExteriorLayers(map: MapLibreMap) {
    val style = map.style ?: return
    style.addSource(GeoJsonSource(ONE_HOME_ZONE_SOURCE))
    style.addSource(GeoJsonSource(ONE_SAFE_ZONES_SOURCE))
    style.addSource(GeoJsonSource(ONE_ROUTE_SOURCE))
    style.addSource(GeoJsonSource(ONE_HOME_POINT_SOURCE))
    style.addSource(GeoJsonSource(ONE_SAFE_POINTS_SOURCE))
    style.addSource(GeoJsonSource(ONE_CURRENT_POINT_SOURCE))
    style.addSource(GeoJsonSource(ONE_SEARCH_POINT_SOURCE))

    style.addLayer(
        FillLayer("one-home-fill", ONE_HOME_ZONE_SOURCE).withProperties(
            fillColor("#1769E8"),
            fillOpacity(0.16f)
        )
    )
    style.addLayer(
        LineLayer("one-home-outline", ONE_HOME_ZONE_SOURCE).withProperties(
            lineColor("#1769E8"),
            lineWidth(2.0f)
        )
    )
    style.addLayer(
        FillLayer("one-safe-fill", ONE_SAFE_ZONES_SOURCE).withProperties(
            fillColor("#0F9D7A"),
            fillOpacity(0.16f)
        )
    )
    style.addLayer(
        LineLayer("one-safe-outline", ONE_SAFE_ZONES_SOURCE).withProperties(
            lineColor("#0F9D7A"),
            lineWidth(2.0f)
        )
    )
    style.addLayer(
        LineLayer("one-route-line", ONE_ROUTE_SOURCE).withProperties(
            lineColor("#E56B1F"),
            lineWidth(3.0f)
        )
    )
    addPointLayer(style, "one-home-center", ONE_HOME_POINT_SOURCE, "#1769E8")
    addPointLayer(style, "one-safe-center", ONE_SAFE_POINTS_SOURCE, "#0F9D7A")
    addPointLayer(style, "one-current-center", ONE_CURRENT_POINT_SOURCE, "#C43D5C", radius = 7.0f)
    addPointLayer(style, "one-search-center", ONE_SEARCH_POINT_SOURCE, "#7B1FA2", radius = 8.0f)
}

private fun addPointLayer(style: Style, id: String, sourceId: String, color: String, radius: Float = 6.0f) {
    style.addLayer(
        CircleLayer(id, sourceId).withProperties(
            circleColor(color),
            circleRadius(radius),
            circleStrokeColor("#FFFFFF"),
            circleStrokeWidth(2.0f)
        )
    )
}

private fun updateOneExteriorSources(
    style: Style,
    home: OneExteriorHomeZone?,
    safePlaces: List<OneExteriorSafePlace>,
    routePoints: List<OneExteriorPoint>,
    currentPoint: OneExteriorPoint?,
    searchPoint: OneExteriorPoint?
) {
    style.getSourceAs<GeoJsonSource>(ONE_HOME_ZONE_SOURCE)?.setGeoJson(
        home?.let {
            FeatureCollection.fromFeatures(arrayOf(circleFeature(it.center, it.radiusMeters)))
        } ?: FeatureCollection.fromFeatures(emptyArray())
    )
    style.getSourceAs<GeoJsonSource>(ONE_SAFE_ZONES_SOURCE)?.setGeoJson(
        FeatureCollection.fromFeatures(safePlaces.map { circleFeature(it.center, it.radiusMeters) }.toTypedArray())
    )
    style.getSourceAs<GeoJsonSource>(ONE_ROUTE_SOURCE)?.setGeoJson(
        if (routePoints.size >= 2) {
            FeatureCollection.fromFeatures(
                arrayOf(Feature.fromGeometry(LineString.fromLngLats(routePoints.map(::mapPoint))))
            )
        } else {
            FeatureCollection.fromFeatures(emptyArray())
        }
    )
    style.getSourceAs<GeoJsonSource>(ONE_HOME_POINT_SOURCE)?.setGeoJson(
        pointCollection(home?.center)
    )
    style.getSourceAs<GeoJsonSource>(ONE_SAFE_POINTS_SOURCE)?.setGeoJson(
        FeatureCollection.fromFeatures(safePlaces.map { Feature.fromGeometry(mapPoint(it.center)) }.toTypedArray())
    )
    style.getSourceAs<GeoJsonSource>(ONE_CURRENT_POINT_SOURCE)?.setGeoJson(
        pointCollection(currentPoint)
    )
    style.getSourceAs<GeoJsonSource>(ONE_SEARCH_POINT_SOURCE)?.setGeoJson(
        pointCollection(searchPoint)
    )
}

private fun pointCollection(point: OneExteriorPoint?): FeatureCollection =
    FeatureCollection.fromFeatures(
        point?.let { arrayOf(Feature.fromGeometry(mapPoint(it))) } ?: emptyArray()
    )

private fun mapPoint(point: OneExteriorPoint): Point = Point.fromLngLat(point.longitude, point.latitude)

private fun circleFeature(center: OneExteriorPoint, radiusMeters: Double): Feature {
    val coordinates = (0..48).map { index ->
        val angle = (index / 48.0) * Math.PI * 2.0
        val point = center.offsetMeters(
            eastMeters = cos(angle) * radiusMeters,
            northMeters = sin(angle) * radiusMeters
        )
        mapPoint(point)
    }
    return Feature.fromGeometry(Polygon.fromLngLats(listOf(coordinates)))
}
