package com.waslha.app

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeMap(
    sessionStore: SessionStore,
    pickup: Coordinates,
    pickupLabel: String,
    destination: AnyDestination?,
    vehicle: AnyVehicle,
    estimate: FareEstimate?,
    estimateLoading: Boolean,
    locationLoading: Boolean,
    requesting: Boolean,
    error: String?,
    onRefreshLocation: () -> Unit,
    onDestinationPicked: (Coordinates) -> Unit,
    onVehicle: (AnyVehicle) -> Unit,
    onRequest: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val customerRepo = remember { CustomerRepository(ApiProvider.api) }
    var paymentMethod by remember { mutableStateOf("cash") }
    var walletBalance by remember { mutableStateOf<Long?>(null) }
    var nearby by remember { mutableStateOf<List<NearbyDriverDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        customerRepo.wallet().onSuccess { walletBalance = it.balance }
    }

    LaunchedEffect(vehicle.id, pickup) {
        while (true) {
            customerRepo.nearbyDrivers(vehicle.id).onSuccess { nearby = it }
            delay(8000)
        }
    }

    val bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 280.dp,
        sheetSwipeEnabled = true,
        containerColor = Color.Transparent,
        sheetContainerColor = Color.White,
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 12.dp,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContent = {
            HomeSheetContent(
                sessionStore = sessionStore,
                pickupLabel = pickupLabel,
                destination = destination,
                vehicle = vehicle,
                estimate = estimate,
                estimateLoading = estimateLoading,
                locationLoading = locationLoading,
                requesting = requesting,
                error = error,
                paymentMethod = paymentMethod,
                walletBalance = walletBalance,
                onRefreshLocation = onRefreshLocation,
                onDestinationPicked = onDestinationPicked,
                onVehicle = onVehicle,
                onPayment = { paymentMethod = it },
                onRequest = { onRequest(paymentMethod) }
            )
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            WaslhaRideMap(
                pickup = pickup,
                destination = destination?.coordinates,
                modifier = Modifier.fillMaxSize(),
                onDestinationPicked = onDestinationPicked
            )

            // Live nearby-driver markers are visualized over the map while searching.
            if (nearby.isNotEmpty() && destination == null) {
                nearby.take(12).forEachIndexed { index, driver ->
                    NearbyDriverMarker(index)
                }
            }

            if (destination == null) {
                val transition = rememberInfiniteTransition(label = "radar")
                val pulse by transition.animateFloat(
                    initialValue = 0.25f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Restart),
                    label = "pulse"
                )
                Box(Modifier.align(Alignment.Center).size(96.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(Color(0xFF087F5B).copy(alpha = 0.14f * pulse), radius = size.minDimension * 0.48f * pulse)
                        drawCircle(Color(0xFF087F5B).copy(alpha = 0.20f), radius = size.minDimension * 0.17f)
                    }
                }
            }
        }
    }
}

typealias AnyDestination = V2Destination
typealias AnyVehicle = V2Vehicle

@Composable
private fun HomeSheetContent(
    sessionStore: SessionStore,
    pickupLabel: String,
    destination: AnyDestination?,
    vehicle: AnyVehicle,
    estimate: FareEstimate?,
    estimateLoading: Boolean,
    locationLoading: Boolean,
    requesting: Boolean,
    error: String?,
    paymentMethod: String,
    walletBalance: Long?,
    onRefreshLocation: () -> Unit,
    onDestinationPicked: (Coordinates) -> Unit,
    onVehicle: (AnyVehicle) -> Unit,
    onPayment: (String) -> Unit,
    onRequest: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(42.dp, 4.dp).background(Color(0xFFD8E0DC), RoundedCornerShape(4.dp)))
        Text("جاهز لمشوارك؟", color = Color(0xFF12201B), fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
        Text("اختار وجهتك والسيارة المناسبة", color = Color(0xFF6D7A75), fontSize = 11.sp)

        HomeRouteItem("من", pickupLabel, Icons.Default.MyLocation, false, locationLoading, onRefreshLocation)
        HomeRouteConnector()
        HomeRouteItem("إلى", destination?.title ?: "وين نوصلك؟ اضغط هنا أو حرّك الخريطة", Icons.Default.LocationOn, true, false) { }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
            items(v2Vehicles, key = { it.id }) { option ->
                VehicleChip(option, option.id == vehicle.id) { onVehicle(option) }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentChip("نقداً", "cash", paymentMethod == "cash") { onPayment("cash") }
            PaymentChip("المحفظة", "wallet", paymentMethod == "wallet", walletBalance) { onPayment("wallet") }
        }

        if (estimate != null || estimateLoading) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color(0xFFF5F8F6)), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CreditCard, null, tint = Color(0xFF087F5B), modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("الأجرة التقديرية", color = Color(0xFF6D7A75), fontSize = 9.sp)
                        Text(if (estimateLoading) "جاري الحساب…" else "${estimate!!.estimatedFare} ${estimate!!.currency}", color = Color(0xFF12201B), fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Black)
                    }
                    if (paymentMethod == "wallet") Text("رصيد المحفظة: ${walletBalance ?: 0} ل.س", color = Color(0xFF087F5B), fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
            }
        }

        if (!error.isNullOrBlank()) {
            Text(error, color = Color(0xFFB42318), fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }

        Button(
            onClick = onRequest,
            enabled = !requesting && destination != null && (paymentMethod != "wallet" || (walletBalance ?: 0L) >= (estimate?.estimatedFare ?: Long.MAX_VALUE)),
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F4DBA))
        ) {
            if (requesting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("اطلب التكسي", color = Color.White, fontWeight = androidx.compose.ui.text.font.FontWeight.Black, fontSize = 15.sp)
        }
    }
}

@Composable
private fun HomeRouteItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, accent: Boolean, loading: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(if (accent) Color.White else Color(0xFFF2F6F4)), shape = RoundedCornerShape(17.dp), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(if (accent) Color(0xFFE7F6F0) else Color.White, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color(0xFF087F5B), modifier = Modifier.size(19.dp)) }
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) { Text(label, color = Color(0xFF6D7A75), fontSize = 9.sp); Text(value, color = Color(0xFF12201B), fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFF087F5B), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun HomeRouteConnector() { Row(Modifier.padding(start = 17.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.width(2.dp).height(8.dp).background(Color(0xFFB8E986))); Spacer(Modifier.width(6.dp)); Text("مسار الرحلة", color = Color(0xFF8A9791), fontSize = 8.sp) } }

@Composable
private fun VehicleChip(vehicle: AnyVehicle, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.clickable { onClick() }, colors = CardDefaults.cardColors(if (selected) Color(0xFFE7F6F0) else Color(0xFFF5F7F6)), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFF087F5B) else Color(0xFFDDE5E1)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DirectionsCar, null, tint = if (selected) Color(0xFF087F5B) else Color(0xFF6D7A75), modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(vehicle.title, color = Color(0xFF12201B), fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
    }
}

@Composable
private fun PaymentChip(title: String, value: String, selected: Boolean, balance: Long? = null, onClick: () -> Unit) {
    Card(Modifier.weight(1f).clickable { onClick() }, colors = CardDefaults.cardColors(if (selected) Color(0xFFE7F6F0) else Color(0xFFF6F8F7)), shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFF087F5B) else Color(0xFFDDE5E1)), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected = selected, onClick = onClick); Column { Text(title, color = Color(0xFF12201B), fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold); if (value == "wallet") Text("${balance ?: 0} ل.س", color = Color(0xFF087F5B), fontSize = 9.sp) } }
    }
}

@Composable
private fun NearbyDriverMarker(index: Int) {
    Box(Modifier.fillMaxSize().padding(start = (26 + (index % 4) * 66).dp, top = (165 + (index % 5) * 58).dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(30.dp).background(Color.White, CircleShape).padding(3.dp).background(Color(0xFF087F5B), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(17.dp)) }
    }
}
