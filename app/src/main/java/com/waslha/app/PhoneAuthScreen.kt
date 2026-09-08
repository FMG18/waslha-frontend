package com.waslha.app

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AuthGreen = Color(0xFF0A8F63)
private val AuthDark = Color(0xFF10201B)
private val AuthMuted = Color(0xFF708079)
private val AuthBg = Color(0xFFF4F7F5)

@Composable
fun PhoneAuthScreen(viewModel: PhoneAuthViewModel, onSignedIn: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Surface(Modifier.fillMaxSize(), color = AuthBg) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (state.step == AuthStep.Otp) {
                    IconButton(onClick = { viewModel.phoneChanged(state.phone) }) {
                        Icon(Icons.Default.ArrowBack, "رجوع")
                    }
                } else Spacer(Modifier.size(48.dp))
                Text("وصلها", Modifier.weight(1f), fontSize = 24.sp, fontWeight = FontWeight.Black, color = AuthGreen)
                Box(Modifier.size(42.dp).background(AuthGreen.copy(alpha = .10f), CircleShape), Alignment.Center) {
                    Icon(Icons.Default.Security, null, tint = AuthGreen, modifier = Modifier.size(21.dp))
                }
            }

            Spacer(Modifier.height(28.dp))
            Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.Center) {
                Box(Modifier.size(92.dp).background(AuthGreen.copy(alpha = .10f), CircleShape), Alignment.Center) {
                    Icon(
                        if (state.step == AuthStep.Otp) Icons.Default.Lock else Icons.Default.Phone,
                        null,
                        tint = AuthGreen,
                        modifier = Modifier.size(42.dp)
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    if (state.step == AuthStep.Otp) "تأكد من رقمك" else "أهلاً بك في وصلها",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = AuthDark
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    if (state.step == AuthStep.Otp) "أدخل رمز التحقق لإكمال تسجيل الدخول" else "مشوارك أسهل. سيارتك أقرب. رحلتك بأمان.",
                    color = AuthMuted,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(24.dp))

                if (state.step == AuthStep.Otp) {
                    Text("رقم الهاتف", fontSize = 12.sp, color = AuthMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(state.phone, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AuthDark)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = state.otp,
                        onValueChange = viewModel::otpChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        placeholder = { Text("رمز التحقق") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    state.devCode?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("رمز الاختبار: $it", color = AuthGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { viewModel.verifyCode() },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AuthGreen)
                    ) {
                        if (state.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                        else Text("تأكيد والمتابعة", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Text("رقم الهاتف", fontSize = 12.sp, color = AuthMuted, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(7.dp))
                    OutlinedTextField(
                        value = state.phone,
                        onValueChange = viewModel::phoneChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        placeholder = { Text("+963 9xxxxxxxx") },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(17.dp))
                    Button(
                        onClick = viewModel::requestCode,
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(17.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AuthGreen)
                    ) {
                        if (state.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                        else Text("إرسال رمز التحقق", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = AuthGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("رقمك يستخدم فقط لتأمين حسابك وإدارة رحلاتك", color = AuthMuted, fontSize = 12.sp)
                    }
                }
                state.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = Color(0xFFB42318), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("بوصلها، أنت توافق على شروط الاستخدام وسياسة الخصوصية", color = AuthMuted, fontSize = 11.sp)
        }
    }

    if (state.step == AuthStep.SignedIn) onSignedIn()
}
