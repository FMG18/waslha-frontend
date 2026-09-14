package com.waslha.captain

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF0B805E)
private val Dark = Color(0xFF075B43)
private val Mint = Color(0xFFE8F5F0)
private val Bg = Color(0xFFF3F7F5)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color.White
private val Red = Color(0xFFB42318)
private val SoftRed = Color(0xFFFFE9E7)
private val Amber = Color(0xFFB54708)
private val SoftAmber = Color(0xFFFFF3D9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptainHomeV3Fixed(session: CaptainSession, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { CaptainRepository() }
    val snackbars = remember { SnackbarHostState() }
    val reporter = remember { CaptainLocationReporter(context) }
    var tab by remember { mutableIntStateOf(0) }
    var driver by remember { mutableStateOf<Driver?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var available by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var active by remember { mutableStateOf<Trip?>(null) }
    var location by remember { mutableStateOf<Coordinates?>(null) }
    var incoming by remember { mutableStateOf<Trip?>(null) }
    var countdown by remember { mutableIntStateOf(15) }
    var rejected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var completedTrip by remember { mutableStateOf<Trip?>(null) }
    var chatTrip by remember { mutableStateOf<Trip?>(null) }
    var chatMessages by remember { mutableStateOf<List<TripMessage>>(emptyList()) }
    var chatLast by remember { mutableStateOf(0L) }
    var chatText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) reporter.start({ location = it }) { driver?.available == true }
        else scope.launch { snackbars.showSnackbar("السماح بالموقع مطلوب لتشغيل التتبع المباشر") }
    }

    DisposableEffect(Unit) { onDispose { reporter.stop() } }

    suspend fun refresh(showLoading: Boolean = false) {
        if (showLoading) loading = true
        runCatching { repository.me() }
            .onSuccess { response ->
                if (response.success) driver = response.data else scope.launch { snackbars.showSnackbar(response.message ?: "تعذر تحميل الحساب") }
            }
            .onFailure { scope.launch { snackbars.showSnackbar(networkMessage(it, "تعذر الاتصال بالخادم")) } }
        runCatching { repository.trips() }
            .onSuccess { response ->
                if (response.success) {
                    trips = response.data.orEmpty().sortedByDescending { it.updatedAt }
                    active = trips.firstOrNull { it.status in ACTIVE_STATUSES }
                } else scope.launch { snackbars.showSnackbar(response.message ?: "تعذر تحميل الرحلات") }
            }
            .onFailure { scope.launch { snackbars.showSnackbar(networkMessage(it, "تعذر تحميل الرحلات")) } }
        if (driver?.available == true && active == null) {
            runCatching { repository.availableTrips() }
                .onSuccess { response ->
                    if (response.success) available = response.data.orEmpty()
                    else scope.launch { snackbars.showSnackbar(response.message ?: "تعذر تحميل الطلبات") }
                }
                .onFailure { scope.launch { snackbars.showSnackbar(networkMessage(it, "تعذر تحديث الطلبات")) } }
        } else available = emptyList()
        loading = false
    }

    LaunchedEffect(Unit) {
        refresh(true)
        if (reporter.hasPermission()) reporter.start({ location = it }) { driver?.available == true }
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(driver?.available, active?.id) {
        if (driver?.available != true) return@LaunchedEffect
        while (true) { delay(3500); refresh() }
    }

    val candidate = available.firstOrNull { it.id !in rejected }
    LaunchedEffect(candidate?.id, active?.id) {
        if (candidate == null || active != null) return@LaunchedEffect
        incoming = candidate
        countdown = 15
        val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        repeat(3) { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 350); delay(500) }
        tone.release()
        while (countdown > 0 && incoming?.id == candidate.id) { delay(1000); countdown-- }
        if (countdown == 0 && incoming?.id == candidate.id) { rejected = rejected + candidate.id; incoming = null }
    }

    LaunchedEffect(chatTrip?.id) {
        chatMessages = emptyList()
        chatLast = 0L
        if (chatTrip == null) return@LaunchedEffect
        while (chatTrip != null) {
            runCatching { repository.messages(chatTrip!!.id, chatLast) }.onSuccess { response ->
                val incomingMessages = response.data.orEmpty()
                if (incomingMessages.isNotEmpty()) {
                    chatMessages = (chatMessages + incomingMessages).distinctBy { it.id }.sortedBy { it.createdAt }
                    chatLast = chatMessages.maxOfOrNull { it.createdAt } ?: chatLast
                }
            }
            delay(2200)
        }
    }

    val todayTrips = trips.filter { isToday(it.updatedAt.takeIf { time -> time > 0 } ?: it.createdAt) }
    val todayCompleted = todayTrips.filter { it.status == "completed" }
    val todayEarnings = todayCompleted.sumOf { it.estimatedFare }

    MaterialTheme {
        Scaffold(
            containerColor = Bg,
            snackbarHost = { SnackbarHost(snackbars) },
            bottomBar = {
                NavigationBar(containerColor = White, modifier = Modifier.navigationBarsPadding()) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂", fontSize = 20.sp) }, label = { Text("الرئيسية") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("↺", fontSize = 20.sp) }, label = { Text("رحلاتي") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("○", fontSize = 20.sp) }, label = { Text("حسابي") })
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (tab) {
                    0 -> CaptainDashboardPage(driver, location, active, available.filter { it.id !in rejected }, todayEarnings, todayCompleted.size, loading, busy,
                        onToggle = {
                            val next = driver?.available != true
                            busy = true
                            scope.launch {
                                runCatching { repository.availability(next) }.onSuccess { response ->
                                    if (response.success) driver = response.data else snackbars.showSnackbar(response.message ?: "تعذر تغيير الحالة")
                                }.onFailure { snackbars.showSnackbar(networkMessage(it, "تعذر تغيير الحالة")) }
                                busy = false
                            }
                        },
                        onAccept = { tripId ->
                            busy = true
                            scope.launch {
                                runCatching { repository.acceptTrip(tripId) }.onSuccess { response ->
                                    if (response.success) { active = response.data; incoming = null; refresh() }
                                    else snackbars.showSnackbar(response.message ?: "تعذر قبول الرحلة")
                                }.onFailure { snackbars.showSnackbar(networkMessage(it, "تعذر قبول الرحلة")) }
                                busy = false
                            }
                        },
                        onStatus = { tripId, status ->
                            busy = true
                            scope.launch {
                                runCatching { repository.updateTripStatus(tripId, status) }.onSuccess { response ->
                                    if (response.success) {
                                        val updated = response.data
                                        if (status == "completed" && updated != null) { completedTrip = updated; active = null } else active = updated
                                        refresh()
                                    } else snackbars.showSnackbar(response.message ?: "تعذر تحديث حالة الرحلة")
                                }.onFailure { snackbars.showSnackbar(networkMessage(it, "تعذر تحديث الرحلة")) }
                                busy = false
                            }
                        },
                        onCall = { phone -> dial(context, phone) }, onChat = { chatTrip = it }, onNavigate = { openNavigation(context, it) }, onAccount = { tab = 2 })
                    1 -> CaptainTripsPage(trips)
                    else -> CaptainAccountPage(driver, onLogout) { context.startActivity(Intent(context, CaptainDocumentsActivity::class.java)) }
                }
                if (loading) Surface(Modifier.align(Alignment.Center), color = White.copy(.94f), shape = RoundedCornerShape(18.dp), shadowElevation = 8.dp) { CircularProgressIndicator(Modifier.padding(22.dp).size(28.dp), color = Green, strokeWidth = 3.dp) }
            }
        }
    }

    incoming?.let { trip ->
        ModalBottomSheet(onDismissRequest = { incoming = null }) { IncomingSheet(trip, countdown, busy, onReject = { rejected = rejected + trip.id; incoming = null }, onAccept = {
            busy = true
            scope.launch {
                runCatching { repository.acceptTrip(trip.id) }.onSuccess { response ->
                    if (response.success) { active = response.data; incoming = null; refresh() } else snackbars.showSnackbar(response.message ?: "تعذر قبول الطلب")
                }.onFailure { snackbars.showSnackbar(networkMessage(it, "تعذر قبول الطلب")) }
                busy = false
            }
        }) }
    }

    completedTrip?.let { trip -> ReceiptDialog(trip, busy, onDismiss = { completedTrip = null }, onRate = { score ->
        busy = true
        scope.launch {
            runCatching { repository.rateTrip(DriverRatingRequest(tripId = trip.id, customerId = trip.customerId, driverId = driver?.id.orEmpty(), score = score)) }
                .onSuccess { response -> if (response.success) completedTrip = null else snackbars.showSnackbar(response.message ?: "تعذر حفظ التقييم") }
                .onFailure { snackbars.showSnackbar(networkMessage(it, "تعذر حفظ التقييم")) }
            busy = false
        }
    }) }

    chatTrip?.let { trip -> ChatSheet(trip, chatMessages, chatText, busy, { chatText = it }, { chatTrip = null; chatText = "" }) {
        val text = chatText.trim()
        if (text.isNotEmpty()) {
            busy = true
            scope.launch {
                runCatching { repository.sendMessage(trip.id, text) }.onSuccess { response ->
                    if (response.success && response.data != null) { chatMessages = (chatMessages + response.data).distinctBy { it.id }; chatLast = maxOf(chatLast, response.data.createdAt); chatText = "" }
                    else snackbars.showSnackbar(response.message ?: "تعذر إرسال الرسالة")
                }.onFailure { snackbars.showSnackbar(networkMessage(it, "تعذر إرسال الرسالة")) }
                busy = false
            }
        }
    } }
}

@Composable
private fun CaptainDashboardPage(driver: Driver?, location: Coordinates?, active: Trip?, available: List<Trip>, todayEarnings: Int, todayCompleted: Int, loading: Boolean, busy: Boolean,
    onToggle: () -> Unit, onAccept: (String) -> Unit, onStatus: (String, String) -> Unit, onCall: (String) -> Unit, onChat: (Trip) -> Unit, onNavigate: (Coordinates) -> Unit, onAccount: () -> Unit) {
    val online = driver?.available == true
    Box(Modifier.fillMaxSize()) {
        CaptainLiveMap(Modifier.fillMaxSize(), location ?: driver?.let { Coordinates(it.lat, it.lng) }, active?.pickup, active?.destination)
        Surface(Modifier.padding(13.dp).fillMaxWidth().align(Alignment.TopCenter), color = White.copy(.96f), shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                        Text(if (online) "متصل الآن • الموقع المباشر فعال" else "غير متصل • اضغط لتبدأ استقبال الرحلات", color = if (online) Green else Muted, fontSize = 9.sp)
                    }
                    Surface(onClick = onToggle, enabled = !busy, color = if (online) Dark else Color(0xFFEEF2EF), shape = RoundedCornerShape(999.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(if (online) White else Muted, CircleShape)); Spacer(Modifier.width(7.dp)); Text(if (online) "ONLINE" else "OFFLINE", color = if (online) White else Ink, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { DashboardStat("أرباح اليوم", "$todayEarnings ل.س"); DashboardStat("رحلات مكتملة", todayCompleted.toString()); DashboardStat("التقييم", String.format(Locale.US, "%.1f", driver?.rating ?: 0.0)) }
            }
        }
        Surface(Modifier.padding(top = 177.dp, end = 13.dp).align(Alignment.TopEnd), color = White.copy(.96f), shape = CircleShape, shadowElevation = 6.dp) { IconButton(onClick = onAccount) { Text("👤", fontSize = 18.sp) } }
        when {
            active != null -> ActiveTripPanel(active, busy, onStatus, onCall, onChat, onNavigate)
            !online -> DashboardActionCard(Modifier.align(Alignment.BottomCenter), "ابدأ استقبال الرحلات", "فعّل ONLINE حتى يبدأ الموقع والطلبات بالعمل تلقائياً.", "تشغيل ONLINE", busy, onToggle)
            available.isEmpty() -> DashboardActionCard(Modifier.align(Alignment.BottomCenter), "أنت متصل", "لا توجد رحلة مناسبة حالياً. سنحدّث الطلبات تلقائياً.", null, false, {})
        }
    }
}

@Composable
private fun RowScope.DashboardStat(label: String, value: String) {
    Surface(Modifier.weight(1f), color = Mint, shape = RoundedCornerShape(15.dp)) { Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Dark, fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center); Text(label, color = Muted, fontSize = 7.sp, textAlign = TextAlign.Center) } }
}

@Composable
private fun DashboardActionCard(modifier: Modifier, title: String, subtitle: String, button: String?, busy: Boolean, action: () -> Unit) {
    Surface(modifier.padding(13.dp).fillMaxWidth(), color = White.copy(.97f), shape = RoundedCornerShape(26.dp), shadowElevation = 11.dp) { Column(Modifier.padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center); if (button != null) Button(onClick = action, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(15.dp)) { Text(button, color = White, fontWeight = FontWeight.Black) } } }
}

@Composable
private fun ActiveTripPanel(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit, onCall: (String) -> Unit, onChat: (Trip) -> Unit, onNavigate: (Coordinates) -> Unit) {
    val next = when (trip.status) { "driver_assigned" -> "arriving"; "arriving" -> "in_progress"; "in_progress" -> "completed"; else -> "" }
    val title = when (trip.status) { "driver_assigned" -> "في الطريق إلى الزبون"; "arriving" -> "وصلت للزبون"; "in_progress" -> "الرحلة جارية"; else -> "الرحلة مكتملة" }
    val button = when (trip.status) { "driver_assigned" -> "وصلت للزبون"; "arriving" -> "بدء الرحلة"; "in_progress" -> "إنهاء الرحلة ودفع المبلغ"; else -> "مكتملة" }
    Surface(Modifier.padding(13.dp).fillMaxWidth().align(Alignment.BottomCenter), color = White.copy(.98f), shape = RoundedCornerShape(26.dp), shadowElevation = 12.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black); Text("${trip.customerName ?: "الزبون"} • ${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 9.sp) }; Surface(color = Mint, shape = RoundedCornerShape(999.dp)) { Text(statusLabel(trip.status), Modifier.padding(horizontal = 9.dp, vertical = 7.dp), color = Green, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) { OutlinedButton(onClick = { trip.customerPhone?.let(onCall) }, enabled = trip.customerPhone != null, modifier = Modifier.weight(1f)) { Text("اتصال", fontSize = 8.sp) }; OutlinedButton(onClick = { onChat(trip) }, modifier = Modifier.weight(1f)) { Text("شات", fontSize = 8.sp) }; OutlinedButton(onClick = { onNavigate(if (trip.status == "in_progress") trip.destination else trip.pickup) }, modifier = Modifier.weight(1f)) { Text("ملاحة", fontSize = 8.sp) } }
            Button(onClick = { if (next.isNotBlank()) onStatus(trip.id, next) }, enabled = !busy && next.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(15.dp)) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = White, strokeWidth = 2.dp) else Text(button, color = White, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun IncomingSheet(trip: Trip, seconds: Int, busy: Boolean, onReject: () -> Unit, onAccept: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(18.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("رحلة جديدة", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black); Text("طلب مباشر • لديك 15 ثانية للتفاعل", color = Muted, fontSize = 10.sp) }; Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = seconds / 15f, modifier = Modifier.size(58.dp), color = if (seconds <= 5) Red else Green, strokeWidth = 5.dp); Text(seconds.toString(), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black) } }
        Surface(color = Bg, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { MiniRouteMap(trip); Text("الاستلام", color = Muted, fontSize = 8.sp); Text(coordText(trip.pickup), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("الوجهة", color = Muted, fontSize = 8.sp); Text(coordText(trip.destination), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { InfoPill("${trip.distanceKm} كم"); InfoPill("${trip.durationMin} دقيقة"); InfoPill("${trip.estimatedFare} ل.س") } } }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onReject, enabled = !busy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("رفض", color = Red, fontWeight = FontWeight.Bold) }; Button(onClick = onAccept, enabled = !busy, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(15.dp)) { Text("قبول الطلب", color = White, fontWeight = FontWeight.Black) } }
    }
}

@Composable
private fun MiniRouteMap(trip: Trip) { Card(Modifier.fillMaxWidth().height(155.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color(0xFFE5ECE8))) { CaptainLiveMap(Modifier.fillMaxSize(), null, trip.pickup, trip.destination) } }

@Composable
private fun RowScope.InfoPill(text: String) { Surface(Modifier.weight(1f), color = Mint, shape = RoundedCornerShape(999.dp)) { Text(text, Modifier.padding(horizontal = 7.dp, vertical = 6.dp), color = Dark, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) } }

@Composable
private fun CaptainTripsPage(trips: List<Trip>) {
    var selected by remember { mutableIntStateOf(0) }
    val filtered = trips.filter { if (selected == 0) it.status == "completed" else it.status == "cancelled" }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) { Text("رحلاتي", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black); Text("سجل الرحلات المنجزة والملغاة", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp, bottom = 13.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected = selected == 0, onClick = { selected = 0 }, label = { Text("الرحلات المكتملة") }, modifier = Modifier.weight(1f)); FilterChip(selected = selected == 1, onClick = { selected = 1 }, label = { Text("الرحلات الملغاة") }, modifier = Modifier.weight(1f)) }; Spacer(Modifier.height(12.dp)); if (filtered.isEmpty()) EmptyTripsState() else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp)) { items(filtered, key = { it.id }) { TripHistoryCard(it) } } }
}

@Composable
private fun EmptyTripsState() { Box(Modifier.fillMaxSize().padding(bottom = 50.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) { Surface(Modifier.size(100.dp), color = Mint, shape = CircleShape) { Box(contentAlignment = Alignment.Center) { Text("🚕", fontSize = 45.sp) } }; Text("لا توجد رحلات مسجلة بعد", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black); Text("ستظهر الرحلات هنا بعد إنهائها", color = Muted, fontSize = 10.sp) } } }

@Composable
private fun TripHistoryCard(trip: Trip) { Surface(color = White, shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("رحلة #${trip.id.takeLast(7)}", color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp); Text(formatDate(trip.updatedAt.takeIf { it > 0 } ?: trip.createdAt), color = Muted, fontSize = 8.sp) }; val cancelled = trip.status == "cancelled"; Surface(color = if (cancelled) SoftRed else Mint, shape = RoundedCornerShape(999.dp)) { Text(statusLabel(trip.status), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = if (cancelled) Red else Green, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }; Text("الانطلاق: ${coordText(trip.pickup)}", color = Ink, fontSize = 9.sp); Text("الوصول: ${coordText(trip.destination)}", color = Ink, fontSize = 9.sp); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 8.sp); Text("${trip.estimatedFare} ل.س", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Black) } } } }

@Composable
private fun CaptainAccountPage(driver: Driver?, onLogout: () -> Unit, onDocuments: () -> Unit) {
    val documentItems = listOf("الهوية" to (driver?.documents?.identity?.status ?: "pending"), "الرخصة" to (driver?.documents?.license?.status ?: "pending"), "أوراق السيارة" to (driver?.documents?.vehicle?.status ?: "pending"))
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("حسابي", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black); Text("بياناتك، مركبتك، ووثائقك في مكان واحد", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp, bottom = 7.dp)) }; item { AccountCard("المعلومات الشخصية") { AccountRow("الاسم", driver?.name?.ifBlank { "غير محدد" } ?: "غير محدد"); AccountRow("رقم الهاتف", formatSyrianPhone(driver?.phone)); AccountRow("التقييم", String.format(Locale.US, "%.1f / 5", driver?.rating ?: 0.0)); AccountRow("الرصيد", "${driver?.walletBalance ?: 0} ل.س") } }; item { AccountCard("المركبة") { AccountRow("النوع", driver?.type?.ifBlank { "اقتصادي" } ?: "اقتصادي"); AccountRow("الموديل", driver?.model?.ifBlank { (driver?.vehicle ?: "").ifBlank { "غير محدد" } } ?: "غير محدد"); AccountRow("اللون", driver?.color?.ifBlank { "غير محدد" } ?: "غير محدد"); AccountRow("رقم اللوحة", driver?.plate?.ifBlank { "غير محدد" } ?: "غير محدد") } }; item { AccountCard("المستندات") { documentItems.forEach { item -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text(item.first, Modifier.weight(1f), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold); val approved = item.second.equals("approved", true); Surface(color = if (approved) Mint else SoftAmber, shape = RoundedCornerShape(999.dp)) { Text(if (approved) "معتمد" else "قيد المراجعة", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (approved) Green else Amber, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } }; Spacer(Modifier.height(6.dp)); OutlinedButton(onClick = onDocuments, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Text("إدارة الوثائق", fontWeight = FontWeight.Bold) } } }; item { Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red), shape = RoundedCornerShape(15.dp)) { Text("تسجيل الخروج", color = White, fontWeight = FontWeight.Black) } } }
}

@Composable
private fun AccountCard(title: String, content: @Composable Column.() -> Unit) { Surface(color = White, shape = RoundedCornerShape(21.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = { Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(4.dp)); content() }) } }

@Composable
private fun AccountRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = Muted, fontSize = 9.sp); Text(value, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End) } }

@Composable
private fun ReceiptDialog(trip: Trip, busy: Boolean, onDismiss: () -> Unit, onRate: (Int) -> Unit) {
    var score by remember { mutableIntStateOf(5) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("ملخص الفاتورة", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("المبلغ المطلوب", color = Muted, fontSize = 10.sp); Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontSize = 28.sp, fontWeight = FontWeight.Black); Surface(color = if (trip.paymentMethod == "cash") SoftAmber else Mint, shape = RoundedCornerShape(14.dp)) { Text(if (trip.paymentMethod == "cash") "تحصيل نقدي من الزبون" else "الدفع الإلكتروني حسب طريقة الرحلة", Modifier.padding(12.dp), color = if (trip.paymentMethod == "cash") Amber else Green, fontSize = 9.sp, fontWeight = FontWeight.Bold) }; Text("قيّم الزبون", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { (1..5).forEach { star -> Text("★", Modifier.clickable { score = star }, color = if (star <= score) Amber else Color(0xFFD0D8D3), fontSize = 26.sp) } } } }, dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("لاحقاً") } }, confirmButton = { Button(onClick = { onRate(score) }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("حفظ وإنهاء", color = White) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSheet(trip: Trip, messages: List<TripMessage>, text: String, busy: Boolean, onTextChange: (String) -> Unit, onDismiss: () -> Unit, onSend: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.fillMaxWidth().height(530.dp).padding(14.dp).navigationBarsPadding()) { Text("الدردشة مع ${trip.customerName ?: "الزبون"}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black); Text("الرحلة #${trip.id.takeLast(6)}", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)); LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(messages, key = { it.id }) { message -> Surface(Modifier.fillMaxWidth(), color = if (message.senderRole == "driver") Mint else White, shape = RoundedCornerShape(14.dp), shadowElevation = 1.dp) { Column(Modifier.padding(10.dp)) { Text(if (message.senderRole == "driver") "أنت" else "الزبون", color = Muted, fontSize = 7.sp); Text(message.text, color = Ink, fontSize = 10.sp) } } } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = text, onValueChange = onTextChange, Modifier.weight(1f), singleLine = true, placeholder = { Text("اكتب رسالة") }); Button(onClick = onSend, enabled = !busy && text.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(14.dp)) { Text("إرسال", color = White) } } } }
}

private val ACTIVE_STATUSES = setOf("driver_assigned", "arriving", "in_progress")
private fun networkMessage(error: Throwable, fallback: String): String = error.message?.takeIf { it.isNotBlank() } ?: fallback
private fun isToday(timestamp: Long): Boolean { if (timestamp <= 0L) return false; val current = java.util.Calendar.getInstance(); val target = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }; return current.get(java.util.Calendar.ERA) == target.get(java.util.Calendar.ERA) && current.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) && current.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR) }
private fun formatDate(timestamp: Long): String = if (timestamp <= 0L) "التاريخ غير متوفر" else SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault()).format(Date(timestamp))
private fun formatSyrianPhone(phone: String?): String { val digits = phone.orEmpty().filter(Char::isDigit).removePrefix("963"); if (digits.isEmpty()) return "+963"; val normalized = digits.take(9); return "+963 ${normalized.take(2)} ${normalized.drop(2).take(3)} ${normalized.drop(5).take(4)}".trim() }
private fun coordText(point: Coordinates): String = "${String.format(Locale.US, "%.5f", point.lat)} ، ${String.format(Locale.US, "%.5f", point.lng)}"
private fun statusLabel(status: String): String = when (status) { "driver_assigned" -> "تم القبول"; "arriving" -> "في الطريق"; "in_progress" -> "بالرحلة"; "completed" -> "مكتملة"; "cancelled" -> "ملغاة"; else -> status }
private fun dial(context: Context, phone: String) { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } }
private fun openNavigation(context: Context, point: Coordinates) { val google = Uri.parse("google.navigation:q=${point.lat},${point.lng}"); val geo = Uri.parse("geo:${point.lat},${point.lng}?q=${point.lat},${point.lng}"); runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, google).apply { setPackage("com.google.android.apps.maps") }) }.onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) } } }
