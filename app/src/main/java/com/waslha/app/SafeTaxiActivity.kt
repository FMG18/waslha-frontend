package com.waslha.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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

private val WaslhaGreen = Color(0xFF078A60)
private val WaslhaPurple = Color(0xFF6F4DBA)
private val WaslhaInk = Color(0xFF10201B)
private val WaslhaMuted = Color(0xFF6F7D78)
private val WaslhaBg = Color(0xFFF5F8F6)
private val WaslhaSoft = Color(0xFFE8F6F0)
private val WaslhaDanger = Color(0xFFB42318)
private val Damascus = Coordinates(33.5138, 36.2765)

private data class TaxiDestination(val title: String, val subtitle: String, val coordinates: Coordinates)
private data class TaxiVehicle(val id: String, val title: String, val subtitle: String, val multiplier: Double)

private val TaxiDestinations = listOf(
    TaxiDestination("ساحة الأمويين", "دمشق", Coordinates(33.5138, 36.2765)),
    TaxiDestination("جامعة دمشق", "المزة - دمشق", Coordinates(33.5101, 36.2766)),
    TaxiDestination("سوق الحميدية", "المدينة القديمة", Coordinates(33.5112, 36.3051)),
    TaxiDestination("المزة", "دمشق", Coordinates(33.4941, 36.2384)),
    TaxiDestination("محطة الحجاز", "دمشق", Coordinates(33.5070, 36.2895))
)

private val TaxiVehicles = listOf(
    TaxiVehicle("economy", "اقتصادي", "سعر مناسب", 1.0),
    TaxiVehicle("comfort", "مريح", "راحة ومساحة أفضل", 1.2),
    TaxiVehicle("family", "عائلي", "مساحة أكبر للركاب", 1.35)
)

private class CustomerPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("waslha_customer_prefs", Context.MODE_PRIVATE)
    var notifications: Boolean
        get() = prefs.getBoolean("notifications", true)
        set(value) { prefs.edit().putBoolean("notifications", value).apply() }
    var offers: Boolean
        get() = prefs.getBoolean("offers", true)
        set(value) { prefs.edit().putBoolean("offers", value).apply() }
    fun saveFavorite(d: TaxiDestination) = prefs.edit()
        .putString("favorite_title", d.title).putString("favorite_subtitle", d.subtitle)
        .putString("favorite_lat", d.coordinates.lat.toString()).putString("favorite_lng", d.coordinates.lng.toString()).apply()
    fun getFavorite(): TaxiDestination? {
        val title = prefs.getString("favorite_title", null) ?: return null
        val subtitle = prefs.getString("favorite_subtitle", "محفوظة") ?: "محفوظة"
        val lat = prefs.getString("favorite_lat", null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString("favorite_lng", null)?.toDoubleOrNull() ?: return null
        return TaxiDestination(title, subtitle, Coordinates(lat, lng))
    }
    fun clearFavorite() = prefs.edit().remove("favorite_title").remove("favorite_subtitle").remove("favorite_lat").remove("favorite_lng").apply()
}

class SafeTaxiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val session = SessionStore(this)
        setContent {
            WaslhaTheme {
                TaxiApp(session, onLogout = {
                    session.clear()
                    startActivity(Intent(this, AuthActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                    finish()
                })
            }
        }
    }
}

@Composable
private fun TaxiApp(session: SessionStore, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TripRepository(ApiProvider.api) }
    val locator = remember { LocationProvider(context) }
    val prefs = remember { CustomerPrefs(context) }

    var tab by remember { mutableStateOf(0) }
    var pickup by remember { mutableStateOf<Coordinates?>(null) }
    var pickupLabel by remember { mutableStateOf("جاري تحديد موقعك…") }
    var destination by remember { mutableStateOf<TaxiDestination?>(null) }
    var selectedVehicle by remember { mutableStateOf(TaxiVehicles.first()) }
    var fare by remember { mutableStateOf<FareEstimate?>(null) }
    var calculating by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var requesting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var history by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var loadingHistory by remember { mutableStateOf(false) }
    var mapPicker by remember { mutableStateOf(false) }
    var destinationDialog by remember { mutableStateOf(false) }
    var supportDialog by remember { mutableStateOf(false) }
    var aboutDialog by remember { mutableStateOf(false) }
    var tripDetails by remember { mutableStateOf<Trip?>(null) }
    var favorite by remember { mutableStateOf(prefs.getFavorite()) }
    var notifications by remember { mutableStateOf(prefs.notifications) }
    var offers by remember { mutableStateOf(prefs.offers) }

    fun refreshLocation() {
        locating = true
        scope.launch {
            val loc = locator.lastKnown()
            if (loc != null) {
                pickup = Coordinates(loc.latitude, loc.longitude)
                pickupLabel = "موقعك الحالي"
                error = null
            } else {
                pickupLabel = "تعذر تحديد الموقع"
                error = "شغّل GPS وتأكد من صلاحية الموقع"
            }
            locating = false
        }
    }

    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) refreshLocation()
        else error = "صلاحية الموقع مطلوبة لطلب التاكسي"
    }

    LaunchedEffect(Unit) {
        if (granted) refreshLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(pickup, destination, selectedVehicle.id) {
        val a = pickup ?: return@LaunchedEffect
        val b = destination?.coordinates ?: return@LaunchedEffect
        calculating = true
        repo.estimate(a, b, selectedVehicle.id)
            .onSuccess { fare = it }
            .onFailure { fare = localEstimate(a, b, selectedVehicle.multiplier) }
        calculating = false
    }

    LaunchedEffect(tab) {
        if (tab == 1) {
            val id = session.userId ?: return@LaunchedEffect
            loadingHistory = true
            repo.list(id).onSuccess { history = it }.onFailure { error = it.message ?: "تعذر جلب الرحلات" }
            loadingHistory = false
        }
    }

    LaunchedEffect(trip?.id) {
        val id = trip?.id ?: return@LaunchedEffect
        while (isActive) {
            delay(5000)
            repo.get(id).onSuccess { updated -> trip = updated }
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = WaslhaBg) {
            when {
                mapPicker -> MapPickerScreen(pickup ?: Damascus, destination?.coordinates, onBack = { mapPicker = false }) { c ->
                    destination = TaxiDestination("الموقع المحدد", "من الخريطة", c)
                    mapPicker = false
                }
                trip != null -> ActiveTrip(trip = trip!!, pickup = pickup, destination = destination,
                    loading = requesting, error = error,
                    onRefresh = {
                        requesting = true
                        scope.launch { repo.get(trip!!.id).onSuccess { trip = it }.onFailure { error = it.message ?: "تعذر التحديث" }; requesting = false }
                    },
                    onCancel = {
                        requesting = true
                        scope.launch { repo.cancel(trip!!.id, "إلغاء من الراكب").onSuccess { trip = it }.onFailure { error = it.message ?: "تعذر الإلغاء" }; requesting = false }
                    },
                    onShare = { shareTrip(context, trip!!) },
                    onDone = { trip = null; tab = 1; error = null }
                )
                else -> Scaffold(
                    containerColor = WaslhaBg,
                    bottomBar = {
                        NavigationBar(containerColor = Color.White, tonalElevation = 5.dp, modifier = Modifier.navigationBarsPadding()) {
                            NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("الرئيسية") })
                            NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.History, null) }, label = { Text("رحلاتي") })
                            NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("حسابي") })
                        }
                    }
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        when (tab) {
                            0 -> HomeScreen(session, pickupLabel, destination, selectedVehicle, fare, calculating, locating, requesting, error, favorite,
                                onRefreshLocation = { if (granted) refreshLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                                onDestination = { destinationDialog = true }, onMap = { mapPicker = true }, onVehicle = { selectedVehicle = it },
                                onSaveFavorite = { destination?.let { prefs.saveFavorite(it); favorite = it } }, onUseFavorite = { favorite?.let { destination = it } },
                                onRequest = {
                                    val a = pickup; val b = destination; val user = session.userId
                                    when {
                                        a == null -> error = "حدد موقع الانطلاق أولًا"
                                        b == null -> destinationDialog = true
                                        user.isNullOrBlank() -> error = "بيانات الحساب غير مكتملة"
                                        else -> {
                                            requesting = true; error = null
                                            scope.launch {
                                                repo.create(TripRequest(user, a, b.coordinates, selectedVehicle.id, "cash"))
                                                    .onSuccess { trip = it }.onFailure { error = it.message ?: "تعذر إنشاء الرحلة" }
                                                requesting = false
                                            }
                                        }
                                    }
                                }, onSupport = { supportDialog = true })
                            1 -> TripsScreen(history, loadingHistory, error, onRefresh = {
                                val user = session.userId ?: return@TripsScreen
                                loadingHistory = true; scope.launch { repo.list(user).onSuccess { history = it }.onFailure { error = it.message ?: "تعذر جلب الرحلات" }; loadingHistory = false }
                            }, onDetails = { tripDetails = it }, onShare = { shareTrip(context, it) })
                            else -> ProfileScreen(session, notifications, offers, favorite,
                                onNotifications = { notifications = it; prefs.notifications = it }, onOffers = { offers = it; prefs.offers = it },
                                onClearFavorite = { prefs.clearFavorite(); favorite = null }, onSupport = { supportDialog = true }, onAbout = { aboutDialog = true }, onLogout = onLogout)
                        }
                    }
                }
            }
        }
    }

    if (destinationDialog) DestinationDialog(favorite, { destinationDialog = false }, { destinationDialog = false; mapPicker = true }) { destination = it; destinationDialog = false }
    tripDetails?.let { TripDetailsDialog(it) { tripDetails = null } }
    if (supportDialog) SupportDialog { supportDialog = false }
    if (aboutDialog) AboutDialog { aboutDialog = false }
}

@Composable
private fun HomeScreen(session: SessionStore, pickup: String, destination: TaxiDestination?, vehicle: TaxiVehicle, fare: FareEstimate?, calculating: Boolean, locating: Boolean, requesting: Boolean, error: String?, favorite: TaxiDestination?, onRefreshLocation: () -> Unit, onDestination: () -> Unit, onMap: () -> Unit, onVehicle: (TaxiVehicle) -> Unit, onSaveFavorite: () -> Unit, onUseFavorite: () -> Unit, onRequest: () -> Unit, onSupport: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 22.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("وصلها", color = WaslhaGreen, fontSize = 34.sp, fontWeight = FontWeight.Black); Text(session.name?.takeIf { it.isNotBlank() }?.let { "أهلًا $it" } ?: "احجز مشوارك بسهولة", color = WaslhaMuted, fontSize = 13.sp) }
                IconButton(onClick = onSupport) { Icon(Icons.Default.HelpOutline, "المساعدة", tint = WaslhaGreen) }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) { QuickCard("موقعي", Icons.Default.MyLocation, Modifier.weight(1f), onRefreshLocation); QuickCard("الخريطة", Icons.Default.LocationOn, Modifier.weight(1f), onMap) } }
        if (favorite != null) item { Card(Modifier.fillMaxWidth().clickable { onUseFavorite() }, colors = CardDefaults.cardColors(WalshaSoftCompat), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(Color.White, CircleShape), Alignment.Center) { Icon(Icons.Default.Favorite, null, tint = WaslhaGreen) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("وجهتك المحفوظة", fontWeight = FontWeight.Black, color = WaslhaInk); Text(favorite.title, color = WaslhaMuted, fontSize = 11.sp) }; Icon(Icons.Default.ChevronLeft, null, tint = WaslhaMuted) } } }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(25.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("إلى أين؟", color = WaslhaInk, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("اختَر الانطلاق والوجهة ونوع التكسي", color = WaslhaMuted, fontSize = 11.sp)
                    LocationCard("نقطة الانطلاق", pickup, Icons.Default.MyLocation, onRefreshLocation, locating)
                    LocationCard("الوجهة", destination?.title ?: "اختيار الوجهة", Icons.Default.LocationOn, onDestination, false)
                    if (destination != null) TextButton(onClick = onSaveFavorite) { Icon(if (favorite?.title == destination.title) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null); Spacer(Modifier.width(5.dp)); Text(if (favorite?.title == destination.title) "محفوظة" else "حفظ الوجهة", color = WaslhaGreen) }
                    Text("نوع التكسي", color = WaslhaInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    TaxiVehicles.forEach { TaxiVehicleCard(it, it.id == vehicle.id) { onVehicle(it) } }
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(WalshaSoftCompat), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CreditCard, null, tint = WaslhaGreen); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("الدفع نقدًا", color = WaslhaInk, fontWeight = FontWeight.Bold); Text(if (calculating) "نحسب الأجرة…" else fare?.let { "${it.estimatedFare} ${it.currency} • ${it.distanceKm} كم • ${it.durationMin} دقيقة" } ?: "اختر الوجهة لحساب الأجرة"), color = WaslhaMuted, fontSize = 11.sp) } }
                    }
                    error?.let { Text(it, color = WaslhaDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    Button(onClick = onRequest, enabled = !requesting, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = WaslhaPurple)) { if (requesting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("طلب التكسي", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp) }
                }
            }
        }
        item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Route, null, tint = WaslhaGreen, modifier = Modifier.size(28.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("وصلها تكسي", color = WaslhaInk, fontWeight = FontWeight.Black); Text("حجز مشوار داخل سوريا بتجربة واضحة وسريعة", color = WaslhaMuted, fontSize = 11.sp) }; Text("آمن • واضح • سريع", color = WaslhaGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } }
    }
}

private val WalshaSoftCompat = WaslhaSoft

@Composable private fun QuickCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) { Card(modifier.clickable { onClick() }, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Icon(icon, null, tint = WaslhaGreen, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(7.dp)); Text(title, color = WaslhaInk, fontWeight = FontWeight.Bold, fontSize = 12.sp) } } }
@Composable private fun LocationCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, loading: Boolean) { Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(WalshaSoftCompat), shape = RoundedCornerShape(17.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = WaslhaGreen, modifier = Modifier.size(26.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = WaslhaMuted, fontSize = 10.sp); Text(value, color = WaslhaInk, fontWeight = FontWeight.Bold, fontSize = 14.sp) }; if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = WaslhaGreen, strokeWidth = 2.dp) else Icon(Icons.Default.ChevronLeft, null, tint = WaslhaMuted) } } }
@Composable private fun TaxiVehicleCard(v: TaxiVehicle, selected: Boolean, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(if (selected) WalshaSoftCompat else Color.White), shape = RoundedCornerShape(17.dp), border = if (selected) BorderStroke(1.dp, WaslhaGreen) else null) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Color(0xFFF0F4F2), CircleShape), Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = WaslhaGreen) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(v.title, color = WaslhaInk, fontWeight = FontWeight.Black); Text(v.subtitle, color = WaslhaMuted, fontSize = 10.sp) }; if (selected) Text("✓", color = WaslhaGreen, fontSize = 21.sp, fontWeight = FontWeight.Black) } } }

@Composable private fun TripsScreen(history: List<Trip>, loading: Boolean, error: String?, onRefresh: () -> Unit, onDetails: (Trip) -> Unit, onShare: (Trip) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 15.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("رحلاتي", color = WaslhaInk, fontSize = 29.sp, fontWeight = FontWeight.Black); Text("سجل رحلات التكسي وحالتها", color = WaslhaMuted, fontSize = 12.sp) }; IconButton(onClick = onRefresh) { if (loading) CircularProgressIndicator(Modifier.size(19.dp), color = WaslhaGreen, strokeWidth = 2.dp) else Icon(Icons.Default.Refresh, "تحديث", tint = WaslhaGreen) } }; Spacer(Modifier.height(10.dp)); if (history.isEmpty() && !loading) Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.History, null, tint = WaslhaMuted, modifier = Modifier.size(54.dp)); Spacer(Modifier.height(8.dp)); Text("لا توجد رحلات بعد", color = WaslhaInk, fontWeight = FontWeight.Bold) } } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) { items(history) { t -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp), elevation = CardDefaults.cardElevation(1.dp)) { Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(t.status, color = WaslhaGreen, fontWeight = FontWeight.Black); Text("${t.distanceKm} كم • ${t.durationMin} دقيقة", color = WaslhaMuted, fontSize = 10.sp) }; Text("${t.estimatedFare} ${t.currency}", color = WaslhaInk, fontWeight = FontWeight.Black) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = { onDetails(t) }) { Text("التفاصيل", color = WaslhaGreen) }; TextButton(onClick = { onShare(t) }) { Icon(Icons.Default.Share, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("مشاركة", color = WaslhaGreen) } } } } } } }
}

@Composable private fun ProfileScreen(session: SessionStore, notifications: Boolean, offers: Boolean, favorite: TaxiDestination?, onNotifications: (Boolean) -> Unit, onOffers: (Boolean) -> Unit, onClearFavorite: () -> Unit, onSupport: () -> Unit, onAbout: () -> Unit, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 15.dp, bottom = 22.dp)) {
        item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(2.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(64.dp).background(WalshaSoftCompat, CircleShape), Alignment.Center) { Icon(Icons.Default.AccountCircle, null, tint = WaslhaGreen, modifier = Modifier.size(48.dp)) }; Spacer(Modifier.width(12.dp)); Column { Text(session.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", color = WaslhaInk, fontSize = 20.sp, fontWeight = FontWeight.Black); Text(session.email ?: session.phone ?: "بيانات الحساب", color = WaslhaMuted, fontSize = 11.sp) } } } }
        item { Section("التفضيلات") }
        item { ToggleRow("إشعارات الرحلات", "تنبيهات حالة الرحلة", Icons.Default.Notifications, notifications, onNotifications) }
        item { ToggleRow("العروض", "تنبيهات الخصومات والعروض", Icons.Default.Star, offers, onOffers) }
        item { Section("الحساب والخدمات") }
        item { ActionRow("الوجهة المحفوظة", favorite?.title ?: "لا توجد وجهة محفوظة", Icons.Default.Favorite) {} }
        if (favorite != null) item { ActionRow("حذف الوجهة المحفوظة", "إزالة الموقع من الجهاز", Icons.Default.FavoriteBorder, onClearFavorite) }
        item { ActionRow("طرق الدفع", "الدفع النقدي متاح حاليًا", Icons.Default.CreditCard) {} }
        item { ActionRow("الأمان والخصوصية", "إعدادات الحساب والحماية", Icons.Default.Security) {} }
        item { ActionRow("المساعدة والدعم", "الأسئلة والمشاكل الشائعة", Icons.Default.HelpOutline, onSupport) }
        item { ActionRow("عن وصلها", "معلومات التطبيق", Icons.Default.Info, onAbout) }
        item { OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = WaslhaDanger)) { Icon(Icons.Default.Logout, null, tint = WaslhaDanger); Spacer(Modifier.width(7.dp)); Text("تسجيل الخروج", color = WaslhaDanger, fontWeight = FontWeight.Bold) } }
    }
}

@Composable private fun Section(text: String) { Text(text, color = WaslhaMuted, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp)) }
@Composable private fun ToggleRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onChanged: (Boolean) -> Unit) { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = WaslhaGreen, modifier = Modifier.size(25.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = WaslhaInk, fontWeight = FontWeight.Bold); Text(subtitle, color = WaslhaMuted, fontSize = 10.sp) }; Switch(checked, onCheckedChange = onChanged) } } }
@Composable private fun ActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit = {}) { Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = WaslhaGreen, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, color = WaslhaInk, fontWeight = FontWeight.Bold); Text(subtitle, color = WaslhaMuted, fontSize = 10.sp) }; Icon(Icons.Default.ChevronLeft, null, tint = WaslhaMuted) } } }

@Composable private fun DestinationDialog(favorite: TaxiDestination?, onDismiss: () -> Unit, onMap: () -> Unit, onSelect: (TaxiDestination) -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("اختيار الوجهة", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { favorite?.let { ActionRow("المحفوظة", it.title, Icons.Default.Favorite) { onSelect(it) } }; TaxiDestinations.forEach { ActionRow(it.title, it.subtitle, Icons.Default.LocationOn) { onSelect(it) } }; ActionRow("اختيار من الخريطة", "حدد موقعًا دقيقًا", Icons.Default.MyLocation, onMap) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = WaslhaGreen) } }) }

@Composable private fun MapPickerScreen(pickup: Coordinates, destination: Coordinates?, onBack: () -> Unit, onPicked: (Coordinates) -> Unit) { Box(Modifier.fillMaxSize().background(Color.White)) { WaslhaRideMap(pickup = pickup, destination = destination, modifier = Modifier.fillMaxSize(), onDestinationPicked = onPicked); Card(Modifier.align(Alignment.TopCenter).padding(top = 14.dp, start = 16.dp, end = 16.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(4.dp)) { Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = WaslhaInk) }; Text("حرّك الخريطة وحدد وجهتك", color = WaslhaInk, fontWeight = FontWeight.Black, fontSize = 12.sp) } } } }

@Composable private fun ActiveTrip(trip: Trip, pickup: Coordinates?, destination: TaxiDestination?, loading: Boolean, error: String?, onRefresh: () -> Unit, onCancel: () -> Unit, onShare: () -> Unit, onDone: () -> Unit) { val terminal = trip.status == "completed" || trip.status == "cancelled"; LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 22.dp)) { item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onDone) { Icon(Icons.Default.ArrowBack, "رجوع") }; Spacer(Modifier.weight(1f)); Text("الرحلة الحالية", color = WaslhaInk, fontSize = 24.sp, fontWeight = FontWeight.Black) } }; if (pickup != null && destination != null) item { Card(Modifier.fillMaxWidth().height(230.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(3.dp)) { WaslhaRideMap(pickup, destination.coordinates, Modifier.fillMaxSize()) { } } }; item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(23.dp), elevation = CardDefaults.cardElevation(2.dp)) { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(trip.status, color = if (trip.status == "cancelled") WaslhaDanger else WaslhaGreen, fontSize = 20.sp, fontWeight = FontWeight.Black); Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = WaslhaMuted, fontSize = 11.sp) }; Text("${trip.estimatedFare} ${trip.currency}", color = WaslhaInk, fontSize = 18.sp, fontWeight = FontWeight.Black) }; Info("نوع السيارة", trip.vehicleType); Info("الدفع", trip.paymentMethod); error?.let { Text(it, color = WaslhaDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onRefresh, enabled = !loading, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { if (loading) CircularProgressIndicator(Modifier.size(17.dp), color = WaslhaGreen, strokeWidth = 2.dp) else Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("تحديث") }; OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(15.dp)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(4.dp)); Text("مشاركة") } }; if (!terminal) OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = WaslhaDanger)) { Text("إلغاء الرحلة", fontWeight = FontWeight.Bold) } else Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(49.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = WaslhaGreen)) { Text("العودة إلى رحلاتي", fontWeight = FontWeight.Black) } } } } }
}

@Composable private fun Info(title: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(title, color = WaslhaMuted, fontSize = 11.sp); Text(value, color = WaslhaInk, fontWeight = FontWeight.Bold, fontSize = 11.sp) } }
@Composable private fun TripDetailsDialog(trip: Trip, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("تفاصيل الرحلة", fontWeight = FontWeight.Black) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Info("الحالة", trip.status); Info("الأجرة", "${trip.estimatedFare} ${trip.currency}"); Info("المسافة", "${trip.distanceKm} كم"); Info("المدة", "${trip.durationMin} دقيقة"); Info("السيارة", trip.vehicleType); Info("الدفع", trip.paymentMethod) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = WaslhaGreen) } }) }
@Composable private fun SupportDialog(onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("المساعدة والدعم", fontWeight = FontWeight.Black) }, text = { Text("حدد الانطلاق والوجهة، اختر نوع التكسي، ثم اضغط طلب التكسي. يمكنك متابعة الرحلة وإلغاؤها من شاشة الرحلة الحالية.", color = WaslhaMuted) }, confirmButton = { TextButton(onClick = onDismiss) { Text("حسنًا", color = WaslhaGreen) } }) }
@Composable private fun AboutDialog(onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text("عن وصلها", fontWeight = FontWeight.Black) }, text = { Column { Text("وصلها", color = WaslhaGreen, fontSize = 28.sp, fontWeight = FontWeight.Black); Spacer(Modifier.height(5.dp)); Text("تطبيق تكسي لحجز المشاوير داخل سوريا.", color = WaslhaInk); Text("واجهة Material 3 محسنة للأجهزة المحمولة.", color = WaslhaMuted, fontSize = 11.sp) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = WaslhaGreen) } }) }

private fun localEstimate(from: Coordinates, to: Coordinates, multiplier: Double): FareEstimate {
    val dx = (to.lng - from.lng) * 85.0
    val dy = (to.lat - from.lat) * 111.0
    val km = sqrt(dx * dx + dy * dy).coerceAtLeast(0.2)
    return FareEstimate(km, (km * 3.0).toInt().coerceAtLeast(3), "ل.س", (2500 + km * 1200 * multiplier).toInt())
}

private fun shareTrip(context: Context, trip: Trip) {
    val text = "رحلة وصلها\nالحالة: ${trip.status}\nالأجرة: ${trip.estimatedFare} ${trip.currency}\nالمسافة: ${trip.distanceKm} كم\nالمدة: ${trip.durationMin} دقيقة"
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text) }, "مشاركة الرحلة"))
}
