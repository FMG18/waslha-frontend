package com.waslha.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import kotlin.math.roundToInt

private fun calculateEstimatedFare(distanceKm: Double, durationMin: Int): Int {
    val base = 2500.0
    val distancePart = distanceKm * 1200.0
    val timePart = durationMin * 70.0
    return ((base + distancePart + timePart) / 250.0).toInt() * 250
}

@Composable
fun WaslhaRideMap(
    pickup: Coordinates,
    destination: Coordinates?,
    modifier: Modifier = Modifier,
    onDestinationPicked: (Coordinates) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val accessToken = remember {
        context.resources.getString(R.string.mapbox_access_token).trim()
    }
    if (accessToken.isNotBlank() && !accessToken.startsWith("YOUR_")) {
        MapboxOptions.accessToken = accessToken
    }

    var routeResult by remember(destination, pickup) { mutableStateOf<RouteResult?>(null) }
    var routeLoading by remember(destination, pickup) { mutableStateOf(false) }
    var routeError by remember(destination, pickup) { mutableStateOf<String?>(null) }

    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(pickup.lng, pickup.lat))
            zoom(13.5)
            pitch(0.0)
            bearing(0.0)
        }
    }

    LaunchedEffect(pickup, destination, accessToken) {
        routeResult = null
        routeError = null
        if (destination != null && accessToken.isNotBlank() && !accessToken.startsWith("YOUR_")) {
            routeLoading = true
            fetchMapboxRoute(pickup, destination, accessToken)
                .onSuccess { routeResult = it }
                .onFailure { routeError = it.message ?: "تعذر حساب المسار" }
            routeLoading = false
        }
    }

    Box(modifier.background(Color(0xFFEAF1EE))) {
        if (accessToken.isBlank() || accessToken.startsWith("YOUR_")) {
            Card(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                colors = CardDefaults.cardColors(Color.White),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(
                    Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Box(
                        Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFFFF1EF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFB42318), modifier = Modifier.size(30.dp))
                    }
                    Text("الخريطة غير مهيأة", fontWeight = FontWeight.Black, color = Color(0xFF10201B), fontSize = 17.sp)
                    Text("سيتم تفعيل الخريطة بعد إعداد مفتاح Mapbox.", fontSize = 11.sp, color = Color(0xFF72807B))
                }
            }
        } else {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = viewport,
                style = { MapboxStandardStyle() },
                onMapClickListener = {
                    val center = viewport.cameraState?.center
                    if (center != null) {
                        onDestinationPicked(Coordinates(center.latitude(), center.longitude()))
                    }
                    true
                }
            ) {
                routeResult?.let { result ->
                    if (result.points.size >= 2) {
                        PolylineAnnotation(
                            points = result.points.map { Point.fromLngLat(it.lng, it.lat) }
                        ) {
                            lineColor = Color(0xFF087F5B)
                            lineWidth = 6.0
                            lineOpacity = 0.9
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MapControlButton(Icons.Default.Add, "تكبير") {
                    val center = viewport.cameraState?.center ?: Point.fromLngLat(pickup.lng, pickup.lat)
                    val currentZoom = viewport.cameraState?.zoom ?: 13.5
                    viewport.flyTo(CameraOptions.Builder().center(center).zoom((currentZoom + 1.0).coerceAtMost(19.0)).build())
                }
                MapControlButton(Icons.Default.Remove, "تصغير") {
                    val center = viewport.cameraState?.center ?: Point.fromLngLat(pickup.lng, pickup.lat)
                    val currentZoom = viewport.cameraState?.zoom ?: 13.5
                    viewport.flyTo(CameraOptions.Builder().center(center).zoom((currentZoom - 1.0).coerceAtLeast(8.0)).build())
                }
                MapControlButton(Icons.Default.MyLocation, "موقعي") {
                    viewport.flyTo(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(pickup.lng, pickup.lat))
                            .zoom(15.0)
                            .pitch(0.0)
                            .bearing(0.0)
                            .build()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .shadow(10.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFFFEFEC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Place, null, tint = Color(0xFFD93838), modifier = Modifier.size(27.dp))
                    }
                }
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFD93838)))
            }

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp, start = 18.dp, end = 18.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(if (destination == null) Color(0xFFFFF0ED) else Color(0xFFE7F6F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            tint = if (destination == null) Color(0xFFD93838) else Color(0xFF087F5B),
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(
                            if (destination == null) "حدد وجهتك" else "الوجهة محددة",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10201B)
                        )
                        Text(
                            if (destination == null) "حرّك الخريطة واجعل المؤشر فوق المكان المطلوب" else "يمكنك تحريك الخريطة لتعديل الموقع",
                            fontSize = 9.sp,
                            color = Color(0xFF72807B)
                        )
                    }
                }
            }

            routeResult?.let { result ->
                val estimatedFare = calculateEstimatedFare(result.distanceKm, result.durationMin)
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                    colors = CardDefaults.cardColors(Color.White.copy(alpha = .98f)),
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(7.dp)
                ) {
                    Column(Modifier.padding(horizontal = 17.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RouteMetric("المسافة", "${result.distanceKm} كم")
                            Box(Modifier.padding(horizontal = 8.dp).size(1.dp, 34.dp).background(Color(0xFFDDE5E1)))
                            RouteMetric("الوقت", "${result.durationMin} دقيقة")
                            Box(Modifier.padding(horizontal = 8.dp).size(1.dp, 34.dp).background(Color(0xFFDDE5E1)))
                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                Text("الأجرة التقديرية", fontSize = 9.sp, color = Color(0xFF72807B))
                                Text("$estimatedFare ل.س", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF10201B))
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4F8F6)).padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.MyLocation, null, tint = Color(0xFF087F5B), modifier = Modifier.size(14.dp))
                            Text("المسار محسوب عبر الطريق الأقرب", Modifier.padding(horizontal = 6.dp), fontSize = 9.sp, color = Color(0xFF6D7A75))
                        }
                    }
                }
            } ?: run {
                when {
                    routeLoading -> Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                        colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(5.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 15.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF087F5B))
                            Text("جاري حساب أفضل مسار...", Modifier.padding(start = 9.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10201B))
                        }
                    }
                    routeError != null -> Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                        colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)),
                        shape = RoundedCornerShape(18.dp),
                        elevation = CardDefaults.cardElevation(5.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFFB42318), modifier = Modifier.size(17.dp))
                            Text("تعذر حساب المسار. جرّب نقطة أخرى.", Modifier.padding(start = 7.dp), fontSize = 10.sp, color = Color(0xFFB42318), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(46.dp)
            .shadow(5.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E9E5), CircleShape)
    ) {
        Icon(icon, contentDescription, tint = Color(0xFF087F5B), modifier = Modifier.size(21.dp))
    }
}

@Composable
private fun RouteMetric(label: String, value: String) {
    Column {
        Text(label, fontSize = 9.sp, color = Color(0xFF72807B))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF10201B))
    }
}

private fun fetchMapboxRoute(
    origin: Coordinates,
    destination: Coordinates,
    accessToken: String
): RouteResult? = null

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
