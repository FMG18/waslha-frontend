package com.waslha.app

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle

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

    var routeResult by remember(pickup, destination) { mutableStateOf<RouteResult?>(null) }
    var routeLoading by remember(pickup, destination) { mutableStateOf(false) }
    var routeError by remember(pickup, destination) { mutableStateOf<String?>(null) }

    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(pickup.lng, pickup.lat))
            zoom(14.0)
            pitch(0.0)
            bearing(0.0)
        }
    }

    LaunchedEffect(pickup, destination, accessToken) {
        routeResult = null
        routeError = null
        if (destination == null) return@LaunchedEffect
        if (accessToken.isBlank() || accessToken.startsWith("YOUR_")) {
            routeError = "مفتاح الخريطة غير مهيأ"
            return@LaunchedEffect
        }

        routeLoading = true
        fetchMapboxRoute(pickup, destination, accessToken)
            .onSuccess { routeResult = it }
            .onFailure { routeError = it.message ?: "تعذر حساب المسار" }
        routeLoading = false
    }

    Box(modifier.background(Color(0xFFE7EFEB))) {
        if (accessToken.isBlank() || accessToken.startsWith("YOUR_")) {
            Card(
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                colors = CardDefaults.cardColors(Color.White),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Place, null, tint = Color(0xFFB42318), modifier = Modifier.size(40.dp))
                    Text("الخريطة غير مهيأة", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF10201B))
                    Text("سيظهر الموقع بعد إعداد مفتاح Mapbox.", modifier = Modifier.padding(top = 5.dp), fontSize = 11.sp, color = Color(0xFF72807B))
                }
            }
        } else {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = viewport,
                style = { MapboxStandardStyle() },
                onMapClickListener = {
                    val center = viewport.cameraState.center
                    onDestinationPicked(Coordinates(center.latitude(), center.longitude()))
                    true
                }
            ) {
                routeResult?.let { result ->
                    if (result.points.size >= 2) {
                        PolylineAnnotation(
                            points = result.points.map { Point.fromLngLat(it.lng, it.lat) }
                        ) {
                            lineColor = Color(0xFF078A60)
                            lineWidth = 5.0
                            lineOpacity = 0.95
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 14.dp)
                    .shadow(7.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .size(50.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Place, null, tint = Color(0xFFD93838), modifier = Modifier.size(36.dp))
            }

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (destination == null) "حرّك الخريطة وحدد وجهتك" else "تم تحديد الوجهة",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10201B)
                        )
                        Text("اضغط على المكان المطلوب لتثبيت النقطة", fontSize = 9.sp, color = Color(0xFF72807B))
                    }
                    IconButton(onClick = {
                        viewport.flyTo(
                            CameraOptions.Builder()
                                .center(Point.fromLngLat(pickup.lng, pickup.lat))
                                .zoom(15.0)
                                .pitch(0.0)
                                .bearing(0.0)
                                .build()
                        )
                    }) {
                        Icon(Icons.Default.MyLocation, "موقعي الحالي", tint = Color(0xFF078A60))
                    }
                }
            }

            when {
                routeLoading -> Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                    colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)),
                    shape = RoundedCornerShape(17.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(Modifier.padding(horizontal = 15.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF078A60))
                        Text("جاري حساب الطريق...", modifier = Modifier.padding(start = 9.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                routeResult != null -> {
                    val result = routeResult!!
                    val estimatedFare = calculateEstimatedFare(result.distanceKm, result.durationMin)
                    Card(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                        colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 17.dp, vertical = 13.dp)) {
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text("المسافة", fontSize = 9.sp, color = Color(0xFF72807B))
                                    Text("${result.distanceKm} كم", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF078A60))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("الوقت", fontSize = 9.sp, color = Color(0xFF72807B))
                                    Text("${result.durationMin} د", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF10201B))
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("الأجرة", fontSize = 9.sp, color = Color(0xFF72807B))
                                    Text("$estimatedFare ل.س", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF10201B))
                                }
                            }
                            Text("السعر تقديري قبل الطلب", modifier = Modifier.padding(top = 6.dp), fontSize = 8.sp, color = Color(0xFF8B9792))
                        }
                    }
                }
                routeError != null -> Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                    colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)),
                    shape = RoundedCornerShape(17.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Text(routeError!!, modifier = Modifier.padding(12.dp), fontSize = 10.sp, color = Color(0xFFB42318))
                }
            }
        }
    }
}
