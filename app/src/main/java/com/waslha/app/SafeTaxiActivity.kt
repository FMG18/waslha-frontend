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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private val Green = Color(0xFF078A60)
private val Ink = Color(0xFF10201B)
private val Muted = Color(0xFF72807B)
private val AppBg = Color(0xFFF5F8F6)
private val Danger = Color(0xFFB42318)
private val GreenSoft = Color(0xFFE8F6F0)
private val RedSoft = Color(0xFFFCECEC)

private val DamascusCenter = Coordinates(33.5138, 36.2765)

private data class TaxiDestination(
    val title: String,
    val subtitle: String,
    val coordinates: Coordinates
)

private data class TaxiVehicle(
    val id: String,
    val title: String,
    val subtitle: String,
    val multiplier: Double
)

private val destinations = listOf(
    TaxiDestination("ساحة الأمويين", "دمشق", Coordinates(33.5138, 36.2765)),
    TaxiDestination("جامعة دمشق", "المزة - دمشق", Coordinates(33.5101, 36.2766)),
    TaxiDestination("سوق الحميدية", "المدينة القديمة", Coordinates(33.5112, 36.3051)),
    TaxiDestination("المزة", "دمشق", Coordinates(33.4941, 36.2384)),
    TaxiDestination("محطة الحجاز", "دمشق", Coordinates(33.5070, 36.2895))
)

private val vehicles = listOf(
    TaxiVehicle("economy", "اقتصادي", "سيارة مريحة وسعر مناسب", 1.0),
    TaxiVehicle("comfort", "مريح", "سيارة أحدث ومساحة أفضل", 1.2),
    TaxiVehicle("family", "عائلي", "مساحة أكبر للركاب", 1.35)
)

class SafeTaxiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val sessionStore = SessionStore(this)
        val locationProvider = LocationProvider(this)

        setContent {
            TaxiExperience(
                sessionStore = sessionStore,
                locationProvider = locationProvider,
                onLogout = {
                    sessionStore.clear()
                    startActivity(Intent(this, AuthActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                    finish()
                }
            )
        }
    }
}

@Composable
private fun TaxiExperience(
    sessionStore: SessionStore,
    locationProvider: LocationProvider,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { TripRepository(ApiProvider.api) }
    val scope = rememberCoroutineScope()

    var tab by remember { mutableStateOf(0) }
    var pickup by remember { mutableStateOf<Coordinates?>(null) }
    var pickupLabel by remember { mutableStateOf("جاري تحديد موقعك...") }
    var destination by remember { mutableStateOf<TaxiDestination?>(null) }
    var selectedVehicle by remember { mutableStateOf(vehicles.first()) }
    var fareEstimate by remember { mutableStateOf<FareEstimate?>(null) }
    var estimateLoading by remember { mutableStateOf(false) }
    var destinationDialog by remember { mutableStateOf(false) }
    var mapPicker by remember { mutableStateOf(false) }
    var supportDialog by remember { mutableStateOf(false) }
    var profileDialog by remember { mutableStateOf(false) }
    var locationLoading by remember { mutableStateOf(false) }
    var requesting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var history by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(false) }

    fun refreshLocation() {
        locationLoading = true
        scope.launch {
            val found = locationProvider.lastKnown()
            if (found != null) {
                pickup = Coordinates(found.latitude, found.longitude)
                pickupLabel = "موقعك الحالي"
                error = null
            } else {
                pickupLabel = "تعذر تحديد الموقع"
                error = "شغّل GPS وتأكد من منح وصلها صلاحية الموقع"
            }
            locationLoading = false
        }
    }

    val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            refreshLocation()
        } else {
            error = "صلاحية الموقع مطلوبة لطلب التاكسي"
        }
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) refreshLocation()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(pickup, destination, selectedVehicle.id) {
        val from = pickup ?: return@LaunchedEffect
        val to = destination?.coordinates ?: return@LaunchedEffect
        estimateLoading = true
        repository.estimate(from, to, selectedVehicle.id)
            .onSuccess { fareEstimate = it }
            .onFailure { fareEstimate = localEstimate(from, to, selectedVehicle.multiplier) }
        estimateLoading = false
    }

    LaunchedEffect(tab) {
        if (tab == 1 && sessionStore.userId?.isNotBlank() == true) {
            historyLoading = true
            repository.list(sessionStore.userId)
                .onSuccess { history = it }
                .onFailure { error = it.message ?: "تعذر جلب الرحلات" }
            historyLoading = false
        }
    }

    LaunchedEffect(trip?.id) {
        val activeId = trip?.id ?: return@LaunchedEffect
        while (isActive) {
            delay(5000)
            repository.get(activeId).onSuccess { updated ->
                trip = updated
                if (updated.status == "completed" || updated.status == "cancelled") return@onSuccess
            }
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = AppBg) {
            if (mapPicker) {
                MapPickerScreen(
                    pickup = pickup ?: DamascusCenter,
                    destination = destination?.coordinates,
                    onBack = { mapPicker = false },
                    onPicked = { coordinates ->
                        destination = TaxiDestination("الموقع المحدد", "تم اختياره من الخريطة", coordinates)
                        mapPicker = false
                    }
                )
            } else if (trip != null) {
                ActiveTripScreen(
                    trip = trip!!,
                    loading = requesting,
                    error = error,
                    onRefresh = {
                        requesting = true
                        scope.launch {
                            repository.get(trip!!.id).onSuccess { trip = it }.onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                            requesting = false
                        }
                    },
                    onCancel = {
                        requesting = true
                        scope.launch {
                            repository.cancel(trip!!.id, "إلغاء من الراكب")
                                .onSuccess { trip = it }
                                .onFailure { error = it.message ?: "تعذر إلغاء الرحلة" }
                            requesting = false
                        }
                    },
                    onDone = {
                        trip = null
                        tab = 1
                        error = null
                    }
                )
            } else {
                Scaffold(
                    containerColor = AppBg,
                    bottomBar = {
                        NavigationBar(containerColor = Color.White, modifier = Modifier.navigationBarsPadding()) {
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                icon = { Icon(Icons.Default.DirectionsCar, null) },
                                label = { Text("الرئيسية") }
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                icon = { Icon(Icons.Default.History, null) },
                                label = { Text("رحلاتي") }
                            )
                            NavigationBarItem(
                                selected = tab == 2,
                                onClick = { tab = 2 },
                                icon = { Icon(Icons.Default.Person, null) },
                                label = { Text("حسابي") }
                            )
                        }
                    }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (tab) {
                            0 -> HomeScreen(
                                sessionStore = sessionStore,
                                pickupLabel = pickupLabel,
                                destination = destination,
                                selectedVehicle = selectedVehicle,
                                fareEstimate = fareEstimate,
                                estimateLoading = estimateLoading,
                                locationLoading = locationLoading,
                                requesting = requesting,
                                error = error,
                                onRefreshLocation = {
                                    if (permissionGranted) refreshLocation()
                                    else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                },
                                onDestination = { destinationDialog = true },
                                onMap = { mapPicker = true },
                                onVehicle = { vehicle -> selectedVehicle = vehicle },
                                onRequest = {
                                    val from = pickup
                                    val to = destination
                                    if (from == null) {
                                        error = "حدد موقع الانطلاق أولًا"
                                        return@HomeScreen
                                    }
                                    if (to == null) {
                                        destinationDialog = true
                                        return@HomeScreen
                                    }
                                    val userId = sessionStore.userId
                                    if (userId.isNullOrBlank()) {
                                        error = "بيانات الحساب غير مكتملة، سجّل الدخول من جديد"
                                        return@HomeScreen
                                    }
                                    requesting = true
                                    error = null
                                    scope.launch {
                                        repository.create(
                                            TripRequest(
                                                customerId = userId,
                                                pickup = from,
                                                destination = to.coordinates,
                                                vehicleType = selectedVehicle.id,
                                                paymentMethod = "cash"
                                            )
                                        ).onSuccess { trip = it }
                                            .onFailure { error = it.message ?: "تعذر إنشاء طلب التاكسي" }
                                        requesting = false
                                    }
                                },
                                onSupport = { supportDialog = true }
                            )
                            1 -> TripsScreen(
                                history = history,
                                loading = historyLoading,
                                error = error,
                                onRefresh = {
                                    if (sessionStore.userId.isNullOrBlank()) return@TripsScreen
                                    historyLoading = true
                                    scope.launch {
                                        repository.list(sessionStore.userId)
                                            .onSuccess { history = it }
                                            .onFailure { error = it.message ?: "تعذر جلب الرحلات" }
                                        historyLoading = false
                                    }
                                }
                            )
                            else -> ProfileScreen(
                                sessionStore = sessionStore,
                                onSupport = { supportDialog = true },
                                onLogout = onLogout
                            )
                        }
                    }
                }
            }
        }
    }

    if (destinationDialog) {
        DestinationDialog(
            onDismiss = { destinationDialog = false },
            onMap = { destinationDialog = false; mapPicker = true },
            onSelect = { destination = it; destinationDialog = false }
        )
    }

    if (supportDialog) {
        SupportDialog(onDismiss = { supportDialog = false })
    }
}

@Composable
private fun HomeScreen(
    sessionStore: SessionStore,
    pickupLabel: String,
    destination: TaxiDestination?,
    selectedVehicle: TaxiVehicle,
    fareEstimate: FareEstimate?,
    estimateLoading: Boolean,
    locationLoading: Boolean,
    requesting: Boolean,
    error: String?,
    onRefreshLocation: () -> Unit,
    onDestination: () -> Unit,
    onMap: () -> Unit,
    onVehicle: (TaxiVehicle) -> Unit,
    onRequest: () -> Unit,
    onSupport: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 22.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("وصلها", color = Green, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(
                        sessionStore.name?.takeIf { it.isNotBlank() }?.let { "أهلًا $it" } ?: "احجز مشوارك بسهولة",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
                IconButton(onClick = onSupport) {
                    Icon(Icons.Default.HelpOutline, "المساعدة", tint = Green)
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("إلى أين؟", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text("حدّد الانطلاق والوجهة ثم اختر السيارة المناسبة", fontSize = 11.sp, color = Muted)

                    Row(Modifier.fillMaxWidth().background(AppBg, RoundedCornerShape(17.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, null, tint = Green, modifier = Modifier.size(23.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("نقطة الانطلاق", fontSize = 10.sp, color = Muted)
                            Text(pickupLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                        }
                        IconButton(onClick = onRefreshLocation) {
                            if (locationLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Green)
                            else Icon(Icons.Default.Refresh, "تحديث الموقع", tint = Green)
                        }
                    }

                    Row(Modifier.fillMaxWidth().background(Color(0xFFF9FAFA), RoundedCornerShape(17.dp)).clickable(onClick = onDestination).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD93838), modifier = Modifier.size(23.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الوجهة", fontSize = 10.sp, color = Muted)
                            Text(destination?.title ?: "اختر وجهتك", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = destination?.let { Ink } ?: Muted)
                            destination?.subtitle?.let { Text(it, fontSize = 9.sp, color = Muted) }
                        }
                        Icon(Icons.Default.Search, "بحث عن وجهة", tint = Green)
                    }

                    OutlinedButton(onClick = onMap, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(15.dp)) {
                        Icon(Icons.Default.LocationOn, null)
                        Spacer(Modifier.width(8.dp))
                        Text("اختيار الوجهة من الخريطة")
                    }

                    Text("نوع التاكسي", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Ink)
                    vehicles.forEach { vehicle ->
                        VehicleCard(vehicle, selected = vehicle.id == selectedVehicle.id, onClick = { onVehicle(vehicle) })
                    }

                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(GreenSoft)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("الدفع", fontSize = 10.sp, color = Muted)
                                Text("نقدًا للكابتن", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                            }
                            Text("نقدي", color = Green, fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }

                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color(0xFFFAFCFB))) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (estimateLoading) {
                                CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp, color = Green)
                                Spacer(Modifier.width(12.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text("التقدير قبل الطلب", fontSize = 10.sp, color = Muted)
                                if (fareEstimate != null) {
                                    Text("${fareEstimate.estimatedFare} ${fareEstimate.currency}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Green)
                                    Text("${fareEstimate.distanceKm} كم • ${fareEstimate.durationMin} دقيقة تقريبًا", fontSize = 10.sp, color = Muted)
                                } else {
                                    Text("اختر الوجهة لحساب الأجرة", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Muted)
                                }
                            }
                            Icon(Icons.Default.AccessTime, null, tint = Green)
                        }
                    }

                    Button(enabled = !requesting, onClick = onRequest, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)) {
                        if (requesting) CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp)
                        else Text("تأكيد وطلب التاكسي", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                    error?.let { Text(it, color = Danger, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFE6A500))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("وصلها تاكسي", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text("نقل ركاب داخل سوريا", fontSize = 10.sp, color = Muted)
                    }
                    Text("آمن • واضح • سريع", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(vehicle: TaxiVehicle, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(if (selected) GreenSoft else Color.White),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Green) else null
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(if (selected) Color.White else AppBg, CircleShape), Alignment.Center) {
                Icon(Icons.Default.DirectionsCar, null, tint = Green)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(vehicle.title, fontWeight = FontWeight.Black, color = Ink, fontSize = 13.sp)
                Text(vehicle.subtitle, color = Muted, fontSize = 10.sp)
            }
            if (selected) Icon(Icons.Default.CheckCircle, null, tint = Green)
        }
    }
}

@Composable
private fun DestinationDialog(
    onDismiss: () -> Unit,
    onMap: () -> Unit,
    onSelect: (TaxiDestination) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار الوجهة", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMap, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.LocationOn, null)
                    Spacer(Modifier.width(7.dp))
                    Text("اختيار من الخريطة")
                }
                destinations.forEach { item ->
                    Card(Modifier.fillMaxWidth().clickable { onSelect(item) }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Color.White)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).background(GreenSoft, CircleShape), Alignment.Center) {
                                Icon(Icons.Default.LocationOn, null, tint = Green)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text(item.subtitle, fontSize = 10.sp, color = Muted)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}

@Composable
private fun MapPickerScreen(
    pickup: Coordinates,
    destination: Coordinates?,
    onBack: () -> Unit,
    onPicked: (Coordinates) -> Unit
) {
    var pending by remember(destination) { mutableStateOf(destination) }

    Scaffold(
        containerColor = AppBg,
        topBar = {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Ink) }
                Column(Modifier.weight(1f)) {
                    Text("اختيار الوجهة", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text("حرّك الخريطة واجعل المؤشر فوق وجهتك", fontSize = 10.sp, color = Muted)
                }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (pending == null) "لم تحدد الوجهة بعد" else "الوجهة جاهزة للتأكيد",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (pending == null) Muted else Green
                )
                Button(enabled = pending != null, onClick = { pending?.let(onPicked) }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp)) {
                    Text("تأكيد هذه الوجهة", fontWeight = FontWeight.Black)
                }
            }
        }
    ) { padding ->
        WaslhaRideMap(
            pickup = pickup,
            destination = pending,
            modifier = Modifier.fillMaxSize().padding(padding),
            onDestinationPicked = { pending = it }
        )
    }
}

@Composable
private fun ActiveTripScreen(
    trip: Trip,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit
) {
    val terminal = trip.status == "completed" || trip.status == "cancelled"
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("رحلتك الحالية", fontSize = 27.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text(trip.statusLabel(), color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onRefresh) {
                    if (loading) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = Green)
                    else Icon(Icons.Default.Refresh, "تحديث", tint = Green)
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(18.dp)) {
                    StatusStep("تم إرسال الطلب", true)
                    StatusStep("جاري البحث عن الكابتن", trip.status != "searching")
                    StatusStep("الكابتن متجه إليك", trip.status == "arriving" || trip.status == "in_progress" || terminal)
                    StatusStep("الرحلة بدأت", trip.status == "in_progress" || trip.status == "completed")
                    StatusStep("انتهت الرحلة", trip.status == "completed")
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(GreenSoft)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("تفاصيل الرحلة", fontSize = 11.sp, color = Muted)
                    DetailRow("المسافة", "${trip.distanceKm} كم")
                    DetailRow("الوقت", "${trip.durationMin} دقيقة تقريبًا")
                    DetailRow("الأجرة", "${trip.estimatedFare} ${trip.currency}")
                    DetailRow("الدفع", if (trip.paymentMethod == "cash") "نقدي" else trip.paymentMethod)
                    DetailRow("رقم الرحلة", trip.id)
                }
            }
        }

        trip.driver?.let { driver ->
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("الكابتن", fontSize = 10.sp, color = Muted)
                        Text(driver.name, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text("${driver.vehicle} • ${driver.plate}", fontSize = 12.sp, color = Muted)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFE6A500), modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(driver.rating.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            if (!terminal) {
                OutlinedButton(onClick = onCancel, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Icon(Icons.Default.Close, null, tint = Danger)
                    Spacer(Modifier.width(6.dp))
                    Text("إلغاء الرحلة", color = Danger, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("العودة إلى الرحلات", fontWeight = FontWeight.Bold)
                }
            }
        }

        error?.let { message ->
            item { Text(message, color = Danger, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun StatusStep(label: String, active: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(if (active) Green else Color(0xFFD9E1DE), CircleShape))
        Spacer(Modifier.width(10.dp))
        Text(label, color = if (active) Ink else Muted, fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, Modifier.weight(1f), color = Muted, fontSize = 11.sp)
        Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun TripsScreen(
    history: List<Trip>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلاتي", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                Text("سجل مشاويرك السابقة", color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = onRefresh) {
                if (loading) CircularProgressIndicator(Modifier.size(19.dp), strokeWidth = 2.dp, color = Green)
                else Icon(Icons.Default.Refresh, "تحديث", tint = Green)
            }
        }
        Spacer(Modifier.height(12.dp))
        when {
            error != null -> Text(error, color = Danger, fontSize = 12.sp)
            history.isEmpty() && !loading -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(21.dp)) {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Green, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("لا توجد رحلات بعد", fontWeight = FontWeight.Black, color = Ink)
                    Text("أول مشوار لك سيظهر هنا", color = Muted, fontSize = 11.sp)
                }
            }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                if (loading) item { Box(Modifier.fillMaxWidth().padding(25.dp), Alignment.Center) { CircularProgressIndicator(color = Green) } }
                items(history.take(30)) { trip ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(Color.White)) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, null, tint = Green)
                                Spacer(Modifier.width(9.dp))
                                Text(trip.statusLabel(), fontWeight = FontWeight.Black, color = Ink, modifier = Modifier.weight(1f))
                                Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة • ${trip.vehicleType}", color = Muted, fontSize = 10.sp)
                            Text("رقم الرحلة: ${trip.id}", color = Muted, fontSize = 9.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    sessionStore: SessionStore,
    onSupport: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 22.dp)
    ) {
        item {
            Text("حسابي", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
            Text("إدارة حسابك وبياناتك", color = Muted, fontSize = 12.sp)
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(Color.White)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(60.dp).background(GreenSoft, CircleShape), Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = Green, modifier = Modifier.size(30.dp))
                    }
                    Text(sessionStore.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Ink)
                    sessionStore.email?.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted, fontSize = 12.sp) }
                    sessionStore.phone?.takeIf { it.isNotBlank() }?.let { Text(it, color = Muted, fontSize = 12.sp) }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(Color.White)) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    ProfileAction(Icons.Default.HelpOutline, "المساعدة والدعم", onSupport)
                    ProfileAction(Icons.Default.Settings, "الإعدادات", onSupport)
                    ProfileAction(Icons.Default.Logout, "تسجيل الخروج", onLogout, danger = true)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(GreenSoft)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFE6A500))
                    Spacer(Modifier.width(9.dp))
                    Column {
                        Text("وصلها", fontWeight = FontWeight.Black, color = Ink)
                        Text("خدمة تاكسي داخل سوريا", fontSize = 10.sp, color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (danger) Danger else Green, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = if (danger) Danger else Ink, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SupportDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("المساعدة والدعم", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("كيف أطلب تاكسي؟", fontWeight = FontWeight.Bold, color = Ink)
                Text("حدد موقع الانطلاق والوجهة، اختر نوع السيارة، ثم اضغط تأكيد وطلب التاكسي.", color = Muted, fontSize = 12.sp)
                Text("كيف ألغي الرحلة؟", fontWeight = FontWeight.Bold, color = Ink)
                Text("يمكنك إلغاء الرحلة من شاشة الرحلة الحالية قبل انتهائها.", color = Muted, fontSize = 12.sp)
                Text("الدفع", fontWeight = FontWeight.Bold, color = Ink)
                Text("الدفع الحالي نقدًا للكابتن بعد الرحلة.", color = Muted, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("حسنًا", color = Green) } }
    )
}

private fun localEstimate(from: Coordinates, to: Coordinates, multiplier: Double): FareEstimate {
    val distance = haversineKm(from, to).coerceIn(1.2, 100.0)
    val duration = (distance * 3.4 + 4).toInt().coerceAtLeast(4)
    val rawFare = (3500 + distance * 900) * multiplier
    val fare = ((rawFare / 250).toInt()) * 250
    return FareEstimate(distance, duration, "SYP", fare)
}

private fun haversineKm(a: Coordinates, b: Coordinates): Double {
    val radius = 6371.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val h = sin(dLat / 2).pow(2) + cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLng / 2).pow(2)
    return radius * 2 * atan2(sqrt(h), sqrt(1 - h))
}
