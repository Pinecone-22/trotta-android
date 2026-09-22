package it.trotta.ticketonbus.data.transit

import android.content.res.AssetManager
import org.json.JSONArray
import org.json.JSONObject

class TransitRepository(private val assets: AssetManager) {

    fun load(): TransitDataset {
        val text = assets.open(ASSET).bufferedReader().use { it.readText() }
        return parse(JSONObject(text))
    }

    companion object {
        const val ASSET = "transit.json"

        fun parse(root: JSONObject): TransitDataset {
            val networks = root.optJSONArray("networks").objectList().map { n ->
                val networkId = n.getString("id")
                val stops = n.optJSONArray("stops").objectList().map { s ->
                    TransitStop(
                        id = s.getString("id"),
                        name = s.getString("name"),
                        lat = s.getDouble("lat"),
                        lon = s.getDouble("lon"),
                        approx = s.optBoolean("approx", false),
                        code = s.optString("code"),
                        lines = s.optJSONArray("lines").stringList(),
                        networkId = networkId,
                    )
                }
                val lines = n.optJSONArray("lines").objectList().map { l ->
                    val lineId = l.getString("id")
                    val services = l.optJSONArray("services").objectList().map { svc ->
                        TransitService(
                            id = svc.getString("id"),
                            lineId = lineId,
                            lineName = svc.optString("lineName", "Linea $lineId"),
                            description = svc.optString("description"),
                            dayType = DayType.from(svc.optString("dayType")),
                            season = Season.from(svc.optString("season")),
                            heading = svc.optString("heading"),
                            stops = svc.optJSONArray("stops").objectList().map { st ->
                                StopTime(
                                    stopId = st.optString("stopId"),
                                    stopName = st.optString("name"),
                                    times = st.optJSONArray("times").stringList(),
                                    estimated = st.optBoolean("estimated", false),
                                )
                            },
                        )
                    }
                    TransitLine(
                        id = lineId,
                        name = l.optString("name", "Linea $lineId"),
                        description = l.optString("description"),
                        timesAvailable = l.optBoolean("timesAvailable", services.isNotEmpty()),
                        days = l.optJSONArray("days").stringList().map { DayType.from(it) },
                        services = services,
                        networkId = networkId,
                    )
                }
                TransitNetwork(
                    id = networkId,
                    name = n.optString("name", networkId),
                    operator = n.optString("operator"),
                    validFrom = n.optString("validFrom"),
                    hoursNote = n.optString("hoursNote"),
                    stops = stops,
                    lines = lines,
                )
            }
            return TransitDataset(
                schemaVersion = root.optInt("schemaVersion", 1),
                generatedAt = root.optString("generatedAt"),
                attribution = root.optString("attribution"),
                networks = networks,
            )
        }

        private fun JSONArray?.objectList(): List<JSONObject> =
            if (this == null) emptyList() else (0 until length()).map { getJSONObject(it) }

        private fun JSONArray?.stringList(): List<String> =
            if (this == null) emptyList() else (0 until length()).map { getString(it) }
    }
}
