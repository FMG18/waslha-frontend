package com.waslha.captain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Green = Color(0xFF0B805E)
private val GreenDark = Color(0xFF075B43)
private val Mint = Color(0xFFE8F5F0)
private val Background = Color(0xFFF4F7F6)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color.White
private val Border = Color(0xFFE0E7E3)
private val Red = Color(0xFFB42318)
private val Amber = Color(0xFFB54708)

class CaptainActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        setContent { CaptainAppV2() }
    }
}

@Composable
private fun CaptainAppV2() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { CaptainSession(context) }
    var signedIn by remember { mutableStateOf(session.isSignedIn) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Background) {
            if (signedIn) CaptainShellV2(session) { session.clear(); signedIn = false }
            else CaptainLoginV2 { result -> session.save(result); signedIn = true }
        }
    }
}

@Composable
private fun CaptainLoginV2(onSuccess: (VerifySessionResponse) -> Unit) {
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var devCode by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun sendCode() {
        val value = phone.trim()
        if (value.filter(Char::isDigit).length < 8) { error = "أدخل رقم هاتف صحيح"; return }
        loading = true; error = null
        scope.launch {
            runCatching { CaptainApiProvider.api.requestCode(OtpRequest(value)) }
                .onSuccess { r -> if (r.success) { sent = true; devCode = r.data?.devCode } else error = r.message ?: "تعذر إرسال الرمز" }
                .onFailure { error = it.message ?: "تعذر الاتصال بالخادم" }
            loading = false
        }
    }
    fun login() {
        if (code.length < 4) { error = "أدخل رمز التحقق"; return }
        loading = true; error = null
        scope.launch {
            runCatching { CaptainApiProvider.api.verifyCode(VerifyOtpRequest(phone.trim(), code.trim())) }
                .onSuccess { r ->
                    val d = r.data
                    if (r.success && d != null && d.role.equals("driver", true)) onSuccess(d)
                    else error = r.message ?: "الحساب غير مسجل ككابتن"
                }
                .onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            loading = false
        }
    }
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(White)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(15.dp)) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Green), contentAlignment = Alignment.Center) { Text("و", color = White, fontSize = 38.sp, fontWeight = FontWeight.Black) }
                Text("وصلها كابتن", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("لوحة عملك لاستقبال وإدارة الرحلات", color = Muted, fontSize = 12.sp)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رقم الهاتف") })
                if (sent) {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رمز التحقق") })
                    devCode?.let { Text("رمز الاختبار: $it", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                error?.let { Text(it, color = Red, fontSize = 12.sp) }
                CaptainButton(if (sent) "دخول" else "إرسال الرمز", !loading, Modifier.fillMaxWidth()) { if (sent) login() else sendCode() }
            }
        }
    }
}

@Composable
private fun CaptainShellV2(session: CaptainSession, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var driver by remember { mutableStateOf<Driver?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var available by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var active by remember { mutableStateOf<Trip?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.me() }.onSuccess { driver = it.data }.onFailure { error = it.message }
        runCatching { CaptainApiProvider.api.trips() }.onSuccess { r -> trips = r.data.orEmpty(); active = trips.firstOrNull { it.status in setOf("driver_assigned", "arriving", "in_progress") } }.onFailure { error = it.message }
        if (driver?.available == true && active == null) runCatching { CaptainApiProvider.api.availableTrips() }.onSuccess { available = it.data.orEmpty() } else available = emptyList()
    }
    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(driver?.available, active?.id) {
        if (driver?.available != true) return@LaunchedEffect
        while (true) { delay(5000); refresh() }
    }
    Scaffold(containerColor = Background, bottomBar = {
        NavigationBar(containerColor = White) {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂") }, label = { Text("الرئيسية") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("↺") }, label = { Text("رحلاتي") })
            NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("○") }, label = { Text("حسابي") })
        }
    }) { padding ->
        when (tab) {
            0 -> Dashboard(Modifier.padding(padding), driver, trips, available, active, busy, error,
                onToggle = { busy = true; scope.launch { runCatching { CaptainApiProvider.api.availability(DriverAvailabilityRequest(driver?.available != true)) }.onSuccess { driver = it.data }.onFailure { error = it.message }; busy = false } },
                onAccept = { id -> busy = true; scope.launch { runCatching { CaptainApiProvider.api.acceptTrip(id) }.onSuccess { active = it.data; refresh() }.onFailure { error = it.message }; busy = false } },
                onStatus = { id, status -> busy = true; scope.launch { runCatching { CaptainApiProvider.api.updateTripStatus(id, TripStatusRequest(status)) }.onSuccess { active = it.data; refresh() }.onFailure { error = it.message }; busy = false } })
            1 -> Trips(Modifier.padding(padding), trips)
            else -> Account(Modifier.padding(padding), driver, onLogout)
        }
    }
}

@Composable
private fun Dashboard(modifier: Modifier, driver: Driver?, trips: List<Trip>, available: List<Trip>, active: Trip?, busy: Boolean, error: String?, onToggle: () -> Unit, onAccept: (String) -> Unit, onStatus: (String, String) -> Unit) {
    val online = driver?.available == true
    val completed = trips.count { it.status == "completed" }
    val earnings = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Text("أهلاً ${driver?.name?.ifBlank { "كابتن" } ?: "كابتن"}", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black) }
        item { Availability(online, busy, onToggle) }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Metric("الأرباح", formatMoney(earnings), "ل.س", Modifier.weight(1f)); Metric("المكتملة", completed.toString(), "رحلة", Modifier.weight(1f)); Metric("التقييم", String.format("%.1f", driver?.rating ?: 0.0), "نجمة", Modifier.weight(1f)) } }
        error?.let { item { Text(it, color = Red, fontSize = 10.sp) } }
        if (active != null) item { ActiveTrip(active, busy, onStatus) }
        else if (!online) item { Empty("أنت غير متصل", "فعّل الاتصال لاستقبال الرحلات.") }
        else if (available.isEmpty()) item { Empty("لا توجد طلبات الآن", "سيتم التحديث تلقائياً.") }
        else items(available.take(5), key = { it.id }) { Incoming(it, busy, onAccept) }
    }
}

@Composable private fun Availability(online: Boolean, busy: Boolean, onToggle: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onToggle), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(if (online) GreenDark else White)) { Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(44.dp).clip(CircleShape).background(if (online) White.copy(.12f) else Mint), contentAlignment = Alignment.Center) { Text(if (online) "✓" else "○", color = if (online) White else Green) }; Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(if (online) "متصل الآن" else "غير متصل", color = if (online) White else Ink, fontSize = 17.sp, fontWeight = FontWeight.Black); Text(if (online) "جاهز لاستقبال الرحلات" else "اضغط للتبديل إلى وضع العمل", color = if (online) White.copy(.72f) else Muted, fontSize = 10.sp) } } } }
@Composable private fun Metric(title: String, value: String, suffix: String, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(12.dp)) { Text(title, color = Muted, fontSize = 9.sp); Text(value, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black); Text(suffix, color = Muted, fontSize = 8.sp) } } }
@Composable private fun Incoming(trip: Trip, busy: Boolean, onAccept: (String) -> Unit) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text("طلب رحلة", color = Amber, fontWeight = FontWeight.Bold); Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 10.sp) }; Text(formatMoney(trip.estimatedFare), color = Green, fontSize = 20.sp, fontWeight = FontWeight.Black) }; Text("من: ${coords(trip.pickup)}", color = Ink, fontSize = 10.sp); Text("إلى: ${coords(trip.destination)}", color = Ink, fontSize = 10.sp); CaptainButton("قبول الرحلة", !busy, Modifier.fillMaxWidth()) { onAccept(trip.id) } } } }
@Composable private fun ActiveTrip(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit) { val next = when (trip.status) { "driver_assigned" -> "arriving"; "arriving" -> "in_progress"; "in_progress" -> "completed"; else -> null }; val label = when (trip.status) { "driver_assigned" -> "أنا في الطريق"; "arriving" -> "وصلت إلى الراكب"; "in_progress" -> "إنهاء الرحلة"; else -> "مكتملة" }; Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(GreenDark)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("الرحلة الحالية", color = White.copy(.7f), fontSize = 10.sp); Text(label, color = White, fontSize = 18.sp, fontWeight = FontWeight.Black); Text("${formatMoney(trip.estimatedFare)} ${trip.currency}", color = White, fontSize = 16.sp); Text("${coords(trip.pickup)} → ${coords(trip.destination)}", color = White.copy(.8f), fontSize = 9.sp); next?.let { CaptainButton(label, !busy, Modifier.fillMaxWidth(), filled = true, textColor = GreenDark) { onStatus(trip.id, it) } } } } }
@Composable private fun Trips(modifier: Modifier, trips: List<Trip>) { LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("رحلاتي", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black) }; if (trips.isEmpty()) item { Empty("لا توجد رحلات", "ستظهر الرحلات هنا بعد قبولها.") } else items(trips, key = { it.id }) { trip -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) { Row(Modifier.padding(15.dp).fillMaxWidth()) { Column(Modifier.weight(1f)) { Text("رحلة #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black); Text(statusText(trip.status), color = Muted, fontSize = 9.sp) }; Text(formatMoney(trip.estimatedFare), color = Green, fontWeight = FontWeight.Black) } } } } } }
@Composable private fun Account(modifier: Modifier, driver: Driver?, onLogout: () -> Unit) { Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("حسابي", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black); Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(driver?.name ?: "الكابتن", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(driver?.phone ?: "", color = Muted, fontSize = 10.sp); Text("المركبة: ${driver?.vehicle ?: "غير مسجلة"}", color = Ink, fontSize = 10.sp); Text("اللوحة: ${driver?.plate ?: "غير مسجلة"}", color = Ink, fontSize = 10.sp); Text("الحالة: ${if (driver?.available == true) "متصل" else "غير متصل"}", color = Muted, fontSize = 10.sp) } }; CaptainButton("تسجيل الخروج", true, Modifier.fillMaxWidth(), outlined = true, textColor = Red) { onLogout() } } }
@Composable private fun Empty(title: String, subtitle: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(22.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color = Ink, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 10.sp) } } }
@Composable private fun CaptainButton(text: String, enabled: Boolean, modifier: Modifier = Modifier, filled: Boolean = false, outlined: Boolean = false, textColor: Color? = null, onClick: () -> Unit) { val bg = if (filled) White else White.takeIf { outlined } ?: Green; val fg = textColor ?: if (filled) GreenDark else if (outlined) Ink else White; Box(modifier.clip(RoundedCornerShape(16.dp)).background(bg).then(if (outlined) Modifier.border(1.dp, Border, RoundedCornerShape(16.dp)) else Modifier).clickable(enabled = enabled, onClick = onClick).padding(vertical = 13.dp), contentAlignment = Alignment.Center) { if (enabled) Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black) else CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp) } }
private fun coords(c: Coordinates) = "${String.format("%.4f", c.lat)} ، ${String.format("%.4f", c.lng)}"
private fun formatMoney(v: Int) = "%,d".format(v)
private fun statusText(v: String) = when (v) { "searching" -> "بانتظار كابتن"; "driver_assigned" -> "تم قبول الرحلة"; "arriving" -> "في الطريق"; "in_progress" -> "الرحلة جارية"; "completed" -> "مكتملة"; else -> v }
