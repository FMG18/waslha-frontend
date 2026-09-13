package com.waslha.app

import android.Manifest
import android.content.Context
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CPrimary = Color(0xFF087F5B)
private val CPrimaryDark = Color(0xFF055C42)
private val CAccent = Color(0xFFB8E986)
private val CInk = Color(0xFF12201B)
private val CMuted = Color(0xFF6D7A75)
private val CBackground = Color(0xFFF7F9F8)
private val CSoft = Color(0xFFE7F6F0)
private val CSoft2 = Color(0xFFF0F5F2)
private val CLine = Color(0xFFDDE5E1)
private val CWhite = Color.White
private val CDanger = Color(0xFFB42318)
private val CPurple = Color(0xFF6F4DBA)

sealed interface CustomerPage { data object Home : CustomerPage; data object Trips : CustomerPage; data object Profile : CustomerPage; data object Settings : CustomerPage; data object Notifications : CustomerPage; data object Payments : CustomerPage; data object SavedPlaces : CustomerPage; data object Support : CustomerPage; data object About : CustomerPage; data object EditProfile : CustomerPage; data object Map : CustomerPage }

data class V2Destination(val title: String, val subtitle: String, val coordinates: Coordinates)
data class V2Vehicle(val id: String, val title: String, val subtitle: String, val multiplier: Double)

val v2Vehicles = listOf(V2Vehicle("economy", "اقتصادي", "الأفضل للسعر", 1.0), V2Vehicle("comfort", "مريح", "راحة ومساحة أفضل", 1.2), V2Vehicle("family", "عائلي", "مساحة أكبر للركاب", 1.35))

class WaslhaCustomerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); ApiProvider.init(this)
        val sessionStore = SessionStore(this); val locationProvider = LocationProvider(this)
        setContent { WaslhaTheme { WaslhaCustomerApp(sessionStore, locationProvider) { sessionStore.clear(); finish() } } }
    }
}

@Composable
private fun WaslhaCustomerApp(sessionStore: SessionStore, locationProvider: LocationProvider, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TripRepository(ApiProvider.api) }
    var page by remember { mutableStateOf<CustomerPage>(CustomerPage.Home) }
    var pickup by remember { mutableStateOf<Coordinates?>(null) }
    var pickupLabel by remember { mutableStateOf("جاري تحديد موقعك…") }
    var destination by remember { mutableStateOf<V2Destination?>(null) }
    var vehicle by remember { mutableStateOf(v2Vehicles.first()) }
    var estimate by remember { mutableStateOf<FareEstimate?>(null) }
    var locationLoading by remember { mutableStateOf(false) }
    var estimateLoading by remember { mutableStateOf(false) }
    var requesting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var tripsLoading by remember { mutableStateOf(false) }

    fun refreshLocation() {
        locationLoading = true
        scope.launch {
            val location = locationProvider.lastKnown()
            if (location != null) {
                pickup = Coordinates(location.latitude, location.longitude)
                pickupLabel = "موقعك الحالي"
                error = null
            } else {
                pickupLabel = "تعذر تحديد الموقع"
                error = "فعّل GPS وامنح وصلها صلاحية الموقع"
            }
            locationLoading = false
        }
    }

    val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) refreshLocation()
        else error = "صلاحية الموقع مطلوبة لاستخدام التكسي"
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) refreshLocation()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(pickup, destination, vehicle.id) {
        val from = pickup ?: return@LaunchedEffect
        val to = destination?.coordinates ?: return@LaunchedEffect
        estimateLoading = true
        repo.estimate(from, to, vehicle.id)
            .onSuccess { estimate = it }
            .onFailure {
                val dx = (to.lng - from.lng) * 85.0
                val dy = (to.lat - from.lat) * 111.0
                val km = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(0.2)
                estimate = FareEstimate(km, (km * 3.0).toInt().coerceAtLeast(3), "ل.س", (2500 + km * 1200 * vehicle.multiplier).toInt())
            }
        estimateLoading = false
    }

    LaunchedEffect(page) {
        if (page == CustomerPage.Trips) {
            val id = sessionStore.userId ?: return@LaunchedEffect
            tripsLoading = true
            repo.list(id).onSuccess { trips = it }.onFailure { error = it.message ?: "تعذر جلب الرحلات" }
            tripsLoading = false
        }
    }

    LaunchedEffect(trip?.id) {
        val activeId = trip?.id ?: return@LaunchedEffect
        while (true) {
            delay(5000)
            val result = repo.get(activeId).getOrNull() ?: continue
            trip = result
            if (result.status == "completed" || result.status == "cancelled") break
        }
    }

    Surface(Modifier.fillMaxSize(), color = CBackground) {
        when (page) {
            CustomerPage.Home -> CustomerScaffold(page, { page = it }) {
                CustomerHomeMap(
                    pickup ?: Coordinates(33.5138, 36.2765),
                    pickupLabel,
                    destination,
                    vehicle,
                    estimate,
                    estimateLoading,
                    locationLoading,
                    requesting,
                    error,
                    { if (permissionGranted) refreshLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    { coords -> destination = V2Destination("الموقع المحدد", "من الخريطة", coords) },
                    { vehicle = it }
                ) { paymentMethod ->
                    val from = pickup
                    val to = destination
                    val userId = sessionStore.userId
                    when {
                        from == null -> error = "حدد موقع الانطلاق أولاً"
                        to == null -> error = "حدد وجهتك أولاً"
                        userId.isNullOrBlank() -> error = "بيانات الحساب غير مكتملة"
                        else -> {
                            requesting = true
                            error = null
                            scope.launch {
                                repo.create(TripRequest(userId, from, to.coordinates, vehicle.id, paymentMethod))
                                    .onSuccess { trip = it }
                                    .onFailure { error = it.message ?: "تعذر طلب التكسي" }
                                requesting = false
                            }
                        }
                    }
                }
            }
            CustomerPage.Trips -> CustomerScaffold(page, { page = it }) { CustomerTrips(trips, tripsLoading, error) { page = CustomerPage.Trips } }
            CustomerPage.Profile -> CustomerScaffold(page, { page = it }) { CustomerProfile(sessionStore, { page = it }, onLogout) }
            CustomerPage.Settings -> CustomerSubPage("الإعدادات", { page = CustomerPage.Profile }) { SimpleSettings { page = CustomerPage.Notifications } }
            CustomerPage.Notifications -> CustomerSubPage("الإشعارات", { page = CustomerPage.Profile }) { NotificationPage() }
            CustomerPage.Payments -> CustomerSubPage("طرق الدفع", { page = CustomerPage.Profile }) { PaymentPage() }
            CustomerPage.SavedPlaces -> CustomerSubPage("الأماكن المحفوظة", { page = CustomerPage.Profile }) { SavedPlacePage() }
            CustomerPage.Support -> CustomerSubPage("المساعدة والدعم", { page = CustomerPage.Profile }) { SupportPage() }
            CustomerPage.About -> CustomerSubPage("عن وصلها", { page = CustomerPage.Profile }) { AboutPage() }
            CustomerPage.EditProfile -> CustomerSubPage("تعديل الحساب", { page = CustomerPage.Profile }) { EditProfilePage(sessionStore) { page = CustomerPage.Profile } }
            CustomerPage.Map -> MapDestinationPage(pickup ?: Coordinates(33.5138, 36.2765), destination?.coordinates, { page = CustomerPage.Home }) { coords -> destination = V2Destination("الموقع المحدد", "من الخريطة", coords); page = CustomerPage.Home }
        }
        if (trip != null && page != CustomerPage.Map) {
            ActiveTripCardOverlay(trip!!, { page = CustomerPage.Trips }) {
                val id = trip?.id ?: return@ActiveTripCardOverlay
                scope.launch { repo.cancel(id, "إلغاء من الراكب").onSuccess { trip = it } }
            }
        }
    }
}

@Composable
private fun CustomerScaffold(
    page: CustomerPage,
    onPage: (CustomerPage) -> Unit,
    content: @Composable () -> Unit
) {
    val selected = when (page) {
        CustomerPage.Home, CustomerPage.Map -> 0
        CustomerPage.Trips -> 1
        else -> 2
    }

    Scaffold(
        containerColor = CBackground,
        bottomBar = {
            NavigationBar(
                containerColor = CWhite,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .navigationBarsPadding()
                    .clip(RoundedCornerShape(22.dp)),
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
            ) {
                NavigationBarItem(
                    selected = selected == 0,
                    onClick = { onPage(CustomerPage.Home) },
                    icon = { Icon(Icons.Default.Home, contentDescription = "الرئيسية") },
                    label = { Text("الرئيسية", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CPurple,
                        selectedTextColor = CPurple,
                        indicatorColor = Color(0xFFF0EBF8),
                        unselectedIconColor = CMuted,
                        unselectedTextColor = CMuted
                    )
                )
                NavigationBarItem(
                    selected = selected == 1,
                    onClick = { onPage(CustomerPage.Trips) },
                    icon = { Icon(Icons.Default.History, contentDescription = "رحلاتي") },
                    label = { Text("رحلاتي", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CPurple,
                        selectedTextColor = CPurple,
                        indicatorColor = Color(0xFFF0EBF8),
                        unselectedIconColor = CMuted,
                        unselectedTextColor = CMuted
                    )
                )
                NavigationBarItem(
                    selected = selected == 2,
                    onClick = { onPage(CustomerPage.Profile) },
                    icon = { Icon(Icons.Default.Person, contentDescription = "حسابي") },
                    label = { Text("حسابي", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = CPurple,
                        selectedTextColor = CPurple,
                        indicatorColor = Color(0xFFF0EBF8),
                        unselectedIconColor = CMuted,
                        unselectedTextColor = CMuted
                    )
                )
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            content()
        }
    }
}

@Composable
private fun RoutePoint(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Boolean, loading: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(if (accent) Color.White else Color.White.copy(alpha = .12f)),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(if (accent) CSoft else Color.White.copy(alpha = .14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (accent) CPrimary else Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = if (accent) CMuted else Color.White.copy(alpha = .68f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(value, color = if (accent) CInk else Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (loading) CircularProgressIndicator(Modifier.size(19.dp), color = if (accent) CPrimary else Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Default.ChevronLeft, null, tint = if (accent) CMuted else Color.White.copy(alpha = .72f), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RouteConnector() {
    Row(Modifier.padding(start = 18.dp, top = 1.dp, bottom = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(2.dp).height(12.dp).background(CAccent))
        Spacer(Modifier.width(8.dp))
        Text("مسار الرحلة", color = Color.White.copy(alpha = .55f), fontSize = 8.sp)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column {
        Text(title, color = CInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(subtitle, color = CMuted, fontSize = 11.sp)
    }
}

@Composable
private fun VehicleRow(vehicle: V2Vehicle, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(if (selected) CSoft else CWhite),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) CPrimary else CLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(if (selected) CPrimary else CSoft2), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DirectionsCar, null, tint = if (selected) Color.White else CPrimary, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(vehicle.title, color = CInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    if (vehicle.id == "economy") {
                        Spacer(Modifier.width(7.dp))
                        Text("شائع", color = CPrimary, fontWeight = FontWeight.Bold, fontSize = 8.sp)
                    }
                }
                Text(vehicle.subtitle, color = CMuted, fontSize = 10.sp)
            }
            if (selected) {
                Box(Modifier.size(25.dp).clip(CircleShape).background(CPrimary), contentAlignment = Alignment.Center) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            } else {
                Icon(Icons.Default.ChevronLeft, null, tint = CMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SimpleSettings(onNotifications: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ProfileAction("الإشعارات", "إدارة تنبيهات الرحلات", Icons.Default.Notifications, onNotifications)
        ProfileAction("الخصوصية والأمان", "حماية بيانات الحساب", Icons.Default.Shield) { }
        ProfileAction("اللغة", "العربية", Icons.Default.Person) { }
    }
}
