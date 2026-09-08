package com.waslha.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.rememberIconImage
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle

@Composable
fun WaslhaRideMap(
    pickup: Coordinates,
    destination: Coordinates?,
    modifier: Modifier = Modifier,
    onDestinationPicked: (Coordinates) -> Unit = {}
) {
    val accessToken = androidx.compose.ui.platform.LocalContext.current
        .resources.getString(R.string.mapbox_access_token)
    var routePoints by remember(destination, pickup) { mutableStateOf<List<Coordinates>>(emptyList()) }
    var routeLoading by remember(destination, pickup) { mutableStateOf(false) }
    var routeError by remember(destination, pickup) { mutableStateOf<String?>(null) }

    LaunchedEffect(pickup, destination, accessToken) {
        routePoints = emptyList()
        routeError = null
        if (destination != null) {
            routeLoading = true
            fetchMapboxRoute(pickup, destination, accessToken)
                .onSuccess { result -> routePoints = result.points }
                .onFailure { routeError = it.message }
            routeLoading = false
        }
    }

    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(pickup.lng, pickup.lat))
            zoom(13.5)
            pitch(0.0)
            bearing(0.0)
        }
    }
    val pinIcon = rememberIconImage(
        key = "waslha-map-pin",
        painter = painterResource(R.drawable.map_pin)
    )

    Box(modifier.background(Color(0xFFE7EFEB))) {
        MapboxMap(
            modifier = Modifier.fillMaxSize(),
            mapViewportState = viewport,
            onMapClickListener = { clickedPoint ->
                onDestinationPicked(
                    Coordinates(clickedPoint.latitude(), clickedPoint.longitude())
                )
                true
            },
            style = { MapboxStandardStyle() }
        ) {
            PointAnnotation(point = Point.fromLngLat(pickup.lng, pickup.lat)) {
                iconImage = pinIcon
                iconSize = 0.8
            }
            destination?.let { target ->
                PointAnnotation(point = Point.fromLngLat(target.lng, target.lat)) {
                    iconImage = pinIcon
                    iconSize = 0.85
                }
            }
            if (routePoints.size >= 2) {
                PolylineAnnotation(
                    points = routePoints.map { Point.fromLngLat(it.lng, it.lat) }
                ) {
                    lineColor = Color(0xFF078A60)
                    lineWidth = 5.0
                }
            }
        }

        Card(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 14.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "اضغط على الخريطة لتحديد الوجهة",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10201B)
            )
        }

        if (routeLoading) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(bottom = 5.dp),
                        strokeWidth = 2.dp,
                        color = Color(0xFF078A60)
                    )
                    Text("نحسب المسار...", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else if (routeError != null) {
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "تعذر حساب المسار، تحقق من إعداد Mapbox",
                    modifier = Modifier.padding(11.dp),
                    fontSize = 10.sp,
                    color = Color(0xFFB42318)
                )
            }
        }
    }
}
