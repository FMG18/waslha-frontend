package com.waslha.app

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val APrimary = Color(0xFF087F5B)
private val APrimaryDark = Color(0xFF055C42)
private val AAccent = Color(0xFFB8E986)
private val AInk = Color(0xFF12201B)
private val AMuted = Color(0xFF6D7A75)
private val ABackground = Color(0xFFF7F9F8)
private val ASoft = Color(0xFFE7F6F0)
private val ASoft2 = Color(0xFFF0F5F2)
private val ALine = Color(0xFFDDE5E1)
private val ADanger = Color(0xFFB42318)

private fun syp(value: Long): String = "%,d".format(value)

@Composable
private fun AccountCardIcon(picture: String?, size: Int = 64, iconSize: Int = 38) {
    val bitmap = remember(picture) {
        runCatching {
            val clean = picture.orEmpty().substringAfter("base64,", picture.orEmpty())
            if (clean.isBlank()) null else BitmapFactory.decodeByteArray(Base64.decode(clean, Base64.DEFAULT), 0, Base64.decode(clean, Base64.DEFAULT).size)?.asImageBitmap()
        }.getOrNull()
    }
    Box(Modifier.size(size.dp).clip(CircleShape).background(AAccent), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap, "الصورة الشخصية", Modifier.fillMaxSize().clip(CircleShape))
        else Icon(Icons.Default.AccountCircle, null, tint = APrimaryDark, modifier = Modifier.size(iconSize.dp))
    }
}

@Composable
fun CustomerProfileV2(session: SessionStore, onPage: (CustomerPage) -> Unit, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { CustomerRepository() }
    var profile by remember { mutableStateOf<CustomerProfileDto?>(null) }
    var loading by remember { mutableStateOf(true) }

    fun load() = scope.launch {
        loading = true
        repo.profile().onSuccess {
            profile = it
            session.updateProfile(it.name, it.phone, it.email, it.picture, it.walletBalance)
        }
        loading = false
    }
    LaunchedEffect(Unit) { load() }

    val currentName = profile?.name?.takeIf { it.isNotBlank() } ?: session.name?.takeIf { it.isNotBlank() } ?: "المستخدم"
    val currentPhone = profile?.phone?.takeIf { it.isNotBlank() } ?: session.phone.orEmpty()
    val currentPicture = profile?.picture?.takeIf { it.isNotBlank() } ?: session.picture
    val balance = profile?.walletBalance ?: session.walletBalance

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 18.dp, bottom = 24.dp)
    ) {
        item {
            Card(
                Modifier.fillMaxWidth().clickable { onPage(CustomerPage.EditProfile) },
                colors = CardDefaults.cardColors(APrimary),
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    AccountCardIcon(currentPicture)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text(currentName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text(currentPhone.ifBlank { session.email ?: "بيانات الحساب" }, color = Color.White.copy(alpha = .76f), fontSize = 11.sp)
                        Text("رصيد المحفظة: ${syp(balance)} ل.س", color = Color.White.copy(alpha = .9f), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                    }
                    Icon(Icons.Default.ChevronLeft, "تعديل الملف الشخصي", tint = Color.White)
                }
            }
        }
        item { AccountActionCard("الأماكن المحفوظة", "المنزل والعمل", Icons.Default.LocationOn) { onPage(CustomerPage.SavedPlaces) } }
        item { AccountActionCard("طرق الدفع والمحفظة", "رصيدك الفعلي والدفع النقدي", Icons.Default.Wallet) { onPage(CustomerPage.Payments) } }
        item { AccountActionCard("الإشعارات", "تنبيهات الرحلات والتحديثات", Icons.Default.Refresh) { onPage(CustomerPage.Notifications) } }
        item { AccountActionCard("الإعدادات", "الخصوصية وحذف الحساب", Icons.Default.Shield) { onPage(CustomerPage.Settings) } }
        item { AccountActionCard("المساعدة والدعم", "تواصل مع فريق وصلها", Icons.Default.SupportAgent) { onPage(CustomerPage.Support) } }
        item { AccountActionCard("عن وصلها", "معلومات التطبيق", Icons.Default.Info) { onPage(CustomerPage.About) } }
        item {
            Card(
                Modifier.fillMaxWidth().clickable { onLogout() },
                colors = CardDefaults.cardColors(Color.White),
                border = BorderStroke(1.dp, Color(0xFFF1D7D4)),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFEEEB)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Logout, null, tint = ADanger) }
                    Spacer(Modifier.width(12.dp))
                    Text("تسجيل الخروج", color = ADanger, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (loading) item { CircularProgressIndicator(color = APrimary, modifier = Modifier.padding(8.dp)) }
    }
}

@Composable
private fun AccountActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, ALine),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(ASoft), contentAlignment = Alignment.Center) { Icon(icon, null, tint = APrimary, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, color = AInk, fontSize = 13.sp, fontWeight = FontWeight.Black); Text(subtitle, color = AMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp)) }
            Icon(Icons.Default.ChevronLeft, null, tint = AMuted, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CustomerEditProfileV2(session: SessionStore, onBack: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { CustomerRepository() }
    var first by remember { mutableStateOf(session.name.orEmpty().trim().split(" ").firstOrNull().orEmpty()) }
    var last by remember { mutableStateOf(session.name.orEmpty().trim().split(" ").drop(1).joinToString(" ")) }
    var phone by remember { mutableStateOf(session.phone.orEmpty()) }
    var picture by remember { mutableStateOf(session.picture.orEmpty()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val bytes = uriToBytes(androidx.compose.ui.platform.LocalContext.current, uri)
            require(bytes.size <= 500_000) { "اختر صورة بحجم أقل من 500 كيلوبايت" }
            picture = "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            message = null
        }.onFailure { message = it.message ?: "تعذر قراءة الصورة" }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = AInk) }
            Spacer(Modifier.weight(1f))
            Text("تعديل الملف الشخصي", color = AInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AccountCardIcon(picture, 92, 46)
                TextButton(onClick = { picker.launch("image/*") }) { Text("تغيير الصورة", color = APrimary, fontWeight = FontWeight.Bold) }
            }
        }
        OutlinedTextField(first, { first = it }, Modifier.fillMaxWidth(), label = { Text("الاسم الأول") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        OutlinedTextField(last, { last = it }, Modifier.fillMaxWidth(), label = { Text("اسم العائلة") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("رقم الهاتف") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        Text("البريد الإلكتروني: ${session.email ?: "غير متوفر"}", color = AMuted, fontSize = 10.sp)
        Button(
            onClick = {
                if (first.isBlank() || phone.isBlank()) { message = "الاسم الأول ورقم الهاتف مطلوبان"; return@Button }
                saving = true
                message = null
                scope.launch {
                    repo.updateProfile(CustomerProfileUpdateRequest((first.trim() + " " + last.trim()).trim(), phone.trim(), picture.ifBlank { null }))
                        .onSuccess {
                            session.updateProfile(it.name, it.phone, it.email, it.picture, it.walletBalance)
                            onSaved()
                        }
                        .onFailure { message = it.message ?: "تعذر حفظ البيانات" }
                    saving = false
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = APrimary)
        ) { if (saving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("حفظ التغييرات", fontWeight = FontWeight.Black) }
        message?.let { Text(it, color = if (it.startsWith("اختر")) ADanger else ADanger, fontSize = 11.sp) }
    }
}

private fun uriToBytes(context: Context, uri: android.net.Uri): ByteArray {
    return context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("تعذر فتح الصورة")
}

@Composable
fun CustomerSettingsV2(session: SessionStore, onBack: () -> Unit, onNotifications: () -> Unit, onDeleteSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { CustomerRepository() }
    var deleting by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = AInk) }
            Spacer(Modifier.weight(1f))
            Text("الإعدادات", color = AInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        AccountActionCard("الإشعارات", "إدارة تنبيهات الرحلات", Icons.Default.Refresh, onNotifications)
        AccountActionCard("الخصوصية والأمان", "بيانات الحساب وحذفه نهائياً", Icons.Default.Shield) { confirmDelete = true }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(ASoft), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, null, tint = APrimary, modifier = Modifier.size(27.dp))
                Spacer(Modifier.width(10.dp))
                Column { Text("حذف الحساب", color = AInk, fontWeight = FontWeight.Black); Text("يحذف الحساب وبياناته نهائياً بعد التأكيد.", color = AMuted, fontSize = 10.sp) }
            }
        }
        error?.let { Text(it, color = ADanger, fontSize = 11.sp) }
    }

    if (confirmDelete) AlertDialog(
        onDismissRequest = { if (!deleting) confirmDelete = false },
        title = { Text("حذف الحساب نهائياً", fontWeight = FontWeight.Black) },
        text = { Text("سيتم حذف حسابك وبياناتك من قاعدة البيانات، ولا يمكن التراجع عن العملية.", color = AMuted) },
        confirmButton = {
            TextButton(enabled = !deleting, onClick = {
                deleting = true
                error = null
                scope.launch {
                    repo.deleteAccount().onSuccess {
                        confirmDelete = false
                        session.clear()
                        onDeleteSuccess()
                    }.onFailure { error = it.message ?: "تعذر حذف الحساب" }
                    deleting = false
                }
            }) { Text("حذف الحساب", color = ADanger, fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(enabled = !deleting, onClick = { confirmDelete = false }) { Text("إلغاء") } }
    )
}

@Composable
fun CustomerPaymentsV2(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { CustomerRepository() }
    var wallet by remember { mutableStateOf<WalletDto?>(null) }
    var selected by remember { mutableStateOf("cash") }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var showTopUp by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf("") }

    fun refresh() = scope.launch {
        loading = true
        repo.wallet().onSuccess { wallet = it }.onFailure { message = it.message ?: "تعذر تحميل الرصيد" }
        loading = false
    }
    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = AInk) }; Spacer(Modifier.weight(1f)); Text("طرق الدفع والمحفظة", color = AInk, fontSize = 22.sp, fontWeight = FontWeight.Black) }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(APrimary), shape = RoundedCornerShape(23.dp), elevation = CardDefaults.cardElevation(7.dp)) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wallet, null, tint = Color.White, modifier = Modifier.size(31.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("رصيد المحفظة", color = Color.White.copy(alpha = .72f), fontSize = 10.sp); Text("${syp(wallet?.balance ?: 0L)} ل.س", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black) }; IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "تحديث", tint = Color.White) }
            }
        }
        Button(onClick = { showTopUp = true }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = APrimary)) { Text("شحن الرصيد", fontWeight = FontWeight.Black) }
        PaymentChoice("cash", "الدفع نقداً", "ادفع للكابتن بعد الوصول", selected == "cash") { selected = "cash" }
        PaymentChoice("wallet", "محفظة وصلها", "${syp(wallet?.balance ?: 0L)} ل.س", selected == "wallet") { if ((wallet?.balance ?: 0L) > 0) selected = "wallet" else message = "رصيد المحفظة غير كافٍ" }
        if (loading) CircularProgressIndicator(color = APrimary)
        message?.let { Text(it, color = ADanger, fontSize = 11.sp) }
    }

    if (showTopUp) AlertDialog(
        onDismissRequest = { showTopUp = false },
        title = { Text("شحن المحفظة", fontWeight = FontWeight.Black) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("أدخل المبلغ المطلوب شحنه بالليرة السورية.", color = AMuted); OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("المبلغ") }, singleLine = true) } },
        confirmButton = { TextButton(onClick = { message = if (amount.toLongOrNull()?.let { it > 0 } == true) "تم تسجيل طلب الشحن وسيحتاج تأكيد فريق الدعم." else "أدخل مبلغاً صالحاً"; if (amount.toLongOrNull()?.let { it > 0 } == true) showTopUp = false }) { Text("إرسال الطلب", color = APrimary, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = { showTopUp = false }) { Text("إلغاء") } }
    )
}

@Composable
private fun PaymentChoice(id: String, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(if (selected) ASoft else Color.White), border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) APrimary else ALine), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected, onClick = onClick); Spacer(Modifier.width(7.dp)); Column(Modifier.weight(1f)) { Text(title, color = AInk, fontWeight = FontWeight.Black, fontSize = 13.sp); Text(subtitle, color = AMuted, fontSize = 10.sp) } }
    }
}

@Composable
fun CustomerSavedPlacesV2(pickup: Coordinates, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repo = remember { CustomerRepository() }
    var places by remember { mutableStateOf<List<SavedPlaceDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var slot by remember { mutableStateOf<String?>(null) }
    var picked by remember { mutableStateOf<Coordinates?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() = scope.launch { loading = true; repo.savedPlaces().onSuccess { places = it }.onFailure { message = it.message ?: "تعذر تحميل الأماكن" }; loading = false }
    LaunchedEffect(Unit) { reload() }

    if (slot != null) {
        Box(Modifier.fillMaxSize()) {
            WaslhaRideMap(pickup = pickup, destination = picked, modifier = Modifier.fillMaxSize(), onDestinationPicked = { picked = it })
            Card(Modifier.align(Alignment.TopCenter).padding(14.dp).fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = { slot = null }) { Icon(Icons.Default.ArrowBack, "رجوع", tint = AInk) }; Column(Modifier.weight(1f)) { Text(if (slot == "home") "تحديد المنزل" else "تحديد العمل", color = AInk, fontWeight = FontWeight.Black); Text("حدد الموقع من الخريطة ثم احفظه", color = AMuted, fontSize = 10.sp) } }
            }
            if (picked != null) Button(onClick = {
                val target = picked ?: return@Button
                val type = slot ?: return@Button
                scope.launch {
                    repo.savePlace(type, SavedPlaceRequest(if (type == "home") "المنزل" else "العمل", target.lat, target.lng)).onSuccess { places = places.filterNot { it.type == type } + it; slot = null; picked = null; message = "تم حفظ ${if (type == "home") "المنزل" else "العمل"}" }.onFailure { message = it.message ?: "تعذر حفظ المكان" }
                }
            }, modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).navigationBarsPadding(), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = APrimary)) { Text("حفظ هذا الموقع", fontWeight = FontWeight.Black) }
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = AInk) }; Spacer(Modifier.weight(1f)); Text("الأماكن المحفوظة", color = AInk, fontSize = 22.sp, fontWeight = FontWeight.Black) }
        PlaceSlotCard("المنزل", places.firstOrNull { it.type == "home" }) { slot = "home" }
        PlaceSlotCard("العمل", places.firstOrNull { it.type == "work" }) { slot = "work" }
        if (loading) CircularProgressIndicator(color = APrimary)
        message?.let { Text(it, color = if (it.startsWith("تم")) APrimary else ADanger, fontSize = 11.sp) }
    }
}

@Composable
private fun PlaceSlotCard(title: String, place: SavedPlaceDto?, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, ALine), shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(3.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(45.dp).clip(CircleShape).background(ASoft), contentAlignment = Alignment.Center) { Icon(if (title == "المنزل") Icons.Default.Home else Icons.Default.LocationOn, null, tint = APrimary, modifier = Modifier.size(23.dp)) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, color = AInk, fontWeight = FontWeight.Black); Text(if (place == null) "إضافة موقع من الخريطة" else "${"%.5f".format(place.latitude)} • ${"%.5f".format(place.longitude)}", color = if (place == null) AMuted else APrimary, fontSize = 10.sp) }; Icon(Icons.Default.ChevronLeft, null, tint = AMuted)
        }
    }
}

@Composable
fun CustomerSupportV2(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "رجوع", tint = AInk) }; Spacer(Modifier.weight(1f)); Text("المساعدة والدعم", color = AInk, fontSize = 22.sp, fontWeight = FontWeight.Black) }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(ASoft), shape = RoundedCornerShape(21.dp), elevation = CardDefaults.cardElevation(3.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.SupportAgent, null, tint = APrimary, modifier = Modifier.size(31.dp)); Spacer(Modifier.width(10.dp)); Column { Text("تواصل مع فريق وصلها", color = AInk, fontWeight = FontWeight.Black); Text("أرسل مشكلتك وسيتم فتح محادثة دعم مرتبطة بحسابك.", color = AMuted, fontSize = 10.sp) } } }
        OutlinedTextField(subject, { subject = it }, Modifier.fillMaxWidth(), label = { Text("عنوان المشكلة") }, singleLine = true, shape = RoundedCornerShape(16.dp))
        OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("رسالتك") }, minLines = 5, shape = RoundedCornerShape(16.dp))
        Button(onClick = {
            if (subject.isBlank() || body.isBlank()) { message = "اكتب عنوان المشكلة ورسالتك أولاً"; return@Button }
            sending = true; message = null
            scope.launch {
                runCatching { ApiProvider.api.createSupportTicket(SupportTicketRequest(subject.trim(), body.trim())) }.onSuccess { subject = ""; body = ""; message = "تم فتح محادثة الدعم بنجاح" }.onFailure { message = it.message ?: "تعذر إرسال الرسالة" }
                sending = false
            }
        }, enabled = !sending, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = APrimary)) { if (sending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("بدء محادثة مع الدعم", fontWeight = FontWeight.Black) }
        message?.let { Text(it, color = if (it.startsWith("تم")) APrimary else ADanger, fontSize = 11.sp) }
    }
}
