package com.waslha.captain

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle

private val FallbackDamascus = Point.fromLngLat(36.2765, 33.5138)

@Composable
fun CaptainLiveMap(
    modifier: Modifier = Modifier,
    driver: Coordinates?,
    pickup: Coordinates? = null,
    destination: Coordinates? = null,
) {
    val context = LocalContext.current
    val accessToken = remember { context.resources.getString(R.string.mapbox_access_token).trim() }
    val center = driver?.let { Point.fromLngLat(it.lng, it.lat) } ?: pickup?.let { Point.fromLngLat(it.lng, it.lat) } ?: FallbackDamascus

    if (accessToken.isNotBlank() && !accessToken.startsWith("YOUR_")) {
        MapboxOptions.accessToken = accessToken
    }

    if (accessToken.isBlank() || accessToken.startsWith("YOUR_")) {
        Box(modifier.background(Color(0xFFE8EFEB)), contentAlignment = Alignment.Center) {
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("الخريطة", color = Color(0xFF075B43), fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        return
    }

    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(center)
            zoom(14.0)
            pitch(0.0)
            bearing(0.0)
        }
    }

    MapboxMap(
        modifier = modifier,
        mapViewportState = viewport,
        style = { MapboxStandardStyle() }
    ) {
        driver?.let {
            CircleAnnotation(point = Point.fromLngLat(it.lng, it.lat)) {
                circleRadius = 9.0
                circleColor = Color(0xFF0B805E)
                circleStrokeWidth = 3.0
                circleStrokeColor = Color.White
            }
        }
        pickup?.let {
            CircleAnnotation(point = Point.fromLngLat(it.lng, it.lat)) {
                circleRadius = 8.0
                circleColor = Color(0xFF075B43)
                circleStrokeWidth = 2.0
                circleStrokeColor = Color.White
            }
        }
        destination?.let {
            CircleAnnotation(point = Point.fromLngLat(it.lng, it.lat)) {
                circleRadius = 8.0
                circleColor = Color(0xFFB42318)
                circleStrokeWidth = 2.0
                circleStrokeColor = Color.White
            }
        }
    }
}
