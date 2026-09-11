package com.waslha.app

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NavGreen = Color(0xFF087F5B)
private val NavInk = Color(0xFF12201B)
private val NavMuted = Color(0xFF6D7A75)
private val NavSoft = Color(0xFFE7F6F0)
private val NavBg = Color(0xFFF7F9F8)
private val NavDanger = Color(0xFFB42318)
private val NavLine = Color(0xFFDDE5E1)

private fun launchFeature(context: Context, screen: String) {
    context.startActivity(Intent(context, FeatureHostActivity::class.java).putExtra("screen", screen))
}

@Composable
fun PassengerBottomBar(selectedTab: Int, onTab: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, NavLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(0, selectedTab == 0, "الرئيسية", Icons.Default.Home, onTab)
            NavItem(1, selectedTab == 1, "رحلاتي", Icons.Default.LocationOn, onTab)
            NavItem(2, selectedTab == 2, "حسابي", Icons.Default.AccountCircle, onTab)
        }
    }
}

@Composable
private fun NavItem(index: Int, selected: Boolean, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onTab: (Int) -> Unit) {
    val container by animateColorAsState(if (selected) NavSoft else Color.Transparent, tween(180), label = "navContainer")
    val iconTint by animateColorAsState(if (selected) NavGreen else NavMuted, tween(180), label = "navIconTint")
    val horizontalPadding by animateDpAsState(if (selected) 20.dp else 16.dp, tween(180), label = "navPadding")
    val iconSize by animateDpAsState(if (selected) 24.dp else 21.dp, tween(180), label = "navIconSize")
    val scale by animateFloatAsState(if (selected) 1f else .96f, tween(180), label = "navScale")
    Column(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable { onTab(index) }
            .background(container, RoundedCornerShape(18.dp))
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(colors = CardDefaults.cardColors(if (selected) Color.White else Color.Transparent), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(0.dp)) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.padding(if (selected) 6.dp else 5.dp).size(iconSize))
        }
        Spacer(Modifier.size(3.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, color = if (selected) NavInk else NavMuted)
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waslha_preferences", Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var locationEnabled by remember { mutableStateOf(prefs.getBoolean("location", true)) }
    Column(Modifier.fillMaxWidth().background(NavBg).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع", color = NavGreen, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            Text("الإعدادات", fontSize = 25.sp, fontWeight = FontWeight.Black, color = NavInk)
        }
        Text("خصّص تجربة وصلها على طريقتك", color = NavMuted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 3.dp))
        SettingsGroup("تفضيلات التطبيق") {
            SettingToggleRow("الإشعارات", "تنبيهات الرحلات والتحديثات", Icons.Default.Notifications, notificationsEnabled) { notificationsEnabled = it; prefs.edit().putBoolean("notifications", it).apply() }
            SettingToggleRow("الموقع أثناء الحجز", "استخدام الموقع لتحديد نقطة الانطلاق", Icons.Default.LocationOn, locationEnabled) { locationEnabled = it; prefs.edit().putBoolean("location", it).apply() }
        }
        SettingsGroup("الحساب والحماية") {
            SettingActionRow("الخصوصية والأمان", "حماية الحساب وصلاحيات التطبيق", Icons.Default.Security) { launchFeature(context, "security") }
            SettingActionRow("حماية تسجيل الدخول", "معلومات الجلسة وطرق الحماية", Icons.Default.AccountCircle) { launchFeature(context, "security") }
        }
        SettingsGroup("الخدمات") {
            SettingActionRow("الأماكن المحفوظة", "المنزل والعمل والمفضلة", Icons.Default.LocationOn) { launchFeature(context, "places") }
            SettingActionRow("طرق الدفع", "اختيار طريقة الدفع", Icons.Default.CreditCard) { launchFeature(context, "payments") }
            SettingActionRow("الإشعارات", "عرض آخر التنبيهات", Icons.Default.Notifications) { launchFeature(context, "notifications") }
            SettingActionRow("المساعدة والدعم", "الأسئلة والحلول", Icons.Default.HelpOutline) { launchFeature(context, "support") }
            SettingActionRow("عن وصلها", "معلومات التطبيق", Icons.Default.Info) { launchFeature(context, "about") }
        }
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(title, color = NavMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(21.dp), border = BorderStroke(1.dp, NavLine), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(vertical = 4.dp)) { content() }
        }
    }
}

@Composable
private fun SettingToggleRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val iconBg by animateColorAsState(if (checked) NavSoft else NavBg, tween(180), label = "toggleIconBg")
    val iconTint by animateColorAsState(if (checked) NavGreen else NavMuted, tween(180), label = "toggleIconTint")
    Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Card(colors = CardDefaults.cardColors(iconBg), shape = RoundedCornerShape(13.dp), elevation = CardDefaults.cardElevation(0.dp)) { Icon(icon, null, tint = iconTint, modifier = Modifier.padding(9.dp).size(21.dp)) }
        Column(Modifier.weight(1f).padding(start = 11.dp)) { Text(title, fontWeight = FontWeight.Bold, color = NavInk); Text(subtitle, fontSize = 10.sp, color = NavMuted) }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingActionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val pressedScale by animateFloatAsState(1f, tween(120), label = "settingScale")
    Row(
        Modifier
            .graphicsLayer { scaleX = pressedScale; scaleY = pressedScale }
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(colors = CardDefaults.cardColors(NavSoft), shape = RoundedCornerShape(13.dp), elevation = CardDefaults.cardElevation(0.dp)) { Icon(icon, null, tint = NavGreen, modifier = Modifier.padding(9.dp).size(21.dp)) }
        Column(Modifier.weight(1f).padding(start = 11.dp)) { Text(title, fontWeight = FontWeight.Bold, color = NavInk); Text(subtitle, fontSize = 10.sp, color = NavMuted) }
        Text("‹", color = NavMuted, fontSize = 24.sp)
    }
}

@Composable
fun PassengerLegacyProfileScreen(sessionStore: SessionStore, onSettings: () -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().background(NavBg).padding(18.dp)) {
        Text("حسابي", fontSize = 29.sp, fontWeight = FontWeight.Black, color = NavInk)
        Text("إدارة معلومات حسابك", color = NavMuted, fontSize = 12.sp)
        Card(Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(23.dp), border = BorderStroke(1.dp, NavLine), elevation = CardDefaults.cardElevation(0.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                Card(colors = CardDefaults.cardColors(NavSoft), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(0.dp)) { Icon(Icons.Default.AccountCircle, null, tint = NavGreen, modifier = Modifier.padding(11.dp).size(37.dp)) }
                Text(sessionStore.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها", fontWeight = FontWeight.Black, color = NavInk, fontSize = 20.sp)
                Text(sessionStore.email ?: sessionStore.phone ?: "بيانات التواصل غير متاحة", color = NavMuted, fontSize = 11.sp)
                TextButton(onClick = { launchFeature(context, "account") }) { Text("تعديل الحساب", color = NavGreen, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onSettings) { Text("الإعدادات", color = NavGreen, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onLogout) { Text("تسجيل الخروج", color = NavDanger, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
