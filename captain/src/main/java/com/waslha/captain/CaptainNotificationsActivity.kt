package com.waslha.captain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

private val NG = Color(0xFF0B805E)
private val ND = Color(0xFF075B43)
private val NM = Color(0xFFE8F5F0)
private val NB = Color(0xFFF4F7F6)
private val NI = Color(0xFF14211C)
private val NX = Color(0xFF6E7D76)
private val NW = Color.White

class CaptainNotificationsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize(), color = NB) { NotificationsScreen { finish() } } } }
    }
}

@Composable
private fun NotificationsScreen(onBack: () -> Unit) {
    var notifications by remember { mutableStateOf<List<CaptainNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    suspend fun refresh() {
        runCatching { CaptainApiProvider.api.me() }
            .onSuccess { me ->
                val id = me.data?.id.orEmpty()
                if (id.isBlank()) {
                    error = me.message ?: "تعذر تحديد حساب الكابتن"
                } else {
                    runCatching { CaptainApiProvider.api.notifications(id, 50) }
                        .onSuccess { response -> notifications = response.data.orEmpty(); error = null }
                        .onFailure { error = it.message ?: "تعذر تحميل الإشعارات" }
                }
            }
            .onFailure { error = it.message ?: "تعذر تحميل الحساب" }
        loading = false
    }

    LaunchedEffect(Unit) {
        refresh()
        while (true) {
            delay(15000)
            refresh()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).background(NW, CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
                    Text("‹", color = NI, fontSize = 27.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("الإشعارات", color = NI, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("آخر تنبيهات الرحلات والحساب", color = NX, fontSize = 10.sp)
                }
                val unread = notifications.count { !it.read }
                if (unread > 0) {
                    Surface(color = NM, shape = RoundedCornerShape(999.dp)) {
                        Text("$unread جديد", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = ND, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
        if (loading && notifications.isEmpty()) {
            item { Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = NG) } }
        } else if (error != null) {
            item { Surface(Modifier.fillMaxWidth(), color = Color(0xFFFFE9E7), shape = RoundedCornerShape(15.dp)) { Text(error ?: "", Modifier.padding(13.dp), color = Color(0xFFB42318), fontSize = 10.sp) } }
        } else if (notifications.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(23.dp), colors = CardDefaults.cardColors(NW)) {
                    Column(Modifier.fillMaxWidth().padding(25.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(52.dp).background(NM, CircleShape), contentAlignment = Alignment.Center) { Text("•", color = NG, fontSize = 22.sp) }
                        Text("ماكو إشعارات حالياً", color = NI, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("راح تظهر هنا تنبيهات الرحلات والحساب.", color = NX, fontSize = 10.sp)
                    }
                }
            }
        } else {
            items(notifications, key = { it.id }) { notification ->
                NotificationCard(notification)
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: CaptainNotification) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(21.dp),
        colors = CardDefaults.cardColors(NW)
    ) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(42.dp).background(if (notification.read) NB else NM, CircleShape), contentAlignment = Alignment.Center) {
                Text("!", color = if (notification.read) NX else NG, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(notification.title, color = NI, fontSize = 12.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                    if (!notification.read) Box(Modifier.size(7.dp).background(NG, CircleShape))
                }
                Text(notification.body, color = NX, fontSize = 10.sp)
            }
        }
    }
}
