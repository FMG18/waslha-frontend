package com.waslha.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FeatureGreen = Color(0xFF078A60)
private val FeatureInk = Color(0xFF10201B)
private val FeatureMuted = Color(0xFF6D7B76)
private val FeatureSurface = Color(0xFFF3F7F5)
private val FeatureSoft = Color(0xFFE8F6F0)
private val FeatureDanger = Color(0xFFB42318)

data class SavedPlace(val id: String, val title: String, val address: String)
data class NotificationItem(val title: String, val body: String, val time: String, val unread: Boolean)
data class SupportTopic(val title: String, val body: String)
data class PlaceEditor(val id: String?, val title: String, val address: String)

@Composable
fun SavedPlacesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waslha_places", Context.MODE_PRIVATE) }
    var places by remember { mutableStateOf(loadPlaces(prefs)) }
    var editor by remember { mutableStateOf<PlaceEditor?>(null) }

    FeatureScaffold("الأماكن المحفوظة", onBack) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("اختصاراتك اليومية", fontSize = 20.sp, fontWeight = FontWeight.Black, color = FeatureInk)
                Text("احفظ المنزل والعمل وأي مكان تستخدمه كثيرًا.", color = FeatureMuted, fontSize = 11.sp)
            }
            IconButton(onClick = { editor = PlaceEditor(null, "", "") }) { Icon(Icons.Default.Add, "إضافة", tint = FeatureGreen) }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(places, key = { it.id }) { place ->
                Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(46.dp).background(FeatureSoft, CircleShape), Alignment.Center) { Icon(Icons.Default.LocationOn, null, tint = FeatureGreen) }
                        Spacer(Modifier.size(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(place.title, fontWeight = FontWeight.Black, color = FeatureInk)
                            Text(place.address, color = FeatureMuted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { editor = PlaceEditor(place.id, place.title, place.address) }) { Icon(Icons.Default.Edit, "تعديل", tint = FeatureGreen) }
                        IconButton(onClick = { places = places.filterNot { it.id == place.id }; savePlaces(prefs, places) }) { Icon(Icons.Default.Delete, "حذف", tint = FeatureDanger) }
                    }
                }
            }
            if (places.isEmpty()) item { EmptyCard("لا توجد أماكن محفوظة بعد", "أضف منزلك أو عملك لتصل إليه بسرعة.") }
        }
    }

    editor?.let { current ->
        PlaceDialog(current, onDismiss = { editor = null }) { title, address ->
            places = if (current.id == null) {
                places + SavedPlace("place_${System.currentTimeMillis()}", title, address)
            } else {
                places.map { if (it.id == current.id) it.copy(title = title, address = address) else it }
            }
            savePlaces(prefs, places)
            editor = null
        }
    }
}

@Composable
fun PaymentsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waslha_preferences", Context.MODE_PRIVATE) }
    var selected by remember { mutableStateOf(prefs.getString("payment_method", "نقداً") ?: "نقداً") }
    var showInfo by remember { mutableStateOf(false) }

    FeatureScaffold("طرق الدفع", onBack) {
        Text("طريقة الدفع المفضلة", fontSize = 20.sp, fontWeight = FontWeight.Black, color = FeatureInk)
        Text("اختيارك يحفظ داخل التطبيق ويستخدم افتراضيًا في الطلب القادم.", color = FeatureMuted, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        PaymentChoice("نقداً", "الدفع للكابتن بعد الوصول", "💵", selected == "نقداً") {
            selected = "نقداً"
            prefs.edit().putString("payment_method", selected).apply()
        }
        Spacer(Modifier.height(9.dp))
        PaymentChoice("محفظة وصلها", "غير متاحة حاليًا", "👛", false) { showInfo = true }
        Spacer(Modifier.height(14.dp))
        HintCard("الدفع النقدي هو الخيار المتاح حاليًا في تطبيق الزبون.")
    }

    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        title = { Text("محفظة وصلها", fontWeight = FontWeight.Black) },
        text = { Text("المحفظة لم تُفعّل بعد. لن يتم تغيير طريقة الدفع الحالية.", color = FeatureMuted) },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text("حسنًا", color = FeatureGreen) } }
    )
}

@Composable
private fun PaymentChoice(title: String, subtitle: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(if (selected) FeatureSoft else Color.White), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            BoxIcon(emoji)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, color = FeatureInk)
                Text(subtitle, color = FeatureMuted, fontSize = 11.sp)
            }
            if (selected) Text("✓", color = FeatureGreen, fontSize = 24.sp, fontWeight = FontWeight.Black) else Icon(Icons.Default.CreditCard, null, tint = FeatureMuted)
        }
    }
}

@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waslha_notifications", Context.MODE_PRIVATE) }
    var readAll by remember { mutableStateOf(prefs.getBoolean("read", false)) }
    val entries = listOf(
        NotificationItem("حالة الرحلة", "سنحدّثك بحالة الطلب والكابتن عند توفره.", "الآن", true),
        NotificationItem("الأمان أولًا", "لا تشارك رموز التحقق أو بيانات حسابك.", "اليوم", false),
        NotificationItem("وصلها", "نتمنى لك رحلة سعيدة وآمنة.", "هذا الأسبوع", false)
    )
    FeatureScaffold("الإشعارات", onBack) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("تنبيهاتك", fontSize = 20.sp, fontWeight = FontWeight.Black, color = FeatureInk)
                Text(if (readAll) "تم تعليم الكل كمقروء" else "لديك إشعارات جديدة", color = FeatureMuted, fontSize = 11.sp)
            }
            if (!readAll) TextButton(onClick = { readAll = true; prefs.edit().putBoolean("read", true).apply() }) {
                Icon(Icons.Default.MarkEmailRead, null, tint = FeatureGreen, modifier = Modifier.size(19.dp))
                Spacer(Modifier.size(4.dp))
                Text("قراءة الكل", color = FeatureGreen, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
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
                        if (item.unread && !readAll) Box(Modifier.size(8.dp).background(FeatureGreen, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<SupportTopic?>(null) }
    val topics = listOf(
        SupportTopic("كيف أطلب سيارة؟", "حدد موقع الانطلاق والوجهة ثم راجع بيانات الرحلة وأرسل الطلب."),
        SupportTopic("كيف ألغي الرحلة؟", "من شاشة الرحلة الحالية اضغط إلغاء ثم أكد العملية."),
        SupportTopic("مشكلة في الدفع", "الدفع النقدي متاح حاليًا. احتفظ برقم الرحلة عند التواصل مع الدعم."),
        SupportTopic("الأمان", "لا تشارك رمز التحقق أو معلومات حسابك مع أي شخص.")
    )
    FeatureScaffold("المساعدة والدعم", onBack) {
        Card(colors = CardDefaults.cardColors(FeatureGreen), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).background(Color.White.copy(alpha = .16f), CircleShape), Alignment.Center) { Icon(Icons.Default.HelpOutline, null, tint = Color.White) }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("نحن معك", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("اختر السؤال أو تواصل معنا مباشرة.", color = Color.White.copy(alpha = .86f), fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
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
            item {
                Button(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:support@waslha.app") }) } }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = FeatureGreen)) {
                    Icon(Icons.Default.MarkEmailRead, null)
                    Spacer(Modifier.size(7.dp))
                    Text("راسل الدعم", fontWeight = FontWeight.Black)
                }
            }
        }
    }
    selected?.let { topic -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(topic.title, fontWeight = FontWeight.Black) }, text = { Text(topic.body, color = FeatureMuted) }, confirmButton = { TextButton(onClick = { selected = null }) { Text("حسنًا", color = FeatureGreen) } }) }
}

@Composable
fun RatingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waslha_rating", Context.MODE_PRIVATE) }
    var rating by remember { mutableStateOf(prefs.getInt("rating", 0)) }
    var comment by remember { mutableStateOf(prefs.getString("comment", "") ?: "") }
    var saved by remember { mutableStateOf(rating > 0) }
    FeatureScaffold("تقييم الرحلة", onBack) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(16.dp))
            Box(Modifier.size(64.dp).background(FeatureSoft, CircleShape), Alignment.Center) { Icon(Icons.Default.Star, null, tint = FeatureGreen, modifier = Modifier.size(34.dp)) }
            Spacer(Modifier.height(12.dp))
            Text("كيف كانت رحلتك؟", fontSize = 24.sp, fontWeight = FontWeight.Black, color = FeatureInk)
            Text("رأيك يساعدنا على تحسين تجربة الركاب.", color = FeatureMuted, fontSize = 11.sp)
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { (1..5).forEach { value -> Icon(Icons.Default.Star, "$value", tint = if (value <= rating) FeatureGreen else Color(0xFFD4DDD9), modifier = Modifier.size(37.dp).clickable { rating = value; saved = false }) } }
            Spacer(Modifier.height(18.dp))
            OutlinedTextField(value = comment, onValueChange = { comment = it }, modifier = Modifier.fillMaxWidth(), label = { Text("ملاحظتك (اختياري)") }, minLines = 3, maxLines = 4)
            Spacer(Modifier.height(12.dp))
            Button(enabled = rating > 0, onClick = { prefs.edit().putInt("rating", rating).putString("comment", comment.trim()).apply(); saved = true }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = FeatureGreen)) { Text(if (saved) "تم حفظ التقييم" else "حفظ التقييم", fontWeight = FontWeight.Black) }
        }
    }
}

@Composable
fun SecurityScreen(onBack: () -> Unit) {
    FeatureScaffold("الخصوصية والأمان", onBack) {
        InfoCard("حماية الحساب", "استخدم تسجيل الدخول الرسمي ولا تشارك رموز التحقق.", Icons.Default.Security)
        Spacer(Modifier.height(9.dp))
        InfoCard("الموقع", "يستخدم عند الحجز لتحديد نقطة الانطلاق وتحسين دقة الخدمة.", Icons.Default.LocationOn)
        Spacer(Modifier.height(9.dp))
        InfoCard("البيانات المحلية", "بعض التفضيلات تحفظ على جهازك مثل طريقة الدفع والإشعارات.", Icons.Default.CreditCard)
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    FeatureScaffold("عن وصلها", onBack) {
        Card(colors = CardDefaults.cardColors(FeatureGreen), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Text("وصلها", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text("تطبيق تاكسي للركاب", color = Color.White.copy(alpha = .9f), fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Text("تجربة مخصصة للزبون لحجز الرحلات وإدارة الحساب.", color = Color.White.copy(alpha = .92f), fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        InfoCard("التطبيق", "هذه النسخة مخصصة للزبائن فقط. الإدارة والكابتن تطبيقات منفصلة.", Icons.Default.Info)
    }
}

@Composable
private fun FeatureScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(FeatureSurface).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("رجوع", color = FeatureGreen, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.weight(1f))
            Text(title, fontSize = 24.sp, fontWeight = FontWeight.Black, color = FeatureInk)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SavedPlaceCard(place: SavedPlace, onEdit: () -> Unit, onDelete: () -> Unit) = Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.LocationOn, null, tint = FeatureGreen)
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) { Text(place.title, fontWeight = FontWeight.Bold, color = FeatureInk); Text(place.address, color = FeatureMuted, fontSize = 11.sp) }
        IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "تعديل", tint = FeatureGreen) }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف", tint = FeatureDanger) }
    }
}

@Composable
private fun PlaceDialog(editor: PlaceEditor, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf(editor.title) }
    var address by remember { mutableStateOf(editor.address) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.id == null) "إضافة مكان" else "تعديل المكان", fontWeight = FontWeight.Black) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("اسم المكان") }, singleLine = true)
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("وصف الموقع") }, singleLine = true)
        } },
        confirmButton = { TextButton(enabled = title.isNotBlank() && address.isNotBlank(), onClick = { onSave(title.trim(), address.trim()) }) { Text("حفظ", color = FeatureGreen, fontWeight = FontWeight.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = FeatureMuted) } }
    )
}

@Composable
private fun InfoCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(19.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(FeatureSoft, CircleShape), Alignment.Center) { Icon(icon, null, tint = FeatureGreen, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, color = FeatureInk); Text(body, color = FeatureMuted, fontSize = 11.sp) }
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.NotificationsNone, null, tint = FeatureGreen); Spacer(Modifier.size(9.dp)); Text(text, color = FeatureMuted, fontSize = 10.sp) }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(title, fontWeight = FontWeight.Black, color = FeatureInk); Spacer(Modifier.height(4.dp)); Text(subtitle, color = FeatureMuted, fontSize = 11.sp) }
    }
}

@Composable
private fun BoxIcon(text: String) {
    Box(Modifier.size(44.dp).background(FeatureSurface, CircleShape), contentAlignment = Alignment.Center) { Text(text, fontSize = 20.sp) }
}

private fun loadPlaces(prefs: android.content.SharedPreferences): List<SavedPlace> {
    val raw = prefs.getString("items", "") ?: ""
    if (raw.isBlank()) return emptyList()
    return raw.split("||").mapNotNull { row ->
        val parts = row.split("|", limit = 3)
        if (parts.size == 3) SavedPlace(parts[0], parts[1], parts[2]) else null
    }
}

private fun savePlaces(prefs: android.content.SharedPreferences, places: List<SavedPlace>) {
    val raw = places.joinToString("||") { "${it.id}|${it.title}|${it.address}" }
    prefs.edit().putString("items", raw).apply()
}
