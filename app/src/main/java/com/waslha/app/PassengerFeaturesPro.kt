package com.waslha.app

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ProGreen = Color(0xFF078A60)
private val ProInk = Color(0xFF10201B)
private val ProMuted = Color(0xFF71807A)
private val ProBg = Color(0xFFF4F7F5)
private val ProSoft = Color(0xFFE8F6F0)
private val ProWarning = Color(0xFFE6A500)
private val ProDanger = Color(0xFFB42318)

@Composable
fun ProSavedPlacesScreen(onBack: () -> Unit) {
    ProScreen("الأماكن المحفوظة", onBack) {
        Text("اختصاراتك للوصول السريع", color = ProMuted, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        listOf("المنزل" to "موقع محفوظ", "العمل" to "موقع محفوظ", "المفضلة" to "مكان محفوظ").forEachIndexed { index, pair ->
            ProListCard(pair.first, pair.second, listOf("⌂", "▣", "★")[index], Icons.Default.LocationOn)
            Spacer(Modifier.height(8.dp))
        }
        ProHint("احفظ الأماكن التي تزورها كثيرًا لتسهيل حجز الرحلة القادمة.")
    }
}

@Composable
fun ProPaymentsScreen(onBack: () -> Unit) {
    ProScreen("طرق الدفع", onBack) {
        Text("طريقة الدفع الحالية", color = ProMuted, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(ProSoft), shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                ProIconBox("💵", ProGreen)
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("الدفع نقدًا", color = ProInk, fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("الدفع للكابتن بعد الوصول", color = ProMuted, fontSize = 11.sp)
                }
                Icon(Icons.Default.CheckCircle, null, tint = ProGreen)
            }
        }
        Spacer(Modifier.height(10.dp))
        ProListCard("محفظة وصلها", "الرصيد والخدمات المالية لاحقًا", "◈", Icons.Default.CreditCard)
        Spacer(Modifier.height(12.dp))
        ProHint("حاليًا الدفع النقدي هو الخيار الافتراضي والآمن لمرحلة الاختبار.")
    }
}

@Composable
fun ProNotificationsScreen(onBack: () -> Unit) {
    ProScreen("الإشعارات", onBack) {
        val entries = listOf(
            "تحديث الرحلة" to "نُحدّثك عند تغيير حالة طلبك.",
            "تذكير" to "لا تنسَ تقييم رحلتك بعد الوصول.",
            "وصلها" to "نتمنى لك رحلة مريحة وآمنة."
        )
        entries.forEachIndexed { index, entry ->
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProIconBox("🔔", if (index == 0) ProGreen else ProMuted)
                    Spacer(Modifier.size(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.first, color = ProInk, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(entry.second, color = ProMuted, fontSize = 11.sp)
                    }
                    if (index == 0) Box(Modifier.size(8.dp).background(ProGreen, CircleShape))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProSupportScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf<String?>(null) }
    ProScreen("المساعدة والدعم", onBack) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(ProGreen), shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                ProIconBox("؟", Color.White)
                Spacer(Modifier.size(11.dp))
                Column {
                    Text("محتاج مساعدة؟", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("اختر موضوع المشكلة", color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        listOf(
            "طلب التاكسي" to "حدد الانطلاق والوجهة ثم اختر نوع السيارة واضغط طلب تاكسي.",
            "إلغاء الرحلة" to "يمكن إلغاء الرحلة من شاشة الرحلة الحالية قبل انتهائها.",
            "الدفع" to "النسخة الحالية تعتمد الدفع النقدي للكابتن.",
            "الأمان" to "عند الحاجة استخدم الدعم وأوقف الرحلة إذا واجهت مشكلة."
        ).forEach { item ->
            ProListCard(item.first, "اضغط لمعرفة التفاصيل", "•", Icons.Default.HelpOutline) { selected = item.second }
            Spacer(Modifier.height(8.dp))
        }
    }
    selected?.let { message ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("المساعدة", fontWeight = FontWeight.Black) },
            text = { Text(message, color = ProMuted) },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("حسنًا", color = ProGreen) } }
        )
    }
}

@Composable
fun ProRatingScreen(onBack: () -> Unit) {
    var rating by remember { mutableIntStateOf(0) }
    var submitted by remember { mutableStateOf(false) }
    ProScreen("تقييم الرحلة", onBack) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(25.dp)) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ProIconBox("🚕", ProGreen)
                Spacer(Modifier.height(12.dp))
                Text("كيف كانت رحلتك؟", color = ProInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
                Text("تقييمك يساعدنا على تحسين وصلها", color = ProMuted, fontSize = 11.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    (1..5).forEach { value ->
                        Icon(
                            Icons.Default.Star,
                            contentDescription = value.toString(),
                            tint = if (value <= rating) ProWarning else Color(0xFFD6DFDB),
                            modifier = Modifier.size(38.dp).clickable { rating = value }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (submitted) {
                    Text("تم حفظ التقييم", color = ProGreen, fontWeight = FontWeight.Bold)
                } else {
                    Button(
                        onClick = { submitted = rating > 0 },
                        enabled = rating > 0,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProGreen)
                    ) { Text("إرسال التقييم", fontWeight = FontWeight.Black) }
                }
            }
        }
    }
}

@Composable
fun ProSecurityScreen(onBack: () -> Unit) {
    ProScreen("الخصوصية والأمان", onBack) {
        ProListCard("حماية الحساب", "جلسة تسجيل الدخول محفوظة بأمان", "🔒", Icons.Default.Security)
        Spacer(Modifier.height(8.dp))
        ProListCard("الموقع", "يُستخدم لتحديد نقطة الانطلاق أثناء الحجز", "📍", Icons.Default.LocationOn)
        Spacer(Modifier.height(8.dp))
        ProListCard("تنبيهات الأمان", "تظهر عند وجود مشكلة أو حالة غير طبيعية", "⚠", Icons.Default.WarningAmber)
        Spacer(Modifier.height(12.dp))
        ProHint("لا تشارك رمز تسجيل الدخول أو بيانات حسابك مع أي شخص.")
    }
}

@Composable
fun ProAboutScreen(onBack: () -> Unit) {
    ProScreen("عن وصلها", onBack) {
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(ProGreen), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(22.dp)) {
                Text("وصلها", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("تطبيق تاكسي للركاب", color = Color.White.copy(alpha = .9f), fontSize = 13.sp)
                Spacer(Modifier.height(14.dp))
                Text("نسخة تجريبية", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        ProHint("الخدمة مخصصة لرحلات التاكسي داخل سوريا في هذه المرحلة.")
    }
}

@Composable
private fun ProScreen(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(ProBg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع", color = ProGreen, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            Text(title, color = ProInk, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            item { Column(Modifier.fillMaxWidth(), content = content) }
        }
    }
}

@Composable
private fun ProListCard(title: String, subtitle: String, symbol: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: (() -> Unit)? = null) {
    Card(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp), elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            ProIconBox(symbol, ProGreen)
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ProInk, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(subtitle, color = ProMuted, fontSize = 10.sp)
            }
            Icon(icon, null, tint = ProGreen, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun ProIconBox(symbol: String, tint: Color) {
    Box(Modifier.size(44.dp).background(ProSoft, CircleShape), contentAlignment = Alignment.Center) {
        Text(symbol, color = tint, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ProHint(text: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsNone, null, tint = ProGreen)
            Spacer(Modifier.size(9.dp))
            Text(text, color = ProMuted, fontSize = 10.sp)
        }
    }
}
