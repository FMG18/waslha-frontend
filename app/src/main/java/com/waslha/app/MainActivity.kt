package com.waslha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

private val WaslhaGreen = Color(0xFF0B8F63)
private val WaslhaInk = Color(0xFF101817)
private val WaslhaMuted = Color(0xFF687571)
private val WaslhaSurface = Color(0xFFF5F8F7)

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
            if (!signedIn) PhoneAuthScreen(authViewModel) { signedIn = true }
            else PassengerApp(onSignedOut = { sessionStore.clear(); signedIn = false })
        }
    }
}

@Composable
private fun PassengerApp(onSignedOut: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var destination by remember { mutableStateOf("") }
    var showDestination by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var activeTrip by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        when {
            activeTrip -> ActiveTripScreen(onBack = { activeTrip = false })
            searching -> SearchingScreen(onCancel = { searching = false })
            tab == 0 -> HomeScreen(destination, { showDestination = true }, { searching = true })
            tab == 1 -> TripsScreen()
            else -> ProfileScreen(onSignedOut)
        }
        if (!searching && !activeTrip) BottomBar(tab, { tab = it })
    }

    if (showDestination) DestinationDialog(destination, { showDestination = false }) { destination = it; showDestination = false }
}

@Composable
private fun HomeScreen(destination: String, onPickDestination: () -> Unit, onRequest: () -> Unit) {
    var selectedType by remember { mutableIntStateOf(0) }
    val types = listOf("اقتصادي" to "3,500 ل.س", "مريح" to "5,000 ل.س", "عائلي" to "6,500 ل.س")

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth().background(Color(0xFFDDE8E4))) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
                        Text("وصلها", Modifier.padding(horizontal = 16.dp, vertical = 10.dp), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = WaslhaGreen)
                    }
                    IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "الإشعارات") }
                }
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White.copy(alpha = .94f)), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, tint = WaslhaGreen)
                        Spacer(Modifier.width(10.dp))
                        Column { Text("موقعك الحالي", color = WaslhaMuted, fontSize = 12.sp); Text("سيتم تحديد موقعك عبر GPS", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(84.dp).background(WaslhaGreen.copy(alpha = .13f), CircleShape), Alignment.Center) { Icon(Icons.Default.LocationOn, null, Modifier.size(42.dp), tint = WaslhaGreen) }
                Spacer(Modifier.height(10.dp))
                Text("الخريطة", fontWeight = FontWeight.Bold)
                Text("Mapbox جاهز للدمج النهائي", fontSize = 12.sp, color = WaslhaMuted)
            }
        }
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 92.dp)) {
                Text("إلى أين تريد الذهاب؟", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = WaslhaInk)
                Spacer(Modifier.height(12.dp))
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(17.dp), colors = CardDefaults.cardColors(WaslhaSurface), onClick = onPickDestination) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = WaslhaGreen)
                        Spacer(Modifier.width(10.dp))
                        Text(if (destination.isBlank()) "حدد وجهتك" else destination, color = if (destination.isBlank()) WaslhaMuted else WaslhaInk, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("نوع التكسي", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEachIndexed { index, pair ->
                        Card(Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(if (selectedType == index) WaslhaGreen.copy(alpha = .12f) else WaslhaSurface), onClick = { selectedType = index }) {
                            Column(Modifier.padding(11.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(listOf("🚕", "🚘", "🚙")[index], fontSize = 23.sp)
                                Text(pair.first, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(pair.second, fontSize = 11.sp, color = WaslhaMuted)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = onRequest, enabled = destination.isNotBlank(), modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = WaslhaGreen)) { Text("اطلب سيارة", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
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
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(100.dp).background(WaslhaGreen.copy(alpha = .12f), CircleShape), Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, Modifier.size(50.dp), tint = WaslhaGreen) }
        Spacer(Modifier.height(20.dp))
        Text("جاري البحث عن كابتن", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("نبحث عن أقرب سيارة متاحة", color = WaslhaMuted)
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) { StatusLine("تم تحديد موقع الانطلاق", true); StatusLine("جاري العثور على كابتن قريب", false); StatusLine("تأكيد الرحلة", false) }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(16.dp)) { Text("إلغاء الطلب") }
    }
}

@Composable
private fun StatusLine(text: String, done: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (done) Icons.Default.CheckCircle else Icons.Default.DirectionsCar, null, tint = if (done) WaslhaGreen else WaslhaMuted)
        Spacer(Modifier.width(10.dp)); Text(text, color = if (done) WaslhaInk else WaslhaMuted, fontWeight = if (done) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun ActiveTripScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") }; Text("رحلتك الحالية", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold) }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFDDE8E4), RoundedCornerShape(24.dp)), Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.LocationOn, null, Modifier.size(60.dp), tint = WaslhaGreen); Text("تتبع الرحلة مباشرة", fontWeight = FontWeight.Bold); Text("سيظهر مسار الكابتن والرحلة هنا", color = WaslhaMuted, fontSize = 12.sp) }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("الكابتن في الطريق إليك", fontSize = 19.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(50.dp).background(WaslhaSurface, CircleShape), Alignment.Center) { Text("👨🏻", fontSize = 24.sp) }; Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) { Text("كابتن وصلها", fontWeight = FontWeight.Bold); Text("Toyota Corolla • 1234", fontSize = 12.sp, color = WaslhaMuted) }; Text("4.9 ★", fontWeight = FontWeight.Bold, color = WaslhaGreen)
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = WaslhaGreen), shape = RoundedCornerShape(16.dp)) { Text("إنهاء العرض التجريبي") }
            }
        }
    }
}

@Composable
private fun TripsScreen() {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("رحلاتي", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Text("سجل رحلاتك مع وصلها", color = WaslhaMuted); Spacer(Modifier.height(18.dp))
        listOf("اليوم • المنزل → وسط المدينة", "أمس • الجامعة → الحي الجديد", "الأسبوع الماضي • المركز → المحطة").forEach { route ->
            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DirectionsCar, null, tint = WaslhaGreen); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(route, fontWeight = FontWeight.SemiBold); Text("مكتملة • نقداً", fontSize = 12.sp, color = WaslhaMuted) }; Text("4,000 ل.س", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(onSignedOut: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("حسابي", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold); Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(60.dp).background(WaslhaGreen.copy(alpha = .12f), CircleShape), Alignment.Center) { Icon(Icons.Default.Person, null, tint = WaslhaGreen) }; Spacer(Modifier.width(12.dp)); Column { Text("حساب وصلها", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("بيانات الحساب والجلسة", color = WaslhaMuted, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(16.dp))
        listOf("بيانات الحساب", "طرق الدفع", "الإشعارات", "المساعدة والدعم", "الخصوصية والشروط").forEach { item -> Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp)) { Text(item, Modifier.padding(16.dp), fontWeight = FontWeight.SemiBold) } }
        Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = onSignedOut, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("تسجيل الخروج") }
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
