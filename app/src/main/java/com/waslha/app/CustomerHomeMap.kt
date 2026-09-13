package com.waslha.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeMap(
    pickup: Coordinates,
    pickupLabel: String,
    destination: V2Destination?,
    vehicle: V2Vehicle,
    estimate: FareEstimate?,
    estimateLoading: Boolean,
    locationLoading: Boolean,
    requesting: Boolean,
    error: String?,
    onRefreshLocation: () -> Unit,
    onDestinationPicked: (Coordinates) -> Unit,
    onVehicle: (V2Vehicle) -> Unit,
    onRequest: (String) -> Unit
) {
    val customerRepo = remember { CustomerRepository(ApiProvider.api) }
    var paymentMethod by remember { mutableStateOf("cash") }
    var walletBalance by remember { mutableStateOf<Long?>(null) }
    var nearby by remember { mutableStateOf<List<NearbyDriverDto>>(emptyList()) }

    LaunchedEffect(Unit) {
        customerRepo.wallet().onSuccess { walletBalance = it.balance }
    }

    LaunchedEffect(vehicle.id, pickup.lat, pickup.lng) {
        while (true) {
            customerRepo.nearbyDrivers(vehicle.id).onSuccess { nearby = it }
            delay(8000)
        }
    }

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    val estimatedFareLong = estimate?.estimatedFare?.toLong()
    val walletCanPay = estimatedFareLong != null && (walletBalance ?: 0L) >= estimatedFareLong
    val canRequest = !requesting && destination != null &&
        (paymentMethod == "cash" || walletCanPay)

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 280.dp,
        sheetSwipeEnabled = true,
        containerColor = Color.Transparent,
        sheetContainerColor = Color.White,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetTonalElevation = 0.dp,
        sheetShadowElevation = 14.dp,
        sheetContent = {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.align(Alignment.CenterHorizontally).size(42.dp, 4.dp)
                        .background(Color(0xFFD8E0DC), RoundedCornerShape(4.dp))
                )
                Text("وين نوصلك؟", color = Color(0xFF12201B), fontSize = 22.sp, fontWeight = FontWeight.Black)
                Text("اختر نقطة الانطلاق والوجهة ثم نوع السيارة", color = Color(0xFF6D7A75), fontSize = 11.sp)
                SheetRouteRow("من", pickupLabel, Icons.Default.MyLocation, locationLoading, onRefreshLocation)
                SheetRouteRow("إلى", destination?.title ?: "حدد وجهتك على الخريطة", Icons.Default.LocationOn)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
                    items(v2Vehicles, key = { it.id }) { option ->
                        VehicleChip(option, option.id == vehicle.id) { onVehicle(option) }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentChip("نقداً", paymentMethod == "cash", null) { paymentMethod = "cash" }
                    PaymentChip("المحفظة", paymentMethod == "wallet", walletBalance) { paymentMethod = "wallet" }
                }
                if (paymentMethod == "wallet") {
                    Text("رصيد المحفظة: ${walletBalance ?: 0} ل.س", color = Color(0xFF087F5B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (estimate != null || estimateLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(Color(0xFFF5F8F6)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, null, tint = Color(0xFF087F5B), modifier = Modifier.size(19.dp))
                            Box(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text("الأجرة التقديرية", color = Color(0xFF6D7A75), fontSize = 9.sp)
                                Text(
                                    if (estimateLoading) "جاري الحساب…" else "${estimate?.estimatedFare ?: 0} ل.س",
                                    color = Color(0xFF12201B), fontSize = 16.sp, fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
                if (!error.isNullOrBlank()) Text(error, color = Color(0xFFB42318), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Button(
                    onClick = { onRequest(paymentMethod) },
                    enabled = canRequest,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6F4DBA))
                ) {
                    if (requesting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    else Text("اطلب التكسي", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            WaslhaRideMap(
                pickup = pickup,
                destination = destination?.coordinates,
                modifier = Modifier.fillMaxSize(),
                onDestinationPicked = onDestinationPicked
            )
            if (nearby.isNotEmpty() && destination == null) {
                nearby.take(8).forEachIndexed { index, _ ->
                    NearbyDriverMarker(index)
                }
            }
        }
    }
}

@Composable
private fun SheetRouteRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    loading: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(Color(0xFFF5F8F6)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFDDE5E1)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).background(Color(0xFFE7F6F0), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF087F5B), modifier = Modifier.size(19.dp))
            }
            Box(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = Color(0xFF6D7A75), fontSize = 9.sp)
                Text(value, color = Color(0xFF12201B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFF087F5B), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun VehicleChip(vehicle: V2Vehicle, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier,
        colors = CardDefaults.cardColors(if (selected) Color(0xFFE7F6F0) else Color(0xFFF5F7F6)),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, if (selected) Color(0xFF087F5B) else Color(0xFFDDE5E1)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF087F5B), modifier = Modifier.size(18.dp))
            Box(Modifier.width(6.dp))
            Text(vehicle.title, color = Color(0xFF12201B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PaymentChip(title: String, selected: Boolean, balance: Long?, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(if (selected) Color(0xFFE7F6F0) else Color(0xFFF6F8F7)),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, if (selected) Color(0xFF087F5B) else Color(0xFFDDE5E1)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(horizontal = 7.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onClick)
            Column {
                Text(title, color = Color(0xFF12201B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (balance != null) Text("${balance} ل.س", color = Color(0xFF087F5B), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun NearbyDriverMarker(index: Int) {
    Box(
        Modifier.fillMaxSize().padding(start = (24 + (index % 4) * 64).dp, top = (160 + (index % 5) * 55).dp),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.size(30.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF087F5B), modifier = Modifier.size(18.dp))
        }
    }
}
