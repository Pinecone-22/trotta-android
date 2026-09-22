package it.trotta.ticketonbus.data.transit

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

class TransitDatasetTest {

    private fun dataset(): TransitDataset {
        val candidates = listOf(
            File("src/main/assets/transit.json"),
            File("app/src/main/assets/transit.json"),
        )
        val file = candidates.firstOrNull { it.exists() }
        assertNotNull("bundled transit.json not found", file)
        return TransitRepository.parse(JSONObject(file!!.readText()))
    }

    @Test
    fun `bundled dataset parses into both networks`() {
        val data = dataset()
        assertEquals(2, data.schemaVersion)
        assertEquals(setOf("campobasso", "fiumicino"), data.networks.map { it.id }.toSet())
        assertTrue(data.attribution.contains("OpenStreetMap"))
    }

    @Test
    fun `campobasso exposes the published urban lines`() {
        val cb = dataset().networks.first { it.id == "campobasso" }
        assertTrue("expected several hundred stops", cb.stops.size > 250)
        assertTrue(cb.lines.any { it.id == "1" })
        val line1 = cb.lines.first { it.id == "1" }
        assertTrue(line1.timesAvailable)
        assertTrue(line1.services.isNotEmpty())
        assertTrue(line1.services.all { it.stops.isNotEmpty() })
        assertTrue(line1.services.any { svc -> svc.stops.all { it.times.isNotEmpty() } })
    }

    @Test
    fun `every scheduled stop reference resolves to a stop`() {
        dataset().networks.forEach { network ->
            val ids = network.stops.map { it.id }.toSet()
            network.lines.forEach { line ->
                line.services.forEach { service ->
                    service.stops.forEach { stop ->
                        assertTrue(
                            "unresolved stop in line ${line.id}: ${stop.stopId}",
                            stop.stopId.isBlank() || stop.stopId in ids,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `coordinates are valid and distances ordered`() {
        val cb = dataset().networks.first { it.id == "campobasso" }
        assertTrue(cb.stops.all { it.lat in -90.0..90.0 && it.lon in -180.0..180.0 })
        val nearby = Geo.nearby(cb.stops, 41.5603, 14.6627, 10)
        assertEquals(10, nearby.size)
        assertTrue(nearby.zipWithNext().all { (a, b) -> a.distanceMeters <= b.distanceMeters })
    }

    @Test
    fun `fiumicino uses the official line page stops, not neighbouring ATAC stops`() {
        val fco = dataset().networks.first { it.id == "fiumicino" }
        assertTrue(fco.stops.isNotEmpty())
        assertTrue(fco.lines.isNotEmpty())
        assertTrue(fco.hoursNote.isNotBlank())

        assertTrue(fco.stops.any { it.name.contains("Focene", ignoreCase = true) })
        assertTrue(fco.lines.any { it.id == "1" })
        val line1 = fco.lines.first { it.id == "1" }
        assertTrue(line1.services.isNotEmpty())
        assertTrue(line1.services.any { it.stops.size > 5 })

        assertTrue(fco.stops.none { it.lat in 41.725..41.745 && it.lon in 12.26..12.30 })
    }

    @Test
    fun `every fiumicino service carries a time for every stop`() {

        val fco = dataset().networks.first { it.id == "fiumicino" }
        fco.lines.forEach { line ->
            line.services.forEach { service ->
                assertTrue(
                    "line ${line.id} service ${service.id} has a stop without times",
                    service.stops.all { it.times.isNotEmpty() },
                )
                assertFalse(
                    "terminus must stay official for ${service.id}",
                    service.stops.first().estimated,
                )
                assertTrue(
                    "at least one stop must be flagged estimated for ${service.id}",
                    service.stops.any { it.estimated },
                )
            }
        }
    }

    @Test
    fun `haversine matches a known distance`() {

        val d = Geo.distanceMeters(41.9010, 12.5017, 41.7710, 12.2390)
        assertTrue("unexpected distance $d", d in 23_000.0..28_000.0)
        assertEquals(0.0, Geo.distanceMeters(41.5, 12.5, 41.5, 12.5), 0.001)
    }

    @Test
    fun `walk time and distance formatting`() {
        assertEquals("300 m", Geo.formatDistance(300.0))
        assertEquals("1.2 km", Geo.formatDistance(1234.0))
        assertTrue(Geo.walkMinutes(800.0) >= 1)
        assertTrue(Geo.bearingLabel(0.0) == "nord")
        assertTrue(Geo.bearingLabel(90.0) == "est")
    }

    private fun syntheticNetwork(): TransitNetwork {
        fun stop(id: String) = TransitStop(id, "Stop $id", 41.5, 14.6, false, "", listOf("1"), "test")
        val service = TransitService(
            id = "s1",
            lineId = "1",
            lineName = "Linea 1",
            description = "Test",
            dayType = DayType.FERIALE,
            heading = "Linea 1 - andata",
            stops = listOf(
                StopTime("a", "Stop a", listOf("06:00", "12:30", "23:50")),
                StopTime("b", "Stop b", listOf("06:10", "12:40", "24:00")),
            ),
        )
        val festive = service.copy(id = "s2", dayType = DayType.FESTIVO, stops = listOf(
            StopTime("a", "Stop a", listOf("09:00")),
            StopTime("b", "Stop b", listOf("09:10")),
        ))
        val line = TransitLine("1", "Linea 1", "Test", true, listOf(DayType.FERIALE, DayType.FESTIVO),
            listOf(service, festive), "test")
        return TransitNetwork("test", "Test", "op", "", "", listOf(stop("a"), stop("b")), listOf(line))
    }

    @Test
    fun `weekday shows only weekday runs after now`() {
        val net = syntheticNetwork()
        val monday = LocalDateTime.of(2026, 9, 21, 12, 0)
        val next = Departures.upcoming(net, "a", monday, 5)
        assertTrue(next.isNotEmpty())
        assertEquals(DayType.FERIALE, next.first().dayType)
        assertEquals("12:30", next.first().time)
    }

    @Test
    fun `sunday shows festive timetables`() {
        val net = syntheticNetwork()
        val sunday = LocalDateTime.of(2026, 9, 20, 8, 0)
        val next = Departures.upcoming(net, "a", sunday, 5)
        assertEquals(DayType.FESTIVO, next.first().dayType)
        assertEquals("09:00", next.first().time)
    }

    @Test
    fun `after the last run the first run of tomorrow is used`() {
        val net = syntheticNetwork()
        val mondayLate = LocalDateTime.of(2026, 9, 21, 23, 55)
        val next = Departures.upcoming(net, "a", mondayLate, 3)
        assertTrue(next.isNotEmpty())
        assertEquals(1, next.first().dayOffset)
        assertEquals("06:00", next.first().time)
    }

    @Test
    fun `italian holidays are treated as festive`() {
        assertTrue(Departures.isHoliday(LocalDate.of(2026, 12, 25)))
        assertTrue(Departures.isHoliday(LocalDate.of(2026, 1, 1)))
        assertTrue(Departures.isHoliday(LocalDate.of(2026, 8, 16)))
        assertFalse(Departures.isHoliday(LocalDate.of(2026, 9, 21)))
        assertTrue(Departures.dayTypesFor(LocalDate.of(2026, 12, 25)) == setOf(DayType.FESTIVO))
    }

    @Test
    fun `reach summary reports distance and walking time`() {
        val stop = TransitStop("a", "Stop a", 41.5610, 14.6630, false, "", listOf("1"), "test")
        val summary = Reach.summary(41.5603, 14.6627, stop)
        assertTrue(summary.contains("verso"))
        assertTrue(summary.contains("a piedi"))
        val line = Reach.line(41.5603, 14.6627, stop)
        assertEquals(2, line.size)
        assertTrue(Reach.googleMapsUrl(41.5603, 14.6627, stop).startsWith("https://www.google.com/maps/dir/"))
    }
}
