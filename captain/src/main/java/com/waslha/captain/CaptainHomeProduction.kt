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
private val ActiveStatuses = setOf("driver_assigned", "arriving", "in_progress")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptainHomeProduction(session: CaptainSession, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { CaptainRepository() }
    val snackbar = remember { SnackbarHostState() }
    val reporter = remember { CaptainLocationReporter(context) }
    var tab by remember { mutableIntStateOf(0) }
    var driver by remember { mutableStateOf<Driver?>(null) }
    var location by remember { mutableStateOf<Coordinates?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var availableTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var incoming by remember { mutableStateOf<Trip?>(null) }
    var active by remember { mutableStateOf<Trip?>(null) }
    var completed by remember { mutableStateOf<Trip?>(null) }
    var chatTrip by remember { mutableStateOf<Trip?>(null) }
    var messages by remember { mutableStateOf<List<TripMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var lastMessageAt by remember { mutableStateOf(0L) }
    var seconds by remember { mutableIntStateOf(15) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var rejectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun reportError(error: Throwable, fallback: String) { scope.launch { snackbar.showSnackbar(error.message?.takeIf { it.isNotBlank() } ?: fallback) } }

    suspend fun refresh(showLoading: Boolean = false) {
        if (showLoading) loading = true
        runCatching { repository.me() }.onSuccess { response -> if (response.success) driver = response.data else response.message?.let { scope.launch { snackbar.showSnackbar(it) } } }.onFailure { reportError(it, "تعذر تحميل بيانات الكابتن") }
        runCatching { repository.trips() }.onSuccess { response -> if (response.success) { trips = response.data.orEmpty().sortedByDescending { it.updatedAt }; active = trips.firstOrNull { it.status in ActiveStatuses } } else response.message?.let { scope.launch { snackbar.showSnackbar(it) } } }.onFailure { reportError(it, "تعذر تحميل الرحلات") }
        if (driver?.available == true && active == null) runCatching { repository.availableTrips() }.onSuccess { response -> if (response.success) availableTrips = response.data.orEmpty() else response.message?.let { scope.launch { snackbar.showSnackbar(it) } } }.onFailure { reportError(it, "تعذر تحديث الطلبات") } else availableTrips = emptyList()
        loading = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) reporter.start({ location = it }) { driver?.available == true } else scope.launch { snackbar.showSnackbar("السماح بالموقع مطلوب لتشغيل التتبع المباشر") }
    }

    DisposableEffect(Unit) { onDispose { reporter.stop() } }

    LaunchedEffect(Unit) { refresh(true); if (reporter.hasPermission()) reporter.start({ location = it }) { driver?.available == true } else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
    LaunchedEffect(driver?.available, active?.id) { if (driver?.available != true) return@LaunchedEffect; while (true) { delay(3500); refresh() } }

    val candidate = availableTrips.firstOrNull { it.id !in rejectedIds }
    LaunchedEffect(candidate?.id, active?.id) {
        if (candidate == null || active != null) return@LaunchedEffect
        incoming = candidate; seconds = 15
        runCatching { val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100); repeat(3) { tone.startTone(ToneGenerator.TONE_PROP_BEEP, 300); delay(450) }; tone.release() }
        while (seconds > 0 && incoming?.id == candidate.id) { delay(1000); seconds-- }
        if (seconds == 0 && incoming?.id == candidate.id) { rejectedIds = rejectedIds + candidate.id; incoming = null }
    }

    LaunchedEffect(chatTrip?.id) {
        messages = emptyList(); lastMessageAt = 0L
        if (chatTrip == null) return@LaunchedEffect
        while (chatTrip != null) { runCatching { repository.messages(chatTrip!!.id, lastMessageAt) }.onSuccess { response -> val batch = response.data.orEmpty(); if (batch.isNotEmpty()) { messages = (messages + batch).distinctBy { it.id }.sortedBy { it.createdAt }; lastMessageAt = messages.maxOfOrNull { it.createdAt } ?: lastMessageAt } }; delay(2200) }
    }

    val today = trips.filter { todayTimestamp(it.updatedAt, it.createdAt) }
    val todayCompleted = today.filter { it.status == "completed" }
    val todayEarnings = todayCompleted.sumOf { it.estimatedFare }

    Scaffold(containerColor = Bg, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = { NavigationBar(containerColor = White, modifier = Modifier.navigationBarsPadding()) { NavigationBarItem(tab == 0, { tab = 0 }, icon = { Text("⌂") }, label = { Text("الرئيسية") }); NavigationBarItem(tab == 1, { tab = 1 }, icon = { Text("↺") }, label = { Text("رحلاتي") }); NavigationBarItem(tab == 2, { tab = 2 }, icon = { Text("○") }, label = { Text("حسابي") }) } }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> CaptainDashboard(driver, location, active, availableTrips.filter { it.id !in rejectedIds }, todayEarnings, todayCompleted.size, busy, onToggle = { val next = driver?.available != true; busy = true; scope.launch { runCatching { repository.availability(next) }.onSuccess { response -> if (response.success) driver = response.data else response.message?.let { snackbar.showSnackbar(it) } }.onFailure { reportError(it, "تعذر تغيير حالة الاتصال") }; busy = false } }, onAccept = { id -> busy = true; scope.launch { runCatching { repository.acceptTrip(id) }.onSuccess { response -> if (response.success) { active = response.data; incoming = null; refresh() } else response.message?.let { snackbar.showSnackbar(it) } }.onFailure { reportError(it, "تعذر قبول الطلب") }; busy = false } }, onStatus = { id, status -> busy = true; scope.launch { runCatching { repository.updateTripStatus(id, status) }.onSuccess { response -> if (response.success) { val updated = response.data; if (status == "completed" && updated != null) { completed = updated; active = null } else active = updated; refresh() } else response.message?.let { snackbar.showSnackbar(it) } }.onFailure { reportError(it, "تعذر تحديث حالة الرحلة") }; busy = false } }, onCall = { dial(context, it) }, onChat = { chatTrip = it }, onNavigate = { navigate(context, it) }, onAccount = { tab = 2 })
                1 -> TripHistory(trips)
                else -> AccountScreen(driver, onLogout) { context.startActivity(Intent(context, CaptainDocumentsActivity::class.java)) }
            }
            if (loading) Surface(Modifier.align(Alignment.Center), color = White.copy(alpha = .95f), shape = RoundedCornerShape(18.dp), shadowElevation = 7.dp) { CircularProgressIndicator(Modifier.padding(24.dp).size(28.dp), color = Green, strokeWidth = 3.dp) }
        }
    }

    incoming?.let { trip -> ModalBottomSheet(onDismissRequest = { incoming = null }) { IncomingOrder(trip, seconds, busy, onReject = { rejectedIds = rejectedIds + trip.id; incoming = null }, onAccept = { busy = true; scope.launch { runCatching { repository.acceptTrip(trip.id) }.onSuccess { response -> if (response.success) { active = response.data; incoming = null; refresh() } else response.message?.let { snackbar.showSnackbar(it) } }.onFailure { reportError(it, "تعذر قبول الطلب") }; busy = false } }) } }
    completed?.let { trip -> ReceiptDialog(trip, busy, onLater = { completed = null }) { score -> busy = true; scope.launch { runCatching { repository.rateTrip(DriverRatingRequest(trip.id, trip.customerId, driver?.id.orEmpty(), score)) }.onSuccess { response -> if (response.success) completed = null else response.message?.let { snackbar.showSnackbar(it) } }.onFailure { reportError(it, "تعذر حفظ التقييم") }; busy = false } } }
    chatTrip?.let { trip -> ChatDialog(trip, messages, messageText, busy, { messageText = it }, { chatTrip = null; messageText = "" }) { val text = messageText.trim(); if (text.isNotEmpty()) { busy = true; scope.launch { runCatching { repository.sendMessage(trip.id, text) }.onSuccess { response -> if (response.success && response.data != null) { messages = (messages + response.data).distinctBy { it.id }; lastMessageAt = maxOf(lastMessageAt, response.data.createdAt); messageText = "" } else response.message?.let { snackbar.showSnackbar(it) } }.onFailure { reportError(it, "تعذر إرسال الرسالة") }; busy = false } } } }
}

@Composable
private fun CaptainDashboard(driver: Driver?, location: Coordinates?, active: Trip?, available: List<Trip>, earnings: Int, completedCount: Int, busy: Boolean, onToggle: () -> Unit, onAccept: (String) -> Unit, onStatus: (String, String) -> Unit, onCall: (String) -> Unit, onChat: (Trip) -> Unit, onNavigate: (Coordinates) -> Unit, onAccount: () -> Unit) {
    val online = driver?.available == true
    Box(Modifier.fillMaxSize()) {
        CaptainLiveMap(Modifier.fillMaxSize(), location ?: driver?.let { Coordinates(it.lat, it.lng) }, active?.pickup, active?.destination)
        Surface(Modifier.padding(13.dp).fillMaxWidth().align(Alignment.TopCenter), color = White.copy(alpha = .97f), shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black); Text(if (online) "متصل الآن • GPS مباشر" else "غير متصل • لن تستقبل طلبات", color = if (online) Green else Muted, fontSize = 9.sp) }; Surface(onClick = onToggle, enabled = !busy, color = if (online) Dark else Color(0xFFEEF2EF), shape = RoundedCornerShape(999.dp)) { Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(8.dp).background(if (online) White else Muted, CircleShape)); Spacer(Modifier.width(7.dp)); Text(if (online) "ONLINE" else "OFFLINE", color = if (online) White else Ink, fontSize = 9.sp, fontWeight = FontWeight.Black) } } }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("أرباح اليوم", "$earnings ل.س"); StatCard("رحلات مكتملة", completedCount.toString()); StatCard("التقييم", String.format(Locale.US, "%.1f", driver?.rating ?: 0.0)) }
            }
        }
        Surface(Modifier.padding(top = 175.dp, end = 13.dp).align(Alignment.TopEnd), color = White.copy(alpha = .97f), shape = CircleShape, shadowElevation = 6.dp) { IconButton(onClick = onAccount) { Text("👤", fontSize = 18.sp) } }
        when { active != null -> ActiveTrip(active, busy, onStatus, onCall, onChat, onNavigate); !online -> ActionCard(Modifier.align(Alignment.BottomCenter), "ابدأ استقبال الرحلات", "فعّل ONLINE حتى يبدأ استقبال الطلبات وتتبع الموقع.", "تشغيل ONLINE", busy, onToggle); available.isEmpty() -> ActionCard(Modifier.align(Alignment.BottomCenter), "أنت متصل", "لا توجد طلبات حالياً. يتم التحديث تلقائياً.", null, false) {} }
    }
}

@Composable private fun RowScope.StatCard(label: String, value: String) { Surface(Modifier.weight(1f), color = Mint, shape = RoundedCornerShape(15.dp)) { Column(Modifier.padding(horizontal = 7.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = Dark, fontSize = 12.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center); Text(label, color = Muted, fontSize = 7.sp, textAlign = TextAlign.Center) } } }

@Composable private fun ActionCard(modifier: Modifier, title: String, subtitle: String, button: String?, busy: Boolean, action: () -> Unit) { Surface(modifier.padding(13.dp).fillMaxWidth(), color = White.copy(alpha = .97f), shape = RoundedCornerShape(25.dp), shadowElevation = 10.dp) { Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(title, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black); Text(subtitle, color = Muted, fontSize = 9.sp, textAlign = TextAlign.Center); if (button != null) Button(onClick = action, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(15.dp)) { Text(button, color = White, fontWeight = FontWeight.Black) } } } }

@Composable private fun ActiveTrip(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit, onCall: (String) -> Unit, onChat: (Trip) -> Unit, onNavigate: (Coordinates) -> Unit) { val next = when (trip.status) { "driver_assigned" -> "arriving"; "arriving" -> "in_progress"; "in_progress" -> "completed"; else -> "" }; val title = when (trip.status) { "driver_assigned" -> "في الطريق إلى الزبون"; "arriving" -> "وصلت للزبون"; "in_progress" -> "الرحلة جارية"; else -> "الرحلة مكتملة" }; val button = when (trip.status) { "driver_assigned" -> "وصلت للزبون"; "arriving" -> "بدء الرحلة"; "in_progress" -> "إنهاء الرحلة ودفع المبلغ"; else -> "مكتملة" }; Surface(Modifier.fillMaxWidth().padding(13.dp), color = White.copy(alpha = .98f), shape = RoundedCornerShape(25.dp), shadowElevation = 11.dp) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black); Text("${trip.customerName ?: "الزبون"} • ${trip.distanceKm} كم • ${trip.durationMin} دقيقة • ${trip.estimatedFare} ل.س", color = Muted, fontSize = 9.sp); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) { OutlinedButton(onClick = { trip.customerPhone?.let(onCall) }, enabled = trip.customerPhone != null, modifier = Modifier.weight(1f)) { Text("اتصال", fontSize = 8.sp) }; OutlinedButton(onClick = { onChat(trip) }, modifier = Modifier.weight(1f)) { Text("شات", fontSize = 8.sp) }; OutlinedButton(onClick = { onNavigate(if (trip.status == "in_progress") trip.destination else trip.pickup) }, modifier = Modifier.weight(1f)) { Text("ملاحة", fontSize = 8.sp) } }; Button(onClick = { if (next.isNotBlank()) onStatus(trip.id, next) }, enabled = !busy && next.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(15.dp)) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = White, strokeWidth = 2.dp) else Text(button, color = White, fontWeight = FontWeight.Black) } } } }

@Composable private fun IncomingOrder(trip: Trip, seconds: Int, busy: Boolean, onReject: () -> Unit, onAccept: () -> Unit) { Column(Modifier.fillMaxWidth().padding(18.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(11.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("رحلة جديدة", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black); Text("طلب مباشر • 15 ثانية للتفاعل", color = Muted, fontSize = 10.sp) }; Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(progress = { seconds / 15f }, modifier = Modifier.fillMaxSize(), color = if (seconds <= 5) Red else Green, strokeWidth = 5.dp); Text(seconds.toString(), color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black) } }; Card(Modifier.fillMaxWidth().height(155.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color(0xFFE5ECE8))) { CaptainLiveMap(Modifier.fillMaxSize(), null, trip.pickup, trip.destination) }; Surface(color = Bg, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("الاستلام", color = Muted, fontSize = 8.sp); Text(coordText(trip.pickup), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold); Text("الوجهة", color = Muted, fontSize = 8.sp); Text(coordText(trip.destination), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { InfoPill("${trip.distanceKm} كم"); InfoPill("${trip.durationMin} دقيقة"); InfoPill("${trip.estimatedFare} ل.س") } } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedButton(onClick = onReject, enabled = !busy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Text("رفض", color = Red, fontWeight = FontWeight.Bold) }; Button(onClick = onAccept, enabled = !busy, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(15.dp)) { Text("قبول الطلب", color = White, fontWeight = FontWeight.Black) } } } }

@Composable private fun MiniRouteMap(trip: Trip) { Card(Modifier.fillMaxWidth().height(155.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color(0xFFE5ECE8))) { CaptainLiveMap(Modifier.fillMaxSize(), null, trip.pickup, trip.destination) } }

@Composable private fun RowScope.InfoPill(text: String) { Surface(Modifier.weight(1f), color = Mint, shape = RoundedCornerShape(999.dp)) { Text(text, Modifier.padding(horizontal = 7.dp, vertical = 6.dp), color = Dark, fontSize = 8.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) } }

@Composable private fun CaptainTripsPage(trips: List<Trip>) { Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) { Text("رحلاتي", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black); Text("سجل الرحلات المنجزة", color = Muted, fontSize = 9.sp); LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp)) { items(trips, key = { it.id }) { trip -> HistoryCard(trip) } } } }

@Composable private fun TripHistory(trips: List<Trip>) { var selected by remember { mutableIntStateOf(0) }; val filtered = trips.filter { if (selected == 0) it.status == "completed" else it.status == "cancelled" }; Column(Modifier.fillMaxSize().padding(14.dp)) { Text("رحلاتي", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black); Text("سجل الرحلات المكتملة والملغاة", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(selected == 0, { selected = 0 }, label = { Text("الرحلات المكتملة") }, modifier = Modifier.weight(1f)); FilterChip(selected == 1, { selected = 1 }, label = { Text("الرحلات الملغاة") }, modifier = Modifier.weight(1f)) }; Spacer(Modifier.height(12.dp)); if (filtered.isEmpty()) EmptyTripsState() else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp)) { items(filtered, key = { it.id }) { TripHistoryCard(it) } } } }

@Composable private fun EmptyTripsState() { Box(Modifier.fillMaxSize().padding(bottom = 50.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) { Surface(Modifier.size(100.dp), color = Mint, shape = CircleShape) { Box(contentAlignment = Alignment.Center) { Text("🚕", fontSize = 45.sp) } }; Text("لا توجد رحلات مسجلة بعد", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black); Text("ستظهر الرحلات هنا بعد إنهائها", color = Muted, fontSize = 10.sp) } } }

@Composable private fun TripHistoryCard(trip: Trip) { Surface(color = White, shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("رحلة #${trip.id.takeLast(7)}", color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp); Text(formatDate(trip.updatedAt.takeIf { it > 0 } ?: trip.createdAt), color = Muted, fontSize = 8.sp) }; val cancelled = trip.status == "cancelled"; Surface(color = if (cancelled) SoftRed else Mint, shape = RoundedCornerShape(999.dp)) { Text(statusLabel(trip.status), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = if (cancelled) Red else Green, fontSize = 8.sp, fontWeight = FontWeight.Bold) } }; Text("الانطلاق: ${coordText(trip.pickup)}", color = Ink, fontSize = 9.sp); Text("الوصول: ${coordText(trip.destination)}", color = Ink, fontSize = 9.sp); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 8.sp); Text("${trip.estimatedFare} ل.س", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Black) } } } }

@Composable private fun AccountScreen(driver: Driver?, onLogout: () -> Unit, onDocuments: () -> Unit) { LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("حسابي", color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Black); Text("بياناتك، مركبتك، ووثائقك", color = Muted, fontSize = 9.sp) }; item { AccountCard("المعلومات الشخصية") { AccountRow("الاسم", driver?.name?.ifBlank { "غير محدد" } ?: "غير محدد"); AccountRow("رقم الهاتف", formatSyrianPhone(driver?.phone)); AccountRow("التقييم", String.format(Locale.US, "%.1f / 5", driver?.rating ?: 0.0)); AccountRow("الرصيد", "${driver?.walletBalance ?: 0} ل.س") } }; item { AccountCard("المركبة") { AccountRow("النوع", driver?.type?.ifBlank { "اقتصادي" } ?: "اقتصادي"); AccountRow("الموديل", driver?.model?.ifBlank { (driver?.vehicle ?: "").ifBlank { "غير محدد" } } ?: "غير محدد"); AccountRow("اللون", driver?.color?.ifBlank { "غير محدد" } ?: "غير محدد"); AccountRow("رقم اللوحة", driver?.plate?.ifBlank { "غير محدد" } ?: "غير محدد") } }; item { AccountCard("المستندات") { listOf("الهوية" to (driver?.documents?.identity?.status ?: "pending"), "الرخصة" to (driver?.documents?.license?.status ?: "pending"), "أوراق السيارة" to (driver?.documents?.vehicle?.status ?: "pending")).forEach { item -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text(item.first, Modifier.weight(1f), color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold); val approved = item.second.equals("approved", true); Surface(color = if (approved) Mint else SoftAmber, shape = RoundedCornerShape(999.dp)) { Text(if (approved) "معتمد" else "قيد المراجعة", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = if (approved) Green else Amber, fontSize = 8.sp, fontWeight = FontWeight.Bold) } } }; OutlinedButton(onClick = onDocuments, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) { Text("إدارة الوثائق", fontWeight = FontWeight.Bold) } } }; item { Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Red), shape = RoundedCornerShape(15.dp)) { Text("تسجيل الخروج", color = White, fontWeight = FontWeight.Black) } } } }

@Composable private fun AccountCard(title: String, content: @Composable Column.() -> Unit) { Surface(color = White, shape = RoundedCornerShape(21.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp), content = { Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(4.dp)); content() }) } }

@Composable private fun AccountRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f), color = Muted, fontSize = 9.sp); Text(value, color = Ink, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End) } }

@Composable private fun ReceiptDialog(trip: Trip, busy: Boolean, onLater: () -> Unit, onRate: (Int) -> Unit) { var score by remember { mutableIntStateOf(5) }; AlertDialog(onDismissRequest = onLater, title = { Text("ملخص الفاتورة", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("المبلغ المطلوب", color = Muted, fontSize = 10.sp); Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontSize = 28.sp, fontWeight = FontWeight.Black); Surface(color = if (trip.paymentMethod == "cash") SoftAmber else Mint, shape = RoundedCornerShape(14.dp)) { Text(if (trip.paymentMethod == "cash") "تحصيل نقدي من الزبون" else "الدفع الإلكتروني حسب طريقة الرحلة", Modifier.padding(12.dp), color = if (trip.paymentMethod == "cash") Amber else Green, fontSize = 9.sp, fontWeight = FontWeight.Bold) }; Text("قيّم الزبون", color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { (1..5).forEach { star -> Text("★", Modifier.clickable { score = star }, color = if (star <= score) Amber else Color(0xFFD0D8D3), fontSize = 26.sp) } } } }, dismissButton = { TextButton(onClick = onLater, enabled = !busy) { Text("لاحقاً") } }, confirmButton = { Button(onClick = { onRate(score) }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("حفظ وإنهاء", color = White) } }) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ChatDialog(trip: Trip, messages: List<TripMessage>, text: String, busy: Boolean, onTextChange: (String) -> Unit, onDismiss: () -> Unit, onSend: () -> Unit) { ModalBottomSheet(onDismissRequest = onDismiss) { Column(Modifier.fillMaxWidth().height(530.dp).padding(14.dp).navigationBarsPadding()) { Text("الدردشة مع ${trip.customerName ?: "الزبون"}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black); Text("الرحلة #${trip.id.takeLast(6)}", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)); LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) { items(messages, key = { it.id }) { message -> Surface(Modifier.fillMaxWidth(), color = if (message.senderRole == "driver") Mint else White, shape = RoundedCornerShape(14.dp), shadowElevation = 1.dp) { Column(Modifier.padding(10.dp)) { Text(if (message.senderRole == "driver") "أنت" else "الزبون", color = Muted, fontSize = 7.sp); Text(message.text, color = Ink, fontSize = 10.sp) } } } }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = text, onValueChange = onTextChange, Modifier.weight(1f), singleLine = true, placeholder = { Text("اكتب رسالة") }); Button(onClick = onSend, enabled = !busy && text.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Green), shape = RoundedCornerShape(14.dp)) { Text("إرسال", color = White) } } } } }

private fun formatSyrianPhone(phone: String?): String { val digits = phone.orEmpty().filter(Char::isDigit).removePrefix("963").take(9); if (digits.isEmpty()) return "+963"; return "+963 ${digits.take(2)} ${digits.drop(2).take(3)} ${digits.drop(5).take(4)}".trim() }
private fun coordText(point: Coordinates): String = "${String.format(Locale.US, "%.5f", point.lat)} ، ${String.format(Locale.US, "%.5f", point.lng)}"
private fun documentStatus(status: String?): String = if (status.equals("approved", true)) "معتمد" else "قيد المراجعة"
private fun dial(context: Context, phone: String) { runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) } }
private fun navigate(context: Context, point: Coordinates) { val google = Uri.parse("google.navigation:q=${point.lat},${point.lng}"); val geo = Uri.parse("geo:${point.lat},${point.lng}?q=${point.lat},${point.lng}"); runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, google).apply { setPackage("com.google.android.apps.maps") }) }.onFailure { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, geo)) } } }
private fun statusLabel(status: String): String = when (status) { "driver_assigned" -> "تم القبول"; "arriving" -> "في الطريق"; "in_progress" -> "بالرحلة"; "completed" -> "مكتملة"; "cancelled" -> "ملغاة"; else -> status }
