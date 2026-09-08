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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private val Green = Color(0xFF078A60)
private val GreenDark = Color(0xFF056C4B)
private val Ink = Color(0xFF10201B)
private val Muted = Color(0xFF6D7B76)
private val SurfaceBg = Color(0xFFF3F7F5)
private val MapBg = Color(0xFFDCE8E2)

private data class PlaceOption(val title: String, val subtitle: String, val emoji: String, val coordinates: Coordinates)

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
    val api = remember { ApiProvider.api }
    val authRepository = remember { AuthRepository(api, sessionStore) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = SurfaceBg) {
            if (signedIn) {
                PassengerApp(
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
    var devHint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(70.dp))
        Text("وصلها", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Green)
        Text("رحلتك .. بأمان وراحة", color = Muted)
        Spacer(Modifier.height(40.dp))

        if (step == 0) {
            Text("مرحباً بك", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("سجل دخولك برقم الهاتف", color = Muted)
            Spacer(Modifier.height(22.dp))
            TextField(
                value = phone,
                onValueChange = { phone = it.filter(Char::isDigit).take(15) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("مثال: 9639xxxxxxxx") }
            )
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = phone.length >= 8 && !loading,
                onClick = {
                    loading = true
                    error = null
                    scope.launch {
                        authRepository.requestCode(normalizePhoneForApi(phone))
                            .onSuccess { response ->
                                devHint = response.devCode
                                otp = response.devCode.orEmpty()
                                step = 1
                            }
                            .onFailure { error = it.message ?: "تعذر إرسال رمز التحقق" }
                        loading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("إرسال رمز التحقق")
            }
        } else {
            Text("رمز التحقق", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("أدخل الرمز المرسل إلى ${normalizePhoneForApi(phone)}", color = Muted)
            Spacer(Modifier.height(22.dp))
            TextField(
                value = otp,
                onValueChange = { otp = it.filter(Char::isDigit).take(6) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("123456") }
            )
            devHint?.let {
                Spacer(Modifier.height(9.dp))
                Card(colors = CardDefaults.cardColors(Green.copy(alpha = .10f)), shape = RoundedCornerShape(13.dp)) {
                    Text("رمز الاختبار: $it", Modifier.padding(12.dp), color = Green, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
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
                if (loading) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                else Text("دخول إلى وصلها")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { step = 0; error = null; devHint = null }) { Text("تغيير الرقم") }
        }

        error?.let {
            Spacer(Modifier.height(14.dp))
            Card(colors = CardDefaults.cardColors(Color(0xFFFFF0F0)), shape = RoundedCornerShape(14.dp)) {
                Text(it, Modifier.padding(13.dp), color = Color(0xFFB42318), fontSize = 12.sp)
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
private fun PassengerApp(
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
    val tripViewModel: PassengerTripViewModel = viewModel()
    val tripState by tripViewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { TripRepository(ApiProvider.api) }

    val hasLocation = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
    var locationPermissionGranted by remember { mutableStateOf(hasLocation) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        locationPermissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!locationPermissionGranted) locationDenied = true
    }

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) location = locationProvider.lastKnown()?.let { Coordinates(it.latitude, it.longitude) }
    }

    val pickup = location ?: Coordinates(33.5138, 36.2765)
    val activeTrip = (tripState as? TripUiState.Success)?.trip
    val isTripFlow = tripState is TripUiState.Loading || activeTrip != null

    Box(Modifier.fillMaxSize()) {
        when {
            settingsOpen -> SettingsScreen(onBack = { settingsOpen = false })
            isTripFlow -> TripFlowScreen(
                state = tripState,
                onCancel = { id, reason -> tripViewModel.cancelTrip(id, reason) },
                onBack = {
                    tripViewModel.clear()
                },
                onRefresh = { activeTrip?.let(tripViewModel::loadTrip) }
            )
            tab == 0 -> HomeScreen(
                destination = destination,
                pickup = pickup,
                locationDenied = locationDenied,
                onDestination = { destinationOpen = true },
                onRefreshLocation = {
                    scope.launch {
                        if (!locationPermissionGranted) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        } else {
                            location = locationProvider.lastKnown()?.let { Coordinates(it.latitude, it.longitude) }
                        }
                    }
                },
                onRequest = {
                    val selected = destination ?: return@HomeScreen
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
            )
            tab == 1 -> TripsScreen(sessionStore.userId)
            else -> ProfileScreen(onLogout = onLogout, onSettings = { settingsOpen = true }, sessionStore = sessionStore)
        }

        if (!isTripFlow && !settingsOpen) BottomBar(tab) { tab = it }
    }

    if (destinationOpen) {
        DestinationPicker(
            selected = destination,
            onDismiss = { destinationOpen = false },
            onSelect = {
                destination = it
                destinationOpen = false
            }
        )
    }
}

@Composable
private fun HomeScreen(
    destination: PlaceOption?,
    pickup: Coordinates,
    locationDenied: Boolean,
    onDestination: () -> Unit,
    onRefreshLocation: () -> Unit,
    onRequest: () -> Unit
) {
    var selected by remember { mutableIntStateOf(0) }
    val types = listOf(
        Triple("اقتصادي", "3,500 ل.س", "3-5 د"),
        Triple("مريح", "5,000 ل.س", "4-6 د"),
        Triple("عائلي", "6,500 ل.س", "5-8 د")
    )
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f).background(MapBg)) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(92.dp).background(Green.copy(alpha = .14f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.LocationOn, null, tint = Green, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("موقعك الحالي", fontWeight = FontWeight.Black, color = Ink, fontSize = 18.sp)
                Text(
                    "${"%.4f".format(pickup.lat)}, ${"%.4f".format(pickup.lng)}",
                    color = Muted,
                    fontSize = 11.sp
                )
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).background(Green, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("وصلها", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Green)
                    }
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefreshLocation) { Icon(Icons.Default.Refresh, "تحديث الموقع", tint = Ink) }
                IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "الإشعارات", tint = Ink) }
            }
            if (locationDenied) {
                Card(Modifier.align(Alignment.BottomCenter).padding(14.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(15.dp)) {
                    Text("فعّل إذن الموقع لتحسين نقطة الانطلاق", Modifier.padding(11.dp), color = Muted, fontSize = 11.sp)
                }
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp).navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("وين نوصلك؟", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text("اختار الوجهة وشوف السعر قبل الطلب", color = Muted, fontSize = 12.sp)
                    }
                    Card(colors = CardDefaults.cardColors(Green.copy(alpha = .10f)), shape = CircleShape) {
                        Text("1/3", modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Card(Modifier.fillMaxWidth().clickable(onClick = onDestination), colors = CardDefaults.cardColors(SurfaceBg), shape = RoundedCornerShape(17.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(36.dp).background(Green.copy(alpha = .10f), CircleShape), Alignment.Center) { Icon(Icons.Default.Search, null, tint = Green, modifier = Modifier.size(19.dp)) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الوجهة", fontSize = 10.sp, color = Muted)
                            Text(destination?.title ?: "ابحث عن مكان أو عنوان", fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
                            destination?.subtitle?.let { Text(it, fontSize = 10.sp, color = Muted) }
                        }
                        Icon(Icons.Default.ChevronLeft, null, tint = Muted)
                    }
                }
                Spacer(Modifier.height(13.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("نوع السيارة", fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.weight(1f))
                    Text("السعر تقديري", fontSize = 10.sp, color = Muted)
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEachIndexed { index, item ->
                        Box(Modifier.weight(1f)) { CarType(item.first, item.second, item.third, index == selected) { selected = index } }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onRequest,
                    enabled = destination != null,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
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
private fun CarType(name: String, price: String, eta: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(if (selected) Green.copy(alpha = .11f) else SurfaceBg),
        shape = RoundedCornerShape(15.dp),
        border = if (selected) BorderStroke(1.dp, Green.copy(alpha = .35f)) else null
    ) {
        Column(Modifier.fillMaxWidth().padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚕", fontSize = 21.sp)
            Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Ink)
            Text(price, fontSize = 10.sp, color = Green, fontWeight = FontWeight.Bold)
            Text(eta, fontSize = 9.sp, color = Muted)
        }
    }
}

@Composable
private fun DestinationPicker(selected: PlaceOption?, onDismiss: () -> Unit, onSelect: (PlaceOption) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حدد وجهتك", fontWeight = FontWeight.Black) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(DamascusPlaces) { place ->
                    Card(
                        colors = CardDefaults.cardColors(if (selected?.title == place.title) Green.copy(alpha = .10f) else SurfaceBg),
                        shape = RoundedCornerShape(15.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(place) }
                    ) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(place.emoji, fontSize = 20.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(place.title, fontWeight = FontWeight.Bold, color = Ink)
                                Text(place.subtitle, fontSize = 10.sp, color = Muted)
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
    var cancelDialog by remember { mutableStateOf(false) }
    when (state) {
        TripUiState.Idle -> Unit
        TripUiState.Loading -> Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(90.dp))
            Box(Modifier.size(116.dp).background(Green.copy(alpha = .10f), CircleShape), Alignment.Center) {
                CircularProgressIndicator(color = Green, modifier = Modifier.size(55.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("نبحث لك عن كابتن", fontSize = 27.sp, fontWeight = FontWeight.Black, color = Ink)
            Text("جاري إرسال طلب الرحلة", color = Muted)
            Spacer(Modifier.height(28.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("إلغاء") }
        }
        is TripUiState.Error -> Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(90.dp))
            Text("تعذر تنفيذ الطلب", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(10.dp))
            Text(state.message, color = Muted)
            Spacer(Modifier.height(22.dp))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("العودة للرئيسية") }
        }
        is TripUiState.Success -> {
            val trip = state.trip
            Column(Modifier.fillMaxSize().padding(18.dp).navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") }
                    Column(Modifier.weight(1f)) {
                        Text("رحلتك", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Ink)
                        Text(trip.statusLabel(), color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث", tint = Green) }
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().weight(1f).background(MapBg, RoundedCornerShape(24.dp)), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.LocationOn, null, tint = Green, modifier = Modifier.size(55.dp))
                        Text("التتبع المباشر", fontWeight = FontWeight.Black, color = Ink)
                        Text("الخريطة الحقيقية تُربط في دفعة Mapbox", color = Muted, fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("الرحلة #${trip.id.takeLast(6)}", fontWeight = FontWeight.Black, color = Ink)
                                Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", fontSize = 11.sp, color = Muted)
                            }
                            Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(12.dp))
                        Route("موقع الانطلاق", "${trip.pickup.lat}, ${trip.pickup.lng}", true)
                        Route("الوجهة", "${trip.destination.lat}, ${trip.destination.lng}", false)
                        Spacer(Modifier.height(10.dp))
                        TripStatusRow(trip.status)
                        if (trip.status in listOf("searching", "driver_assigned", "arriving", "in_progress")) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { cancelDialog = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                                Text("إلغاء الرحلة")
                            }
                        } else {
                            Spacer(Modifier.height(10.dp))
                            Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                                Text("العودة للرئيسية")
                            }
                        }
                    }
                }
            }
        }
    }

    if (cancelDialog) {
        CancelTripDialog(
            onDismiss = { cancelDialog = false },
            onConfirm = {
                cancelDialog = false
                val trip = (state as? TripUiState.Success)?.trip ?: return@CancelTripDialog
                onCancel(trip.id, it)
            }
        )
    }
}

@Composable
private fun TripStatusRow(status: String) {
    val text = when (status) {
        "searching" -> "جاري البحث عن كابتن قريب"
        "driver_assigned" -> "تم العثور على كابتن"
        "arriving" -> "الكابتن في الطريق إليك"
        "in_progress" -> "الرحلة بدأت"
        "completed" -> "اكتملت الرحلة"
        "cancelled" -> "تم إلغاء الرحلة"
        else -> "حالة الرحلة: $status"
    }
    Card(colors = CardDefaults.cardColors(Green.copy(alpha = .08f)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, Modifier.padding(11.dp), color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable private fun Route(label: String, value: String, start: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).background(if (start) Green else Ink, CircleShape))
        Spacer(Modifier.width(10.dp))
        Column { Text(label, fontSize = 11.sp, color = Muted); Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink) }
    }
}

@Composable
private fun TripsScreen(customerId: String?) {
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val repository = remember { TripRepository(ApiProvider.api) }

    fun load() {
        loading = true
        error = null
        scope.launch {
            repository.list(customerId)
                .onSuccess { trips = it }
                .onFailure { error = it.message ?: "تعذر جلب الرحلات" }
            loading = false
        }
    }

    LaunchedEffect(customerId) { load() }

    Column(Modifier.fillMaxSize().padding(18.dp).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلاتي", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
                Text("سجل مشاويرك الحقيقي", color = Muted)
            }
            IconButton(onClick = ::load) { Icon(Icons.Default.Refresh, "تحديث", tint = Green) }
        }
        Spacer(Modifier.height(16.dp))
        when {
            loading -> Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) { CircularProgressIndicator(color = Green) }
            error != null -> Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) { Text(error!!, color = Muted) }
            trips.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) { Text("ما عندك رحلات بعد", color = Muted) }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.weight(1f)) {
                items(trips) { trip ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Green)
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text("رحلة #${trip.id.takeLast(6)}", fontWeight = FontWeight.Bold, color = Ink)
                                Text(trip.statusLabel(), fontSize = 11.sp, color = Muted)
                            }
                            Text("${trip.estimatedFare} ${trip.currency}", fontWeight = FontWeight.Black, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(onLogout: () -> Unit, onSettings: () -> Unit, sessionStore: SessionStore) {
    Column(Modifier.fillMaxSize().padding(18.dp).navigationBarsPadding()) {
        Text("حسابي", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(58.dp).background(Green.copy(alpha = .10f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Green, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("حساب وصلها", fontWeight = FontWeight.Black)
                    Text(sessionStore.phone ?: "رقم الهاتف", fontSize = 12.sp, color = Muted)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ProfileMenuItem("طرق الدفع", "💳")
        ProfileMenuItem("الإشعارات", "🔔")
        ProfileMenuItem("المساعدة والدعم", "💬")
        ProfileMenuItem("الأمان والخصوصية", "🛡️")
        ProfileMenuItem("الإعدادات", "⚙️", onSettings)
        Spacer(Modifier.height(14.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Text("تسجيل الخروج") }
    }
}

@Composable
private fun ProfileMenuItem(title: String, icon: String, onClick: (() -> Unit)? = null) {
    Card(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = Ink)
            Icon(Icons.Default.ChevronLeft, null, tint = Muted)
        }
    }
}

@Composable
private fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Text("الإعدادات", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink)
        }
        Spacer(Modifier.height(14.dp))
        ProfileMenuItem("الإشعارات والرحلات", "🔔")
        ProfileMenuItem("العروض والتنبيهات", "🎁")
        ProfileMenuItem("اللغة العربية", "🌐")
        ProfileMenuItem("الأمان", "🛡️")
        ProfileMenuItem("عن وصلها", "ℹ️")
    }
}

@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(selected = tab == 0, onClick = { onTab(0) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("الرئيسية") })
        NavigationBarItem(selected = tab == 1, onClick = { onTab(1) }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("رحلاتي") })
        NavigationBarItem(selected = tab == 2, onClick = { onTab(2) }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("حسابي") })
    }
}
