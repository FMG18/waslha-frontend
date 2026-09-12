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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Green = Color(0xFF0B805E)
private val Dark = Color(0xFF075B43)
private val Mint = Color(0xFFE8F5F0)
private val Bg = Color(0xFFF4F7F6)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color.White
private val Red = Color(0xFFB42318)
private val Amber = Color(0xFFB54708)

class CaptainActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        setContent { CaptainApp() }
    }
}

@Composable
private fun CaptainApp() {
    val context = LocalContext.current
    val session = remember { CaptainSession(context) }
    var signedIn by remember { mutableStateOf(session.isSignedIn) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            if (signedIn) CaptainShell { session.clear(); signedIn = false }
            else CaptainLogin { response -> session.save(response); signedIn = true }
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
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        val value = phone.trim()
        if (value.filter(Char::isDigit).length < 8) { error = "أدخل رقم هاتف صحيح"; return }
        if (sent && code.length < 4) { error = "أدخل رمز التحقق"; return }
        busy = true
        error = null
        scope.launch {
            if (!sent) {
                runCatching { CaptainApiProvider.api.requestCode(OtpRequest(value)) }
                    .onSuccess { r ->
                        if (r.success) { sent = true; devCode = r.data?.devCode }
                        else error = r.message ?: "تعذر إرسال الرمز"
                    }
                    .onFailure { error = it.message ?: "تعذر الاتصال بالخادم" }
            } else {
                runCatching { CaptainApiProvider.api.verifyCode(VerifyOtpRequest(value, code.trim())) }
                    .onSuccess { r ->
                        val data = r.data
                        if (r.success && data != null && data.role.equals("driver", true)) onSuccess(data)
                        else error = r.message ?: "الحساب غير مسجل ككابتن"
                    }
                    .onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(White)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Green), contentAlignment = Alignment.Center) { Text("و", color = White, fontSize = 38.sp, fontWeight = FontWeight.Black) }
                Text("وصلها كابتن", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("إدارة الرحلات واستقبال الطلبات", color = Muted, fontSize = 12.sp)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رقم الهاتف") })
                if (sent) {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رمز التحقق") })
                    devCode?.let { Text("رمز الاختبار: $it", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                error?.let { Text(it, color = Red, fontSize = 11.sp, textAlign = TextAlign.Center) }
                CaptainButton(if (sent) "دخول" else "إرسال الرمز", !busy, Modifier.fillMaxWidth()) { submit() }
                if (sent) TextButton(onClick = { sent = false; code = ""; devCode = null; error = null }) { Text("تغيير الرقم", color = Green) }
            }
        }
    }
}

@Composable
private fun CaptainShell(onLogout: () -> Unit) {
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
        runCatching { CaptainApiProvider.api.trips() }.onSuccess { r ->
            trips = r.data.orEmpty()
            active = trips.firstOrNull { it.status in setOf("driver_assigned", "arriving", "in_progress") }
        }.onFailure { error = it.message ?: "تعذر تحميل الرحلات" }
        if (driver?.available == true && active == null) {
            runCatching { CaptainApiProvider.api.availableTrips() }.onSuccess { available = it.data.orEmpty() }.onFailure { error = it.message ?: "تعذر تحميل الطلبات" }
        } else available = emptyList()
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(driver?.available, active?.id) {
        if (driver?.available != true) return@LaunchedEffect
        while (true) { delay(5000); refresh() }
    }

    Scaffold(containerColor = Bg, bottomBar = {
        NavigationBar(containerColor = White) {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂") }, label = { Text("الرئيسية") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("↺") }, label = { Text("رحلاتي") })
            NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("○") }, label = { Text("حسابي") })
        }
    }) { padding ->
        when (tab) {
            0 -> CaptainDashboard(Modifier.padding(padding), driver, trips, available, active, busy, error,
                onToggle = {
                    busy = true
                    scope.launch {
                        runCatching { CaptainApiProvider.api.availability(DriverAvailabilityRequest(driver?.available != true)) }
                            .onSuccess { driver = it.data }
                            .onFailure { error = it.message ?: "تعذر تغيير الحالة" }
                        busy = false
                    }
                },
                onAccept = { id ->
                    busy = true
                    scope.launch {
                        runCatching { CaptainApiProvider.api.acceptTrip(id) }
                            .onSuccess { response -> active = response.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                        busy = false
                    }
                },
                onStatus = { id, status ->
                    busy = true
                    scope.launch {
                        runCatching { CaptainApiProvider.api.updateTripStatus(id, TripStatusRequest(status)) }
                            .onSuccess { response -> active = response.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                        busy = false
                    }
                })
            1 -> CaptainTrips(Modifier.padding(padding), trips)
            else -> CaptainAccount(Modifier.padding(padding), driver, onLogout)
        }
    }
}

@Composable
private fun CaptainDashboard(modifier: Modifier, driver: Driver?, trips: List<Trip>, available: List<Trip>, active: Trip?, busy: Boolean, error: String?, onToggle: () -> Unit, onAccept: (String) -> Unit, onStatus: (String, String) -> Unit) {
    val online = driver?.available == true
    val completed = trips.count { it.status == "completed" }
    val income = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("لوحة الكابتن", color = Muted, fontSize = 11.sp); Text(driver?.name ?: "أهلاً كابتن", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black) }
                Box(Modifier.size(46.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) { Text("و", color = Green, fontSize = 21.sp, fontWeight = FontWeight.Black) }
            }
        }
        item {
            Card(Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onToggle), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(if (online) Dark else White)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(if (online) White.copy(.12f) else Mint), contentAlignment = Alignment.Center) { Text(if (online) "✓" else "○", color = if (online) White else Green, fontSize = 20.sp, fontWeight = FontWeight.Black) }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(if (online) "متصل الآن" else "غير متصل", color = if (online) White else Ink, fontSize = 16.sp, fontWeight = FontWeight.Black); Text(if (online) "جاهز لاستقبال الرحلات" else "اضغط للتفعيل", color = if (online) White.copy(.72f) else Muted, fontSize = 10.sp) }
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricCard("الأرباح", "%,d".format(income), "ل.س", Modifier.weight(1f)); MetricCard("المكتملة", completed.toString(), "رحلة", Modifier.weight(1f)); MetricCard("المتاحة", available.size.toString(), "طلب", Modifier.weight(1f)) } }
        error?.let { item { Text(it, color = Red, fontSize = 10.sp) } }
        if (active != null) {
            item { Text("الرحلة الحالية", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
            item { ActiveTripCard(active, busy, onStatus) }
        } else {
            item { Text("طلبات جديدة", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
            if (!online) item { EmptyCard("أنت غير متصل", "فعّل الاتصال حتى تبدأ باستقبال الرحلات") }
            else if (available.isEmpty()) item { EmptyCard("لا توجد طلبات الآن", "سيتم تحديث الطلبات تلقائياً") }
            else items(available, key = { it.id }) { trip -> IncomingTrip(trip, busy, onAccept) }
        }
    }
}

@Composable private fun MetricCard(title: String, value: String, suffix: String, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(12.dp)) { Text(title, color = Muted, fontSize = 9.sp); Text(value, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Black); Text(suffix, color = Muted, fontSize = 8.sp) } } }

@Composable private fun IncomingTrip(trip: Trip, busy: Boolean, onAccept: (String) -> Unit) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text("طلب #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black); Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 10.sp) }; Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black) }; Text("${trip.pickup.lat}, ${trip.pickup.lng} → ${trip.destination.lat}, ${trip.destination.lng}", color = Muted, fontSize = 9.sp); CaptainButton("قبول الرحلة", !busy, Modifier.fillMaxWidth()) { onAccept(trip.id) } } } }

@Composable private fun ActiveTripCard(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit) { val next = when (trip.status) { "driver_assigned" -> "arriving"; "arriving" -> "in_progress"; "in_progress" -> "completed"; else -> null }; val label = when (trip.status) { "driver_assigned" -> "أنا في الطريق"; "arriving" -> "وصلت للراكب"; "in_progress" -> "إنهاء الرحلة"; else -> "مكتملة" }; Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Dark)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("${trip.estimatedFare} ${trip.currency}", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black); Text("${trip.pickup.lat}, ${trip.pickup.lng} → ${trip.destination.lat}, ${trip.destination.lng}", color = White.copy(.78f), fontSize = 9.sp); next?.let { CaptainButton(label, !busy, Modifier.fillMaxWidth(), filled = true, textColor = Dark) { onStatus(trip.id, it) } } } } }

@Composable private fun CaptainTrips(modifier: Modifier, trips: List<Trip>) { LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("رحلاتي", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black); Text("سجل الرحلات والدخل", color = Muted, fontSize = 11.sp) }; if (trips.isEmpty()) item { EmptyCard("لا توجد رحلات", "ستظهر الرحلات هنا بعد قبولها") } else items(trips, key = { it.id }) { trip -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("رحلة #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black); Text(statusText(trip.status), color = Muted, fontSize = 9.sp) }; Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black) } } } } }

@Composable private fun CaptainAccount(modifier: Modifier, driver: Driver?, onLogout: () -> Unit) { val context = LocalContext.current; LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("حسابي", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black) }; item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(White)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(56.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) { Text("و", color = Green, fontSize = 23.sp, fontWeight = FontWeight.Black) }; Spacer(Modifier.size(12.dp)); Column(Modifier.weight(1f)) { Text(driver?.name ?: "الكابتن", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black); Text(driver?.phone ?: "", color = Muted, fontSize = 10.sp) } } } }; item { AccountAction("الملف والمركبة", "بيانات الحساب والمركبة") { context.startActivity(Intent(context, CaptainAccountCenterActivity::class.java)) } }; item { AccountAction("الإشعارات", "التنبيهات") { context.startActivity(Intent(context, CaptainNotificationsActivity::class.java)) } }; item { AccountAction("الأرباح", "الدخل وسجل العمليات") { context.startActivity(Intent(context, CaptainEarningsActivity::class.java)) } }; item { CaptainButton("تسجيل الخروج", true, Modifier.fillMaxWidth(), textColor = Red) { onLogout() } } } }

@Composable private fun AccountAction(title: String, subtitle: String, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(White)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Mint), contentAlignment = Alignment.Center) { Text("•", color = Green, fontSize = 18.sp) }; Column(Modifier.weight(1f).padding(horizontal = 11.dp)) { Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 9.sp) }; Text("‹", color = Muted, fontSize = 22.sp) } } }
@Composable private fun EmptyCard(title: String, subtitle: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 5.dp)) } } }
@Composable private fun CaptainButton(text: String, enabled: Boolean, modifier: Modifier, filled: Boolean = false, outlined: Boolean = false, textColor: Color? = null, onClick: () -> Unit) { val bg = if (filled) White else White.takeIf { outlined } ?: Green; val fg = textColor ?: if (filled) Dark else if (outlined) Ink else White; Box(modifier.clip(RoundedCornerShape(16.dp)).background(bg).clickable(enabled = enabled, onClick = onClick).padding(vertical = 13.dp), contentAlignment = Alignment.Center) { if (enabled) Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black) else CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp) } }
private fun statusText(status: String): String = when (status) { "searching" -> "بانتظار كابتن"; "driver_assigned" -> "تم قبول الرحلة"; "arriving" -> "في الطريق"; "in_progress" -> "الرحلة جارية"; "completed" -> "مكتملة"; "cancelled" -> "ملغاة"; else -> status }
