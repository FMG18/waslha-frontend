package com.waslha.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val accessToken = remember { context.resources.getString(R.string.mapbox_access_token).trim() }
    if (accessToken.isNotBlank() && !accessToken.startsWith("YOUR_")) MapboxOptions.accessToken = accessToken

    var routeResult by remember(destination, pickup) { mutableStateOf<RouteResult?>(null) }
    var routeLoading by remember(destination, pickup) { mutableStateOf(false) }
    var routeError by remember(destination, pickup) { mutableStateOf<String?>(null) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceSearchDto>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val recentPrefs = remember { context.getSharedPreferences("waslha_recent_places", Context.MODE_PRIVATE) }
    var recent by remember { mutableStateOf(loadRecentPlaces(recentPrefs)) }
    val repository = remember { PlaceSearchRepository(ApiProvider.api) }

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

    fun runSearch(text: String) {
        searchJob?.cancel()
        val normalized = text.trim()
        query = text
        searchError = null
        if (normalized.length < 2) {
            results = emptyList()
            searching = false
            return
        }
        searching = true
        searchJob = scope.launch {
            delay(300)
            repository.search(normalized)
                .onSuccess { results = it }
                .onFailure {
                    results = emptyList()
                    searchError = it.message ?: "تعذر البحث عن العنوان"
                }
            searching = false
        }
    }

    fun selectPlace(place: PlaceSearchDto) {
        onDestinationPicked(place.coordinates)
        recent = saveRecentPlace(
            recentPrefs,
            RecentPlace(place.name, place.address, place.coordinates.lat, place.coordinates.lng),
            recent
        )
        searchOpen = false
        query = ""
        results = emptyList()
        keyboard?.hide()
        viewport.flyTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(place.coordinates.lng, place.coordinates.lat))
                .zoom(15.0)
                .build()
        )
    }

    Box(modifier.background(Color(0xFFEAF1EE))) {
        if (accessToken.isBlank() || accessToken.startsWith("YOUR_")) {
            Card(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                colors = CardDefaults.cardColors(Color.White),
                shape = RoundedCornerShape(22.dp),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Color(0xFFFFF1EF)), contentAlignment = Alignment.Center) {
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
                    if (center != null) onDestinationPicked(Coordinates(center.latitude(), center.longitude()))
                    true
                }
            ) {
                routeResult?.let { result ->
                    if (result.points.size >= 2) {
                        PolylineAnnotation(points = result.points.map { Point.fromLngLat(it.lng, it.lat) }) {
                            lineColor = Color(0xFF087F5B)
                            lineWidth = 6.0
                            lineOpacity = 0.9
                        }
                    }
                }
            }

            Column(Modifier.align(Alignment.CenterEnd).padding(end = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    viewport.flyTo(CameraOptions.Builder().center(Point.fromLngLat(pickup.lng, pickup.lat)).zoom(15.0).pitch(0.0).bearing(0.0).build())
                }
            }

            Card(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp, start = 18.dp, end = 18.dp).fillMaxWidth().clickable { searchOpen = true },
                colors = CardDefaults.cardColors(Color.White.copy(alpha = .98f)),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE7F6F0)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Search, null, tint = Color(0xFF087F5B), modifier = Modifier.size(21.dp))
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                        Text(if (destination == null) "ابحث عن وجهتك" else "الوجهة محددة", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF10201B))
                        Text(if (destination == null) "ابحث باسم المكان أو استخدم المؤشر" else "يمكنك البحث لتغيير الوجهة", fontSize = 9.sp, color = Color(0xFF72807B))
                    }
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF087F5B), modifier = Modifier.size(20.dp))
                }
            }

            Column(Modifier.align(Alignment.Center).padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(50.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFFFEFEC)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Place, null, tint = Color(0xFFD93838), modifier = Modifier.size(27.dp))
                    }
                }
                Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFD93838)))
            }

            routeResult?.let { result ->
                val estimatedFare = calculateEstimatedFare(result.distanceKm, result.durationMin)
                Card(Modifier.align(Alignment.BottomCenter).padding(14.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = .98f)), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(7.dp)) {
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
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFF4F8F6)).padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, null, tint = Color(0xFF087F5B), modifier = Modifier.size(14.dp))
                            Text("المسار محسوب عبر الطريق الأقرب", Modifier.padding(horizontal = 6.dp), fontSize = 9.sp, color = Color(0xFF6D7A75))
                        }
                    }
                }
            } ?: run {
                when {
                    routeLoading -> Card(Modifier.align(Alignment.BottomCenter).padding(14.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(5.dp)) {
                        Row(Modifier.padding(horizontal = 15.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF087F5B))
                            Text("جاري حساب أفضل مسار...", Modifier.padding(start = 9.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10201B))
                        }
                    }
                    routeError != null -> Card(Modifier.align(Alignment.BottomCenter).padding(14.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(5.dp)) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFFB42318), modifier = Modifier.size(17.dp))
                            Text("تعذر حساب المسار. جرّب نقطة أخرى.", Modifier.padding(start = 7.dp), fontSize = 10.sp, color = Color(0xFFB42318), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (searchOpen) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .22f)))
                Card(Modifier.align(Alignment.TopCenter).padding(top = 12.dp, start = 12.dp, end = 12.dp).fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(10.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { searchOpen = false; keyboard?.hide() }) { Icon(Icons.Default.Close, "إغلاق", tint = Color(0xFF10201B)) }
                            OutlinedTextField(
                                value = query,
                                onValueChange = ::runSearch,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                placeholder = { Text("ابحث عن مكان في سوريا", fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF087F5B)) },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); runSearch(query) })
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        when {
                            searching -> Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = Color(0xFF087F5B)) }
                            searchError != null -> Text(searchError!!, color = Color(0xFFB42318), fontSize = 11.sp, modifier = Modifier.padding(10.dp))
                            results.isNotEmpty() -> LazyColumn(Modifier.fillMaxWidth().height(360.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(results, key = { it.id }) { place ->
                                    PlaceResultRow(place, onClick = { selectPlace(place) })
                                }
                            }
                            query.trim().length >= 2 -> Text("لا توجد نتائج لهذا البحث", color = Color(0xFF72807B), fontSize = 11.sp, modifier = Modifier.padding(14.dp))
                            recent.isNotEmpty() -> {
                                Text("آخر الأماكن", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF10201B), modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp))
                                LazyColumn(Modifier.fillMaxWidth().height(250.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(recent, key = { "recent_${it.name}_${it.lat}_${it.lng}" }) { place ->
                                        RecentPlaceRow(place, onClick = {
                                            selectPlace(PlaceSearchDto("recent", place.name, place.address, Coordinates(place.lat, place.lng)))
                                        })
                                    }
                                }
                            }
                            else -> Text("اكتب اسم المكان أو المنطقة للبحث عنه", color = Color(0xFF72807B), fontSize = 11.sp, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaceResultRow(place: PlaceSearchDto, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).background(Color(0xFFF7F9F8)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE7F6F0)), contentAlignment = Alignment.Center) { Icon(Icons.Default.LocationOn, null, tint = Color(0xFF087F5B), modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF10201B))
            Text(place.address, fontSize = 9.sp, color = Color(0xFF72807B), maxLines = 2)
        }
    }
}

@Composable
private fun RecentPlaceRow(place: RecentPlace, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).background(Color(0xFFF7F9F8)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF0F3F1)), contentAlignment = Alignment.Center) { Icon(Icons.Default.History, null, tint = Color(0xFF6D7B76), modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(place.name, fontWeight = FontWeight.Black, fontSize = 12.sp, color = Color(0xFF10201B))
            Text(place.address, fontSize = 9.sp, color = Color(0xFF72807B), maxLines = 2)
        }
    }
}

@Composable
private fun MapControlButton(icon: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(46.dp).shadow(5.dp, CircleShape).clip(CircleShape).background(Color.White).border(1.dp, Color(0xFFE2E9E5), CircleShape)) {
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

data class RecentPlace(val name: String, val address: String, val lat: Double, val lng: Double)

private fun loadRecentPlaces(prefs: android.content.SharedPreferences): List<RecentPlace> {
    val raw = prefs.getString("items", "").orEmpty()
    if (raw.isBlank()) return emptyList()
    return raw.split("\n").mapNotNull { line ->
        val parts = line.split("|", limit = 4)
        if (parts.size != 4) return@mapNotNull null
        val lat = parts[2].toDoubleOrNull() ?: return@mapNotNull null
        val lng = parts[3].toDoubleOrNull() ?: return@mapNotNull null
        RecentPlace(parts[0], parts[1], lat, lng)
    }.take(8)
}

private fun saveRecentPlace(
    prefs: android.content.SharedPreferences,
    place: RecentPlace,
    current: List<RecentPlace>
): List<RecentPlace> {
    val filtered = current.filterNot { it.lat == place.lat && it.lng == place.lng }
    val updated = listOf(place) + filtered
    prefs.edit().putString("items", updated.take(8).joinToString("\n") { "${it.name.replace("|", " ")}|${it.address.replace("|", " ")}|${it.lat}|${it.lng}" }).apply()
    return updated.take(8)
}

class PlaceSearchRepository(private val api: WaslhaApi) {
    suspend fun search(query: String): Result<List<PlaceSearchDto>> = runCatching {
        val response = api.searchPlaces(query)
        require(response.success && response.data != null) { response.message ?: "تعذر البحث عن العنوان" }
        response.data
    }
}
