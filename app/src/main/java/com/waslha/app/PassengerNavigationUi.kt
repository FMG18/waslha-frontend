package com.waslha.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NavGreen = Color(0xFF078A60)
private val NavInk = Color(0xFF10201B)
private val NavMuted = Color(0xFF72807B)
private val NavSoft = Color(0xFFE8F6F0)
private val NavBg = Color(0xFFF4F7F5)
private val NavDanger = Color(0xFFB42318)

@Composable
fun PassengerBottomBar(selectedTab: Int, onTab: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(0, selectedTab == 0, "الرئيسية", Icons.Default.Home, onTab)
            NavItem(1, selectedTab == 1, "رحلاتي", Icons.Default.History, onTab)
            NavItem(2, selectedTab == 2, "حسابي", Icons.Default.Person, onTab)
        }
    }
}

@Composable
private fun NavItem(
    index: Int,
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onTab: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .clickable { onTab(index) }
            .background(if (selected) NavSoft else Color.Transparent, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            null,
            tint = if (selected) NavGreen else NavMuted,
            modifier = Modifier.size(23.dp)
        )
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            color = if (selected) NavInk else NavMuted
        )
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    var notificationsEnabled by remember { mutableStateOf(true) }
    var locationEnabled by remember { mutableStateOf(true) }
    var openDialog by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxWidth().background(NavBg).padding(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("رجوع", color = NavGreen, fontWeight = FontWeight.Bold)
            }
            Text(
                "الإعدادات",
                modifier = Modifier.padding(start = 8.dp),
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = NavInk
            )
        }

        Card(
            Modifier.fillMaxWidth().padding(top = 14.dp),
            colors = CardDefaults.cardColors(Color.White),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(vertical = 7.dp)) {
                SettingToggleRow(
                    title = "الإشعارات",
                    subtitle = "تنبيهات الرحلات والعروض",
                    icon = Icons.Default.Notifications,
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it }
                )
                SettingToggleRow(
                    title = "الموقع أثناء الحجز",
                    subtitle = "استخدام موقعك لتحديد نقطة الانطلاق",
                    icon = Icons.Default.Home,
                    checked = locationEnabled,
                    onCheckedChange = { locationEnabled = it }
                )
                SettingActionRow(
                    title = "الخصوصية والأمان",
                    subtitle = "حماية الحساب ومعلوماته",
                    icon = Icons.Default.Security,
                    onClick = { openDialog = "privacy" }
                )
                SettingActionRow(
                    title = "قفل الحساب",
                    subtitle = "معلومات الحماية وتسجيل الدخول",
                    icon = Icons.Default.Lock,
                    onClick = { openDialog = "lock" }
                )
                SettingActionRow(
                    title = "عن وصلها",
                    subtitle = "الإصدار ومعلومات التطبيق",
                    icon = Icons.Default.Info,
                    onClick = { openDialog = "about" }
                )
            }
        }
    }

    openDialog?.let { dialog ->
        val title = when (dialog) {
            "privacy" -> "الخصوصية والأمان"
            "lock" -> "حماية الحساب"
            else -> "عن وصلها"
        }
        val body = when (dialog) {
            "privacy" -> "يستخدم التطبيق صلاحيات الموقع فقط عند الحاجة إلى تحديد نقطة الانطلاق."
            "lock" -> "جلسة تسجيل الدخول محفوظة محليًا، ويمكن إنهاؤها من صفحة الحساب."
            else -> "وصلها — تطبيق تاكسي للركاب. هذه نسخة تجريبية مخصصة لرحلات التاكسي داخل سوريا."
        }
        AlertDialog(
            onDismissRequest = { openDialog = null },
            title = { Text(title, fontWeight = FontWeight.Black, color = NavInk) },
            text = { Text(body, color = NavMuted) },
            confirmButton = {
                TextButton(onClick = { openDialog = null }) {
                    Text("حسنًا", color = NavGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NavGreen, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = NavInk)
            Text(subtitle, fontSize = 10.sp, color = NavMuted)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingActionRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NavGreen, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = NavInk)
            Text(subtitle, fontSize = 10.sp, color = NavMuted)
        }
        Text("‹", color = NavMuted, fontSize = 26.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
fun PassengerLegacyProfileScreen(
    sessionStore: SessionStore,
    onSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Column(Modifier.background(NavBg).padding(18.dp)) {
        Text("حسابي", fontSize = 30.sp, fontWeight = FontWeight.Black, color = NavInk)
        Text("إدارة معلومات حسابك", color = NavMuted, fontSize = 12.sp)

        Card(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            colors = CardDefaults.cardColors(Color.White),
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            sessionStore.name?.takeIf { it.isNotBlank() } ?: "مستخدم وصلها",
                            fontWeight = FontWeight.Black,
                            color = NavInk,
                            fontSize = 20.sp
                        )
                        Text(
                            sessionStore.email ?: sessionStore.phone ?: "بيانات التواصل غير متاحة",
                            color = NavMuted,
                            fontSize = 11.sp
                        )
                    }
                    Icon(Icons.Default.Person, null, tint = NavGreen, modifier = Modifier.size(32.dp))
                }
                Text("معرّف المستخدم: ${sessionStore.userId ?: "—"}", color = NavMuted, fontSize = 10.sp)
                TextButton(onClick = onSettings) { Text("الإعدادات", color = NavGreen, fontWeight = FontWeight.Bold) }
                TextButton(onClick = onLogout) { Text("تسجيل الخروج", color = NavDanger, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
