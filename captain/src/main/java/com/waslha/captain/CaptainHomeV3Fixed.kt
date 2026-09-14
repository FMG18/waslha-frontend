package com.waslha.captain

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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

private val Green = Color(0xFF0B805E)
private val Dark = Color(0xFF075B43)
private val Mint = Color(0xFFE8F5F0)
private val Bg = Color(0xFFF4F7F6)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color.White
private val Red = Color(0xFFB42318)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptainHomeV3Fixed(session: CaptainSession, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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
    var error by remember { mutableStateOf<String?>(null) }
    var chatTrip by remember { mutableStateOf<Trip?>(null) }

    val reporter = remember { CaptainLocationReporter(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            reporter.start { location = it }
        } else error = "السماح بالموقع مطلوب لإظهار موقعك للزبائن"
    }

    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.me() }
            .onSuccess { driver = it.data }
            .onFailure { error = it.message ?: "تعذر تحميل الحساب" }
        runCatching { CaptainApiProvider.api.trips() }
            .onSuccess {
                trips = it.data.orEmpty()
                active = trips.firstOrNull { t -> t.status in setOf("driver_assigned", "arriving", "in_progress") }
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
            if (reporter.hasPermission()) reporter.start { location = it }
            else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else reporter.stop()
    }

    val candidate = available.firstOrNull { it.id !in rejected }
    LaunchedEffect(candidate?.id, active?.id) {
        if (candidate == null || active != null) return@LaunchedEffect
        incoming = candidate
        countdown = 15
        while (countdown > 0 && incoming?.id == candidate.id) {
            delay(1000)
            countdown--
        }
        if (countdown == 0 && incoming?.id == candidate.id) {
            rejected = rejected + candidate.id
            incoming = null
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> CaptainMapHome(
                            driver = driver,
                            location = location,
                            active = active,
                            available = available.filter { it.id !in rejected },
                            busy = busy,
                            error = error,
                            onToggle = {
                                busy = true
                                scope.launch {
                                    runCatching { CaptainApiProvider.api.availability(DriverAvailabilityRequest(driver?.available != true)) }
                                        .onSuccess { driver = it.data }
                                        .onFailure { error = it.message ?: "تعذر تغيير الحالة" }
                                    busy = false
                                }
                            },
                            onStatus = { id, status ->
                                busy = true
                                scope.launch {
                                    runCatching { CaptainApiProvider.api.updateTripStatus(id, TripStatusRequest(status)) }
                                        .onSuccess { active = it.data; refresh() }
                                        .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                                    busy = false
                                }
                            },
                            onAccept = { id ->
                                busy = true
                                scope.launch {
                                    runCatching { CaptainApiProvider.api.acceptTrip(id) }
                                        .onSuccess { active = it.data; incoming = null; refresh() }
                                        .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                                    busy = false
                                }
                            },
                            onCall = { dial(context, it) },
                            onChat = { chatTrip = it },
                            onNavigate = { openNavigation(context, it) },
                            onAccount = { context.startActivity(Intent(context, CaptainAccountCenterActivity::class.java)) }
                        )
                        1 -> CaptainTripsPage(trips)
                        else -> CaptainAccountPage(driver, onLogout) {
                            context.startActivity(Intent(context, CaptainDocumentsActivity::class.java))
                        }
                    }
                }
                NavigationBar(containerColor = White, modifier = Modifier.navigationBarsPadding()) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂") }, label = { Text("الرئيسية") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("↺") }, label = { Text("رحلاتي") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("○") }, label = { Text("حسابي") })
                }
            }
        }
    }

    incoming?.let { trip ->
        ModalBottomSheet(onDismissRequest = { incoming = null }) {
            IncomingSheet(trip, countdown, busy,
                onReject = { rejected = rejected + trip.id; incoming = null },
                onAccept = { busy = true; scope.launch {
                    runCatching { CaptainApiProvider.api.acceptTrip(trip.id) }
                        .onSuccess { active = it.data; incoming = null; refresh() }
                        .onFailure { error = it.message ?: "تعذر قبول الطلب" }
                    busy = false
                } })
        }
    }
    chatTrip?.let { trip -> CaptainChat(trip, onDismiss = { chatTrip = null }) }
}

@Composable
private fun CaptainMapHome(
    driver: Driver?, location: Coordinates?, active: Trip?, available: List<Trip>, busy: Boolean, error: String?,
    onToggle: () -> Unit, onStatus: (String, String) -> Unit, onAccept: (String) -> Unit,
    onCall: (String) -> Unit, onChat: (Trip) -> Unit, onNavigate: (Coordinates) -> Unit, onAccount: () -> Unit
) {
    val online = driver?.available == true
    Box(Modifier.fillMaxSize()) {
        CaptainLiveMap(Modifier.fillMaxSize(), location ?: driver?.let { Coordinates(it.lat, it.lng) }, active?.pickup, active?.destination)
        Surface(Modifier.padding(14.dp).fillMaxWidth().align(Alignment.TopCenter), shape = RoundedCornerShape(22.dp), color = White.copy(.96f), shadowElevation = 6.dp) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    Text(if (online) "متصل الآن • تستقبل الطلبات" else "غير متصل", color = if (online) Green else Muted, fontSize = 9.sp)
                }
                Surface(onClick = onToggle, enabled = !busy, color = if (online) Dark else Color(0xFFEEF2EF), shape = RoundedCornerShape(999.dp)) {
                    Text(if (online) "متصل" else "اتصل", Modifier.padding(horizontal = 15.dp, vertical = 9.dp), color = if (online) White else Ink, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }
            }
        }
        if (active != null) {
            ActivePanel(active, busy, onStatus, onCall, onChat, onNavigate)
        } else if (!online) {
            HomeCard(Modifier.align(Alignment.BottomCenter), "ابدأ استقبال الرحلات", "فعّل متصل الآن حتى يبدأ وصلها بإرسال الطلبات لك", "متصل الآن", busy, onToggle)
        } else if (available.isEmpty()) {
            HomeCard(Modifier.align(Alignment.BottomCenter), "لا توجد طلبات الآن", "سيتم تحديث الطلبات تلقائيًا", null, false, {})
        }
        error?.let { Surface(Modifier.padding(14.dp).align(Alignment.BottomStart), color = Color(0xFFFFE9E7), shape = RoundedCornerShape(14.dp)) { Text(it, Modifier.padding(10.dp), color = Red, fontSize = 9.sp) } }
        Surface(Modifier.padding(14.dp).align(Alignment.CenterEnd), color = White.copy(.96f), shape = CircleShape, shadowElevation = 5.dp) { IconButton(onClick = onAccount) { Text("👤") } }
    }
}

@Composable private fun HomeCard(modifier: Modifier, title: String, subtitle: String, button: String?, busy: Boolean, action: () -> Unit) {
    Surface(modifier.padding(14.dp).fillMaxWidth(), color = White.copy(.97f), shape = RoundedCornerShape(24.dp), shadowElevation = 8.dp) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(5.dp)); Text(subtitle, color = Muted, fontSize = 10.sp, textAlign = TextAlign.Center)
            if (button != null) { Spacer(Modifier.height(10.dp)); Button(action, enabled = !busy, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(button, color = White, fontWeight = FontWeight.Black) } }
        }
    }
}

@Composable
private fun BoxScope.ActivePanel(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit, onCall: (String) -> Unit, onChat: (Trip) -> Unit, onNavigate: (Coordinates) -> Unit) {
    val next = when (trip.status) { "driver_assigned" -> "arriving"; "arriving" -> "in_progress"; "in_progress" -> "completed"; else -> "" }
    val label = when (trip.status) { "driver_assigned" -> "أنا في الطريق"; "arriving" -> "وصلت للزبون"; "in_progress" -> "إنهاء الرحلة"; else -> "مكتملة" }
    Surface(Modifier.padding(14.dp).fillMaxWidth().align(Alignment.BottomCenter), color = White.copy(.97f), shape = RoundedCornerShape(25.dp), shadowElevation = 10.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("الرحلة #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black); Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة • ${trip.estimatedFare} ل.س", color = Muted, fontSize = 10.sp) }
                Surface(color = Mint, shape = RoundedCornerShape(999.dp)) { Text(statusLabel(trip.status), Modifier.padding(horizontal = 8.dp, vertical = 6.dp), color = Green, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { trip.customerPhone?.let(onCall) }, modifier = Modifier.weight(1f)) { Text("اتصال", fontSize = 9.sp) }
                OutlinedButton(onClick = { onChat(trip) }, modifier = Modifier.weight(1f)) { Text("دردشة", fontSize = 9.sp) }
                OutlinedButton(onClick = { onNavigate(if (trip.status == "in_progress") trip.destination else trip.pickup) }, modifier = Modifier.weight(1f)) { Text("ملاحة", fontSize = 9.sp) }
            }
            Button(onClick = { onStatus(trip.id, next) }, enabled = !busy && next.isNotBlank(), modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(label, color = White, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable private fun IncomingSheet(trip: Trip, seconds: Int, busy: Boolean, onReject: () -> Unit, onAccept: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(18.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("رحلة جديدة", color = Ink, fontSize = 21.sp, fontWeight = FontWeight.Black); Text("طلب مباشر", color = Muted, fontSize = 10.sp) }
            Surface(color = if (seconds <= 5) Color(0xFFFFE9E7) else Mint, shape = CircleShape) { Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) { Text(seconds.toString(), color = if (seconds <= 5) Red else Green, fontSize = 20.sp, fontWeight = FontWeight.Black) } }
        }
        Text("من ${trip.pickupLabel ?: "الموقع الحالي"} إلى ${trip.destinationLabel ?: "الوجهة"}", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة • ${trip.estimatedFare} ل.س", color = Muted, fontSize = 10.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onReject, enabled = !busy, modifier = Modifier.weight(1f)) { Text("رفض") }
            Button(onClick = onAccept, enabled = !busy, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("قبول", color = White, fontWeight = FontWeight.Black) }
        }
    }
}

@Composable private fun CaptainTripsPage(trips: List<Trip>) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("رحلاتي", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(trips) { trip ->
                Surface(color = White, shape = RoundedCornerShape(20.dp), shadowElevation = 3.dp) {
                    Column(Modifier.padding(14.dp)) {
                        Text("#${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black)
                        Text(statusLabel(trip.status), color = Green, fontSize = 10.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("${trip.pickupLabel ?: "الموقع"} → ${trip.destinationLabel ?: "الوجهة"}", color = Ink, fontSize = 11.sp)
                        Text("${trip.distanceKm} كم • ${trip.estimatedFare} ل.س", color = Muted, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable private fun CaptainAccountPage(driver: Driver?, onLogout: () -> Unit, onDocuments: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("حساب الكابتن", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
        Surface(color = White, shape = RoundedCornerShape(22.dp), shadowElevation = 4.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(52.dp), shape = CircleShape, color = Mint) { Box(contentAlignment = Alignment.Center) { Text("👤", fontSize = 22.sp) } }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(driver?.name ?: "الكابتن", color = Ink, fontWeight = FontWeight.Black)
                    Text(driver?.phone ?: "", color = Muted, fontSize = 10.sp)
                }
            }
        }
        Button(onClick = onDocuments, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("المستندات", color = White, fontWeight = FontWeight.Black) }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Text("تسجيل الخروج") }
    }
}

@Composable private fun CaptainChat(trip: Trip, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }, title = { Text("دردشة الرحلة") }, text = { Text("يمكنك استخدام دردشة الرحلة المرتبطة بالطلب #${trip.id.takeLast(6)} من شاشة الرحلة.") })
}

private fun statusLabel(status: String): String = when (status) {
    "driver_assigned" -> "تم التعيين"
    "arriving" -> "في الطريق"
    "in_progress" -> "داخل الرحلة"
    "completed" -> "مكتملة"
    "cancelled" -> "ملغاة"
    else -> status.replace('_', ' ')
}

private fun dial(context: Context, phone: String) {
    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))
}

private fun openNavigation(context: Context, target: Coordinates) {
    val uri = Uri.parse("google.navigation:q=${target.lat},${target.lng}")
    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
}
