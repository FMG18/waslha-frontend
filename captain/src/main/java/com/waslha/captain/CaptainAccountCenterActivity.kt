package com.waslha.captain

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import kotlinx.coroutines.launch

private val AGreen = Color(0xFF0B805E)
private val ADark = Color(0xFF075B43)
private val AMint = Color(0xFFE8F5F0)
private val ABg = Color(0xFFF4F7F6)
private val AInk = Color(0xFF14211C)
private val AMuted = Color(0xFF6E7D76)
private val AWhite = Color.White
private val ARed = Color(0xFFB42318)

class CaptainAccountCenterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize(), color = ABg) { AccountCenter(onBack = { finish() }) } } }
    }
}

@Composable
private fun AccountCenter(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var driver by remember { mutableStateOf<Driver?>(null) }
    var notifications by remember { mutableStateOf<List<CaptainNotification>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { CaptainApiProvider.api.me() }
            .onSuccess { response ->
                driver = response.data
                val id = response.data?.id.orEmpty()
                if (id.isNotBlank()) {
                    runCatching { CaptainApiProvider.api.notifications(id) }
                        .onSuccess { notifications = it.data.orEmpty() }
                        .onFailure { error = it.message ?: "تعذر تحميل الإشعارات" }
                }
            }
            .onFailure { error = it.message ?: "تعذر تحميل الحساب" }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BoxBack(onBack)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("الحساب والإعدادات", color = AInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("إدارة بياناتك وكل ما يخص حساب الكابتن", color = AMuted, fontSize = 10.sp)
                }
            }
        }
        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(27.dp), colors = CardDefaults.cardColors(AWhite)) {
                Row(Modifier.fillMaxWidth().padding(19.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(62.dp).background(AMint, CircleShape), contentAlignment = Alignment.Center) {
                        Text("و", color = AGreen, fontSize = 26.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(driver?.name?.ifBlank { "الكابتن" } ?: "الكابتن", color = AInk, fontSize = 19.sp, fontWeight = FontWeight.Black)
                        Text(driver?.phone ?: "", color = AMuted, fontSize = 10.sp)
                    }
                    Surface(color = if (driver?.available == true) AMint else Color(0xFFF1F2F1), shape = RoundedCornerShape(999.dp)) {
                        Text(if (driver?.available == true) "متصل" else "غير متصل", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = if (driver?.available == true) AGreen else AMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { AccountSectionTitle("الملف والمركبة") }
        item { AccountRow("المعلومات الشخصية", "الاسم ورقم الهاتف") {} }
        item { AccountRow("المركبة", listOfNotBlank(driver?.vehicle, driver?.plate).ifBlank { "بيانات المركبة غير مكتملة" }) {} }
        item { AccountRow("المستندات", "حالة المستندات والاعتماد") {} }
        item {
            AccountRow(
                "الإشعارات",
                if (notifications.isEmpty()) "لا توجد إشعارات جديدة" else "${notifications.count { !it.read }} غير مقروء"
            ) {
                context.startActivity(Intent(context, CaptainNotificationsActivity::class.java))
            }
        }
        item { AccountSectionTitle("المساعدة") }
        item { AccountRow("الدعم", "تواصل مع فريق وصلها عند وجود مشكلة") {} }
        item { AccountRow("الأسئلة الشائعة", "إجابات عن العمل والرحلات والأرباح") {} }
        item { AccountSectionTitle("الإعدادات") }
        item { AccountRow("اللغة", "العربية") {} }
        item {
            AccountRow("إشعارات الرحلات", "تنبيهات الرحلات والأخبار") {
                context.startActivity(Intent(context, CaptainNotificationsActivity::class.java))
            }
        }
        item { AccountRow("الخصوصية والأمان", "حماية الحساب والجلسة") {} }
        error?.let { message -> item { Text(message, color = ARed, fontSize = 10.sp) } }
        item {
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("العودة", color = AGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun BoxBack(onBack: () -> Unit) {
    Box(Modifier.size(42.dp).background(AWhite, CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) { Text("‹", color = AInk, fontSize = 27.sp) }
}

@Composable
private fun AccountSectionTitle(text: String) {
    Text(text, color = AInk, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 5.dp))
}

@Composable
private fun AccountRow(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(AWhite)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(AMint, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) { Text("•", color = AGreen, fontSize = 18.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = AInk, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = AMuted, fontSize = 9.sp)
            }
            Text("‹", color = AMuted, fontSize = 22.sp)
        }
    }
}

private fun listOfNotBlank(vehicle: String?, plate: String?): String = listOf(vehicle, plate).filter { !it.isNullOrBlank() }.joinToString(" • ")
