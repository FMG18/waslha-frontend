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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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

class AdminDriverDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        val id = intent.getStringExtra(EXTRA_DRIVER_ID).orEmpty()
        setContent { AdminDriverDetailsScreen(id) { finish() } }
    }
    companion object { const val EXTRA_DRIVER_ID = "driver_id" }
}

@Composable
private fun AdminDriverDetailsScreen(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var driver by remember { mutableStateOf<AdminDriverDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            message = null
            runCatching { AdminApiProvider.api.driver(id) }
                .onSuccess { if (it.success) driver = it.data else message = it.message }
                .onFailure { message = it.message ?: "تعذر الاتصال بالخادم" }
            loading = false
        }
    }

    LaunchedEffect(id) { reload() }

    MaterialTheme {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { OutlinedButton(onClick = onBack) { Text("رجوع") } }
            item { Text("تفاصيل الكابتن", fontSize = 28.sp, fontWeight = FontWeight.Black) }
            if (loading && driver == null) item { CircularProgressIndicator() }
            message?.let { item { Text(it, color = Color(0xFFB42318), fontSize = 11.sp) } }
            driver?.let { d ->
                item {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(d.name.ifBlank { d.id }, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("المعرّف: ${d.id}")
                        Text("الهاتف: ${d.phone ?: "غير متوفر"}")
                        Text("المركبة: ${d.vehicle.ifBlank { "غير محددة" }}")
                        Text("اللوحة: ${d.plate.ifBlank { "غير محددة" }}")
                        Text("النوع: ${d.type}")
                        Text("التقييم: ${d.rating}")
                        Text("الحالة: ${if (d.available) "متصل" else "غير متصل"}", color = if (d.available) Color(0xFF0B805E) else Color(0xFF6E7D76))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = {
                            scope.launch {
                                loading = true
                                val result = runCatching { AdminApiProvider.api.setDriverAvailability(d.id, AdminAvailabilityRequest(!d.available)) }.getOrNull()
                                if (result?.success == true) reload() else message = result?.message ?: "تعذر تغيير الحالة"
                                loading = false
                            }
                        }) { Text(if (d.available) "جعل الكابتن غير متصل" else "جعل الكابتن متصل") }
                        OutlinedButton(onClick = { reload() }) { Text("تحديث") }
                    }
                }
            }
        }
    }
}
