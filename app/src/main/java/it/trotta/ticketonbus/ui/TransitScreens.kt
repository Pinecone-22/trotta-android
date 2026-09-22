package it.trotta.ticketonbus.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.trotta.ticketonbus.R
import it.trotta.ticketonbus.data.transit.Departure
import it.trotta.ticketonbus.data.transit.Departures
import it.trotta.ticketonbus.data.transit.Geo
import it.trotta.ticketonbus.data.transit.TransitLine
import it.trotta.ticketonbus.data.transit.TransitService
import it.trotta.ticketonbus.data.transit.TransitStop

@Composable
fun TransitApp(state: TransitUiState, vm: TransitViewModel, onOpenUrl: (String) -> Unit, onExit: () -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result -> vm.onPermissionResult(result.values.any { it }) }

    LaunchedEffect(Unit) {
        if (state.location == null && !state.locationDenied) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    val message = state.message?.resolve()
    LaunchedEffect(message) {
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            vm.consumeMessage()
        }
    }

    BackHandler { if (!vm.back()) onExit() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state.screen) {
                TransitScreen.NEARBY -> NearbyScreen(state, vm, onExit)
                TransitScreen.STOPS -> StopsScreen(state, vm)
                TransitScreen.STOP -> StopDetailScreen(state, vm, onOpenUrl)
                TransitScreen.LINES -> LinesScreen(state, vm)
                TransitScreen.LINE -> LineDetailScreen(state, vm)
                TransitScreen.MAP -> MapScreen(state, vm)
            }
            if (state.loading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NearbyScreen(state: TransitUiState, vm: TransitViewModel, onExit: () -> Unit) {
    val network = vm.network
    val nearby = vm.nearby(60)
    val favourites = vm.favouriteStops()
    val nowMinutes = state.now.hour * 60 + state.now.minute

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.transit_title),
            subtitle = listOfNotNull(network?.name, activeMapProvider.label).joinToString(" · "),
            onBack = onExit,
            actions = {
                IconButton(onClick = { vm.requestLocation() }) {
                    Icon(Icons.Filled.LocationOn, contentDescription = stringResource(R.string.action_use_location))
                }
                IconButton(onClick = { vm.go(TransitScreen.MAP) }) {
                    Icon(Icons.Filled.Place, contentDescription = stringResource(R.string.action_map))
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NetworkPicker(state, vm)
            QuickActions(vm)

            if (favourites.isNotEmpty()) {
                Text(
                    stringResource(R.string.favourites_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                favourites.forEach { stop ->
                    StopRow(
                        stop = stop,
                        subtitle = stop.lines.takeIf { it.isNotEmpty() }?.joinToString(" · ") { "Linea $it" },
                        departures = vm.departures(stop.id, 2),
                        nowMinutes = nowMinutes,
                        onClick = { vm.openStop(stop.id) },
                    )
                }
            }

            when {
                state.locating -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.transit_locating), style = MaterialTheme.typography.bodyMedium)
                }

                state.location == null -> Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.transit_nearby),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            stringResource(
                                if (state.locationDenied) {
                                    R.string.transit_location_denied
                                } else {
                                    R.string.transit_location_prompt
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { vm.requestLocation() }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_use_location))
                        }
                        OutlinedButton(onClick = { vm.go(TransitScreen.STOPS) }, modifier = Modifier.fillMaxWidth()) {
                            val count = network?.stops?.size ?: 0
                            Text(pluralStringResource(R.plurals.n_stops, count, count))
                        }
                    }
                }

                else -> {
                    Text(
                        stringResource(R.string.transit_nearby),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (nearby.isEmpty()) {
                        EmptyState(stringResource(R.string.transit_nearby_empty))
                    }
                    nearby.take(20).forEach { item ->
                        val walk = Geo.walkMinutes(item.distanceMeters)
                        StopRow(
                            stop = item.stop,
                            subtitle = "${Geo.formatDistance(item.distanceMeters)} · " +
                                pluralStringResource(R.plurals.walk_minutes, walk, walk),
                            departures = vm.departures(item.stop.id, 2),
                            nowMinutes = nowMinutes,
                            onClick = { vm.openStop(item.stop.id) },
                        )
                    }
                    OutlinedButton(onClick = { vm.go(TransitScreen.MAP) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.action_see_map))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NetworkPicker(state: TransitUiState, vm: TransitViewModel) {
    if (state.networks.size <= 1) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        state.networks.forEach { network ->
            SelectableChip(
                label = "${network.name} (${network.stops.size})",
                selected = network.id == state.networkId,
                onClick = { vm.selectNetwork(network.id) },
            )
        }
    }
}

@Composable
private fun QuickActions(vm: TransitViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { vm.go(TransitScreen.STOPS) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.action_search_stops))
        }
        OutlinedButton(onClick = { vm.go(TransitScreen.LINES) }, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.action_lines_times))
        }
    }
}

@Composable
private fun StopsScreen(state: TransitUiState, vm: TransitViewModel) {
    val results = vm.searchResults()
    val nowMinutes = state.now.hour * 60 + state.now.minute
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.stops_title),
            subtitle = vm.network?.name,
            onBack = { vm.go(TransitScreen.NEARBY) },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                label = { Text(stringResource(R.string.stops_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                pluralStringResource(R.plurals.n_stops, results.size, results.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            results.take(300).forEach { stop ->
                val distance = state.location?.let {
                    Geo.formatDistance(Geo.distanceMeters(it.lat, it.lon, stop.lat, stop.lon))
                }
                StopRow(
                    stop = stop,
                    subtitle = listOfNotNull(
                        distance,
                        stop.lines.takeIf { it.isNotEmpty() }?.joinToString(" · ") { "Linea $it" },
                    ).joinToString(" · ").ifBlank { null },
                    departures = emptyList(),
                    nowMinutes = nowMinutes,
                    onClick = { vm.openStop(stop.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StopRow(
    stop: TransitStop,
    subtitle: String?,
    departures: List<Departure>,
    nowMinutes: Int,
    onClick: () -> Unit,
) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stop.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (departures.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        departures.forEach { dep -> NextBusChip(dep) }
                    }
                }
            }
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun NextBusChip(departure: Departure) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = buildString {
                append("L").append(departure.lineId).append(" ").append(departure.time)
                if (departure.estimated) append(" ~")
            },
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StopDetailScreen(state: TransitUiState, vm: TransitViewModel, onOpenUrl: (String) -> Unit) {
    val stop = vm.stop(state.stopId)
    if (stop == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(R.string.stops_title), onBack = { vm.back() })
            Column(Modifier.padding(16.dp)) { EmptyState(stringResource(R.string.stops_none_scheduled)) }
        }
        return
    }
    val lines = vm.network?.lines.orEmpty().filter { it.id in stop.lines }
    val departures = vm.departures(stop.id, 24)
    val reach = vm.reachSummary(stop)
    val path = vm.reachLine(stop)
    val user = state.location
    val nowMinutes = state.now.hour * 60 + state.now.minute
    val today = departures.filter { it.dayOffset == 0 }
    val tomorrow = departures.filter { it.dayOffset > 0 }
    val isFavourite = stop.id in state.favourites

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stop.name,
            subtitle = vm.network?.name,
            onBack = { vm.back() },
            actions = {
                IconButton(onClick = { vm.toggleFavourite(stop.id) }) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = stringResource(
                            if (isFavourite) R.string.favourite_remove else R.string.favourite_add,
                        ),
                        tint = if (isFavourite) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        },
                    )
                }
                IconButton(onClick = { vm.openMap() }) {
                    Icon(Icons.Filled.Place, contentDescription = stringResource(R.string.action_map))
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (stop.code.isNotBlank()) LineChip("Cod. ${stop.code}")
                if (stop.approx) ChipWarning(stringResource(R.string.chip_approx))
                stop.lines.forEach { LineChip("Linea $it") }
            }

            Text(
                stringResource(R.string.stop_next),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (departures.isEmpty()) {
                EmptyState(stringResource(R.string.stops_none_scheduled))
            }
            if (today.isNotEmpty()) {
                Text(
                    stringResource(R.string.label_today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                today.forEach { dep -> DepartureRow(dep, nowMinutes) }
            }
            if (tomorrow.isNotEmpty()) {
                Text(
                    stringResource(R.string.label_tomorrow),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                tomorrow.take(8).forEach { dep -> DepartureRow(dep, nowMinutes) }
            }
            if (departures.any { it.estimated }) {
                WarningBanner(stringResource(R.string.transit_estimates_banner))
            }

            Text(
                stringResource(R.string.transit_map_reach),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (user != null) {
                Text(reach.orEmpty(), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    stringResource(R.string.transit_reach_prompt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { vm.requestLocation() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_use_location))
                }
            }
            val pins = buildList {
                if (user != null) add(MapPin("me", user.lat, user.lon, "Tu sei qui", isUser = true))
                add(
                    MapPin(
                        id = stop.id,
                        lat = stop.lat,
                        lon = stop.lon,
                        title = stop.name,
                        snippet = departures.firstOrNull()?.let { "L${it.lineId} · ${it.time}" },
                    ),
                )
            }
            val routes = buildList {
                lines.take(3).forEachIndexed { index, line ->
                    val points = vm.lineRoute(line)
                    if (points.size >= 2) add(MapRoute(points, lineColor(index), 7f))
                }
                if (path.size >= 2) add(MapRoute(path.map { it.lat to it.lon }, 0xFF546E7A.toInt(), 8f))
            }
            AppMapView(
                pins = pins,
                routes = routes,
                center = stop.lat to stop.lon,
                zoom = 15.0,
                onPinClick = { pin -> if (pin.id != "me") vm.openStop(pin.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            )
            val mapsUrl = vm.mapsUrl(stop)
            if (mapsUrl != null) {
                Button(onClick = { onOpenUrl(mapsUrl) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_directions))
                }
            }

            Text(
                stringResource(R.string.transit_times_per_line),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            if (lines.isEmpty()) {
                EmptyState(stringResource(R.string.transit_no_lines_for_stop))
            }
            lines.forEach { line ->
                LineTimesCard(line, stop) { vm.openLine(line.id) }
            }
        }
    }
}

@Composable
private fun DepartureRow(departure: Departure, nowMinutes: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LineChip("Linea ${departure.lineId}")
                    Spacer(Modifier.width(8.dp))
                    Text(departure.time, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (departure.dayOffset > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.label_tomorrow_lower),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (departure.estimated) {
                        Spacer(Modifier.width(6.dp))
                        EstimatedChip()
                    }
                }
                if (departure.heading.isNotBlank()) {
                    Text(
                        departure.heading,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                countdownLabel(departure.time, departure.dayOffset, nowMinutes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun countdownLabel(time: String, dayOffset: Int, nowMinutes: Int): String {
    val minutes = Departures.parseMinutes(time) ?: return ""
    val delta = minutes - nowMinutes + dayOffset * 24 * 60
    return when {
        delta < 0 -> ""
        delta == 0 -> stringResource(R.string.countdown_now)
        delta < 60 -> stringResource(R.string.countdown_minutes, delta)
        else -> stringResource(R.string.countdown_hours, delta / 60, delta % 60)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineTimesCard(line: TransitLine, stop: TransitStop, onOpenLine: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(line.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (line.description.isNotBlank()) {
                        Text(
                            line.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                TextButton(onClick = onOpenLine) { Text(stringResource(R.string.action_lines_times)) }
            }
            HorizontalDivider()
            line.services.forEach { service ->
                val column = Departures.stopTime(service, stop.id)
                if (column != null && column.times.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LineChip(stringResource(service.dayType.labelRes))
                            LineChip(stringResource(service.season.labelRes))
                            if (column.estimated) EstimatedChip() else OfficialChip()
                        }
                        Text(
                            column.times.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LinesScreen(state: TransitUiState, vm: TransitViewModel) {
    val network = vm.network
    val query = state.lineQuery.trim().lowercase()
    val lines = network?.lines.orEmpty()
        .filter { query.isEmpty() || it.name.lowercase().contains(query) || it.description.lowercase().contains(query) }
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.lines_title),
            subtitle = network?.name,
            onBack = { vm.go(TransitScreen.NEARBY) },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val note = network?.hoursNote.orEmpty()
            if (note.isNotBlank()) WarningBanner(note)
            OutlinedTextField(
                value = state.lineQuery,
                onValueChange = vm::setLineQuery,
                label = { Text(stringResource(R.string.lines_filter_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (lines.isEmpty()) EmptyState(stringResource(R.string.lines_none))
            lines.forEach { line ->
                LineSummaryCard(line) { vm.openLine(line.id) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LineSummaryCard(line: TransitLine, onClick: () -> Unit) {
    val runs = line.services.sumOf { it.departures }
    val days = line.services.map { it.dayType }.distinct()
    val seasons = line.services.map { it.season }.distinct()
    val first = line.services.firstOrNull()
    Card(modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onClick)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    line.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    pluralStringResource(R.plurals.n_runs, runs, runs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (line.description.isNotBlank()) {
                Text(
                    line.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (first != null) {
                Text(
                    stringResource(
                        R.string.line_from_to,
                        first.stops.firstOrNull()?.stopName.orEmpty(),
                        first.stops.lastOrNull()?.stopName.orEmpty(),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                days.forEach { LineChip(stringResource(it.labelRes)) }
                seasons.forEach { LineChip(stringResource(it.labelRes)) }
            }
        }
    }
}

@Composable
private fun LineDetailScreen(state: TransitUiState, vm: TransitViewModel) {
    val line = vm.line(state.lineId)
    if (line == null) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = stringResource(R.string.lines_title), onBack = { vm.back() })
            Column(Modifier.padding(16.dp)) { EmptyState(stringResource(R.string.lines_none)) }
        }
        return
    }
    val route = vm.lineRoute(line)
    val stops = line.stops
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = line.name,
            subtitle = line.description.ifBlank { vm.network?.name.orEmpty() },
            onBack = { vm.back() },
            actions = {
                IconButton(onClick = { vm.openMap(line.id) }) {
                    Icon(Icons.Filled.Place, contentDescription = stringResource(R.string.action_map))
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (route.size >= 2) {
                AppMapView(
                    pins = stops.mapNotNull { vm.stop(it.stopId) }
                        .map { stop -> MapPin(stop.id, stop.lat, stop.lon, stop.name) },
                    routes = listOf(MapRoute(route, lineColor(0), 10f)),
                    center = route.first(),
                    zoom = 13.0,
                    onPinClick = { pin -> vm.openStop(pin.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                )
            }
            Text(
                stringResource(
                    R.string.line_summary,
                    line.services.size,
                    line.services.sumOf { it.departures },
                    stops.size,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            line.services.forEach { service -> ServiceCard(service, vm) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceCard(service: TransitService, vm: TransitViewModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LineChip(stringResource(service.dayType.labelRes))
                LineChip(stringResource(service.season.labelRes))
                Text(
                    pluralStringResource(R.plurals.n_runs, service.departures, service.departures),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (service.heading.isNotBlank()) {
                Text(
                    service.heading,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            Text(
                stringResource(R.string.service_stops_times),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            service.stops.forEach { st ->
                val stop = vm.stop(st.stopId)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = stop != null) { stop?.let { vm.openStop(it.id) } },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            st.stopName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        if (st.estimated) EstimatedChip() else OfficialChip()
                    }
                    Text(
                        st.times.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MapScreen(state: TransitUiState, vm: TransitViewModel) {
    val network = vm.network
    val user = state.location
    val lines = network?.lines.orEmpty()
    val selected = lines.firstOrNull { it.id == state.mapLineId }
    val visibleStops = if (user != null) vm.nearby(120).map { it.stop } else network?.stops.orEmpty()
    val center = user?.let { it.lat to it.lon }
        ?: visibleStops.firstOrNull()?.let { it.lat to it.lon }
        ?: vm.networkCenter()
        ?: (41.77 to 12.23)

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = selected?.name ?: stringResource(R.string.map_title),
            subtitle = listOfNotNull(network?.name, activeMapProvider.label).joinToString(" · "),
            onBack = { vm.back() },
            actions = {
                IconButton(onClick = { vm.requestLocation() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                }
            },
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectableChip(
                    label = stringResource(R.string.map_all_count, lines.size),
                    selected = selected == null,
                ) { vm.selectMapLine(null) }
                lines.forEach { line ->
                    SelectableChip(line.id, line.id == selected?.id) { vm.selectMapLine(line.id) }
                }
            }
            if (selected == null) {
                Text(
                    stringResource(R.string.map_pick_line),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val routes = selected?.let { line ->
            val points = vm.lineRoute(line)
            if (points.size >= 2) listOf(MapRoute(points, lineColor(0), 11f)) else emptyList()
        }.orEmpty()
        AppMapView(
            pins = visibleStops.map { stop ->
                MapPin(
                    id = stop.id,
                    lat = stop.lat,
                    lon = stop.lon,
                    title = stop.name,
                    snippet = vm.departures(stop.id, 1).firstOrNull()?.let { "L${it.lineId} · ${it.time}" },
                )
            } + listOfNotNull(user?.let { MapPin("me", it.lat, it.lon, "Tu sei qui", isUser = true) }),
            routes = routes,
            center = center,
            zoom = if (selected != null) 12.5 else 13.0,
            onPinClick = { pin -> if (pin.id != "me") vm.openStop(pin.id) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
        if (user == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.map_showing_all),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { vm.requestLocation() }) {
                    Text(stringResource(R.string.action_use_location))
                }
            }
        }
    }
}

@Composable
private fun LineChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EstimatedChip() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            stringResource(R.string.chip_estimated),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun OfficialChip() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            stringResource(R.string.chip_official),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ChipWarning(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

private val LINE_COLORS = listOf(
    0xFF1565C0, 0xFFC62828, 0xFF2E7D32, 0xFF6A1B9A,
    0xFFEF6C00, 0xFF00838F, 0xFFAD1457, 0xFF4E342E,
    0xFF283593, 0xFF00897B, 0xFF9E9D24, 0xFFD84315,
).map { it.toInt() }

private fun lineColor(index: Int): Int = LINE_COLORS[index % LINE_COLORS.size]
