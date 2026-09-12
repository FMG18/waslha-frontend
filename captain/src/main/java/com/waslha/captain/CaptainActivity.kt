package com.waslha.captain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val WaslhaGreen = Color(0xFF0B805E)
private val WaslhaGreenDark = Color(0xFF075B43)
private val WaslhaMint = Color(0xFFE8F5F0)
private val AppBackground = Color(0xFFF4F7F6)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color(0xFFFFFFFF)
private val Border = Color(0xFFE0E7E3)
private val Red = Color(0xFFB42318)
private val Amber = Color(0xFFB54708)

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
        Surface(modifier = Modifier.fillMaxSize(), color = AppBackground) {
            if (signedIn) {
                CaptainShell(session) {
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
                    } else error = response.message ?: "تعذر إرسال رمز التحقق"
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

    Box(modifier = Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(74.dp).background(WaslhaGreen, RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("و", color = White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                }
                Text("وصلها كابتن", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("مساحة العمل الخاصة بالكابتن", color = Muted, fontSize = 13.sp)
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("رقم الهاتف") }
                )
                if (sent) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.filter(Char::isDigit).take(6) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("رمز التحقق") }
                    )
                    devCode?.let { Text("رمز الاختبار: $it", color = WaslhaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
                error?.let { Text(it, color = Red, fontSize = 12.sp) }
                PrimaryButton(
                    text = if (sent) "دخول إلى مساحة الكابتن" else "إرسال رمز التحقق",
                    enabled = !loading
                ) { if (sent) verifyCode() else requestCode() }
                if (sent) {
                    TextButton(onClick = { sent = false; code = ""; error = null; devCode = null }) {
                        Text("تغيير الرقم", color = WaslhaGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptainShell(session: CaptainSession, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var driver by remember { mutableStateOf<Driver?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var available by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var active by remember { mutableStateOf<Trip?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.me() }
            .onSuccess { driver = it.data }
            .onFailure { error = it.message ?: "تعذر تحميل الحساب" }
        runCatching { CaptainApiProvider.api.trips() }
            .onSuccess { result ->
                trips = result.data.orEmpty()
                active = trips.firstOrNull { it.status in setOf("driver_assigned", "arriving", "in_progress") }
            }
            .onFailure { error = it.message ?: "تعذر تحميل الرحلات" }
        if (driver?.available == true && active == null) {
            runCatching { CaptainApiProvider.api.availableTrips() }
                .onSuccess { available = it.data.orEmpty() }
                .onFailure { error = it.message ?: "تعذر تحميل الطلبات" }
        } else if (driver?.available != true || active != null) {
            available = emptyList()
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(driver?.available, active?.id) {
        if (driver?.available != true) return@LaunchedEffect
        while (true) {
            delay(5000)
            refresh()
        }
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            NavigationBar(containerColor = White) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂", fontSize = 18.sp) }, label = { Text("الرئيسية") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("↺", fontSize = 18.sp) }, label = { Text("الرحلات") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("○", fontSize = 18.sp) }, label = { Text("حسابي") })
            }
        }
    ) { padding ->
        when (tab) {
            0 -> CaptainDashboard(
                modifier = Modifier.padding(padding),
                driver = driver,
                trips = trips,
                availableTrips = available,
                activeTrip = active,
                busy = busy,
                error = error,
                onToggleAvailability = {
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.availability(DriverAvailabilityRequest(driver?.available != true)) }
                            .onSuccess { driver = it.data }
                            .onFailure { error = it.message ?: "تعذر تغيير حالة الاتصال" }
                        busy = false
                    }
                },
                onAccept = { tripId ->
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.acceptTrip(tripId) }
                            .onSuccess { active = it.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                        busy = false
                    }
                },
                onStatus = { tripId, status ->
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.updateTripStatus(tripId, TripStatusRequest(status)) }
                            .onSuccess { active = it.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                        busy = false
                    }
                }
            )
            1 -> CaptainTrips(Modifier.padding(padding), trips)
            else -> CaptainAccount(Modifier.padding(padding), driver, onLogout)
        }
    }
}

@Composable
private fun CaptainDashboard(
    modifier: Modifier,
    driver: Driver?,
    trips: List<Trip>,
    availableTrips: List<Trip>,
    activeTrip: Trip?,
    busy: Boolean,
    error: String?,
    onToggleAvailability: () -> Unit,
    onAccept: (String) -> Unit,
    onStatus: (String, String) -> Unit
) {
    val online = driver?.available == true
    val completed = trips.count { it.status == "completed" }
    val todayEarnings = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }
    val rating = driver?.rating ?: 0.0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("مساء الخير 👋", color = Muted, fontSize = 12.sp)
                    Text(driver?.name?.ifBlank { "كابتن وصلها" } ?: "كابتن وصلها", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(WaslhaMint),
                    contentAlignment = Alignment.Center
                ) { Text("و", color = WaslhaGreen, fontSize = 22.sp, fontWeight = FontWeight.Black) }
            }
        }
        item {
            CaptainMapCard(driver = driver, activeTrip = activeTrip)
        }
        item {
            AvailabilityCard(online, busy, onToggleAvailability)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("أرباح اليوم", formatMoney(todayEarnings), "ل.س", Modifier.weight(1f))
                MetricCard("رحلات مكتملة", completed.toString(), "رحلة", Modifier.weight(1f))
                MetricCard("التقييم", if (rating > 0) String.format("%.1f", rating) else "—", "نجمة", Modifier.weight(1f))
            }
        }
        if (error != null) {
            item { InlineError(error) }
        }
        if (activeTrip != null) {
            item {
                ActiveTripCard(activeTrip, busy, onStatus)
            }
        } else {
            item {
                SectionHeader("الطلبات القريبة", if (availableTrips.isEmpty()) null else "${availableTrips.size} متاح")
            }
            if (!online) {
                item { EmptyStateCard("اتصل بالإنترنت واستقبل الرحلات", "فعّل حالة الاتصال حتى تظهر الطلبات القريبة.") }
            } else if (availableTrips.isEmpty()) {
                item { EmptyStateCard("لا توجد رحلات حالياً", "سنحدّث الطلبات تلقائياً كل عدة ثوانٍ.") }
            } else {
                items(availableTrips.take(5)) { trip ->
                    IncomingTripCard(trip, busy, onAccept)
                }
            }
        }
    }
}

@Composable
private fun CaptainMapCard(driver: Driver?, activeTrip: Trip?) {
    Card(
        modifier = Modifier.fillMaxWidth().height(230.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF0ED))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val road = Path().apply {
                    moveTo(-20f, h * .72f)
                    cubicTo(w * .20f, h * .55f, w * .32f, h * .82f, w * .52f, h * .62f)
                    cubicTo(w * .68f, h * .48f, w * .74f, h * .44f, w + 20f, h * .24f)
                }
                drawPath(road, Color(0xFFFFFFFF), style = Stroke(width = 22f, cap = StrokeCap.Round))
                drawPath(road, Color(0xFFD4DDD8), style = Stroke(width = 12f, cap = StrokeCap.Round))
                val route = Path().apply {
                    moveTo(w * .10f, h * .82f)
                    cubicTo(w * .30f, h * .70f, w * .42f, h * .62f, w * .58f, h * .48f)
                    cubicTo(w * .70f, h * .36f, w * .80f, h * .26f, w * .92f, h * .18f)
                }
                drawPath(route, WaslhaGreen, style = Stroke(width = 6f, cap = StrokeCap.Round))
                drawCircle(Color.White, radius = 18f, center = Offset(w * .58f, h * .48f))
                drawCircle(WaslhaGreen, radius = 9f, center = Offset(w * .58f, h * .48f))
                drawCircle(Color.White, radius = 18f, center = Offset(w * .92f, h * .18f))
                drawCircle(Red, radius = 9f, center = Offset(w * .92f, h * .18f))
            }
            Column(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = White.copy(alpha = .94f)) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Text(if (activeTrip != null) "أنت في رحلة" else "موقعك الحالي", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (activeTrip != null) "تتبع الرحلة النشطة" else "الموقع متزامن",
                            color = Muted,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                shape = RoundedCornerShape(15.dp),
                color = White.copy(alpha = .96f)
            ) {
                Text(
                    text = "${driver?.lat?.let { String.format("%.4f", it) } ?: "--"}, ${driver?.lng?.let { String.format("%.4f", it) } ?: "--"}",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    color = Muted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun AvailabilityCard(online: Boolean, busy: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (online) WaslhaGreen else White)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (online) White.copy(alpha = .18f) else WaslhaMint),
                contentAlignment = Alignment.Center
            ) { Text(if (online) "●" else "○", color = if (online) White else WaslhaGreen, fontSize = 20.sp) }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(if (online) "أنت متصل" else "أنت غير متصل", color = if (online) White else Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(if (online) "تستقبل الرحلات الآن" else "اضغط لتبدأ استقبال الرحلات", color = if (online) White.copy(alpha = .82f) else Muted, fontSize = 11.sp)
            }
            StatusPill(if (online) "متصل" else "متوقف", online)
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, suffix: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = White)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Muted, fontSize = 10.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(4.dp))
                Text(suffix, color = Muted, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, trailing: String?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        trailing?.let { Text(it, color = WaslhaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun EmptyStateCard(title: String, subtitle: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = White)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(WaslhaMint), contentAlignment = Alignment.Center) {
                Text("↗", color = WaslhaGreen, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Text(title, color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun IncomingTripCard(trip: Trip, busy: Boolean, onAccept: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = White)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(WaslhaGreen))
                        Spacer(Modifier.width(7.dp))
                        Text("طلب جديد", color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp)
                    }
                    Text("يحتاج كابتن قريب", color = Muted, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMoney(trip.estimatedFare), color = WaslhaGreen, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(trip.currency, color = Muted, fontSize = 9.sp)
                }
            }
            Divider(color = Border)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoPill("${trip.distanceKm} كم")
                InfoPill("${trip.durationMin} دقيقة")
                InfoPill(trip.paymentMethod)
            }
            RouteSummary(trip)
            PrimaryButton("قبول الرحلة", !busy) { onAccept(trip.id) }
        }
    }
}

@Composable
private fun RouteSummary(trip: Trip) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(WaslhaGreen))
            Box(modifier = Modifier.width(1.dp).height(25.dp).background(Border))
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Red))
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("نقطة الانطلاق", color = Muted, fontSize = 10.sp)
            Text("الوجهة", color = Muted, fontSize = 10.sp)
        }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(formatCoordinates(trip.pickup), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(formatCoordinates(trip.destination), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActiveTripCard(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit) {
    val next = when (trip.status) {
        "driver_assigned" -> "arriving"
        "arriving" -> "in_progress"
        "in_progress" -> "completed"
        else -> null
    }
    val actionLabel = when (trip.status) {
        "driver_assigned" -> "أنا في الطريق إلى الراكب"
        "arriving" -> "وصلت إلى الراكب"
        "in_progress" -> "إنهاء الرحلة"
        else -> null
    }
    val statusTitle = when (trip.status) {
        "driver_assigned" -> "تم قبول الرحلة"
        "arriving" -> "أنت في الطريق"
        "in_progress" -> "الرحلة جارية"
        else -> "رحلة نشطة"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = WaslhaGreenDark)
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(statusTitle, color = White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                    Text("الرحلة الحالية", color = White.copy(alpha = .72f), fontSize = 11.sp)
                }
                StatusPill("نشطة", true, dark = true)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TripInfoCard("المبلغ", formatMoney(trip.estimatedFare) + " " + trip.currency, Modifier.weight(1f))
                TripInfoCard("المسافة", "${trip.distanceKm} كم", Modifier.weight(1f))
                TripInfoCard("الوقت", "${trip.durationMin} د", Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth().background(White.copy(alpha = .08f), RoundedCornerShape(18.dp)).padding(14.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("موقع الانطلاق", color = White.copy(alpha = .68f), fontSize = 9.sp)
                    Text(formatCoordinates(trip.pickup), color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("الوجهة", color = White.copy(alpha = .68f), fontSize = 9.sp)
                    Text(formatCoordinates(trip.destination), color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (next != null && actionLabel != null) {
                PrimaryButton(actionLabel, !busy, filled = true) { onStatus(trip.id, next) }
            }
        }
    }
}

@Composable
private fun TripInfoCard(title: String, value: String, modifier: Modifier) {
    Column(modifier = modifier.background(White.copy(alpha = .08f), RoundedCornerShape(16.dp)).padding(11.dp)) {
        Text(title, color = White.copy(alpha = .65f), fontSize = 9.sp)
        Text(value, color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CaptainTrips(modifier: Modifier, trips: List<Trip>) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("رحلاتي", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("سجل الرحلات والأداء السابق", color = Muted, fontSize = 11.sp)
        }
        if (trips.isEmpty()) {
            item { EmptyStateCard("لا توجد رحلات بعد", "ستظهر هنا الرحلات التي تقبلها وتنجزها.") }
        } else {
            items(trips) { trip ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = White)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("رحلة #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                Text(trip.status, color = Muted, fontSize = 10.sp)
                            }
                            Text(formatMoney(trip.estimatedFare) + " " + trip.currency, color = WaslhaGreen, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                        Divider(color = Border)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            InfoPill("${trip.distanceKm} كم")
                            InfoPill("${trip.durationMin} د")
                            InfoPill(trip.paymentMethod)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptainAccount(modifier: Modifier, driver: Driver?, onLogout: () -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("حسابي", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(58.dp).clip(CircleShape).background(WaslhaMint), contentAlignment = Alignment.Center) {
                        Text("و", color = WaslhaGreen, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(driver?.phone ?: "", color = Muted, fontSize = 10.sp)
                    }
                }
                Divider(color = Border)
                ProfileRow("المركبة", driver?.vehicle?.ifBlank { "غير مسجلة" } ?: "غير مسجلة")
                ProfileRow("رقم اللوحة", driver?.plate?.ifBlank { "غير مسجل" } ?: "غير مسجل")
                ProfileRow("التقييم", if ((driver?.rating ?: 0.0) > 0) String.format("%.1f / 5.0", driver?.rating) else "لا يوجد")
            }
        }
        PrimaryButton("تسجيل الخروج", true, outlined = true, textColor = Red) { onLogout() }
    }
}

@Composable
private fun ProfileRow(title: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Muted, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = WaslhaMint) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = WaslhaGreenDark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusPill(text: String, positive: Boolean, dark: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = when {
            dark -> White.copy(alpha = .12f)
            positive -> WaslhaMint
            else -> Color(0xFFF0F2F1)
        }
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = when {
                dark -> White
                positive -> WaslhaGreenDark
                else -> Muted
            },
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InlineError(message: String) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFFE9E7)) {
        Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp), color = Red, fontSize = 10.sp)
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    enabled: Boolean,
    filled: Boolean = false,
    outlined: Boolean = false,
    textColor: Color? = null,
    onClick: () -> Unit
) {
    val background = when {
        outlined -> White
        filled -> White
        else -> WaslhaGreen
    }
    val foreground = textColor ?: when {
        outlined -> Ink
        filled -> WaslhaGreenDark
        else -> White
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(17.dp))
            .background(background)
            .then(if (outlined) Modifier.border(1.dp, Border, RoundedCornerShape(17.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!enabled) {
            CircularProgressIndicator(modifier = Modifier.size(19.dp), color = foreground, strokeWidth = 2.dp)
        } else {
            Text(text, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun formatMoney(value: Int): String = "%,d".format(value)

private fun formatCoordinates(coordinates: Coordinates): String =
    "${coordinates.lat.roundToInt()}° ، ${coordinates.lng.roundToInt()}°"
