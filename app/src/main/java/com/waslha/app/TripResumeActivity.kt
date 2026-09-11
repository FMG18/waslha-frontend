package com.waslha.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class TripResumeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val session = SessionStore(this)
        val repository = TripRepository(ApiProvider.api)
        val activeId = session.activeTripId

        setContent {
            WaslhaTheme {
                var trip by remember { mutableStateOf<Trip?>(null) }
                var error by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(activeId) {
                    if (activeId.isNullOrBlank()) {
                        finish()
                        return@LaunchedEffect
                    }
                    repository.get(activeId)
                        .onSuccess { loaded ->
                            trip = loaded
                            if (loaded.status == "completed" || loaded.status == "cancelled") finish()
                        }
                        .onFailure { error = it.message ?: "تعذر استعادة الرحلة" }
                }

                val current = trip
                if (current == null && error == null) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = Color(0xFF087F5B))
                        Spacer(Modifier.height(14.dp))
                        Text("جاري استعادة رحلتك…", fontWeight = FontWeight.Bold)
                    }
                } else if (current != null) {
                    Column(Modifier.fillMaxSize().background(Color(0xFFF5F8F6)).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { finish() }) { Icon(Icons.Default.ArrowBack, "رجوع") }
                            Text("استعادة الرحلة", Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Black)
                            IconButton(onClick = { shareTrip(current) }) { Icon(Icons.Default.Share, "مشاركة الرحلة") }
                        }
                        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(0.dp)) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF087F5B))
                                    Spacer(Modifier.height(1.dp))
                                    Text("${current.statusLabel()}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF10201B))
                                }
                                Text("الرحلة: ${current.id}", fontSize = 11.sp, color = Color(0xFF72807B))
                                Text("الأجرة التقديرية: ${current.estimatedFare} ${current.currency}", fontWeight = FontWeight.Bold)
                                Text("المسافة: ${current.distanceKm} كم • ${current.durationMin} دقيقة", fontSize = 12.sp, color = Color(0xFF72807B))
                                current.driver?.let { driver ->
                                    Text("الكابتن: ${driver.name} • ${driver.vehicle}", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { openTaxi() }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                            Text("متابعة الرحلة", fontWeight = FontWeight.Black)
                        }
                        error?.let { Text(it, color = Color(0xFFB42318), fontSize = 11.sp) }
                    }
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(error ?: "لا توجد رحلة نشطة", color = Color(0xFFB42318), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { finish() }) { Text("العودة") }
                    }
                }
            }
        }
    }

    private fun openTaxi() {
        startActivity(Intent(this, TaxiBookingActivity::class.java))
        finish()
    }

    private fun shareTrip(trip: Trip) {
        val driver = trip.driver?.let { "الكابتن: ${it.name} • ${it.vehicle}" } ?: "الكابتن: جارٍ التعيين"
        val body = "رحلتي على وصلها\n${trip.statusLabel()}\nرقم الرحلة: ${trip.id}\n$driver\nالأجرة التقديرية: ${trip.estimatedFare} ${trip.currency}"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }, "مشاركة الرحلة"))
    }
}
