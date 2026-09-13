package com.waslha.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Green = Color(0xFF0B805E)
private val Dark = Color(0xFF075B43)
private val Bg = Color(0xFFF4F7F6)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color.White
private val Red = Color(0xFFB42318)
private val Amber = Color(0xFFB54708)

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        setContent { AdminApp() }
    }
}

@Composable
private fun AdminApp() {
    var tab by remember { mutableStateOf(0) }
    var overview by remember { mutableStateOf<AdminOverview?>(null) }
    var trips by remember { mutableStateOf<List<AdminTripDto>>(emptyList()) }
    var drivers by remember { mutableStateOf<List<AdminDriverDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                val o = AdminApiProvider.api.overview().data
                val t = AdminApiProvider.api.trips().data ?: emptyList()
                val d = AdminApiProvider.api.drivers().data ?: emptyList()
                overview = o
                trips = t
                drivers = d
            } catch (e: Exception) {
                error = e.message ?: "تعذر تحميل بيانات الإدارة"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            refresh()
            delay(15_000)
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
                    0 -> Dashboard(Modifier.padding(padding), overview, trips, loading, error, ::refresh)
                    1 -> Trips(Modifier.padding(padding), trips, loading, error, ::refresh)
                    else -> Drivers(Modifier.padding(padding), drivers, loading, error, ::refresh)
                }
            }
        }
    }
}

@Composable
private fun Dashboard(modifier: Modifier, overview: AdminOverview?, trips: List<AdminTripDto>, loading: Boolean, error: String?, refresh: () -> Unit) {
    val totals = overview?.totals ?: AdminTotals()
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("وصلها إدارة", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("مركز التحكم بالنظام", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = refresh) { Text("تحديث") }
            }
        }
        if (error != null) item {
            Card(colors = CardDefaults.cardColors(Color(0xFFFFF1F0)), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(15.dp)) {
                    Text(error, color = Red, fontWeight = FontWeight.Bold)
                    Text("تحقق من الاتصال بالخادم ثم أعد التحديث", color = Muted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(Dark)) {
                Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("حالة النظام", color = White.copy(alpha = .7f), fontSize = 10.sp)
                    Text(if (loading) "جاري تحديث البيانات" else "النظام يعمل", color = White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text("تحديث تلقائي كل 15 ثانية", color = White.copy(alpha = .75f), fontSize = 10.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("الرحلات", totals.trips.toString(), Ink, Modifier.weight(1f))
                Stat("جارية", totals.activeTrips.toString(), Green, Modifier.weight(1f))
                Stat("بانتظار", totals.waitingTrips.toString(), Amber, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("مكتملة", totals.completedTrips.toString(), Ink, Modifier.weight(1f))
                Stat("الكباتن", totals.drivers.toString(), Dark, Modifier.weight(1f))
                Stat("متصل", totals.onlineDrivers.toString(), Green, Modifier.weight(1f))
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("إيرادات الرحلات", color = Muted, fontSize = 10.sp)
                        Text("${totals.revenue} ل.س", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    }
                    Text("إجمالي", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Text("آخر الرحلات", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
        if (!loading && trips.isEmpty()) item { EmptyState("لا توجد رحلات حالياً") }
        items(trips.take(5), key = { it.id }) { TripCard(it) }
    }
}

@Composable
private fun Trips(modifier: Modifier, trips: List<AdminTripDto>, loading: Boolean, error: String?, refresh: () -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("إدارة الرحلات", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("بيانات الرحلات من الخادم", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = refresh) { Text("تحديث") }
            }
        }
        if (error != null) item { Text(error, color = Red, fontSize = 11.sp) }
        if (!loading && trips.isEmpty()) item { EmptyState("لا توجد رحلات") }
        items(trips, key = { it.id }) { TripCard(it) }
    }
}

@Composable
private fun Drivers(modifier: Modifier, drivers: List<AdminDriverDto>, loading: Boolean, error: String?, refresh: () -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الكباتن", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Text("المتصلون وحالة التوفر من الخادم", color = Muted, fontSize = 11.sp)
                }
                TextButton(onClick = refresh) { Text("تحديث") }
            }
        }
        if (error != null) item { Text(error, color = Red, fontSize = 11.sp) }
        if (!loading && drivers.isEmpty()) item { EmptyState("لا توجد بيانات كباتن") }
        items(drivers, key = { it.id }) { driver -> DriverCard(driver) }
    }
}

@Composable
private fun TripCard(trip: AdminTripDto) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("رحلة #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text("الزبون: ${trip.customerId.ifBlank { "غير محدد" }}", color = Muted, fontSize = 9.sp)
                }
                Text(trip.status.ifBlank { "غير معروف" }, color = statusColor(trip.status), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${trip.vehicleType} • ${trip.paymentMethod}", color = Muted, fontSize = 9.sp)
                Text("${trip.estimatedFare} ${trip.currency}", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            if (trip.driver != null) Text("الكابتن: ${trip.driver.name.ifBlank { trip.driver.id }}", color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun DriverCard(driver: AdminDriverDto) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(44.dp), shape = CircleShape, color = if (driver.available) Color(0xFFE8F5F0) else Bg) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("ك", color = Green, fontWeight = FontWeight.Black) }
            }
            Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                Text(driver.name.ifBlank { driver.id }, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("${driver.vehicle.ifBlank { driver.type }}${if (driver.plate.isNotBlank()) " • ${driver.plate}" else ""}", color = Muted, fontSize = 9.sp)
            }
            Text(if (driver.available) "متاح" else "غير متاح", color = if (driver.available) Green else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun statusColor(status: String): Color = when {
    status.contains("جارية") || status.contains("accepted", true) || status.contains("in_progress", true) -> Green
    status.contains("انتظار") || status.contains("pending", true) -> Amber
    status.contains("cancel", true) || status.contains("ملغ") -> Red
    else -> Muted
}

@Composable
private fun EmptyState(text: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = White) {
        Box(Modifier.padding(28.dp).fillMaxWidth(), contentAlignment = Alignment.Center) { Text(text, color = Muted, fontSize = 12.sp) }
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
