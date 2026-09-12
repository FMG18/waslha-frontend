package com.waslha.captain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF0B805E)
private val Bg = Color(0xFFF4F7F6)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color.White

@Composable
fun CaptainEarningsV2(modifier: Modifier, trips: List<Trip>) {
    val completed = trips.filter { it.status == "completed" }
    val total = completed.sumOf { it.estimatedFare }
    val average = if (completed.isEmpty()) 0 else total / completed.size
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("الأرباح والرحلات", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("ملخص دخلك وسجل الرحلات", color = Muted, fontSize = 11.sp)
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Green)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("إجمالي الأرباح", color = White.copy(.75f), fontSize = 10.sp)
                    Text("%,d ل.س".format(total), color = White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("${completed.size} رحلة مكتملة • متوسط ${"%,d".format(average)} ل.س", color = White.copy(.8f), fontSize = 10.sp)
                }
            }
        }
        item { Text("سجل الرحلات", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
        if (trips.isEmpty()) item { Text("لا توجد رحلات بعد", color = Muted, fontSize = 11.sp) }
        else items(trips, key = { it.id }) { trip ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(White)) {
                Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth(.7f)) {
                            Text("رحلة #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                            Text(statusLabel(trip.status), color = Muted, fontSize = 9.sp)
                        }
                        Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black)
                    }
                    Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 9.sp)
                }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "completed" -> "مكتملة"
    "driver_assigned" -> "مقبولة"
    "arriving" -> "في الطريق"
    "in_progress" -> "جارية"
    "cancelled" -> "ملغاة"
    else -> status
}
