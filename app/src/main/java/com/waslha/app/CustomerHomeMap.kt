package com.waslha.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Canvas
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

private const val WASLHA_MAX_SERVICE_DISTANCE_KM = 100.0

private val SheetBackground = Color.White
private val SurfaceSoft = Color(0xFFF5F7FA)
private val SurfaceSoftGreen = Color(0xFFEAF6F1)
private val Ink = Color(0xFF12201B)
private val Muted = Color(0xFF6D7A75)
private val Line = Color(0xFFDDE5E1)
private val Primary = Color(0xFF087F5B)
private val AccentPurple = Color(0xFF6F4DBA)
private val ErrorRed = Color(0xFFB42318)

private fun isInsideSyria(point: Coordinates): Boolean =
    point.lat in 32.0..37.5 && point.lng in 35.5..42.5

private fun distanceKm(a: Coordinates, b: Coordinates): Double {
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(b.lat - a.lat)
    val dLng = Math.toRadians(b.lng - a.lng)
    val lat1 = Math.toRadians(a.lat)
    val lat2 = Math.toRadians(b.lat)
    val h = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(lat1) * kotlin.math.cos(lat2) * kotlin.math.sin(dLng / 2).let { it * it }
    return 2.0 * earthRadiusKm * kotlin.math.asin(kotlin.math.sqrt(h.coerceIn(0.0, 1.0)))
}

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
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var paymentMethod by rememberSaveable { mutableStateOf("cash") }
    var walletBalance by rememberSaveable { mutableStateOf<Long?>(null) }
    var persistedSheetCollapsed by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        customerRepo.wallet().onSuccess { walletBalance = it.balance }
    }

    val destinationAllowed = destination?.coordinates?.let { point ->
        isInsideSyria(pickup) && isInsideSyria(point) && distanceKm(pickup, point) <= WASLHA_MAX_SERVICE_DISTANCE_KM
    } ?: true

    val serviceError = if (destination != null && !destinationAllowed) {
        "الوجهة المحددة خارج نطاق الخدمة المتاح حالياً"
    } else null

    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)

    LaunchedEffect(persistedSheetCollapsed) {
        if (persistedSheetCollapsed) {
            scaffoldState.bottomSheetState.partialExpand()
        }
    }

    DisposableEffect(lifecycleOwner, scaffoldState) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && persistedSheetCollapsed) {
                scope.launch { scaffoldState.bottomSheetState.partialExpand() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(scaffoldState.bottomSheetState.currentValue) {
        persistedSheetCollapsed = scaffoldState.bottomSheetState.currentValue == SheetValue.PartiallyExpanded
    }

    val estimatedFareLong = estimate?.estimatedFare?.toLong()
    val walletCanPay = estimatedFareLong != null && (walletBalance ?: 0L) >= estimatedFareLong
    val canRequest = !requesting &&
        destination != null &&
        destinationAllowed &&
        (paymentMethod == "cash" || walletCanPay)

    Box(Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 284.dp,
            sheetSwipeEnabled = true,
            containerColor = Color.Transparent,
            sheetContainerColor = SheetBackground,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetTonalElevation = 0.dp,
            sheetShadowElevation = 16.dp,
            sheetContent = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFD8DEDA))
                    )
                    Text(
                        "وين نوصلك؟",
                        color = Ink,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "حدد موقعك والوجهة ثم اختر السيارة المناسبة",
                        color = Muted,
                        fontSize = 11.sp
                    )

                    DestinationFields(
                        pickupLabel = pickupLabel,
                        destinationLabel = destination?.title ?: "حدد وجهتك على الخريطة",
                        locationLoading = locationLoading,
                        onRefreshLocation = onRefreshLocation
                    )

                    if (serviceError != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(Color(0xFFFFF3F1)),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Text(
                                serviceError,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                color = ErrorRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(v2Vehicles, key = { it.id }) { option ->
                            VehicleChip(option, option.id == vehicle.id) { onVehicle(option) }
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentChip("نقداً", paymentMethod == "cash", null) {
                            paymentMethod = "cash"
                        }
                        PaymentChip("المحفظة", paymentMethod == "wallet", walletBalance) {
                            paymentMethod = "wallet"
                        }
                    }

                    if (paymentMethod == "wallet") {
                        Text(
                            "رصيد المحفظة: ${walletBalance ?: 0} ل.س",
                            color = Primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (estimate != null || estimateLoading) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(SurfaceSoftGreen),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    null,
                                    tint = Primary,
                                    modifier = Modifier.size(19.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("الأجرة التقديرية", color = Muted, fontSize = 9.sp)
                                    Text(
                                        if (estimateLoading) "جاري الحساب…" else "${estimate?.estimatedFare ?: 0} ل.س",
                                        color = Ink,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }

                    if (!error.isNullOrBlank()) {
                        Text(
                            error,
                            color = ErrorRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { if (canRequest) onRequest(paymentMethod) },
                        enabled = canRequest,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple,
                            disabledContainerColor = Color(0xFFE2DFEA),
                            disabledContentColor = Color(0xFF8E879A)
                        )
                    ) {
                        if (requesting) {
                            CircularProgressIndicator(
                                Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "اطلب التكسي",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                    Spacer(Modifier.navigationBarsPadding().height(2.dp))
                }
            }
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                WaslhaRideMap(
                    pickup = pickup,
                    destination = if (destinationAllowed) destination?.coordinates else null,
                    modifier = Modifier.fillMaxSize(),
                    onDestinationPicked = { picked ->
                        if (!isInsideSyria(picked) || distanceKm(pickup, picked) > WASLHA_MAX_SERVICE_DISTANCE_KM) {
                            return@WaslhaRideMap
                        }
                        onDestinationPicked(picked)
                    }
                )
            }
        }
    }
}

@Composable
private fun DestinationFields(
    pickupLabel: String,
    destinationLabel: String,
    locationLoading: Boolean,
    onRefreshLocation: () -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        Column(
            Modifier.width(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RouteIconCircle(Icons.Default.MyLocation, SurfaceSoftGreen, Primary)
            Canvas(
                modifier = Modifier
                    .width(22.dp)
                    .height(28.dp)
            ) {
                drawLine(
                    color = Color(0xFFB9C5BF),
                    start = Offset(size.width / 2f, 3.dp.toPx()),
                    end = Offset(size.width / 2f, size.height - 3.dp.toPx()),
                    strokeWidth = 1.4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                    )
                )
            }
            RouteIconCircle(Icons.Default.LocationOn, Color(0xFFFFF0EE), Color(0xFFD93838))
        }
        Spacer(Modifier.width(8.dp))
        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DestinationCard(
                label = "من",
                value = pickupLabel,
                icon = Icons.Default.MyLocation,
                iconBackground = SurfaceSoftGreen,
                iconTint = Primary,
                loading = locationLoading,
                onClick = onRefreshLocation
            )
            DestinationCard(
                label = "إلى",
                value = destinationLabel,
                icon = Icons.Default.LocationOn,
                iconBackground = Color(0xFFFFF0EE),
                iconTint = Color(0xFFD93838),
                loading = false,
                onClick = {}
            )
        }
    }
}

@Composable
private fun DestinationCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBackground: Color,
    iconTint: Color,
    loading: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(SurfaceSoft),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(value, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            if (loading) {
                CircularProgressIndicator(
                    Modifier.size(18.dp),
                    color = Primary,
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

@Composable
private fun RouteIconCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    background: Color,
    tint: Color
) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(13.dp))
    }
}

@Composable
private fun VehicleChip(vehicle: V2Vehicle, selected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier,
        colors = CardDefaults.cardColors(
            if (selected) SurfaceSoftGreen else SurfaceSoft
        ),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(
            1.dp,
            if (selected) Primary else Line
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.DirectionsCar,
                null,
                tint = Primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                vehicle.title,
                color = Ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PaymentChip(
    title: String,
    selected: Boolean,
    balance: Long?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
            if (selected) SurfaceSoftGreen else SurfaceSoft
        ),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(
            1.dp,
            if (selected) Primary else Line
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column {
                Text(title, color = Ink, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                if (balance != null) {
                    Text(
                        "${balance} ل.س",
                        color = Primary,
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
