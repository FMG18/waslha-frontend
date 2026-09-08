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
import com.mapbox.maps.extension.compose.MapEffect
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.cameraOptions
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.EdgeInsets

@Composable
fun WaslhaRideMap(
    pickup: Coordinates,
    destination: Coordinates?,
    modifier: Modifier = Modifier,
    onDestinationPicked: (Coordinates) -> Unit = {}
) {
    val accessToken = androidx.compose.ui.platform.LocalContext.current
        .resources.getString(R.string.mapbox_access_token)
    var routeResult by remember(destination, pickup) { mutableStateOf<RouteResult?>(null) }
    var routeLoading by remember(destination, pickup) { mutableStateOf(false) }
    var routeError by remember(destination, pickup) { mutableStateOf<String?>(null) }
    var currentCenter by remember(pickup, destination) {
        mutableStateOf(destination ?: pickup)
    }

    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(currentCenter.lng, currentCenter.lat))
            zoom(13.5)
            pitch(0.0)
            bearing(0.0)
        }
    }

    LaunchedEffect(pickup, destination, accessToken) {
        currentCenter = destination ?: pickup
        routeResult = null
        routeError = null
        if (destination != null) {
            routeLoading = true
            fetchMapboxRoute(pickup, destination, accessToken)
                .onSuccess { result -> routeResult = result }
                .onFailure { routeError = it.message }
            routeLoading = false
        }
    }

    Box(modifier.background(Color(0xFFE7EFEB))) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewport,
            style = { MapboxStandardStyle() },
            onMapClickListener = {
                val center = viewport.cameraState.center
                currentCenter = Coordinates(center.latitude(), center.longitude())
                onDestinationPicked(currentCenter)
                true
            }
        )

        MapEffect(Unit) { mapView ->
            mapView.location.enabled = true
            mapView.gestures.rotateEnabled = true
            mapView.gestures.pitchEnabled = false
            mapView.camera.cancelAllAnimations()
        }

        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 12.dp)
                .shadow(6.dp, CircleShape)
                .background(Color.White, CircleShape)
                .size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Place, null, tint = Color(0xFFD93838), modifier = Modifier.size(34.dp))
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp, start = 18.dp, end = 18.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(Color.White.copy(alpha = .96f)),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (destination == null) "حدد وجهتك على الخريطة" else "تم تحديد الوجهة",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10201B)
                    )
                    Text(
                        "حرّك الخريطة حتى يصبح المكان المطلوب تحت الدبوس",
                        fontSize = 9.sp,
                        color = Color(0xFF72807B)
                    )
                }
                IconButton(
                    onClick = {
                        viewport.easeTo(
                            cameraOptions {
                                center(Point.fromLngLat(pickup.lng, pickup.lat))
                                zoom(15.0)
                                pitch(0.0)
                                bearing(0.0)
                            }
                        )
                        currentCenter = pickup
                    }
                ) {
                    Icon(Icons.Default.MyLocation, "موقعي الحالي", tint = Color(0xFF078A60))
                }
            }
        }

        routeResult?.let { result ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
                colors = CardDefaults.cardColors(Color.White.copy(alpha = .96f)),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Row(Modifier.padding(horizontal = 18.dp, vertical = 11.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("المسافة", fontSize = 9.sp, color = Color(0xFF72807B))
                        Text("${result.distanceKm} كم", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF078A60))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("الوقت التقريبي", fontSize = 9.sp, color = Color(0xFF72807B))
                        Text("${result.durationMin} دقيقة", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color(0xFF10201B))
                    }
                }
            }
        } ?: run {
            if (routeLoading) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                    colors = CardDefaults.cardColors(Color.White.copy(alpha = .96f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(Modifier.padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF078A60))
                        Text(" نحسب أقرب طريق...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (routeError != null) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                    colors = CardDefaults.cardColors(Color.White.copy(alpha = .96f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "تعذر حساب المسار. جرّب نقطة أخرى.",
                        modifier = Modifier.padding(11.dp),
                        fontSize = 10.sp,
                        color = Color(0xFFB42318)
                    )
                }
            }
        }
    }
}
