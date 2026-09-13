package com.waslha.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.mapbox.common.MapboxOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.style.standard.MapboxStandardStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val RideGreen = Color(0xFF087F5B)
private val RideDark = Color(0xFF055C42)
private val RideInk = Color(0xFF12201B)
private val RideMuted = Color(0xFF6D7A75)
private val RideBg = Color(0xFFF7F9F8)
private val RideSoft = Color(0xFFE7F6F0)
private val RideLine = Color(0xFFDDE5E1)
private val RideDanger = Color(0xFFB42318)

class CustomerRideExperienceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        setContent {
            WaslhaTheme {
                CustomerRideExperience(
                    session = SessionStore(this@CustomerRideExperienceActivity),
                    onExit = { finish() }
                )
            }
        }
    }
}

private enum class RideMode { BOOKING, SEARCHING, ASSIGNED, ARRIVING, IN_PROGRESS, COMPLETED, CANCELLED }
private enum class PaymentChoice { CASH, WALLET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerRideExperience(session: SessionStore, onExit: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TripRepository(ApiProvider.api) }
    val customerRepo = remember { CustomerRepository(ApiProvider.api) }

    var pickup by remember { mutableStateOf<Coordinates?>(null) }
    var destination by remember { mutableStateOf<Coordinates?>(null) }
    var destinationName by remember { mutableStateOf("حدد وجهتك") }
    var wallet by remember { mutableStateOf<WalletDto?>(null) }
    var savedPlaces by remember { mutableStateOf<List<SavedPlaceDto>>(emptyList()) }
    var nearbyDrivers by remember { mutableStateOf<List<NearbyDriverDto>>(emptyList()) }
    var vehicleType by remember { mutableStateOf("economy") }
    var payment by remember { mutableStateOf(PaymentChoice.CASH) }
    var estimate by remember { mutableStateOf<FareEstimate?>(null) }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var mode by remember { mutableStateOf(RideMode.BOOKING) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCancel by remember { mutableStateOf(false) }
    var showChat by remember { mutableStateOf(false) }
    var chatText by remember { mutableStateOf("") }
    var chatMessages by remember { mutableStateOf(listOf<TripMessageDto>()) }
    var chatAfter by remember { mutableStateOf(0L) }

    val accessToken = remember { context.resources.getString(R.string.mapbox_access_token).trim() }
    if (accessToken.isNotBlank() && !accessToken.startsWith("YOUR_")) MapboxOptions.accessToken = accessToken

    val fallbackPickup = Coordinates(33.5138, 36.2765)
    val mapCenter = pickup ?: fallbackPickup
    val viewport = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(mapCenter.lng, mapCenter.lat))
            zoom(13.5)
        }
    }

    fun refreshPickup() {
        val locationProvider = LocationProvider(context)
        scope.launch {
            val location = locationProvider.lastKnown()
            if (location != null) {
                pickup = Coordinates(location.latitude, location.longitude)
                viewport.flyTo(CameraOptions.Builder().center(Point.fromLngLat(location.longitude, location.latitude)).zoom(14.5).build())
                error = null
            } else error = "تعذر تحديد موقعك. فعّل GPS واسمح للتطبيق بالموقع."
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) refreshPickup()
        else error = "صلاحية الموقع مطلوبة للحجز."
    }

    LaunchedEffect(Unit) {
        val allowed = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (allowed) refreshPickup() else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        customerRepo.wallet().onSuccess { wallet = it }.onFailure { error = it.message ?: "تعذر تحميل المحفظة" }
        customerRepo.savedPlaces().onSuccess { savedPlaces = it }.onFailure { error = it.message ?: "تعذر تحميل الأماكن المحفوظة" }
    }

    LaunchedEffect(pickup, destination, vehicleType) {
        val from = pickup ?: return@LaunchedEffect
        val to = destination ?: return@LaunchedEffect
        repo.estimate(from, to, vehicleType)
            .onSuccess { estimate = it }
            .onFailure { estimate = null; error = it.message ?: "تعذر حساب الأجرة" }
    }

    LaunchedEffect(mode, vehicleType) {
        if (mode != RideMode.SEARCHING) return@LaunchedEffect
        while (mode == RideMode.SEARCHING) {
            customerRepo.nearbyDrivers(vehicleType).onSuccess { nearbyDrivers = it }
            delay(5000)
        }
    }

    LaunchedEffect(trip?.id) {
        val id = trip?.id ?: return@LaunchedEffect
        while (true) {
            delay(4000)
            repo.track(id).onSuccess { tracking ->
                mode = when (tracking.status) {
                    "searching" -> RideMode.SEARCHING
                    "driver_assigned" -> RideMode.ASSIGNED
                    "arriving" -> RideMode.ARRIVING
                    "in_progress" -> RideMode.IN_PROGRESS
                    "completed" -> RideMode.COMPLETED
                    "cancelled" -> RideMode.CANCELLED
                    else -> mode
                }
                if (tracking.driver != null && trip != null && trip?.driver == null) trip = trip?.copy(driver = tracking.driver)
            }.onFailure { error = it.message ?: "تعذر تحديث حالة الرحلة" }
            if (mode == RideMode.COMPLETED || mode == RideMode.CANCELLED) break
        }
    }

    LaunchedEffect(showChat, trip?.id) {
        val id = trip?.id ?: return@LaunchedEffect
        if (!showChat) return@LaunchedEffect
        while (showChat) {
            repo.messages(id, chatAfter).onSuccess { fresh ->
                if (fresh.isNotEmpty()) {
                    chatMessages = (chatMessages + fresh).distinctBy { it.id }.sortedBy { it.createdAt }.takeLast(100)
                    chatAfter = chatMessages.maxOfOrNull { it.createdAt } ?: chatAfter
                }
            }
            delay(2500)
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.Expanded, skipHiddenState = true)
    )

    Box(Modifier.fillMaxSize().background(Color(0xFFEAF1EE))) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 150.dp,
            sheetSwipeEnabled = true,
            containerColor = Color.Transparent,
            sheetContainerColor = RideBg,
            sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetTonalElevation = 8.dp,
            topBar = {
                Box(Modifier.fillMaxWidth().padding(top = 12.dp, start = 14.dp, end = 14.dp)) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onExit) { Icon(Icons.Default.Search, "إغلاق", tint = RideMuted) }
                            Column(Modifier.weight(1f)) {
                                Text("وصلها", color = RideInk, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                Text("نقل ركاب داخل سوريا", color = RideMuted, fontSize = 9.sp)
                            }
                            Icon(Icons.Default.LocationOn, null, tint = RideGreen)
                        }
                    }
                }
            },
            sheetContent = {
                RideBottomSheet(
                    mode = mode,
                    pickup = pickup,
                    destinationName = destinationName,
                    estimate = estimate,
                    wallet = wallet,
                    vehicleType = vehicleType,
                    payment = payment,
                    trip = trip,
                    busy = busy,
                    error = error,
                    savedPlaces = savedPlaces,
                    onRefreshLocation = { refreshPickup() },
                    onDestination = {
                        viewport.flyTo(CameraOptions.Builder().center(Point.fromLngLat(mapCenter.lng, mapCenter.lat)).zoom(14.5).build())
                    },
                    onVehicle = { vehicleType = it },
                    onPayment = { payment = it },
                    onRequest = {
                        val from = pickup
                        val to = destination
                        val userId = session.userId
                        if (from == null) { error = "حدد موقع الانطلاق أولاً"; return@RideBottomSheet }
                        if (to == null) { error = "اضغط على الخريطة لاختيار الوجهة"; return@RideBottomSheet }
                        if (userId.isNullOrBlank()) { error = "بيانات الحساب غير مكتملة"; return@RideBottomSheet }
                        if (payment == PaymentChoice.WALLET && (wallet?.balance ?: 0L) <= 0L) { error = "رصيد المحفظة غير كافٍ"; return@RideBottomSheet }
                        busy = true
                        error = null
                        scope.launch {
                            repo.create(TripRequest(userId, from, to, vehicleType, if (payment == PaymentChoice.WALLET) "wallet" else "cash"))
                                .onSuccess { trip = it; mode = RideMode.SEARCHING }
                                .onFailure { error = it.message ?: "تعذر طلب التكسي" }
                            busy = false
                        }
                    },
                    onCancel = { showCancel = true },
                    onCall = {
                        val phone = trip?.driver?.phone?.takeIf { it.isNotBlank() }
                        if (phone != null) context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) else error = "رقم الكابتن غير متوفر حالياً"
                    },
                    onChat = { showChat = true },
                    onSavePlace = { slot ->
                        val to = destination
                        if (to == null) { error = "حدد الوجهة أولاً"; return@RideBottomSheet }
                        scope.launch {
                            customerRepo.savePlace(slot, SavedPlaceRequest(if (slot == "home") "المنزل" else "العمل", to.lat, to.lng))
                                .onSuccess { savedPlaces = (savedPlaces.filter { it.type != slot } + it) }
                                .onFailure { error = it.message ?: "تعذر حفظ المكان" }
                        }
                    },
                    onSavedPlace = { place ->
                        destination = Coordinates(place.latitude, place.longitude)
                        destinationName = place.name
                        viewport.flyTo(CameraOptions.Builder().center(Point.fromLngLat(place.longitude, place.latitude)).zoom(15.0).build())
                    }
                )
            }
        ) { _ -> Box(Modifier.fillMaxSize()) }

        if (accessToken.isBlank() || accessToken.startsWith("YOUR_")) {
            Card(Modifier.align(Alignment.Center).padding(24.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) {
                Text("الخريطة غير مهيأة", Modifier.padding(22.dp), color = RideDanger, fontWeight = FontWeight.Bold)
            }
        } else {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = viewport,
                style = { MapboxStandardStyle() },
                onMapClickListener = {
                    val center = viewport.cameraState?.center
                    if (mode == RideMode.BOOKING && center != null) {
                        destination = Coordinates(center.latitude(), center.longitude())
                        destinationName = "الموقع المحدد"
                    }
                    true
                }
            ) {
                nearbyDrivers.forEach { driver ->
                    CircleAnnotation(point = Point.fromLngLat(driver.lng, driver.lat)) {
                        circleRadius = 6.0
                        circleColor = Color(0xFF087F5B)
                        circleStrokeColor = Color.White
                        circleStrokeWidth = 2.0
                    }
                }
            }
            if (mode == RideMode.SEARCHING) RadarPulse()
            FloatingMapButton(Modifier.align(Alignment.CenterStart).padding(start = 12.dp), Icons.Default.MyLocation, "موقعي", ::refreshPickup)
        }
    }

    if (showCancel) {
        CancelTripDialog(
            onDismiss = { showCancel = false },
            onConfirm = { reason ->
                showCancel = false
                val id = trip?.id ?: return@CancelTripDialog
                busy = true
                scope.launch {
                    repo.cancel(id, reason).onSuccess { trip = it; mode = RideMode.CANCELLED; customerRepo.wallet().onSuccess { wallet = it } }.onFailure { error = it.message ?: "تعذر إلغاء الرحلة" }
                    busy = false
                }
            }
        )
    }

    if (showChat) {
        AlertDialog(
            onDismissRequest = { showChat = false },
            title = { Text("الدردشة مع الكابتن", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chatMessages.isEmpty()) Text("ابدأ المحادثة مع الكابتن من هنا.", color = RideMuted, fontSize = 11.sp)
                    else chatMessages.forEach { message ->
                        val mine = message.senderId == session.userId
                        Text(if (mine) "أنت: ${message.text}" else "الكابتن: ${message.text}", color = RideInk, fontSize = 11.sp)
                    }
                    androidx.compose.material3.OutlinedTextField(chatText, { chatText = it }, Modifier.fillMaxWidth(), label = { Text("رسالتك") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val id = trip?.id ?: return@TextButton
                    val text = chatText.trim()
                    if (text.isBlank()) return@TextButton
                    scope.launch {
                        repo.sendMessage(id, text)
                            .onSuccess { sent -> chatMessages = (chatMessages + sent).distinctBy { it.id }; chatAfter = maxOf(chatAfter, sent.createdAt); chatText = "" }
                            .onFailure { error = it.message ?: "تعذر إرسال الرسالة" }
                    }
                }) { Text("إرسال", color = RideGreen, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showChat = false }) { Text("إغلاق") } }
        )
    }
}

@Composable
private fun RideBottomSheet(
    mode: RideMode,
    pickup: Coordinates?,
    destinationName: String,
    estimate: FareEstimate?,
    wallet: WalletDto?,
    vehicleType: String,
    payment: PaymentChoice,
    trip: Trip?,
    busy: Boolean,
    error: String?,
    savedPlaces: List<SavedPlaceDto>,
    onRefreshLocation: () -> Unit,
    onDestination: () -> Unit,
    onVehicle: (String) -> Unit,
    onPayment: (PaymentChoice) -> Unit,
    onRequest: () -> Unit,
    onCancel: () -> Unit,
    onCall: () -> Unit,
    onChat: () -> Unit,
    onSavePlace: (String) -> Unit,
    onSavedPlace: (SavedPlaceDto) -> Unit
) {
    Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(42.dp, 4.dp).background(RideLine, RoundedCornerShape(2.dp)).align(Alignment.CenterHorizontally))
        when (mode) {
            RideMode.BOOKING -> BookingSheet(pickup, destinationName, estimate, wallet, vehicleType, payment, savedPlaces, busy, error, onRefreshLocation, onDestination, onVehicle, onPayment, onRequest, onSavePlace, onSavedPlace)
            RideMode.SEARCHING -> SearchingSheet(error, onCancel)
            RideMode.ASSIGNED, RideMode.ARRIVING, RideMode.IN_PROGRESS -> ActiveTripSheet(mode, trip, onCancel, onCall, onChat)
            RideMode.COMPLETED, RideMode.CANCELLED -> FinishedSheet(mode, trip, estimate)
        }
    }
}

@Composable
private fun BookingSheet(
    pickup: Coordinates?, destinationName: String, estimate: FareEstimate?, wallet: WalletDto?, vehicleType: String, payment: PaymentChoice,
    savedPlaces: List<SavedPlaceDto>, busy: Boolean, error: String?, onRefreshLocation: () -> Unit, onDestination: () -> Unit, onVehicle: (String) -> Unit,
    onPayment: (PaymentChoice) -> Unit, onRequest: () -> Unit, onSavePlace: (String) -> Unit, onSavedPlace: (SavedPlaceDto) -> Unit
) {
    Text("وين نوصلك؟", color = RideInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
    Text("الخريطة تبقى مفتوحة أثناء الاختيار", color = RideMuted, fontSize = 10.sp)
    LocationChoice("من", if (pickup == null) "جاري تحديد موقعك" else "موقعك الحالي", Icons.Default.MyLocation, onRefreshLocation)
    LocationChoice("إلى", destinationName, Icons.Default.LocationOn, onDestination)
    if (savedPlaces.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
            items(savedPlaces, key = { it.id }) { place ->
                Card(Modifier.clickable { onSavedPlace(place) }, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, RideLine)) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (place.type == "home") Icons.Default.Home else Icons.Default.Work, null, tint = RideGreen, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(if (place.type == "home") "المنزل" else "العمل", color = RideInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("economy" to "اقتصادي", "comfort" to "مريح", "family" to "عائلي").forEach { (id, title) ->
            val selected = vehicleType == id
            Card(Modifier.weight(1f).clickable { onVehicle(id) }, colors = CardDefaults.cardColors(if (selected) RideSoft else Color.White), shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) RideGreen else RideLine)) {
                Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DirectionsCar, null, tint = RideGreen, modifier = Modifier.size(20.dp))
                    Text(title, color = RideInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    Text("طريقة الدفع", color = RideInk, fontWeight = FontWeight.Black, fontSize = 15.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaymentMini("نقداً", PaymentChoice.CASH == payment, true) { onPayment(PaymentChoice.CASH) }
        PaymentMini("المحفظة • ${wallet?.balance ?: 0} ل.س", PaymentChoice.WALLET == payment, (wallet?.balance ?: 0) > 0) { onPayment(PaymentChoice.WALLET) }
    }
    estimate?.let { fare ->
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, RideLine)) {
            Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("الأجرة التقديرية", color = RideMuted, fontSize = 9.sp)
                    Text("${fare.estimatedFare} ل.س", color = RideInk, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${"%.1f".format(fare.distanceKm)} كم", color = RideMuted, fontSize = 9.sp)
                    Text("${fare.durationMin} دقيقة", color = RideMuted, fontSize = 9.sp)
                }
            }
        }
    }
    if (error != null) Text(error, color = RideDanger, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Button(onClick = { onSavePlace("home") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RideSoft, contentColor = RideGreen), shape = RoundedCornerShape(14.dp)) { Text("حفظ كمنزل", fontSize = 9.sp) }
        Button(onClick = { onSavePlace("work") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RideSoft, contentColor = RideGreen), shape = RoundedCornerShape(14.dp)) { Text("حفظ كعمل", fontSize = 9.sp) }
    }
    Button(enabled = !busy, onClick = onRequest, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = RideGreen), shape = RoundedCornerShape(17.dp)) {
        if (busy) CircularProgressIndicator(Modifier.size(21.dp), color = Color.White, strokeWidth = 2.dp) else Text("طلب التكسي", fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
private fun SearchingSheet(error: String?, onCancel: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "search")
    val alpha by transition.animateFloat(0.25f, 0.95f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "alpha")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(64.dp).background(RideSoft, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(38.dp).background(RideGreen.copy(alpha = alpha), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Search, null, tint = Color.White, modifier = Modifier.size(21.dp)) }
        }
        Text("جاري البحث عن أقرب كابتن", color = RideInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text("نبحث عن أفضل كابتن متاح بالقرب منك", color = RideMuted, fontSize = 10.sp)
        if (error != null) Text(error, color = RideDanger, fontSize = 10.sp)
        TextButton(onClick = onCancel) { Text("إلغاء الرحلة", color = RideDanger, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ActiveTripSheet(mode: RideMode, trip: Trip?, onCancel: () -> Unit, onCall: () -> Unit, onChat: () -> Unit) {
    val title = when (mode) { RideMode.ASSIGNED -> "تم قبول رحلتك"; RideMode.ARRIVING -> "الكابتن في الطريق إليك"; else -> "الرحلة جارية" }
    Text(title, color = RideInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
    trip?.let { current ->
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(RideSoft, CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = RideGreen) }
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Text(current.driver?.name?.ifBlank { "كابتن وصلها" } ?: "كابتن وصلها", color = RideInk, fontWeight = FontWeight.Black)
                Text(current.driver?.vehicle ?: "سيارة وصلها", color = RideMuted, fontSize = 10.sp)
                current.driver?.plate?.takeIf { it.isNotBlank() }?.let { Text("اللوحة: $it", color = RideMuted, fontSize = 9.sp) }
            }
            Row {
                IconButton(onClick = onCall) { Icon(Icons.Default.Call, "اتصال", tint = RideGreen) }
                IconButton(onClick = onChat) { Icon(Icons.Default.Search, "دردشة", tint = RideGreen) }
            }
        }
    }
    TripProgressTimelineCompact(mode)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("إلغاء", color = RideDanger, fontWeight = FontWeight.Bold) }
        Button(onClick = onChat, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = RideSoft, contentColor = RideGreen), shape = RoundedCornerShape(15.dp)) { Text("دردشة") }
    }
}

@Composable
private fun FinishedSheet(mode: RideMode, trip: Trip?, estimate: FareEstimate?) {
    val cancelled = mode == RideMode.CANCELLED
    Text(if (cancelled) "تم إلغاء الرحلة" else "اكتملت الرحلة", color = if (cancelled) RideDanger else RideGreen, fontSize = 21.sp, fontWeight = FontWeight.Black)
    Text(if (cancelled) "يمكنك طلب رحلة جديدة من الخريطة." else "شكراً لاستخدامك وصلها", color = RideMuted, fontSize = 10.sp)
    trip?.let { Text("رقم الرحلة: ${it.id}", color = RideMuted, fontSize = 9.sp) }
    estimate?.let { Text("الأجرة التقديرية: ${it.estimatedFare} ل.س", color = RideInk, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}

@Composable
private fun LocationChoice(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, RideLine)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RideGreen, modifier = Modifier.size(21.dp)); Spacer(Modifier.size(9.dp)); Column(Modifier.weight(1f)) { Text(label, color = RideMuted, fontSize = 9.sp); Text(value, color = RideInk, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}

@Composable
private fun RowScope.PaymentMini(title: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(Modifier.weight(1f).clickable(enabled = enabled, onClick = onClick), colors = CardDefaults.cardColors(if (selected) RideSoft else Color.White), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) RideGreen else RideLine)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = { if (enabled) onClick() }, enabled = enabled)
            Text(title, color = if (enabled) RideInk else RideMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TripProgressTimelineCompact(mode: RideMode) {
    val stages = listOf(RideMode.ASSIGNED to "تم تعيين الكابتن", RideMode.ARRIVING to "الكابتن في الطريق", RideMode.IN_PROGRESS to "الرحلة جارية")
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        stages.forEachIndexed { index, (stage, label) ->
            val active = mode == stage
            val done = mode.ordinal >= stage.ordinal
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Box(Modifier.size(11.dp).background(if (done) RideGreen else RideLine, CircleShape))
                Text(label, color = if (active) RideInk else RideMuted, fontSize = 8.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
            }
            if (index != stages.lastIndex) Box(Modifier.weight(.5f).height(1.dp).background(RideLine))
        }
    }
}

@Composable
private fun RadarPulse() {
    val transition = rememberInfiniteTransition(label = "radar")
    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "radarProgress")
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = 30f + 80f * progress
        drawCircle(color = RideGreen.copy(alpha = (1f - progress) * .35f), radius = radius, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        drawCircle(color = RideGreen.copy(alpha = .12f), radius = 26f, center = center, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
    }
}

@Composable
private fun FloatingMapButton(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    IconButton(modifier = modifier.size(48.dp).background(Color.White, CircleShape), onClick = onClick) { Icon(icon, label, tint = RideGreen) }
}
