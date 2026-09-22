package it.trotta.ticketonbus.ui

import android.content.Context
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import it.trotta.ticketonbus.BuildConfig
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmMapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File

data class MapPin(
    val id: String,
    val lat: Double,
    val lon: Double,
    val title: String,
    val snippet: String? = null,
    val isUser: Boolean = false,
)

data class MapRoute(
    val points: List<Pair<Double, Double>>,
    val color: Int = 0xFF1565C0.toInt(),
    val width: Float = 9f,
)

enum class MapProvider(val label: String) {
    GOOGLE("Google Maps"),
    OPENSTREETMAP("OpenStreetMap"),
}

val activeMapProvider: MapProvider
    get() = if (BuildConfig.MAPS_API_KEY.isNotBlank()) MapProvider.GOOGLE else MapProvider.OPENSTREETMAP

@Composable
fun AppMapView(
    pins: List<MapPin>,
    routes: List<MapRoute> = emptyList(),
    center: Pair<Double, Double>,
    zoom: Double = 15.0,
    modifier: Modifier = Modifier,
    onPinClick: (MapPin) -> Unit = {},
) {

    val clipped = modifier.clipToBounds()
    when (activeMapProvider) {
        MapProvider.GOOGLE -> GoogleMapView(pins, routes, center, zoom, clipped, onPinClick)
        MapProvider.OPENSTREETMAP -> OsmdroidMapView(pins, routes, center, zoom, clipped, onPinClick)
    }
}

@Composable
private fun GoogleMapView(
    pins: List<MapPin>,
    routes: List<MapRoute>,
    center: Pair<Double, Double>,
    zoom: Double,
    modifier: Modifier,
    onPinClick: (MapPin) -> Unit,
) {
    val context = LocalContext.current
    val click by rememberUpdatedState(onPinClick)
    val mapState = remember { mutableStateOf<GoogleMap?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        modifier = modifier,
        factory = {
            MapView(context).also { view ->
                view.onCreate(Bundle())
                view.getMapAsync { googleMap ->
                    googleMap.uiSettings.isZoomControlsEnabled = true
                    googleMap.uiSettings.isCompassEnabled = true
                    googleMap.setOnMarkerClickListener { marker ->
                        (marker.tag as? MapPin)?.let { click(it) }
                        true
                    }
                    mapState.value = googleMap
                }
                mapView = view
            }
        },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val view = mapView ?: return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_DESTROY -> view.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(mapState.value, pins, routes) {
        val googleMap = mapState.value ?: return@LaunchedEffect
        googleMap.clear()
        routes.forEach { route ->
            if (route.points.size >= 2) {
                googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(route.points.map { LatLng(it.first, it.second) })
                        .width(route.width)
                        .color(route.color),
                )
            }
        }
        pins.forEach { pin ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(pin.lat, pin.lon))
                    .title(pin.title)
                    .snippet(pin.snippet),
            )
            marker?.tag = pin
        }
    }

    LaunchedEffect(mapState.value, center, zoom) {
        mapState.value?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(center.first, center.second), zoom.toFloat()),
        )
    }
}

@Composable
private fun OsmdroidMapView(
    pins: List<MapPin>,
    routes: List<MapRoute>,
    center: Pair<Double, Double>,
    zoom: Double,
    modifier: Modifier,
    onPinClick: (MapPin) -> Unit,
) {
    val context = LocalContext.current
    val click by rememberUpdatedState(onPinClick)
    remember(context) {
        configureOsmdroid(context)
        true
    }
    val mapView = remember {
        OsmMapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
            controller.setZoom(zoom)
            controller.setCenter(GeoPoint(center.first, center.second))
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AndroidView(
        modifier = modifier,
        factory = { mapView },
        update = { map ->
            map.overlays.clear()
            routes.forEach { route ->
                if (route.points.size >= 2) {
                    map.overlays.add(
                        Polyline(map).apply {
                            setPoints(route.points.map { GeoPoint(it.first, it.second) })
                            outlinePaint.strokeWidth = route.width
                            outlinePaint.color = route.color
                        },
                    )
                }
            }
            pins.forEach { pin ->
                map.overlays.add(
                    Marker(map).apply {
                        position = GeoPoint(pin.lat, pin.lon)
                        title = pin.title
                        snippet = pin.snippet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        if (pin.isUser) {
                            icon = map.context.getDrawable(android.R.drawable.ic_menu_mylocation)
                        }
                        setOnMarkerClickListener { _, _ ->
                            click(pin)
                            true
                        }
                    },
                )
            }
            map.invalidate()
        },
    )

    LaunchedEffect(center, zoom) {
        mapView.controller.setCenter(GeoPoint(center.first, center.second))
        mapView.controller.setZoom(zoom)
    }
}

private fun configureOsmdroid(context: Context) {
    val config = Configuration.getInstance()
    config.userAgentValue = context.packageName
    val base = File(context.filesDir, "osmdroid")
    val tiles = File(context.cacheDir, "osmdroid-tiles")
    base.mkdirs()
    tiles.mkdirs()
    config.osmdroidBasePath = base
    config.osmdroidTileCache = tiles
}
