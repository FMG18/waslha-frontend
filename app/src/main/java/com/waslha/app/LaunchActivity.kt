package com.waslha.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Branded Android launch surface. It intentionally performs no network work;
 * MainActivity owns authentication and the passenger experience.
 */
class LaunchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var visible by rememberLaunchVisibility()

            LaunchedEffect(Unit) {
                delay(950)
                startActivity(Intent(this@LaunchActivity, MainActivity::class.java))
                finish()
            }

            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF087F5B),
                                    Color(0xFF0B9A6A),
                                    Color(0xFF063F2E)
                                )
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
                                modifier = Modifier
                                    .size(112.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("و", color = Color(0xFF087F5B), fontSize = 64.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        AnimatedVisibility(visible = visible, enter = fadeIn()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("وصلها", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(5.dp))
                                Text("مشوارك يبدأ هنا", color = Color.White.copy(alpha = .82f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun rememberLaunchVisibility(): androidx.compose.runtime.MutableState<Boolean> {
    val state = androidx.compose.runtime.remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { state.value = true }
    return state
}
