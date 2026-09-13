package com.waslha.app

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

private val LPrimary = Color(0xFF087F5B)
private val LPrimaryDark = Color(0xFF055C42)
private val LAccent = Color(0xFFB8E986)
private val LInk = Color(0xFF12201B)
private val LMuted = Color(0xFF6D7A75)
private val LBackground = Color(0xFFF7F9F8)
private val LSoft = Color(0xFFE7F6F0)
private val LLine = Color(0xFFDDE5E1)
private val LWhite = Color.White
private val LDanger = Color(0xFFB42318)

@Composable
fun CustomerTrips(trips: List<Trip>, loading: Boolean, error: String?, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("رحلاتي", color = LInk, fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("رحلاتك السابقة والحالية", color = LMuted, fontSize = 12.sp)
            }
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "تحديث", tint = LPrimary) }
        }
        Spacer(Modifier.height(10.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LPrimary) }
            trips.isEmpty() -> EmptyState(Icons.Default.History, "لا توجد رحلات بعد", "بعد أول حجز ستظهر تفاصيل رحلتك هنا")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
                items(trips, key = { it.id }) { trip ->
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(LWhite), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, LLine)) {
                        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(trip.statusLabel(), color = LPrimary, fontWeight = FontWeight.Black)
                                Text("${trip.estimatedFare} ${trip.currency}", color = LInk, fontWeight = FontWeight.Black)
                            }
                            Text("${trip.distanceKm} كم • ${trip.durationMin} دقيقة", color = LMuted, fontSize = 11.sp)
                            Text("${trip.vehicleType} • ${trip.paymentMethod}", color = LMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
        if (!error.isNullOrBlank()) Text(error, color = LDanger, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(78.dp).clip(CircleShape).background(LSoft), contentAlignment = Alignment.Center) { Icon(icon, null, tint = LPrimary, modifier = Modifier.size(39.dp)) }
        Spacer(Modifier.height(13.dp))
        Text(title, color = LInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(subtitle, color = LMuted, fontSize = 11.sp)
    }
}

@Composable
fun CustomerProfile(sessionStore: SessionStore, onPage: (CustomerPage) -> Unit, onLogout: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 20.dp)) {
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(LPrimary), shape = RoundedCornerShape(26.dp), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(60.dp).clip(CircleShape).background(LAccent), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountCircle, null, tint = LPrimaryDark, modifier = Modifier.size(39.dp)) }
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(sessionStore.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", color = LWhite, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(sessionStore.email ?: sessionStore.phone ?: "بيانات الحساب", color = LWhite.copy(alpha = .74f), fontSize = 11.sp)
                    }
                    IconButton(onClick = { onPage(CustomerPage.EditProfile) }) { Icon(Icons.Default.ChevronLeft, "تعديل", tint = LWhite) }
                }
            }
        }
        item { ProfileAction("الأماكن المحفوظة", "المنزل والعمل والمفضلة", Icons.Default.LocationOn) { onPage(CustomerPage.SavedPlaces) } }
        item { ProfileAction("طرق الدفع", "الدفع نقداً أو من المحفظة", Icons.Default.CreditCard) { onPage(CustomerPage.Payments) } }
        item { ProfileAction("الإشعارات", "تنبيهات الرحلات والتحديثات", Icons.Default.Notifications) { onPage(CustomerPage.Notifications) } }
        item { ProfileAction("الإعدادات", "تفضيلات التطبيق والخصوصية", Icons.Default.Settings) { onPage(CustomerPage.Settings) } }
        item { ProfileAction("المساعدة والدعم", "الأسئلة والحلول", Icons.Default.HelpOutline) { onPage(CustomerPage.Support) } }
        item { ProfileAction("عن وصلها", "معلومات التطبيق", Icons.Default.Info) { onPage(CustomerPage.About) } }
        item { OutlinedButton(onClick = onLogout, Modifier.fillMaxWidth().height(51.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = LDanger)) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(7.dp)); Text("تسجيل الخروج", fontWeight = FontWeight.Bold) } }
    }
}

@Composable
fun ProfileAction(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(LWhite), shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, LLine), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(LSoft), contentAlignment = Alignment.Center) { Icon(icon, null, tint = LPrimary, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = LInk, fontWeight = FontWeight.Bold); Text(subtitle, color = LMuted, fontSize = 10.sp) }
            Icon(Icons.Default.ChevronLeft, null, tint = LMuted)
        }
    }
}

@Composable
fun CustomerSubPage(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = LInk) }
            Spacer(Modifier.width(4.dp)); Text(title, color = LInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(10.dp)); content()
    }
}

@Composable
fun NotificationPage() { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { SettingToggle("تحديثات الرحلة", "تنبيه عند تغيّر حالة الحجز", true); SettingToggle("العروض", "التنبيهات المتعلقة بالخدمة", true) } }

@Composable
fun SettingToggle(title: String, subtitle: String, initial: Boolean) {
    var enabled by remember { mutableStateOf(initial) }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(LWhite), shape = RoundedCornerShape(19.dp), border = BorderStroke(1.dp, LLine)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(LSoft), contentAlignment = Alignment.Center) { Icon(Icons.Default.Notifications, null, tint = LPrimary, modifier = Modifier.size(21.dp)) }
            Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = LInk); Text(subtitle, color = LMuted, fontSize = 10.sp) }; Switch(checked = enabled, onCheckedChange = { enabled = it })
        }
    }
}

@Composable
fun PaymentPage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAction("الدفع نقداً", "الدفع للكابتن بعد انتهاء الرحلة", Icons.Default.CreditCard) { }
        ProfileAction("محفظة وصلها", "استخدم رصيدك للدفع عند توفره", Icons.Default.CreditCard) { }
    }
}

@Composable
fun SavedPlacePage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAction("المنزل", "حفظ الموقع المفضل للمنزل", Icons.Default.Home) { }
        ProfileAction("العمل", "حفظ الموقع المفضل للعمل", Icons.Default.LocationOn) { }
        Text("يمكن تحديد المكان لاحقاً من الخريطة.", color = LMuted, fontSize = 11.sp)
    }
}

@Composable
fun SupportPage() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileAction("كيف أطلب تكسي؟", "اختر موقعك والوجهة ثم اطلب الرحلة", Icons.Default.HelpOutline) { }
        ProfileAction("مشكلة في الرحلة", "تواصل مع الدعم عند وجود مشكلة", Icons.Default.HelpOutline) { }
        ProfileAction("فتح تذكرة دعم", "أرسل تفاصيل المشكلة لفريق وصلها", Icons.Default.HelpOutline) { }
    }
}

@Composable
fun AboutPage() {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(LPrimary), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(LAccent), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = LPrimaryDark, modifier = Modifier.size(27.dp)) }
            Text("وصلها", color = LWhite, fontSize = 27.sp, fontWeight = FontWeight.Black)
            Text("تطبيق تكسي لحجز المشاوير داخل سوريا.", color = LWhite.copy(alpha = .86f), fontSize = 13.sp)
            Text("العملة: الليرة السورية (ل.س)", color = LWhite.copy(alpha = .68f), fontSize = 10.sp)
        }
    }
}

@Composable
fun EditProfilePage(sessionStore: SessionStore, onSaved: () -> Unit) {
    var name by remember { mutableStateOf(sessionStore.name.orEmpty()) }
    var phone by remember { mutableStateOf(sessionStore.phone.orEmpty()) }
    var email by remember { mutableStateOf(sessionStore.email.orEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("الاسم") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), label = { Text("البريد الإلكتروني") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        Button(onClick = { sessionStore.updateProfile(name.trim(), phone.trim(), email.trim()); onSaved() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = LPrimary)) { Text("حفظ التغييرات", fontWeight = FontWeight.Black) }
    }
}

@Composable
fun MapDestinationPage(pickup: Coordinates, selected: Coordinates?, onBack: () -> Unit, onPicked: (Coordinates) -> Unit) {
    Box(Modifier.fillMaxSize().background(LWhite)) {
        WaslhaRideMap(pickup = pickup, destination = selected, modifier = Modifier.fillMaxSize(), onDestinationPicked = onPicked)
        Card(Modifier.align(Alignment.TopStart).padding(14.dp), colors = CardDefaults.cardColors(LWhite), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(4.dp)) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = LInk) } }
        Card(Modifier.align(Alignment.BottomCenter).padding(16.dp), colors = CardDefaults.cardColors(LWhite.copy(alpha = .98f)), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(6.dp)) {
            Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(LPrimary)); Spacer(Modifier.height(7.dp)); Text("حدد وجهتك على الخريطة", color = LInk, fontWeight = FontWeight.Black, fontSize = 14.sp); Text("حرّك الخريطة حتى يكون المؤشر على المكان المطلوب", color = LMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun ActiveTripCardOverlay(trip: Trip, onOpen: () -> Unit, onCancel: () -> Unit) {
    val statusColor = when (trip.status) { "completed" -> LPrimary; "cancelled" -> LDanger; else -> Color(0xFF6F4DBA) }
    val hint = when (trip.status) { "searching" -> "نبحث عن أقرب كابتن"; "driver_assigned" -> "تم العثور على كابتن"; "arriving" -> "الكابتن متجه إليك"; "completed" -> "اكتملت الرحلة"; "cancelled" -> "تم إلغاء الرحلة"; else -> "تحديث مباشر لحالة الرحلة" }
    Card(Modifier.fillMaxWidth().padding(12.dp), colors = CardDefaults.cardColors(LWhite), shape = RoundedCornerShape(23.dp), elevation = CardDefaults.cardElevation(10.dp), border = BorderStroke(1.dp, LLine)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(LSoft), contentAlignment = Alignment.Center) { Icon(Icons.Default.DirectionsCar, null, tint = LPrimary, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(trip.statusLabel(), color = statusColor, fontWeight = FontWeight.Black, fontSize = 14.sp); Text(hint, color = LMuted, fontSize = 10.sp) }; Text("${trip.estimatedFare} ${trip.currency}", color = LInk, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Spacer(Modifier.height(10.dp)); Row(Modifier.fillMaxWidth().background(Color(0xFFF0F5F2), RoundedCornerShape(14.dp)).padding(horizontal = 11.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("${trip.distanceKm} كم", color = LMuted, fontSize = 10.sp); Text("${trip.durationMin} دقيقة", color = LMuted, fontSize = 10.sp); Text(trip.vehicleType, color = LPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(9.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onOpen) { Text("تفاصيل الرحلة", color = LPrimary, fontWeight = FontWeight.Bold) }; if (trip.status == "searching" || trip.status == "driver_assigned" || trip.status == "arriving") TextButton(onClick = onCancel) { Text("إلغاء", color = LDanger, fontWeight = FontWeight.Bold) } }
        }
    }
}
