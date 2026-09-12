package com.waslha.captain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Green = Color(0xFF087F5B)
private val Background = Color(0xFFF4F7F5)
private val Ink = Color(0xFF14201B)
private val Muted = Color(0xFF6B7872)
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
        Surface(modifier = Modifier.fillMaxSize(), color = Background) {
            if (signedIn) {
                CaptainHome(session = session, onLogout = {
                    session.clear()
                    signedIn = false
                })
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
        val normalized = phone.trim()
        if (normalized.filter(Char::isDigit).length < 8) {
            error = "أدخل رقم هاتف صحيح"
            return
        }
        loading = true
        error = null
        scope.launch {
            runCatching { CaptainApiProvider.api.requestCode(OtpRequest(normalized)) }
                .onSuccess { response ->
                    if (response.success) {
                        sent = true
                        devCode = response.data?.devCode
                    } else {
                        error = response.message ?: "تعذر إرسال رمز التحقق"
                    }
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
                    if (response.success && data != null && data.role.equals("driver", ignoreCase = true)) {
                        onSuccess(data)
                    } else {
                        error = response.message ?: "الحساب غير مسجل ككابتن"
                    }
                }
                .onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            loading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(68.dp).background(Green, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("و", color = White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                }
                Text("وصلها كابتن", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("سجل دخولك وابدأ استقبال الرحلات", color = Muted, fontSize = 13.sp)

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
                    devCode?.takeIf { it.isNotBlank() }?.let {
                        Text("رمز الاختبار: $it", color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                error?.let { Text(it, color = Red, fontSize = 12.sp) }

                Button(
                    onClick = { if (sent) verifyCode() else requestCode() },
                    enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (sent) "دخول" else "إرسال الرمز", fontWeight = FontWeight.Bold)
                    }
                }

                if (sent) {
                    TextButton(onClick = {
                        sent = false
                        code = ""
                        devCode = null
                        error = null
                    }) {
                        Text("تغيير الرقم", color = Green)
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptainHome(
    session: CaptainSession,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }
    var driver by remember { mutableStateOf<Driver?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var availableTrips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var activeTrip by remember { mutableStateOf<Trip?>(null) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.me() }
            .onSuccess { driver = it.data }
            .onFailure { error = it.message ?: "تعذر تحميل بيانات الكابتن" }

        runCatching { CaptainApiProvider.api.trips() }
            .onSuccess { result ->
                trips = result.data.orEmpty()
                activeTrip = trips.firstOrNull { it.status in setOf("driver_assigned", "arriving", "in_progress") }
            }
            .onFailure { error = it.message ?: "تعذر تحميل الرحلات" }

        if (driver?.available == true) {
            runCatching { CaptainApiProvider.api.availableTrips() }
                .onSuccess { availableTrips = it.data.orEmpty() }
                .onFailure { error = it.message ?: "تعذر تحميل الطلبات" }
        } else {
            availableTrips = emptyList()
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        refresh()
        loading = false
    }

    LaunchedEffect(driver?.available) {
        if (driver?.available != true) return@LaunchedEffect
        while (true) {
            delay(5000)
            refresh()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = White) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Text("⌂") }, label = { Text("الرئيسية") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Text("↺") }, label = { Text("الرحلات") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Text("○") }, label = { Text("حسابي") })
            }
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green)
            }
            tab == 0 -> CaptainDashboard(
                modifier = Modifier.padding(padding),
                driver = driver,
                trips = trips,
                availableTrips = availableTrips,
                activeTrip = activeTrip,
                busy = busy,
                error = error,
                onToggleAvailability = {
                    busy = true
                    error = null
                    scope.launch {
                        runCatching {
                            CaptainApiProvider.api.availability(
                                DriverAvailabilityRequest(driver?.available != true)
                            )
                        }.onSuccess {
                            driver = it.data
                            if (driver?.available != true) availableTrips = emptyList()
                        }.onFailure {
                            error = it.message ?: "تعذر تحديث الحالة"
                        }
                        busy = false
                    }
                },
                onAcceptTrip = { id ->
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.acceptTrip(id) }
                            .onSuccess {
                                activeTrip = it.data
                                refresh()
                            }
                            .onFailure { error = it.message ?: "تعذر قبول الرحلة" }
                        busy = false
                    }
                },
                onStatus = { id, status ->
                    busy = true
                    error = null
                    scope.launch {
                        runCatching { CaptainApiProvider.api.updateTripStatus(id, TripStatusRequest(status)) }
                            .onSuccess {
                                activeTrip = it.data
                                refresh()
                            }
                            .onFailure { error = it.message ?: "تعذر تحديث حالة الرحلة" }
                        busy = false
                    }
                }
            )
            tab == 1 -> CaptainTripHistory(Modifier.padding(padding), trips)
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
    onAcceptTrip: (String) -> Unit,
    onStatus: (String, String) -> Unit
) {
    val completed = trips.count { it.status == "completed" }
    val earnings = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }
    val online = driver?.available == true

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("الرئيسية", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text(
                    driver?.name?.takeIf { it.isNotBlank() }?.let { "أهلاً $it" } ?: "أهلاً كابتن",
                    color = Muted,
                    fontSize = 13.sp
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = if (online) Green else White),
                onClick = { if (!busy) onToggleAvailability() }
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (online) "أنت متصل" else "أنت غير متصل", color = if (online) White else Ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (online) "استقبال طلبات الرحلات مفعّل" else "فعّل الحالة حتى تستقبل طلبات جديدة",
                        color = if (online) White.copy(alpha = 0.78f) else Muted,
                        fontSize = 12.sp
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("الرحلات", completed.toString(), Modifier.weight(1f))
                StatCard("الأرباح", "$earnings", Modifier.weight(1f))
                StatCard("التقييم", String.format("%.1f", driver?.rating ?: 0.0), Modifier.weight(1f))
            }
        }
        error?.let { message -> item { Text(message, color = Red, fontSize = 12.sp) } }

        activeTrip?.let { trip ->
            item { ActiveTripCard(trip, busy, onStatus) }
        }

        item { Text("الطلبات المتاحة", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }

        if (online && activeTrip == null && availableTrips.isEmpty()) {
            item { Text("لا توجد طلبات متاحة حالياً", color = Muted, fontSize = 12.sp) }
        }

        items(availableTrips) { trip ->
            AvailableTripCard(trip, busy, onAcceptTrip)
        }
    }
}

@Composable
private fun ActiveTripCard(trip: Trip, busy: Boolean, onStatus: (String, String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Green)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("رحلة نشطة", color = White, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("المبلغ: ${trip.estimatedFare} ${trip.currency}", color = White, fontWeight = FontWeight.Bold)
            Text("المسافة: ${trip.distanceKm} كم", color = White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text("الحالة: ${trip.status}", color = White.copy(alpha = 0.85f), fontSize = 12.sp)
            when (trip.status) {
                "driver_assigned" -> Button(onClick = { onStatus(trip.id, "arriving") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("أنا في الطريق") }
                "arriving" -> Button(onClick = { onStatus(trip.id, "in_progress") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("بدأت الرحلة") }
                "in_progress" -> Button(onClick = { onStatus(trip.id, "completed") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("إنهاء الرحلة") }
            }
        }
    }
}

@Composable
private fun AvailableTripCard(trip: Trip, busy: Boolean, onAcceptTrip: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("طلب جديد", color = Ink, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(2.dp))
                    Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 12.sp)
                }
                Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black)
            }
            Button(onClick = { onAcceptTrip(trip.id) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text("قبول الرحلة")
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(title, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CaptainTripHistory(modifier: Modifier, trips: List<Trip>) {
    if (trips.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا توجد رحلات بعد", color = Muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("رحلاتي", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black) }
        items(trips) { trip ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("رحلة #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Bold)
                    Text("${trip.status} • ${trip.distanceKm} كم", color = Muted, fontSize = 12.sp)
                    Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun CaptainAccount(modifier: Modifier, driver: Driver?, onLogout: () -> Unit) {
    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("حسابي", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = White)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text(driver?.phone ?: "رقم الهاتف غير متوفر", color = Muted, fontSize = 12.sp)
                Text(driver?.vehicle ?: "السيارة غير مسجلة", color = Ink, fontWeight = FontWeight.Bold)
                Text(driver?.plate ?: "", color = Muted, fontSize = 12.sp)
                Text("التقييم ${String.format("%.1f", driver?.rating ?: 0.0)}", color = Green, fontWeight = FontWeight.Bold)
            }
        }
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("تسجيل الخروج")
        }
    }
}
