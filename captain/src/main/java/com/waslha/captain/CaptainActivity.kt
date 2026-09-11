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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF087F5B)
private val GreenDark = Color(0xFF055C42)
private val Mint = Color(0xFFE7F6F0)
private val Ink = Color(0xFF12201B)
private val Muted = Color(0xFF6D7A75)
private val Background = Color(0xFFF5F8F6)
private val White = Color.White

class CaptainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { CaptainApp() }
    }
}

@Composable
private fun CaptainApp() {
    var online by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }

    MaterialTheme {
        Surface(color = Background, modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = White,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.DirectionsCar, null) }, label = { Text("الرئيسية") })
                        NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.AccessTime, null) }, label = { Text("الرحلات") })
                        NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.Person, null) }, label = { Text("حسابي") })
                    }
                }
            ) { padding ->
                when (tab) {
                    0 -> CaptainHome(Modifier.padding(padding), online) { online = !online }
                    1 -> TripsPlaceholder(Modifier.padding(padding))
                    else -> ProfilePlaceholder(Modifier.padding(padding))
                }
            }
        }
    }
}

@Composable
private fun CaptainHome(modifier: Modifier, online: Boolean, onToggle: () -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(18.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("أهلًا كابتن", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("خلينا نوصلك لأكثر اليوم", color = Muted, fontSize = 12.sp)
                }
                Box(Modifier.size(46.dp).clip(CircleShape).background(Mint), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notifications, null, tint = Green, modifier = Modifier.size(24.dp))
                }
            }
        }

        item {
            Card(
                Modifier.fillMaxWidth().clickable { onToggle() },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(if (online) Green else White),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(54.dp).clip(CircleShape).background(if (online) Color.White.copy(alpha = .16f) else Mint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PowerSettingsNew, null, tint = if (online) White else Green, modifier = Modifier.size(27.dp))
                    }
                    Spacer(Modifier.size(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (online) "أنت متصل الآن" else "أنت غير متصل", color = if (online) White else Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(if (online) "تقدر تستقبل طلبات جديدة" else "فعّل الحالة حتى تستقبل طلبات", color = if (online) White.copy(alpha = .75f) else Muted, fontSize = 12.sp)
                    }
                    Text(if (online) "متصل" else "تشغيل", color = if (online) White else Green, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("رحلات اليوم", "0", Icons.Default.CheckCircle, Modifier.weight(1f))
                StatCard("أرباح اليوم", "0 ل.س", Icons.Default.LocationOn, Modifier.weight(1f))
                StatCard("التقييم", "5.0", Icons.Default.Star, Modifier.weight(1f))
            }
        }

        item {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(White)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("الطلبات الجديدة", color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("ماكو طلبات حالياً", color = Muted, fontSize = 13.sp)
                    Text("راح يظهر الطلب هنا فور توفره.", color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(White)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(20.dp))
            Text(value, color = Ink, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(title, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TripsPlaceholder(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AccessTime, null, tint = Green, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(10.dp))
            Text("رحلات الكابتن", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("سجل الرحلات سيُربط بالـBackend في الدفعة القادمة.", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ProfilePlaceholder(modifier: Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Person, null, tint = Green, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(10.dp))
            Text("حساب الكابتن", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("الملف، السيارة، الأرباح والإعدادات قادمة ضمن مراحل التطبيق.", color = Muted, fontSize = 12.sp)
        }
    }
}
