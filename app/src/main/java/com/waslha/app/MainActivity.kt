package com.waslha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WaslhaGreen = Color(0xFF0B8F63)
private val WaslhaGreenDark = Color(0xFF08704E)
private val WaslhaInk = Color(0xFF101817)
private val WaslhaMuted = Color(0xFF687571)
private val WaslhaSurface = Color(0xFFF5F8F7)

private data class RideType(val id: String, val title: String, val subtitle: String, val price: String, val eta: String, val icon: String, val featured: Boolean = false)
private val rideTypes = listOf(
    RideType("economy", "اقتصادي", "سعر مناسب للرحلات اليومية", "من 3,500 ل.س", "3–6 د", "🚕", true),
    RideType("comfort", "مريح", "سيارة أحدث ومساحة أفضل", "من 5,000 ل.س", "4–8 د", "🚘"),
    RideType("family", "عائلي", "مساحة أكبر للركاب والأمتعة", "من 6,500 ل.س", "6–10 د", "🚙")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WaslhaApp() }
    }
}

@Composable
private fun WaslhaApp() {
    var tab by remember { mutableIntStateOf(0) }
    var destination by remember { mutableStateOf("") }
    var showDestination by remember { mutableStateOf(false) }
    var selectedRide by remember { mutableStateOf<RideType?>(null) }
    var searching by remember { mutableStateOf(false) }
    var activeTrip by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = WaslhaSurface) {
            Box(Modifier.fillMaxSize()) {
                when {
                    activeTrip -> ActiveTripScreen(onCancel = { activeTrip = false })
                    searching -> SearchingScreen(onCancel = { searching = false })
                    tab == 0 -> HomeScreen(
                        destination = destination,
                        onDestination = { showDestination = true },
                        onRequest = { searching = true },
                        selectedRide = selectedRide
                    )
                    tab == 1 -> TripsScreen()
                    else -> ProfileScreen()
                }
                if (!searching && !activeTrip) {
                    BottomBar(tab = tab, onTab = { tab = it })
                }
            }
        }
    }

    if (showDestination) {
        DestinationDialog(
            initial = destination,
            onDismiss = { showDestination = false },
            onConfirm = { destination = it; showDestination = false }
        )
    }

    if (!activeTrip && selectedRide == null && destination.isNotBlank()) {
        // Ride selection is surfaced by the home CTA after destination entry.
    }
}

@Composable
private fun HomeScreen(destination: String, onDestination: () -> Unit, onRequest: () -> Unit, selectedRide: RideType?) {
    var rideIndex by remember { mutableIntStateOf(0) }
    val ride = rideTypes[rideIndex]

    Column(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFFDDE8E4)),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(Modifier.fillMaxSize().padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).background(WaslhaGreen, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text("وصلها", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = WaslhaGreen)
                        }
                    }
                    Row {
                        IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "الإشعارات", tint = WaslhaInk) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Person, "الحساب", tint = WaslhaInk) }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Card(colors = CardDefaults.cardColors(Color.White.copy(alpha = .94f)), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = WaslhaGreen)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("موقعك الحالي", fontSize = 12.sp, color = WaslhaMuted)
                            Text("تحديد تلقائي للموقع", fontWeight = FontWeight.SemiBold, color = WaslhaInk)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.Tune, null, tint = WaslhaMuted)
                    }
                }
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).background(WaslhaGreen.copy(alpha = .14f), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(38.dp), tint = WaslhaGreen)
                }
                Spacer(Modifier.height(10.dp))
                Text("الخريطة الحية", fontWeight = FontWeight.Bold, color = WaslhaInk)
                Text("سيتم وضع Mapbox هنا", fontSize = 12.sp, color = WaslhaMuted)
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            colors = CardDefaults.cardColors(Color.White),
            elevation = CardDefaults.cardElevation(10.dp)
        ) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 84.dp)) {
                Text("إلى أين؟", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = WaslhaInk)
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth().clickable { onDestination() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(WaslhaSurface)) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = WaslhaGreen)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(if (destination.isBlank()) "حدد وجهتك" else destination, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (destination.isBlank()) WaslhaMuted else WaslhaInk)
                            Text("ابحث عن عنوان أو مكان", fontSize = 12.sp, color = WaslhaMuted)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("اختر نوع التكسي", fontWeight = FontWeight.Bold, color = WaslhaInk)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    rideTypes.forEachIndexed { index, item ->
                        RideMiniCard(item, selected = rideIndex == index, onClick = { rideIndex = index })
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onRequest,
                    enabled = destination.isNotBlank(),
                    Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WaslhaGreen, disabledContainerColor = Color(0xFFCBD5D1))
                ) {
                    Text(if (destination.isBlank()) "حدد الوجهة أولاً" else "اطلب سيارة • ${ride.price}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RideMiniCard(ride: RideType, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.weight(1f).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(if (selected) WaslhaGreen.copy(alpha = .10f) else WaslhaSurface),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, WaslhaGreen) else null
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(ride.icon, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(ride.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = WaslhaInk)
            Text(ride.eta, fontSize = 11.sp, color = WaslhaMuted)
        }
    }
}

@Composable
private fun DestinationDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تحديد الوجهة", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("اكتب اسم المكان أو العنوان", color = WaslhaMuted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                TextField(value = value, onValueChange = { value = it }, singleLine = true, placeholder = { Text("مثال: ساحة الأمويين") })
                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(WaslhaSurface), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = WaslhaGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("استخدام الدبوس على الخريطة", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onConfirm(value) }) { Text("تأكيد", color = WaslhaGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun SearchingScreen(onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(104.dp).background(WaslhaGreen.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(52.dp), tint = WaslhaGreen)
        }
        Spacer(Modifier.height(22.dp))
        Text("جاري البحث عن كابتن", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = WaslhaInk)
        Text("نبحث عن أقرب سيارة متاحة لموقعك", color = WaslhaMuted)
        Spacer(Modifier.height(28.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(18.dp)) {
                StatusLine("تم تحديد موقع الانطلاق", true)
                StatusLine("جاري العثور على كابتن قريب", false)
                StatusLine("تأكيد الرحلة", false)
            }
        }
        Spacer(Modifier.height(22.dp))
        OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(16.dp)) {
            Icon(Icons.Default.Close, null)
            Spacer(Modifier.width(8.dp))
            Text("إلغاء الطلب")
        }
    }
}

@Composable
private fun StatusLine(text: String, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Default.CheckCircle else Icons.Default.DirectionsCar, null, tint = if (done) WaslhaGreen else WaslhaMuted, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, color = if (done) WaslhaInk else WaslhaMuted, fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ActiveTripScreen(onCancel: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Text("رحلتك الحالية", fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFDDE8E4), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(58.dp), tint = WaslhaGreen)
                Text("تتبع الرحلة مباشرة", fontWeight = FontWeight.Bold)
                Text("المسار والسائق سيظهران هنا مع ربط الخرائط", color = WaslhaMuted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("الكابتن في الطريق إليك", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).background(WaslhaSurface, CircleShape), contentAlignment = Alignment.Center) { Text("👨🏻", fontSize = 24.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("كابتن وصلها", fontWeight = FontWeight.Bold); Text("Toyota Corolla • 1234", fontSize = 12.sp, color = WaslhaMuted) }
                    Column(horizontalAlignment = Alignment.End) { Text("4.9 ★", fontWeight = FontWeight.Bold, color = WaslhaGreen); Text("3 د", color = WaslhaMuted) }
                }
                Divider(Modifier.padding(vertical = 14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("الأجرة المتوقعة", color = WaslhaMuted); Text("3,500–4,200 ل.س", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(14.dp))
                OutlinedButton(onClick = onCancel, Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("إلغاء الرحلة") }
            }
        }
    }
}

@Composable
private fun TripsScreen() {
    val trips = listOf("اليوم • ساحة الأمويين → المزة", "أمس • باب توما → كفرسوسة", "الثلاثاء • جرمانا → وسط دمشق")
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("رحلاتي", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = WaslhaInk)
        Text("سجل رحلاتك مع وصلها", color = WaslhaMuted)
        Spacer(Modifier.height(18.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)) {
            items(trips) { trip ->
                Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(WaslhaGreen.copy(alpha = .1f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = WaslhaGreen) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) { Text(trip, fontWeight = FontWeight.SemiBold); Text("مكتملة • نقداً", fontSize = 12.sp, color = WaslhaMuted) }
                        Text("4,000 ل.س", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("حسابي", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = WaslhaInk)
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(62.dp).background(WaslhaGreen.copy(alpha = .12f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, modifier = Modifier.size(34.dp), tint = WaslhaGreen) }
                Spacer(Modifier.width(14.dp))
                Column { Text("ضيف وصلها", fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("سجّل دخولك لإدارة حسابك", color = WaslhaMuted, fontSize = 13.sp) }
            }
        }
        Spacer(Modifier.height(14.dp))
        ProfileRow("بيانات الحساب", "تعديل الاسم ورقم الهاتف")
        ProfileRow("طرق الدفع", "النقد حالياً • خيارات إضافية لاحقاً")
        ProfileRow("الإشعارات", "تنبيهات الرحلة والعروض")
        ProfileRow("المساعدة والدعم", "تواصل مع وصلها")
        ProfileRow("الشروط والخصوصية", "سياسة الاستخدام والخصوصية")
    }
}

@Composable
private fun ProfileRow(title: String, subtitle: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, fontSize = 12.sp, color = WaslhaMuted) }
            Icon(Icons.Default.ArrowBack, null, tint = WaslhaMuted)
        }
    }
}

@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White, modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
        NavigationBarItem(selected = tab == 0, onClick = { onTab(0) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("الرئيسية") })
        NavigationBarItem(selected = tab == 1, onClick = { onTab(1) }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("رحلاتي") })
        NavigationBarItem(selected = tab == 2, onClick = { onTab(2) }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("حسابي") })
    }
}
