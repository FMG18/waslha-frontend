package com.waslha.app

import android.Manifest
import android.content.Context
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
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
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

private sealed interface CustomerPage {
    data object Home : CustomerPage
    data object Trips : CustomerPage
    data object Profile : CustomerPage
    data object Settings : CustomerPage
    data object Notifications : CustomerPage
    data object Payments : CustomerPage
    data object SavedPlaces : CustomerPage
    data object Support : CustomerPage
    data object About : CustomerPage
    data object EditProfile : CustomerPage
    data object Map : CustomerPage
}

private data class V2Destination(val title: String, val subtitle: String, val coordinates: Coordinates)
private data class V2Vehicle(val id: String, val title: String, val subtitle: String, val multiplier: Double)

private val v2Vehicles = listOf(
    V2Vehicle("economy", "اقتصادي", "الأفضل للسعر", 1.0),
    V2Vehicle("comfort", "مريح", "راحة ومساحة أفضل", 1.2),
    V2Vehicle("family", "عائلي", "مساحة أكبر للركاب", 1.35)
)

class WaslhaCustomerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val sessionStore = SessionStore(this)
        val locationProvider = LocationProvider(this)
        setContent {
            WaslhaTheme {
                WaslhaCustomerApp(
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
}

@Composable
private fun WaslhaCustomerApp(
    sessionStore: SessionStore,
    locationProvider: LocationProvider,
    onLogout: () -> Unit
) {
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

    val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

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
                CustomerHome(
                    sessionStore, pickupLabel, destination, vehicle, estimate, estimateLoading, locationLoading, requesting, error,
                    onRefreshLocation = { if (permissionGranted) refreshLocation() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
                    onDestination = { page = CustomerPage.Map },
                    onVehicle = { vehicle = it },
                    onRequest = {
                        val from = pickup
                        val to = destination
                        val userId = sessionStore.userId
                        when {
                            from == null -> error = "حدد موقع الانطلاق أولاً"
                            to == null -> page = CustomerPage.Map
                            userId.isNullOrBlank() -> error = "بيانات الحساب غير مكتملة"
                            else -> {
                                requesting = true
                                error = null
                                scope.launch {
                                    repo.create(TripRequest(userId, from, to.coordinates, vehicle.id, "cash"))
                                        .onSuccess { trip = it }
                                        .onFailure { error = it.message ?: "تعذر طلب التكسي" }
                                    requesting = false
                                }
                            }
                        }
                    }
                )
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
            CustomerPage.Map -> MapDestinationPage(
                pickup ?: Coordinates(33.5138, 36.2765), destination?.coordinates,
                onBack = { page = CustomerPage.Home },
                onPicked = { coords -> destination = V2Destination("الموقع المحدد", "من الخريطة", coords); page = CustomerPage.Home }
            )
        }

        if (trip != null && page != CustomerPage.Map) {
            ActiveTripCardOverlay(
                trip!!,
                onOpen = { page = CustomerPage.Trips },
                onCancel = {
                    val id = trip?.id ?: return@ActiveTripCardOverlay
                    scope.launch { repo.cancel(id, "إلغاء من الراكب").onSuccess { trip = it } }
                }
            )
        }
    }
}

@Composable
private fun CustomerScaffold(page: CustomerPage, onPage: (CustomerPage) -> Unit, content: @Composable () -> Unit) {
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
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(selected == 0, { onPage(CustomerPage.Home) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("الرئيسية") })
                NavigationBarItem(selected == 1, { onPage(CustomerPage.Trips) }, icon = { Icon(Icons.Default.History, null) }, label = { Text("رحلاتي") })
                NavigationBarItem(selected == 2, { onPage(CustomerPage.Profile) }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("حسابي") })
            }
        }
    ) { padding -> Box(Modifier.fillMaxSize().padding(padding)) { content() } }
}

@Composable
private fun CustomerHome(
    sessionStore: SessionStore,
    pickupLabel: String,
    destination: V2Destination?,
    vehicle: V2Vehicle,
    estimate: FareEstimate?,
    estimateLoading: Boolean,
    locationLoading: Boolean,
    requesting: Boolean,
    error: String?,
    onRefreshLocation: () -> Unit,
    onDestination: () -> Unit,
    onVehicle: (V2Vehicle) -> Unit,
    onRequest: () -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 24.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("أهلًا${sessionStore.name?.takeIf { it.isNotBlank() }?.let { "، $it" } ?: " بك"}", color = CInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text("جاهز لمشوارك؟", color = CMuted, fontSize = 12.sp)
                }
                Box(Modifier.size(48.dp).clip(CircleShape).background(CSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.AccountCircle, "الحساب", tint = CPrimary, modifier = Modifier.size(32.dp))
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(CPrimary), elevation = CardDefaults.cardElevation(0.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("وين نوصلك؟", color = Color.White.copy(alpha = .72f), fontSize = 12.sp)
                            Text("احجز تكسي خلال ثوانٍ", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                        }
                        Box(Modifier.size(42.dp).clip(CircleShape).background(CAccent), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DirectionsCar, null, tint = CPrimaryDark, modifier = Modifier.size(23.dp))
                        }
                    }
                    LocationRow("نقطة الانطلاق", pickupLabel, Icons.Default.MyLocation, onRefreshLocation, locationLoading, emphasized = false)
                    LocationRow("الوجهة", destination?.title ?: "اختيار الوجهة من الخريطة", Icons.Default.LocationOn, onDestination, false, emphasized = true)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("اختر نوع التكسي", "الاختيار الأنسب لمشوارك")
                v2Vehicles.forEach { option -> VehicleRow(option, option.id == vehicle.id) { onVehicle(option) } }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CWhite), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CLine)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(CSoft), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.CreditCard, null, tint = CPrimary, modifier = Modifier.size(21.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text("الدفع نقداً", color = CInk, fontWeight = FontWeight.Bold)
                        Text(if (estimateLoading) "جاري حساب الأجرة…" else estimate?.let { "التقدير ${it.estimatedFare} ${it.currency}" } ?: "اختر الوجهة لحساب الأجرة", color = CMuted, fontSize = 11.sp)
                    }
                    if (estimate != null) Text("${estimate!!.distanceKm.toInt()} كم", color = CPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
        item {
            if (!error.isNullOrBlank()) {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFFFFF1F0)), shape = RoundedCornerShape(15.dp)) {
                    Text(error, Modifier.padding(12.dp), color = CDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
            }
            Button(
                onClick = onRequest,
                enabled = !requesting,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CPurple)
            ) {
                if (requesting) CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp)
                else Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(21.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("اطلب التكسي", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = CPrimary, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(7.dp))
                Text("تجربة حجز بسيطة وواضحة داخل سوريا", color = CMuted, fontSize = 10.sp)
            }
        }
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
private fun LocationRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, loading: Boolean, emphasized: Boolean) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(if (emphasized) Color.White else Color.White.copy(alpha = .12f)),
        shape = RoundedCornerShape(17.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(39.dp).clip(CircleShape).background(if (emphasized) CSoft else Color.White.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (emphasized) CPrimary else Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = if (emphasized) CMuted else Color.White.copy(alpha = .72f), fontSize = 10.sp)
                Text(value, color = if (emphasized) CInk else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            if (loading) CircularProgressIndicator(Modifier.size(19.dp), color = if (emphasized) CPrimary else Color.White, strokeWidth = 2.dp)
            else Icon(Icons.Default.ChevronLeft, null, tint = if (emphasized) CMuted else Color.White.copy(alpha = .75f))
        }
    }
}

@Composable
private fun VehicleRow(vehicle: V2Vehicle, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(if (selected) CSoft else CWhite),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) CPrimary else CLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(if (selected) CPrimary else CSoft2), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.DirectionsCar, null, tint = if (selected) Color.White else CPrimary, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(vehicle.title, color = CInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(vehicle.subtitle, color = CMuted, fontSize = 10.sp)
            }
            if (selected) {
                Box(Modifier.size(25.dp).clip(CircleShape).background(CPrimary), contentAlignment = Alignment.Center) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CustomerTrips(trips: List<Trip>, loading: Boolean, error: String?, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلاتي", color = CInk, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("رحلاتك السابقة والحالية", color = CMuted, fontSize = 12.sp)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث", tint = CPrimary) }
        }
        Spacer(Modifier.height(10.dp))
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CPrimary) }
        else if (trips.isEmpty()) EmptyState(Icons.Default.History, "لا توجد رحلات بعد", "بعد أول حجز ستظهر تفاصيل رحلتك هنا")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
            items(trips) { trip ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CWhite), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CLine)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(trip.statusLabel(), color = CPrimary, fontWeight = FontWeight.Black)
                            Text("${trip.estimatedFare} ${trip.currency}", color = CInk, fontWeight = FontWeight.Black)
                        }
                        Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = CMuted, fontSize = 11.sp)
                        Text("${trip.vehicleType} • ${trip.paymentMethod}", color = CMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        if (!error.isNullOrBlank()) Text(error, color = CDanger, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(78.dp).clip(CircleShape).background(CSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = CPrimary, modifier = Modifier.size(39.dp))
        }
        Spacer(Modifier.height(13.dp))
        Text(title, color = CInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(subtitle, color = CMuted, fontSize = 11.sp)
    }
}

@Composable
private fun CustomerProfile(sessionStore: SessionStore, onPage: (CustomerPage) -> Unit, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CPrimary), shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(60.dp).clip(CircleShape).background(CAccent), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountCircle, null, tint = CPrimaryDark, modifier = Modifier.size(39.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sessionStore.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(sessionStore.email ?: sessionStore.phone ?: "بيانات الحساب", color = Color.White.copy(alpha = .74f), fontSize = 11.sp)
                    }
                    IconButton(onClick = { onPage(CustomerPage.EditProfile) }) { Icon(Icons.Default.ChevronLeft, "تعديل", tint = Color.White) }
                }
            }
        }
        item { ProfileAction("الأماكن المحفوظة", "المنزل والعمل والمفضلة", Icons.Default.LocationOn) { onPage(CustomerPage.SavedPlaces) } }
        item { ProfileAction("طرق الدفع", "طريقة الدفع المتاحة", Icons.Default.CreditCard) { onPage(CustomerPage.Payments) } }
        item { ProfileAction("الإشعارات", "تنبيهات الرحلات والتحديثات", Icons.Default.Notifications) { onPage(CustomerPage.Notifications) } }
        item { ProfileAction("الإعدادات", "تفضيلات التطبيق والخصوصية", Icons.Default.Settings) { onPage(CustomerPage.Settings) } }
        item { ProfileAction("المساعدة والدعم", "الأسئلة والحلول", Icons.Default.HelpOutline) { onPage(CustomerPage.Support) } }
        item { ProfileAction("عن وصلها", "معلومات الإصدار والتطبيق", Icons.Default.Info) { onPage(CustomerPage.About) } }
        item {
            OutlinedButton(onClick = onLogout, Modifier.fillMaxWidth().height(51.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = CDanger)) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(7.dp))
                Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(CWhite), shape = RoundedCornerShape(19.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CLine), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(CSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = CPrimary, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = CInk, fontWeight = FontWeight.Bold)
                Text(subtitle, color = CMuted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = CMuted)
        }
    }
}

@Composable
private fun CustomerSubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = CInk) }
            Spacer(Modifier.width(4.dp))
            Text(title, color = CInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp))
        content()
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

@Composable
private fun NotificationPage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingToggle("تحديثات الرحلة", "تنبيهك عند تغيّر حالة الحجز", true)
        SettingToggle("العروض", "التنبيهات المتعلقة بالعروض", true)
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, initial: Boolean) {
    var enabled by remember { mutableStateOf(initial) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CWhite), shape = RoundedCornerShape(19.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CLine)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(CSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Notifications, null, tint = CPrimary, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = CInk)
                Text(subtitle, color = CMuted, fontSize = 10.sp)
            }
            Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
    }
}

@Composable
private fun PaymentPage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAction("الدفع نقداً", "الدفع للكابتن بعد انتهاء الرحلة", Icons.Default.CreditCard) { }
        ProfileAction("محفظة وصلها", "جاهزة لإضافة الرصيد مستقبلاً", Icons.Default.CreditCard) { }
        Text("الدفع النقدي هو الخيار المتاح حالياً لرحلات التكسي.", color = CMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun SavedPlacePage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAction("المنزل", "أضف موقع المنزل لاحقاً", Icons.Default.Home) { }
        ProfileAction("العمل", "أضف موقع العمل لاحقاً", Icons.Default.LocationOn) { }
        Text("الأماكن محفوظة كواجهة حالياً، وسيتم ربط الحفظ الدائم مع نظام البيانات لاحقاً.", color = CMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun SupportPage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAction("كيف أطلب تكسي؟", "اختر موقعك والوجهة ثم اضغط طلب التكسي", Icons.Default.HelpOutline) { }
        ProfileAction("كيف ألغي الرحلة؟", "من بطاقة الرحلة الحالية", Icons.Default.HelpOutline) { }
        ProfileAction("مشكلة في الحساب", "راجع الاتصال ثم أعد تسجيل الدخول", Icons.Default.HelpOutline) { }
    }
}

@Composable
private fun AboutPage() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CPrimary), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(CAccent), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = CPrimaryDark, modifier = Modifier.size(27.dp)) }
            Text("وصلها", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("تطبيق تكسي لحجز المشاوير داخل سوريا.", color = Color.White.copy(alpha = .86f), fontSize = 13.sp)
            Text("النسخة الحالية: 1.3.5", color = Color.White.copy(alpha = .68f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun EditProfilePage(sessionStore: SessionStore, onSaved: () -> Unit) {
    var name by remember { mutableStateOf(sessionStore.name.orEmpty()) }
    var phone by remember { mutableStateOf(sessionStore.phone.orEmpty()) }
    var email by remember { mutableStateOf(sessionStore.email.orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("الاسم") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("البريد الإلكتروني") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        Button(onClick = { sessionStore.updateProfile(name.trim(), phone.trim(), email.trim()); onSaved() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = CPrimary)) {
            Text("حفظ التغييرات", fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun MapDestinationPage(pickup: Coordinates, selected: Coordinates?, onBack: () -> Unit, onPicked: (Coordinates) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        WaslhaRideMap(pickup = pickup, destination = selected, modifier = Modifier.fillMaxSize(), onDestinationPicked = onPicked)
        Card(Modifier.align(Alignment.TopStart).padding(14.dp), colors = CardDefaults.cardColors(CWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = CInk) }
        }
        Card(Modifier.align(Alignment.BottomCenter).padding(16.dp), colors = CardDefaults.cardColors(CWhite.copy(alpha = .98f)), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(6.dp)) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(CPrimary)) {}
                Spacer(Modifier.height(7.dp))
                Text("حدد وجهتك على الخريطة", color = CInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text("حرّك الخريطة حتى يكون المؤشر على المكان المطلوب", color = CMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun ActiveTripCardOverlay(trip: Trip, onOpen: () -> Unit, onCancel: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(CWhite), shape = RoundedCornerShape(22.dp), elevation = CardDefaults.cardElevation(10.dp), border = androidx.compose.foundation.BorderStroke(1.dp, CLine)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(43.dp).clip(CircleShape).background(CSoft), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = CPrimary) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(trip.statusLabel(), color = CInk, fontWeight = FontWeight.Black)
                Text("${trip.distanceKm} كم • ${trip.estimatedFare} ${trip.currency}", color = CMuted, fontSize = 10.sp)
            }
            TextButton(onClick = onOpen) { Text("فتح", color = CPrimary, fontWeight = FontWeight.Bold) }
            if (trip.status == "searching" || trip.status == "driver_assigned" || trip.status == "arriving") TextButton(onClick = onCancel) { Text("إلغاء", color = CDanger, fontWeight = FontWeight.Bold) }
        }
    }
}
