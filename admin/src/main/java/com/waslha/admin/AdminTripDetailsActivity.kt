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

class AdminTripDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        val id = intent.getStringExtra(EXTRA_TRIP_ID).orEmpty()
        setContent { AdminTripDetailsScreen(id) { finish() } }
    }
    companion object { const val EXTRA_TRIP_ID = "trip_id" }
}

@Composable
private fun AdminTripDetailsScreen(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var trip by remember { mutableStateOf<AdminTripDto?>(null) }
    var drivers by remember { mutableStateOf<List<AdminDriverDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            message = null
            runCatching { AdminApiProvider.api.trip(id) }.onSuccess { if (it.success) trip = it.data else message = it.message }
            runCatching { AdminApiProvider.api.drivers() }.onSuccess { if (it.success) drivers = it.data.orEmpty() }
            loading = false
        }
    }
    LaunchedEffect(id) { reload() }

    MaterialTheme {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedButton(onClick = onBack) { Text("رجوع") } }
            item { Text("تفاصيل الرحلة", fontSize = 28.sp, fontWeight = FontWeight.Black) }
            if (loading && trip == null) item { CircularProgressIndicator() }
            message?.let { item { Text(it, color = Color(0xFFB42318), fontSize = 11.sp) } }
            trip?.let { t ->
                item {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("رحلة #${t.id.takeLast(8)}", fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text("الحالة: ${t.status}")
                        Text("الزبون: ${t.customerId}")
                        Text("الكابتن: ${t.driver?.name ?: "غير معين"}")
                        Text("المبلغ: ${t.estimatedFare} ${t.currency}")
                        Text("المسافة: ${t.distanceKm} كم • ${t.durationMin} دقيقة")
                        Text("الدفع: ${t.paymentMethod}")
                    }
                }
                if (t.status == "searching") {
                    item { Text("تعيين كابتن", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    items(drivers.filter { it.available && it.type == t.vehicleType }, key = { it.id }) { d ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(d.name.ifBlank { d.id }, fontWeight = FontWeight.Bold)
                                Text("${d.type} • ${d.rating}", fontSize = 9.sp)
                            }
                            Button(onClick = {
                                scope.launch {
                                    loading = true
                                    val result = runCatching { AdminApiProvider.api.assignDriver(id, AdminDriverAssignRequest(d.id)) }.getOrNull()
                                    if (result?.success == true) reload() else message = result?.message ?: "تعذر تعيين الكابتن"
                                    loading = false
                                }
                            }) { Text("تعيين") }
                        }
                    }
                }
                if (t.status == "driver_assigned") item { Button(onClick = { scope.launch { AdminApiProvider.api.updateStatus(id, AdminStatusRequest("arriving")); reload() } }) { Text("تحويل إلى: الكابتن في الطريق") } }
                if (t.status == "arriving") item { Button(onClick = { scope.launch { AdminApiProvider.api.updateStatus(id, AdminStatusRequest("in_progress")); reload() } }) { Text("بدء الرحلة") } }
                if (t.status == "in_progress") item { Button(onClick = { scope.launch { AdminApiProvider.api.updateStatus(id, AdminStatusRequest("completed")); reload() } }) { Text("إنهاء الرحلة") } }
                if (t.status !in setOf("completed", "cancelled")) item {
                    OutlinedButton(onClick = { scope.launch { AdminApiProvider.api.cancelTrip(id, AdminCancelRequest()); reload() } }) { Text("إلغاء الرحلة", color = Color(0xFFB42318)) }
                }
            }
        }
    }
}
