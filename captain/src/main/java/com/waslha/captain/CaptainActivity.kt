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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Green = Color(0xFF087F5B)
private val Background = Color(0xFFF4F7F5)
private val Ink = Color(0xFF14201B)
private val Muted = Color(0xFF6B7872)
private val White = Color.White
private val Red = Color(0xFFB42318)

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
        Surface(Modifier.fillMaxSize(), color = Background) {
            if (signedIn) {
                CaptainHome(session) {
                    session.clear()
                    signedIn = false
                }
            } else {
                CaptainLogin { result ->
                    session.save(result)
                    signedIn = true
                }
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

    fun requestCode() {
        val value = phone.trim()
        if (value.filter(Char::isDigit).length < 8) {
            error = "أدخل رقم هاتف صحيح"
            return
        }
        loading = true
        error = null
        scope.launch {
            runCatching { CaptainApiProvider.api.requestCode(OtpRequest(value)) }
                .onSuccess { response ->
                    if (response.success) {
                        sent = true
                        devCode = response.data?.devCode
                    } else error = response.message ?: "تعذر إرسال الرمز"
                }
                .onFailure { error = it.message ?: "تعذر الاتصال بالخادم" }
            loading = false
        }
    }

    fun verifyCode() {
        if (code.length < 4) {
            error = "أدخل رمز التحقق"
            return
        }
        loading = true
        error = null
        scope.launch {
            runCatching { CaptainApiProvider.api.verifyCode(VerifyOtpRequest(phone.trim(), code.trim())) }
                .onSuccess { response ->
                    val data = response.data
                    if (response.success && data != null && data.role.equals("driver", true)) onSuccess(data)
                    else error = response.message ?: "الحساب غير مسجل ككابتن"
                }
                .onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            loading = false
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth().padding(22.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(White)) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(68.dp).background(Green, RoundedCornerShape(20.dp)), contentAlignment = Alignment.Center) {
                    Text("و", color = White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                }
                Text("وصلها كابتن", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("سجل دخولك وابدأ استقبال الرحلات", color = Muted, fontSize = 13.sp)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رقم الهاتف") })
                if (sent) {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رمز التحقق") })
                    devCode?.takeIf { it.isNotBlank() }?.let { Text("رمز الاختبار: $it", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
                error?.let { Text(it, color = Red, fontSize = 12.sp) }
                ActionChip(if (sent) "دخول" else "إرسال الرمز", !loading) { if (sent) verifyCode() else requestCode() }
                if (sent) TextButton(onClick = { sent = false; code = ""; devCode = null; error = null }) { Text("تغيير الرقم", color = Green) }
            }
        }
    }
}

@Composable
private fun CaptainHome(session: CaptainSession, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var driver by remember { mutableStateOf<Driver?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var available by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var active by remember { mutableStateOf<Trip?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.me() }.onSuccess { driver = it.data }.onFailure { error = it.message ?: "تعذر تحميل الحساب" }
        runCatching { CaptainApiProvider.api.trips() }.onSuccess { result ->
            trips = result.data.orEmpty()
            active = trips.firstOrNull { it.status in setOf("driver_assigned", "arriving", "in_progress") }
        }.onFailure { error = it.message ?: "تعذر تحميل الرحلات" }
        if (driver?.available == true) {
            runCatching { CaptainApiProvider.api.availableTrips() }.onSuccess { available = it.data.orEmpty() }.onFailure { error = it.message ?: "تعذر تحميل الطلبات" }
        } else available = emptyList()
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(driver?.available) {
        if (driver?.available != true) return@LaunchedEffect
        while (true) { delay(5000); refresh() }
    }

    Scaffold(bottomBar = {
        NavigationBar(containerColor = White) {
            NavigationBarItem(tab == 0, { tab = 0 }, icon = { Text("⌂") }, label = { Text("الرئيسية") })
            NavigationBarItem(tab == 1, { tab = 1 }, icon = { Text("↺") }, label = { Text("الرحلات") })
            NavigationBarItem(tab == 2, { tab = 2 }, icon = { Text("○") }, label = { Text("حسابي") })
        }
    }) { padding ->
        when (tab) {
            0 -> Dashboard(Modifier.padding(padding), driver, trips, available, active, busy, error,
                onToggle = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.availability(DriverAvailabilityRequest(driver?.available != true)) }
                            .onSuccess { driver = it.data }
                            .onFailure { error = it.message ?: "تعذر تغيير الحالة" }
                        busy = false
                    }
                },
                onAccept = { id ->
                    busy = true; error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.acceptTrip(id) }
                            .onSuccess { active = it.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                        busy = false
                    }
                },
                onStatus = { id, status ->
                    busy = true; error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.updateTripStatus(id, TripStatusRequest(status)) }
                            .onSuccess { active = it.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                        busy = false
                    }
                })
            1 -> TripHistory(Modifier.padding(padding), trips)
            else -> Account(Modifier.padding(padding), driver, onLogout)
        }
    }
}

@Composable
private fun Dashboard(modifier: Modifier, driver: Driver?, trips: List<Trip>, available: List<Trip>, active: Trip?, busy: Boolean, error: String?, onToggle: () -> Unit, onAccept: (String) -> Unit, onStatus: (String, String) -> Unit) {
    val online = driver?.available == true
    val completed = trips.count { it.status == "completed" }
    val earnings = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("الرئيسية", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black); Text(driver?.name?.let { "أهلاً $it" } ?: "أهلاً كابتن", color = Muted, fontSize = 13.sp) }
        item { ClickCard(if (online) "أنت متصل" else "أنت غير متصل", if (online) "استقبال الرحلات مفعّل" else "فعّل الحالة حتى تستقبل الطلبات", online, busy, onToggle) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Stat("الرحلات", completed.toString(), Modifier.weight(1f)); Stat("الأرباح", earnings.toString(), Modifier.weight(1f)); Stat("التقييم", String.format("%.1f", driver?.rating ?: 0.0), Modifier.weight(1f)) } }
        error?.let { msg -> item { Text(msg, color = Red, fontSize = 12.sp) } }
        active?.let { trip -> item { ActiveTrip(trip, busy, onStatus) } }
        item { Text("الطلبات المتاحة", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
        if (online && active == null && available.isEmpty()) item { Text("لا توجد طلبات متاحة حالياً", color = Muted, fontSize = 12.sp) }
        items(available) { trip -> AvailableTrip(trip, busy, onAccept) }
    }
}

@Composable
private fun ClickCard(title: String, subtitle: String, online: Boolean, busy: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(if (online) Green else White)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = if (online) White else Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = if (online) White.copy(alpha = .8f) else Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ActiveTrip(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit) {
    val next = when (trip.status) { "driver_assigned" -> "arriving"; "arriving" -> "in_progress"; "in_progress" -> "completed"; else -> null }
    val label = when (trip.status) { "driver_assigned" -> "أنا في الطريق"; "arriving" -> "بدأت الرحلة"; "in_progress" -> "إنهاء الرحلة"; else -> null }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Green)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("رحلة نشطة", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("المبلغ: ${trip.estimatedFare} ${trip.currency}", color = White, fontWeight = FontWeight.Bold)
            Text("المسافة: ${trip.distanceKm} كم", color = White.copy(alpha = .85f), fontSize = 12.sp)
            Text("الحالة: ${trip.status}", color = White.copy(alpha = .85f), fontSize = 12.sp)
            if (next != null && label != null) ActionChip(label, !busy, true) { onStatus(trip.id, next) }
        }
    }
}

@Composable
private fun AvailableTrip(trip: Trip, busy: Boolean, onAccept: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("طلب جديد", color = Ink, fontWeight = FontWeight.Black); Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 12.sp) }
                Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black)
            }
            ActionChip("قبول الرحلة", !busy) { onAccept(trip.id) }
        }
    }
}

@Composable
private fun TripHistory(modifier: Modifier, trips: List<Trip>) {
    if (trips.isEmpty()) return Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد رحلات بعد", color = Muted, fontWeight = FontWeight.Bold) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("رحلاتي", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black) }
        items(trips) { trip -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(16.dp)) { Text("رحلة #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Bold); Text("${trip.status} • ${trip.distanceKm} كم", color = Muted, fontSize = 12.sp); Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black) } } }
    }
}

@Composable
private fun Account(modifier: Modifier, driver: Driver?, onLogout: () -> Unit) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("حسابي", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black); Text(driver?.phone ?: "", color = Muted, fontSize = 12.sp); Text(driver?.vehicle ?: "السيارة غير مسجلة", color = Ink, fontWeight = FontWeight.Bold); Text(driver?.plate ?: "", color = Muted, fontSize = 12.sp) } }
        ActionChip("تسجيل الخروج", true) { onLogout() }
    }
}

@Composable
private fun Stat(title: String, value: String, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(12.dp)) { Text(value, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black); Text(title, color = Muted, fontSize = 10.sp) } } }

@Composable
private fun ActionChip(text: String, enabled: Boolean, filled: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).background(if (filled) White else Green, RoundedCornerShape(16.dp)).padding(15.dp), contentAlignment = Alignment.Center) {
        if (!enabled) CircularProgressIndicator(Modifier.size(18.dp), color = if (filled) Green else White, strokeWidth = 2.dp) else Text(text, color = if (filled) Green else White, fontWeight = FontWeight.Bold)
    }
}
