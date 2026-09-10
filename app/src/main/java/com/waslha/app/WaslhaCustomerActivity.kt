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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Home
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

private val CGreen = Color(0xFF078A60)
private val CInk = Color(0xFF10201B)
private val CMuted = Color(0xFF71807A)
private val CBackground = Color(0xFFF4F7F5)
private val CSoft = Color(0xFFE8F6F0)
private val CDanger = Color(0xFFB42318)

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

private val v2Destinations = listOf(
    V2Destination("ساحة الأمويين", "دمشق", Coordinates(33.5138, 36.2765)),
    V2Destination("جامعة دمشق", "المزة - دمشق", Coordinates(33.5101, 36.2766)),
    V2Destination("سوق الحميدية", "المدينة القديمة", Coordinates(33.5112, 36.3051)),
    V2Destination("المزة", "دمشق", Coordinates(33.4941, 36.2384)),
    V2Destination("محطة الحجاز", "دمشق", Coordinates(33.5070, 36.2895))
)

private val v2Vehicles = listOf(
    V2Vehicle("economy", "اقتصادي", "سعر مناسب", 1.0),
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

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            refreshLocation()
        } else {
            error = "صلاحية الموقع مطلوبة لاستخدام التكسي"
        }
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) refreshLocation() else permissionLauncher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
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

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = CBackground) {
            when (page) {
                CustomerPage.Home -> CustomerScaffold(page, { page = it }) {
                    CustomerHome(
                        sessionStore = sessionStore,
                        pickupLabel = pickupLabel,
                        destination = destination,
                        vehicle = vehicle,
                        estimate = estimate,
                        estimateLoading = estimateLoading,
                        locationLoading = locationLoading,
                        requesting = requesting,
                        error = error,
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
                                            .onFailure { error = it.message ?: "تعذر طلب التاكسي" }
                                        requesting = false
                                    }
                                }
                            }
                        }
                    )
                }
                CustomerPage.Trips -> CustomerScaffold(page, { page = it }) {
                    CustomerTrips(trips, tripsLoading, error) {
                        page = CustomerPage.Trips
                    }
                }
                CustomerPage.Profile -> CustomerScaffold(page, { page = it }) {
                    CustomerProfile(
                        sessionStore = sessionStore,
                        onPage = { page = it },
                        onLogout = onLogout
                    )
                }
                CustomerPage.Settings -> CustomerSubPage("الإعدادات", { page = CustomerPage.Profile }) {
                    SimpleSettings { page = CustomerPage.Notifications }
                }
                CustomerPage.Notifications -> CustomerSubPage("الإشعارات", { page = CustomerPage.Profile }) {
                    NotificationPage()
                }
                CustomerPage.Payments -> CustomerSubPage("طرق الدفع", { page = CustomerPage.Profile }) {
                    PaymentPage()
                }
                CustomerPage.SavedPlaces -> CustomerSubPage("الأماكن المحفوظة", { page = CustomerPage.Profile }) {
                    SavedPlacePage()
                }
                CustomerPage.Support -> CustomerSubPage("المساعدة والدعم", { page = CustomerPage.Profile }) {
                    SupportPage()
                }
                CustomerPage.About -> CustomerSubPage("عن وصلها", { page = CustomerPage.Profile }) {
                    AboutPage()
                }
                CustomerPage.EditProfile -> CustomerSubPage("تعديل الحساب", { page = CustomerPage.Profile }) {
                    EditProfilePage(sessionStore) { page = CustomerPage.Profile }
                }
                CustomerPage.Map -> MapDestinationPage(
                    pickup = pickup ?: Coordinates(33.5138, 36.2765),
                    selected = destination?.coordinates,
                    onBack = { page = CustomerPage.Home },
                    onPicked = { coords ->
                        destination = V2Destination("الموقع المحدد", "من الخريطة", coords)
                        page = CustomerPage.Home
                    }
                )
            }

            if (trip != null && page != CustomerPage.Map) {
                ActiveTripCardOverlay(
                    trip = trip!!,
                    onOpen = { page = CustomerPage.Trips },
                    onCancel = {
                        val id = trip?.id ?: return@ActiveTripCardOverlay
                        scope.launch { repo.cancel(id, "إلغاء من الراكب").onSuccess { trip = it } }
                    }
                )
            }
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
            NavigationBar(containerColor = Color.White, modifier = Modifier.navigationBarsPadding()) {
                NavigationBarItem(selected == 0, { onPage(CustomerPage.Home) }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("الرئيسية") })
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
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 14.dp, bottom = 20.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("وصلها", color = CGreen, fontSize = 33.sp, fontWeight = FontWeight.Black)
                    Text(sessionStore.name?.takeIf { it.isNotBlank() }?.let { "أهلًا $it" } ?: "احجز تكسي بسهولة", color = CMuted, fontSize = 13.sp)
                }
                Icon(Icons.Default.Help, "المساعدة", tint = CGreen, modifier = Modifier.size(26.dp))
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    Text("إلى أين؟", color = CInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("حدد الانطلاق والوجهة ثم اطلب السيارة", color = CMuted, fontSize = 11.sp)
                    LocationRow("نقطة الانطلاق", pickupLabel, Icons.Default.MyLocation, onRefreshLocation, locationLoading)
                    LocationRow("الوجهة", destination?.title ?: "اختيار الوجهة من الخريطة", Icons.Default.LocationOn, onDestination, false)
                    Text("نوع التكسي", color = CInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    v2Vehicles.forEach { option ->
                        VehicleRow(option, option.id == vehicle.id) { onVehicle(option) }
                    }
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CSoft), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, null, tint = CGreen)
                            Spacer(Modifier.size(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("الدفع نقداً", color = CInk, fontWeight = FontWeight.Bold)
                                Text(if (estimateLoading) "نحسب الأجرة…" else estimate?.let { "${it.estimatedFare} ${it.currency}" } ?: "اختر الوجهة لحساب الأجرة", color = CMuted, fontSize = 11.sp)
                            }
                        }
                    }
                    if (!error.isNullOrBlank()) Text(error, color = CDanger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onRequest, enabled = !requesting, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F4DBA))) {
                        if (requesting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("طلب التكسي", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = CGreen, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.size(10.dp))
                    Column {
                        Text("وصلها تكسي", color = CInk, fontWeight = FontWeight.Black)
                        Text("نقل ركاب داخل سوريا", color = CMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationRow(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, loading: Boolean) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(Color(0xFFF6F8F7)), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = CGreen, modifier = Modifier.size(28.dp))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = CMuted, fontSize = 10.sp)
                Text(value, color = CInk, fontWeight = FontWeight.Bold)
            }
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = CGreen, strokeWidth = 2.dp)
            else Icon(Icons.Default.ChevronLeft, null, tint = CMuted)
        }
    }
}

@Composable
private fun VehicleRow(vehicle: V2Vehicle, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(if (selected) CSoft else Color.White), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Color(0xFFF4F7F6), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = CGreen) }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(vehicle.title, color = CInk, fontWeight = FontWeight.Black)
                Text(vehicle.subtitle, color = CMuted, fontSize = 10.sp)
            }
            if (selected) Text("✓", color = CGreen, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
    }
}

@Composable
private fun CustomerTrips(trips: List<Trip>, loading: Boolean, error: String?, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلاتي", color = CInk, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("كل رحلات التكسي الخاصة بك", color = CMuted, fontSize = 12.sp)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث", tint = CGreen) }
        }
        if (loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = CGreen) }
        else if (trips.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.History, null, tint = CMuted, modifier = Modifier.size(52.dp)); Text("لا توجد رحلات بعد", color = CInk, fontWeight = FontWeight.Bold); Text("عندما تطلب تكسي ستظهر الرحلة هنا", color = CMuted, fontSize = 11.sp) } }
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(trips) { trip ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(trip.statusLabel(), color = CGreen, fontWeight = FontWeight.Black)
                            Text("${trip.estimatedFare} ${trip.currency}", color = CInk, fontWeight = FontWeight.Black)
                        }
                        Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = CMuted, fontSize = 11.sp)
                        Text("${trip.vehicleType} • ${trip.paymentMethod}", color = CMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        if (!error.isNullOrBlank()) Text(error, color = CDanger, fontSize = 11.sp)
    }
}

@Composable
private fun CustomerProfile(sessionStore: SessionStore, onPage: (CustomerPage) -> Unit, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Icon(Icons.Default.AccountCircle, null, tint = CGreen, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(7.dp))
                    Text(sessionStore.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", color = CInk, fontSize = 21.sp, fontWeight = FontWeight.Black)
                    Text(sessionStore.email ?: sessionStore.phone ?: "بيانات الحساب", color = CMuted, fontSize = 12.sp)
                    TextButton(onClick = { onPage(CustomerPage.EditProfile) }) { Text("تعديل الحساب", color = CGreen, fontWeight = FontWeight.Bold) }
                }
            }
        }
        item { ProfileAction("الأماكن المحفوظة", "المنزل والعمل والمفضلة", Icons.Default.LocationOn) { onPage(CustomerPage.SavedPlaces) } }
        item { ProfileAction("طرق الدفع", "النقد والدفع المتاح", Icons.Default.Payment) { onPage(CustomerPage.Payments) } }
        item { ProfileAction("الإشعارات", "تنبيهات الرحلات والعروض", Icons.Default.Notifications) { onPage(CustomerPage.Notifications) } }
        item { ProfileAction("الإعدادات", "تفضيلات التطبيق والخصوصية", Icons.Default.Settings) { onPage(CustomerPage.Settings) } }
        item { ProfileAction("المساعدة والدعم", "الأسئلة والمشاكل الشائعة", Icons.Default.Help) { onPage(CustomerPage.Support) } }
        item { ProfileAction("عن وصلها", "معلومات عن التطبيق", Icons.Default.Home) { onPage(CustomerPage.About) } }
        item {
            OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = CDanger)) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.size(7.dp))
                Text("تسجيل الخروج", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ProfileAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = CGreen, modifier = Modifier.size(25.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = CInk, fontWeight = FontWeight.Bold); Text(subtitle, color = CMuted, fontSize = 10.sp) }
            Icon(Icons.Default.ChevronLeft, null, tint = CMuted)
        }
    }
}

@Composable
private fun CustomerSubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = CInk) }
            Spacer(Modifier.weight(1f))
            Text(title, color = CInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(13.dp))
        content()
    }
}

@Composable
private fun SimpleSettings(onNotifications: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        ProfileAction("الإشعارات", "إدارة إشعارات الرحلات", Icons.Default.Notifications, onNotifications)
        ProfileAction("الخصوصية والأمان", "حماية بيانات الحساب", Icons.Default.Settings) { }
        ProfileAction("اللغة", "العربية", Icons.Default.Person) { }
    }
}

@Composable
private fun NotificationPage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Notifications, null, tint = CGreen); Spacer(Modifier.size(11.dp)); Column { Text("حالة الرحلة", fontWeight = FontWeight.Bold, color = CInk); Text("تنبيهات عند قبول الرحلة وتغيّر حالتها", color = CMuted, fontSize = 11.sp) } } }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, tint = CGreen); Spacer(Modifier.size(11.dp)); Column { Text("العروض", fontWeight = FontWeight.Bold, color = CInk); Text("خصومات وتنبيهات من وصلها", color = CMuted, fontSize = 11.sp) } } }
    }
}

@Composable
private fun PaymentPage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAction("الدفع نقداً", "الدفع للكابتن بعد انتهاء الرحلة", Icons.Default.Payment) { }
        ProfileAction("محفظة وصلها", "الخيار متاح للرصيد مستقبلاً", Icons.Default.Payment) { }
        Text("حالياً يتم اعتماد الدفع النقدي في طلبات التكسي.", color = CMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SavedPlacePage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAction("المنزل", "أضف موقع المنزل لاحقاً", Icons.Default.Home) { }
        ProfileAction("العمل", "أضف موقع العمل لاحقاً", Icons.Default.LocationOn) { }
        Text("الأماكن هنا جاهزة للربط بالحفظ الدائم عندما نضيف قاعدة بيانات الأماكن.", color = CMuted, fontSize = 11.sp)
    }
}

@Composable
private fun SupportPage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAction("كيف أطلب تكسي؟", "اختر الموقع والوجهة ثم اطلب السيارة", Icons.Default.Help) { }
        ProfileAction("كيف ألغي الرحلة؟", "من بطاقة الرحلة الحالية", Icons.Default.Help) { }
        ProfileAction("مشكلة في الحساب", "تأكد من الاتصال ثم أعد تسجيل الدخول", Icons.Default.Help) { }
    }
}

@Composable
private fun AboutPage() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("وصلها", color = CGreen, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("تطبيق تكسي لحجز المشاوير داخل سوريا.", color = CInk, fontSize = 14.sp)
            Text("النسخة الحالية: 1.3.5", color = CMuted, fontSize = 11.sp)
            Text("نركز على تجربة حجز بسيطة وسريعة وآمنة.", color = CMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun EditProfilePage(sessionStore: SessionStore, onSaved: () -> Unit) {
    var name by remember { mutableStateOf(sessionStore.name.orEmpty()) }
    var phone by remember { mutableStateOf(sessionStore.phone.orEmpty()) }
    var email by remember { mutableStateOf(sessionStore.email.orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("الاسم") }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, singleLine = true)
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("البريد الإلكتروني") }, singleLine = true)
        Button(onClick = { sessionStore.updateProfile(name.trim(), phone.trim(), email.trim()); onSaved() }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = CGreen)) { Text("حفظ التغييرات", fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun MapDestinationPage(pickup: Coordinates, selected: Coordinates?, onBack: () -> Unit, onPicked: (Coordinates) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.White)) {
        WaslhaRideMap(pickup = pickup, destination = selected, modifier = Modifier.fillMaxSize(), onDestinationPicked = onPicked)
        IconButton(onClick = onBack, modifier = Modifier.padding(top = 14.dp, start = 14.dp)) { Icon(Icons.Default.ArrowBack, "رجوع", tint = CInk) }
        Card(Modifier.align(Alignment.BottomCenter).padding(16.dp), colors = CardDefaults.cardColors(Color.White.copy(alpha = .97f)), shape = RoundedCornerShape(18.dp)) {
            Text("حرّك الخريطة حتى يصل المؤشر إلى وجهتك ثم اضغط على المكان", modifier = Modifier.padding(13.dp), color = CInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ActiveTripCardOverlay(trip: Trip, onOpen: () -> Unit, onCancel: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(8.dp)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DirectionsCar, null, tint = CGreen, modifier = Modifier.size(30.dp))
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(trip.statusLabel(), color = CInk, fontWeight = FontWeight.Black)
                Text("${trip.distanceKm} كم • ${trip.estimatedFare} ${trip.currency}", color = CMuted, fontSize = 10.sp)
            }
            TextButton(onClick = onOpen) { Text("فتح", color = CGreen) }
            if (trip.status == "searching" || trip.status == "driver_assigned" || trip.status == "arriving") TextButton(onClick = onCancel) { Text("إلغاء", color = CDanger) }
        }
    }
}
