package com.waslha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SafeGreen = Color(0xFF078A60)
private val SafeInk = Color(0xFF10201B)
private val SafeMuted = Color(0xFF72807B)
private val SafeBg = Color(0xFFF5F8F6)

class SafeTaxiActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionStore = SessionStore(this)
        setContent {
            SafeTaxiHome(
                sessionStore = sessionStore,
                onLogout = {
                    sessionStore.clear()
                    finish()
                }
            )
        }
    }
}

@Composable
private fun SafeTaxiHome(sessionStore: SessionStore, onLogout: () -> Unit) {
    var destination by remember { mutableStateOf<String?>(null) }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = SafeBg) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("وصلها", color = SafeGreen, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text(
                            sessionStore.name?.takeIf { it.isNotBlank() }?.let { "أهلاً $it" } ?: "أهلاً بك",
                            color = SafeMuted,
                            fontSize = 13.sp
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "تسجيل الخروج", tint = SafeInk)
                    }
                }

                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(Color.White),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DirectionsCar, null, tint = SafeGreen, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.size(10.dp))
                            Column {
                                Text("احجز تاكسي", fontSize = 21.sp, fontWeight = FontWeight.Black, color = SafeInk)
                                Text("اختر وجهتك وابدأ طلب المشوار", color = SafeMuted, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(18.dp))

                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(SafeBg)
                        ) {
                            Column(Modifier.padding(15.dp)) {
                                Text("موقع الانطلاق", fontSize = 11.sp, color = SafeMuted)
                                Text("سيتم تحديد موقعك عند بدء الحجز", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SafeInk)
                            }
                        }
                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = { destination = "دمشق - وسط المدينة" },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Person, null)
                            Spacer(Modifier.size(8.dp))
                            Text(destination ?: "اختيار الوجهة")
                        }

                        Spacer(Modifier.height(10.dp))
                        Button(
                            enabled = destination != null,
                            onClick = { },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("طلب تاكسي")
                        }
                    }
                }

                Text(
                    "تسجيل الدخول محفوظ على هذا الجهاز",
                    color = SafeMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
