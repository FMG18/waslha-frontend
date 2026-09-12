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

class AdminUserDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminApiProvider.init(this)
        val id = intent.getStringExtra(EXTRA_USER_ID).orEmpty()
        setContent { AdminUserDetailsScreen(id) { finish() } }
    }
    companion object { const val EXTRA_USER_ID = "user_id" }
}

@Composable
private fun AdminUserDetailsScreen(id: String, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<AdminUserDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            message = null
            runCatching { AdminApiProvider.api.user(id) }
                .onSuccess { if (it.success) user = it.data else message = it.message }
                .onFailure { message = it.message ?: "تعذر الاتصال بالخادم" }
            loading = false
        }
    }
    LaunchedEffect(id) { reload() }

    MaterialTheme {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { OutlinedButton(onClick = onBack) { Text("رجوع") } }
            item { Text("تفاصيل الزبون", fontSize = 28.sp, fontWeight = FontWeight.Black) }
            if (loading && user == null) item { CircularProgressIndicator() }
            message?.let { item { Text(it, color = Color(0xFFB42318), fontSize = 11.sp) } }
            user?.let { u ->
                item {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(u.name.ifBlank { "زبون ${u.id.takeLast(6)}" }, fontSize = 20.sp, fontWeight = FontWeight.Black)
                        Text("المعرّف: ${u.id}")
                        Text("الهاتف: ${u.phone.ifBlank { "غير متوفر" }}")
                        Text("البريد: ${u.email.ifBlank { "غير متوفر" }}")
                        Text("عدد الرحلات: ${u.tripsCount}")
                    }
                }
                item { Button(onClick = { reload() }) { Text("تحديث") } }
            }
        }
    }
}
