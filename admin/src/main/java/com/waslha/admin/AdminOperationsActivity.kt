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
import androidx.compose.material3.FilterChip
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
    var period by remember { mutableStateOf("all") }
    var report by remember { mutableStateOf<AdminReportDto?>(null) }
    var tickets by remember { mutableStateOf<List<AdminSupportTicketDto>>(emptyList()) }
    var recipient by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var ticketFilter by remember { mutableStateOf("all") }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            message = null
            runCatching { AdminApiProvider.api.report(period) }
                .onSuccess { if (it.success) report = it.data else message = it.message }
                .onFailure { message = it.message ?: "تعذر تحميل التقرير" }
            runCatching { AdminApiProvider.api.supportTickets() }
                .onSuccess { if (it.success) tickets = it.data.orEmpty() }
            loading = false
        }
    }

    LaunchedEffect(period) { reload() }

    val visibleTickets = tickets.filter { ticketFilter == "all" || it.status.equals(ticketFilter, true) }

    MaterialTheme {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("عمليات الإدارة", fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("التقارير والتنبيهات والدعم", color = Color(0xFF6E7D76), fontSize = 10.sp)
                    }
                    TextButton(onClick = onBack) { Text("رجوع") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "الكل", "today" to "اليوم", "week" to "7 أيام", "month" to "الشهر").forEach { (value, label) ->
                        FilterChip(selected = period == value, onClick = { period = value }, label = { Text(label, fontSize = 10.sp) })
                    }
                }
            }
            item { Text("التقارير والإيرادات", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            report?.let { r ->
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("كل الرحلات: ${r.totalTrips}")
                            Text("المكتملة: ${r.completedTrips}")
                            Text("الملغاة: ${r.cancelledTrips}")
                            Text("الجارية: ${r.activeTrips}")
                            Text("الإيراد: ${r.totalRevenue} ل.س", fontWeight = FontWeight.Bold)
                            Text("متوسط الرحلة: ${"%.0f".format(r.averageFare)} ل.س")
                            Text("الزبائن: ${r.totalCustomers} • الكباتن: ${r.totalDrivers} • متصلون: ${r.onlineDrivers}")
                        }
                    }
                }
            }
            item { Text("إرسال إشعار", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            item {
                OutlinedTextField(recipient, { recipient = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("معرّف الزبون (اختياري)") })
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true, label = { Text("العنوان") })
                OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 3, label = { Text("نص الإشعار") })
                Button(onClick = {
                    scope.launch {
                        if (title.isBlank() || body.isBlank()) { message = "أدخل العنوان والنص"; return@launch }
                        val target = recipient.trim().takeIf { it.isNotBlank() }
                        val result = runCatching { AdminApiProvider.api.notify(AdminNotificationRequest(userId = target, title = title.trim(), body = body.trim())) }.getOrNull()
                        message = if (result?.success == true) {
                            title = ""
                            body = ""
                            if (target == null) "تم إرسال الإشعار إلى ${result.data?.sent ?: 0} زبون" else "تم إرسال الإشعار إلى الزبون"
                        } else result?.message ?: "تعذر إرسال الإشعار"
                    }
                }, Modifier.padding(top = 8.dp)) { Text(if (recipient.isBlank()) "إرسال للجميع" else "إرسال للزبون") }
            }
            item { Text("تذاكر الدعم", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "الكل", "open" to "مفتوحة", "pending" to "قيد المتابعة", "resolved" to "محلولة").forEach { (value, label) ->
                        FilterChip(selected = ticketFilter == value, onClick = { ticketFilter = value }, label = { Text(label, fontSize = 9.sp) })
                    }
                }
            }
            if (visibleTickets.isEmpty() && !loading) item { Text("لا توجد تذاكر بهذه الحالة", color = Color(0xFF6E7D76), fontSize = 11.sp) }
            items(visibleTickets, key = { it.id }) { ticket ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(Color.White)) {
                    Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(ticket.subject.ifBlank { "تذكرة دعم" }, fontWeight = FontWeight.Black)
                        Text(ticket.message, color = Color(0xFF6E7D76), fontSize = 10.sp)
                        Text("الزبون: ${ticket.userId.ifBlank { "غير معروف" }}", fontSize = 9.sp)
                        Text("الحالة: ${ticket.status}", fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!ticket.status.equals("resolved", true)) {
                                Button(onClick = { scope.launch { AdminApiProvider.api.updateSupportTicket(ticket.id, AdminTicketStatusRequest("resolved")); reload() } }) { Text("حل") }
                            }
                            if (!ticket.status.equals("pending", true)) {
                                Button(onClick = { scope.launch { AdminApiProvider.api.updateSupportTicket(ticket.id, AdminTicketStatusRequest("pending")); reload() } }) { Text("قيد المتابعة") }
                            }
                        }
                    }
                }
            }
            message?.let { item { Text(it, color = Color(0xFF0B805E), fontSize = 11.sp) } }
            if (loading) item { CircularProgressIndicator() }
            item { Button(onClick = { reload() }, Modifier.fillMaxWidth()) { Text("تحديث البيانات") } }
        }
    }
}
