package com.waslha.captain

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val G = Color(0xFF0B805E)
private val GD = Color(0xFF075B43)
private val M = Color(0xFFE8F5F0)
private val BG = Color(0xFFF4F7F6)
private val INK = Color(0xFF14211C)
private val MUTED = Color(0xFF6E7D76)
private val WHITE = Color.White
private val RED = Color(0xFFB42318)

class CaptainTripActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        val id = intent.getStringExtra(EXTRA_TRIP_ID).orEmpty()
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize(), color = BG) { TripScreen(id, { finish() }) } } }
    }
    companion object { const val EXTRA_TRIP_ID = "trip_id" }
}

@Composable
private fun TripScreen(id: String, onFinish: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var trip by remember { mutableStateOf<Trip?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    suspend fun refresh() {
        if (id.isBlank()) { error = "معرّف الرحلة غير صالح"; return }
        runCatching { CaptainApiProvider.api.trip(id) }
            .onSuccess { trip = it.data; if (it.data?.status == "completed") onFinish() }
            .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
    }

    LaunchedEffect(id) { refresh(); while (true) { delay(5000); refresh() } }

    val t = trip
    if (t == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (error == null) CircularProgressIndicator(color = G)
            else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(error ?: "تعذر تحميل الرحلة", color = INK, fontWeight = FontWeight.Bold)
                Text("إعادة المحاولة", color = WHITE, modifier = Modifier.background(G, RoundedCornerShape(15.dp)).clickable { scope.launch { refresh() } }.padding(14.dp))
            }
        }
        return
    }

    val next = when (t.status) { "driver_assigned" -> "arriving"; "arriving" -> "in_progress"; "in_progress" -> "completed"; else -> null }
    val label = when (t.status) { "driver_assigned" -> "أنا في الطريق إلى الراكب"; "arriving" -> "وصلت إلى الراكب"; "in_progress" -> "إنهاء الرحلة"; else -> "الرحلة مكتملة" }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(WHITE, CircleShape).clickable { onFinish() }, contentAlignment = Alignment.Center) { Text("‹", fontSize = 27.sp, color = INK) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) { Text("الرحلة الحالية", color = MUTED, fontSize = 10.sp); Text("#${t.id.takeLast(6)}", color = INK, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                Surface(color = M, shape = RoundedCornerShape(999.dp)) { Text("نشطة", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = G, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
            }
        }
        item { MapCard(t) }
        item { Progress(t.status) }
        item { Details(t) }
        item {
            Text("إدارة الحساب", color = INK, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(7.dp))
            Box(
                Modifier.fillMaxWidth().background(M, RoundedCornerShape(17.dp)).clickable {
                    context.startActivity(Intent(context, CaptainEarningsActivity::class.java))
                }.padding(14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text("الأرباح وسجل الرحلات", color = GD, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("راجع رحلاتك المكتملة ودخلك من مكان واحد", color = MUTED, fontSize = 9.sp)
                }
            }
        }
        error?.let { item { Surface(Modifier.fillMaxWidth(), color = Color(0xFFFFE9E7), shape = RoundedCornerShape(14.dp)) { Text(it, Modifier.padding(12.dp), color = RED, fontSize = 10.sp) } } }
        item {
            Box(Modifier.fillMaxWidth().background(G, RoundedCornerShape(18.dp)).clickable(enabled = !busy && next != null) {
                busy = true; error = null; scope.launch {
                    runCatching { CaptainApiProvider.api.updateTripStatus(id, TripStatusRequest(next ?: "")) }
                        .onSuccess { trip = it.data; if (next == "completed") onFinish() }
                        .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                    busy = false
                }
            }.padding(16.dp), contentAlignment = Alignment.Center) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = WHITE, strokeWidth = 2.dp)
                else Text(label, color = WHITE, fontSize = 13.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable private fun MapCard(t: Trip) {
    Card(Modifier.fillMaxWidth().height(220.dp), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(Color(0xFFE7EEEA))) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.size(26.dp).background(G, CircleShape).align(Alignment.CenterStart).padding(start = 30.dp))
            Box(Modifier.size(26.dp).background(RED, CircleShape).align(Alignment.CenterEnd).padding(end = 30.dp))
            Surface(Modifier.align(Alignment.TopStart).padding(14.dp), color = WHITE.copy(alpha = .95f), shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(10.dp)) { Text(statusLabel(t.status), color = INK, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("تحديث تلقائي", color = MUTED, fontSize = 9.sp) } }
        }
    }
}

@Composable private fun Progress(status: String) {
    val s = when (status) { "driver_assigned" -> 1; "arriving" -> 2; "in_progress" -> 3; "completed" -> 4; else -> 1 }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(WHITE)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("مراحل الرحلة", color = INK, fontSize = 14.sp, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { for (i in 1..4) { Box(Modifier.size(27.dp).background(if (i <= s) G else BG, CircleShape), contentAlignment = Alignment.Center) { Text(if (i < s) "✓" else i.toString(), color = if (i <= s) WHITE else MUTED, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; if (i < 4) Box(Modifier.weight(1f).height(2.dp).background(if (i < s) G else Color(0xFFE2E9E5))) } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("قبول", color = MUTED, fontSize = 8.sp); Text("في الطريق", color = MUTED, fontSize = 8.sp); Text("مع الراكب", color = MUTED, fontSize = 8.sp); Text("مكتملة", color = MUTED, fontSize = 8.sp) }
        }
    }
}

@Composable private fun Details(t: Trip) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(WHITE)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("تفاصيل الرحلة", color = MUTED, fontSize = 10.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Info("${t.estimatedFare} ${t.currency}"); Info("${t.distanceKm} كم"); Info("${t.durationMin} دقيقة") }
            Row(Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text("الانطلاق", color = MUTED, fontSize = 8.sp); Text(coord(t.pickup), color = INK, fontSize = 10.sp, fontWeight = FontWeight.Bold) }; Column(Modifier.weight(1f)) { Text("الوجهة", color = MUTED, fontSize = 8.sp); Text(coord(t.destination), color = INK, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }
            Text(if (t.paymentMethod == "cash") "الدفع نقدي" else "طريقة الدفع: ${t.paymentMethod}", color = MUTED, fontSize = 10.sp)
        }
    }
}

@Composable private fun Info(text: String) { Surface(color = M, shape = RoundedCornerShape(999.dp)) { Text(text, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = GD, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
private fun coord(c: Coordinates) = "${String.format("%.4f", c.lat)} ، ${String.format("%.4f", c.lng)}"
private fun statusLabel(s: String) = when (s) { "driver_assigned" -> "تم قبول الرحلة"; "arriving" -> "في الطريق"; "in_progress" -> "الرحلة جارية"; "completed" -> "مكتملة"; else -> "رحلة نشطة" }
