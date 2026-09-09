package com.waslha.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class LaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                visible = true
                delay(950)
                val sessionStore = SessionStore(this@LaunchActivity)
                val target = if (sessionStore.isSignedIn) SafeTaxiActivity::class.java else AuthActivity::class.java
                startActivity(Intent(this@LaunchActivity, target).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                })
                finish()
            }
            MaterialThemeSafeLaunch(visible)
        }
    }
}

@Composable
private fun MaterialThemeSafeLaunch(visible: Boolean) {
    androidx.compose.material3.MaterialTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(Color(0xFF12082A), Color(0xFF23104D), Color(0xFF080812))
                )
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn()) {
                    Box(
                        modifier = Modifier.size(124.dp).clip(RoundedCornerShape(34.dp)).background(Color(0xFF0F0F1A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.waslha_brand_logo),
                            contentDescription = "وصلها",
                            modifier = Modifier.size(104.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                AnimatedVisibility(visible = visible, enter = fadeIn()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.Text("وصلها", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        androidx.compose.material3.Text("وصلها.. أسرع وأسهل", color = Color(0xFFC7B8FF), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
