package it.trotta.ticketonbus.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.trotta.ticketonbus.data.transit.Departure
import it.trotta.ticketonbus.data.transit.Departures
import it.trotta.ticketonbus.data.transit.Geo
import it.trotta.ticketonbus.data.transit.GeoPoint
import it.trotta.ticketonbus.data.transit.LocationProvider
import it.trotta.ticketonbus.data.transit.NearbyStop
import it.trotta.ticketonbus.data.transit.Reach
import it.trotta.ticketonbus.data.transit.TransitLine
import it.trotta.ticketonbus.data.transit.TransitNetwork
import it.trotta.ticketonbus.data.transit.TransitRepository
import it.trotta.ticketonbus.data.transit.TransitStop
import it.trotta.ticketonbus.data.transit.BundledTransitProvider
import it.trotta.ticketonbus.data.AppPreferences
import it.trotta.ticketonbus.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

enum class TransitScreen { NEARBY, STOPS, STOP, LINES, LINE, MAP }

data class TransitUiState(
    val loading: Boolean = true,
    val networks: List<TransitNetwork> = emptyList(),
    val networkId: String = CAMPOBASSO,
    val screen: TransitScreen = TransitScreen.NEARBY,
    val query: String = "",

    val lineQuery: String = "",
    val stopId: String? = null,
    val lineId: String? = null,
    val location: GeoPoint? = null,
    val locating: Boolean = false,
    val locationDenied: Boolean = false,
    val message: UiMessage? = null,

    val favourites: Set<String> = emptySet(),
    val now: LocalDateTime = LocalDateTime.now(),

    val networkPinned: Boolean = false,

    val mapLineId: String? = null,
) {
    companion object {
        const val CAMPOBASSO = "campobasso"
    }
}

class TransitViewModel(app: Application) : AndroidViewModel(app) {

    private val provider = BundledTransitProvider(TransitRepository(app.assets))
    private val prefs = AppPreferences(app)
    private val locationProvider = LocationProvider(app)
    private val backStack = ArrayDeque<TransitScreen>()

    private val _state = MutableStateFlow(TransitUiState(favourites = prefs.favourites))
    val state: StateFlow<TransitUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val dataset = runCatching { provider.load() }.getOrNull()
            if (dataset == null) {
                _state.update {
                    it.copy(loading = false, message = UiMessage.Res(R.string.msg_dataset_error))
                }
            } else {
                val default = dataset.networks.firstOrNull { it.id == TransitUiState.CAMPOBASSO }?.id
                    ?: dataset.networks.firstOrNull()?.id.orEmpty()
                _state.update { it.copy(loading = false, networks = dataset.networks, networkId = default) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                _state.update { it.copy(now = LocalDateTime.now()) }
            }
        }
    }

    val network: TransitNetwork?
        get() = _state.value.networks.firstOrNull { it.id == _state.value.networkId }

    fun stop(id: String?): TransitStop? =
        id?.let { wanted -> network?.stops?.firstOrNull { it.id == wanted } }

    fun line(id: String?): TransitLine? =
        id?.let { wanted -> network?.lines?.firstOrNull { it.id == wanted } }

    fun nearby(limit: Int = 40): List<NearbyStop> {
        val net = network ?: return emptyList()
        val loc = _state.value.location ?: return emptyList()
        return Geo.nearby(net.stops, loc.lat, loc.lon, limit)
    }

    fun searchResults(): List<TransitStop> {
        val net = network ?: return emptyList()
        val q = _state.value.query.trim().lowercase()
        if (q.isEmpty()) return net.stops.sortedBy { it.name.lowercase() }
        return net.stops
            .filter { it.name.lowercase().contains(q) || it.code.contains(q) || it.lines.any { l -> l.lowercase() == q } }
            .sortedBy { it.name.lowercase() }
    }

    fun reachSummary(stop: TransitStop): String? {
        val loc = _state.value.location ?: return null
        return Reach.summary(loc.lat, loc.lon, stop)
    }

    fun reachLine(stop: TransitStop): List<GeoPoint> {
        val loc = _state.value.location ?: return emptyList()
        return Reach.line(loc.lat, loc.lon, stop)
    }

    fun mapsUrl(stop: TransitStop): String? {
        val loc = _state.value.location ?: return null
        return Reach.googleMapsUrl(loc.lat, loc.lon, stop)
    }

    fun departures(stopId: String?, limit: Int = 8): List<Departure> {
        val net = network ?: return emptyList()
        val id = stopId ?: return emptyList()
        return Departures.upcoming(net, id, _state.value.now, limit)
    }

    private fun push() {
        backStack.addLast(_state.value.screen)
    }

    fun go(screen: TransitScreen) {
        backStack.clear()
        _state.update { it.copy(screen = screen) }
    }

    fun openStop(stopId: String) {
        push()
        _state.update { it.copy(screen = TransitScreen.STOP, stopId = stopId) }
    }

    fun openLine(lineId: String) {
        push()
        _state.update { it.copy(screen = TransitScreen.LINE, lineId = lineId) }
    }

    fun openMap(lineId: String? = null) {
        push()
        _state.update { it.copy(screen = TransitScreen.MAP, mapLineId = lineId) }
    }

    fun selectMapLine(lineId: String?) = _state.update { it.copy(mapLineId = lineId) }

    fun lineRoute(line: TransitLine): List<Pair<Double, Double>> {
        val net = network ?: return emptyList()
        val stopsById = net.stops.associateBy { it.id }
        val variant = line.services.maxByOrNull { it.stops.size } ?: return emptyList()
        return variant.stops.mapNotNull { stopsById[it.stopId]?.let { stop -> stop.lat to stop.lon } }
    }

    fun networkCenter(): Pair<Double, Double>? {
        val stops = network?.stops.orEmpty()
        if (stops.isEmpty()) return null
        return stops.map { it.lat }.average() to stops.map { it.lon }.average()
    }

    fun back(): Boolean {
        val previous = backStack.removeLastOrNull() ?: return false
        _state.update { it.copy(screen = previous) }
        return true
    }

    fun selectNetwork(networkId: String) {
        backStack.clear()
        _state.update {
            it.copy(
                networkId = networkId, screen = TransitScreen.NEARBY,
                stopId = null, lineId = null, query = "", lineQuery = "", networkPinned = true,
            )
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query) }

    fun setLineQuery(query: String) = _state.update { it.copy(lineQuery = query) }

    fun selectStop(stopId: String) = _state.update { it.copy(stopId = stopId) }

    fun requestLocation() {
        if (!locationProvider.hasPermission()) {
            _state.update { it.copy(locationDenied = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(locating = true, locationDenied = false) }
            val fix = locationProvider.awaitLocation()
            _state.update {
                val point = fix?.let { l -> GeoPoint(l.latitude, l.longitude) }
                val chosen = if (point != null && !it.networkPinned) {
                    nearestNetworkId(point.lat, point.lon) ?: it.networkId
                } else {
                    it.networkId
                }
                it.copy(
                    locating = false,
                    location = point,
                    networkId = chosen,
                    message = if (fix == null) UiMessage.Res(R.string.msg_location_unavailable) else it.message,
                )
            }
        }
    }

    private fun nearestNetworkId(lat: Double, lon: Double): String? =
        _state.value.networks.minByOrNull { network ->
            network.stops.minOfOrNull { Geo.distanceMeters(lat, lon, it.lat, it.lon) } ?: Double.MAX_VALUE
        }?.id

    fun onPermissionResult(granted: Boolean) {
        _state.update { it.copy(locationDenied = !granted) }
        if (granted) requestLocation()
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun toggleFavourite(stopId: String) {
        prefs.toggleFavourite(stopId)
        _state.update { it.copy(favourites = prefs.favourites) }
    }

    fun favouriteStops(): List<TransitStop> =
        network?.stops?.filter { it.id in _state.value.favourites }.orEmpty()
}
