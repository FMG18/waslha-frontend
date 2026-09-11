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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

private val Green = Color(0xFF087F5B)
private val Mint = Color(0xFFE7F6F0)
private val Ink = Color(0xFF12201B)
private val Muted = Color(0xFF6D7A75)
private val Background = Color(0xFFF5F8F6)
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
                CaptainHomeShell(session) {
                    session.clear()
                    signedIn = false
                }
            } else {
                CaptainLogin { user ->
                    session.save(user)
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
    var devCode by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun request() {
        if (phone.filter(Char::isDigit).length < 8) { error = "أدخل رقم هاتف صحيح"; return }
        loading = true; error = null
        scope.launch {
            runCatching { CaptainApiProvider.api.requestCode(OtpRequest(phone.trim())) }
                .onSuccess { response ->
                    if (response.success) { devCode = response.data?.devCode; sent = true }
                    else error = response.message ?: "تعذر إرسال الرمز"
                }
                .onFailure { error = it.message ?: "تعذر الاتصال بالخادم" }
            loading = false
        }
    }

    fun verify() {
        if (code.filter(Char::isDigit).length < 4) { error = "أدخل رمز التحقق"; return }
        loading = true; error = null
        scope.launch {
            runCatching { CaptainApiProvider.api.verifyCode(VerifyOtpRequest(phone.trim(), code.trim())) }
                .onSuccess { response ->
                    val data = response.data
                    if (response.success && data != null && data.role.equals("driver", true)) onSuccess(data)
                    else error = response.message ?: "هذا الحساب غير مسجل ككابتن"
                }
                .onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            loading = false
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth().padding(22.dp), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(White)) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Green, modifier = Modifier.size(38.dp))
                }
                Text("وصلها كابتن", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("سجل دخولك وابدأ استقبال الرحلات", color = Muted, fontSize = 13.sp)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, leadingIcon = { Icon(Icons.Default.Phone, null) }, singleLine = true)
                if (sent) {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), label = { Text("رمز التحقق") }, singleLine = true)
                    devCode?.takeIf { it.isNotBlank() }?.let { Text("رمز الاختبار: $it", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
                error?.let { Text(it, color = Red, fontSize = 12.sp) }
                Button(onClick = { if (sent) verify() else request() }, Modifier.fillMaxWidth(), enabled = !loading, shape = RoundedCornerShape(18.dp)) {
                    if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(if (sent) "دخول" else "إرسال الرمز", fontWeight = FontWeight.Bold)
                }
                if (sent) TextButton(onClick = { sent = false; code = "" }) { Text("تغيير الرقم") }
            }
        }
    }
}

@Composable
private fun CaptainHomeShell(session: CaptainSession, onLogout: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var driver by remember { mutableStateOf<Driver?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var available by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var online by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }

    fun refresh() {
        scope.launch {
            runCatching { CaptainApiProvider.api.me() }.onSuccess { driver = it.data; online = it.data?.available == true }
            runCatching { CaptainApiProvider.api.trips() }.onSuccess { trips = it.data.orEmpty() }
            if (online) runCatching { CaptainApiProvider.api.availableTrips() }.onSuccess { available = it.data.orEmpty() }
        }
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(online) {
        if (!online) return@LaunchedEffect
        while (true) {
            delay(5000)
            runCatching { CaptainApiProvider.api.availableTrips() }.onSuccess { available = it.data.orEmpty() }
            runCatching { CaptainApiProvider.api.trips() }.onSuccess { list ->
                trips = list.data.orEmpty()
                activeTrip = trips.firstOrNull { it.status in setOf("driver_assigned", "arriving", "in_progress") }
            }
        }
    }

    Scaffold(
        topBar = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(when (tab) { 0 -> "الرئيسية"; 1 -> "الرحلات"; else -> "حسابي" }, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(driver?.name?.takeIf { it.isNotBlank() }?.let { "أهلاً $it" } ?: "أهلاً كابتن", color = Muted, fontSize = 12.sp)
                }
                IconButton(onClick = { tab = 0; refresh() }) { Icon(Icons.Default.Notifications, "تحديث", tint = Green) }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = White, modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("الرئيسية") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.AccessTime, null) }, label = { Text("الرحلات") })
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.AccountCircle, null) }, label = { Text("حسابي") })
            }
        }
    ) { padding ->
        when (tab) {
            0 -> CaptainDashboard(Modifier.padding(padding), online, driver, trips, available, activeTrip, busy, error,
                onToggle = {
                    busy = true; error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.availability(DriverAvailabilityRequest(!online)) }
                            .onSuccess { driver = it.data; online = it.data?.available == true }
                            .onFailure { error = it.message ?: "تعذر تحديث الحالة" }
                        busy = false
                    }
                },
                onAccept = { id ->
                    busy = true; error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.acceptTrip(id) }
                            .onSuccess { response -> activeTrip = response.data; available = available.filterNot { it.id == id }; refresh() }
                            .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                        busy = false
                    }
                },
                onStatus = { id, status ->
                    busy = true
                    scope.launch {
                        runCatching { CaptainApiProvider.api.updateTripStatus(id, TripStatusRequest(status)) }
                            .onSuccess { activeTrip = it.data; refresh() }
                            .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                        busy = false
                    }
                }
            )
            1 -> CaptainTrips(Modifier.padding(padding), trips)
            else -> CaptainProfile(Modifier.padding(padding), driver, onLogout)
        }
    }
}

@Composable
private fun CaptainDashboard(modifier: Modifier, online: Boolean, driver: Driver?, trips: List<Trip>, available: List<Trip>, activeTrip: Trip?, busy: Boolean, error: String?, onToggle: () -> Unit, onAccept: (String) -> Unit, onStatus: (String, String) -> Unit) {
    val completed = trips.count { it.status == "completed" }
    val earnings = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }
    LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(18.dp)) {
        item {
            Card(Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onToggle), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(if (online) Green else White)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(if (online) White.copy(.15f) else Mint), contentAlignment = Alignment.Center) { Icon(Icons.Default.PowerSettingsNew, null, tint = if (online) White else Green, modifier = Modifier.size(27.dp)) }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (online) "أنت متصل الآن" else "أنت غير متصل", color = if (online) White else Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (online) "طلبات الرحلات مفعلة" else "فعّل الحالة حتى تستقبل الطلبات", color = if (online) White.copy(.75f) else Muted, fontSize = 12.sp)
                    }
                    Text(if (online) "متصل" else "تشغيل", color = if (online) White else Green, fontWeight = FontWeight.Bold)
                }
            }
        }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { CaptainStat("رحلات اليوم", completed.toString(), Icons.Default.CheckCircle, Modifier.weight(1f)); CaptainStat("الأرباح", "$earnings ل.س", Icons.Default.Work, Modifier.weight(1f)); CaptainStat("التقييم", String.format("%.1f", driver?.rating ?: 5.0), Icons.Default.Star, Modifier.weight(1f)) } }
        error?.let { item { Text(it, color = Red, fontSize = 12.sp) } }
        if (activeTrip != null) item {
            CaptainActiveTrip(activeTrip, busy, onStatus)
        }
        item {
            Text("الطلبات الجديدة", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        if (online && activeTrip == null && available.isEmpty()) item { Text("ماكو طلبات متاحة حالياً", color = Muted, fontSize = 12.sp) }
        items(available) { trip -> CaptainAvailableTrip(trip, busy, onAccept) }
    }
}

@Composable
private fun CaptainActiveTrip(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Green)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("رحلة نشطة", color = White, fontWeight = FontWeight.Black, fontSize = 17.sp)
            Text("استلام الزبون: ${"%.4f".format(trip.pickup.lat)}, ${"%.4f".format(trip.pickup.lng)}", color = White.copy(.85f), fontSize = 12.sp)
            Text("الوجهة: ${"%.4f".format(trip.destination.lat)}, ${"%.4f".format(trip.destination.lng)}", color = White.copy(.85f), fontSize = 12.sp)
            Text("${trip.estimatedFare} ${trip.currency} • ${trip.distanceKm} كم", color = White, fontWeight = FontWeight.Bold)
            when (trip.status) {
                "driver_assigned" -> Button(onClick = { onStatus(trip.id, "arriving") }, enabled = !busy, Modifier.fillMaxWidth()) { Text("أنا في الطريق") }
                "arriving" -> Button(onClick = { onStatus(trip.id, "in_progress") }, enabled = !busy, Modifier.fillMaxWidth()) { Text("بدأت الرحلة") }
                "in_progress" -> Button(onClick = { onStatus(trip.id, "completed") }, enabled = !busy, Modifier.fillMaxWidth()) { Text("إنهاء الرحلة") }
            }
        }
    }
}

@Composable
private fun CaptainAvailableTrip(trip: Trip, busy: Boolean, onAccept: (String) -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Green)
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("طلب جديد", color = Ink, fontWeight = FontWeight.Bold)
                    Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 11.sp)
                }
                Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black)
            }
            Button(onClick = { onAccept(trip.id) }, enabled = !busy, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("قبول الرحلة") }
        }
    }
}

@Composable
private fun CaptainStat(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Icon(icon, null, tint = Green, modifier = Modifier.size(20.dp)); Text(value, color = Ink, fontWeight = FontWeight.Black, fontSize = 15.sp); Text(title, color = Muted, fontSize = 10.sp) }
    }
}

@Composable
private fun CaptainTrips(modifier: Modifier, trips: List<Trip>) {
    if (trips.isEmpty()) return Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.AccessTime, null, tint = Green, modifier = Modifier.size(46.dp)); Spacer(Modifier.size(10.dp)); Text("لا توجد رحلات بعد", color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text("رحلاتك ستظهر هنا.", color = Muted, fontSize = 12.sp) } }
    LazyColumn(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(18.dp)) { items(trips) { trip -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(White)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("رحلة #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Bold); Text("${trip.status} • ${trip.distanceKm} كم", color = Muted, fontSize = 11.sp); Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black) } } } }
}

@Composable
private fun CaptainProfile(modifier: Modifier, driver: Driver?, onLogout: () -> Unit) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(driver?.phone ?: "رقم الهاتف غير متوفر", color = Muted, fontSize = 12.sp)
                Text("${driver?.vehicle ?: "السيارة غير مسجلة"} • ${driver?.plate ?: ""}", color = Ink, fontSize = 13.sp)
                Text("التقييم ${String.format("%.1f", driver?.rating ?: 0.0)}", color = Green, fontWeight = FontWeight.Bold)
            }
        }
        Button(onClick = onLogout, Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) { Text("تسجيل الخروج") }
    }
}
