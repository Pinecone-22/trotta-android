package it.trotta.ticketonbus.data.transit

import androidx.annotation.StringRes
import it.trotta.ticketonbus.R

data class TransitStop(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,

    val approx: Boolean,
    val code: String,
    val lines: List<String>,
    val networkId: String,
)

data class StopTime(
    val stopId: String,
    val stopName: String,
    val times: List<String>,

    val estimated: Boolean = false,
) {

    fun departures(): List<String> = times
}

enum class DayType {
    FERIALE,
    FESTIVO,
    SCOLASTICO;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            FERIALE -> R.string.day_feriale
            FESTIVO -> R.string.day_festivo
            SCOLASTICO -> R.string.day_scolastico
        }

    companion object {
        fun from(raw: String?): DayType = when (raw?.lowercase()) {
            "festivo" -> FESTIVO
            "scolastico" -> SCOLASTICO
            else -> FERIALE
        }
    }
}

data class TransitService(
    val id: String,
    val lineId: String,
    val lineName: String,
    val description: String,
    val dayType: DayType,
    val season: Season = Season.ANNUALE,
    val heading: String,
    val stops: List<StopTime>,
) {
    val departures: Int get() = stops.maxOfOrNull { it.times.size } ?: 0
}

enum class Season {
    ANNUALE,
    INVERNALE,
    ESTIVA;

    @get:StringRes
    val labelRes: Int
        get() = when (this) {
            ANNUALE -> R.string.season_annual
            INVERNALE -> R.string.season_winter
            ESTIVA -> R.string.season_summer
        }

    companion object {
        fun from(raw: String?): Season = when (raw?.lowercase()) {
            "invernale" -> INVERNALE
            "estiva", "estivo" -> ESTIVA
            else -> ANNUALE
        }

        fun current(date: java.time.LocalDate): Season =
            if (date.monthValue in 6..8 || (date.monthValue == 9 && date.dayOfMonth <= 15)) ESTIVA else INVERNALE
    }
}

data class TransitLine(
    val id: String,
    val name: String,
    val description: String,
    val timesAvailable: Boolean,
    val days: List<DayType>,
    val services: List<TransitService>,
    val networkId: String,
) {

    val stops: List<StopTime>
        get() {
            val seen = LinkedHashMap<String, StopTime>()
            services.forEach { svc -> svc.stops.forEach { st -> seen.putIfAbsent(st.stopId, st) } }
            return seen.values.toList()
        }
}

data class TransitNetwork(
    val id: String,
    val name: String,
    val operator: String,
    val validFrom: String,
    val hoursNote: String,
    val stops: List<TransitStop>,
    val lines: List<TransitLine>,
)

data class TransitDataset(
    val schemaVersion: Int,
    val generatedAt: String,
    val attribution: String,
    val networks: List<TransitNetwork>,
)

data class Departure(
    val lineId: String,
    val lineName: String,
    val heading: String,
    val dayType: DayType,
    val serviceId: String,
    val timeMinutes: Int,
    val time: String,

    val dayOffset: Int = 0,

    val estimated: Boolean = false,

    val fromTerminus: Boolean = false,
)

data class NearbyStop(
    val stop: TransitStop,
    val distanceMeters: Double,
)

data class GeoPoint(val lat: Double, val lon: Double)
