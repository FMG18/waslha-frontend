package com.waslha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WaslhaGreen = Color(0xFF0B8F63)
private val WaslhaInk = Color(0xFF101817)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WaslhaApp() }
    }
}

@Composable
private fun WaslhaApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFF7F9F8)) {
            HomeScreen()
        }
    }
}

@Composable
private fun HomeScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE3ECE8)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LocationOn, null, tint = WaslhaGreen)
                Spacer(Modifier.height(8.dp))
                Text("خريطة وصلها", fontWeight = FontWeight.Bold, color = WaslhaInk)
                Text("ستظهر الخريطة الحية هنا", color = Color(0xFF5D6A67))
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("وصلها", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = WaslhaGreen)
                    }
                }
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Icon(Icons.Default.Person, "الحساب", modifier = Modifier.padding(12.dp), tint = WaslhaInk)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("إلى أين تريد الذهاب؟", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = WaslhaInk)
                    Spacer(Modifier.height(14.dp))
                    LocationRow("موقع الانطلاق الحالي")
                    Spacer(Modifier.height(10.dp))
                    LocationRow("حدد وجهتك")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {},
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WaslhaGreen)
                    ) {
                        Text("اختيار الوجهة", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationRow(label: String) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color(0xFFF2F5F4))) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = WaslhaGreen)
            Spacer(Modifier.padding(4.dp))
            Text(label, color = Color(0xFF687571), fontSize = 15.sp)
        }
    }
}
