package com.waslha.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TaxiGreen = Color(0xFF078A60)
private val TaxiInk = Color(0xFF10201B)
private val TaxiMuted = Color(0xFF72807B)
private val TaxiBg = Color(0xFFF5F8F6)
private val TaxiDanger = Color(0xFFB42318)
private val TaxiSoft = Color(0xFFE8F6F0)

class TaxiBookingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val session = SessionStore(this)
        val location = LocationProvider(this)
        setContent {
            TaxiBookingApp(
                session = session,
                locationProvider = location,
                onLogout = {
                    session.clear()
                    finish()
                }
            )
        }
    }
}

@Composable
private fun TaxiBookingApp(
    session: SessionStore,
    locationProvider: LocationProvider,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { TripRepository(ApiProvider.api) }
    var pickup by remember { mutableStateOf<Coordinates?>(null) }
    var destination by remember { mutableStateOf<Coordinates?>(null) }
    var destinationName by remember { mutableStateOf("حدد وجهتك على الخريطة") }
    var screen by remember { mutableStateOf("home") }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var loading by remember { mutableStateOf(false) }
    var locationLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var history by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(false) }
    var historyLoaded by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var showProfile by remember { mutableStateOf(false) }

    fun refreshLocation() {
        locationLoading = true
        scope.launch {
            val found = locationProvider.lastKnown()
            if (found != null) {
                pickup = Coordinates(found.latitude, found.longitude)
                error = null
            } else {
                error = "تعذر تحديد موقعك. فعّل GPS واسمح للتطبيق بالموقع."
            }
            locationLoading = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            refreshLocation()
        } else {
            error = "صلاحية الموقع مطلوبة لتحديد نقطة الانطلاق."
        }
    }

    LaunchedEffect(Unit) {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (allowed) refreshLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(trip?.id) {
        val active = trip ?: return@LaunchedEffect
        while (active.status != "completed" && active.status != "cancelled") {
            delay(5000)
            repository.get(active.id).onSuccess { trip = it }
            break
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = TaxiBg) {
            Scaffold(
                containerColor = TaxiBg,
                bottomBar = {
                    if (trip == null && screen != "map") {
                        BottomBar(
                            selected = screen,
                            onHome = { screen = "home" },
                            onHistory = {
                                screen = "history"
                                if (!historyLoaded && session.userId.orEmpty().isNotBlank()) {
                                    historyLoading = true
                                    scope.launch {
                                        repository.list(session.userId).onSuccess { history = it; historyLoaded = true }
                                            .onFailure { error = it.message ?: "تعذر تحميل الرحلات" }
                                        historyLoading = false
                                    }
                                }
                            },
                            onProfile = { showProfile = true }
                        )
                    }
                }
            ) { padding ->
                when {
                    trip != null -> ActiveTripScreen(
                        trip = trip!!,
                        busy = loading,
                        error = error,
                        onRefresh = {
                            loading = true
                            scope.launch {
                                repository.get(trip!!.id).onSuccess { trip = it }.onFailure { error = it.message }
                                loading = false
                            }
                        },
                        onCancel = {
                            loading = true
                            scope.launch {
                                repository.cancel(trip!!.id, "إلغاء من الراكب")
                                    .onSuccess { trip = it }
                                    .onFailure { error = it.message ?: "تعذر إلغاء الرحلة" }
                                loading = false
                            }
                        },
                        onBack = {
                            if (trip?.status == "completed" || trip?.status == "cancelled") {
                                trip = null
                                destination = null
                                destinationName = "حدد وجهتك على الخريطة"
                                error = null
                            }
                        }
                    )
                    screen == "map" -> MapPickerScreen(
                        pickup = pickup,
                        destination = destination,
                        onDestination = {
                            destination = it
                            destinationName = "تم تحديد الوجهة"
                        },
                        onBack = { screen = "home" },
                        onConfirm = {
                            if (destination != null) screen = "home"
                        }
                    )
                    screen == "history" -> HistoryScreen(history, historyLoading, error, onRefresh = {
                        historyLoading = true
                        scope.launch {
                            repository.list(session.userId).onSuccess { history = it; historyLoaded = true }
                                .onFailure { error = it.message ?: "تعذر تحميل الرحلات" }
                            historyLoading = false
                        }
                    })
                    else -> HomeScreen(
                        session = session,
                        pickup = pickup,
                        destinationName = destinationName,
                        locationLoading = locationLoading,
                        error = error,
                        onLocation = {
                            val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (allowed) refreshLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        },
                        onMap = { screen = "map" },
                        onRequest = {
                            when {
                                pickup == null -> error = "حدد موقع الانطلاق أولًا."
                                destination == null -> screen = "map"
                                session.userId.orEmpty().isBlank() -> error = "بيانات الحساب غير مكتملة."
                                else -> showConfirm = true
                            }
                        }
                    )
                }
            }
        }
    }

    if (showConfirm) {
        ConfirmRideDialog(
            destination = destinationName,
            onDismiss = { showConfirm = false },
            onConfirm = {
                showConfirm = false
                loading = true
                error = null
                scope.launch {
                    repository.create(
                        TripRequest(
                            customerId = session.userId.orEmpty(),
                            pickup = pickup!!,
                            destination = destination!!,
                            vehicleType = "economy",
                            paymentMethod = "cash"
                        )
                    ).onSuccess { trip = it }
                        .onFailure { error = it.message ?: "تعذر إنشاء الرحلة" }
                    loading = false
                }
            }
        )
    }

    if (showProfile) {
        ProfileDialog(session, onDismiss = { showProfile = false }, onLogout = onLogout)
    }
}

@Composable
private fun HomeScreen(
    session: SessionStore,
    pickup: Coordinates?,
    destinationName: String,
    locationLoading: Boolean,
    error: String?,
    onLocation: () -> Unit,
    onMap: () -> Unit,
    onRequest: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("وصلها", color = TaxiGreen, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text(session.name?.takeIf { it.isNotBlank() }?.let { "أهلًا $it" } ?: "احجز مشوارك بسهولة", color = TaxiMuted, fontSize = 13.sp)
                }
                Box(Modifier.size(46.dp).background(TaxiSoft, CircleShape), Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, null, tint = TaxiGreen)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("إلى أين؟", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TaxiInk)
                    Text("حدد موقعك ووجهتك، ثم راجع الرحلة قبل الإرسال.", fontSize = 11.sp, color = TaxiMuted)
                    LocationRow("موقع الانطلاق", if (pickup == null) "جاري تحديد موقعك..." else "موقعك الحالي", Icons.Default.MyLocation, locationLoading, onLocation)
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = onMap).background(Color(0xFFF8FAF9), RoundedCornerShape(16.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD93838), modifier = Modifier.size(23.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الوجهة", fontSize = 10.sp, color = TaxiMuted)
                            Text(destinationName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (destinationName == "حدد وجهتك على الخريطة") TaxiMuted else TaxiInk)
                        }
                        Icon(Icons.Default.Search, null, tint = TaxiGreen)
                    }
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(TaxiSoft)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, null, tint = TaxiGreen)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("تاكسي اقتصادي", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TaxiInk)
                                Text("دفع نقدي", fontSize = 10.sp, color = TaxiMuted)
                            }
                            Text("اقتصادي", color = TaxiGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Button(onClick = onRequest, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)) {
                        Text("متابعة وطلب تاكسي", fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                    error?.let { Text(it, color = TaxiDanger, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFE6A500))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("وصلها تاكسي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TaxiInk)
                        Text("خدمة نقل ركاب داخل سوريا", fontSize = 10.sp, color = TaxiMuted)
                    }
                    Text("آمن • واضح", color = TaxiGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LocationRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, loading: Boolean, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(TaxiBg, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TaxiGreen, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 10.sp, color = TaxiMuted)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TaxiInk)
        }
        IconButton(onClick = onRefresh) {
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = TaxiGreen)
            else Icon(Icons.Default.Refresh, "تحديث", tint = TaxiGreen)
        }
    }
}

@Composable
private fun MapPickerScreen(
    pickup: Coordinates?,
    destination: Coordinates?,
    onDestination: (Coordinates) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Column(Modifier.weight(1f)) {
                Text("حدد وجهتك", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TaxiInk)
                Text(if (destination == null) "حرّك الخريطة واضغط لتحديد الوجهة" else "تم تحديد الوجهة", fontSize = 10.sp, color = TaxiMuted)
            }
        }
        Box(Modifier.weight(1f)) {
            if (pickup != null) {
                WaslhaRideMap(
                    pickup = pickup,
                    destination = destination,
                    modifier = Modifier.fillMaxSize(),
                    onDestinationPicked = onDestination
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("جارٍ تحديد موقع الانطلاق...", color = TaxiMuted) }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(16.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (destination == null) "اختر نقطة على الخريطة" else "الوجهة محددة", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TaxiInk)
                Text("سيتم حساب المسافة والوقت والأجرة عند تأكيد الرحلة.", fontSize = 10.sp, color = TaxiMuted)
                Button(enabled = destination != null, onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("تأكيد الوجهة", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun ConfirmRideDialog(destination: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تأكيد طلب الرحلة", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("الوجهة: $destination", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("الفئة: تاكسي اقتصادي", fontSize = 12.sp, color = TaxiMuted)
                Text("الدفع: نقدي", fontSize = 12.sp, color = TaxiMuted)
                Text("سيتم إرسال الطلب للكباتن المتاحين.", fontSize = 11.sp, color = TaxiMuted)
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("تأكيد الطلب") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("رجوع") } }
    )
}

@Composable
private fun ActiveTripScreen(
    trip: Trip,
    busy: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    onBack: () -> Unit
) {
    val terminal = trip.status == "completed" || trip.status == "cancelled"
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلتك", fontSize = 27.sp, fontWeight = FontWeight.Black, color = TaxiInk)
                Text(trip.statusLabel(), fontSize = 12.sp, color = TaxiGreen, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onRefresh, enabled = !busy) {
                if (busy) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = TaxiGreen) else Icon(Icons.Default.Refresh, "تحديث")
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(16.dp)) {
                TripRow("المسافة", "${trip.distanceKm} كم")
                TripRow("المدة", "${trip.durationMin} دقيقة")
                TripRow("الأجرة التقديرية", "${trip.estimatedFare} ${trip.currency}")
                TripRow("الدفع", "نقدي")
            }
        }
        if (trip.status == "driver_assigned" || trip.status == "arriving" || trip.status == "in_progress") {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(TaxiSoft)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).background(Color.White, CircleShape), Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = TaxiGreen) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("الكابتن في طريقه إليك", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TaxiInk)
                        Text("سنحدّث حالة الرحلة تلقائيًا", fontSize = 10.sp, color = TaxiMuted)
                    }
                    Icon(Icons.Default.AccessTime, null, tint = TaxiGreen)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (!terminal) {
            OutlinedButton(onClick = onCancel, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Close, null, tint = TaxiDanger)
                Spacer(Modifier.width(6.dp))
                Text("إلغاء الرحلة", color = TaxiDanger, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("العودة للرئيسية", fontWeight = FontWeight.Bold) }
        }
        error?.let { Text(it, color = TaxiDanger, fontSize = 11.sp) }
    }
}

@Composable
private fun TripRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(label, Modifier.weight(1f), fontSize = 11.sp, color = TaxiMuted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TaxiInk)
    }
}

@Composable
private fun HistoryScreen(trips: List<Trip>, loading: Boolean, error: String?, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلاتي", fontSize = 27.sp, fontWeight = FontWeight.Black, color = TaxiInk)
                Text("آخر رحلاتك على وصلها", fontSize = 11.sp, color = TaxiMuted)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث") }
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = TaxiGreen) }
            error != null && trips.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error, color = TaxiDanger, fontSize = 12.sp) }
            trips.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد رحلات سابقة بعد", color = TaxiMuted) }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(trips.take(30)) { trip ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(Color.White)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(42.dp).background(TaxiSoft, CircleShape), Alignment.Center) { Icon(Icons.Default.History, null, tint = TaxiGreen) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text(trip.statusLabel(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TaxiInk)
                                Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", fontSize = 10.sp, color = TaxiMuted)
                            }
                            Text("${trip.estimatedFare} ${trip.currency}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TaxiGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(selected: String, onHome: () -> Unit, onHistory: () -> Unit, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 26.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        BottomItem(Icons.Default.DirectionsCar, "الرئيسية", selected == "home", onHome)
        BottomItem(Icons.Default.History, "رحلاتي", selected == "history", onHistory)
        BottomItem(Icons.Default.Person, "حسابي", false, onProfile)
    }
}

@Composable
private fun BottomItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, active: Boolean, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = if (active) TaxiGreen else TaxiMuted, modifier = Modifier.size(22.dp))
        Text(text, color = if (active) TaxiGreen else TaxiMuted, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun ProfileDialog(session: SessionStore, onDismiss: () -> Unit, onLogout: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حسابي", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(session.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                session.email?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, color = TaxiMuted) }
                session.phone?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, color = TaxiMuted) }
                Text("طريقة الدفع الحالية: نقدي", fontSize = 12.sp, color = TaxiMuted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        dismissButton = { TextButton(onClick = onLogout) { Text("تسجيل الخروج", color = TaxiDanger) } }
    )
}
