package com.waslha.captain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Green = androidx.compose.ui.graphics.Color(0xFF087F5B)
private val Bg = androidx.compose.ui.graphics.Color(0xFFF5F8F6)
private val Ink = androidx.compose.ui.graphics.Color(0xFF12201B)
private val Muted = androidx.compose.ui.graphics.Color(0xFF6D7A75)

class CaptainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        setContent { CaptainApp() }
    }
}

@Composable
private fun CaptainApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { CaptainSession(context) }
    var signedIn by remember { mutableStateOf(session.isSignedIn) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            if (signedIn) {
                CaptainShell(session) { session.clear(); signedIn = false }
            } else {
                CaptainLogin { session.save(it); signedIn = true }
            }
        }
    }
}

@Composable
private fun CaptainLogin(onSuccess: (VerifySessionResponse) -> Unit) {
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var devCode by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (phone.filter(Char::isDigit).length < 8) { error = "أدخل رقم هاتف صحيح"; return }
        loading = true; error = null
        scope.launch {
            if (!sent) {
                runCatching { CaptainApiProvider.api.requestCode(OtpRequest(phone.trim())) }
                    .onSuccess { r ->
                        if (r.success) { devCode = r.data?.devCode; sent = true }
                        else error = r.message ?: "تعذر إرسال الرمز"
                    }.onFailure { error = it.message ?: "تعذر الاتصال بالخادم" }
            } else {
                if (code.filter(Char::isDigit).length < 4) { error = "أدخل رمز التحقق" }
                else runCatching { CaptainApiProvider.api.verifyCode(VerifyOtpRequest(phone.trim(), code.trim())) }
                    .onSuccess { r ->
                        val data = r.data
                        if (r.success && data != null && data.role.equals("driver", true)) onSuccess(data)
                        else error = r.message ?: "هذا الحساب غير مسجل ككابتن"
                    }.onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            }
            loading = false
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth().padding(22.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DirectionsCar, null, tint = Green, modifier = Modifier.size(52.dp))
                Text("وصلها كابتن", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
                Text("تسجيل دخول الكابتن", color = Muted, fontSize = 13.sp)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, singleLine = true)
                if (sent) {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("رمز التحقق") }, singleLine = true)
                    devCode?.let { Text("رمز الاختبار: $it", color = Green, fontWeight = FontWeight.Bold) }
                }
                error?.let { Text(it, color = androidx.compose.ui.graphics.Color(0xFFB42318), fontSize = 12.sp) }
                Button(submit, Modifier.fillMaxWidth(), enabled = !loading, shape = RoundedCornerShape(16.dp)) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (sent) "دخول" else "إرسال الرمز")
                }
                if (sent) TextButton({ sent = false; code = "" }) { Text("تغيير الرقم") }
            }
        }
    }
}

@Composable
private fun CaptainShell(session: CaptainSession, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var driver by remember { mutableStateOf<Driver?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var availableTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    var online by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.me() }.onSuccess { r -> driver = r.data; online = r.data?.available == true }
        runCatching { CaptainApiProvider.api.trips() }.onSuccess { r ->
            trips = r.data.orEmpty()
            activeTrip = trips.firstOrNull { it.status == "driver_assigned" || it.status == "arriving" || it.status == "in_progress" }
        }
        if (online) runCatching { CaptainApiProvider.api.availableTrips() }.onSuccess { r -> availableTrips = r.data.orEmpty() } else availableTrips = emptyList()
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(online) {
        if (!online) return@LaunchedEffect
        while (true) {
            delay(5000)
            refresh()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("الرئيسية") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.AccessTime, null) }, label = { Text("الرحلات") })
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("حسابي") })
            }
        }
    ) { padding ->
        when (tab) {
            0 -> CaptainDashboard(Modifier.padding(padding), online, driver, trips, availableTrips, activeTrip, busy, error,
                onToggle = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.availability(DriverAvailabilityRequest(!online)) }
                            .onSuccess { r -> driver = r.data; online = r.data?.available == true }
                            .onFailure { error = it.message ?: "تعذر تحديث الحالة" }
                        busy = false
                    }
                },
                onAccept = { id ->
                    busy = true; error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.acceptTrip(id) }
                            .onSuccess { r -> activeTrip = r.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                        busy = false
                    }
                },
                onStatus = { id, status ->
                    busy = true; error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.updateTripStatus(id, TripStatusRequest(status)) }
                            .onSuccess { r -> activeTrip = r.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                        busy = false
                    }
                }
            )
            1 -> TripHistory(Modifier.padding(padding), trips)
            else -> CaptainProfile(Modifier.padding(padding), driver, onLogout)
        }
    }
}

@Composable
private fun CaptainDashboard(modifier: Modifier, online: Boolean, driver: Driver?, trips: List<Trip>, available: List<Trip>, activeTrip: Trip?, busy: Boolean, error: String?, onToggle: () -> Unit, onAccept: (String) -> Unit, onStatus: (String, String) -> Unit) {
    val completed = trips.count { it.status == "completed" }
    val earnings = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(if (online) Green else androidx.compose.ui.graphics.Color.White)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PowerSettingsNew, null, tint = if (online) androidx.compose.ui.graphics.Color.White else Green, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (online) "أنت متصل" else "أنت غير متصل", color = if (online) androidx.compose.ui.graphics.Color.White else Ink, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(if (online) "استقبال الرحلات مفعّل" else "فعّل الحالة لاستقبال الطلبات", color = if (online) androidx.compose.ui.graphics.Color.White.copy(alpha = .8f) else Muted, fontSize = 12.sp)
                    }
                    Button(onClick = onToggle, enabled = !busy) { Text(if (online) "إيقاف" else "تشغيل") }
                }
            }
        }
        item { Text("رحلات: $completed   •   أرباح: $earnings ل.س   •   تقييم: ${String.format("%.1f", driver?.rating ?: 0.0)}", color = Muted, fontSize = 13.sp) }
        error?.let { item { Text(it, color = androidx.compose.ui.graphics.Color(0xFFB42318), fontSize = 12.sp) } }
        activeTrip?.let { item { ActiveTripCard(it, busy, onStatus) } }
        item { Text("الطلبات الجديدة", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        if (online && activeTrip == null) items(available) { trip ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Green)
                        Spacer(Modifier.size(8.dp))
                        Column(Modifier.weight(1f)) { Text("طلب جديد", fontWeight = FontWeight.Bold); Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 11.sp) }
                        Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black)
                    }
                    Button({ onAccept(trip.id) }, enabled = !busy, Modifier.fillMaxWidth()) { Text("قبول الرحلة") }
                }
            }
        }
    }
}

@Composable
private fun ActiveTripCard(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Green)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("رحلة نشطة", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Black)
            Text("الاستلام: ${trip.pickup.lat}, ${trip.pickup.lng}", color = androidx.compose.ui.graphics.Color.White.copy(alpha = .85f), fontSize = 12.sp)
            Text("الوجهة: ${trip.destination.lat}, ${trip.destination.lng}", color = androidx.compose.ui.graphics.Color.White.copy(alpha = .85f), fontSize = 12.sp)
            Text("${trip.estimatedFare} ${trip.currency}", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
            when (trip.status) {
                "driver_assigned" -> Button({ onStatus(trip.id, "arriving") }, enabled = !busy, Modifier.fillMaxWidth()) { Text("أنا في الطريق") }
                "arriving" -> Button({ onStatus(trip.id, "in_progress") }, enabled = !busy, Modifier.fillMaxWidth()) { Text("بدأت الرحلة") }
                "in_progress" -> Button({ onStatus(trip.id, "completed") }, enabled = !busy, Modifier.fillMaxWidth()) { Text("إنهاء الرحلة") }
            }
        }
    }
}

@Composable
private fun TripHistory(modifier: Modifier, trips: List<Trip>) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(trips) { trip ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("رحلة #${trip.id.takeLast(6)}", fontWeight = FontWeight.Bold)
                    Text("${trip.status} • ${trip.distanceKm} كم", color = Muted, fontSize = 12.sp)
                    Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black)
                }
            }
        }
        if (trips.isEmpty()) item { Box(Modifier.fillMaxWidth().padding(top = 100.dp), contentAlignment = Alignment.Center) { Text("لا توجد رحلات بعد", color = Muted) } }
    }
}

@Composable
private fun CaptainProfile(modifier: Modifier, driver: Driver?, onLogout: () -> Unit) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text(driver?.phone ?: "رقم الهاتف غير متوفر", color = Muted, fontSize = 12.sp)
                Text("${driver?.vehicle ?: "السيارة غير مسجلة"} ${driver?.plate ?: ""}", fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = Green); Spacer(Modifier.size(4.dp)); Text(String.format("%.1f", driver?.rating ?: 0.0), fontWeight = FontWeight.Bold) }
            }
        }
        Button(onLogout, Modifier.fillMaxWidth()) { Text("تسجيل الخروج") }
    }
}
