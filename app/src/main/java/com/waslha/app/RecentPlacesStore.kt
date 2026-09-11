package com.waslha.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class RecentPlace(val name: String, val address: String, val coordinates: Coordinates)

class RecentPlacesStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("waslha_recent_places", Context.MODE_PRIVATE)

    fun list(): List<RecentPlace> = runCatching {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val array = JSONArray(raw)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(RecentPlace(
                    name = item.optString("name"),
                    address = item.optString("address"),
                    coordinates = Coordinates(item.optDouble("lat"), item.optDouble("lng"))
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun add(place: RecentPlace) {
        val cleaned = place.copy(
            name = place.name.trim().take(100),
            address = place.address.trim().take(180)
        )
        val updated = list().filterNot { sameCoordinates(it.coordinates, cleaned.coordinates) }.toMutableList()
        updated.add(0, cleaned)
        save(updated.take(8))
    }

    fun clear() = prefs.edit().remove("items").apply()

    private fun save(items: List<RecentPlace>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(JSONObject().apply {
                put("name", item.name)
                put("address", item.address)
                put("lat", item.coordinates.lat)
                put("lng", item.coordinates.lng)
            })
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    private fun sameCoordinates(a: Coordinates, b: Coordinates): Boolean =
        kotlin.math.abs(a.lat - b.lat) < 0.0001 && kotlin.math.abs(a.lng - b.lng) < 0.0001
}
