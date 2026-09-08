package com.waslha.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

private val TripGreen = Color(0xFF078A60)
private val TripDark = Color(0xFF056C4B)
private val TripInk = Color(0xFF10201B)
private val TripMuted = Color(0xFF6D7B76)
private val TripSurface = Color(0xFFF3F7F5)

@Composable
fun TripSafetyPanel(onShare: () -> Unit, onEmergency: () -> Unit) {
    var showSafety by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(21.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("الأمان", fontWeight = FontWeight.Black, fontSize = 16.sp, color = TripInk)
            Spacer(Modifier.height(9.dp))
            SafetyAction("مشاركة تفاصيل الرحلة", "شارك موقع الرحلة مع شخص تثق به", Icons.Default.LocationOn, onShare)
            SafetyAction("مركز الأمان", "إرشادات وأدوات السلامة", Icons.Default.Security) { showSafety = true }
            SafetyAction("مساعدة عاجلة", "للحالات التي تحتاج تدخلاً سريعاً", Icons.Default.Warning, onEmergency)
        }
    }
    if (showSafety) {
        AlertDialog(
            onDismissRequest = { showSafety = false },
            title = { Text("مركز الأمان", fontWeight = FontWeight.Black) },
            text = { Text("تأكد من بيانات السيارة قبل الركوب. يمكنك مشاركة تفاصيل الرحلة وطلب المساعدة من داخل شاشة الرحلة." , color = TripMuted) },
            confirmButton = { TextButton(onClick = { showSafety = false }) { Text("حسناً", color = TripGreen) } }
        )
    }
}

@Composable
private fun SafetyAction(label: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).background(TripSurface, CircleShape), Alignment.Center) { Icon(icon, null, tint = TripGreen) }
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, color = TripInk, fontSize = 13.sp)
            Text(subtitle, color = TripMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun CancelTripDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var selected by remember { mutableStateOf("") }
    val reasons = listOf("غيّرت رأيي", "وجدت سيارة أخرى", "الكابتن بعيد", "وقت الانتظار طويل", "سبب آخر")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إلغاء الرحلة", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("اختر سبب الإلغاء", color = TripMuted, fontSize = 12.sp)
                reasons.forEach { reason ->
                    Card(
                        colors = CardDefaults.cardColors(if (selected == reason) TripGreen.copy(alpha = .10f) else TripSurface),
                        shape = RoundedCornerShape(13.dp),
                        modifier = Modifier.fillMaxWidth().clickable { selected = reason }
                    ) { Text(reason, Modifier.padding(12.dp), fontWeight = FontWeight.SemiBold, color = TripInk) }
                }
            }
        },
        confirmButton = { TextButton(enabled = selected.isNotBlank(), onClick = { onConfirm(selected) }) { Text("تأكيد الإلغاء", color = TripGreen) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("رجوع") } }
    )
}

@Composable
fun TripProgressTimeline(status: TripStatus) {
    val stages = listOf(
        TripStatus.REQUESTING to "نبحث عن كابتن",
        TripStatus.ASSIGNED to "تم تعيين الكابتن",
        TripStatus.ARRIVING to "الكابتن في الطريق إليك",
        TripStatus.PICKED_UP to "بدأت الرحلة",
        TripStatus.COMPLETED to "اكتملت الرحلة"
    )
    Column {
        stages.forEachIndexed { index, pair ->
            val done = status.ordinal >= pair.first.ordinal
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(14.dp).background(if (done) TripGreen else Color(0xFFD0D9D5), CircleShape))
                Spacer(Modifier.size(10.dp))
                Text(pair.second, color = if (done) TripInk else TripMuted, fontWeight = if (done) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
            }
            if (index != stages.lastIndex) Spacer(Modifier.height(6.dp))
        }
    }
}

enum class TripStatus { REQUESTING, ASSIGNED, ARRIVING, PICKED_UP, COMPLETED }

@Composable
fun DriverContactCard(onCall: () -> Unit, onClose: () -> Unit) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(55.dp).background(TripSurface, CircleShape), Alignment.Center) { Text("👨🏻", fontSize = 25.sp) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text("محمد — كابتن وصلها", fontWeight = FontWeight.Black, color = TripInk)
                Text("Toyota Corolla • 1234", color = TripMuted, fontSize = 11.sp)
                Text("4.9 ★ • 3 دقائق", color = TripGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onCall, modifier = Modifier.background(TripGreen.copy(alpha = .10f), CircleShape)) { Icon(Icons.Default.Call, "اتصال", tint = TripGreen) }
            Spacer(Modifier.size(6.dp))
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TripMuted) }
        }
    }
}

@Composable
fun TripDemoAction(onOpen: () -> Unit) {
    Button(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TripDark)
    ) {
        Icon(Icons.Default.DirectionsCar, null)
        Spacer(Modifier.size(8.dp))
        Text("فتح تفاصيل الرحلة", fontWeight = FontWeight.ExtraBold)
    }
}
