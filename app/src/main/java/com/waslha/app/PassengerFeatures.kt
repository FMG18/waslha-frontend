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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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

private val FeatureGreen = Color(0xFF078A60)
private val FeatureInk = Color(0xFF10201B)
private val FeatureMuted = Color(0xFF6D7B76)
private val FeatureSurface = Color(0xFFF3F7F5)
private val FeatureSoft = Color(0xFFE8F6F0)

@Composable
fun SavedPlacesScreen(onBack: () -> Unit) {
    FeatureScaffold("الأماكن المحفوظة", onBack) {
        Text("اختصارات جاهزة للرحلات المتكررة", color = FeatureMuted, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        val places = listOf(
            SavedPlace("المنزل", "موقع محفوظ", "🏠"),
            SavedPlace("العمل", "موقع محفوظ", "💼"),
            SavedPlace("المفضلة", "مكان محفوظ", "⭐")
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(places) { place -> PlaceRow(place) }
        }
        Spacer(Modifier.height(10.dp))
        HintCard("يمكن ربط هذه الأماكن بمواضع فعلية لاحقًا من شاشة الخريطة.")
    }
}

@Composable
fun PaymentsScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf("نقداً") }
    FeatureScaffold("طرق الدفع", onBack) {
        Text("اختر الطريقة المستخدمة عند طلب الرحلة", color = FeatureMuted, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        PaymentChoice("نقداً", "الدفع للكابتن بعد الوصول", "💵", selected == "نقداً") { selected = "نقداً" }
        Spacer(Modifier.height(9.dp))
        PaymentChoice("محفظة وصلها", "جاهزة للتفعيل لاحقًا", "👛", selected == "محفظة وصلها") { selected = "محفظة وصلها" }
        Spacer(Modifier.height(12.dp))
        HintCard(if (selected == "نقداً") "الدفع النقدي هو المتاح حاليًا." else "المحفظة غير مفعلة بعد، وتم الاحتفاظ بالخيار للتطوير القادم.")
    }
}

@Composable
private fun PaymentChoice(title: String, subtitle: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(if (selected) FeatureSoft else Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(if (selected) 2.dp else 1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BoxIcon(emoji)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, color = FeatureInk)
                Text(subtitle, color = FeatureMuted, fontSize = 11.sp)
            }
            if (selected) Text("✓", color = FeatureGreen, fontSize = 24.sp, fontWeight = FontWeight.Black)
            else Icon(Icons.Default.CreditCard, null, tint = FeatureGreen)
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    FeatureScaffold("الإشعارات", onBack) {
        Text("آخر التنبيهات الخاصة برحلاتك", color = FeatureMuted, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        val entries = listOf(
            NotificationItem("تحديث الرحلة", "سنرسل لك حالة الرحلة أولاً بأول", "الآن", true),
            NotificationItem("عرض جديد", "قد تتوفر عروض على الرحلات القادمة", "أمس", false),
            NotificationItem("وصلها", "نتمنى لك رحلة سعيدة وآمنة", "هذا الأسبوع", false)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(entries) { item ->
                Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        BoxIcon("🔔")
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.title, fontWeight = FontWeight.Bold, color = FeatureInk)
                                Text(item.time, color = FeatureMuted, fontSize = 10.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(item.body, color = FeatureMuted, fontSize = 12.sp)
                        }
                        if (item.unread) Box(Modifier.size(8.dp).background(FeatureGreen, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
fun SupportScreen(onBack: () -> Unit) {
    var selected by remember { mutableStateOf<SupportTopic?>(null) }
    val topics = listOf(
        SupportTopic("كيف أطلب سيارة؟", "حدد موقع الانطلاق والوجهة ثم اختر نوع التكسي وأرسل الطلب."),
        SupportTopic("كيف ألغي الرحلة؟", "من شاشة الرحلة الحالية يمكنك إلغاء الطلب قبل بدء الرحلة."),
        SupportTopic("مشكلة في الدفع", "راجع طريقة الدفع ثم تواصل مع الدعم مع رقم الرحلة."),
        SupportTopic("الأمان", "استخدم زر الاتصال والدعم من صفحة الرحلة عند الحاجة.")
    )
    FeatureScaffold("المساعدة والدعم", onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(FeatureGreen), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        BoxIcon("💬")
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text("محتاج مساعدة؟", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text("اختر السؤال الأقرب لمشكلتك", color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            items(topics) { topic ->
                Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().clickable { selected = topic }) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HelpOutline, null, tint = FeatureGreen)
                        Spacer(Modifier.size(10.dp))
                        Text(topic.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = FeatureInk)
                        Icon(Icons.Default.ChevronLeft, null, tint = FeatureMuted)
                    }
                }
            }
        }
    }
    selected?.let { topic ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(topic.title, fontWeight = FontWeight.Black) },
            text = { Text(topic.body, color = FeatureMuted) },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("حسناً", color = FeatureGreen) } }
        )
    }
}

@Composable
fun RatingScreen(onBack: () -> Unit) {
    var rating by remember { mutableStateOf(0) }
    FeatureScaffold("تقييم الرحلة", onBack) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(24.dp))
            BoxIcon("⭐")
            Spacer(Modifier.height(14.dp))
            Text("كيف كانت رحلتك؟", fontSize = 24.sp, fontWeight = FontWeight.Black, color = FeatureInk)
            Text("قيّم تجربتك حتى نحسّن وصلها", color = FeatureMuted)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { value ->
                    Icon(Icons.Default.Star, "$value", tint = if (value <= rating) FeatureGreen else Color(0xFFD4DDD9), modifier = Modifier.size(36.dp).clickable { rating = value })
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(if (rating == 0) "اختر التقييم" else "تقييمك: $rating من 5", fontWeight = FontWeight.Bold, color = FeatureInk)
        }
    }
}

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    FeatureScaffold("الخصوصية والأمان", onBack) {
        InfoCard("حماية الحساب", "جلسة الدخول محفوظة داخل التطبيق.", Icons.Default.Security)
        Spacer(Modifier.height(9.dp))
        InfoCard("الموقع", "يستخدم لتحديد نقطة الانطلاق أثناء الحجز.", Icons.Default.LocationOn)
        Spacer(Modifier.height(9.dp))
        HintCard("لا تشارك بيانات الدخول أو رموز التحقق مع أي شخص.")
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    FeatureScaffold("عن وصلها", onBack) {
        Card(colors = CardDefaults.cardColors(FeatureGreen), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("وصلها", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("تطبيق تاكسي للركاب", color = Color.White.copy(alpha = .9f), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text("نسخة تجريبية", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        InfoCard("الخدمة", "رحلات تاكسي داخل سوريا في هذه المرحلة.", Icons.Default.Info)
    }
}

@Composable
private fun FeatureScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().background(FeatureSurface).padding(horizontal = 18.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع", color = FeatureGreen, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = FeatureInk)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun PlaceRow(place: SavedPlace) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BoxIcon(place.emoji)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(place.title, fontWeight = FontWeight.Bold, color = FeatureInk)
                Text(place.address, color = FeatureMuted, fontSize = 11.sp)
            }
            Icon(Icons.Default.LocationOn, null, tint = FeatureGreen)
        }
    }
}

@Composable
private fun InfoCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = FeatureGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = FeatureInk)
                Text(body, color = FeatureMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.NotificationsNone, null, tint = FeatureGreen)
            Spacer(Modifier.size(9.dp))
            Text(text, color = FeatureMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun BoxIcon(text: String) {
    Box(Modifier.size(44.dp).background(FeatureSurface, CircleShape), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 20.sp)
    }
}
