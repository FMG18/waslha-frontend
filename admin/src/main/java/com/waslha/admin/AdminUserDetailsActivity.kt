package com.waslha.admin

import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Green = Color(0xFF0B805E)
private val Red = Color(0xFFB42318)
private val Amber = Color(0xFFB54708)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val Bg = Color(0xFFF4F7F6)
private val White = Color.White

class AdminUserDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        val id = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
        setContent { AdminUserDetailsScreen(id) { finish() } }
    }
    companion object { const val EXTRA_USER_ID = "user_id" }
}

@Composable
private fun AdminUserDetailsScreen(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var user by remember { mutableStateOf<AdminUserDetailsDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            message = null
            runCatching { AdminApiProvider.api.user(id) }
                .onSuccess { if (it.success) user = it.data else message = it.message }
                .onFailure { message = it.message ?: "تعذر الاتصال بالخادم" }
            loading = false
        }
    }

    LaunchedEffect(id) { reload() }

    MaterialTheme {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { OutlinedButton(onClick = onBack) { Text("رجوع") } }
            item { Text("تفاصيل الزبون", fontSize = 28.sp, fontWeight = FontWeight.Black) }
            if (loading && user == null) item { CircularProgressIndicator() }
            message?.let { item { Text(it, color = Red, fontSize = 11.sp) } }

            user?.let { u ->
                val trips = u.trips
                val active = trips.count { it.status in setOf("searching", "driver_assigned", "arriving", "in_progress") }
                val completed = trips.count { it.status == "completed" }
                val cancelled = trips.count { it.status == "cancelled" }
                val spent = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }

                item {
                    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(u.name.ifBlank { "زبون ${u.id.takeLast(6)}" }, color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
                            Text("المعرّف: ${u.id}", color = Muted, fontSize = 10.sp)
                            Text("الهاتف: ${u.phone.ifBlank { "غير متوفر" }}", color = Ink, fontSize = 12.sp)
                            Text("البريد: ${u.email.ifBlank { "غير متوفر" }}", color = Ink, fontSize = 12.sp)
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStat("الكل", trips.size.toString(), Ink, Modifier.weight(1f))
                        MiniStat("نشطة", active.toString(), Green, Modifier.weight(1f))
                        MiniStat("مكتملة", completed.toString(), Green, Modifier.weight(1f))
                        MiniStat("ملغاة", cancelled.toString(), Red, Modifier.weight(1f))
                    }
                }

                item {
                    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(White)) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text("إجمالي المدفوع للرحلات المكتملة", color = Muted, fontSize = 10.sp)
                            Text("$spent ل.س", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                item { Text("سجل الرحلات", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Black) }
                if (trips.isEmpty()) item { Text("لا توجد رحلات لهذا الزبون", color = Muted, fontSize = 11.sp) }
                items(trips, key = { it.id }) { trip ->
                    Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(White)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("رحلة #${trip.id.takeLast(7)}", color = Ink, fontWeight = FontWeight.Black)
                                Text(trip.status, color = tripStatusColor(trip.status), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("الكابتن: ${trip.driver?.name ?: "غير معين"}", color = Muted, fontSize = 10.sp)
                            Text("${trip.vehicleType} • ${trip.paymentMethod}", color = Muted, fontSize = 9.sp)
                            Text("${trip.estimatedFare} ${trip.currency}", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { context.startActivity(Intent(context, AdminOperationsActivity::class.java)) }, Modifier.weight(1f)) { Text("عمليات الإدارة") }
                        OutlinedButton(onClick = { reload() }, Modifier.weight(1f)) { Text("تحديث") }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(title: String, value: String, accent: Color, modifier: Modifier) {
    Card(modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(White)) {
        Column(Modifier.padding(10.dp)) {
            Text(title, color = Muted, fontSize = 8.sp)
            Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black)
        }
    }
}

private fun tripStatusColor(status: String): Color = when {
    status in setOf("completed") -> Green
    status in setOf("cancelled") -> Red
    status in setOf("searching", "driver_assigned", "arriving", "in_progress") -> Amber
    else -> Muted
}
