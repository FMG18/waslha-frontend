package com.waslha.captain

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val VGreen = Color(0xFF0B805E)
private val VDark = Color(0xFF075B43)
private val VMint = Color(0xFFE8F5F0)
private val VBg = Color(0xFFF4F7F6)
private val VInk = Color(0xFF14211C)
private val VMuted = Color(0xFF6E7D76)
private val VWhite = Color.White
private val VRed = Color(0xFFB42318)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptainHomeV3(session: CaptainSession, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var driver by remember { mutableStateOf<Driver?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var available by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var active by remember { mutableStateOf<Trip?>(null) }
    var location by remember { mutableStateOf<Coordinates?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var incoming by remember { mutableStateOf<Trip?>(null) }
    var countdown by remember { mutableIntStateOf(15) }
    var rejectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var chatOpen by remember { mutableStateOf(false) }

    val reporter = remember { CaptainLocationReporter(context) }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            reporter.start { point -> location = point }
        } else {
            error = "يجب السماح بالموقع حتى يظهر موقعك للزبائن"
        }
    }

    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.me() }
            .onSuccess { driver = it.data }
            .onFailure { error = it.message ?: "تعذر تحميل بيانات الكابتن" }
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
        } else available = emptyList()
    }

    LaunchedEffect(Unit) { refresh() }
    LaunchedEffect(driver?.available, active?.id) {
        if (driver?.available != true) return@LaunchedEffect
        while (true) { delay(4000); refresh() }
    }

    LaunchedEffect(driver?.available) {
        if (driver?.available == true) {
            if (reporter.hasPermission()) reporter.start { point -> location = point }
            else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else reporter.stop()
    }
    DisposableEffect(Unit) { onDispose { reporter.stop() } }

    LaunchedEffect(available.firstOrNull { it.id !in rejectedIds }?.id) {
        val candidate = available.firstOrNull { it.id !in rejectedIds }
        if (candidate == null || active != null) { incoming = null; return@LaunchedEffect }
        incoming = candidate
        countdown = 15
        playIncomingTone()
        while (countdown > 0 && incoming?.id == candidate.id) {
            delay(1000)
            countdown -= 1
        }
        if (countdown == 0 && incoming?.id == candidate.id) {
            rejectedIds = rejectedIds + candidate.id
            incoming = null
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = VBg) {
            ScaffoldLike(
                tab = tab,
                onTab = { tab = it },
                content = {
                    when (tab) {
                        0 -> CaptainLiveDashboard(
                            driver = driver,
                            location = location,
                            active = active,
                            available = available.filter { it.id !in rejectedIds },
                            busy = busy,
                            error = error,
                            onToggle = {
                                if (driver?.available == true) reporter.stop()
                                busy = true
                                error = null
                                scope.launch {
                                    runCatching { CaptainApiProvider.api.availability(DriverAvailabilityRequest(driver?.available != true)) }
                                        .onSuccess { response -> driver = response.data }
                                        .onFailure { error = it.message ?: "تعذر تغيير الحالة" }
                                    busy = false
                                }
                            },
                            onAccept = { tripId ->
                                busy = true
                                scope.launch {
                                    runCatching { CaptainApiProvider.api.acceptTrip(tripId) }
                                        .onSuccess { response ->
                                            active = response.data
                                            incoming = null
                                            refresh()
                                        }
                                        .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                                    busy = false
                                }
                            },
                            onStatus = { tripId, status ->
                                busy = true
                                scope.launch {
                                    runCatching { CaptainApiProvider.api.updateTripStatus(tripId, TripStatusRequest(status)) }
                                        .onSuccess { response -> active = response.data; refresh() }
                                        .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                                    busy = false
                                }
                            },
                            onCall = { phone -> dial(context, phone) },
                            onChat = { chatOpen = true },
                            onNavigate = { point -> openNavigation(context, point) },
                            onAccount = { context.startActivity(Intent(context, CaptainAccountCenterActivity::class.java)) }
                        )
                        1 -> CaptainTripsList(trips)
                        else -> CaptainProfilePreview(driver, onLogout) {
                            context.startActivity(Intent(context, CaptainAccountCenterActivity::class.java))
                        }
                    }
                }
            )
        }
    }

    val currentIncoming = incoming
    if (currentIncoming != null) {
        ModalBottomSheet(onDismissRequest = { incoming = null }) {
            IncomingRequestSheet(
                trip = currentIncoming,
                seconds = countdown,
                busy = busy,
                onReject = {
                    rejectedIds = rejectedIds + currentIncoming.id
                    incoming = null
                },
                onAccept = {
                    busy = true
                    scope.launch {
                        runCatching { CaptainApiProvider.api.acceptTrip(currentIncoming.id) }
                            .onSuccess { response -> active = response.data; incoming = null; refresh() }
                            .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                        busy = false
                    }
                }
            )
        }
    }

    if (chatOpen && active != null) {
        CaptainChatDialog(active!!, onDismiss = { chatOpen = false })
    }
}

@Composable
private fun ScaffoldLike(tab: Int, onTab: (Int) -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        NavigationBar(containerColor = VWhite, modifier = Modifier.navigationBarsPadding()) {
            NavigationBarItem(selected = tab == 0, onClick = { onTab(0) }, icon = { Text("⌂") }, label = { Text("الرئيسية") })
            NavigationBarItem(selected = tab == 1, onClick = { onTab(1) }, icon = { Text("↺") }, label = { Text("رحلاتي") })
            NavigationBarItem(selected = tab == 2, onClick = { onTab(2) }, icon = { Text("○") }, label = { Text("حسابي") })
        }
    }
}

@Composable
private fun CaptainLiveDashboard(
    driver: Driver?,
    location: Coordinates?,
    active: Trip?,
    available: List<Trip>,
    busy: Boolean,
    error: String?,
    onToggle: () -> Unit,
    onAccept: (String) -> Unit,
    onStatus: (String, String) -> Unit,
    onCall: (String) -> Unit,
    onChat: () -> Unit,
    onNavigate: (Coordinates) -> Unit,
    onAccount: () -> Unit,
) {
    val online = driver?.available == true
    Box(Modifier.fillMaxSize()) {
        CaptainLiveMap(
            modifier = Modifier.fillMaxSize(),
            driver = location ?: driver?.let { Coordinates(it.lat, it.lng) },
            pickup = active?.pickup,
            destination = active?.destination,
        )
        Surface(
            modifier = Modifier.padding(14.dp).align(Alignment.TopCenter).fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = VWhite.copy(alpha = .96f),
            shadowElevation = 6.dp
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = VInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(if (online) "أنت متصل وتستقبل الرحلات" else "أنت غير متصل", color = if (online) VGreen else VMuted, fontSize = 9.sp)
                }
                StatusSwitch(online = online, enabled = !busy, onClick = onToggle)
            }
        }

        if (error != null) {
            Surface(Modifier.padding(14.dp).align(Alignment.BottomCenter).fillMaxWidth(), color = Color(0xFFFFE9E7), shape = RoundedCornerShape(17.dp)) {
                Text(error, Modifier.padding(12.dp), color = VRed, fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }

        if (active != null) {
            ActiveTripOverlay(active, busy, onStatus, onCall, onChat, onNavigate)
        } else if (!online) {
            Surface(Modifier.padding(14.dp).align(Alignment.BottomCenter).fillMaxWidth(), color = VWhite.copy(.96f), shape = RoundedCornerShape(22.dp), shadowElevation = 6.dp) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ابدأ استقبال الرحلات", color = VInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("فعّل متصل الآن حتى يبدأ نظام وصلها بإرسال الطلبات لك", color = VMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    PrimaryButton("متصل الآن", !busy, Modifier.fillMaxWidth(), onToggle)
                }
            }
        } else if (available.isEmpty()) {
            Surface(Modifier.padding(14.dp).align(Alignment.BottomCenter).fillMaxWidth(), color = VWhite.copy(.95f), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(15.dp)) { Text("لا توجد طلبات الآن", color = VInk, fontWeight = FontWeight.Black); Text("سيتم فحص الطلبات تلقائيًا", color = VMuted, fontSize = 10.sp) }
            }
        }

        Surface(Modifier.padding(14.dp).align(Alignment.BottomStart), color = VWhite.copy(.96f), shape = CircleShape, shadowElevation = 5.dp) {
            IconButton(onClick = onAccount) { Text("👤", fontSize = 18.sp) }
        }
    }
}

@Composable
private fun StatusSwitch(online: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, color = if (online) VDark else Color(0xFFEEF2EF), shape = RoundedCornerShape(999.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(if (online) VWhite else Color(0xFF8C9993), CircleShape))
            Spacer(Modifier.width(7.dp))
            Text(if (online) "متصل الآن" else "غير متصل", color = if (online) VWhite else VInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun ActiveTripOverlay(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit, onCall: (String) -> Unit, onChat: () -> Unit, onNavigate: (Coordinates) -> Unit) {
    val next = when (trip.status) { "driver_assigned" -> "arriving"; "arriving" -> "in_progress"; "in_progress" -> "completed"; else -> null }
    val mainLabel = when (trip.status) { "driver_assigned" -> "أنا في الطريق"; "arriving" -> "وصلت للزبون"; "in_progress" -> "إنهاء الرحلة"; else -> "مكتملة" }
    Surface(Modifier.padding(14.dp).align(Alignment.BottomCenter).fillMaxWidth(), color = VWhite.copy(.97f), shape = RoundedCornerShape(25.dp), shadowElevation = 10.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("الرحلة الحالية • #${trip.id.takeLast(6)}", color = VInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة • ${trip.estimatedFare} ل.س", color = VMuted, fontSize = 10.sp)
                }
                Surface(color = VMint, shape = RoundedCornerShape(999.dp)) { Text(statusText(trip.status), Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = VGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction("اتصال", Modifier.weight(1f)) { trip.customerPhone?.let(onCall) }
                SmallAction("دردشة", Modifier.weight(1f), onChat)
                SmallAction("الملاحة", Modifier.weight(1f)) { onNavigate(if (trip.status == "in_progress") trip.destination else trip.pickup) }
            }
            PrimaryButton(mainLabel, !busy && next != null, Modifier.fillMaxWidth()) { onStatus(trip.id, next ?: "") }
        }
    }
}

@Composable
private fun SmallAction(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier, contentPadding = PaddingValues(vertical = 9.dp), shape = RoundedCornerShape(14.dp)) { Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomingRequestSheet(trip: Trip, seconds: Int, busy: Boolean, onReject: () -> Unit, onAccept: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("رحلة جديدة", color = VInk, fontSize = 21.sp, fontWeight = FontWeight.Black); Text("قريبة منك • طلب مباشر", color = VMuted, fontSize = 10.sp) }
            Surface(color = if (seconds <= 5) Color(0xFFFFE9E7) else VMint, shape = CircleShape) { Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) { Text("$seconds", color = if (seconds <= 5) VRed else VGreen, fontSize = 24.sp, fontWeight = FontWeight.Black) } }
        }
        CardLike("الأجرة التقديرية", "${trip.estimatedFare} ل.س")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { CardLike("المسافة", "${trip.distanceKm} كم", Modifier.weight(1f)); CardLike("الوصول", "${trip.durationMin} دقيقة", Modifier.weight(1f)) }
        Surface(color = Color(0xFFF2F5F3), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("الانطلاق", color = VMuted, fontSize = 8.sp); Text("${trip.pickup.lat.format4()} ، ${trip.pickup.lng.format4()}", color = VInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("الوجهة", color = VMuted, fontSize = 8.sp); Text("${trip.destination.lat.format4()} ، ${trip.destination.lng.format4()}", color = VInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedButton(onClick = onReject, enabled = !busy, modifier = Modifier.weight(1f), shape = RoundedCornerShape(17.dp)) { Text("رفض", color = VRed, fontWeight = FontWeight.Bold) }
            PrimaryButton("قبول الطلب", !busy, Modifier.weight(1.6f), onAccept)
        }
    }
}

@Composable
private fun CardLike(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier.fillMaxWidth(), color = Color(0xFFF7FAF8), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(12.dp)) { Text(title, color = VMuted, fontSize = 8.sp); Text(value, color = VInk, fontSize = 15.sp, fontWeight = FontWeight.Black) } }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = VGreen, contentColor = VWhite)) {
        if (enabled) Text(text, fontWeight = FontWeight.Black, fontSize = 12.sp) else CircularProgressIndicator(Modifier.size(18.dp), color = VWhite, strokeWidth = 2.dp)
    }
}

@Composable
private fun CaptainTripsList(trips: List<Trip>) {
    LazyColumn(Modifier.fillMaxSize().background(VBg), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("رحلاتي", color = VInk, fontSize = 26.sp, fontWeight = FontWeight.Black); Text("كل الرحلات المرتبطة بحسابك", color = VMuted, fontSize = 10.sp) }
        items(trips, key = { it.id }) { trip -> Surface(Modifier.fillMaxWidth(), color = VWhite, shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("#${trip.id.takeLast(6)}", color = VInk, fontWeight = FontWeight.Black); Text(statusText(trip.status), color = VMuted, fontSize = 9.sp) }; Text("${trip.estimatedFare} ل.س", color = VGreen, fontWeight = FontWeight.Black) } } }
        if (trips.isEmpty()) item { Text("لا توجد رحلات مسجلة بعد", color = VMuted, modifier = Modifier.fillMaxWidth().padding(40.dp), textAlign = TextAlign.Center) }
    }
}

@Composable
private fun CaptainProfilePreview(driver: Driver?, onLogout: () -> Unit, onDocuments: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().background(VBg), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("حساب الكابتن", color = VInk, fontSize = 26.sp, fontWeight = FontWeight.Black); Text("بياناتك، مركبتك ومستنداتك", color = VMuted, fontSize = 10.sp) }
        item { Surface(Modifier.fillMaxWidth(), color = VWhite, shape = RoundedCornerShape(23.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(58.dp).background(VMint, CircleShape), contentAlignment = Alignment.Center) { Text("و", color = VGreen, fontSize = 25.sp, fontWeight = FontWeight.Black) }; Spacer(Modifier.width(12.dp)); Column { Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = VInk, fontSize = 18.sp, fontWeight = FontWeight.Black); Text(driver?.phone ?: "+963", color = VMuted, fontSize = 10.sp); Text("${driver?.vehicle.orEmpty()} • ${driver?.plate.orEmpty()}", color = VMuted, fontSize = 9.sp) } } } }
        item { Surface(Modifier.fillMaxWidth().clickable(onClick = onDocuments), color = VWhite, shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(17.dp)) { Text("الملف والمركبة", color = VInk, fontWeight = FontWeight.Black); Text("استعراض الهوية والرخصة وأوراق السيارة وحالتها", color = VMuted, fontSize = 9.sp) } } }
        item { Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = VRed, contentColor = VWhite)) { Text("تسجيل الخروج", fontWeight = FontWeight.Black) } }
    }
}

@Composable
private fun CaptainChatDialog(trip: Trip, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var messages by remember { mutableStateOf<List<TripMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(trip.id) {
        while (true) {
            runCatching { CaptainApiProvider.api.tripMessages(trip.id) }.onSuccess { messages = it.data.orEmpty() }
            delay(2500)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("دردشة مع ${trip.customerName ?: "الزبون"}", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyColumn(Modifier.fillMaxWidth().height(230.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(messages, key = { it.id }) { message -> Surface(color = if (message.senderRole == "driver") VMint else Color(0xFFF1F3F2), shape = RoundedCornerShape(12.dp)) { Text(message.text, Modifier.padding(9.dp), color = VInk, fontSize = 10.sp) } }
                }
                OutlinedTextField(draft, { draft = it.take(500) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("اكتب رسالة") })
                error?.let { Text(it, color = VRed, fontSize = 9.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && draft.isNotBlank(), onClick = {
                busy = true; error = null
                scope.launch {
                    runCatching { CaptainApiProvider.api.sendTripMessage(trip.id, TripMessageRequest(draft.trim())) }
                        .onSuccess { response -> if (response.success) draft = "" else error = response.message ?: "تعذر الإرسال" }
                        .onFailure { error = it.message ?: "تعذر الإرسال" }
                    busy = false
                }
            }) { Text("إرسال", color = VGreen, fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

private fun playIncomingTone() {
    runCatching {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val player = MediaPlayer.create(null, uri) ?: return
        player.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        player.setOnCompletionListener { it.release() }
        player.start()
    }
}

private fun dial(context: android.content.Context, phone: String) {
    if (phone.isBlank()) return
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}")))
}

private fun openNavigation(context: android.content.Context, point: Coordinates) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${point.lat},${point.lng}")))
    }.onFailure {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${point.lat},${point.lng}")))
    }
}

private fun statusText(status: String) = when (status) { "driver_assigned" -> "تم قبول الرحلة"; "arriving" -> "متجه للزبون"; "in_progress" -> "الرحلة جارية"; "completed" -> "مكتملة"; else -> status }
private fun Double.format4(): String = String.format("%.4f", this)
