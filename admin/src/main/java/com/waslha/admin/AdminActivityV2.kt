package com.waslha.admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Green = Color(0xFF0B805E)
private val Dark = Color(0xFF075B43)
private val Bg = Color(0xFFF4F7F6)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color.White
private val Red = Color(0xFFB42318)
private val Amber = Color(0xFFB54708)

class AdminActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        setContent { AdminAppV2() }
    }
}

@Composable
private fun AdminAppV2() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { AdminSessionStore(context) }
    var signedIn by remember { mutableStateOf(session.signedIn) }
    MaterialTheme { Surface(Modifier.fillMaxSize(), color = Bg) { if (signedIn) AdminHomeV2(session) { session.clear(); signedIn = false } else AdminLoginV2 { session.save(it); signedIn = true } } }
}

@Composable
private fun AdminLoginV2(onSuccess: (Session) -> Unit) {
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var devCode by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    fun submit() {
        if (phone.filter(Char::isDigit).length < 8) { error = "أدخل رقم هاتف صحيح"; return }
        if (sent && code.length < 4) { error = "أدخل رمز التحقق"; return }
        loading = true; error = null
        scope.launch {
            if (!sent) {
                runCatching { AdminApiProvider.api.requestCode(CodeRequest(phone.trim())) }
                    .onSuccess { r -> if (r.success) { sent = true; devCode = r.data?.devCode } else error = r.message ?: "تعذر إرسال الرمز" }
                    .onFailure { error = it.message ?: "تعذر الاتصال بالخادم" }
            } else {
                runCatching { AdminApiProvider.api.verifyCode(VerifyRequest(phone.trim(), code.trim())) }
                    .onSuccess { r -> if (r.success && r.data != null && r.data.role.equals("admin", true)) onSuccess(r.data) else error = r.message ?: "الحساب ليس حساب إدارة" }
                    .onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            }
            loading = false
        }
    }
    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(White)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(68.dp).clip(RoundedCornerShape(20.dp)).background(Green), Alignment.Center) { Text("إ", color = White, fontSize = 34.sp, fontWeight = FontWeight.Black) }
                Text("وصلها إدارة", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("دخول لوحة التحكم", color = Muted, fontSize = 12.sp)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رقم الإدارة") })
                if (sent) { OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رمز التحقق") }); devCode?.let { Text("رمز الاختبار: $it", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                error?.let { Text(it, color = Red, fontSize = 11.sp) }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Green).clickable(enabled = !loading) { submit() }.padding(vertical = 15.dp), Alignment.Center) { if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = White) else Text(if (sent) "دخول" else "إرسال الرمز", color = White, fontWeight = FontWeight.Black) }
                if (sent) TextButton({ sent = false; code = ""; devCode = null }) { Text("تغيير الرقم", color = Green) }
            }
        }
    }
}

@Composable
private fun AdminHomeV2(session: AdminSessionStore, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var overview by remember { mutableStateOf<AdminOverview?>(null) }
    var trips by remember { mutableStateOf<List<AdminTripDto>>(emptyList()) }
    var drivers by remember { mutableStateOf<List<AdminDriverDto>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    fun refresh() {
        scope.launch {
            loading = true; error = null
            runCatching { AdminApiProvider.api.overview() }.onSuccess { if (it.success) overview = it.data else error = it.message }
            runCatching { AdminApiProvider.api.trips() }.onSuccess { if (it.success) trips = it.data.orEmpty() }
            runCatching { AdminApiProvider.api.drivers() }.onSuccess { if (it.success) drivers = it.data.orEmpty() }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh(); while (true) { delay(10000); refresh() } }
    Scaffold(bottomBar = { NavigationBar(containerColor = White) { NavigationBarItem(tab == 0, { tab = 0 }, icon = { Text("⌂") }, label = { Text("الرئيسية") }); NavigationBarItem(tab == 1, { tab = 1 }, icon = { Text("↺") }, label = { Text("الرحلات") }); NavigationBarItem(tab == 2, { tab = 2 }, icon = { Text("●") }, label = { Text("الكباتن") }) } }) { padding ->
        when (tab) { 0 -> AdminDashboard(Modifier.padding(padding), session.name, overview, loading, error); 1 -> AdminTrips(Modifier.padding(padding)); else -> AdminDrivers(Modifier.padding(padding), drivers) }
    }
}

@Composable private fun AdminDashboard(modifier: Modifier, name: String, overview: AdminOverview?, loading: Boolean, error: String?) {
    val t = overview?.totals
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("لوحة الإدارة", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Black); Text("مرحباً $name", color = Muted, fontSize = 11.sp) }
        item { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(25.dp), colors = CardDefaults.cardColors(Dark)) { Column(Modifier.padding(19.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("حالة النظام", color = White.copy(.7f), fontSize = 10.sp); Text(if (loading) "جاري التحديث..." else "النظام متصل", color = White, fontSize = 20.sp, fontWeight = FontWeight.Black); Text("البيانات من الخادم مباشرة", color = White.copy(.75f), fontSize = 10.sp) } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { AdminStat("الرحلات", "${t?.trips ?: 0}", Green, Modifier.weight(1f)); AdminStat("جارية", "${t?.activeTrips ?: 0}", Amber, Modifier.weight(1f)); AdminStat("كباتن متصلين", "${t?.onlineDrivers ?: 0}", Ink, Modifier.weight(1f)) } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { AdminStat("مكتملة", "${t?.completedTrips ?: 0}", Ink, Modifier.weight(1f)); AdminStat("الإيراد", "${t?.revenue ?: 0} ل.س", Green, Modifier.weight(1f)) } }
        error?.let { item { Text(it, color = Red, fontSize = 11.sp) } }
        item { Text("آخر الرحلات", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black) }
        items(overview?.latestTrips?.take(5).orEmpty(), key = { it.id }) { AdminTripRow(it) }
    }
}

@Composable private fun AdminTrips(modifier: Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var trips by remember { mutableStateOf<List<AdminTripDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { runCatching { AdminApiProvider.api.trips() }.onSuccess { trips = it.data.orEmpty() }; loading = false }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("إدارة الرحلات", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black); Text("اضغط على أي رحلة للتحكم بها", color = Muted, fontSize = 11.sp) }
        if (loading) item { CircularProgressIndicator() }
        items(trips, key = { it.id }) { t -> AdminTripRow(t) { context.startActivity(Intent(context, AdminTripDetailsActivity::class.java).putExtra(AdminTripDetailsActivity.EXTRA_TRIP_ID, t.id)) } }
    }
}

@Composable private fun AdminDrivers(modifier: Modifier, drivers: List<AdminDriverDto>) { LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("الكباتن", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black); Text("الحالة الحالية للكباتن", color = Muted, fontSize = 11.sp) }; items(drivers, key = { it.id }) { d -> Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Text("ك", color = Green, fontWeight = FontWeight.Black, fontSize = 20.sp); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(d.name.ifBlank { d.id }, color = Ink, fontWeight = FontWeight.Black); Text("${d.type} • ${d.rating}", color = Muted, fontSize = 9.sp) }; Text(if (d.available) "متصل" else "غير متصل", color = if (d.available) Green else Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } } } }
@Composable private fun AdminTripRow(t: AdminTripDto, onClick: (() -> Unit)? = null) { Card(Modifier.fillMaxWidth().clickable(enabled = onClick != null) { onClick?.invoke() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("رحلة #${t.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black); Text("زبون: ${t.customerId}", color = Muted, fontSize = 9.sp); Text("كابتن: ${t.driver?.name ?: "غير معين"}", color = Muted, fontSize = 9.sp) }; Column(horizontalAlignment = Alignment.End) { Text(t.status, color = when (t.status) { "in_progress" -> Green; "searching" -> Amber; else -> Muted }, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text("${t.estimatedFare} ${t.currency}", color = Ink, fontWeight = FontWeight.Black, fontSize = 12.sp) } } } }
@Composable private fun AdminStat(title: String, value: String, accent: Color, modifier: Modifier) { Surface(modifier, shape = RoundedCornerShape(18.dp), color = White) { Column(Modifier.padding(12.dp)) { Text(title, color = Muted, fontSize = 8.sp); Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.Black) } } }
