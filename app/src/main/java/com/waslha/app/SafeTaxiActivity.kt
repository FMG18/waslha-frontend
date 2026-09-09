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
import androidx.compose.material.icons.filled.Logout
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
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Green = Color(0xFF078A60)
private val Ink = Color(0xFF10201B)
private val Muted = Color(0xFF72807B)
private val AppBg = Color(0xFFF5F8F6)
private val Danger = Color(0xFFB42318)
private val GreenSoft = Color(0xFFE8F6F0)

private data class TaxiDestination(val title: String, val subtitle: String, val coordinates: Coordinates)

private val destinations = listOf(
    TaxiDestination("ساحة الأمويين", "دمشق", Coordinates(33.5138, 36.2765)),
    TaxiDestination("جامعة دمشق", "المزة - دمشق", Coordinates(33.5101, 36.2766)),
    TaxiDestination("سوق الحميدية", "المدينة القديمة", Coordinates(33.5112, 36.3051)),
    TaxiDestination("المزة", "دمشق", Coordinates(33.4941, 36.2384)),
    TaxiDestination("محطة الحجاز", "دمشق", Coordinates(33.5070, 36.2895))
)

class SafeTaxiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val sessionStore = SessionStore(this)
        val locationProvider = LocationProvider(this)
        setContent {
            TaxiApp(
                sessionStore = sessionStore,
                locationProvider = locationProvider,
                onLogout = {
                    sessionStore.clear()
                    finish()
                }
            )
        }
    }
}

@Composable
private fun TaxiApp(
    sessionStore: SessionStore,
    locationProvider: LocationProvider,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { TripRepository(ApiProvider.api) }
    val scope = rememberCoroutineScope()

    var pickup by remember { mutableStateOf<Coordinates?>(null) }
    var locationLabel by remember { mutableStateOf("جاري تحديد موقعك...") }
    var destination by remember { mutableStateOf<TaxiDestination?>(null) }
    var destinationDialog by remember { mutableStateOf(false) }
    var profileDialog by remember { mutableStateOf(false) }
    var historyDialog by remember { mutableStateOf(false) }
    var locationLoading by remember { mutableStateOf(false) }
    var requesting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var refreshTick by remember { mutableStateOf(0) }

    fun refreshLocation() {
        locationLoading = true
        scope.launch {
            val found = locationProvider.lastKnown()
            if (found != null) {
                pickup = Coordinates(found.latitude, found.longitude)
                locationLabel = "موقعك الحالي"
                error = null
            } else {
                locationLabel = "تعذر تحديد الموقع"
                error = "تأكد من تشغيل GPS ومنح التطبيق صلاحية الموقع"
            }
            locationLoading = false
        }
    }

    val hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) refreshLocation() else error = "صلاحية الموقع مطلوبة لتحديد نقطة الانطلاق"
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) refreshLocation()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(trip?.id, refreshTick) {
        val active = trip ?: return@LaunchedEffect
        while (active.status != "completed" && active.status != "cancelled") {
            delay(5000)
            repository.get(active.id).onSuccess { updated -> trip = updated }
            break
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = AppBg) {
            Scaffold(
                containerColor = AppBg,
                bottomBar = {
                    if (trip == null) {
                        BottomNav(
                            onHome = { error = null },
                            onHistory = { historyDialog = true },
                            onProfile = { profileDialog = true }
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    if (trip == null) {
                        TaxiHome(
                            sessionStore = sessionStore,
                            pickup = pickup,
                            locationLabel = locationLabel,
                            destination = destination,
                            locationLoading = locationLoading,
                            requesting = requesting,
                            error = error,
                            onRefreshLocation = {
                                if (hasLocationPermission) refreshLocation()
                                else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            },
                            onDestination = { destinationDialog = true },
                            onRequest = {
                                val selected = destination
                                val selectedPickup = pickup
                                if (selectedPickup == null) {
                                    error = "حدد موقع الانطلاق أولًا"
                                } else if (selected == null) {
                                    destinationDialog = true
                                } else if (sessionStore.userId.isNullOrBlank()) {
                                    error = "بيانات الحساب غير مكتملة"
                                } else {
                                    requesting = true
                                    error = null
                                    scope.launch {
                                        repository.create(
                                            TripRequest(
                                                customerId = sessionStore.userId.orEmpty(),
                                                pickup = selectedPickup,
                                                destination = selected.coordinates,
                                                vehicleType = "economy",
                                                paymentMethod = "cash"
                                            )
                                        ).onSuccess {
                                            trip = it
                                        }.onFailure {
                                            error = it.message ?: "تعذر إنشاء طلب التاكسي"
                                        }
                                        requesting = false
                                    }
                                }
                            }
                        )
                    } else {
                        ActiveTrip(
                            trip = trip!!,
                            refreshing = requesting,
                            onRefresh = {
                                requesting = true
                                scope.launch {
                                    repository.get(trip!!.id).onSuccess { trip = it }.onFailure { error = it.message }
                                    requesting = false
                                    refreshTick++
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
                            error = error,
                            onBack = {
                                if (trip?.status == "completed" || trip?.status == "cancelled") {
                                    trip = null
                                    error = null
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (destinationDialog) {
        DestinationDialog(
            onDismiss = { destinationDialog = false },
            onSelect = { destination = it; destinationDialog = false }
        )
    }

    if (profileDialog) {
        ProfileDialog(sessionStore, onDismiss = { profileDialog = false }, onLogout = onLogout)
    }

    if (historyDialog) {
        HistoryDialog(repository = repository, customerId = sessionStore.userId.orEmpty(), onDismiss = { historyDialog = false })
    }
}

@Composable
private fun TaxiHome(
    sessionStore: SessionStore,
    pickup: Coordinates?,
    locationLabel: String,
    destination: TaxiDestination?,
    locationLoading: Boolean,
    requesting: Boolean,
    error: String?,
    onRefreshLocation: () -> Unit,
    onDestination: () -> Unit,
    onRequest: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("وصلها", color = Green, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    Text(
                        sessionStore.name?.takeIf { it.isNotBlank() }?.let { "أهلًا $it" } ?: "احجز مشوارك بسهولة",
                        color = Muted,
                        fontSize = 13.sp
                    )
                }
                Box(Modifier.size(46.dp).background(GreenSoft, CircleShape), Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, null, tint = Green)
                }
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("إلى أين؟", fontSize = 23.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text("اختر الوجهة وخلّ الباقي على وصلها", fontSize = 11.sp, color = Muted)

                    Row(Modifier.fillMaxWidth().background(AppBg, RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, null, tint = Green, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("موقع الانطلاق", fontSize = 10.sp, color = Muted)
                            Text(locationLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                            if (pickup != null) Text("تم تحديد الموقع", fontSize = 9.sp, color = Green)
                        }
                        IconButton(onClick = onRefreshLocation) {
                            if (locationLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Green)
                            else Icon(Icons.Default.Refresh, "تحديث الموقع", tint = Green)
                        }
                    }

                    Row(Modifier.fillMaxWidth().clickable(onClick = onDestination).background(Color(0xFFF8FAF9), RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFFD93838), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الوجهة", fontSize = 10.sp, color = Muted)
                            Text(destination?.title ?: "اختر وجهتك", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (destination == null) Muted else Ink)
                            destination?.subtitle?.let { Text(it, fontSize = 9.sp, color = Muted) }
                        }
                        Icon(Icons.Default.Search, "اختيار الوجهة", tint = Green)
                    }

                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(GreenSoft)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Green)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("تاكسي اقتصادي", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Ink)
                                Text("دفع نقدي", fontSize = 10.sp, color = Muted)
                            }
                            Text("اقتصادي", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(enabled = !requesting, onClick = onRequest, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)) {
                        if (requesting) CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp)
                        else Text("طلب تاكسي", fontSize = 16.sp, fontWeight = FontWeight.Black)
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
                    Text("آمن • سريع", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DestinationDialog(onDismiss: () -> Unit, onSelect: (TaxiDestination) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختر وجهتك", fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(destinations) { item ->
                    Card(Modifier.fillMaxWidth().clickable { onSelect(item) }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Color.White)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).background(GreenSoft, CircleShape), Alignment.Center) { Icon(Icons.Default.LocationOn, null, tint = Green) }
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
private fun ActiveTrip(
    trip: Trip,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onCancel: () -> Unit,
    error: String?,
    onBack: () -> Unit
) {
    val terminal = trip.status == "completed" || trip.status == "cancelled"
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث", tint = Green) }
            Spacer(Modifier.width(4.dp))
            Column(Modifier.weight(1f)) {
                Text("رحلتك الحالية", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(trip.statusLabel(), color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Green)
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TripInfo("المسافة", "${trip.distanceKm} كم")
                TripInfo("الوقت التقريبي", "${trip.durationMin} دقيقة")
                TripInfo("الأجرة التقديرية", "${trip.estimatedFare} ${trip.currency}")
                TripInfo("الدفع", if (trip.paymentMethod == "cash") "نقدي" else trip.paymentMethod)
                TripInfo("رقم الرحلة", trip.id)
            }
        }

        trip.driver?.let { driver ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(GreenSoft)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("الكابتن", fontSize = 10.sp, color = Muted)
                    Text(driver.name, fontSize = 19.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text("${driver.vehicle} • ${driver.plate}", fontSize = 11.sp, color = Muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFE6A500), modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(driver.rating.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        if (!terminal) {
            OutlinedButton(onClick = onCancel, enabled = !refreshing, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Close, null, tint = Danger)
                Spacer(Modifier.width(6.dp))
                Text("إلغاء الرحلة", color = Danger, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Text("العودة للرئيسية", fontWeight = FontWeight.Bold)
            }
        }
        error?.let { Text(it, color = Danger, fontSize = 11.sp) }
    }
}

@Composable
private fun TripInfo(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, Modifier.weight(1f), fontSize = 11.sp, color = Muted)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
    }
}

@Composable
private fun BottomNav(onHome: () -> Unit, onHistory: () -> Unit, onProfile: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 28.dp, vertical = 9.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        NavItem(Icons.Default.DirectionsCar, "الرئيسية", onHome)
        NavItem(Icons.Default.History, "الرحلات", onHistory)
        NavItem(Icons.Default.Person, "حسابي", onProfile)
    }
}

@Composable
private fun NavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Green, modifier = Modifier.size(21.dp))
        Text(text, color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun ProfileDialog(sessionStore: SessionStore, onDismiss: () -> Unit, onLogout: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حسابي", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(sessionStore.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                sessionStore.email?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, color = Muted) }
                sessionStore.phone?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, color = Muted) }
                Text("طريقة الدفع: نقدي", fontSize = 12.sp, color = Muted)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        dismissButton = { TextButton(onClick = onLogout) { Text("تسجيل الخروج", color = Danger) } }
    )
}

@Composable
private fun HistoryDialog(repository: TripRepository, customerId: String, onDismiss: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(customerId) {
        loading = true
        repository.list(customerId).onSuccess { trips = it }.onFailure { error = it.message ?: "تعذر جلب الرحلات" }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الرحلات السابقة", fontWeight = FontWeight.Black) },
        text = {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green) }
                error != null -> Text(error ?: "تعذر جلب الرحلات", color = Danger, fontSize = 11.sp)
                trips.isEmpty() -> Text("لا توجد رحلات سابقة بعد", color = Muted, fontSize = 12.sp)
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(trips.take(10)) { item ->
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                Text(item.statusLabel(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                                Text("${item.distanceKm} كم • ${item.estimatedFare} ${item.currency}", fontSize = 10.sp, color = Muted)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } }
    )
}
