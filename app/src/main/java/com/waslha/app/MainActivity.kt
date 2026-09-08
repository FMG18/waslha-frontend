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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.lifecycle.viewmodel.compose.viewModel

private val WaslhaGreen = Color(0xFF078A60)
private val WaslhaGreenDark = Color(0xFF056C4B)
private val WaslhaInk = Color(0xFF10201B)
private val WaslhaMuted = Color(0xFF6D7B76)
private val WaslhaSurface = Color(0xFFF3F7F5)
private val WaslhaMap = Color(0xFFDCE8E2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WaslhaApp() }
    }
}

@Composable
private fun WaslhaApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sessionStore = remember { SessionStore(context) }
    val api = remember { WaslhaApiClient.create(sessionStore) }
    val authRepository = remember { AuthRepository(api, sessionStore) }
    val authViewModel: PhoneAuthViewModel = viewModel(factory = PhoneAuthViewModelFactory(authRepository))
    var signedIn by remember { mutableStateOf(sessionStore.isSignedIn) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = WaslhaSurface) {
            if (!signedIn) {
                PhoneAuthScreen(authViewModel) { signedIn = true }
            } else {
                PassengerApp(onSignedOut = { sessionStore.clear(); signedIn = false })
            }
        }
    }
}

@Composable
private fun PassengerApp(onSignedOut: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var destination by remember { mutableStateOf("") }
    var showDestination by remember { mutableStateOf(false) }
    var booking by remember { mutableStateOf(false) }
    var activeTrip by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        when {
            activeTrip -> ActiveTripScreen(onBack = { activeTrip = false })
            booking -> SearchCaptainScreen(destination = destination, onCancel = { booking = false }, onFound = { activeTrip = true; booking = false })
            tab == 0 -> HomeScreen(
                destination = destination,
                onPickDestination = { showDestination = true },
                onRequest = { booking = true }
            )
            tab == 1 -> TripsScreen()
            else -> ProfileScreen(onSignedOut)
        }
        if (!booking && !activeTrip) BottomBar(tab, { tab = it })
    }

    if (showDestination) {
        DestinationDialog(
            initial = destination,
            onDismiss = { showDestination = false },
            onConfirm = { destination = it; showDestination = false }
        )
    }
}

@Composable
private fun HomeScreen(destination: String, onPickDestination: () -> Unit, onRequest: () -> Unit) {
    var selectedType by remember { mutableIntStateOf(0) }
    val types = listOf(
        Triple("اقتصادي", "3,500 ل.س", "🚕"),
        Triple("مريح", "5,000 ل.س", "🚘"),
        Triple("عائلي", "6,500 ل.س", "🚙")
    )

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxWidth().weight(1f).background(WaslhaMap)
            ) {
                MapPreview()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("وصلها", fontSize = 21.sp, fontWeight = FontWeight.Black, color = WaslhaGreen)
                            Spacer(Modifier.width(7.dp))
                            Text("🚕", fontSize = 17.sp)
                        }
                    }
                    IconButton(
                        onClick = {},
                        modifier = Modifier.background(Color.White, CircleShape)
                    ) { Icon(Icons.Default.NotificationsNone, "الإشعارات", tint = WaslhaInk) }
                }
                Card(
                    Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp, vertical = 18.dp),
                    colors = CardDefaults.cardColors(Color.White),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(WaslhaGreen, CircleShape))
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("موقعك الحالي", fontSize = 11.sp, color = WaslhaMuted)
                            Text("سيتم تحديد موقعك تلقائياً", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                colors = CardDefaults.cardColors(Color.White)
            ) {
                Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 19.dp, bottom = 88.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("وين نوصلك؟", fontSize = 24.sp, fontWeight = FontWeight.Black, color = WaslhaInk)
                            Text("اختار وجهتك وخلي الباقي علينا", color = WaslhaMuted, fontSize = 12.sp)
                        }
                        Box(Modifier.size(42.dp).background(WaslhaSurface, CircleShape), Alignment.Center) {
                            Icon(Icons.Default.Place, null, tint = WaslhaGreen)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Card(
                        Modifier.fillMaxWidth().clickable(onClick = onPickDestination),
                        colors = CardDefaults.cardColors(WaslhaSurface),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).background(WaslhaGreen.copy(alpha = .12f), CircleShape), Alignment.Center) {
                                Icon(Icons.Default.Search, null, tint = WaslhaGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) {
                                Text("الوجهة", fontSize = 11.sp, color = WaslhaMuted)
                                Text(
                                    if (destination.isBlank()) "ابحث عن مكان أو عنوان" else destination,
                                    fontSize = 15.sp,
                                    color = if (destination.isBlank()) WaslhaMuted else WaslhaInk,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(Icons.Default.ChevronLeft, null, tint = WaslhaMuted)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("اختار نوع التكسي", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = WaslhaInk)
                    Spacer(Modifier.height(9.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        types.forEachIndexed { index, type ->
                            CarTypeCard(type, index == selectedType) { selectedType = index }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = onRequest,
                        enabled = destination.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WaslhaGreen, disabledContainerColor = Color(0xFFB9C8C2))
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("اطلب سيارة الآن", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        QuickAction("موقع العمل", "📍")
                        QuickAction("المنزل", "🏠")
                        QuickAction("محفوظة", "⭐")
                    }
                }
            }
        }
    }
}

@Composable
private fun MapPreview() {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(78.dp).background(WaslhaGreen.copy(alpha = .14f), CircleShape), Alignment.Center) {
                Icon(Icons.Default.LocationOn, null, tint = WaslhaGreen, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.height(9.dp))
            Text("الخريطة جاهزة", fontWeight = FontWeight.ExtraBold, color = WaslhaInk)
            Text("سيتم عرض Mapbox وموقعك هنا", color = WaslhaMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CarTypeCard(type: Triple<String, String, String>, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.weight(1f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(if (selected) WaslhaGreen.copy(alpha = .11f) else WaslhaSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(type.third, fontSize = 24.sp)
            Text(type.first, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WaslhaInk)
            Text(type.second, fontSize = 10.sp, color = WaslhaMuted)
            if (selected) Text("مختار", color = WaslhaGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun QuickAction(title: String, icon: String) {
    Card(colors = CardDefaults.cardColors(WaslhaSurface), shape = RoundedCornerShape(13.dp), modifier = Modifier.weight(1f)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 17.sp)
            Text(title, fontSize = 10.sp, color = WaslhaMuted, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun DestinationDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("حدد وجهتك", fontWeight = FontWeight.Black, color = WaslhaInk) },
        text = {
            Column {
                Text("ابحث عن المكان الذي تريد الوصول إليه", color = WaslhaMuted, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = true,
                    placeholder = { Text("مثال: ساحة الأمويين") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = WaslhaGreen) },
                    shape = RoundedCornerShape(15.dp)
                )
                Spacer(Modifier.height(10.dp))
                Card(colors = CardDefaults.cardColors(WaslhaSurface), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = WaslhaGreen)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("اختيار من الخريطة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("ضع الدبوس على مكان الوصول", color = WaslhaMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (value.isNotBlank()) onConfirm(value) }) { Text("تأكيد", color = WaslhaGreen, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}

@Composable
private fun SearchCaptainScreen(destination: String, onCancel: () -> Unit, onFound: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(22.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(42.dp))
        Box(Modifier.size(112.dp).background(WaslhaGreen.copy(alpha = .10f), CircleShape), Alignment.Center) {
            Icon(Icons.Default.DirectionsCar, null, tint = WaslhaGreen, modifier = Modifier.size(55.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text("نبحث لك عن كابتن", fontSize = 27.sp, fontWeight = FontWeight.Black, color = WaslhaInk)
        Text("أقرب سيارة متاحة في منطقتك", color = WaslhaMuted)
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("تفاصيل الرحلة", fontWeight = FontWeight.Black, fontSize = 17.sp)
                Spacer(Modifier.height(13.dp))
                RouteLine("موقعك الحالي", "GPS", true)
                RouteLine("الوجهة", destination.ifBlank { "غير محددة" }, false)
                Divider(Modifier.padding(vertical = 8.dp), color = WaslhaSurface)
                Text("وقت الوصول المتوقع: 3–7 دقائق", color = WaslhaMuted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("جاري الاتصال بالكباتن القريبين…", color = WaslhaGreen, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Button(onClick = onFound, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = WaslhaGreen)) {
            Text("عرض رحلة تجريبية", fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(9.dp))
        OutlinedButton(onClick = onCancel, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("إلغاء الطلب") }
    }
}

@Composable
private fun RouteLine(label: String, value: String, start: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(12.dp).background(if (start) WaslhaGreen else WaslhaInk, CircleShape))
        Spacer(Modifier.width(11.dp))
        Column {
            Text(label, fontSize = 11.sp, color = WaslhaMuted)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WaslhaInk)
        }
    }
}

@Composable
private fun ActiveTripScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Column(Modifier.weight(1f)) {
                Text("رحلتك الحالية", fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("الكابتن في الطريق إليك", color = WaslhaGreen, fontSize = 11.sp)
            }
            Box(Modifier.size(42.dp).background(WaslhaGreen.copy(alpha = .10f), CircleShape), Alignment.Center) {
                Icon(Icons.Default.Phone, null, tint = WaslhaGreen)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().weight(1f).background(WaslhaMap, RoundedCornerShape(25.dp)), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DirectionsCar, null, tint = WaslhaGreen, modifier = Modifier.size(62.dp))
                Spacer(Modifier.height(8.dp))
                Text("التتبع المباشر", fontWeight = FontWeight.Black, fontSize = 18.sp)
                Text("سيظهر مسار الكابتن والرحلة على الخريطة", color = WaslhaMuted, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(54.dp).background(WaslhaSurface, CircleShape), Alignment.Center) { Text("👨🏻", fontSize = 26.sp) }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("محمد — كابتن وصلها", fontWeight = FontWeight.Black, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = WaslhaGreen, modifier = Modifier.size(15.dp))
                            Text(" 4.9 • Toyota Corolla • 1234", color = WaslhaMuted, fontSize = 11.sp)
                        }
                    }
                    Text("3 د", color = WaslhaGreen, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.height(13.dp))
                Button(onClick = onBack, Modifier.fillMaxWidth().height(51.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = WaslhaGreenDark)) {
                    Text("إنهاء العرض التجريبي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TripsScreen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp)) {
        Text("رحلاتي", fontSize = 29.sp, fontWeight = FontWeight.Black, color = WaslhaInk)
        Text("كل مشاويرك في مكان واحد", color = WaslhaMuted, fontSize = 13.sp)
        Spacer(Modifier.height(17.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxSize().padding(bottom = 80.dp)) {
            items(listOf("اليوم • المنزل → وسط المدينة", "أمس • الجامعة → الحي الجديد", "الأسبوع الماضي • المركز → المحطة")) { route ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(43.dp).background(WaslhaGreen.copy(alpha = .10f), CircleShape), Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = WaslhaGreen) }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(route, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("مكتملة • نقداً", color = WaslhaMuted, fontSize = 11.sp)
                        }
                        Text("4,000 ل.س", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(onSignedOut: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp)) {
        Text("حسابي", fontSize = 29.sp, fontWeight = FontWeight.Black, color = WaslhaInk)
        Text("إدارة حسابك وتفضيلاتك", color = WaslhaMuted, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(23.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).background(WaslhaGreen.copy(alpha = .10f), CircleShape), Alignment.Center) { Icon(Icons.Default.Person, null, tint = WaslhaGreen, modifier = Modifier.size(31.dp)) }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text("حساب وصلها", fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("رقم الهاتف والحساب", color = WaslhaMuted, fontSize = 12.sp)
                }
                Icon(Icons.Default.ChevronLeft, null, tint = WaslhaMuted)
            }
        }
        Spacer(Modifier.height(13.dp))
        listOf("بيانات الحساب" to "👤", "طرق الدفع" to "💳", "الإشعارات" to "🔔", "المساعدة والدعم" to "💬", "الخصوصية والشروط" to "🛡️").forEach { item ->
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp)) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.second, fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(item.first, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ChevronLeft, null, tint = WaslhaMuted, modifier = Modifier.size(19.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onSignedOut, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("تسجيل الخروج", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun BottomBar(tab: Int, onTab: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White, modifier = Modifier.navigationBarsPadding()) {
        NavigationBarItem(selected = tab == 0, onClick = { onTab(0) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("الرئيسية") })
        NavigationBarItem(selected = tab == 1, onClick = { onTab(1) }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("رحلاتي") })
        NavigationBarItem(selected = tab == 2, onClick = { onTab(2) }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("حسابي") })
    }
}
