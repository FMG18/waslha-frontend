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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
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
import kotlinx.coroutines.launch

private val V2Green = Color(0xFF078A60)
private val V2GreenDark = Color(0xFF056B4B)
private val V2Mint = Color(0xFFE8F6F0)
private val V2Bg = Color(0xFFF4F7F5)
private val V2Ink = Color(0xFF10201B)
private val V2Muted = Color(0xFF71807A)
private val V2Danger = Color(0xFFB42318)

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
    V2Vehicle("economy", "اقتصادي", "أفضل سعر للرحلة اليومية", 1.0),
    V2Vehicle("comfort", "مريح", "سيارة أحدث ومساحة أكبر", 1.2),
    V2Vehicle("family", "عائلي", "مناسب للعائلة والحقائب", 1.35)
)

class PassengerHomeV2Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val session = SessionStore(this)
        setContent {
            MaterialTheme {
                PassengerHomeV2(session) {
                    session.clear()
                    startActivity(Intent(this, AuthActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    })
                    finish()
                }
            }
        }
    }
}

@Composable
private fun PassengerHomeV2(session: SessionStore, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { TripRepository(ApiProvider.api) }
    val locator = remember { LocationProvider(context) }

    var tab by remember { mutableStateOf(0) }
    var pickup by remember { mutableStateOf<Coordinates?>(null) }
    var pickupLabel by remember { mutableStateOf("جاري تحديد موقعك…") }
    var destination by remember { mutableStateOf<V2Destination?>(null) }
    var vehicle by remember { mutableStateOf(v2Vehicles.first()) }
    var trip by remember { mutableStateOf<Trip?>(null) }
    var history by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var historyLoading by remember { mutableStateOf(false) }
    var locationLoading by remember { mutableStateOf(false) }
    var requestLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var destinationDialog by remember { mutableStateOf(false) }
    var vehicleDialog by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }

    fun refreshLocation() {
        locationLoading = true
        scope.launch {
            val location = locator.lastKnown()
            if (location != null) {
                pickup = Coordinates(location.latitude, location.longitude)
                pickupLabel = "موقعك الحالي"
                error = null
            } else {
                pickupLabel = "تعذر تحديد الموقع"
                error = "فعّل GPS وتأكد من صلاحية الموقع"
            }
            locationLoading = false
        }
    }

    val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            refreshLocation()
        } else {
            error = "صلاحية الموقع مطلوبة لطلب التاكسي"
        }
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) refreshLocation()
        else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    LaunchedEffect(tab) {
        if (tab == 1 && !session.userId.isNullOrBlank()) {
            historyLoading = true
            repo.list(session.userId!!)
                .onSuccess { history = it }
                .onFailure { error = it.message ?: "تعذر جلب الرحلات" }
            historyLoading = false
        }
    }

    LaunchedEffect(trip?.id) {
        val activeId = trip?.id ?: return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(5000)
            val result = repo.get(activeId)
            result.onSuccess { updated -> trip = updated }
            if (trip?.status == "completed" || trip?.status == "cancelled") break
        }
    }

    Surface(Modifier.fillMaxSize(), color = V2Bg) {
        when {
            showMore -> MoreHubScreen(
                onBack = { showMore = false },
                onSavedPlaces = { context.startActivity(Intent(context, FeatureHostActivity::class.java).putExtra("screen", "places")) },
                onPayments = { context.startActivity(Intent(context, FeatureHostActivity::class.java).putExtra("screen", "payments")) },
                onNotifications = { context.startActivity(Intent(context, FeatureHostActivity::class.java).putExtra("screen", "notifications")) },
                onSupport = { context.startActivity(Intent(context, FeatureHostActivity::class.java).putExtra("screen", "support")) },
                onRating = { context.startActivity(Intent(context, FeatureHostActivity::class.java).putExtra("screen", "rating")) }
            )
            trip != null -> ActiveTripV2(
                trip = trip!!,
                loading = requestLoading,
                error = error,
                onRefresh = {
                    requestLoading = true
                    scope.launch {
                        repo.get(trip!!.id)
                            .onSuccess { trip = it }
                            .onFailure { error = it.message ?: "تعذر تحديث الرحلة" }
                        requestLoading = false
                    }
                },
                onCancel = {
                    requestLoading = true
                    scope.launch {
                        repo.cancel(trip!!.id, "إلغاء من الراكب")
                            .onSuccess { trip = it }
                            .onFailure { error = it.message ?: "تعذر إلغاء الرحلة" }
                        requestLoading = false
                    }
                },
                onDone = {
                    if (trip?.status == "completed" || trip?.status == "cancelled") {
                        trip = null
                        destination = null
                        tab = 1
                    }
                }
            )
            else -> Scaffold(
                containerColor = V2Bg,
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
                        0 -> HomeV2(
                            session = session,
                            pickupLabel = pickupLabel,
                            destination = destination,
                            vehicle = vehicle,
                            locating = locationLoading,
                            requesting = requestLoading,
                            error = error,
                            onLocation = {
                                if (permissionGranted) refreshLocation()
                                else permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            },
                            onDestination = { destinationDialog = true },
                            onVehicle = { vehicleDialog = true },
                            onRequest = {
                                val from = pickup
                                val to = destination
                                val userId = session.userId
                                when {
                                    from == null -> error = "حدد موقع الانطلاق أولًا"
                                    to == null -> destinationDialog = true
                                    userId.isNullOrBlank() -> error = "بيانات الحساب غير مكتملة"
                                    else -> {
                                        requestLoading = true
                                        error = null
                                        scope.launch {
                                            repo.create(TripRequest(userId, from, to.coordinates, vehicle.id, "cash"))
                                                .onSuccess { trip = it }
                                                .onFailure { error = it.message ?: "تعذر إنشاء الرحلة" }
                                            requestLoading = false
                                        }
                                    }
                                }
                            },
                            onMore = { showMore = true }
                        )
                        1 -> HistoryV2(history, historyLoading, error) {
                            val userId = session.userId ?: return@HistoryV2
                            historyLoading = true
                            scope.launch {
                                repo.list(userId)
                                    .onSuccess { history = it }
                                    .onFailure { error = it.message ?: "تعذر تحديث الرحلات" }
                                historyLoading = false
                            }
                        }
                        else -> AccountV2(session, onMore = { showMore = true }, onLogout = onLogout)
                    }
                }
            }
        }
    }

    if (destinationDialog) {
        DestinationV2Dialog(
            onDismiss = { destinationDialog = false },
            onSelect = { destination = it; destinationDialog = false }
        )
    }
    if (vehicleDialog) {
        VehicleV2Dialog(
            selected = vehicle,
            onDismiss = { vehicleDialog = false },
            onSelect = { vehicle = it; vehicleDialog = false }
        )
    }
}

@Composable
private fun HomeV2(session: SessionStore, pickupLabel: String, destination: V2Destination?, vehicle: V2Vehicle, locating: Boolean, requesting: Boolean, error: String?, onLocation: () -> Unit, onDestination: () -> Unit, onVehicle: () -> Unit, onRequest: () -> Unit, onMore: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(13.dp), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("وصلها", color = V2Green, fontSize = 34.sp, fontWeight = FontWeight.Black)
                    Text(session.name?.takeIf { it.isNotBlank() }?.let { "أهلًا $it 👋" } ?: "جاهز لمشوارك؟", color = V2Muted, fontSize = 13.sp)
                }
                IconButton(onClick = onMore) { Icon(Icons.Default.AccountCircle, "المزيد", tint = V2Green, modifier = Modifier.size(31.dp)) }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(V2Green), shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(3.dp)) {
                Column(Modifier.padding(18.dp)) {
                    Text("اطلب سيارة الآن", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text("نوصلك داخل المدينة بسرعة ووضوح", color = Color.White.copy(alpha = .86f), fontSize = 11.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth().background(Color.White.copy(alpha = .12f), RoundedCornerShape(16.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الوجهة", color = Color.White.copy(alpha = .7f), fontSize = 10.sp)
                            Text(destination?.title ?: "حدد وجهتك", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White.copy(alpha = .7f))
                    }
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    V2LocationRow("نقطة الانطلاق", pickupLabel, Icons.Default.MyLocation, locating, onLocation)
                    V2LocationRow("الوجهة", destination?.title ?: "اضغط لاختيار الوجهة", Icons.Default.LocationOn, false, onDestination)
                    Card(Modifier.fillMaxWidth().clickable(onClick = onVehicle), colors = CardDefaults.cardColors(V2Mint), shape = RoundedCornerShape(17.dp)) {
                        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(43.dp).background(Color.White, CircleShape), Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = V2Green) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(vehicle.title, color = V2Ink, fontWeight = FontWeight.Black)
                                Text(vehicle.subtitle, color = V2Muted, fontSize = 10.sp)
                            }
                            Text("تغيير", color = V2Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CreditCard, null, tint = V2Green)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الدفع نقدًا", color = V2Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("الدفع للكابتن بعد الوصول", color = V2Muted, fontSize = 10.sp)
                        }
                        Text("نقدي", color = V2Green, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    if (!error.isNullOrBlank()) Text(error, color = V2Danger, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Button(onClick = onRequest, enabled = !requesting, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(17.dp)) {
                        if (requesting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("طلب تاكسي", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        item { FeatureBanner("مساعدة ودعم", "نحن هنا لمساعدتك في أي مشكلة", Icons.Default.HelpOutline, onMore) }
    }
}

@Composable
private fun V2LocationRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, loading: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).background(V2Bg, RoundedCornerShape(16.dp)).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = V2Green, modifier = Modifier.size(25.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = V2Muted, fontSize = 10.sp)
            Text(value, color = V2Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = V2Green, strokeWidth = 2.dp)
        else Icon(Icons.Default.ArrowBack, null, tint = V2Muted)
    }
}

@Composable
private fun FeatureBanner(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(V2Mint, CircleShape), Alignment.Center) { Icon(icon, null, tint = V2Green) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = V2Ink, fontWeight = FontWeight.Black)
                Text(subtitle, color = V2Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ArrowBack, null, tint = V2Muted)
        }
    }
}

@Composable
private fun DestinationV2Dialog(onDismiss: () -> Unit, onSelect: (V2Destination) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("اختيار الوجهة", fontWeight = FontWeight.Black) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            v2Destinations.forEach { destination ->
                Card(Modifier.fillMaxWidth().clickable { onSelect(destination) }, colors = CardDefaults.cardColors(V2Bg), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = V2Green)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(destination.title, color = V2Ink, fontWeight = FontWeight.Bold)
                            Text(destination.subtitle, color = V2Muted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = V2Green) } })
}

@Composable
private fun VehicleV2Dialog(selected: V2Vehicle, onDismiss: () -> Unit, onSelect: (V2Vehicle) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("نوع التكسي", fontWeight = FontWeight.Black) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            v2Vehicles.forEach { item ->
                Card(Modifier.fillMaxWidth().clickable { onSelect(item) }, colors = CardDefaults.cardColors(if (item.id == selected.id) V2Mint else V2Bg), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DirectionsCar, null, tint = V2Green)
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, color = V2Ink, fontWeight = FontWeight.Black)
                            Text(item.subtitle, color = V2Muted, fontSize = 10.sp)
                        }
                        if (item.id == selected.id) Text("✓", color = V2Green, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق", color = V2Green) } })
}

@Composable
private fun HistoryV2(history: List<Trip>, loading: Boolean, error: String?, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلاتي", color = V2Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("كل رحلاتك السابقة في مكان واحد", color = V2Muted, fontSize = 11.sp)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث", tint = V2Green) }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = V2Green) }
            history.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.History, null, tint = V2Muted, modifier = Modifier.size(52.dp)); Spacer(Modifier.height(8.dp)); Text("لا توجد رحلات بعد", color = V2Ink, fontWeight = FontWeight.Bold) } }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(history.take(40)) { item ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(43.dp).background(V2Mint, CircleShape), Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = V2Green) }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.statusLabel(), color = V2Ink, fontWeight = FontWeight.Black)
                                Text("${item.distanceKm} كم • ${item.durationMin} دقيقة", color = V2Muted, fontSize = 10.sp)
                            }
                            Text("${item.estimatedFare} ${item.currency}", color = V2Green, fontWeight = FontWeight.Black, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        if (!error.isNullOrBlank()) Text(error, color = V2Danger, fontSize = 11.sp)
    }
}

@Composable
private fun AccountV2(session: SessionStore, onMore: () -> Unit, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(11.dp), contentPadding = PaddingValues(bottom = 22.dp)) {
        item {
            Text("حسابي", color = V2Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text("كل أدوات حسابك وإعداداتك", color = V2Muted, fontSize = 11.sp)
        }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(62.dp).background(V2Mint, CircleShape), Alignment.Center) { Icon(Icons.Default.Person, null, tint = V2Green, modifier = Modifier.size(34.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(session.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", color = V2Ink, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(session.email ?: session.phone ?: "بيانات الحساب", color = V2Muted, fontSize = 11.sp)
                    }
                }
            }
        }
        item { AccountAction("الإعدادات والخدمات", "إشعارات، دفع، أماكن محفوظة، دعم", Icons.Default.Settings, onMore) }
        item { AccountAction("الأمان والخصوصية", "حماية الحساب وبياناتك", Icons.Default.Security, onMore) }
        item { AccountAction("تقييم الرحلة", "شاركنا رأيك بعد كل مشوار", Icons.Default.Star, onMore) }
        item { AccountAction("مساعدة ودعم", "الأسئلة الشائعة والمساعدة", Icons.Default.HelpOutline, onMore) }
        item {
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(7.dp))
                Text("تسجيل الخروج", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun AccountAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(V2Mint, CircleShape), Alignment.Center) { Icon(icon, null, tint = V2Green) }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = V2Ink, fontWeight = FontWeight.Black)
                Text(subtitle, color = V2Muted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ArrowBack, null, tint = V2Muted)
        }
    }
}

@Composable
private fun MoreHubScreen(onBack: () -> Unit, onSavedPlaces: () -> Unit, onPayments: () -> Unit, onNotifications: () -> Unit, onSupport: () -> Unit, onRating: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = V2Ink) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) { Text("الخدمات والحساب", color = V2Ink, fontSize = 25.sp, fontWeight = FontWeight.Black); Text("كل ما تحتاجه في مكان واحد", color = V2Muted, fontSize = 10.sp) }
            }
        }
        item { MoreItem("الأماكن المحفوظة", "المنزل والعمل والمفضلة", Icons.Default.Favorite, onSavedPlaces) }
        item { MoreItem("طرق الدفع", "إدارة طريقة الدفع", Icons.Default.CreditCard, onPayments) }
        item { MoreItem("الإشعارات", "تنبيهات الرحلات والعروض", Icons.Default.Notifications, onNotifications) }
        item { MoreItem("المساعدة والدعم", "أسئلة شائعة وحلول سريعة", Icons.Default.HelpOutline, onSupport) }
        item { MoreItem("تقييم الرحلة", "قيّم تجربتك مع وصلها", Icons.Default.Star, onRating) }
    }
}

@Composable
private fun MoreItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(V2Mint, CircleShape), Alignment.Center) { Icon(icon, null, tint = V2Green) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = V2Ink, fontWeight = FontWeight.Black); Text(subtitle, color = V2Muted, fontSize = 10.sp) }
            Icon(Icons.Default.ArrowBack, null, tint = V2Muted)
        }
    }
}

@Composable
private fun ActiveTripV2(trip: Trip, loading: Boolean, error: String?, onRefresh: () -> Unit, onCancel: () -> Unit, onDone: () -> Unit) {
    val terminal = trip.status == "completed" || trip.status == "cancelled"
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلتك الحالية", color = V2Ink, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text(trip.statusLabel(), color = V2Green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            IconButton(onClick = onRefresh, enabled = !loading) {
                if (loading) CircularProgressIndicator(Modifier.size(19.dp), color = V2Green, strokeWidth = 2.dp) else Icon(Icons.Default.Refresh, "تحديث", tint = V2Green)
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(V2Green), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("تفاصيل الرحلة", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                Text("${trip.estimatedFare} ${trip.currency}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
                Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة • دفع نقدي", color = Color.White.copy(alpha = .9f), fontSize = 11.sp)
            }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(21.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                TripLineV2("الحالة", trip.statusLabel())
                TripLineV2("المسافة", "${trip.distanceKm} كم")
                TripLineV2("المدة", "${trip.durationMin} دقيقة")
                TripLineV2("الدفع", "نقدي")
            }
        }
        Spacer(Modifier.weight(1f))
        if (!terminal) Button(onClick = onCancel, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("إلغاء الرحلة", fontWeight = FontWeight.Black) }
        else Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("العودة للرحلات", fontWeight = FontWeight.Black) }
        if (!error.isNullOrBlank()) Text(error, color = V2Danger, fontSize = 11.sp)
    }
}

@Composable
private fun TripLineV2(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) { Text(label, Modifier.weight(1f), color = V2Muted, fontSize = 11.sp); Text(value, color = V2Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
}
