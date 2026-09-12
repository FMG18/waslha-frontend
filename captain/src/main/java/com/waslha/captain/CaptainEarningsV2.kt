package com.waslha.captain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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
import java.util.Calendar

private val EarningsGreen = Color(0xFF0B805E)
private val EarningsDark = Color(0xFF075B43)
private val EarningsMint = Color(0xFFE8F5F0)
private val EarningsBg = Color(0xFFF4F7F6)
private val EarningsInk = Color(0xFF14211C)
private val EarningsMuted = Color(0xFF6E7D76)
private val EarningsWhite = Color.White
private val EarningsRed = Color(0xFFB42318)

@Composable
fun CaptainEarningsV2(modifier: Modifier, trips: List<Trip>) {
    var filter by remember { mutableStateOf(0) }
    val completed = trips.filter { it.status == "completed" }
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val today = completed.filter { it.createdAt >= todayStart }
    val shown = when (filter) {
        1 -> completed
        2 -> trips.filter { it.status in setOf("driver_assigned", "arriving", "in_progress") }
        3 -> trips.filter { it.status == "cancelled" }
        else -> trips
    }
    val todayIncome = today.sumOf { it.estimatedFare }
    val totalIncome = completed.sumOf { it.estimatedFare }
    val average = if (completed.isEmpty()) 0 else totalIncome / completed.size

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("الأرباح والرحلات", color = EarningsInk, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("تابع دخلك وسجل عملياتك من مكان واحد", color = EarningsMuted, fontSize = 11.sp)
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(27.dp), colors = CardDefaults.cardColors(EarningsDark)) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("أرباح اليوم", color = EarningsWhite.copy(alpha = .7f), fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(formatEarnings(todayIncome), color = EarningsWhite, fontSize = 31.sp, fontWeight = FontWeight.Black)
                        Text(" ل.س", color = EarningsWhite.copy(alpha = .72f), fontSize = 10.sp, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EarningsMetric("الكل", formatEarnings(totalIncome), Modifier.weight(1f))
                        EarningsMetric("المكتملة", completed.size.toString(), Modifier.weight(1f))
                        EarningsMetric("متوسط الرحلة", formatEarnings(average), Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                FilterChip("الكل", filter == 0, Modifier.weight(1f)) { filter = 0 }
                FilterChip("مكتملة", filter == 1, Modifier.weight(1f)) { filter = 1 }
                FilterChip("جارية", filter == 2, Modifier.weight(1f)) { filter = 2 }
                FilterChip("ملغاة", filter == 3, Modifier.weight(1f)) { filter = 3 }
            }
        }
        item {
            Text("سجل الرحلات", color = EarningsInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
        if (shown.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(EarningsWhite)) {
                    Column(
                        Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Surface(Modifier.size(48.dp), shape = CircleShape, color = EarningsMint) {
                            Column(
                                Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("—", color = EarningsGreen, fontSize = 22.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text("لا توجد رحلات", color = EarningsInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("ستظهر الرحلات هنا بعد تنفيذها", color = EarningsMuted, fontSize = 10.sp)
                    }
                }
            }
        } else {
            items(shown, key = { it.id }) { trip -> EarningsTripRow(trip) }
        }
    }
}

@Composable
private fun EarningsMetric(title: String, value: String, modifier: Modifier) {
    Column(
        modifier.background(EarningsWhite.copy(alpha = .08f), RoundedCornerShape(15.dp)).padding(10.dp)
    ) {
        Text(title, color = EarningsWhite.copy(alpha = .62f), fontSize = 8.sp)
        Text(value, color = EarningsWhite, fontSize = 13.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(13.dp),
        color = if (selected) EarningsGreen else EarningsWhite,
        onClick = onClick
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = if (selected) EarningsWhite else EarningsMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EarningsTripRow(trip: Trip) {
    val status = when (trip.status) {
        "completed" -> "مكتملة"
        "driver_assigned" -> "مقبولة"
        "arriving" -> "في الطريق"
        "in_progress" -> "جارية"
        "cancelled" -> "ملغاة"
        else -> trip.status
    }
    val statusPositive = trip.status == "completed"
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(EarningsWhite)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("رحلة #${trip.id.takeLast(6)}", color = EarningsInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(status, color = EarningsMuted, fontSize = 9.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatEarnings(trip.estimatedFare), color = if (statusPositive) EarningsGreen else EarningsInk, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text(trip.currency, color = EarningsMuted, fontSize = 8.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                EarningsInfo("${trip.distanceKm} كم")
                EarningsInfo("${trip.durationMin} د")
                EarningsInfo(if (trip.paymentMethod == "cash") "نقدي" else trip.paymentMethod)
            }
            RouteText("الانطلاق", coordinatesForEarnings(trip.pickup), EarningsGreen)
            RouteText("الوجهة", coordinatesForEarnings(trip.destination), EarningsRed)
        }
    }
}

@Composable
private fun EarningsInfo(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = EarningsMint) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = EarningsDark, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RouteText(title: String, value: String, dot: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BoxDot(dot)
        Column(Modifier.padding(start = 9.dp)) {
            Text(title, color = EarningsMuted, fontSize = 8.sp)
            Text(value, color = EarningsInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun BoxDot(color: Color) {
    Surface(Modifier.size(9.dp), shape = CircleShape, color = color) {}
}

private fun formatEarnings(value: Int): String = "%,d".format(value)
private fun coordinatesForEarnings(c: Coordinates): String = "${String.format("%.4f", c.lat)} ، ${String.format("%.4f", c.lng)}"
