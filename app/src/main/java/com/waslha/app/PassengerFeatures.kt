package com.waslha.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val FeatureGreen = Color(0xFF087F5B)
private val FeatureGreenDark = Color(0xFF055C42)
private val FeatureInk = Color(0xFF10201B)
private val FeatureMuted = Color(0xFF6D7B76)
private val FeatureSurface = Color(0xFFF7F9F8)
private val FeatureSoft = Color(0xFFE7F6F0)
private val FeatureLine = Color(0xFFDDE5E1)
private val FeatureDanger = Color(0xFFB42318)

private val CardShape = RoundedCornerShape(20.dp)


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
            IconButton(
                onClick = { editor = PlaceEditor(null, "", "") },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(FeatureGreen)
            ) {
                Icon(Icons.Default.Add, "إضافة", tint = Color.White)
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(places, key = { it.id }) { place ->
                SavedPlaceCard(
                    place,
                    onEdit = { editor = PlaceEditor(place.id, place.title, place.address) },
                    onDelete = {
                        places = places.filterNot { it.id == place.id }
                        savePlaces(prefs, places)
                    }
                )
            }
            if (places.isEmpty()) item {
                EmptyCard(
                    icon = Icons.Default.LocationOn,
                    title = "لا توجد أماكن محفوظة بعد",
                    subtitle = "أضف منزلك أو عملك لتصل إليه بسرعة."
                )
            }
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
        PaymentChoice("نقداً", "الدفع للكابتن بعد الوصول", Icons.Default.CreditCard, selected == "نقداً") {
            selected = "نقداً"
            prefs.edit().putString("payment_method", selected).apply()
        }
        Spacer(Modifier.height(9.dp))
        PaymentChoice("محفظة وصلها", "غير متاحة حاليًا", Icons.Default.CreditCard, false) { showInfo = true }
        Spacer(Modifier.height(14.dp))
        HintCard(Icons.Default.Info, "الدفع النقدي هو الخيار المتاح حاليًا في تطبيق الزبون.")
    }

    if (showInfo) AlertDialog(
        onDismissRequest = { showInfo = false },
        title = { Text("محفظة وصلها", fontWeight = FontWeight.Black) },
        text = { Text("المحفظة لم تُفعّل بعد. لن يتم تغيير طريقة الدفع الحالية.", color = FeatureMuted) },
        confirmButton = { TextButton(onClick = { showInfo = false }) { Text("حسنًا", color = FeatureGreen, fontWeight = FontWeight.Bold) } }
    )
}

@Composable
private fun PaymentChoice(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(if (selected) FeatureSoft else Color.White),
        shape = CardShape,
        border = BorderStroke(1.dp, if (selected) FeatureGreen else FeatureLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(if (selected) FeatureGreen else FeatureSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = if (selected) Color.White else FeatureGreen)
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, color = FeatureInk)
                Text(subtitle, color = FeatureMuted, fontSize = 11.sp)
            }
            if (selected) {
                Box(Modifier.size(25.dp).clip(CircleShape).background(FeatureGreen), contentAlignment = Alignment.Center) {
                    Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            } else {
                Text("قريبًا", color = FeatureMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
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
    val unreadCount = entries.count { it.unread && !readAll }

    FeatureScaffold("الإشعارات", onBack) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("تنبيهاتك", fontSize = 20.sp, fontWeight = FontWeight.Black, color = FeatureInk)
                Text(
                    when {
                        unreadCount > 0 -> "$unreadCount إشعار جديد"
                        else -> "كل الإشعارات مقروءة"
                    },
                    color = FeatureMuted,
                    fontSize = 11.sp
                )
            }
            if (!readAll) TextButton(onClick = { readAll = true; prefs.edit().putBoolean("read", true).apply() }) {
                Icon(Icons.Default.MarkEmailRead, null, tint = FeatureGreen, modifier = Modifier.size(19.dp))
                Spacer(Modifier.size(4.dp))
                Text("قراءة الكل", color = FeatureGreen, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 12.dp)
        ) {
            items(entries) { item ->
                NotificationCard(item, readAll)
            }
        }
    }
}

@Composable
private fun NotificationCard(item: NotificationItem, readAll: Boolean) {
    val active = item.unread && !readAll
    Card(
        colors = CardDefaults.cardColors(if (active) FeatureSoft.copy(alpha = .65f) else Color.White),
        shape = CardShape,
        border = BorderStroke(1.dp, if (active) FeatureGreen.copy(alpha = .28f) else FeatureLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(45.dp).clip(CircleShape).background(if (active) FeatureGreen else FeatureSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, null, tint = if (active) Color.White else FeatureGreen, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.size(11.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.title, fontWeight = FontWeight.Black, color = FeatureInk, fontSize = 13.sp)
                    Text(item.time, color = FeatureMuted, fontSize = 9.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(item.body, color = FeatureMuted, fontSize = 11.sp, lineHeight = 17.sp)
            }
            if (active) {
                Spacer(Modifier.size(7.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(FeatureGreen))
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
        Card(
            colors = CardDefaults.cardColors(FeatureGreen),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(50.dp).clip(CircleShape).background(FeatureGreenDark), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.HelpOutline, null, tint = Color.White, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("نحن معك", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("اختر السؤال أو تواصل معنا مباشرة.", color = Color.White.copy(alpha = .84f), fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 10.dp)
        ) {
            items(topics) { topic ->
                Card(
                    colors = CardDefaults.cardColors(Color.White),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, FeatureLine),
                    modifier = Modifier.fillMaxWidth().clickable { selected = topic },
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(FeatureSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.HelpOutline, null, tint = FeatureGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.size(10.dp))
                        Text(topic.title, Modifier.weight(1f), fontWeight = FontWeight.Bold, color = FeatureInk, fontSize = 13.sp)
                        Icon(Icons.Default.ChevronLeft, null, tint = FeatureMuted)
                    }
                }
            }
            item {
                Button(
                    onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("mailto:support@waslha.app") }) } },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(17.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FeatureGreen)
                ) {
                    Icon(Icons.Default.MarkEmailRead, null)
                    Spacer(Modifier.size(7.dp))
                    Text("راسل الدعم", fontWeight = FontWeight.Black)
                }
            }
        }
    }
    selected?.let { topic ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(topic.title, fontWeight = FontWeight.Black) },
            text = { Text(topic.body, color = FeatureMuted, lineHeight = 20.sp) },
            confirmButton = { TextButton(onClick = { selected = null }) { Text("حسنًا", color = FeatureGreen, fontWeight = FontWeight.Bold) } }
        )
    }
}

@Composable
fun RatingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("waslha_rating", Context.MODE_PRIVATE) }
    var rating by remember { mutableStateOf(prefs.getInt("rating", 0)) }
    var comment by remember { mutableStateOf(prefs.getString("comment", "") ?: "") }
    var saved by remember { mutableStateOf(rating > 0) }
    FeatureScaffold("تقييم الرحلة", onBack) {
        Card(
            colors = CardDefaults.cardColors(Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, FeatureLine),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Box(Modifier.size(64.dp).clip(CircleShape).background(FeatureSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Star, null, tint = FeatureGreen, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("كيف كانت رحلتك؟", fontSize = 24.sp, fontWeight = FontWeight.Black, color = FeatureInk)
                Text("رأيك يساعدنا على تحسين تجربة الركاب.", color = FeatureMuted, fontSize = 11.sp)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    (1..5).forEach { value ->
                        Icon(
                            Icons.Default.Star,
                            "$value",
                            tint = if (value <= rating) FeatureGreen else Color(0xFFD4DDD9),
                            modifier = Modifier.size(37.dp).clickable { rating = value; saved = false }
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it; saved = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ملاحظتك (اختياري)") },
                    minLines = 3,
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    enabled = rating > 0,
                    onClick = { prefs.edit().putInt("rating", rating).putString("comment", comment.trim()).apply(); saved = true },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FeatureGreen)
                ) {
                    Text(if (saved) "تم حفظ التقييم" else "حفظ التقييم", fontWeight = FontWeight.Black)
                }
            }
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
        Card(
            colors = CardDefaults.cardColors(FeatureGreen),
            shape = RoundedCornerShape(26.dp),
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Box(Modifier.size(50.dp).clip(CircleShape).background(FeatureGreenDark), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.height(12.dp))
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
    Column(
        Modifier
            .fillMaxSize()
            .background(FeatureSurface)
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("رجوع", color = FeatureGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(title, fontSize = 23.sp, fontWeight = FontWeight.Black, color = FeatureInk)
        }
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun SavedPlaceCard(place: SavedPlace, onEdit: () -> Unit, onDelete: () -> Unit) = Card(
    colors = CardDefaults.cardColors(Color.White),
    shape = CardShape,
    border = BorderStroke(1.dp, FeatureLine),
    elevation = CardDefaults.cardElevation(0.dp),
    modifier = Modifier.fillMaxWidth()
) {
    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(FeatureSoft), contentAlignment = Alignment.Center) {
            Icon(if (place.title.contains("منزل") || place.title.contains("بيت")) Icons.Default.Home else Icons.Default.LocationOn, null, tint = FeatureGreen)
        }
        Spacer(Modifier.size(11.dp))
        Column(Modifier.weight(1f)) {
            Text(place.title, fontWeight = FontWeight.Black, color = FeatureInk, fontSize = 13.sp)
            Text(place.address, color = FeatureMuted, fontSize = 10.sp)
        }
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, "تعديل", tint = FeatureGreen, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "حذف", tint = FeatureDanger, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PlaceDialog(editor: PlaceEditor, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf(editor.title) }
    var address by remember { mutableStateOf(editor.address) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.id == null) "إضافة مكان" else "تعديل المكان", fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("اسم المكان") }, singleLine = true, shape = RoundedCornerShape(15.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("وصف الموقع") }, singleLine = true, shape = RoundedCornerShape(15.dp))
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && address.isNotBlank(), onClick = { onSave(title.trim(), address.trim()) }) {
                Text("حفظ", color = FeatureGreen, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء", color = FeatureMuted) } }
    )
}

@Composable
private fun InfoCard(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(Color.White),
        shape = CardShape,
        border = BorderStroke(1.dp, FeatureLine),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(FeatureSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = FeatureGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, color = FeatureInk, fontSize = 13.sp)
                Text(body, color = FeatureMuted, fontSize = 10.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun HintCard(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Card(
        colors = CardDefaults.cardColors(FeatureSoft.copy(alpha = .7f)),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, FeatureLine),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = FeatureGreen, modifier = Modifier.size(19.dp))
            Spacer(Modifier.size(9.dp))
            Text(text, color = FeatureMuted, fontSize = 10.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun EmptyCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(Color.White),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, FeatureLine),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(FeatureSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = FeatureGreen, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Black, color = FeatureInk, fontSize = 15.sp)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = FeatureMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun BoxIcon(text: String) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(FeatureSurface), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 20.sp)
    }
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
