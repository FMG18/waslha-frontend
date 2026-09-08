package com.waslha.app

import android.Manifest
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val Green = Color(0xFF078A60)
private val GreenDark = Color(0xFF056C4B)
private val GreenSoft = Color(0xFFE8F6F0)
private val Ink = Color(0xFF10201B)
private val Muted = Color(0xFF72807B)
private val AppBg = Color(0xFFF5F8F6)
private val CardBg = Color.White
private val MapBg = Color(0xFFDCE9E3)
private val Danger = Color(0xFFB42318)

private data class PlaceOption(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val coordinates: Coordinates
)

private val DamascusPlaces = listOf(
    PlaceOption("ساحة الأمويين", "دمشق", "📍", Coordinates(33.5138, 36.2765)),
    PlaceOption("جامعة دمشق", "المزة - دمشق", "🎓", Coordinates(33.5101, 36.2766)),
    PlaceOption("سوق الحميدية", "المدينة القديمة", "🛍️", Coordinates(33.5112, 36.3051)),
    PlaceOption("المزة", "دمشق", "🏙️", Coordinates(33.4941, 36.2384)),
    PlaceOption("محطة الحجاز", "دمشق", "🚉", Coordinates(33.5070, 36.2895))
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionStore = SessionStore(this)
        val locationProvider = LocationProvider(this)
        setContent { WaslhaApp(sessionStore, locationProvider) }
    }
}

@Composable
private fun WaslhaApp(sessionStore: SessionStore, locationProvider: LocationProvider) {
    var signedIn by remember { mutableStateOf(sessionStore.isSignedIn) }
    val authRepository = remember { AuthRepository(ApiProvider.api, sessionStore) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = AppBg) {
            if (signedIn) {
                PassengerShell(
                    sessionStore = sessionStore,
                    locationProvider = locationProvider,
                    onLogout = {
                        authRepository.signOut()
                        signedIn = false
                    }
                )
            } else {
                LoginScreen(authRepository) { signedIn = true }
            }
        }
    }
}

@Composable
private fun LoginScreen(authRepository: AuthRepository, onAuthenticated: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var devCode by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize().background(AppBg)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(64.dp))
            Box(Modifier.size(84.dp).clip(CircleShape).background(Green), Alignment.Center) {
                Text("و", fontSize = 48.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.height(14.dp))
            Text("وصلها", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Green)
            Text("مشوارك يبدأ هنا", color = Muted, fontSize = 13.sp)
            Spacer(Modifier.height(34.dp))

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(CardBg),
                shape = RoundedCornerShape(28.dp),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text(
                        if (step == 0) "أهلاً بك في وصلها" else "تأكيد رقم الهاتف",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black,
                        color = Ink
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (step == 0) "سجّل دخولك حتى تبدأ حجز مشوارك" else "أدخل رمز التحقق المرسل إلى رقمك",
                        color = Muted,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(20.dp))

                    if (step == 0) {
                        TextField(
                            value = phone,
                            onValueChange = { phone = it.filter(Char::isDigit).take(15) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("رقم الهاتف") },
                            placeholder = { Text("مثال: 093xxxxxxxx") },
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            enabled = phone.length >= 8 && !loading,
                            onClick = {
                                loading = true
                                error = null
                                scope.launch {
                                    authRepository.requestCode(normalizePhoneForApi(phone))
                                        .onSuccess {
                                            devCode = it.devCode
                                            otp = it.devCode.orEmpty()
                                            step = 1
                                        }
                                        .onFailure { error = it.message ?: "تعذر إرسال رمز التحقق" }
                                    loading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("إرسال رمز التحقق", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        TextField(
                            value = otp,
                            onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("رمز التحقق") },
                            placeholder = { Text("123456") },
                            shape = RoundedCornerShape(16.dp)
                        )
                        devCode?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("رمز الاختبار: $it", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            enabled = otp.length == 6 && !loading,
                            onClick = {
                                loading = true
                                error = null
                                scope.launch {
                                    authRepository.verifyCode(normalizePhoneForApi(phone), otp)
                                        .onSuccess { onAuthenticated() }
                                        .onFailure { error = it.message ?: "رمز التحقق غير صحيح" }
                                    loading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("دخول إلى وصلها", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { step = 0; error = null; devCode = null }) {
                            Text("تغيير رقم الهاتف")
                        }
                    }
                    error?.let {
                        Spacer(Modifier.height(10.dp))
                        Card(colors = CardDefaults.cardColors(Color(0xFFFFF1F1)), shape = RoundedCornerShape(12.dp)) {
                            Text(it, Modifier.padding(12.dp), color = Danger, fontSize = 11.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = Green, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("بياناتك محمية ونستخدمها فقط لتقديم الخدمة", color = Muted, fontSize = 10.sp)
            }
        }
    }
}

private fun normalizePhoneForApi(value: String): String = when {
    value.startsWith("+") -> value
    value.startsWith("963") -> "+$value"
    value.startsWith("0") -> "+963${value.drop(1)}"
    else -> "+963$value"
}

@Composable
private fun PassengerShell(
    sessionStore: SessionStore,
    locationProvider: LocationProvider,
    onLogout: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var destination by remember { mutableStateOf<PlaceOption?>(null) }
    var destinationOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Coordinates?>(null) }
    var locationDenied by remember { mutableStateOf(false) }
    var mapDestination by remember { mutableStateOf<Coordinates?>(null) }
    val locationScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current
    val tripViewModel: PassengerTripViewModel = viewModel()
    val tripState by tripViewModel.state.collectAsState()
    val permissionGrantedNow = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    var permissionGranted by remember { mutableStateOf(permissionGrantedNow) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        permissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!permissionGranted) locationDenied = true
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            location = locationProvider.lastKnown()?.let { Coordinates(it.latitude, it.longitude) }
        }
    }

    val pickup = location ?: Coordinates(33.5138, 36.2765)
    val activeTrip = (tripState as? TripUiState.Success)?.trip
    val tripFlow = tripState is TripUiState.Loading || activeTrip != null
    val showNav = !settingsOpen && !tripFlow

    Scaffold(
        containerColor = AppBg,
        bottomBar = {
            if (showNav) PassengerBottomBar(tab, onTab = { tab = it })
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                settingsOpen -> SettingsScreen(onBack = { settingsOpen = false })
                tripFlow -> TripFlowScreen(
                    state = tripState,
                    onCancel = { id, reason -> tripViewModel.cancelTrip(id, reason) },
                    onBack = { tripViewModel.reset() },
                    onRefresh = { activeTrip?.let { tripViewModel.loadTrip(it.id) } }
                )
                tab == 0 -> HomeScreen(
                    destination = destination,
                    pickup = pickup,
                    locationDenied = locationDenied,
                    onDestination = { destinationOpen = true },
                    mapDestination = mapDestination,
                    onMapDestinationPicked = { mapDestination = it },
                    onRefreshLocation = {
                        if (!permissionGranted) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        } else {
                            locationScope.launch {
                                location = locationProvider.lastKnown()?.let { Coordinates(it.latitude, it.longitude) }
                            }
                        }
                    },
                    onRequest = {
                        destination?.let { selected ->
                            tripViewModel.createTrip(
                                TripRequest(
                                    customerId = sessionStore.userId ?: "guest",
                                    pickup = pickup,
                                    destination = selected.coordinates,
                                    vehicleType = "economy",
                                    paymentMethod = "cash"
                                )
                            )
                        }
                    }
                )
                tab == 1 -> TripsScreen(sessionStore.userId)
                else -> ProfileScreen(sessionStore, onSettings = { settingsOpen = true }, onLogout = onLogout)
            }
        }
    }

    if (destinationOpen) {
        DestinationPicker(
            selected = destination,
            onDismiss = { destinationOpen = false },
            onSelect = { destination = it; mapDestination = it.coordinates; destinationOpen = false }
        )
    }
}

@Composable
private fun HomeScreen(
    destination: PlaceOption?,
    mapDestination: Coordinates?,
    pickup: Coordinates,
    locationDenied: Boolean,
    onDestination: () -> Unit,
    onMapDestinationPicked: (Coordinates) -> Unit,
    onRefreshLocation: () -> Unit,
    onRequest: () -> Unit
) {
    var selectedType by remember { mutableIntStateOf(0) }
    val vehicles = listOf(
        Triple("اقتصادي", "3,500 ل.س", "3–5 د"),
        Triple("مريح", "5,000 ل.س", "4–6 د"),
        Triple("عائلي", "6,500 ل.س", "5–8 د")
    )

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f)) {
            WaslhaRideMap(
                pickup = pickup,
                destination = mapDestination ?: destination?.coordinates,
                modifier = Modifier.fillMaxSize(),
                onDestinationPicked = onMapDestinationPicked
            )
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).background(Green, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("وصلها", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Green)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefreshLocation) { Icon(Icons.Default.Refresh, "تحديث الموقع", tint = Ink) }
                IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "الإشعارات", tint = Ink) }
            }
            if (locationDenied) {
                Card(Modifier.align(Alignment.BottomCenter).padding(14.dp), colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(14.dp)) {
                    Text("فعّل إذن الموقع للحصول على نقطة انطلاق أدق", Modifier.padding(10.dp), color = Muted, fontSize = 10.sp)
                }
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            colors = CardDefaults.cardColors(CardBg),
            elevation = CardDefaults.cardElevation(7.dp)
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("وين نوصلك؟", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text("اختار وجهتك ونوع السيارة", color = Muted, fontSize = 12.sp)
                    }
                    Card(colors = CardDefaults.cardColors(GreenSoft), shape = CircleShape) {
                        Text("1", Modifier.padding(horizontal = 11.dp, vertical = 7.dp), color = Green, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Card(
                    Modifier.fillMaxWidth().clickable(onClick = onDestination),
                    colors = CardDefaults.cardColors(AppBg),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFE1E9E5))
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).background(GreenSoft, CircleShape), Alignment.Center) {
                            Icon(Icons.Default.Search, null, tint = Green, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الوجهة", fontSize = 10.sp, color = Muted)
                            Text(destination?.title ?: "ابحث عن مكان أو عنوان", fontWeight = FontWeight.Bold, color = Ink)
                            destination?.subtitle?.let { Text(it, fontSize = 9.sp, color = Muted) }
                        }
                        Icon(Icons.Default.ChevronLeft, null, tint = Muted)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("اختار فئة السيارة", fontWeight = FontWeight.Black, color = Ink)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    vehicles.forEachIndexed { index, item ->
                        VehicleCard(item.first, item.second, item.third, index == selectedType) { selectedType = index }
                    }
                }
                Spacer(Modifier.height(13.dp))
                Button(
                    onClick = onRequest,
                    enabled = destination != null,
                    modifier = Modifier.fillMaxWidth().height(55.dp),
                    shape = RoundedCornerShape(17.dp)
                ) {
                    Icon(Icons.Default.DirectionsCar, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (destination == null) "حدد وجهتك أولاً" else "اطلب سيارة الآن", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.VehicleCard(name: String, price: String, eta: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.weight(1f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(if (selected) GreenSoft else AppBg),
        shape = RoundedCornerShape(15.dp),
        border = if (selected) BorderStroke(1.dp, Green.copy(alpha = .45f)) else BorderStroke(1.dp, Color(0xFFE7ECE9))
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 9.dp, horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚕", fontSize = 22.sp)
            Text(name, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(price, color = Green, fontSize = 10.sp, fontWeight = FontWeight.Black)
            Text(eta, color = Muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun DestinationPicker(selected: PlaceOption?, onDismiss: () -> Unit, onSelect: (PlaceOption) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حدد وجهتك", fontWeight = FontWeight.Black, color = Ink) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                items(DamascusPlaces) { place ->
                    Card(
                        Modifier.fillMaxWidth().clickable { onSelect(place) },
                        colors = CardDefaults.cardColors(if (selected?.title == place.title) GreenSoft else AppBg),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(place.emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(place.title, fontWeight = FontWeight.Bold, color = Ink)
                                Text(place.subtitle, color = Muted, fontSize = 10.sp)
                            }
                            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun TripFlowScreen(
    state: TripUiState,
    onCancel: (String, String) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    var cancelOpen by remember { mutableStateOf(false) }

    when (state) {
        TripUiState.Idle -> Unit
        TripUiState.Loading -> CenterTripState("نرسل طلبك الآن", "جاري البحث عن أقرب كابتن", true, onBack, "إلغاء")
        is TripUiState.Error -> CenterTripState("تعذر تنفيذ الطلب", state.message, false, onBack, "العودة للرئيسية")
        is TripUiState.Success -> {
            val trip = state.trip
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") }
                    Column(Modifier.weight(1f)) {
                        Text("رحلتك الحالية", fontSize = 23.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text(trip.statusLabel(), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث", tint = Green) }
                }
                Spacer(Modifier.height(10.dp))
                Card(Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(MapBg), shape = RoundedCornerShape(24.dp)) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocationOn, null, tint = Green, modifier = Modifier.size(58.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("التتبع المباشر", fontWeight = FontWeight.Black, color = Ink, fontSize = 18.sp)
                            Text("سيظهر مسار الرحلة وموقع الكابتن هنا", color = Muted, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("رحلة #${trip.id.takeLast(6)}", fontWeight = FontWeight.Black, color = Ink)
                                Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = Muted, fontSize = 10.sp)
                            }
                            Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(9.dp))
                        RouteRow("الانطلاق", "${trip.pickup.lat}, ${trip.pickup.lng}", true)
                        RouteRow("الوجهة", "${trip.destination.lat}, ${trip.destination.lng}", false)
                        Spacer(Modifier.height(9.dp))
                        TripStatusRow(trip.status)
                        trip.driver?.let {
                            Spacer(Modifier.height(9.dp))
                            DriverRow(it)
                        }
                        Spacer(Modifier.height(10.dp))
                        if (trip.status in listOf("searching", "driver_assigned", "arriving", "in_progress")) {
                            OutlinedButton(onClick = { cancelOpen = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                                Text("إلغاء الرحلة")
                            }
                        } else {
                            Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                                Text("العودة للرئيسية")
                            }
                        }
                    }
                }
            }
        }
    }

    if (cancelOpen) {
        CancelTripDialog(
            onDismiss = { cancelOpen = false },
            onConfirm = { reason ->
                cancelOpen = false
                val trip = (state as? TripUiState.Success)?.trip ?: return@CancelTripDialog
                onCancel(trip.id, reason)
            }
        )
    }
}

@Composable
private fun CenterTripState(title: String, message: String, loading: Boolean, onAction: () -> Unit, actionText: String) {
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(90.dp))
        Box(Modifier.size(122.dp).background(GreenSoft, CircleShape), Alignment.Center) {
            if (loading) CircularProgressIndicator(color = Green, modifier = Modifier.size(54.dp))
            else Icon(Icons.Default.Refresh, null, tint = Green, modifier = Modifier.size(54.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(25.dp))
        OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Text(actionText)
        }
    }
}

@Composable
private fun TripStatusRow(status: String) {
    val text = when (status) {
        "searching" -> "جاري البحث عن كابتن قريب منك"
        "driver_assigned" -> "تم العثور على كابتن"
        "arriving" -> "الكابتن في الطريق إلى موقعك"
        "in_progress" -> "الرحلة بدأت"
        "completed" -> "اكتملت الرحلة بنجاح"
        "cancelled" -> "تم إلغاء الرحلة"
        else -> "حالة الرحلة: $status"
    }
    Card(colors = CardDefaults.cardColors(GreenSoft), shape = RoundedCornerShape(13.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(11.dp), color = GreenDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun DriverRow(driver: Driver) {
    Row(Modifier.fillMaxWidth().background(AppBg, RoundedCornerShape(15.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).background(GreenSoft, CircleShape), Alignment.Center) {
            Icon(Icons.Default.Person, null, tint = Green, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(driver.name, fontWeight = FontWeight.Black, color = Ink)
            Text("${driver.vehicle} • ${driver.plate}", fontSize = 10.sp, color = Muted)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, null, tint = Green, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(3.dp))
            Text("${driver.rating}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RouteRow(label: String, value: String, start: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).background(if (start) Green else Ink, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Muted)
            Text(value, fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TripsScreen(customerId: String?) {
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repository = remember { TripRepository(ApiProvider.api) }

    fun loadTrips() {
        loading = true
        error = null
        scope.launch {
            repository.list(customerId)
                .onSuccess { trips = it }
                .onFailure { error = it.message ?: "تعذر جلب الرحلات" }
            loading = false
        }
    }

    LaunchedEffect(customerId) { loadTrips() }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلاتي", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Ink)
                Text("كل مشاويرك في مكان واحد", color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = ::loadTrips) { Icon(Icons.Default.Refresh, "تحديث", tint = Green) }
        }
        Spacer(Modifier.height(14.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) { CircularProgressIndicator(color = Green) }
            error != null -> EmptyTrips("تعذر تحميل الرحلات", error!!)
            trips.isEmpty() -> EmptyTrips("لا توجد رحلات بعد", "رحلتك الأولى تبدأ من الصفحة الرئيسية")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 12.dp)) {
                items(trips) { trip ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(19.dp)) {
                        Column(Modifier.padding(15.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(42.dp).background(GreenSoft, CircleShape), Alignment.Center) {
                                    Icon(Icons.Default.DirectionsCar, null, tint = Green, modifier = Modifier.size(22.dp))
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("رحلة #${trip.id.takeLast(6)}", fontWeight = FontWeight.Black, color = Ink)
                                    Text(trip.statusLabel(), fontSize = 10.sp, color = Muted)
                                }
                                Text("${trip.estimatedFare} ${trip.currency}", fontWeight = FontWeight.Black, color = Green, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(10.dp))
                            Divider(color = Color(0xFFE8EEEB))
                            Spacer(Modifier.height(8.dp))
                            Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", fontSize = 10.sp, color = Muted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.EmptyTrips(title: String, message: String) {
    Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(88.dp).background(GreenSoft, CircleShape), Alignment.Center) {
            Icon(Icons.Default.CalendarMonth, null, tint = Green, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(13.dp))
        Text(title, fontWeight = FontWeight.Black, color = Ink, fontSize = 19.sp)
        Text(message, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ProfileScreen(sessionStore: SessionStore, onSettings: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("حسابي", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(23.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).background(Green, CircleShape), Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(31.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("عميل وصلها", fontWeight = FontWeight.Black, color = Ink, fontSize = 17.sp)
                    Text(sessionStore.phone ?: "رقم الهاتف غير متاح", color = Muted, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        AccountItem("الإشعارات", "تنبيهات الرحلات والعروض", Icons.Default.NotificationsNone)
        AccountItem("طرق الدفع", "الدفع النقدي والخيارات القادمة", Icons.Default.Tune)
        AccountItem("الأمان والخصوصية", "إدارة أمان الحساب", Icons.Default.Security)
        AccountItem("الإعدادات", "اللغة والتفضيلات", Icons.Default.Settings, onSettings)
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.Logout, null)
            Spacer(Modifier.width(7.dp))
            Text("تسجيل الخروج", color = Danger, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AccountItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)? = null) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(CardBg),
        shape = RoundedCornerShape(17.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(GreenSoft, CircleShape), Alignment.Center) {
                Icon(icon, null, tint = Green, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Ink, fontSize = 13.sp)
                Text(subtitle, color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    Scaffold(
        containerColor = AppBg,
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات", fontWeight = FontWeight.Black) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
            SettingsRow("الإشعارات", "التحكم بتنبيهات الرحلات", Icons.Default.NotificationsNone)
            SettingsRow("اللغة", "العربية", Icons.Default.Tune)
            SettingsRow("الأمان", "إعدادات الحساب والحماية", Icons.Default.Security)
            SettingsRow("عن وصلها", "الإصدار ومعلومات التطبيق", Icons.Default.LocationOn)
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(CardBg), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Ink)
                Text(subtitle, color = Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}

@Composable
private fun PassengerBottomBar(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp,
        modifier = Modifier.navigationBarsPadding()
    ) {
        NavigationBarItem(
            selected = tab == 0,
            onClick = { onTab(0) },
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("الرئيسية") }
        )
        NavigationBarItem(
            selected = tab == 1,
            onClick = { onTab(1) },
            icon = { Icon(Icons.Default.DirectionsCar, null) },
            label = { Text("رحلاتي") }
        )
        NavigationBarItem(
            selected = tab == 2,
            onClick = { onTab(2) },
            icon = { Icon(Icons.Default.Person, null) },
            label = { Text("حسابي") }
        )
    }
}
