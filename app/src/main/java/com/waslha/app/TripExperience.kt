package com.waslha.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TripGreen = Color(0xFF087F5B)
private val TripDark = Color(0xFF055C42)
private val TripInk = Color(0xFF12201B)
private val TripMuted = Color(0xFF6D7A75)
private val TripSurface = Color(0xFFF7F9F8)
private val TripSoft = Color(0xFFE7F6F0)
private val TripLine = Color(0xFFDDE5E1)
private val TripDanger = Color(0xFFB42318)

@Composable
fun TripLiveStatusCard(
    status: TripStatus,
    etaMinutes: Int? = null,
    plate: String? = null,
    vehicle: String? = null
) {
    val title = when (status) {
        TripStatus.REQUESTING -> "جاري البحث عن كابتن"
        TripStatus.ASSIGNED -> "تم العثور على كابتن"
        TripStatus.ARRIVING -> "الكابتن في طريقه إليك"
        TripStatus.PICKED_UP -> "الرحلة جارية"
        TripStatus.COMPLETED -> "اكتملت الرحلة"
    }
    val subtitle = when (status) {
        TripStatus.REQUESTING -> "نبحث عن أقرب كابتن متاح لك"
        TripStatus.ASSIGNED, TripStatus.ARRIVING -> etaMinutes?.let { "الوصول المتوقع خلال $it دقيقة" } ?: "نحدّث وقت الوصول باستمرار"
        TripStatus.PICKED_UP -> "نتابع مسارك حتى الوصول إلى الوجهة"
        TripStatus.COMPLETED -> "شكراً لاستخدامك وصلها"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, TripLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(TripSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.DirectionsCar, null, tint = TripGreen, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.size(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = TripInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text(subtitle, color = TripMuted, fontSize = 10.sp)
                }
                if (etaMinutes != null && (status == TripStatus.ASSIGNED || status == TripStatus.ARRIVING)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("ETA", color = TripMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("$etaMinutes د", color = TripGreen, fontSize = 17.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            if (plate != null || vehicle != null) {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TripSurface).padding(11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DirectionsCar, null, tint = TripGreen, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(vehicle ?: "السيارة", color = TripInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        plate?.let { Text("رقم اللوحة: $it", color = TripMuted, fontSize = 9.sp) }
                    }
                    Box(Modifier.size(8.dp).clip(CircleShape).background(TripGreen))
                }
            }
        }
    }
}

@Composable
fun TripRouteSummary(
    pickup: String,
    destination: String,
    distanceKm: Double? = null,
    durationMin: Int? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, TripLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RouteLineItem("الانطلاق", pickup, Icons.Default.MyLocation)
            Box(
                Modifier.padding(start = 10.dp).size(2.dp, 14.dp).clip(RoundedCornerShape(1.dp)).background(TripLine)
            )
            RouteLineItem("الوجهة", destination, Icons.Default.LocationOn, destinationColor = Color(0xFFD93838))
            if (distanceKm != null || durationMin != null) {
                Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (distanceKm != null) MetricChip("${distanceKm} كم")
                    if (durationMin != null) MetricChip("${durationMin} دقيقة")
                }
            }
        }
    }
}

@Composable
private fun RouteLineItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    destinationColor: Color = TripGreen
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(if (destinationColor == TripGreen) TripSoft else Color(0xFFFFEFEC)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = destinationColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TripMuted, fontSize = 9.sp)
            Text(value, color = TripInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MetricChip(value: String) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(TripSoft).padding(horizontal = 9.dp, vertical = 6.dp)) {
        Text(value, color = TripGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TripSafetyPanel(onShare: () -> Unit, onEmergency: () -> Unit) {
    var showSafety by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, TripLine),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(TripSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Security, null, tint = TripGreen, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.size(10.dp))
                Column {
                    Text("الأمان أولاً", fontWeight = FontWeight.Black, fontSize = 16.sp, color = TripInk)
                    Text("أدوات الحماية متاحة أثناء الرحلة", color = TripMuted, fontSize = 10.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            SafetyAction("مشاركة الرحلة", "شارك تفاصيل الرحلة مع شخص تثق به", Icons.Default.LocationOn, onShare)
            SafetyAction("مركز الأمان", "إرشادات وأدوات السلامة", Icons.Default.Security, onClick = { showSafety = true })
            SafetyAction("مساعدة عاجلة", "للحالات التي تحتاج تدخلاً سريعاً", Icons.Default.Warning, onEmergency, danger = true)
        }
    }
    if (showSafety) {
        AlertDialog(
            onDismissRequest = { showSafety = false },
            title = { Text("مركز الأمان", fontWeight = FontWeight.Black) },
            text = { Text("تأكد من بيانات السيارة قبل الركوب. يمكنك مشاركة تفاصيل الرحلة وطلب المساعدة من داخل شاشة الرحلة.", color = TripMuted) },
            confirmButton = { TextButton(onClick = { showSafety = false }) { Text("حسناً", color = TripGreen, fontWeight = FontWeight.Bold) } }
        )
    }
}

@Composable
private fun SafetyAction(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    danger: Boolean = false
) {
    val tint = if (danger) TripDanger else TripGreen
    val bg = if (danger) Color(0xFFFFF2F0) else TripSoft
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, color = if (danger) TripDanger else TripInk, fontSize = 13.sp)
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
                    val selectedState by animateColorAsState(
                        if (selected == reason) TripSoft else TripSurface,
                        animationSpec = tween(160),
                        label = "cancelReason"
                    )
                    Card(
                        colors = CardDefaults.cardColors(selectedState),
                        border = BorderStroke(1.dp, if (selected == reason) TripGreen else TripLine),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().clickable { selected = reason }
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(20.dp).clip(CircleShape).background(if (selected == reason) TripGreen else Color.Transparent)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(reason, fontWeight = if (selected == reason) FontWeight.Bold else FontWeight.Medium, color = TripInk)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(enabled = selected.isNotBlank(), onClick = { onConfirm(selected) }) { Text("تأكيد الإلغاء", color = TripDanger, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("رجوع", color = TripMuted) } }
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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        stages.forEachIndexed { index, pair ->
            val done = status.ordinal >= pair.first.ordinal
            val active = status == pair.first
            val dotColor by animateColorAsState(if (done) TripGreen else TripLine, tween(180), label = "timelineDot$index")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(if (active) 16.dp else 13.dp).clip(CircleShape).background(dotColor), contentAlignment = Alignment.Center) {
                    if (active) Box(Modifier.size(6.dp).clip(CircleShape).background(Color.White))
                }
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(pair.second, color = if (done) TripInk else TripMuted, fontWeight = if (active) FontWeight.Black else if (done) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp)
                    if (active) Text("الحالة الحالية", color = TripGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                if (done) Text("✓", color = TripGreen, fontWeight = FontWeight.Black)
            }
            if (index != stages.lastIndex) {
                Box(Modifier.padding(start = 6.dp).size(2.dp, 13.dp).background(if (status.ordinal > pair.first.ordinal) TripGreen else TripLine))
            }
        }
    }
}

enum class TripStatus { REQUESTING, ASSIGNED, ARRIVING, PICKED_UP, COMPLETED }

@Composable
fun DriverContactCard(onCall: () -> Unit, onClose: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(23.dp),
        border = BorderStroke(1.dp, TripLine),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(TripSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, null, tint = TripGreen, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("الكابتن", fontWeight = FontWeight.Black, color = TripInk, fontSize = 16.sp)
                    Text("معلومات الكابتن والسيارة تظهر هنا", color = TripMuted, fontSize = 10.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(TripGreen))
                        Spacer(Modifier.size(5.dp))
                        Text("متصل", color = TripGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
                IconButton(onClick = onCall, modifier = Modifier.background(TripSoft, CircleShape)) {
                    Icon(Icons.Default.Call, "اتصال", tint = TripGreen)
                }
                Spacer(Modifier.size(6.dp))
                IconButton(onClick = onClose) { Icon(Icons.Default.Close, "إغلاق", tint = TripMuted) }
            }
        }
    }
}

@Composable
fun TripDemoAction(onOpen: () -> Unit) {
    Button(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TripDark)
    ) {
        Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text("فتح تفاصيل الرحلة", fontWeight = FontWeight.Black)
    }
}
