package com.waslha.captain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DG = Color(0xFF0B805E)
private val DI = Color(0xFF14211C)
private val DM = Color(0xFF6E7D76)
private val DB = Color(0xFFF4F7F6)
private val DW = Color.White
private val DR = Color(0xFFB42318)

class CaptainDocumentsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        setContent { MaterialTheme { CaptainDocumentsScreen({ finish() }) } }
    }
}

@Composable
private fun CaptainDocumentsScreen(onBack: () -> Unit) {
    var driver by remember { mutableStateOf<Driver?>(null) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { CaptainApiProvider.api.me() }.onSuccess { driver = it.data }
    }
    val docs = driver?.documents
    LazyColumn(Modifier.fillMaxSize().background(DB), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(DW, CircleShape), contentAlignment = Alignment.Center) { androidx.compose.material3.TextButton(onClick = onBack) { Text("‹", fontSize = 26.sp, color = DI) } }
                Column(Modifier.weight(1f).padding(start = 10.dp)) { Text("الملف والمركبة", color = DI, fontSize = 24.sp, fontWeight = FontWeight.Black); Text("حالة مستندات اعتماد الكابتن", color = DM, fontSize = 10.sp) }
            }
        }
        item { DocumentCard("الهوية الشخصية", docs?.identity?.status ?: "pending", docs?.identity?.note) }
        item { DocumentCard("رخصة القيادة", docs?.license?.status ?: "pending", docs?.license?.note) }
        item { DocumentCard("أوراق المركبة", docs?.vehicle?.status ?: "pending", docs?.vehicle?.note) }
        item { Surface(Modifier.fillMaxWidth(), color = DW, shape = RoundedCornerShape(20.dp)) { Text("تظهر الحالة القادمة من حساب الكابتن في الخادم. لا يتم اعتبار المستند مقبولًا إلا عندما تعيده المنظومة بحالة accepted.", Modifier.padding(15.dp), color = DM, fontSize = 9.sp) } }
    }
}

@Composable
private fun DocumentCard(title: String, status: String, note: String?) {
    val accepted = status.equals("accepted", true) || status.equals("approved", true)
    val reviewing = status.equals("pending", true) || status.equals("reviewing", true)
    val label = when { accepted -> "مقبول"; reviewing -> "قيد المراجعة"; else -> "مرفوض / يحتاج إجراء" }
    val tint = when { accepted -> DG; reviewing -> Color(0xFFB54708); else -> DR }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(21.dp), colors = CardDefaults.cardColors(DW)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(Color(0xFFE8F5F0), CircleShape), contentAlignment = Alignment.Center) { Text(if (accepted) "✓" else "▣", color = DG, fontWeight = FontWeight.Black) }
            Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, color = DI, fontSize = 13.sp, fontWeight = FontWeight.Black); if (!note.isNullOrBlank()) Text(note, color = DM, fontSize = 9.sp) }
            Surface(color = tint.copy(alpha = .11f), shape = RoundedCornerShape(999.dp)) { Text(label, Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = tint, fontSize = 9.sp, fontWeight = FontWeight.Black) }
        }
    }
}
