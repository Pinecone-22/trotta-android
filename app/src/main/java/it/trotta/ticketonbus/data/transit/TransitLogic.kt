package it.trotta.ticketonbus.data.transit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

interface TransitProvider {
    suspend fun load(): TransitDataset
}

class BundledTransitProvider(private val repository: TransitRepository) : TransitProvider {
    override suspend fun load(): TransitDataset = withContext(Dispatchers.IO) { repository.load() }
}

object Geo {

    private const val EARTH_RADIUS_M = 6_371_000.0

    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * EARTH_RADIUS_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dl = Math.toRadians(lon2 - lon1)
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun bearingLabel(degrees: Double): String {
        val dirs = listOf("nord", "nord-est", "est", "sud-est", "sud", "sud-ovest", "ovest", "nord-ovest")
        return dirs[((degrees / 45.0).roundToInt()) % 8]
    }

    fun walkMinutes(distanceMeters: Double): Int =
        (distanceMeters * 1.25 / 80.0).roundToInt().coerceAtLeast(1)

    fun formatDistance(meters: Double): String = when {
        meters < 950 -> "${meters.roundToInt()} m"
        else -> "%.1f km".format(meters / 1000.0)
    }

    fun nearby(stops: List<TransitStop>, lat: Double, lon: Double, limit: Int = 40): List<NearbyStop> =
        stops.map { NearbyStop(it, distanceMeters(lat, lon, it.lat, it.lon)) }
            .sortedBy { it.distanceMeters }
            .take(limit)
}

object Departures {

    private val FIXED_HOLIDAYS = setOf(
        MonthDay.of(1, 1), MonthDay.of(1, 6), MonthDay.of(4, 25), MonthDay.of(5, 1),
        MonthDay.of(6, 2), MonthDay.of(8, 15), MonthDay.of(11, 1), MonthDay.of(12, 8),
        MonthDay.of(12, 25), MonthDay.of(12, 26),
    )

    fun isHoliday(date: LocalDate): Boolean =
        date.dayOfWeek == DayOfWeek.SUNDAY || MonthDay.from(date) in FIXED_HOLIDAYS || date == easterMonday(date.year)

    fun dayTypesFor(date: LocalDate): Set<DayType> =
        if (isHoliday(date)) setOf(DayType.FESTIVO) else setOf(DayType.FERIALE, DayType.SCOLASTICO)

    fun upcoming(
        network: TransitNetwork,
        stopId: String,
        now: LocalDateTime,
        limit: Int = 8,
    ): List<Departure> {
        val result = ArrayList<Departure>()
        for (offset in 0..1) {
            val day = now.toLocalDate().plusDays(offset.toLong())
            val types = dayTypesFor(day)
            val season = Season.current(day)
            val floor = if (offset == 0) now.toLocalTime().toSecondOfDay() / 60 else -1
            network.lines.forEach { line ->
                line.services
                    .filter { it.dayType in types && (it.season == Season.ANNUALE || it.season == season) }
                    .forEach { svc ->
                    val at = svc.stops.firstOrNull { it.stopId == stopId } ?: return@forEach
                    val isTerminus = svc.stops.firstOrNull()?.stopId == stopId
                    at.times.forEach { raw ->
                        val minutes = parseMinutes(raw) ?: return@forEach
                        if (minutes >= floor) {
                            result += Departure(
                                lineId = line.id,
                                lineName = line.name,
                                heading = svc.heading.ifBlank { svc.description }.ifBlank { line.description },
                                dayType = svc.dayType,
                                serviceId = svc.id,
                                timeMinutes = minutes,
                                time = formatMinutes(minutes),
                                dayOffset = offset,
                                estimated = at.estimated,
                                fromTerminus = isTerminus || !at.estimated,
                            )
                        }
                    }
                }
            }
            if (result.size >= limit) break
        }
        return result.sortedWith(compareBy({ it.dayOffset }, { it.timeMinutes }, { it.estimated }))
            .distinctBy { "${it.lineId}|${it.timeMinutes}|${it.dayOffset}" }
            .take(limit)
    }

    fun stopTimes(service: TransitService, stopId: String): List<String> =
        stopTime(service, stopId)?.times ?: emptyList()

    fun stopTime(service: TransitService, stopId: String): StopTime? =
        service.stops.firstOrNull { it.stopId == stopId }

    fun parseMinutes(raw: String): Int? {
        val parts = raw.replace('.', ':').split(':')
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..29 || m !in 0..59) return null
        return h * 60 + m
    }

    fun formatMinutes(minutes: Int): String = "%02d:%02d".format(minutes / 60, minutes % 60)

    fun secondsUntil(minutes: Int, now: LocalTime): Int {
        val nowMinutes = now.hour * 60 + now.minute
        return (minutes - nowMinutes) * 60 - now.second
    }

    private fun easterMonday(year: Int): LocalDate {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(year, month, day).plusDays(1)
    }

    fun nowDefault(): LocalDateTime = LocalDateTime.now()
}

object Reach {
    fun summary(fromLat: Double, fromLon: Double, stop: TransitStop): String {
        val distance = Geo.distanceMeters(fromLat, fromLon, stop.lat, stop.lon)
        val bearing = Geo.bearingLabel(Geo.bearingDegrees(fromLat, fromLon, stop.lat, stop.lon))
        val walk = Geo.walkMinutes(distance)
        return "${Geo.formatDistance(distance)} verso $bearing, circa $walk min a piedi"
    }

    fun line(fromLat: Double, fromLon: Double, stop: TransitStop): List<GeoPoint> =
        listOf(GeoPoint(fromLat, fromLon), GeoPoint(stop.lat, stop.lon))

    fun googleMapsUrl(fromLat: Double, fromLon: Double, stop: TransitStop): String =
        "https://www.google.com/maps/dir/?api=1&origin=$fromLat,$fromLon" +
            "&destination=${stop.lat},${stop.lon}&travelmode=walking"
}
