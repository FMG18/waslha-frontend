package com.waslha.app

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.math.roundToInt

data class RouteResult(
    val points: List<Coordinates>,
    val distanceKm: Double,
    val durationMin: Int
)

suspend fun fetchMapboxRoute(
    origin: Coordinates,
    destination: Coordinates,
    accessToken: String
): Result<RouteResult> = withContext(Dispatchers.IO) {
    if (accessToken.isBlank() || accessToken.startsWith("YOUR_")) {
        return@withContext Result.failure(IllegalStateException("Mapbox access token is not configured"))
    }

    runCatching {
        val coordinates = "${origin.lng},${origin.lat};${destination.lng},${destination.lat}"
        val url = "https://api.mapbox.com/directions/v5/mapbox/driving/$coordinates?alternatives=false&overview=full&geometries=geojson&access_token=$accessToken"
        val request = Request.Builder().url(url).get().build()
        OkHttpClient().newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Mapbox Directions HTTP ${response.code}")
            val body = response.body?.string().orEmpty()
            val root = JsonParser.parseString(body).asJsonObject
            val routes = root.getAsJsonArray("routes")
            if (routes == null || routes.size() == 0) error("No route found")
            val route = routes[0].asJsonObject
            val geometry = route.getAsJsonObject("geometry")
            val coords = geometry.getAsJsonArray("coordinates")
            val points = buildList {
                for (entry in coords) {
                    val pair = entry.asJsonArray
                    add(Coordinates(pair[1].asDouble, pair[0].asDouble))
                }
            }
            RouteResult(
                points = points,
                distanceKm = (route.get("distance").asDouble / 1000.0 * 10.0).roundToInt() / 10.0,
                durationMin = (route.get("duration").asDouble / 60.0).roundToInt()
            )
        }
    }
}
