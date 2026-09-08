package com.waslha.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
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

@Composable
fun SavedPlacesScreen(onBack: () -> Unit) {
    FeatureScaffold("الأماكن المحفوظة", onBack) {
        val places = listOf(
            SavedPlace("المنزل", "موقعي المحفوظ", "🏠"),
            SavedPlace("العمل", "موقعي المحفوظ", "💼"),
            SavedPlace("المفضلة", "مكان محفوظ", "⭐")
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(places) { PlaceRow(it) }
        }
    }
}

@Composable
fun PaymentsScreen(onBack: () -> Unit) {
    FeatureScaffold("طرق الدفع", onBack) {
        val methods = listOf(
            PaymentMethod("نقداً", "الدفع للكابتن بعد الوصول", "💵"),
            PaymentMethod("محفظة وصلها", "رصيد الحساب", "👛")
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(methods) { method ->
                Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        BoxIcon(method.emoji)
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(method.title, fontWeight = FontWeight.Bold, color = FeatureInk)
                            Text(method.subtitle, color = FeatureMuted, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.CreditCard, null, tint = FeatureGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    FeatureScaffold("الإشعارات", onBack) {
        val items = listOf(
            NotificationItem("تحديث الرحلة", "سنرسل لك حالة الكابتن أولاً بأول", "الآن", true),
            NotificationItem("عرض جديد", "خصم على رحلتك القادمة", "أمس", false),
            NotificationItem("وصلها", "نتمنى لك رحلة سعيدة", "هذا الأسبوع", false)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(items) { item ->
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
                Card(
                    colors = CardDefaults.cardColors(Color.White),
                    shape = RoundedCornerShape(17.dp),
                    modifier = Modifier.fillMaxWidth().clickable { selected = topic }
                ) {
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
            BoxIcon("🚕")
            Spacer(Modifier.height(14.dp))
            Text("كيف كانت رحلتك؟", fontSize = 24.sp, fontWeight = FontWeight.Black, color = FeatureInk)
            Text("قيّم تجربتك حتى نحسّن وصلها", color = FeatureMuted)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { value ->
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "$value",
                        tint = if (value <= rating) FeatureGreen else Color(0xFFD4DDD9),
                        modifier = Modifier.size(36.dp).clickable { rating = value }
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            Text(if (rating == 0) "اختر التقييم" else "تقييمك: $rating من 5", fontWeight = FontWeight.Bold, color = FeatureInk)
        }
    }
}

@Composable
private fun FeatureScaffold(title: String, onBack: () -> Unit, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
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
private fun BoxIcon(text: String) {
    Row(Modifier.size(44.dp).background(FeatureSurface, CircleShape), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(text, fontSize = 20.sp)
    }
}
