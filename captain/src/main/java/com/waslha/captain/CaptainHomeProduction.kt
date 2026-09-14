package com.waslha.captain

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
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

private val CaptainGreen = Color(0xFF0B805E)
private val CaptainDark = Color(0xFF075B43)
private val CaptainBg = Color(0xFFF3F7F5)
private val CaptainInk = Color(0xFF14211C)
private val CaptainMuted = Color(0xFF6E7D76)
private val CaptainRed = Color(0xFFB42318)
private val CaptainWhite = Color.White
private val ActiveTripStatuses = setOf("driver_assigned", "arriving", "in_progress")

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
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    var incomingTrip by remember { mutableStateOf<Trip?>(null) }
    var completedTrip by remember { mutableStateOf<Trip?>(null) }
    var rejectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableIntStateOf(15) }
    var chatTrip by remember { mutableStateOf<Trip?>(null) }
    var messages by remember { mutableStateOf<List<TripMessage>>(emptyList()) }
    var messageText by remember { mutableStateOf("") }
    var lastMessageAt by remember { mutableStateOf(0L) }

    fun showError(error: Throwable, fallback: String) {
        scope.launch { snackbar.showSnackbar(error.message?.takeIf { it.isNotBlank() } ?: fallback) }
    }

    suspend fun refresh() {
        runCatching { repository.me() }
            .onSuccess { response -> if (response.success) driver = response.data }
            .onFailure { showError(it, "تعذر تحميل بيانات الكابتن") }
        runCatching { repository.trips() }
            .onSuccess { response ->
                if (response.success) {
                    trips = response.data.orEmpty().sortedByDescending { it.updatedAt }
                    activeTrip = trips.firstOrNull { it.status in ActiveTripStatuses }
                }
            }
            .onFailure { showError(it, "تعذر تحميل الرحلات") }
        if (driver?.available == true && activeTrip == null) {
            runCatching { repository.availableTrips() }
                .onSuccess { response -> if (response.success) availableTrips = response.data.orEmpty() }
                .onFailure { showError(it, "تعذر تحديث الطلبات") }
        } else {
            availableTrips = emptyList()
        }
        loading = false
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            reporter.start({ location = it }) { driver?.available == true }
        } else {
            scope.launch { snackbar.showSnackbar("السماح بالموقع مطلوب لتشغيل GPS") }
        }
    }

    DisposableEffect(Unit) {
        onDispose { reporter.stop() }
    }

    LaunchedEffect(Unit) {
        refresh()
        if (reporter.hasPermission()) {
            reporter.start({ location = it }) { driver?.available == true }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(driver?.available, activeTrip?.id) {
        if (driver?.available != true) return@LaunchedEffect
        while (true) {
            delay(3500)
            refresh()
        }
    }

    val nextIncoming = availableTrips.firstOrNull { it.id !in rejectedIds }
    LaunchedEffect(nextIncoming?.id, activeTrip?.id) {
        if (nextIncoming == null || activeTrip != null) return@LaunchedEffect
        incomingTrip = nextIncoming
        secondsLeft = 15
        while (secondsLeft > 0 && incomingTrip?.id == nextIncoming.id) {
            delay(1000)
            secondsLeft--
        }
        if (secondsLeft == 0 && incomingTrip?.id == nextIncoming.id) {
            rejectedIds = rejectedIds + nextIncoming.id
            incomingTrip = null
        }
    }

    LaunchedEffect(chatTrip?.id) {
        messages = emptyList()
        lastMessageAt = 0L
        while (chatTrip != null) {
            runCatching { repository.messages(chatTrip!!.id, lastMessageAt) }
                .onSuccess { response ->
                    val batch = response.data.orEmpty()
                    if (batch.isNotEmpty()) {
                        messages = (messages + batch).distinctBy { it.id }.sortedBy { it.createdAt }
                        lastMessageAt = messages.maxOfOrNull { it.createdAt } ?: lastMessageAt
                    }
                }
            delay(2200)
        }
    }

    val todayTrips = trips.filter { isToday(it.updatedAt.takeIf { it > 0 } ?: it.createdAt) }
    val todayCompleted = todayTrips.count { it.status == "completed" }
    val todayEarnings = todayTrips.filter { it.status == "completed" }.sumOf { it.estimatedFare }

    Scaffold(
        containerColor = CaptainBg,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar(containerColor = CaptainWhite, modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Text("⌂") }, label = { Text("الرئيسية") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Text("↺") }, label = { Text("رحلاتي") })
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Text("○") }, label = { Text("حسابي") })
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> CaptainDashboard(
                    driver = driver,
                    location = location,
                    activeTrip = activeTrip,
                    availableTrips = availableTrips.filter { it.id !in rejectedIds },
                    todayEarnings = todayEarnings,
                    todayCompleted = todayCompleted,
                    busy = busy,
                    onToggleOnline = {
                        val next = driver?.available != true
                        busy = true
                        scope.launch {
                            runCatching { repository.availability(next) }
                                .onSuccess { response ->
                                    if (response.success) driver = response.data
                                    else response.message?.let { snackbar.showSnackbar(it) }
                                }
                                .onFailure { showError(it, "تعذر تغيير حالة الاتصال") }
                            busy = false
                        }
                    },
                    onAccept = { id ->
                        busy = true
                        scope.launch {
                            runCatching { repository.acceptTrip(id) }
                                .onSuccess { response ->
                                    if (response.success) {
                                        activeTrip = response.data
                                        incomingTrip = null
                                        refresh()
                                    } else response.message?.let { snackbar.showSnackbar(it) }
                                }
                                .onFailure { showError(it, "تعذر قبول الطلب") }
                            busy = false
                        }
                    },
                    onStatus = { id, status ->
                        busy = true
                        scope.launch {
                            runCatching { repository.updateTripStatus(id, status) }
                                .onSuccess { response ->
                                    if (response.success) {
                                        val updated = response.data
                                        if (status == "completed" && updated != null) {
                                            completedTrip = updated
                                            activeTrip = null
                                        } else {
                                            activeTrip = updated
                                        }
                                        refresh()
                                    } else response.message?.let { snackbar.showSnackbar(it) }
                                }
                                .onFailure { showError(it, "تعذر تحديث حالة الرحلة") }
                            busy = false
                        }
                    },
                    onCall = { phone -> dial(context, phone) },
                    onChat = { chatTrip = it },
                    onNavigate = { navigate(context, it) },
                    onAccount = { tab = 2 }
                )
                1 -> CaptainTripsScreen(trips)
                else -> CaptainAccountScreen(driver, onLogout) {
                    context.startActivity(Intent(context, CaptainDocumentsActivity::class.java))
                }
            }
            if (loading) {
                Surface(
                    Modifier.align(Alignment.Center),
                    color = CaptainWhite,
                    shape = RoundedCornerShape(18.dp),
                    shadowElevation = 6.dp
                ) {
                    CircularProgressIndicator(
                        Modifier.padding(24.dp).size(30.dp),
                        color = CaptainGreen,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }

    incomingTrip?.let { trip ->
        ModalBottomSheet(onDismissRequest = { incomingTrip = null }) {
            IncomingTripSheet(
                trip = trip,
                seconds = secondsLeft,
                busy = busy,
                onReject = {
                    rejectedIds = rejectedIds + trip.id
                    incomingTrip = null
                },
                onAccept = {
                    busy = true
                    scope.launch {
                        runCatching { repository.acceptTrip(trip.id) }
                            .onSuccess { response ->
                                if (response.success) {
                                    activeTrip = response.data
                                    incomingTrip = null
                                    refresh()
                                } else response.message?.let { snackbar.showSnackbar(it) }
                            }
                            .onFailure { showError(it, "تعذر قبول الطلب") }
                        busy = false
                    }
                }
            )
        }
    }

    completedTrip?.let { trip ->
        RatingDialog(
            trip = trip,
            busy = busy,
            onLater = { completedTrip = null },
            onRate = { score ->
                busy = true
                scope.launch {
                    runCatching {
                        repository.rateTrip(
                            DriverRatingRequest(trip.id, trip.customerId, driver?.id.orEmpty(), score)
                        )
                    }.onSuccess { response ->
                        if (response.success) completedTrip = null
                        else response.message?.let { snackbar.showSnackbar(it) }
                    }.onFailure { showError(it, "تعذر حفظ التقييم") }
                    busy = false
                }
            }
        )
    }

    chatTrip?.let { trip ->
        ChatSheet(
            trip = trip,
            messages = messages,
            text = messageText,
            busy = busy,
            onTextChange = { messageText = it },
            onDismiss = { chatTrip = null; messageText = "" },
            onSend = {
                val text = messageText.trim()
                if (text.isNotEmpty()) {
                    busy = true
                    scope.launch {
                        runCatching { repository.sendMessage(trip.id, text) }
                            .onSuccess { response ->
                                if (response.success && response.data != null) {
                                    messages = (messages + response.data).distinctBy { it.id }
                                    lastMessageAt = maxOf(lastMessageAt, response.data.createdAt)
                                    messageText = ""
                                } else response.message?.let { snackbar.showSnackbar(it) }
                            }
                            .onFailure { showError(it, "تعذر إرسال الرسالة") }
                        busy = false
                    }
                }
            }
        )
    }
}

@Composable
private fun CaptainDashboard(
    driver: Driver?,
    location: Coordinates?,
    activeTrip: Trip?,
    availableTrips: List<Trip>,
    todayEarnings: Int,
    todayCompleted: Int,
    busy: Boolean,
    onToggleOnline: () -> Unit,
    onAccept: (String) -> Unit,
    onStatus: (String, String) -> Unit,
    onCall: (String) -> Unit,
    onChat: (Trip) -> Unit,
    onNavigate: (Coordinates) -> Unit,
    onAccount: () -> Unit
) {
    val online = driver?.available == true
    Box(Modifier.fillMaxSize()) {
        CaptainLiveMap(
            Modifier.fillMaxSize(),
            location ?: driver?.let { Coordinates(it.lat, it.lng) },
            activeTrip?.pickup,
            activeTrip?.destination
        )

        Surface(
            Modifier.padding(12.dp).fillMaxWidth().align(Alignment.TopCenter),
            color = CaptainWhite.copy(alpha = 0.97f),
            shape = RoundedCornerShape(22.dp),
            shadowElevation = 7.dp
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن",
                            color = CaptainInk,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            if (online) "متصل الآن • GPS مباشر" else "غير متصل • لن تستقبل طلبات",
                            color = if (online) CaptainGreen else CaptainMuted,
                            fontSize = 9.sp
                        )
                    }
                    Button(
                        onClick = onToggleOnline,
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (online) CaptainDark else Color(0xFFE8EFEB),
                            contentColor = if (online) CaptainWhite else CaptainInk
                        ),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(if (online) "ONLINE" else "OFFLINE", fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CaptainStat("أرباح اليوم", "$todayEarnings ل.س")
                    CaptainStat("المكتملة", todayCompleted.toString())
                    CaptainStat("التقييم", String.format(Locale.US, "%.1f", driver?.rating ?: 0.0))
                }
            }
        }

        Surface(
            Modifier.padding(top = 152.dp, end = 12.dp).align(Alignment.TopEnd),
            color = CaptainWhite.copy(alpha = 0.96f),
            shape = CircleShape,
            shadowElevation = 5.dp
        ) {
            IconButton(onClick = onAccount) { Text("👤", fontSize = 18.sp) }
        }

        when {
            activeTrip != null -> ActiveTripSheet(activeTrip, busy, onStatus, onCall, onChat, onNavigate)
            !online -> BottomNotice(
                title = "ابدأ استقبال الرحلات",
                subtitle = "فعّل ONLINE حتى يبدأ استقبال الطلبات وتتبع موقعك.",
                button = "تشغيل ONLINE",
                enabled = !busy,
                onClick = onToggleOnline
            )
            availableTrips.isEmpty() -> BottomNotice(
                title = "أنت متصل",
                subtitle = "لا توجد طلبات حالياً. التطبيق يفحص الطلبات تلقائياً.",
                button = null,
                enabled = false,
                onClick = {}
            )
        }
    }
}

@Composable
private fun CaptainStat(label: String, value: String) {
    Surface(
        Modifier.weight(1f),
        color = Color(0xFFE8F5F0),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = CaptainDark, fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
            Text(label, color = CaptainMuted, fontSize = 7.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BottomNotice(
    title: String,
    subtitle: String,
    button: String?,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth(),
        color = CaptainWhite.copy(alpha = 0.98f),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 9.dp
    ) {
        Column(Modifier.padding(15.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = CaptainInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(subtitle, color = CaptainMuted, fontSize = 9.sp, textAlign = TextAlign.Center)
            if (button != null) {
                Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CaptainGreen), shape = RoundedCornerShape(14.dp)) {
                    Text(button, color = CaptainWhite, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ActiveTripSheet(
    trip: Trip,
    busy: Boolean,
    onStatus: (String, String) -> Unit,
    onCall: (String) -> Unit,
    onChat: (Trip) -> Unit,
    onNavigate: (Coordinates) -> Unit
) {
    val nextStatus = when (trip.status) {
        "driver_assigned" -> "arriving"
        "arriving" -> "in_progress"
        "in_progress" -> "completed"
        else -> ""
    }
    val title = when (trip.status) {
        "driver_assigned" -> "في الطريق إلى الزبون"
        "arriving" -> "وصلت للزبون"
        "in_progress" -> "الرحلة جارية"
        else -> "الرحلة"
    }
    val button = when (trip.status) {
        "driver_assigned" -> "وصلت للزبون"
        "arriving" -> "بدء الرحلة"
        "in_progress" -> "إنهاء الرحلة"
        else -> "مكتملة"
    }

    Surface(
        Modifier.align(Alignment.BottomCenter).padding(12.dp).fillMaxWidth(),
        color = CaptainWhite.copy(alpha = 0.99f),
        shape = RoundedCornerShape(23.dp),
        shadowElevation = 10.dp
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, color = CaptainInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Text(
                "${trip.customerName ?: "الزبون"} • ${trip.distanceKm} كم • ${trip.durationMin} دقيقة • ${trip.estimatedFare} ${trip.currency}",
                color = CaptainMuted,
                fontSize = 9.sp
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedButton(onClick = { trip.customerPhone?.let(onCall) }, enabled = trip.customerPhone != null, modifier = Modifier.weight(1f)) { Text("اتصال", fontSize = 8.sp) }
                OutlinedButton(onClick = { onChat(trip) }, modifier = Modifier.weight(1f)) { Text("شات", fontSize = 8.sp) }
                OutlinedButton(onClick = { onNavigate(if (trip.status == "in_progress") trip.destination else trip.pickup) }, modifier = Modifier.weight(1f)) { Text("ملاحة", fontSize = 8.sp) }
            }
            Button(onClick = { if (nextStatus.isNotBlank()) onStatus(trip.id, nextStatus) }, enabled = !busy && nextStatus.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CaptainGreen), shape = RoundedCornerShape(14.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = CaptainWhite, strokeWidth = 2.dp) else Text(button, color = CaptainWhite, fontWeight = FontWeight.Black)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomingTripSheet(
    trip: Trip,
    seconds: Int,
    busy: Boolean,
    onReject: () -> Unit,
    onAccept: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(18.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلة جديدة", color = CaptainInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("طلب مباشر • ${seconds} ثانية", color = CaptainMuted, fontSize = 10.sp)
            }
            Surface(color = if (seconds <= 5) Color(0xFFFFE9E7) else Color(0xFFE8F5F0), shape = CircleShape) {
                Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) { Text(seconds.toString(), color = CaptainInk, fontSize = 17.sp, fontWeight = FontWeight.Black) }
            }
        }
        Surface(color = CaptainBg, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("الاستلام", color = CaptainMuted, fontSize = 8.sp)
                Text(formatCoordinates(trip.pickup), color = CaptainInk, fontSize = 10.sp)
                Text("الوجهة", color = CaptainMuted, fontSize = 8.sp)
                Text(formatCoordinates(trip.destination), color = CaptainInk, fontSize = 10.sp)
                Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة • ${trip.estimatedFare} ${trip.currency}", color = CaptainGreen, fontSize = 12.sp, fontWeight = FontWeight.Black)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onReject, enabled = !busy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("رفض", color = CaptainRed, fontWeight = FontWeight.Bold) }
            Button(onClick = onAccept, enabled = !busy, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = CaptainGreen), shape = RoundedCornerShape(14.dp)) { Text("قبول الطلب", color = CaptainWhite, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
private fun CaptainTripsScreen(trips: List<Trip>) {
    var filter by remember { mutableIntStateOf(0) }
    val filtered = trips.filter { if (filter == 0) it.status == "completed" else it.status == "cancelled" }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Text("رحلاتي", color = CaptainInk, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Text("سجل الرحلات المكتملة والملغاة", color = CaptainMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(filter == 0, { filter = 0 }, label = { Text("المكتملة") }, modifier = Modifier.weight(1f))
            FilterChip(filter == 1, { filter = 1 }, label = { Text("الملغاة") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد رحلات في هذا القسم", color = CaptainMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { trip ->
                    Surface(color = CaptainWhite, shape = RoundedCornerShape(18.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("رحلة #${trip.id.takeLast(7)}", color = CaptainInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text(formatDateValue(trip.updatedAt.takeIf { it > 0 } ?: trip.createdAt), color = CaptainMuted, fontSize = 8.sp)
                            Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = CaptainMuted, fontSize = 9.sp)
                            Text("${trip.estimatedFare} ${trip.currency}", color = CaptainGreen, fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptainAccountScreen(driver: Driver?, onLogout: () -> Unit, onDocuments: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("حسابي", color = CaptainInk, fontSize = 25.sp, fontWeight = FontWeight.Black)
            Text("بياناتك والمركبة والوثائق", color = CaptainMuted, fontSize = 9.sp)
        }
        item {
            AccountCard("المعلومات الشخصية") {
                AccountRow("الاسم", driver?.name?.ifBlank { "غير محدد" } ?: "غير محدد")
                AccountRow("الهاتف", formatPhone(driver?.phone))
                AccountRow("التقييم", String.format(Locale.US, "%.1f / 5", driver?.rating ?: 0.0))
                AccountRow("الرصيد", "${driver?.walletBalance ?: 0} ل.س")
            }
        }
        item {
            AccountCard("المركبة") {
                AccountRow("النوع", driver?.type?.ifBlank { "اقتصادي" } ?: "اقتصادي")
                AccountRow("الموديل", driver?.model?.ifBlank { "غير محدد" } ?: "غير محدد")
                AccountRow("اللون", driver?.color?.ifBlank { "غير محدد" } ?: "غير محدد")
                AccountRow("رقم اللوحة", driver?.plate?.ifBlank { "غير محدد" } ?: "غير محدد")
            }
        }
        item {
            AccountCard("المستندات") {
                AccountRow("الهوية", driver?.documents?.identity?.status ?: "pending")
                AccountRow("الرخصة", driver?.documents?.license?.status ?: "pending")
                AccountRow("أوراق السيارة", driver?.documents?.vehicle?.status ?: "pending")
                OutlinedButton(onClick = onDocuments, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                    Text("إدارة الوثائق", fontWeight = FontWeight.Bold)
                }
            }
        }
        item {
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CaptainRed), shape = RoundedCornerShape(14.dp)) {
                Text("تسجيل الخروج", color = CaptainWhite, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun AccountCard(title: String, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Surface(color = CaptainWhite, shape = RoundedCornerShape(20.dp), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = CaptainInk, fontSize = 13.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            content()
        }
    }
}

@Composable
private fun AccountRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = CaptainMuted, fontSize = 9.sp)
        Text(value, color = CaptainInk, fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
    }
}

@Composable
private fun RatingDialog(trip: Trip, busy: Boolean, onLater: () -> Unit, onRate: (Int) -> Unit) {
    var score by remember { mutableIntStateOf(5) }
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text("تقييم الزبون", fontWeight = FontWeight.Black) },
        text = { Text("المبلغ: ${trip.estimatedFare} ${trip.currency}") },
        dismissButton = { TextButton(onClick = onLater, enabled = !busy) { Text("لاحقاً") } },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..5).forEach { star ->
                    TextButton(onClick = { score = star }, enabled = !busy) {
                        Text(if (star <= score) "★" else "☆", fontSize = 22.sp, color = CaptainGreen)
                    }
                }
                Button(onClick = { onRate(score) }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = CaptainGreen)) {
                    Text("حفظ", color = CaptainWhite)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatSheet(
    trip: Trip,
    messages: List<TripMessage>,
    text: String,
    busy: Boolean,
    onTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().height(520.dp).padding(14.dp).navigationBarsPadding()) {
            Text("الدردشة مع ${trip.customerName ?: "الزبون"}", color = CaptainInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("الرحلة #${trip.id.takeLast(6)}", color = CaptainMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(messages, key = { it.id }) { message ->
                    Surface(color = if (message.senderRole == "captain") Color(0xFFE8F5F0) else Color(0xFFF0F2F1), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(message.text, Modifier.padding(10.dp), color = CaptainInk, fontSize = 10.sp)
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(value = text, onValueChange = onTextChange, modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("اكتب رسالة") })
                Spacer(Modifier.width(7.dp))
                Button(onClick = onSend, enabled = !busy && text.isNotBlank()) { Text("إرسال") }
            }
        }
    }
}

private fun isToday(timestamp: Long): Boolean {
    if (timestamp <= 0L) return false
    val format = SimpleDateFormat("yyyyMMdd", Locale.US)
    return format.format(Date(timestamp)) == format.format(Date())
}

private fun formatDateValue(timestamp: Long): String {
    if (timestamp <= 0L) return "بدون تاريخ"
    return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(timestamp))
}

private fun formatPhone(phone: String?): String {
    val value = phone.orEmpty().trim()
    if (value.isBlank()) return "غير محدد"
    return when {
        value.startsWith("+963") -> value
        value.startsWith("0") -> "+963 ${value.drop(1)}"
        else -> "+963 $value"
    }
}

private fun formatCoordinates(point: Coordinates): String =
    String.format(Locale.US, "%.5f, %.5f", point.lat, point.lng)

private fun dial(context: Context, phone: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))
}

private fun navigate(context: Context, point: Coordinates) {
    val uri = Uri.parse("google.navigation:q=${point.lat},${point.lng}")
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        .onFailure {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${point.lat},${point.lng}?q=${point.lat},${point.lng}")))
        }
}
