package com.waslha.app

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccountGreen = Color(0xFF078A60)
private val AccountInk = Color(0xFF10201B)
private val AccountMuted = Color(0xFF6D7B76)
private val AccountBg = Color(0xFFF3F7F5)
private val AccountSoft = Color(0xFFE8F6F0)
private val AccountDanger = Color(0xFFB42318)

private fun openFeature(context: Context, screen: String) {
    context.startActivity(Intent(context, FeatureHostActivity::class.java).putExtra("screen", screen))
}

@Composable
fun AccountCenterScreen(session: SessionStore, onBack: () -> Unit) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }
    val displayName = session.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها"
    val contact = session.email ?: session.phone ?: "بيانات التواصل غير متاحة"

    Column(Modifier.fillMaxSize().background(AccountBg)) {
        AccountHeader("حسابي", onBack)
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(AccountGreen),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(62.dp).background(Color.White.copy(alpha = .18f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountCircle, null, tint = Color.White, modifier = Modifier.size(42.dp))
                        }
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text(contact, color = Color.White.copy(alpha = .86f), fontSize = 11.sp)
                            if (saved) Text("تم حفظ التغييرات", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { editing = true }) {
                            Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            item { AccountSectionTitle("إدارة الحساب") }
            item { AccountRow("تعديل البيانات", "الاسم ورقم الهاتف والبريد", Icons.Default.Edit) { editing = true } }
            item { AccountRow("الخصوصية والأمان", "الحماية وصلاحيات التطبيق", Icons.Default.Security) { openFeature(context, "security") } }
            item { AccountSectionTitle("الخدمات") }
            item { AccountRow("الأماكن المحفوظة", "المنزل والعمل والمفضلة", Icons.Default.LocationOn) { openFeature(context, "places") } }
            item { AccountRow("طرق الدفع", "طريقة الدفع المتاحة", Icons.Default.CreditCard) { openFeature(context, "payments") } }
            item { AccountRow("الإشعارات", "تنبيهات الرحلات والتحديثات", Icons.Default.Notifications) { openFeature(context, "notifications") } }
            item { AccountRow("المساعدة والدعم", "الأسئلة والحلول", Icons.Default.HelpOutline) { openFeature(context, "support") } }
            item { AccountRow("الإعدادات", "تفضيلات التطبيق", Icons.Default.Settings) { openFeature(context, "settings") } }
            item { AccountRow("عن وصلها", "معلومات التطبيق والنسخة", Icons.Default.Info) { openFeature(context, "about") } }
            item {
                Button(
                    onClick = { editing = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccountGreen)
                ) { Text("تعديل الحساب", fontWeight = FontWeight.Black) }
            }
        }
    }

    if (editing) {
        EditAccountDialog(
            session = session,
            onDismiss = { editing = false },
            onSaved = { saved = true; editing = false }
        )
    }
}

@Composable
fun SettingsCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waslha_preferences", Context.MODE_PRIVATE) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var location by remember { mutableStateOf(prefs.getBoolean("location", true)) }
    var open by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AccountBg)) {
        AccountHeader("الإعدادات", onBack)
        androidx.compose.foundation.lazy.LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { AccountSectionTitle("تفضيلات التطبيق") }
            item { SettingToggle("الإشعارات", "تنبيهات الرحلات والتحديثات", Icons.Default.Notifications, notifications) { notifications = it; prefs.edit().putBoolean("notifications", it).apply() } }
            item { SettingToggle("الموقع أثناء الحجز", "استخدام موقعك لتحديد الانطلاق", Icons.Default.LocationOn, location) { location = it; prefs.edit().putBoolean("location", it).apply() } }
            item { AccountSectionTitle("الخصوصية") }
            item { AccountRow("الخصوصية والأمان", "معلومات حماية الحساب والموقع", Icons.Default.Security) { openFeature(context, "security") } }
            item { AccountRow("حماية تسجيل الدخول", "إدارة جلسة الحساب", Icons.Default.Lock) { open = true } }
            item { AccountSectionTitle("الخدمات") }
            item { AccountRow("الأماكن المحفوظة", "إدارة الأماكن المفضلة", Icons.Default.LocationOn) { openFeature(context, "places") } }
            item { AccountRow("طرق الدفع", "اختيار طريقة الدفع", Icons.Default.CreditCard) { openFeature(context, "payments") } }
            item { AccountRow("الإشعارات", "عرض آخر التنبيهات", Icons.Default.Notifications) { openFeature(context, "notifications") } }
            item { AccountRow("الدعم", "الحصول على المساعدة", Icons.Default.HelpOutline) { openFeature(context, "support") } }
            item { AccountRow("عن وصلها", "معلومات التطبيق", Icons.Default.Info) { openFeature(context, "about") } }
        }
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            title = { Text("حماية تسجيل الدخول", fontWeight = FontWeight.Black) },
            text = { Text("جلسة الدخول محفوظة محليًا ولا يتم عرض بياناتها الحساسة داخل التطبيق.", color = AccountMuted) },
            confirmButton = { TextButton(onClick = { open = false }) { Text("حسنًا", color = AccountGreen, fontWeight = FontWeight.Bold) } }
        )
    }
}

@Composable
private fun AccountHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack) { Text("رجوع", color = AccountGreen, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.weight(1f))
        Text(title, color = AccountInk, fontSize = 25.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.size(10.dp))
    }
}

@Composable
private fun AccountSectionTitle(text: String) {
    Text(text, color = AccountMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
}

@Composable
private fun AccountRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(Color.White), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(1.dp)) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(AccountSoft, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = AccountGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = AccountInk, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = AccountMuted, fontSize = 10.sp)
            }
            Icon(Icons.Default.ChevronLeft, null, tint = AccountMuted)
        }
    }
}

@Composable
private fun SettingToggle(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(AccountSoft, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = AccountGreen) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = AccountInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subtitle, color = AccountMuted, fontSize = 10.sp)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun EditAccountDialog(session: SessionStore, onDismiss: () -> Unit, onSaved: () -> Unit) {
    var name by remember { mutableStateOf(session.name.orEmpty()) }
    var phone by remember { mutableStateOf(session.phone.orEmpty()) }
    var email by remember { mutableStateOf(session.email.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات الحساب", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الهاتف") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("البريد الإلكتروني") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                session.updateProfile(name.trim().ifBlank { null }, phone.trim().ifBlank { null }, email.trim().ifBlank { null })
                onSaved()
            }) { Text("حفظ", color = AccountGreen, fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = AccountMuted) } }
    )
}
