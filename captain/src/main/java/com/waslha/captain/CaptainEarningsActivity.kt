package com.waslha.captain

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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val EG = Color(0xFF0B805E)
private val ED = Color(0xFF075B43)
private val EM = Color(0xFFE8F5F0)
private val EB = Color(0xFFF4F7F6)
private val EI = Color(0xFF14211C)
private val EX = Color(0xFF6E7D76)
private val EW = Color.White
private val ER = Color(0xFFB42318)

class CaptainEarningsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = EB) {
                    CaptainEarningsScreen(onBack = { finish() })
                }
            }
        }
    }
}

@Composable
private fun CaptainEarningsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.trips() }
            .onSuccess { trips = it.data.orEmpty(); error = null }
            .onFailure { error = it.message ?: "تعذر تحميل الرحلات" }
        loading = false
    }

    LaunchedEffect(Unit) {
        refresh()
        while (true) {
            delay(10000)
            refresh()
        }
    }

    val completed = trips.filter { it.status == "completed" }
    val active = trips.count { it.status in setOf("driver_assigned", "arriving", "in_progress") }
    val total = completed.sumOf { it.estimatedFare }
    val average = if (completed.isEmpty()) 0 else total / completed.size

    if (loading && trips.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EG)
        }
        return
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).background(EW, CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Text("‹", color = EI, fontSize = 27.sp) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("الأرباح والرحلات", color = EI, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text("ملخص أدائك وسجل رحلاتك", color = EX, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(27.dp), colors = CardDefaults.cardColors(ED)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("إجمالي أرباح الرحلات المكتملة", color = EW.copy(alpha = .72f), fontSize = 10.sp)
                    Text("${money(total)} ل.س", color = EW, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("${completed.size} رحلة مكتملة • ${active} رحلة قيد التنفيذ", color = EW.copy(alpha = .72f), fontSize = 10.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                SummaryTile("الرحلات المكتملة", completed.size.toString(), Modifier.weight(1f))
                SummaryTile("متوسط الرحلة", "${money(average)}", Modifier.weight(1f))
            }
        }
        item {
            Text("سجل الرحلات", color = EI, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        error?.let { message ->
            item {
                Surface(Modifier.fillMaxWidth(), color = Color(0xFFFFE9E7), shape = RoundedCornerShape(14.dp)) {
                    Text(message, Modifier.padding(12.dp), color = ER, fontSize = 10.sp)
                }
            }
        }
        if (trips.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(EW)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("لا توجد رحلات حتى الآن", color = EI, fontWeight = FontWeight.Black)
                        Text("ستظهر الرحلات هنا بعد قبولها", color = EX, fontSize = 10.sp)
                    }
                }
            }
        } else {
            items(trips, key = { it.id }) { trip ->
                TripHistoryCard(trip)
            }
        }
    }
}

@Composable
private fun SummaryTile(title: String, value: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(EW)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = EX, fontSize = 9.sp)
            Text(value, color = EI, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun TripHistoryCard(trip: Trip) {
    val completed = trip.status == "completed"
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(EW)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("رحلة #${trip.id.takeLast(6)}", color = EI, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    Text(statusText(trip.status), color = if (completed) EG else EX, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${money(trip.estimatedFare)}", color = EG, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(trip.currency, color = EX, fontSize = 8.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Pill("${trip.distanceKm} كم")
                Pill("${trip.durationMin} دقيقة")
                Pill(if (trip.paymentMethod == "cash") "نقدي" else trip.paymentMethod)
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(EG, CircleShape))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("الانطلاق", color = EX, fontSize = 8.sp)
                    Text(coords(trip.pickup), color = EI, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.size(8.dp).background(ER, CircleShape))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("الوجهة", color = EX, fontSize = 8.sp)
                    Text(coords(trip.destination), color = EI, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Pill(text: String) {
    Surface(color = EM, shape = RoundedCornerShape(999.dp)) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = ED, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

private fun money(value: Int): String = "%,d".format(value)
private fun coords(c: Coordinates): String = "${String.format("%.4f", c.lat)} ، ${String.format("%.4f", c.lng)}"
private fun statusText(s: String): String = when (s) {
    "completed" -> "مكتملة"
    "driver_assigned" -> "تم قبولها"
    "arriving" -> "في الطريق للراكب"
    "in_progress" -> "قيد التنفيذ"
    "searching" -> "بانتظار كابتن"
    else -> s
}
