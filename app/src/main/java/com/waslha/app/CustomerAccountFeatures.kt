package com.waslha.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val CustomerGreen = Color(0xFF087F5B)
private val CustomerInk = Color(0xFF12201B)
private val CustomerMuted = Color(0xFF6D7A75)
private val CustomerSoft = Color(0xFFE7F6F0)
private val CustomerLine = Color(0xFFDDE5E1)
private val CustomerDanger = Color(0xFFB42318)

private fun formatSyp(value: Long): String = "%,d".format(value)

@Composable
fun CustomerPaymentsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val repository = remember { CustomerRepository() }
    var wallet by remember { mutableStateOf<WalletDto?>(null) }
    var selected by remember { mutableStateOf("cash") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    fun refresh() { scope.launch { loading = true; error = null; repository.wallet().onSuccess { wallet = it }.onFailure { error = it.message ?: "تعذر تحميل رصيد المحفظة" }; loading = false } }
    LaunchedEffect(Unit) { refresh() }
    CustomerFeatureScaffold("طرق الدفع", onBack) {
        Text("اختر طريقة الدفع", color = CustomerInk, fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("يمكنك تغييرها قبل كل رحلة.", color = CustomerMuted, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        wallet?.let { current -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CustomerGreen), shape = RoundedCornerShape(22.dp)) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Wallet, null, tint = Color.White, modifier = Modifier.size(28.dp)); Spacer(Modifier.size(12.dp)); Column(Modifier.weight(1f)) { Text("رصيد المحفظة", color = Color.White.copy(alpha = .75f), fontSize = 10.sp); Text("${formatSyp(current.balance)} ل.س", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black) }; TextButton(onClick = ::refresh) { Icon(Icons.Default.Refresh, null, tint = Color.White) } } } }
        Spacer(Modifier.height(12.dp))
        PaymentMethodCard("cash", "الدفع نقداً", "ادفع للكابتن بعد الوصول", selected == "cash", true) { selected = "cash" }
        Spacer(Modifier.height(8.dp))
        PaymentMethodCard("wallet", "رصيد المحفظة", "${wallet?.let { formatSyp(it.balance) } ?: "—"} ل.س", selected == "wallet", (wallet?.balance ?: 0L) > 0) { if ((wallet?.balance ?: 0L) > 0) selected = "wallet" else error = "رصيد المحفظة غير كافٍ" }
        error?.let { Text(it, color = CustomerDanger, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
private fun PaymentMethodCard(id: String, title: String, subtitle: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick), colors = CardDefaults.cardColors(if (selected) CustomerSoft else Color.White), border = BorderStroke(1.dp, if (selected) CustomerGreen else CustomerLine), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(selected, { if (enabled) onClick() }, enabled); Column(Modifier.weight(1f)) { Text(title, color = CustomerInk, fontWeight = FontWeight.Black, fontSize = 13.sp); Text(subtitle, color = if (enabled) CustomerMuted else CustomerDanger, fontSize = 10.sp) } }
    }
}

@Composable
fun CustomerSavedPlacesScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope(); val repository = remember { CustomerRepository() }
    var places by remember { mutableStateOf<List<SavedPlaceDto>>(emptyList()) }; var loading by remember { mutableStateOf(true) }; var message by remember { mutableStateOf<String?>(null) }
    fun reload() { scope.launch { loading = true; message = null; repository.savedPlaces().onSuccess { places = it }.onFailure { message = it.message ?: "تعذر تحميل الأماكن المحفوظة" }; loading = false } }
    LaunchedEffect(Unit) { reload() }
    CustomerFeatureScaffold("الأماكن المحفوظة", onBack) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("منزلك وعملك", color = CustomerInk, fontSize = 22.sp, fontWeight = FontWeight.Black); Text("محفوظة في حسابك.", color = CustomerMuted, fontSize = 10.sp) }; TextButton(onClick = ::reload) { Icon(Icons.Default.Refresh, null, tint = CustomerGreen) } }
        Spacer(Modifier.height(10.dp))
        if (loading) CircularProgressIndicator()
        if (!loading && places.isEmpty()) Text("لا توجد أماكن محفوظة حالياً.", color = CustomerMuted, fontSize = 11.sp)
        places.forEach { place -> SavedPlaceServerCard(place) { scope.launch { repository.deletePlace(place.id).onSuccess { reload() }.onFailure { message = it.message ?: "تعذر حذف المكان" } } } }
        message?.let { Text(it, color = if (it.startsWith("تم")) CustomerGreen else CustomerDanger, fontSize = 10.sp) }
    }
}

@Composable
private fun SavedPlaceServerCard(place: SavedPlaceDto, onDelete: () -> Unit) {
    val title = when (place.type) { "home" -> "المنزل"; "work" -> "العمل"; else -> place.name }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, CustomerLine), elevation = CardDefaults.cardElevation(0.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (place.type == "home") Icons.Default.Home else Icons.Default.LocationOn, null, tint = CustomerGreen, modifier = Modifier.size(23.dp)); Spacer(Modifier.size(10.dp)); Column(Modifier.weight(1f)) { Text(title, color = CustomerInk, fontSize = 13.sp, fontWeight = FontWeight.Black); Text(place.name, color = CustomerMuted, fontSize = 10.sp); Text("${"%.5f".format(place.latitude)} • ${"%.5f".format(place.longitude)}", color = CustomerMuted, fontSize = 8.sp) }; TextButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف", tint = CustomerDanger) } }
    }
}

@Composable
fun CustomerSupportScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope(); val api = ApiProvider.api
    var subject by remember { mutableStateOf("") }; var body by remember { mutableStateOf("") }; var loading by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    CustomerFeatureScaffold("المساعدة والدعم", onBack) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CustomerSoft), shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.SupportAgent, null, tint = CustomerGreen, modifier = Modifier.size(29.dp)); Spacer(Modifier.size(10.dp)); Column { Text("تحتاج مساعدة؟", color = CustomerInk, fontWeight = FontWeight.Black); Text("افتح تذكرة دعم مباشرة.", color = CustomerMuted, fontSize = 10.sp) } } }
        Spacer(Modifier.height(12.dp)); OutlinedTextField(subject, { subject = it }, Modifier.fillMaxWidth(), label = { Text("عنوان المشكلة") }, singleLine = true); OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("اكتب رسالتك") }, minLines = 5)
        Button(onClick = { scope.launch { if (subject.isBlank() || body.isBlank()) { message = "اكتب العنوان والرسالة أولاً"; return@launch }; loading = true; message = null; runCatching { api.createSupportTicket(SupportTicketRequest(subject.trim(), body.trim())) }.onSuccess { subject = ""; body = ""; message = "تم فتح تذكرة الدعم بنجاح" }.onFailure { message = it.message ?: "تعذر إرسال التذكرة" }; loading = false } }, enabled = !loading, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(17.dp), colors = ButtonDefaults.buttonColors(containerColor = CustomerGreen)) { if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("فتح تذكرة دعم", fontWeight = FontWeight.Black) }
        message?.let { Text(it, color = if (it.startsWith("تم")) CustomerGreen else CustomerDanger, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
    }
}

@Composable
fun CustomerSecurityScreen(session: SessionStore, onBack: () -> Unit, onDeleted: () -> Unit) {
    val scope = rememberCoroutineScope(); val repository = remember { CustomerRepository() }; var deleting by remember { mutableStateOf(false) }; var showConfirm by remember { mutableStateOf(false) }; var error by remember { mutableStateOf<String?>(null) }
    CustomerFeatureScaffold("الخصوصية والأمان", onBack) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(CustomerSoft), shape = RoundedCornerShape(20.dp)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Shield, null, tint = CustomerGreen, modifier = Modifier.size(28.dp)); Spacer(Modifier.size(10.dp)); Column { Text("حسابك محمي", color = CustomerInk, fontWeight = FontWeight.Black); Text("لا يمكن الحذف أثناء رحلة نشطة.", color = CustomerMuted, fontSize = 10.sp) } } }
        Spacer(Modifier.height(14.dp)); Text("معرّف الحساب: ${session.userId ?: "غير متاح"}", color = CustomerMuted, fontSize = 10.sp); Text("الهاتف: ${session.phone ?: "غير متاح"}", color = CustomerMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp)); Spacer(Modifier.height(18.dp))
        Button(onClick = { showConfirm = true }, enabled = !deleting, Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = CustomerDanger)) { if (deleting) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else { Icon(Icons.Default.Delete, null); Spacer(Modifier.size(7.dp)); Text("حذف الحساب نهائياً", fontWeight = FontWeight.Black) } }
        error?.let { Text(it, color = CustomerDanger, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
    }
    if (showConfirm) AlertDialog(onDismissRequest = { if (!deleting) showConfirm = false }, title = { Text("حذف الحساب نهائياً", fontWeight = FontWeight.Black) }, text = { Text("سيتم حذف حسابك وبيانات الإشعارات والجهاز. لا يمكن التراجع.", color = CustomerMuted) }, confirmButton = { TextButton(enabled = !deleting, onClick = { deleting = true; error = null; scope.launch { repository.deleteAccount().onSuccess { showConfirm = false; session.clear(); onDeleted() }.onFailure { error = it.message ?: "تعذر حذف الحساب" }; deleting = false } }) { Text("حذف نهائياً", color = CustomerDanger, fontWeight = FontWeight.Black) } }, dismissButton = { TextButton(enabled = !deleting, onClick = { showConfirm = false }) { Text("إلغاء") } })
}

@Composable
private fun CustomerFeatureScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = onBack) { Text("رجوع", color = CustomerGreen, fontWeight = FontWeight.Bold) }; Spacer(Modifier.weight(1f)); Text(title, color = CustomerInk, fontSize = 24.sp, fontWeight = FontWeight.Black) }
        Spacer(Modifier.height(8.dp)); content(); Spacer(Modifier.height(18.dp))
    }
}
