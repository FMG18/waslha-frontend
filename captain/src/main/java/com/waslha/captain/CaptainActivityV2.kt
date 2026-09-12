package com.waslha.captain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
            if (signedIn) {
                CaptainShellV2(session) {
                    session.clear()
                    signedIn = false
                }
            } else {
                CaptainLoginV2 { result ->
                    session.save(result)
                    signedIn = true
                }
            }
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

    fun login() {
        if (code.length < 4) {
            error = "أدخل رمز التحقق"
            return
        }
        loading = true
        error = null
        scope.launch {
            runCatching {
                CaptainApiProvider.api.verifyCode(VerifyOtpRequest(phone.trim(), code.trim()))
            }.onSuccess { response ->
                val data = response.data
                if (response.success && data != null && data.role.equals("driver", true)) {
                    onSuccess(data)
                } else error = response.message ?: "الحساب غير مسجل ككابتن"
            }.onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            loading = false
        }
    }

    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(Green), contentAlignment = Alignment.Center) {
                    Text("و", color = White, fontSize = 38.sp, fontWeight = FontWeight.Black)
                }
                Text("وصلها كابتن", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("لوحة عملك لاستقبال وإدارة الرحلات", color = Muted, fontSize = 12.sp)
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
                    devCode?.let { Text("رمز الاختبار: $it", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                error?.let { Text(it, color = Red, fontSize = 12.sp) }
                V2Button(if (sent) "دخول" else "إرسال الرمز", !loading) {
                    if (sent) login() else sendCode()
                }
                if (sent) {
                    TextButton(onClick = { sent = false; code = ""; devCode = null; error = null }) {
                        Text("تغيير الرقم", color = Green)
                    }
                }
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
    var dismissed by remember { mutableStateOf<Set<String>>(emptySet()) }
    var active by remember { mutableStateOf<Trip?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.me() }
            .onSuccess { driver = it.data }
            .onFailure { error = it.message ?: "تعذر تحميل الحساب" }
        runCatching { CaptainApiProvider.api.trips() }
            .onSuccess { response ->
                trips = response.data.orEmpty()
                active = trips.firstOrNull { it.status in setOf("driver_assigned", "arriving", "in_progress") }
            }
            .onFailure { error = it.message ?: "تعذر تحميل الرحلات" }
        if (driver?.available == true && active == null) {
            runCatching { CaptainApiProvider.api.availableTrips() }
                .onSuccess { available = it.data.orEmpty() }
                .onFailure { error = it.message ?: "تعذر تحميل الطلبات" }
        } else if (active != null || driver?.available != true) {
            available = emptyList()
        }
        refreshTick++
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
        containerColor = Background,
        bottomBar = {
            NavigationBar(containerColor = White) {
                V2NavItem(tab == 0, { tab = 0 }, "⌂", "الرئيسية")
                V2NavItem(tab == 1, { tab = 1 }, "↺", "رحلاتي")
                V2NavItem(tab == 2, { tab = 2 }, "○", "حسابي")
            }
        }
    ) { padding ->
        when (tab) {
            0 -> CaptainHomeV2(
                Modifier.padding(padding),
                driver,
                trips,
                available.filterNot { it.id in dismissed },
                active,
                busy,
                error,
                refreshTick,
                onToggle = {
                    busy = true
                    error = null
                    scope.launch {
                        runCatching {
                            CaptainApiProvider.api.availability(
                                DriverAvailabilityRequest(driver?.available != true)
                            )
                        }.onSuccess { driver = it.data }
                            .onFailure { error = it.message ?: "تعذر تغيير الحالة" }
                        busy = false
                    }
                },
                onAccept = { id ->
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.acceptTrip(id) }
                            .onSuccess { active = it.data; dismissed = emptySet(); refresh() }
                            .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                        busy = false
                    }
                },
                onDismiss = { id -> dismissed = dismissed + id },
                onStatus = { id, status ->
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.updateTripStatus(id, TripStatusRequest(status)) }
                            .onSuccess { active = it.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                        busy = false
                    }
                }
            )
            1 -> CaptainTripsV2(Modifier.padding(padding), trips)
            else -> CaptainAccountV2(Modifier.padding(padding), driver, onLogout)
        }
    }
}

@Composable
private fun CaptainHomeV2(
    modifier: Modifier,
    driver: Driver?,
    trips: List<Trip>,
    available: List<Trip>,
    active: Trip?,
    busy: Boolean,
    error: String?,
    refreshTick: Int,
    onToggle: () -> Unit,
    onAccept: (String) -> Unit,
    onDismiss: (String) -> Unit,
    onStatus: (String, String) -> Unit
) {
    val online = driver?.available == true
    val completed = trips.count { it.status == "completed" }
    val earnings = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }
    val todayRequests = available.size

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { HomeHeader(driver?.name, online) }
        item { QuickStatusCard(online, busy, onToggle) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Metric("الأرباح", formatMoneyV2(earnings), "ل.س", Modifier.weight(1f))
                Metric("المنجزة", completed.toString(), "رحلة", Modifier.weight(1f))
                Metric("المتاحة", todayRequests.toString(), "طلبات", Modifier.weight(1f))
            }
        }
        item { MiniMap(active, driver) }
        error?.let { item { ErrorBannerV2(it) } }
        if (active != null) {
            item { SectionTitleV2("الرحلة الحالية", "نفّذ الخطوة التالية من هنا") }
            item { ActiveTripV2(active, busy, onStatus) }
        } else {
            item {
                SectionTitleV2(
                    "طلبات جديدة",
                    if (online) "${available.size} طلب قريب" else "اتصل حتى تبدأ باستقبال الرحلات"
                )
            }
            if (!online) {
                item { EmptyV2("أنت غير متصل", "شغّل حالة الاتصال من البطاقة بالأعلى.") }
            } else if (available.isEmpty()) {
                item { EmptyV2("لا توجد طلبات الآن", "سيتم تحديث الطلبات تلقائياً.") }
            } else {
                items(available, key = { it.id }) { trip ->
                    IncomingTripV2(trip, busy, onAccept, onDismiss)
                }
            }
        }
        item { Text("آخر تحديث: $refreshTick", color = Color.Transparent, fontSize = 1.sp) }
    }
}

@Composable
private fun HomeHeader(name: String?, online: Boolean) {
    Row(Modifier.fillMaxWidth().padding(top = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("لوحة الكابتن", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(name?.ifBlank { "أهلاً كابتن" } ?: "أهلاً كابتن", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text(if (online) "جاهز لاستقبال الرحلات" else "فعّل الاتصال وابدأ العمل", color = Muted, fontSize = 11.sp)
        }
        Box(Modifier.size(48.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
            Text(if (online) "●" else "○", color = Green, fontSize = 20.sp)
        }
    }
}

@Composable
private fun QuickStatusCard(online: Boolean, busy: Boolean, onToggle: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onToggle),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(if (online) GreenDark else White)
    ) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(if (online) White.copy(alpha = .12f) else Mint), contentAlignment = Alignment.Center) {
                Text(if (online) "✓" else "○", color = if (online) White else Green, fontSize = 21.sp, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(if (online) "متصل الآن" else "غير متصل", color = if (online) White else Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                Text(if (online) "طلبات جديدة ستظهر هنا" else "اضغط للتبديل إلى وضع العمل", color = if (online) White.copy(alpha = .72f) else Muted, fontSize = 10.sp)
            }
            StatusTag(if (online) "إيقاف" else "تشغيل", online)
        }
    }
}

@Composable
private fun Metric(title: String, value: String, suffix: String, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(White)) {
        Column(Modifier.padding(13.dp)) {
            Text(title, color = Muted, fontSize = 9.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(suffix, color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun MiniMap(active: Trip?, driver: Driver?) {
    Card(Modifier.fillMaxWidth().height(160.dp), shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(Color(0xFFE7EEEA))) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.padding(15.dp)) {
                Surface(color = White.copy(alpha = .94f), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Text(if (active == null) "موقعك الحالي" else "متابعة الرحلة", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (active == null) "الموقع متزامن مع الخادم" else "الرحلة قيد المتابعة",
                            color = Muted,
                            fontSize = 9.sp
                        )
                    }
                }
            }
            Box(Modifier.align(Alignment.Center).size(84.dp).clip(CircleShape).background(Mint.copy(alpha = .65f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Green), contentAlignment = Alignment.Center) {
                    Text("🚕", fontSize = 16.sp)
                }
            }
            Surface(Modifier.align(Alignment.BottomEnd).padding(12.dp), color = White.copy(alpha = .92f), shape = RoundedCornerShape(12.dp)) {
                Text(
                    "${driver?.lat?.let { String.format("%.3f", it) } ?: "--"}, ${driver?.lng?.let { String.format("%.3f", it) } ?: "--"}",
                    Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    color = Muted,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun SectionTitleV2(title: String, subtitle: String?) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
            subtitle?.let { Text(it, color = Muted, fontSize = 10.sp) }
        }
    }
}

@Composable
private fun IncomingTripV2(trip: Trip, busy: Boolean, onAccept: (String) -> Unit, onDismiss: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Amber))
                        Spacer(Modifier.width(7.dp))
                        Text("طلب وارد", color = Amber, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text("رحلة جديدة", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatMoneyV2(trip.estimatedFare), color = Green, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(trip.currency, color = Muted, fontSize = 9.sp)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Info("${trip.distanceKm} كم")
                Info("${trip.durationMin} دقيقة")
                Info(if (trip.paymentMethod == "cash") "نقدي" else trip.paymentMethod)
            }
            RouteCard(trip)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                SecondaryButton("تجاهل", !busy) { onDismiss(trip.id) }
                V2Button("قبول الرحلة", !busy, Modifier.weight(1f)) { onAccept(trip.id) }
            }
        }
    }
}

@Composable
private fun RouteCard(trip: Trip) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Background)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Green))
                Box(Modifier.width(2.dp).height(28.dp).background(Border))
                Box(Modifier.size(10.dp).clip(CircleShape).background(Red))
            }
            Column(Modifier.padding(start = 11.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("موقع الانطلاق", color = Muted, fontSize = 9.sp)
                Text(formatCoordinatesV2(trip.pickup), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("الوجهة", color = Muted, fontSize = 9.sp)
                Text(formatCoordinatesV2(trip.destination), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActiveTripV2(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit) {
    val next = when (trip.status) {
        "driver_assigned" -> "arriving"
        "arriving" -> "in_progress"
        "in_progress" -> "completed"
        else -> null
    }
    val action = when (trip.status) {
        "driver_assigned" -> "أنا في الطريق"
        "arriving" -> "وصلت إلى الراكب"
        "in_progress" -> "إنهاء الرحلة"
        else -> null
    }
    val state = when (trip.status) {
        "driver_assigned" -> 1
        "arriving" -> 2
        "in_progress" -> 3
        "completed" -> 4
        else -> 1
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(GreenDark)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("رحلتك الحالية", color = White.copy(alpha = .68f), fontSize = 10.sp)
                    Text(activeStatus(state), color = White, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
                StatusTag("نشطة", true)
            }
            ProgressSteps(state)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActiveMetric("المبلغ", formatMoneyV2(trip.estimatedFare), Modifier.weight(1f))
                ActiveMetric("المسافة", "${trip.distanceKm} كم", Modifier.weight(1f))
                ActiveMetric("الوقت", "${trip.durationMin} د", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth().background(White.copy(alpha = .07f), RoundedCornerShape(17.dp)).padding(13.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("من", color = White.copy(alpha = .58f), fontSize = 8.sp)
                    Text(formatCoordinatesV2(trip.pickup), color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("إلى", color = White.copy(alpha = .58f), fontSize = 8.sp)
                    Text(formatCoordinatesV2(trip.destination), color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (next != null && action != null) {
                V2Button(action, !busy, Modifier.fillMaxWidth(), filled = true) { onStatus(trip.id, next) }
            }
        }
    }
}

@Composable
private fun ProgressSteps(state: Int) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..4) {
            Box(Modifier.size(25.dp).clip(CircleShape).background(if (i <= state) White else White.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                Text(if (i < state) "✓" else i.toString(), color = if (i <= state) GreenDark else White.copy(alpha = .6f), fontSize = 10.sp, fontWeight = FontWeight.Black)
            }
            if (i < 4) {
                Box(Modifier.weight(1f).height(2.dp).background(if (i < state) White else White.copy(alpha = .15f)))
            }
        }
    }
}

private fun activeStatus(state: Int): String = when (state) {
    1 -> "تم قبول الرحلة"
    2 -> "أنت في الطريق"
    3 -> "الرحلة جارية"
    else -> "الرحلة مكتملة"
}

@Composable
private fun ActiveMetric(title: String, value: String, modifier: Modifier) {
    Column(modifier.background(White.copy(alpha = .07f), RoundedCornerShape(15.dp)).padding(10.dp)) {
        Text(title, color = White.copy(alpha = .62f), fontSize = 8.sp)
        Text(value, color = White, fontSize = 11.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun EmptyV2(title: String, subtitle: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(White)) {
        Column(Modifier.fillMaxWidth().padding(23.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                Text("○", color = Green, fontSize = 24.sp)
            }
            Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ErrorBannerV2(text: String) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), color = Color(0xFFFFE9E7)) {
        Text(text, Modifier.padding(12.dp), color = Red, fontSize = 10.sp)
    }
}

@Composable
private fun CaptainTripsV2(modifier: Modifier, trips: List<Trip>) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        item {
            Text("رحلاتي", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("سجل الرحلات والعمليات الأخيرة", color = Muted, fontSize = 11.sp)
        }
        if (trips.isEmpty()) {
            item { EmptyV2("لا توجد رحلات", "ستظهر هنا الرحلات التي تقبلها وتنجزها.") }
        } else {
            items(trips, key = { it.id }) { trip ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(White)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("رحلة #${trip.id.takeLast(6)}", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                Text(statusTextV2(trip.status), color = Muted, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatMoneyV2(trip.estimatedFare), color = Green, fontSize = 15.sp, fontWeight = FontWeight.Black)
                                Text(trip.currency, color = Muted, fontSize = 8.sp)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Info("${trip.distanceKm} كم")
                            Info("${trip.durationMin} د")
                            Info(if (trip.paymentMethod == "cash") "نقدي" else trip.paymentMethod)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptainAccountV2(modifier: Modifier, driver: Driver?, onLogout: () -> Unit) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("حسابي", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(27.dp), colors = CardDefaults.cardColors(White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                        Text("و", color = Green, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(driver?.phone ?: "", color = Muted, fontSize = 10.sp)
                    }
                }
                DividerV2()
                ProfileV2("المركبة", driver?.vehicle?.ifBlank { "غير مسجلة" } ?: "غير مسجلة")
                ProfileV2("رقم اللوحة", driver?.plate?.ifBlank { "غير مسجل" } ?: "غير مسجل")
                ProfileV2("التقييم", if ((driver?.rating ?: 0.0) > 0) String.format("%.1f / 5.0", driver?.rating) else "لا يوجد")
                ProfileV2("الحالة", if (driver?.available == true) "متصل" else "غير متصل")
            }
        }
        V2Button("تسجيل الخروج", true, outlined = true, textColor = Red) { onLogout() }
    }
}

@Composable
private fun ProfileV2(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
        Text(value, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DividerV2() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Border))
}

@Composable
private fun Info(text: String) {
    Surface(shape = RoundedCornerShape(999.dp), color = Mint) {
        Text(text, Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = GreenDark, fontSize = 8.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusTag(text: String, active: Boolean) {
    Surface(shape = RoundedCornerShape(999.dp), color = if (active) White.copy(alpha = .12f) else Mint) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = if (active) White else GreenDark, fontSize = 9.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun SecondaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(Background)
            .border(1.dp, Border, RoundedCornerShape(16.dp)).clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun V2Button(
    text: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    outlined: Boolean = false,
    textColor: Color? = null,
    onClick: () -> Unit
) {
    val bg = when {
        filled -> White
        outlined -> White
        else -> Green
    }
    val fg = textColor ?: when {
        filled -> GreenDark
        outlined -> Ink
        else -> White
    }
    Box(
        modifier.clip(RoundedCornerShape(17.dp)).background(bg)
            .then(if (outlined) Modifier.border(1.dp, Border, RoundedCornerShape(17.dp)) else Modifier)
            .clickable(enabled = enabled, onClick = onClick).padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!enabled) CircularProgressIndicator(Modifier.size(18.dp), color = fg, strokeWidth = 2.dp)
        else Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun V2NavItem(selected: Boolean, onClick: () -> Unit, icon: String, label: String) {
    NavigationBarItem(selected = selected, onClick = onClick, icon = { Text(icon, fontSize = 18.sp) }, label = { Text(label) })
}

private fun statusTextV2(status: String): String = when (status) {
    "searching" -> "بانتظار كابتن"
    "driver_assigned" -> "تم قبول الرحلة"
    "arriving" -> "في الطريق إلى الراكب"
    "in_progress" -> "الرحلة جارية"
    "completed" -> "مكتملة"
    else -> status
}

private fun formatMoneyV2(value: Int): String = "%,d".format(value)

private fun formatCoordinatesV2(coordinates: Coordinates): String =
    "${String.format("%.4f", coordinates.lat)} ، ${String.format("%.4f", coordinates.lng)}"
