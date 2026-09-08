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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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

private val Green = Color(0xFF078A60)
private val GreenDark = Color(0xFF056C4B)
private val Ink = Color(0xFF10201B)
private val Muted = Color(0xFF6D7B76)
private val SurfaceBg = Color(0xFFF3F7F5)
private val MapBg = Color(0xFFDCE8E2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WaslhaApp() }
    }
}

@Composable
private fun WaslhaApp() {
    var signedIn by remember { mutableStateOf(false) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = SurfaceBg) {
            if (signedIn) PassengerApp { signedIn = false } else LoginScreen { signedIn = true }
        }
    }
}

@Composable
private fun LoginScreen(onLogin: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(70.dp))
        Text("وصلها", fontSize = 40.sp, fontWeight = FontWeight.Black, color = Green)
        Text("رحلتك .. بأمان وراحة", color = Muted)
        Spacer(Modifier.height(45.dp))
        if (step == 0) {
            Text("مرحباً بك", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("سجل دخولك برقم الهاتف", color = Muted)
            Spacer(Modifier.height(22.dp))
            TextField(value = phone, onValueChange = { phone = it }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("رقم الهاتف") })
            Spacer(Modifier.height(16.dp))
            Button(onClick = { if (phone.isNotBlank()) step = 1 }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("إرسال رمز التحقق") }
        } else {
            Text("رمز التحقق", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("أدخل الرمز المرسل إلى $phone", color = Muted)
            Spacer(Modifier.height(22.dp))
            TextField(value = otp, onValueChange = { otp = it.take(6) }, singleLine = true, modifier = Modifier.fillMaxWidth(), placeholder = { Text("123456") })
            Spacer(Modifier.height(16.dp))
            Button(onClick = onLogin, enabled = otp.length >= 4, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("دخول إلى وصلها") }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { step = 0 }) { Text("تغيير الرقم") }
        }
    }
}

@Composable
private fun PassengerApp(onLogout: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var destination by remember { mutableStateOf("") }
    var destinationOpen by remember { mutableStateOf(false) }
    var booking by remember { mutableStateOf(false) }
    var active by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        when {
            active -> ActiveTrip(onBack = { active = false })
            booking -> SearchCaptain(destination, onCancel = { booking = false }, onFound = { booking = false; active = true })
            settingsOpen -> SettingsScreen(onBack = { settingsOpen = false })
            tab == 0 -> HomeScreen(destination, { destinationOpen = true }, { if (destination.isNotBlank()) booking = true })
            tab == 1 -> TripsScreen()
            else -> ProfileScreen(onLogout = onLogout, onSettings = { settingsOpen = true })
        }
        if (!booking && !active && !settingsOpen) BottomBar(tab) { tab = it }
    }
    if (destinationOpen) DestinationDialog(destination, { destinationOpen = false }) { destination = it; destinationOpen = false }
}

@Composable
private fun HomeScreen(destination: String, onDestination: () -> Unit, onRequest: () -> Unit) {
    var selected by remember { mutableIntStateOf(0) }
    val types = listOf(
        Triple("اقتصادي", "3,500 ل.س", "3-5 د"),
        Triple("مريح", "5,000 ل.س", "4-6 د"),
        Triple("عائلي", "6,500 ل.س", "5-8 د")
    )
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1f).background(MapBg)) {
            // Map placeholder kept intentionally lightweight until Mapbox credentials are wired.
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(88.dp).background(Green.copy(alpha = .14f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.LocationOn, null, tint = Green, modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("موقعك الحالي", fontWeight = FontWeight.Black, color = Ink)
                Text("حدد الوجهة وسنقترح أقرب سيارة", color = Muted, fontSize = 11.sp)
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
                Card(colors = CardDefaults.cardColors(Color.White), shape = CircleShape) {
                    IconButton(onClick = {}) { Icon(Icons.Default.NotificationsNone, "الإشعارات", tint = Ink) }
                }
            }
            Card(Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 14.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Place, null, tint = Green, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("موقع الانطلاق", fontSize = 10.sp, color = Muted)
                        Text("استخدام موقع GPS الحالي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
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
                            Text(if (destination.isBlank()) "ابحث عن مكان أو عنوان" else destination, fontWeight = FontWeight.Bold, color = Ink, maxLines = 1)
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
                Button(onClick = onRequest, enabled = destination.isNotBlank(), modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Default.DirectionsCar, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (destination.isBlank()) "حدد وجهتك أولاً" else "اطلب سيارة الآن", fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun CarType(name: String, price: String, eta: String, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(if (selected) Green.copy(alpha = .11f) else SurfaceBg), shape = RoundedCornerShape(15.dp), border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Green.copy(alpha = .35f)) else null) {
        Column(Modifier.fillMaxWidth().padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🚕", fontSize = 21.sp)
            Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Ink)
            Text(price, fontSize = 10.sp, color = Green, fontWeight = FontWeight.Bold)
            Text(eta, fontSize = 9.sp, color = Muted)
        }
    }
}

@Composable
private fun SearchCaptain(destination: String, onCancel: () -> Unit, onFound: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp).navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(40.dp)); Box(Modifier.size(110.dp).background(Green.copy(alpha = .10f), CircleShape), Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = Green, modifier = Modifier.size(54.dp)) }
        Spacer(Modifier.height(18.dp)); Text("نبحث لك عن كابتن", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Ink); Text("أقرب سيارة متاحة في منطقتك", color = Muted)
        Spacer(Modifier.height(24.dp)); Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Text("تفاصيل الرحلة", fontWeight = FontWeight.Black); Spacer(Modifier.height(10.dp)); Route("موقعك الحالي", "GPS", true); Route("الوجهة", destination, false); Divider(); Text("وقت الوصول المتوقع: 3–7 دقائق", fontSize = 12.sp, color = Muted) } }
        Spacer(Modifier.weight(1f)); Button(onClick = onFound, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text("عرض رحلة تجريبية") }; Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) { Text("إلغاء الطلب") }
    }
}

@Composable private fun Route(label: String, value: String, start: Boolean) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(11.dp).background(if (start) Green else Ink, CircleShape)); Spacer(Modifier.width(10.dp)); Column { Text(label, fontSize = 11.sp, color = Muted); Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink) } } }

@Composable private fun ActiveTrip(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") }; Column(Modifier.weight(1f)) { Text("رحلتك الحالية", fontSize = 21.sp, fontWeight = FontWeight.Black); Text("الكابتن في الطريق إليك", fontSize = 11.sp, color = Green) }; Icon(Icons.Default.Phone, null, tint = Green) }
        Spacer(Modifier.height(10.dp)); Box(Modifier.fillMaxWidth().weight(1f).background(MapBg, RoundedCornerShape(24.dp)), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.LocationOn, null, tint = Green, modifier = Modifier.size(58.dp)); Text("التتبع المباشر", fontWeight = FontWeight.Black); Text("سيتم وضع الخريطة الحقيقية هنا", fontSize = 11.sp, color = Muted) } }
        Spacer(Modifier.height(12.dp)); Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(52.dp).background(SurfaceBg, CircleShape), Alignment.Center) { Text("👨🏻", fontSize = 24.sp) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text("محمد — كابتن وصلها", fontWeight = FontWeight.Black); Text("4.9 • Toyota Corolla • 1234", fontSize = 11.sp, color = Muted) }; Text("3 د", color = Green, fontWeight = FontWeight.Black) }; Spacer(Modifier.height(12.dp)); Button(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) { Text("إنهاء العرض التجريبي") } } }
    }
}

@Composable private fun TripsScreen() { Column(Modifier.fillMaxSize().padding(18.dp)) { Text("رحلاتي", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink); Text("سجل مشاويرك", color = Muted); Spacer(Modifier.height(16.dp)); LazyColumnSafe() } }

@Composable private fun LazyColumnSafe() { val trips = listOf("المنزل → وسط المدينة", "الجامعة → الحي الجديد", "المركز → المحطة"); Column(verticalArrangement = Arrangement.spacedBy(9.dp)) { trips.forEach { route -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.DirectionsCar, null, tint = Green); Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(route, fontWeight = FontWeight.Bold); Text("مكتملة • نقداً", fontSize = 11.sp, color = Muted) }; Text("4,000 ل.س", fontWeight = FontWeight.Black, fontSize = 12.sp) } } } } }

@Composable private fun ProfileScreen(onLogout: () -> Unit, onSettings: () -> Unit) { Column(Modifier.fillMaxSize().padding(18.dp)) { Text("حسابي", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Ink); Spacer(Modifier.height(16.dp)); Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(58.dp).background(Green.copy(alpha = .10f), CircleShape), Alignment.Center) { Icon(Icons.Default.Person, null, tint = Green, modifier = Modifier.size(30.dp)) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("حساب وصلها", fontWeight = FontWeight.Black); Text("رقم الهاتف والبيانات", fontSize = 12.sp, color = Muted) } } }; Spacer(Modifier.height(12.dp)); MenuItem("طرق الدفع", "💳"); MenuItem("الإشعارات", "🔔"); MenuItem("المساعدة والدعم", "💬"); MenuItem("الأمان والخصوصية", "🛡️"); MenuItemClick("الإعدادات", "⚙️", onSettings); Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) { Text("تسجيل الخروج") } } }

@Composable private fun MenuItem(title: String, icon: String) { Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 18.sp); Spacer(Modifier.width(12.dp)); Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Icon(Icons.Default.ChevronLeft, null, tint = Muted) } } }
@Composable private fun MenuItemClick(title: String, icon: String, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable(onClick = onClick), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(16.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 18.sp); Spacer(Modifier.width(12.dp)); Text(title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Icon(Icons.Default.ChevronLeft, null, tint = Muted) } } }

@Composable private fun SettingsScreen(onBack: () -> Unit) { Column(Modifier.fillMaxSize().padding(18.dp).navigationBarsPadding()) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع") }; Text("الإعدادات", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Ink) }; Spacer(Modifier.height(14.dp)); listOf("الإشعارات والرحلات" to "🔔", "العروض والتنبيهات" to "🎁", "اللغة" to "🌐", "الوضع الداكن" to "🌙", "الخصوصية" to "🔒", "عن وصلها" to "ℹ️").forEach { row -> MenuItem(row.first, row.second) } } }

@Composable private fun DestinationDialog(initial: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) { var value by remember { mutableStateOf(initial) }; AlertDialog(onDismissRequest = onDismiss, title = { Text("حدد وجهتك", fontWeight = FontWeight.Black) }, text = { TextField(value, { value = it }, singleLine = true, placeholder = { Text("مثال: ساحة الأمويين") }) }, confirmButton = { TextButton(onClick = { if (value.isNotBlank()) onConfirm(value) }) { Text("تأكيد", color = Green, fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }) }

@Composable private fun BottomBar(tab: Int, onTab: (Int) -> Unit) { NavigationBar(containerColor = Color.White) { NavigationBarItem(selected = tab == 0, onClick = { onTab(0) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("الرئيسية") }); NavigationBarItem(selected = tab == 1, onClick = { onTab(1) }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("رحلاتي") }); NavigationBarItem(selected = tab == 2, onClick = { onTab(2) }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("حسابي") }) } }