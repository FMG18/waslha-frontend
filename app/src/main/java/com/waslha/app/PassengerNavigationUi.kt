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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val NavGreen = Color(0xFF078A60)
private val NavInk = Color(0xFF10201B)
private val NavMuted = Color(0xFF72807B)

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
            NavItem(1, selectedTab == 1, "رحلاتي", Icons.Default.Home, onTab)
            NavItem(2, selectedTab == 2, "حسابي", Icons.Default.Person, onTab)
        }
    }
}

@Composable
private fun NavItem(index: Int, selected: Boolean, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onTab: (Int) -> Unit) {
    Column(
        modifier = Modifier.clickable { onTab(index) }.padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = if (selected) NavGreen else NavMuted, modifier = Modifier.size(22.dp))
        Text(label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, color = if (selected) NavGreen else NavMuted)
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    Column(Modifier.padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع", color = NavGreen) }
            Text("الإعدادات", fontSize = 26.sp, fontWeight = FontWeight.Black, color = NavInk)
        }
        Card(Modifier.fillMaxWidth().padding(top = 14.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingRow("الإشعارات", "إدارة إشعارات الرحلات", Icons.Default.Settings)
                SettingRow("الخصوصية والأمان", "خيارات حماية الحساب", Icons.Default.Person)
                SettingRow("عن وصلها", "معلومات التطبيق", Icons.Default.Home)
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = NavGreen, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = NavInk)
            Text(subtitle, fontSize = 10.sp, color = NavMuted)
        }
    }
}

@Composable
fun PassengerLegacyProfileScreen(sessionStore: SessionStore, onSettings: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.padding(18.dp)) {
        Text("حسابي", fontSize = 30.sp, fontWeight = FontWeight.Black, color = NavInk)
        Text("إدارة معلومات حسابك", color = NavMuted, fontSize = 12.sp)
        Card(Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("بيانات الحساب", fontWeight = FontWeight.Black, color = NavInk)
                Text(sessionStore.phone ?: "رقم الهاتف غير متاح", color = NavMuted, fontSize = 12.sp)
                Text("معرّف المستخدم: ${sessionStore.userId ?: "—"}", color = NavMuted, fontSize = 10.sp)
                TextButton(onClick = onSettings) { Text("الإعدادات", color = NavGreen) }
                TextButton(onClick = onLogout) { Text("تسجيل الخروج", color = Color(0xFFB42318)) }
            }
        }
    }
}
