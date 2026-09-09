package com.waslha.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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

private val SafeGreen = Color(0xFF078A60)
private val SafeInk = Color(0xFF10201B)
private val SafeMuted = Color(0xFF72807B)
private val SafeBg = Color(0xFFF5F8F6)
private val SafeDanger = Color(0xFFB42318)

private data class TaxiDestination(val title: String, val subtitle: String, val coordinates: Coordinates)

private val destinations = listOf(
    TaxiDestination("ساحة الأمويين", "دمشق", Coordinates(33.5138, 36.2765)),
    TaxiDestination("جامعة دمشق", "المزة - دمشق", Coordinates(33.5101, 36.2766)),
    TaxiDestination("سوق الحميدية", "المدينة القديمة", Coordinates(33.5112, 36.3051)),
    TaxiDestination("المزة", "دمشق", Coordinates(33.4941, 36.2384)),
    TaxiDestination("محطة الحجاز", "دمشق", Coordinates(33.5070, 36.2895))
)

class SafeTaxiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        val sessionStore = SessionStore(this)
        setContent {
            SafeTaxiHome(
                sessionStore = sessionStore,
                onLogout = {
                    sessionStore.clear()
                    finish()
                }
            )
        }
    }
}

@Composable
private fun SafeTaxiHome(sessionStore: SessionStore, onLogout: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val locationProvider = remember { LocationProvider(context) }
    val repository = remember { TripRepository(ApiProvider.api) }
    val scope = rememberCoroutineScope()

    var pickup by remember { mutableStateOf(Coordinates(33.5138, 36.2765)) }
    var locationLabel by remember { mutableStateOf("دمشق - الموقع التقريبي") }
    var destination by remember { mutableStateOf<TaxiDestination?>(null) }
    var destinationDialog by remember { mutableStateOf(false) }
    var loadingLocation by remember { mutableStateOf(false) }
    var requesting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var trip by remember { mutableStateOf<Trip?>(null) }

    val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result[Manifest.permission.ACCESS_FINE_LOCATION] == true || result[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            loadingLocation = true
            scope.launch {
                pickup = locationProvider.lastKnown()?.let { Coordinates(it.latitude, it.longitude) } ?: pickup
                locationLabel = "موقعك الحالي"
                loadingLocation = false
            }
        } else {
            error = "اسمح بالوصول إلى الموقع حتى نحدد نقطة الانطلاق بدقة"
        }
    }

    LaunchedEffect(Unit) {
        if (permissionGranted) {
            loadingLocation = true
            pickup = locationProvider.lastKnown()?.let { Coordinates(it.latitude, it.longitude) } ?: pickup
            locationLabel = "موقعك الحالي"
            loadingLocation = false
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = SafeBg) {
            if (trip != null) {
                TaxiRequestState(
                    trip = trip!!,
                    onCancel = {
                        val current = trip ?: return@TaxiRequestState
                        requesting = true
                        scope.launch {
                            repository.cancel(current.id, "إلغاء من الراكب")
                                .onSuccess { trip = it }
                                .onFailure { error = it.message ?: "تعذر إلغاء الرحلة" }
                            requesting = false
                        }
                    },
                    requesting = requesting,
                    error = error,
                    onBack = { trip = null; error = null }
                )
            } else {
                Column(
                    Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("وصلها", color = SafeGreen, fontSize = 30.sp, fontWeight = FontWeight.Black)
                            Text(
                                sessionStore.name?.takeIf { it.isNotBlank() }?.let { "أهلاً $it" } ?: "أهلاً بك",
                                color = SafeMuted,
                                fontSize = 13.sp
                            )
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.Logout, contentDescription = "تسجيل الخروج", tint = SafeInk)
                        }
                    }

                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(Color.White),
                        elevation = CardDefaults.cardElevation(3.dp)
                    ) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, null, tint = SafeGreen, modifier = Modifier.size(30.dp))
                                Spacer(Modifier.size(10.dp))
                                Column {
                                    Text("احجز تاكسي", fontSize = 21.sp, fontWeight = FontWeight.Black, color = SafeInk)
                                    Text("حدد مكانك ووجهتك ثم اطلب المشوار", color = SafeMuted, fontSize = 12.sp)
                                }
                            }

                            Card(
                                Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(SafeBg)
                            ) {
                                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MyLocation, null, tint = SafeGreen, modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.size(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("نقطة الانطلاق", fontSize = 10.sp, color = SafeMuted)
                                        Text(locationLabel, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafeInk)
                                    }
                                    IconButton(
                                        onClick = {
                                            if (!permissionGranted) {
                                                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                            } else {
                                                loadingLocation = true
                                                scope.launch {
                                                    pickup = locationProvider.lastKnown()?.let { Coordinates(it.latitude, it.longitude) } ?: pickup
                                                    locationLabel = "موقعك الحالي"
                                                    loadingLocation = false
                                                }
                                            }
                                        }
                                    ) {
                                        if (loadingLocation) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = SafeGreen)
                                        else Icon(Icons.Default.LocationOn, "تحديث الموقع", tint = SafeGreen)
                                    }
                                }
                            }

                            Button(
                                onClick = { destinationDialog = true },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Place, null)
                                Spacer(Modifier.size(8.dp))
                                Text(destination?.title ?: "اختيار الوجهة")
                            }

                            destination?.let { selected ->
                                Card(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(Color(0xFFE8F6F0))
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        Text("الوجهة المختارة", fontSize = 10.sp, color = SafeMuted)
                                        Text(selected.title, fontSize = 16.sp, fontWeight = FontWeight.Black, color = SafeInk)
                                        Text(selected.subtitle, fontSize = 11.sp, color = SafeMuted)
                                    }
                                }
                            }

                            Button(
                                enabled = destination != null && !requesting,
                                onClick = {
                                    val selected = destination ?: return@Button
                                    requesting = true
                                    error = null
                                    scope.launch {
                                        repository.create(
                                            TripRequest(
                                                customerId = sessionStore.userId.orEmpty(),
                                                pickup = pickup,
                                                destination = selected.coordinates,
                                                vehicleType = "economy",
                                                paymentMethod = "cash"
                                            )
                                        ).onSuccess { trip = it }
                                            .onFailure { error = it.message ?: "تعذر طلب التاكسي" }
                                        requesting = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (requesting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                else Text("طلب تاكسي", fontWeight = FontWeight.Bold)
                            }

                            error?.let {
                                Text(it, color = SafeDanger, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Text(
                        "الخدمة الحالية مخصصة للتاكسي فقط",
                        color = SafeMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            if (destinationDialog) {
                AlertDialog(
                    onDismissRequest = { destinationDialog = false },
                    title = { Text("اختيار الوجهة", fontWeight = FontWeight.Black) },
                    text = {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(destinations) { item ->
                                Card(
                                    Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(Color.White)
                                ) {
                                    TextButton(
                                        onClick = { destination = item; destinationDialog = false },
                                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                                    ) {
                                        Column(Modifier.fillMaxWidth()) {
                                            Text(item.title, fontWeight = FontWeight.Bold, color = SafeInk)
                                            Text(item.subtitle, fontSize = 11.sp, color = SafeMuted)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }
        }
    }
}

@Composable
private fun TaxiRequestState(
    trip: Trip,
    onCancel: () -> Unit,
    requesting: Boolean,
    error: String?,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("طلب الرحلة", color = SafeGreen, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(trip.statusLabel(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = SafeInk)
                Text("المسافة: ${trip.distanceKm} كم", color = SafeMuted, fontSize = 12.sp)
                Text("الوقت التقريبي: ${trip.durationMin} دقيقة", color = SafeMuted, fontSize = 12.sp)
                Text("الأجرة التقديرية: ${trip.estimatedFare} ${trip.currency}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SafeGreen)
                Spacer(Modifier.height(8.dp))
                if (trip.driver != null) {
                    Text("الكابتن: ${trip.driver.name}", fontWeight = FontWeight.Bold, color = SafeInk)
                    Text("المركبة: ${trip.driver.vehicle} — ${trip.driver.plate}", color = SafeMuted, fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !requesting && trip.status != "completed" && trip.status != "cancelled",
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("إلغاء الرحلة")
                }
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text("العودة للرئيسية")
                }
                error?.let { Text(it, color = SafeDanger, fontSize = 11.sp) }
            }
        }
    }
}
