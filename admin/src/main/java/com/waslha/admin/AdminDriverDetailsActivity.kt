package com.waslha.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Green = Color(0xFF0B805E)
private val Red = Color(0xFFB42318)
private val Muted = Color(0xFF6E7D76)
private val Ink = Color(0xFF14211C)
private val Bg = Color(0xFFF4F7F6)
private val White = Color.White

class AdminDriverDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        val id = intent.getStringExtra(EXTRA_DRIVER_ID).orEmpty()
        setContent { AdminDriverDetailsScreen(id) { finish() } }
    }
    companion object { const val EXTRA_DRIVER_ID = "driver_id" }
}

@Composable
private fun AdminDriverDetailsScreen(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var driver by remember { mutableStateOf<AdminDriverDto?>(null) }
    var trips by remember { mutableStateOf<List<AdminTripDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            message = null
            runCatching { AdminApiProvider.api.driver(id) }
                .onSuccess { if (it.success) driver = it.data else message = it.message }
                .onFailure { message = it.message ?: "تعذر الاتصال بالخادم" }
            runCatching { AdminApiProvider.api.driverTrips(id) }
                .onSuccess { if (it.success) trips = it.data.orEmpty() }
            loading = false
        }
    }

    LaunchedEffect(id) { reload() }

    MaterialTheme {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedButton(onClick = onBack) { Text("رجوع") } }
            item { Text("ملف الكابتن", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black) }
            if (loading && driver == null) item { CircularProgressIndicator() }
            message?.let { item { Text(it, color = Red, fontSize = 11.sp) } }
            driver?.let { d ->
                item {
                    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(d.name.ifBlank { d.id }, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            Text("المعرّف: ${d.id}", color = Muted, fontSize = 10.sp)
                            Text("الهاتف: ${d.phone ?: "غير متوفر"}", color = Ink, fontSize = 12.sp)
                            Text("المركبة: ${d.vehicle.ifBlank { "غير محددة" }} • ${d.plate.ifBlank { "بدون لوحة" }}", color = Ink, fontSize = 12.sp)
                            Text("الفئة: ${d.type} • التقييم: ${d.rating}", color = Ink, fontSize = 12.sp)
                            Text("الحالة: ${if (d.available) "متصل ومتاح" else "غير متصل"}", color = if (d.available) Green else Muted, fontWeight = FontWeight.Bold)
                            Text("آخر موقع: ${d.lat?.let { "%.5f".format(it) } ?: "غير متوفر"}, ${d.lng?.let { "%.5f".format(it) } ?: "غير متوفر"}", color = Muted, fontSize = 10.sp)
                            Text("آخر تحديث للموقع: ${d.lastLocationAt ?: 0L}", color = Muted, fontSize = 9.sp)
                            Text("إجمالي الرحلات: ${d.tripsCount} • النشطة: ${d.activeTripsCount}", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            scope.launch {
                                val result = runCatching { AdminApiProvider.api.setDriverAvailability(d.id, AdminAvailabilityRequest(!d.available)) }.getOrNull()
                                if (result?.success == true) reload() else message = result?.message ?: "تعذر تغيير الحالة"
                            }
                        }) { Text(if (d.available) "إيقاف التوفر" else "تفعيل التوفر") }
                        OutlinedButton(onClick = { reload() }) { Text("تحديث") }
                    }
                }
                item { Text("سجل رحلات الكابتن", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
                if (trips.isEmpty() && !loading) item { Text("لا توجد رحلات مسجلة لهذا الكابتن", color = Muted, fontSize = 11.sp) }
                items(trips, key = { it.id }) { trip ->
                    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(White)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("رحلة #${trip.id.takeLast(7)}", color = Ink, fontWeight = FontWeight.Black)
                            Text("الحالة: ${trip.status}", color = Muted, fontSize = 10.sp)
                            Text("الزبون: ${trip.customerId}", color = Muted, fontSize = 10.sp)
                            Text("المبلغ: ${trip.estimatedFare} ${trip.currency}", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
