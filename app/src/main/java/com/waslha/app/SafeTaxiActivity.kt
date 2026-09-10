package com.waslha.app

import android.Manifest
import android.content.Intent
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private val WaslhaGreen = Color(0xFF078A60)
private val WaslhaPurple = Color(0xFF6F4DBA)
private val WaslhaInk = Color(0xFF10201B)
private val WaslhaMuted = Color(0xFF6F7D78)
private val WaslhaBg = Color(0xFFF5F8F6)
private val WaslhaSoft = Color(0xFFE8F6F0)
private val WaslhaDanger = Color(0xFFB42318)

private data class SafeDestination(val title: String, val subtitle: String, val coordinates: Coordinates)
private data class SafeVehicle(val id: String, val title: String, val subtitle: String, val multiplier: Double)

private val SafeDestinations = listOf(
    SafeDestination("ساحة الأمويين", "دمشق", Coordinates(33.5138, 36.2765)),
    SafeDestination("جامعة دمشق", "المزة - دمشق", Coordinates(33.5101, 36.2766)),
    SafeDestination("سوق الحميدية", "المدينة القديمة", Coordinates(33.5112, 36.3051)),
    SafeDestination("المزة", "دمشق", Coordinates(33.4941, 36.2384)),
    SafeDestination("محطة الحجاز", "دمشق", Coordinates(33.5070, 36.2895))
)

private val SafeVehicles = listOf(
    SafeVehicle("economy", "اقتصادي", "سعر مناسب", 1.0),
    SafeVehicle("comfort", "مريح", "راحة ومساحة أفضل", 1.2),
    SafeVehicle("family", "عائلي", "مساحة أكبر للركاب", 1.35)
)

class SafeTaxiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val session = SessionStore(this)
        setContent {
            MaterialTheme {
                SafeTaxiApp(session) {
                    session.clear()
                    startActivity(Intent(this, AuthActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                    finish()
                }
            }
        }
    }
}

@Composable
private fun SafeTaxiApp(session: SessionStore, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TripRepository(ApiProvider.api) }
    val locator = remember { LocationProvider(context) }

    var tab by remember { mutableStateOf(0) }
    var pickup by remember { mutableStateOf<Coordinates?>(null) }
    var pickupText by remember { mutableStateOf("جاري تحديد موقعك…") }
    var destination by remember { mutableStateOf<SafeDestination?>(null) }
    var vehicle by remember { mutableStateOf(SafeVehicles.first()) }
    var fare by remember { mutableStateOf<FareEstimate?>(null) }
    var locating by remember { mutableStateOf(false) }
    var estimating by remember { mutableStateOf(false) }
    var requesting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var history by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(false) }
    var destinationDialog by remember { mutableStateOf(false) }
    var supportDialog by remember { mutableStateOf(false) }
    var profileDialog by remember { mutableStateOf(false) }

    fun refreshLocation() {
        locating = true
        scope.launch {
            val location = locator.lastKnown()
            if (location != null) {
                pickup = Coordinates(location.latitude, location.longitude)
                pickupText = "موقعك الحالي"
                error = null
            } else {
                pickupText = "تعذر تحديد الموقع"
                error = "شغّل GPS وتأكد من صلاحية الموقع"
            }
            locating = false
        }
    }

    val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) refreshLocation()
        else error = "صلاحية الموقع مطلوبة لطلب التاكسي"
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) refreshLocation()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(pickup, destination, vehicle.id) {
        val from = pickup ?: return@LaunchedEffect
        val to = destination?.coordinates ?: return@LaunchedEffect
        estimating = true
        repo.estimate(from, to, vehicle.id)
            .onSuccess { fare = it }
            .onFailure { fare = safeLocalEstimate(from, to, vehicle.multiplier) }
        estimating = false
    }

    LaunchedEffect(tab) {
        if (tab == 1) {
            val id = session.userId ?: return@LaunchedEffect
            historyLoading = true
            repo.list(id).onSuccess { history = it }.onFailure { error = it.message ?: "تعذر جلب الرحلات" }
            historyLoading = false
        }
    }

    LaunchedEffect(trip?.id) {
        val id = trip?.id ?: return@LaunchedEffect
        while (isActive) {
            delay(5000)
            repo.get(id).onSuccess { updated -> trip = updated }
        }
    }

    Surface(Modifier.fillMaxSize(), color = WaslhaBg) {
        when {
            trip != null -> SafeActiveTrip(trip!!, requesting, error,
                onRefresh = {
                    requesting = true
                    scope.launch {
                        repo.get(trip!!.id).onSuccess { trip = it }.onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                        requesting = false
                    }
                },
                onCancel = {
                    requesting = true
                    scope.launch {
                        repo.cancel(trip!!.id, "إلغاء من الراكب").onSuccess { trip = it }.onFailure { error = it.message ?: "تعذر إلغاء الرحلة" }
                        requesting = false
                    }
                },
                onDone = { trip = null; tab = 1; error = null }
            )
            else -> Scaffold(
                containerColor = WaslhaBg,
                bottomBar = {
                    NavigationBar(containerColor = Color.White, modifier = Modifier.navigationBarsPadding()) {
                        NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("الرئيسية") })
                        NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("رحلاتي") })
                        NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("حسابي") })
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (tab) {
                        0 -> SafeHome(
                            session = session,
                            pickupText = pickupText,
                            destination = destination,
                            selectedVehicle = vehicle,
                            fare = fare,
                            estimating = estimating,
                            locating = locating,
                            requesting = requesting,
                            error = error,
                            onRefreshLocation = { if (permissionGranted) refreshLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                            onDestination = { destinationDialog = true },
                            onVehicle = { vehicle = it },
                            onRequest = {
                                val from = pickup
                                val to = destination
                                val id = session.userId
                                when {
                                    from == null -> error = "حدد موقع الانطلاق أولًا"
                                    to == null -> destinationDialog = true
                                    id.isNullOrBlank() -> error = "بيانات الحساب غير مكتملة، سجّل الدخول من جديد"
                                    else -> {
                                        requesting = true
                                        error = null
                                        scope.launch {
                                            repo.create(TripRequest(id, from, to.coordinates, vehicle.id, "cash"))
                                                .onSuccess { trip = it }
                                                .onFailure { error = it.message ?: "تعذر إنشاء طلب التاكسي" }
                                            requesting = false
                                        }
                                    }
                                }
                            },
                            onSupport = { supportDialog = true }
                        )
                        1 -> SafeTrips(history, historyLoading, error) { }
                        else -> SafeProfile(session, onSupport = { supportDialog = true }, onSettings = { profileDialog = true }, onLogout = onLogout)
                    }
                }
            }
        }
    }

    if (destinationDialog) {
        SafeDestinationDialog(
            onDismiss = { destinationDialog = false },
            onSelect = { destination = it; destinationDialog = false }
        )
    }
    if (supportDialog) SafeInfoDialog("المساعدة والدعم", "حدد موقعك ثم الوجهة واختر نوع التكسي واضغط طلب التكسي.") { supportDialog = false }
    if (profileDialog) SafeInfoDialog("الإعدادات", "إعدادات الحساب والإشعارات والدفع ستكون متاحة من هنا في المرحلة التالية.") { profileDialog = false }
}

@Composable
private fun SafeHome(session: SessionStore, pickupText: String, destination: SafeDestination?, selectedVehicle: SafeVehicle, fare: FareEstimate?, estimating: Boolean, locating: Boolean, requesting: Boolean, error: String?, onRefreshLocation: () -> Unit, onDestination: () -> Unit, onVehicle: (SafeVehicle) -> Unit, onRequest: () -> Unit, onSupport: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 22.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("وصلها", color = WaslhaGreen, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(session.name?.takeIf { it.isNotBlank() }?.let { "أهلًا $it" } ?: "احجز مشوارك بسهولة", color = WaslhaMuted, fontSize = 13.sp)
                }
                IconButton(onClick = onSupport) { Icon(Icons.Default.HelpOutline, "المساعدة", tint = WaslhaGreen) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                SafeQuick("موقعي", Icons.Default.MyLocation, Modifier.weight(1f), onRefreshLocation)
                SafeQuick("الوجهة", Icons.Default.LocationOn, Modifier.weight(1f), onDestination)
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("إلى أين؟", color = WaslhaInk, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("اختَر موقع الانطلاق والوجهة ثم اطلب السيارة", color = WaslhaMuted, fontSize = 11.sp)
                    SafeLocation("نقطة الانطلاق", pickupText, Icons.Default.MyLocation, onRefreshLocation, locating)
                    SafeLocation("الوجهة", destination?.title ?: "اختيار الوجهة", Icons.Default.LocationOn, onDestination, false)
                    Text("نوع التكسي", color = WaslhaInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    SafeVehicles.forEach { SafeVehicleRow(it, it.id == selectedVehicle.id) { onVehicle(it) } }
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(WalshaSoftCompat), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, null, tint = WaslhaGreen)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("الدفع نقدًا", color = WaslhaInk, fontWeight = FontWeight.Bold)
                                Text(if (estimating) "نحسب الأجرة…" else fare?.let { "${it.estimatedFare} ${it.currency} • ${it.distanceKm} كم • ${it.durationMin} دقيقة" } ?: "اختر الوجهة لحساب الأجرة"), color = WaslhaMuted, fontSize = 11.sp)
                            }
                        }
                    }
                    if (!error.isNullOrBlank()) Text(error, color = WaslhaDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onRequest, enabled = !requesting, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = WaslhaPurple)) {
                        if (requesting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("طلب التكسي", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

private val WalshaSoftCompat = WaslhaSoft

@Composable private fun SafeQuick(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable { onClick() }, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(13.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = WaslhaGreen); Spacer(Modifier.width(7.dp)); Text(title, color = WaslhaInk, fontWeight = FontWeight.Bold, fontSize = 12.sp) } }
}

@Composable private fun SafeLocation(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, loading: Boolean) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(WalshaSoftCompat), shape = RoundedCornerShape(17.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = WaslhaGreen, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = WaslhaMuted, fontSize = 10.sp); Text(value, color = WaslhaInk, fontWeight = FontWeight.Bold, fontSize = 14.sp) }; if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = WaslhaGreen, strokeWidth = 2.dp) else Icon(Icons.Default.ArrowBack, null, tint = WaslhaMuted) } }
}

@Composable private fun SafeVehicleRow(vehicle: SafeVehicle, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(if (selected) WalshaSoftCompat else Color.White), shape = RoundedCornerShape(17.dp)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Color(0xFFF0F4F2), CircleShape), Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = WaslhaGreen) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(vehicle.title, color = WaslhaInk, fontWeight = FontWeight.Black); Text(vehicle.subtitle, color = WaslhaMuted, fontSize = 10.sp) }; if (selected) Text("✓", color = WaslhaGreen, fontSize = 21.sp, fontWeight = FontWeight.Black) } }
}

@Composable private fun SafeDestinationDialog(onDismiss: () -> Unit, onSelect: (SafeDestination) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("اختيار الوجهة", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { SafeDestinations.forEach { d -> Card(Modifier.fillMaxWidth().clickable { onSelect(d) }, colors = CardDefaults.cardColors(WalshaBg), shape = RoundedCornerShape(14.dp)) { Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = WaslhaGreen); Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text(d.title, color = WaslhaInk, fontWeight = FontWeight.Bold); Text(d.subtitle, color = WaslhaMuted, fontSize = 10.sp) } } } } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = WaslhaGreen) } })
}

@Composable private fun SafeTrips(history: List<Trip>, loading: Boolean, error: String?, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("رحلاتي", color = WaslhaInk, fontSize = 29.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f)); IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث", tint = WaslhaGreen) } }
        Spacer(Modifier.height(10.dp))
        if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = WaslhaGreen) }
        else if (history.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.History, null, tint = WaslhaMuted, modifier = Modifier.size(54.dp)); Spacer(Modifier.height(8.dp)); Text("لا توجد رحلات بعد", color = WaslhaInk, fontWeight = FontWeight.Bold) } }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(history) { t -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(t.statusLabel(), color = WaslhaGreen, fontWeight = FontWeight.Black); Text("${t.distanceKm} كم • ${t.durationMin} دقيقة", color = WaslhaMuted, fontSize = 10.sp) }; Text("${t.estimatedFare} ${t.currency}", color = WaslhaInk, fontWeight = FontWeight.Black) } } } }
        error?.let { Text(it, color = WaslhaDanger, fontSize = 11.sp) }
    }
}

@Composable private fun SafeProfile(session: SessionStore, onSupport: () -> Unit, onSettings: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text("حسابي", color = WaslhaInk, fontSize = 29.sp, fontWeight = FontWeight.Black)
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(23.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(55.dp).background(WalshaSoftCompat, CircleShape), Alignment.Center) { Icon(Icons.Default.Person, null, tint = WaslhaGreen, modifier = Modifier.size(32.dp)) }; Spacer(Modifier.width(11.dp)); Column { Text(session.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", color = WaslhaInk, fontSize = 19.sp, fontWeight = FontWeight.Black); Text(session.email ?: session.phone ?: "بيانات الحساب", color = WaslhaMuted, fontSize = 11.sp) } } }
        OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Settings, null); Spacer(Modifier.width(7.dp)); Text("الإعدادات") }
        OutlinedButton(onClick = onSupport, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.HelpOutline, null); Spacer(Modifier.width(7.dp)); Text("المساعدة والدعم") }
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Logout, null, tint = WaslhaDanger); Spacer(Modifier.width(7.dp)); Text("تسجيل الخروج", color = WaslhaDanger) }
    }
}

@Composable private fun SafeInfoDialog(title: String, body: String, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title, fontWeight = FontWeight.Black) }, text = { Text(body, color = WaslhaMuted) }, confirmButton = { TextButton(onClick = onDismiss) { Text("حسنًا", color = WaslhaGreen) } })
}

@Composable private fun SafeActiveTrip(trip: Trip, loading: Boolean, error: String?, onRefresh: () -> Unit, onCancel: () -> Unit, onDone: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onDone) { Icon(Icons.Default.ArrowBack, "رجوع") }; Spacer(Modifier.weight(1f)); Text("الرحلة الحالية", color = WaslhaInk, fontSize = 23.sp, fontWeight = FontWeight.Black) }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(23.dp)) { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text(trip.statusLabel(), color = if (trip.status == "cancelled") WaslhaDanger else WaslhaGreen, fontSize = 20.sp, fontWeight = FontWeight.Black); Text("${trip.estimatedFare} ${trip.currency}", color = WaslhaInk, fontWeight = FontWeight.Black); Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = WaslhaMuted); if (!error.isNullOrBlank()) Text(error, color = WaslhaDanger, fontSize = 11.sp); OutlinedButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.fillMaxWidth()) { if (loading) CircularProgressIndicator(Modifier.size(17.dp), color = WaslhaGreen, strokeWidth = 2.dp) else Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(5.dp)); Text("تحديث") }; if (trip.status != "completed" && trip.status != "cancelled") OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("إلغاء الرحلة", color = WaslhaDanger) } else Button(onClick = onDone, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = WaslhaGreen)) { Text("العودة إلى رحلاتي", fontWeight = FontWeight.Black) } } }
    }
}

private fun safeLocalEstimate(from: Coordinates, to: Coordinates, multiplier: Double): FareEstimate {
    val dx = (to.lng - from.lng) * 85.0
    val dy = (to.lat - from.lat) * 111.0
    val km = sqrt(dx * dx + dy * dy).coerceAtLeast(0.2)
    return FareEstimate(km, (km * 3.0).toInt().coerceAtLeast(3), "ل.س", (2500 + km * 1200 * multiplier).toInt())
}
