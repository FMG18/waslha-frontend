package com.waslha.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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

private val Green = Color(0xFF0B805E)
private val Dark = Color(0xFF075B43)
private val Bg = Color(0xFFF4F7F6)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color.White
private val Red = Color(0xFFB42318)
private val Amber = Color(0xFFB54708)

private data class AdminTrip(
    val id: String,
    val status: String,
    val customer: String,
    val driver: String,
    val fare: Int
)

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AdminApp() }
    }
}

@Composable
private fun AdminApp() {
    var tab by remember { mutableStateOf(0) }
    var trips by remember {
        mutableStateOf(
            listOf(
                AdminTrip("W-1001", "جارية", "زبون 1", "أحمد", 8500),
                AdminTrip("W-1002", "بانتظار كابتن", "زبون 2", "—", 12000),
                AdminTrip("W-1003", "مكتملة", "زبون 3", "محمد", 7000)
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            trips = trips.toList()
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Scaffold(
                containerColor = Bg,
                bottomBar = {
                    NavigationBar(containerColor = White) {
                        NavigationBarItem(tab == 0, { tab = 0 }, icon = { Text("⌂") }, label = { Text("الرئيسية") })
                        NavigationBarItem(tab == 1, { tab = 1 }, icon = { Text("↺") }, label = { Text("الرحلات") })
                        NavigationBarItem(tab == 2, { tab = 2 }, icon = { Text("●") }, label = { Text("الكباتن") })
                    }
                }
            ) { padding ->
                when (tab) {
                    0 -> Dashboard(Modifier.padding(padding), trips)
                    1 -> Trips(Modifier.padding(padding), trips)
                    else -> Drivers(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun Dashboard(modifier: Modifier, trips: List<AdminTrip>) {
    val active = trips.count { it.status == "جارية" }
    val waiting = trips.count { it.status == "بانتظار كابتن" }
    val completed = trips.count { it.status == "مكتملة" }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("وصلها إدارة", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("مركز التحكم بالنظام", color = Muted, fontSize = 11.sp)
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(Dark)) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("حالة النظام", color = White.copy(alpha = .7f), fontSize = 10.sp)
                    Text("النظام يعمل", color = White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("متابعة الرحلات والكباتن لحظياً", color = White.copy(alpha = .75f), fontSize = 10.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("جارية", active.toString(), Green, Modifier.weight(1f))
                Stat("بانتظار", waiting.toString(), Amber, Modifier.weight(1f))
                Stat("مكتملة", completed.toString(), Ink, Modifier.weight(1f))
            }
        }
        item { Section("آخر الرحلات") }
        items(trips.take(3), key = { it.id }) { trip -> TripCard(trip) }
    }
}

@Composable
private fun Trips(modifier: Modifier, trips: List<AdminTrip>) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("إدارة الرحلات", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("كل الرحلات في مكان واحد", color = Muted, fontSize = 11.sp)
        }
        items(trips, key = { it.id }) { TripCard(it) }
    }
}

@Composable
private fun Drivers(modifier: Modifier) {
    val drivers = listOf("أحمد" to "متصل", "محمد" to "متصل", "علي" to "غير متصل")
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("الكباتن", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("متابعة حالة الكباتن", color = Muted, fontSize = 11.sp)
        }
        items(drivers) { (name, state) ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(44.dp), shape = CircleShape, color = if (state == "متصل") Color(0xFFE8F5F0) else Bg) {
                        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("ك", color = Green, fontWeight = FontWeight.Black)
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                        Text(name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text("كابتن", color = Muted, fontSize = 9.sp)
                    }
                    Text(state, color = if (state == "متصل") Green else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TripCard(trip: AdminTrip) {
    Card(
        Modifier.fillMaxWidth().clickable { },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(White)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("رحلة #${trip.id.takeLast(4)}", color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("${trip.customer}  •  الكابتن: ${trip.driver}", color = Muted, fontSize = 9.sp)
                }
                Text(trip.status, color = when (trip.status) { "جارية" -> Green; "بانتظار كابتن" -> Amber; else -> Muted }, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("المبلغ", color = Muted, fontSize = 9.sp)
                Text("${trip.fare} ل.س", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Stat(title: String, value: String, accent: Color, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = White) {
        Column(Modifier.padding(12.dp)) {
            Text(title, color = Muted, fontSize = 9.sp)
            Spacer(Modifier.height(3.dp))
            Text(value, color = accent, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun Section(text: String) {
    Text(text, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
}
