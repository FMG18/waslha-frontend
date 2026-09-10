package com.waslha.app

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

private val Green = Color(0xFF078A60)
private val Purple = Color(0xFF6F4DBA)
private val Ink = Color(0xFF10201B)
private val Muted = Color(0xFF72807B)
private val AppBg = Color(0xFFF5F8F6)
private val SoftGreen = Color(0xFFE8F6F0)
private val Danger = Color(0xFFB42318)

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
    TaxiVehicle("economy", "اقتصادي", "سعر مناسب", 1.0),
    TaxiVehicle("comfort", "مريح", "راحة ومساحة أفضل", 1.2),
    TaxiVehicle("family", "عائلي", "مساحة أكبر للركاب", 1.35)
)

private class CustomerPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("waslha_customer_prefs", Context.MODE_PRIVATE)

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean("notifications", true)
        set(value) = prefs.edit().putBoolean("notifications", value).apply()

    var offersEnabled: Boolean
        get() = prefs.getBoolean("offers", true)
        set(value) = prefs.edit().putBoolean("offers", value).apply()

    fun saveFavorite(destination: TaxiDestination) {
        prefs.edit()
            .putString("favorite_title", destination.title)
            .putString("favorite_subtitle", destination.subtitle)
            .putString("favorite_lat", destination.coordinates.lat.toString())
            .putString("favorite_lng", destination.coordinates.lng.toString())
            .apply()
    }

    fun favorite(): TaxiDestination? {
        val title = prefs.getString("favorite_title", null) ?: return null
        val subtitle = prefs.getString("favorite_subtitle", "من الأماكن المحفوظة") ?: "من الأماكن المحفوظة"
        val lat = prefs.getString("favorite_lat", null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString("favorite_lng", null)?.toDoubleOrNull() ?: return null
        return TaxiDestination(title, subtitle, Coordinates(lat, lng))
    }

    fun clearFavorite() = prefs.edit()
        .remove("favorite_title")
        .remove("favorite_subtitle")
        .remove("favorite_lat")
        .remove("favorite_lng")
        .apply()
}

class SafeTaxiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val sessionStore = SessionStore(this)
        setContent {
            TaxiExperience(
                sessionStore = sessionStore,
                onLogout = {
                    sessionStore.clear()
                    finish()
                }
            )
        }
    }
}

@Composable
private fun TaxiExperience(
    sessionStore: SessionStore,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { TripRepository(ApiProvider.api) }
    val locationProvider = remember { LocationProvider(context) }
    val customerPrefs = remember { CustomerPrefs(context) }

    var tab by remember { mutableStateOf(0) }
    var pickup by remember { mutableStateOf<Coordinates?>(null) }
    var pickupLabel by remember { mutableStateOf("جاري تحديد موقعك…") }
    var destination by remember { mutableStateOf<TaxiDestination?>(null) }
    var selectedVehicle by remember { mutableStateOf(vehicles.first()) }
    var fareEstimate by remember { mutableStateOf<FareEstimate?>(null) }
    var estimateLoading by remember { mutableStateOf(false) }
    var destinationDialog by remember { mutableStateOf(false) }
    var mapPicker by remember { mutableStateOf(false) }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var history by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(false) }
    var requestLoading by remember { mutableStateOf(false) }
    var locationLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var supportDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }
    var detailTrip by remember { mutableStateOf<Trip?>(null) }
    var favorite by remember { mutableStateOf(customerPrefs.favorite()) }
    var notificationsEnabled by remember { mutableStateOf(customerPrefs.notificationsEnabled) }
    var offersEnabled by remember { mutableStateOf(customerPrefs.offersEnabled) }

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

    val permissionGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true || result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            refreshLocation()
        } else {
            error = "صلاحية الموقع مطلوبة لطلب التاكسي"
        }
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) refreshLocation()
        else permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
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
        if (tab == 1) {
            val userId = sessionStore.userId ?: return@LaunchedEffect
            historyLoading = true
            repository.list(userId)
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
            when {
                mapPicker -> MapPickerScreenPlus(
                    pickup = pickup ?: DamascusCenter,
                    destination = destination?.coordinates,
                    onBack = { mapPicker = false },
                    onPicked = { coordinates ->
                        destination = TaxiDestination("الموقع المحدد", "تم اختياره من الخريطة", coordinates)
                        mapPicker = false
                    }
                )
                trip != null -> ActiveTripScreenPlus(
                    trip = trip!!,
                    pickup = pickup,
                    destination = destination,
                    loading = requestLoading,
                    error = error,
                    onRefresh = {
                        requestLoading = true
                        scope.launch {
                            repository.get(trip!!.id)
                                .onSuccess { trip = it }
                                .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                            requestLoading = false
                        }
                    },
                    onCancel = {
                        requestLoading = true
                        scope.launch {
                            repository.cancel(trip!!.id, "إلغاء من الراكب")
                                .onSuccess { trip = it }
                                .onFailure { error = it.message ?: "تعذر إلغاء الرحلة" }
                            requestLoading = false
                        }
                    },
                    onShare = { shareTrip(context, trip!!) },
                    onDone = {
                        trip = null
                        tab = 1
                        error = null
                    }
                )
                else -> Scaffold(
                    containerColor = AppBg,
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
                            0 -> HomeScreenPlus(
                                sessionStore = sessionStore,
                                pickupLabel = pickupLabel,
                                destination = destination,
                                selectedVehicle = selectedVehicle,
                                fareEstimate = fareEstimate,
                                estimateLoading = estimateLoading,
                                locationLoading = locationLoading,
                                requesting = requestLoading,
                                error = error,
                                favorite = favorite,
                                onRefreshLocation = {
                                    if (permissionGranted) refreshLocation()
                                    else permissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
                                },
                                onDestination = { destinationDialog = true },
                                onMap = { mapPicker = true },
                                onVehicle = { selectedVehicle = it },
                                onSaveFavorite = {
                                    destination?.let {
                                        customerPrefs.saveFavorite(it)
                                        favorite = it
                                    }
                                },
                                onUseFavorite = { favorite?.let { destination = it } },
                                onRequest = {
                                    val from = pickup
                                    val to = destination
                                    val userId = sessionStore.userId
                                    when {
                                        from == null -> error = "حدد موقع الانطلاق أولًا"
                                        to == null -> destinationDialog = true
                                        userId.isNullOrBlank() -> error = "بيانات الحساب غير مكتملة، سجّل الدخول من جديد"
                                        else -> {
                                            requestLoading = true
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
                                                requestLoading = false
                                            }
                                        }
                                    }
                                },
                                onSupport = { supportDialog = true }
                            )
                            1 -> TripsScreenPlus(
                                history = history,
                                loading = historyLoading,
                                error = error,
                                onRefresh = {
                                    val userId = sessionStore.userId ?: return@TripsScreenPlus
                                    historyLoading = true
                                    scope.launch {
                                        repository.list(userId)
                                            .onSuccess { history = it }
                                            .onFailure { error = it.message ?: "تعذر تحديث الرحلات" }
                                        historyLoading = false
                                    }
                                },
                                onDetails = { detailTrip = it },
                                onShare = { shareTrip(context, it) }
                            )
                            else -> ProfileScreenPlus(
                                sessionStore = sessionStore,
                                notificationsEnabled = notificationsEnabled,
                                offersEnabled = offersEnabled,
                                favorite = favorite,
                                onNotificationsChanged = {
                                    notificationsEnabled = it
                                    customerPrefs.notificationsEnabled = it
                                },
                                onOffersChanged = {
                                    offersEnabled = it
                                    customerPrefs.offersEnabled = it
                                },
                                onFavoriteClear = {
                                    customerPrefs.clearFavorite()
                                    favorite = null
                                },
                                onSupport = { supportDialog = true },
                                onAbout = { aboutDialog = true },
                                onLogout = onLogout
                            )
                        }
                    }
                }
            }
        }
    }

    if (destinationDialog) {
        DestinationDialogPlus(
            favorite = favorite,
            onDismiss = { destinationDialog = false },
            onMap = { destinationDialog = false; mapPicker = true },
            onSelect = { destination = it; destinationDialog = false }
        )
    }
    detailTrip?.let { selected -> TripDetailsDialog(selected) { detailTrip = null } }
    if (supportDialog) SupportDialogPlus { supportDialog = false }
    if (aboutDialog) AboutDialogPlus { aboutDialog = false }
}

private fun localEstimate(from: Coordinates, to: Coordinates, multiplier: Double): FareEstimate {
    val dx = (to.lng - from.lng) * 85.0
    val dy = (to.lat - from.lat) * 111.0
    val km = sqrt(dx * dx + dy * dy).coerceAtLeast(0.2)
    return FareEstimate(km, (km * 3.0).toInt().coerceAtLeast(3), "ل.س", (2500 + km * 1200 * multiplier).toInt())
}

private fun shareTrip(context: Context, trip: Trip) {
    val text = buildString {
        append("رحلة وصلها\n")
        append("الحالة: ${trip.statusLabel()}\n")
        append("الأجرة: ${trip.estimatedFare} ${trip.currency}\n")
        append("المسافة: ${trip.distanceKm} كم\n")
        append("المدة: ${trip.durationMin} دقيقة")
    }
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, "مشاركة تفاصيل الرحلة"))
}

@Composable
private fun HomeScreenPlus(
    sessionStore: SessionStore,
    pickupLabel: String,
    destination: TaxiDestination?,
    selectedVehicle: TaxiVehicle,
    fareEstimate: FareEstimate?,
    estimateLoading: Boolean,
    locationLoading: Boolean,
    requesting: Boolean,
    error: String?,
    favorite: TaxiDestination?,
    onRefreshLocation: () -> Unit,
    onDestination: () -> Unit,
    onMap: () -> Unit,
    onVehicle: (TaxiVehicle) -> Unit,
    onSaveFavorite: () -> Unit,
    onUseFavorite: () -> Unit,
    onRequest: () -> Unit,
    onSupport: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 22.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("وصلها", color = Green, fontSize = 35.sp, fontWeight = FontWeight.Black)
                    Text(sessionStore.name?.takeIf { it.isNotBlank() }?.let { "أهلًا $it" } ?: "احجز مشوارك بسهولة", color = Muted, fontSize = 13.sp)
                }
                IconButton(onClick = onSupport) { Icon(Icons.Default.HelpOutline, "المساعدة", tint = Green) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                QuickActionCard("موقعي", Icons.Default.MyLocation, Modifier.weight(1f), onRefreshLocation)
                QuickActionCard("الخريطة", Icons.Default.LocationOn, Modifier.weight(1f), onMap)
            }
        }
        if (favorite != null) item {
            Card(Modifier.fillMaxWidth().clickable { onUseFavorite() }, colors = CardDefaults.cardColors(SoftGreen), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Favorite, null, tint = Green) }
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("وجهتك المحفوظة", color = Ink, fontWeight = FontWeight.Black)
                        Text(favorite.title, color = Muted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronLeft, null, tint = Muted)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("إلى أين؟", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Ink)
                    Text("اختَر موقع الانطلاق والوجهة ثم اطلب السيارة", fontSize = 11.sp, color = Muted)
                    LocationSelector("نقطة الانطلاق", pickupLabel, Icons.Default.MyLocation, onRefreshLocation, locationLoading)
                    LocationSelector("الوجهة", destination?.title ?: "اختيار الوجهة", Icons.Default.LocationOn, onDestination, false)
                    if (destination != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onSaveFavorite) {
                            Icon(if (favorite?.title == destination.title) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (favorite?.title == destination.title) "محفوظة" else "حفظ الوجهة", color = Green, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("نوع التكسي", color = Ink, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    vehicles.forEach { vehicle -> VehicleRowPlus(vehicle, vehicle.id == selectedVehicle.id) { onVehicle(vehicle) } }
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(SoftGreen), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, null, tint = Green)
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("الدفع نقداً", color = Ink, fontWeight = FontWeight.Bold)
                                Text(if (estimateLoading) "نحسب الأجرة…" else fareEstimate?.let { "${it.estimatedFare} ${it.currency}" } ?: "اختر الوجهة لحساب الأجرة"), color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                    if (!error.isNullOrBlank()) Text(error, color = Danger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onRequest, enabled = !requesting, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple)) {
                        if (requesting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("طلب التكسي", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Route, null, tint = Green, modifier = Modifier.size(29.dp))
                    Spacer(Modifier.size(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("وصلها تكسي", color = Ink, fontWeight = FontWeight.Black)
                        Text("حجز مشوار داخل سوريا بتجربة بسيطة وواضحة", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.clickable { onClick() }, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(7.dp))
            Text(title, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun LocationSelector(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, loading: Boolean) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(AppBg), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(27.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Muted, fontSize = 10.sp)
                Text(value, color = Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            if (loading) CircularProgressIndicator(Modifier.size(19.dp), color = Green, strokeWidth = 2.dp)
            else Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}

@Composable
private fun VehicleRowPlus(vehicle: TaxiVehicle, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(if (selected) SoftGreen else Color.White), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color(0xFFF0F4F2), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = Green) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(vehicle.title, color = Ink, fontWeight = FontWeight.Black)
                Text(vehicle.subtitle, color = Muted, fontSize = 10.sp)
            }
            if (selected) Text("✓", color = Green, fontSize = 21.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun TripsScreenPlus(history: List<Trip>, loading: Boolean, error: String?, onRefresh: () -> Unit, onDetails: (Trip) -> Unit, onShare: (Trip) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 15.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلاتي", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("سجل رحلات التكسي وحالتها", color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث", tint = Green) }
        }
        Spacer(Modifier.height(10.dp))
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green) }
        else if (history.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.History, null, tint = Muted, modifier = Modifier.size(54.dp))
                Spacer(Modifier.height(8.dp))
                Text("لا توجد رحلات بعد", color = Ink, fontWeight = FontWeight.Bold)
                Text("عندما تطلب تكسي ستظهر الرحلة هنا", color = Muted, fontSize = 11.sp)
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(history) { trip ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(trip.statusLabel(), color = Green, fontWeight = FontWeight.Black)
                                Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 10.sp)
                            }
                            Text("${trip.estimatedFare} ${trip.currency}", color = Ink, fontWeight = FontWeight.Black)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onDetails(trip) }) { Text("التفاصيل", color = Green) }
                            TextButton(onClick = { onShare(trip) }) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("مشاركة", color = Green)
                            }
                        }
                    }
                }
            }
        }
        if (!error.isNullOrBlank()) Text(error, color = Danger, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileScreenPlus(
    sessionStore: SessionStore,
    notificationsEnabled: Boolean,
    offersEnabled: Boolean,
    favorite: TaxiDestination?,
    onNotificationsChanged: (Boolean) -> Unit,
    onOffersChanged: (Boolean) -> Unit,
    onFavoriteClear: () -> Unit,
    onSupport: () -> Unit,
    onAbout: () -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 15.dp, bottom = 20.dp)) {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Default.AccountCircle, null, tint = Green, modifier = Modifier.size(58.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(sessionStore.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(sessionStore.email ?: sessionStore.phone ?: "بيانات الحساب", color = Muted, fontSize = 12.sp)
                }
            }
        }
        item { SectionTitle("التفضيلات") }
        item { ToggleCard("إشعارات الرحلات", "تنبيهات حالة الرحلة", Icons.Default.Notifications, notificationsEnabled, onNotificationsChanged) }
        item { ToggleCard("العروض", "تنبيهات الخصومات والعروض", Icons.Default.Star, offersEnabled, onOffersChanged) }
        item { SectionTitle("الحساب والخدمات") }
        item { ProfileActionPlus("الوجهة المحفوظة", favorite?.title ?: "لا توجد وجهة محفوظة", Icons.Default.Favorite) { } }
        if (favorite != null) item { ProfileActionPlus("حذف الوجهة المحفوظة", "إزالة الموقع من الجهاز", Icons.Default.FavoriteBorder, onFavoriteClear) }
        item { ProfileActionPlus("طرق الدفع", "الدفع النقدي متاح حالياً", Icons.Default.CreditCard) { } }
        item { ProfileActionPlus("الأمان والخصوصية", "إعدادات الحساب والحماية", Icons.Default.Security) { } }
        item { ProfileActionPlus("المساعدة والدعم", "الأسئلة والمشاكل الشائعة", Icons.Default.HelpOutline, onSupport) }
        item { ProfileActionPlus("عن وصلها", "معلومات النسخة والتطبيق", Icons.Default.Info, onAbout) }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp)) {
                Icon(Icons.Default.Logout, null, tint = Danger)
                Spacer(Modifier.width(7.dp))
                Text("تسجيل الخروج", color = Danger, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
}

@Composable
private fun ToggleCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 10.sp)
            }
            Switch(checked = checked, onCheckedChange = onChanged)
        }
    }
}

@Composable
private fun ProfileActionPlus(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}

@Composable
private fun DestinationDialogPlus(favorite: TaxiDestination?, onDismiss: () -> Unit, onMap: () -> Unit, onSelect: (TaxiDestination) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("اختيار الوجهة", color = Ink, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                favorite?.let { DestinationOption("المحفوظة", it.title, Icons.Default.Favorite) { onSelect(it) } }
                destinations.forEach { item -> DestinationOption(item.title, item.subtitle, Icons.Default.LocationOn) { onSelect(item) } }
                DestinationOption("اختيار من الخريطة", "حرّك الخريطة واختر نقطة دقيقة", Icons.Default.MyLocation, onMap)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = Green) } }
    )
}

@Composable
private fun DestinationOption(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(AppBg), shape = RoundedCornerShape(15.dp)) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}

@Composable
private fun ActiveTripScreenPlus(trip: Trip, pickup: Coordinates?, destination: TaxiDestination?, loading: Boolean, error: String?, onRefresh: () -> Unit, onCancel: () -> Unit, onShare: () -> Unit, onDone: () -> Unit) {
    val completed = trip.status == "completed"
    val cancelled = trip.status == "cancelled"
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Ink) }
            Spacer(Modifier.weight(1f))
            Text("الرحلة الحالية", color = Ink, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
        if (pickup != null && destination != null) {
            Card(Modifier.fillMaxWidth().height(235.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp)) {
                WaslhaRideMap(pickup = pickup, destination = destination.coordinates, modifier = Modifier.fillMaxSize(), onDestinationPicked = { })
            }
            Spacer(Modifier.height(12.dp))
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(23.dp)) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(trip.statusLabel(), color = if (cancelled) Danger else Green, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 11.sp)
                    }
                    Text("${trip.estimatedFare} ${trip.currency}", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                }
                DividerLine()
                InfoRow("نوع السيارة", trip.vehicleType)
                InfoRow("طريقة الدفع", trip.paymentMethod)
                if (!error.isNullOrBlank()) Text(error, color = Danger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) {
                        if (loading) CircularProgressIndicator(Modifier.size(17.dp), color = Green, strokeWidth = 2.dp) else Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(5.dp))
                        Text("تحديث")
                    }
                    OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(5.dp))
                        Text("مشاركة")
                    }
                }
                if (!completed && !cancelled) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)) {
                        Text("إلغاء الرحلة", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(49.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                        Text("العودة إلى رحلاتي", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun DividerLine() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE7ECE9)))
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = Muted, fontSize = 11.sp)
        Text(value, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TripDetailsDialog(trip: Trip, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تفاصيل الرحلة", color = Ink, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                InfoRow("الحالة", trip.statusLabel())
                InfoRow("الأجرة", "${trip.estimatedFare} ${trip.currency}")
                InfoRow("المسافة", "${trip.distanceKm} كم")
                InfoRow("المدة", "${trip.durationMin} دقيقة")
                InfoRow("السيارة", trip.vehicleType)
                InfoRow("الدفع", trip.paymentMethod)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = Green) } }
    )
}

@Composable
private fun SupportDialogPlus(onDismiss: () -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    val topics = listOf(
        "كيف أطلب تكسي؟" to "حدد موقع الانطلاق، اختر الوجهة ونوع السيارة ثم اضغط طلب التكسي.",
        "كيف أختار الوجهة؟" to "يمكنك اختيار وجهة جاهزة أو فتح الخريطة وتحديد موقع دقيق.",
        "كيف ألغي الرحلة؟" to "من صفحة الرحلة الحالية استخدم زر إلغاء الرحلة ما دامت الرحلة قابلة للإلغاء.",
        "مشكلة في الحساب" to "تأكد من الاتصال بالإنترنت ثم سجّل الدخول من جديد عند الحاجة."
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("المساعدة والدعم", color = Ink, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                topics.forEach { (title, body) ->
                    Card(Modifier.fillMaxWidth().clickable { selected = body }, colors = CardDefaults.cardColors(AppBg), shape = RoundedCornerShape(15.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HelpOutline, null, tint = Green)
                            Spacer(Modifier.width(8.dp))
                            Text(title, color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = Green) } }
    )
    selected?.let { body ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("مساعدة") },
            text = { Text(body, color = Muted) },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("حسناً", color = Green) } }
        )
    }
}

@Composable
private fun AboutDialogPlus(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("عن وصلها", color = Ink, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("وصلها", color = Green, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("تطبيق تكسي لحجز المشاوير داخل سوريا.", color = Ink)
                Text("الإصدار الحالي: 1.3.7", color = Muted, fontSize = 11.sp)
                Text("الواجهة مصممة لتكون سريعة وواضحة على الهاتف.", color = Muted, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = Green) } }
    )
}

@Composable
private fun MapPickerScreenPlus(pickup: Coordinates, destination: Coordinates?, onBack: () -> Unit, onPicked: (Coordinates) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        WaslhaRideMap(pickup = pickup, destination = destination, modifier = Modifier.fillMaxSize(), onDestinationPicked = onPicked)
        Card(Modifier.align(Alignment.TopCenter).padding(top = 14.dp, start = 16.dp, end = 16.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = .96f)), shape = RoundedCornerShape(17.dp)) {
            Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = Ink) }
                Spacer(Modifier.width(5.dp))
                Text("حرّك الخريطة وحدد وجهتك", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
        }
        Card(Modifier.align(Alignment.BottomCenter).padding(16.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)), shape = RoundedCornerShape(18.dp)) {
            Text("اضغط على المكان بعد ضبط الخريطة لتثبيت الوجهة", modifier = Modifier.padding(13.dp), color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}
