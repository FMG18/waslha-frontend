package com.waslha.captain

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Green = Color(0xFF0B805E)
private val Dark = Color(0xFF075B43)
private val Bg = Color(0xFFF4F7F6)
private val Ink = Color(0xFF14211C)
private val Muted = Color(0xFF6E7D76)
private val White = Color.White
private val Red = Color(0xFFB42318)

class CaptainActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        setContent { CaptainApp() }
    }
}

@Composable
private fun CaptainApp() {
    val context = LocalContext.current
    val session = remember { CaptainSession(context) }
    var signedIn by remember { mutableStateOf(session.isSignedIn) }
    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Bg) {
            if (signedIn) CaptainHome(session) { session.clear(); signedIn = false }
            else CaptainLogin { session.save(it); signedIn = true }
        }
    }
}

@Composable
private fun CaptainLogin(onSuccess: (VerifySessionResponse) -> Unit) {
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var devCode by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (phone.filter(Char::isDigit).length < 8) { error = "أدخل رقم هاتف صحيح"; return }
        if (sent && code.length < 4) { error = "أدخل رمز التحقق"; return }
        loading = true; error = null
        scope.launch {
            if (!sent) {
                runCatching { CaptainApiProvider.api.requestCode(OtpRequest(phone.trim())) }
                    .onSuccess { r -> if (r.success) { sent = true; devCode = r.data?.devCode } else error = r.message ?: "تعذر إرسال الرمز" }
                    .onFailure { error = it.message ?: "تعذر الاتصال بالخادم" }
            } else {
                runCatching { CaptainApiProvider.api.verifyCode(VerifyOtpRequest(phone.trim(), code.trim())) }
                    .onSuccess { r -> if (r.success && r.data != null && r.data.role.equals("driver", true)) onSuccess(r) else error = r.message ?: "الحساب غير مسجل ككابتن" }
                    .onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            }
            loading = false
        }
    }

    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(White)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(68.dp).clip(RoundedCornerShape(20.dp)).background(Green), Alignment.Center) { Text("و", color = White, fontSize = 36.sp, fontWeight = FontWeight.Black) }
                Text("وصلها كابتن", color = Ink, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("سجّل الدخول لإدارة رحلاتك", color = Muted, fontSize = 12.sp)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رقم الهاتف") })
                if (sent) {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رمز التحقق") })
                    devCode?.let { Text("رمز الاختبار: $it", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }
                error?.let { Text(it, color = Red, fontSize = 11.sp) }
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Green).clickable(!loading) { submit() }.padding(vertical = 15.dp), Alignment.Center) {
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), color = White, strokeWidth = 2.dp) else Text(if (sent) "دخول" else "إرسال الرمز", color = White, fontWeight = FontWeight.Black)
                }
                if (sent) TextButton({ sent = false; code = ""; devCode = null; error = null }) { Text("تغيير الرقم", color = Green) }
            }
        }
    }
}

@Composable
private fun CaptainHome(session: CaptainSession, onLogout: () -> Unit) {
    val scope = rememberCoroutineScope()
    var driver by remember { mutableStateOf<Driver?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var available by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        scope.launch {
            loading = true
            runCatching { CaptainApiProvider.api.me() }.onSuccess { driver = it.data }
            runCatching { CaptainApiProvider.api.trips() }.onSuccess { trips = it.data.orEmpty() }
            if (driver?.available == true) runCatching { CaptainApiProvider.api.availableTrips() }.onSuccess { available = it.data.orEmpty() }
            loading = false
        }
    }
    LaunchedEffect(Unit) { refresh() }

    Scaffold(bottomBar = {
        NavigationBar(containerColor = White) {
            CaptainTab(tab == 0, { tab = 0 }, "⌂", "الرئيسية")
            CaptainTab(tab == 1, { tab = 1 }, "↺", "رحلاتي")
            CaptainTab(tab == 2, { tab = 2 }, "○", "حسابي")
        }
    }) { padding ->
        when (tab) {
            0 -> Dashboard(Modifier.padding(padding), driver, trips, available, loading, message, refresh) { id ->
                scope.launch { loading = true; runCatching { CaptainApiProvider.api.acceptTrip(id) }.onSuccess { refresh() }.onFailure { message = it.message ?: "تعذر قبول الرحلة" }; loading = false }
            }
            1 -> CaptainTripsV2(Modifier.padding(padding), trips)
            else -> CaptainAccountV2(Modifier.padding(padding), driver, onLogout)
        }
    }
}

@Composable
private fun CaptainTab(selected: Boolean, onClick: () -> Unit, icon: String, label: String) {
    Column(Modifier.clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected) Green else Muted, fontSize = 19.sp)
        Text(label, color = if (selected) Green else Muted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Dashboard(modifier: Modifier, driver: Driver?, trips: List<Trip>, available: List<Trip>, loading: Boolean, message: String?, refresh: () -> Unit, onAccept: (String) -> Unit) {
    val completed = trips.count { it.status == "completed" }
    val income = trips.filter { it.status == "completed" }.sumOf { it.estimatedFare }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.fillMaxWidth(.68f)) { Text("لوحة الكابتن", color = Muted, fontSize = 11.sp); Text(driver?.name ?: "أهلاً كابتن", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Black) }; Text(if (driver?.available == true) "● متصل" else "○ غير متصل", color = if (driver?.available == true) Green else Muted, fontWeight = FontWeight.Bold) } }
        item { Card(Modifier.fillMaxWidth().clickable { refresh() }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(if (driver?.available == true) Dark else White)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.fillMaxWidth(.75f)) { Text("حالة استقبال الرحلات", color = if (driver?.available == true) White else Ink, fontWeight = FontWeight.Bold); Text(if (driver?.available == true) "أنت جاهز لاستقبال الطلبات" else "غير متصل حالياً", color = if (driver?.available == true) White.copy(.75f) else Muted, fontSize = 10.sp) }; Text("تحديث", color = if (driver?.available == true) White else Green, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } }
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Metric("الأرباح", "%,d ل.س".format(income), Modifier.fillMaxWidth(.31f)); Metric("مكتملة", completed.toString(), Modifier.fillMaxWidth(.48f)); Metric("متاحة", available.size.toString(), Modifier.fillMaxWidth()) } }
        message?.let { item { Text(it, color = Red, fontSize = 11.sp) } }
        item { Text("طلبات جديدة", color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Black) }
        if (available.isEmpty()) item { Text(if (loading) "جاري التحديث..." else "لا توجد طلبات جديدة حالياً", color = Muted, fontSize = 11.sp) }
        else items(available, key = { it.id }) { trip ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth(.7f)) { Text("طلب #${trip.id.takeLast(6)}", color = Ink, fontWeight = FontWeight.Black); Text("${trip.distanceKm} كم", color = Muted, fontSize = 10.sp) }; Text("${trip.estimatedFare} ${trip.currency}", color = Green, fontWeight = FontWeight.Black) }
                    Text("${trip.pickup.lat}, ${trip.pickup.lng} → ${trip.destination.lat}, ${trip.destination.lng}", color = Muted, fontSize = 9.sp)
                    Text("قبول الرحلة", Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Green).clickable { onAccept(trip.id) }.padding(vertical = 12.dp), color = White, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun Metric(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = White) { Column(Modifier.padding(12.dp)) { Text(title, color = Muted, fontSize = 9.sp); Text(value, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black) } }
}
