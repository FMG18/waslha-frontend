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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
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
import androidx.compose.runtime.saveable.rememberSaveable
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
    var editing by rememberSaveable { mutableStateOf(false) }
    var saved by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    val displayName = session.name?.trim().takeUnless { it.isNullOrBlank() } ?: "مستخدم وصلها"
    val contact = session.email?.trim().takeUnless { it.isNullOrBlank() }
        ?: session.phone?.trim().takeUnless { it.isNullOrBlank() }
        ?: "أكمل بيانات حسابك"
    val initial = displayName.firstOrNull()?.toString() ?: "و"

    Column(Modifier.fillMaxSize().background(AccountBg)) {
        AccountHeader("حسابي", onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccountGreen),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(64.dp).background(Color.White.copy(alpha = .18f), androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(initial, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.size(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text(contact, color = Color.White.copy(alpha = .88f), fontSize = 11.sp)
                            if (saved) {
                                Text("تم حفظ التغييرات", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(onClick = { editing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            item { AccountSectionTitle("الحساب والأمان") }
            item { AccountRow("تعديل البيانات", "الاسم ورقم الهاتف والبريد", Icons.Default.Edit) { editing = true } }
            item { AccountRow("الخصوصية والأمان", "الحماية وصلاحيات التطبيق", Icons.Default.Security) { openFeature(context, "security") } }
            item { AccountRow("حماية تسجيل الدخول", "معلومات الجلسة والحساب", Icons.Default.Lock) { openFeature(context, "security") } }
            item { AccountSectionTitle("الخدمات") }
            item { AccountRow("الأماكن المحفوظة", "المنزل والعمل والمواقع المفضلة", Icons.Default.LocationOn) { openFeature(context, "places") } }
            item { AccountRow("طرق الدفع", "إدارة طريقة الدفع", Icons.Default.CreditCard) { openFeature(context, "payments") } }
            item { AccountRow("الإشعارات", "تنبيهات الرحلات والتحديثات", Icons.Default.Notifications) { openFeature(context, "notifications") } }
            item { AccountRow("المساعدة والدعم", "الأسئلة والمساعدة", Icons.Default.HelpOutline) { openFeature(context, "support") } }
            item { AccountRow("الإعدادات", "تخصيص تجربة التطبيق", Icons.Default.Settings) { openFeature(context, "settings") } }
            item { AccountRow("عن وصلها", "الإصدار ومعلومات التطبيق", Icons.Default.Info) { openFeature(context, "about") } }
            item {
                Button(
                    onClick = { confirmLogout = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AccountDanger)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("تسجيل الخروج", fontWeight = FontWeight.Black)
                }
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

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("تسجيل الخروج؟", fontWeight = FontWeight.Black, color = AccountInk) },
            text = { Text("سيتم إنهاء جلسة الحساب والعودة إلى شاشة تسجيل الدخول.", color = AccountMuted) },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; session.clear() }) {
                    Text("تسجيل الخروج", color = AccountDanger, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("إلغاء", color = AccountMuted) } }
        )
    }
}

@Composable
fun SettingsCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waslha_preferences", Context.MODE_PRIVATE) }
    var notifications by rememberSaveable { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var location by rememberSaveable { mutableStateOf(prefs.getBoolean("location", true)) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(AccountBg)) {
        AccountHeader("الإعدادات", onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { AccountSectionTitle("تفضيلات التطبيق") }
            item {
                SettingToggle("الإشعارات", "تنبيهات الرحلات والتحديثات", Icons.Default.Notifications, notifications) {
                    notifications = it
                    prefs.edit().putBoolean("notifications", it).apply()
                }
            }
            item {
                SettingToggle("الموقع أثناء الحجز", "استخدام موقعك لتحديد نقطة الانطلاق", Icons.Default.LocationOn, location) {
                    location = it
                    prefs.edit().putBoolean("location", it).apply()
                }
            }
            item { AccountSectionTitle("الحساب والخصوصية") }
            item { AccountRow("الخصوصية والأمان", "إدارة خيارات الخصوصية", Icons.Default.Security) { openFeature(context, "security") } }
            item { AccountRow("حماية تسجيل الدخول", "معلومات حماية جلسة الحساب", Icons.Default.Lock) { openFeature(context, "security") } }
            item { AccountSectionTitle("الدعم والخدمات") }
            item { AccountRow("الأماكن المحفوظة", "إدارة المنزل والعمل والمفضلة", Icons.Default.LocationOn) { openFeature(context, "places") } }
            item { AccountRow("طرق الدفع", "طريقة الدفع", Icons.Default.CreditCard) { openFeature(context, "payments") } }
            item { AccountRow("الإشعارات", "عرض التنبيهات", Icons.Default.Notifications) { openFeature(context, "notifications") } }
            item { AccountRow("الدعم", "الحصول على المساعدة", Icons.Default.HelpOutline) { openFeature(context, "support") } }
            item { AccountRow("عن وصلها", "معلومات التطبيق", Icons.Default.Info) { openFeature(context, "about") } }
            item {
                TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("إعادة تفضيلات التطبيق", color = AccountDanger, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("إعادة التفضيلات؟", fontWeight = FontWeight.Black) },
            text = { Text("سيتم إعادة الإشعارات والموقع إلى الوضع الافتراضي.", color = AccountMuted) },
            confirmButton = {
                TextButton(onClick = {
                    notifications = true
                    location = true
                    prefs.edit().putBoolean("notifications", true).putBoolean("location", true).apply()
                    confirmReset = false
                }) { Text("إعادة", color = AccountDanger, fontWeight = FontWeight.Black) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("إلغاء") } }
        )
    }
}

@Composable
private fun AccountHeader(title: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) { Text("رجوع", color = AccountGreen, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.weight(1f))
        Text(title, color = AccountInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.size(14.dp))
    }
}

@Composable
private fun AccountSectionTitle(text: String) {
    Text(
        text,
        color = AccountMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    )
}

@Composable
private fun AccountRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
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
private fun SettingToggle(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(horizontal = 15.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(AccountSoft, androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = AccountGreen)
            }
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
    var name by rememberSaveable { mutableStateOf(session.name.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(session.phone.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(session.email.orEmpty()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات الحساب", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; error = null }, label = { Text("الاسم") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it; error = null }, label = { Text("رقم الهاتف") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it; error = null }, label = { Text("البريد الإلكتروني") }, singleLine = true)
                if (error != null) Text(error.orEmpty(), color = AccountDanger, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank() && phone.isBlank() && email.isBlank()) {
                    error = "أدخل معلومة واحدة على الأقل."
                } else {
                    session.updateProfile(name.trim().ifBlank { null }, phone.trim().ifBlank { null }, email.trim().ifBlank { null })
                    onSaved()
                }
            }) { Text("حفظ", color = AccountGreen, fontWeight = FontWeight.Black) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = AccountMuted) } }
    )
}
