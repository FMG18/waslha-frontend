package com.waslha.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class AdminAuditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        setContent { AdminAuditScreen { finish() } }
    }
}

@Composable
private fun AdminAuditScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<AdminAuditLogDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching { AdminApiProvider.api.auditLogs() }
                .onSuccess { if (it.success) logs = it.data.orEmpty() else error = it.message ?: "تعذر تحميل السجل" }
                .onFailure { error = it.message ?: "تعذر الاتصال بالخادم" }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    MaterialTheme {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Column(Modifier.fillMaxWidth()) { OutlinedButton(onClick = onBack) { Text("رجوع") }; Text("سجل عمليات الإدارة", fontSize = 27.sp, fontWeight = FontWeight.Black); Text("آخر 100 عملية مسجلة", color = Color(0xFF6E7D76), fontSize = 10.sp) } }
            item { OutlinedButton(onClick = { reload() }) { Text("تحديث") } }
            error?.let { item { Text(it, color = Color(0xFFB42318), fontSize = 11.sp) } }
            if (loading) item { CircularProgressIndicator() }
            if (!loading && logs.isEmpty()) item { Text("لا توجد عمليات مسجلة بعد", color = Color(0xFF6E7D76)) }
            items(logs, key = { it.id }) { log ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(log.action.ifBlank { "عملية" }, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        if (log.target.isNotBlank()) Text("الهدف: ${log.target}", fontSize = 10.sp)
                        if (log.details.isNotBlank()) Text(log.details, color = Color(0xFF6E7D76), fontSize = 10.sp)
                        Text("المدير: ${log.adminId.ifBlank { "غير محدد" }}", fontSize = 9.sp)
                        Text("وقت العملية: ${log.createdAt}", fontSize = 9.sp, color = Color(0xFF6E7D76))
                    }
                }
            }
        }
    }
}
