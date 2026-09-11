package com.waslha.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CurrentAboutScreen(onBack: () -> Unit) {
    val green = Color(0xFF087F5B)
    val greenDark = Color(0xFF055C42)
    val ink = Color(0xFF10201B)
    val muted = Color(0xFF6D7B76)
    val surface = Color(0xFFF7F9F8)
    val soft = Color(0xFFE7F6F0)
    val line = Color(0xFFDDE5E1)

    Column(
        modifier = Modifier
            .background(surface)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text("رجوع", color = green, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("عن وصلها", fontSize = 23.sp, fontWeight = FontWeight.Black, color = ink)
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = green),
            shape = RoundedCornerShape(26.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.size(52.dp).clip(CircleShape).background(greenDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Info, null, tint = Color.White, modifier = Modifier.size(25.dp))
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text("وصلها", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("تطبيق التاكسي للزبائن", color = Color.White.copy(alpha = .88f), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "تجربة مخصصة للزبون لحجز الرحلات ومتابعتها وإدارة الحساب.",
                    color = Color.White.copy(alpha = .92f),
                    fontSize = 11.sp,
                    lineHeight = 18.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, line),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("إصدار التطبيق", color = muted, fontSize = 10.sp)
                Text("v${BuildConfig.VERSION_NAME}", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("هذا الرقم مطابق للإصدار المثبت من Android.", color = muted, fontSize = 10.sp)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = soft),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, line),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Text(
                "هذا التطبيق مخصص للزبائن فقط. تطبيق الكابتن والإدارة منفصلان.",
                color = ink,
                fontSize = 11.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
