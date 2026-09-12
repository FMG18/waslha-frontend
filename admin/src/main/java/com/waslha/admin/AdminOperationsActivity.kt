package com.waslha.admin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class AdminOperationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        setContent { AdminOperationsScreen { finish() } }
    }
}

@Composable
private fun AdminOperationsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<AdminReportDto?>(null) }
    var tickets by remember { mutableStateOf<List<AdminSupportTicketDto>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            message = null
            runCatching { AdminApiProvider.api.report() }.onSuccess { if (it.success) report = it.data }
                .onFailure { message = it.message ?: "تعذر تحميل التقرير" }
            runCatching { AdminApiProvider.api.supportTickets() }.onSuccess { if (it.success) tickets = it.data.orEmpty() }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    MaterialTheme {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("عمليات الإدارة", fontSize = 28.sp, fontWeight = FontWeight.Black); TextButton(onClick = onBack) { Text("رجوع") } } }
            item { Text("التقارير والإيرادات", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            report?.let { r ->
                item { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text("كل الرحلات: ${r.totalTrips}"); Text("المكتملة: ${r.completedTrips}"); Text("الملغاة: ${r.cancelledTrips}"); Text("الجارية: ${r.activeTrips}"); Text("الإيراد: ${r.totalRevenue} ل.س"); Text("متوسط الرحلة: ${"%.0f".format(r.averageFare)} ل.س"); Text("الزبائن: ${r.totalCustomers}"); Text("الكباتن: ${r.totalDrivers} • متصلون: ${r.onlineDrivers}") } } }
            }
            item { Text("إرسال إشعار", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            item {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("العنوان") })
                OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3, label = { Text("نص الإشعار") })
                Button(onClick = {
                    scope.launch {
                        if (title.isBlank() || body.isBlank()) { message = "أدخل العنوان والنص"; return@launch }
                        val result = runCatching { AdminApiProvider.api.notify(AdminNotificationRequest(title = title.trim(), body = body.trim())) }.getOrNull()
                        message = if (result?.success == true) "تم إرسال الإشعار إلى ${result.data?.sent ?: 0} زبون" else result?.message ?: "تعذر إرسال الإشعار"
                    }
                }, Modifier.padding(top = 8.dp)) { Text("إرسال للجميع") }
            }
            item { Text("تذاكر الدعم", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            items(tickets, key = { it.id }) { ticket ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(ticket.subject, fontWeight = FontWeight.Black)
                        Text(ticket.message, color = Color(0xFF6E7D76), fontSize = 10.sp)
                        Text("الحالة: ${ticket.status}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { scope.launch { AdminApiProvider.api.updateSupportTicket(ticket.id, AdminTicketStatusRequest("resolved")); reload() } }) { Text("حل") }
                            Button(onClick = { scope.launch { AdminApiProvider.api.updateSupportTicket(ticket.id, AdminTicketStatusRequest("pending")); reload() } }) { Text("قيد المتابعة") }
                        }
                    }
                }
            }
            message?.let { item { Text(it, color = Color(0xFF0B805E), fontSize = 11.sp) } }
            if (loading) item { CircularProgressIndicator() }
            item { Button(onClick = { reload() }) { Text("تحديث") } }
        }
    }
}
