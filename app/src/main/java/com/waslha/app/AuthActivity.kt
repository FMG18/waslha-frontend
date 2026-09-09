package com.waslha.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val AuthGreen = Color(0xFF078A60)
private val AuthDark = Color(0xFF10201B)
private val AuthMuted = Color(0xFF72807B)
private val AuthBg = Color(0xFFF5F8F6)
private val AuthDanger = Color(0xFFB42318)

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)

        val sessionStore = SessionStore(this)
        if (sessionStore.isSignedIn) {
            openMain()
            return
        }

        val repository = AuthRepository(ApiProvider.api, sessionStore)
        setContent {
            WaslhaAuthScreen(repository = repository, onAuthenticated = ::openMain)
        }
    }

    private fun openMain() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        finish()
    }
}

@Composable
private fun WaslhaAuthScreen(repository: AuthRepository, onAuthenticated: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val googleAuthClient = remember(context, repository) { GoogleAuthClient(context, repository) }
    val scope = rememberCoroutineScope()

    var phone by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var devCode by remember { mutableStateOf<String?>(null) }

    Surface(Modifier.fillMaxSize(), color = AuthBg) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(34.dp))
            Box(Modifier.size(76.dp).background(AuthGreen, CircleShape), contentAlignment = Alignment.Center) {
                Text("و", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text("وصلها", fontSize = 34.sp, fontWeight = FontWeight.Black, color = AuthGreen)
            Text("مشوارك يبدأ هنا", color = AuthMuted, fontSize = 13.sp)
            Spacer(Modifier.height(28.dp))

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(Color.White),
                elevation = CardDefaults.cardElevation(3.dp)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(if (step) "تأكيد رقم الهاتف" else "أهلاً بك في وصلها", fontSize = 25.sp, fontWeight = FontWeight.Black, color = AuthDark)
                    Spacer(Modifier.height(6.dp))
                    Text(if (step) "أدخل رمز التحقق لإكمال تسجيل الدخول" else "اختر طريقة الدخول المناسبة لك", color = AuthMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(20.dp))

                    if (!step) {
                        Button(
                            enabled = !loading,
                            onClick = {
                                loading = true
                                error = null
                                scope.launch {
                                    val result = googleAuthClient.signIn()
                                    result
                                        .onSuccess { onAuthenticated() }
                                        .onFailure { error = googleAuthClient.userMessage(it) }
                                    loading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AuthDark),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp)
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else {
                                Box(Modifier.size(30.dp).background(Color(0xFFF1F3F4), CircleShape), contentAlignment = Alignment.Center) {
                                    Text("G", fontSize = 17.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(Modifier.size(10.dp))
                                Text("متابعة باستخدام Google", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(18.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(Modifier.weight(1f), color = Color(0xFFE5EAE7))
                            Text("  أو  ", color = AuthMuted, fontSize = 11.sp)
                            HorizontalDivider(Modifier.weight(1f), color = Color(0xFFE5EAE7))
                        }
                        Spacer(Modifier.height(18.dp))

                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.filter(Char::isDigit).take(15); error = null },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            label = { Text("رقم الهاتف") },
                            placeholder = { Text("مثال: 093xxxxxxxx") },
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            enabled = phone.length >= 8 && !loading,
                            onClick = {
                                loading = true
                                error = null
                                scope.launch {
                                    repository.requestCode(normalizePhoneForApi(phone))
                                        .onSuccess {
                                            devCode = it.devCode
                                            otp = it.devCode.orEmpty()
                                            step = true
                                        }
                                        .onFailure { error = it.message ?: "تعذر إرسال رمز التحقق" }
                                    loading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("متابعة برقم الهاتف", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(phone, color = AuthDark, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = otp,
                            onValueChange = { otp = it.filter(Char::isDigit).take(6); error = null },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("رمز التحقق") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            shape = RoundedCornerShape(16.dp)
                        )
                        devCode?.let {
                            Spacer(Modifier.height(7.dp))
                            Text("رمز الاختبار: $it", color = AuthGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(14.dp))
                        Button(
                            enabled = otp.length == 6 && !loading,
                            onClick = {
                                loading = true
                                error = null
                                scope.launch {
                                    repository.verifyCode(normalizePhoneForApi(phone), otp)
                                        .onSuccess { onAuthenticated() }
                                        .onFailure { error = it.message ?: "رمز التحقق غير صحيح" }
                                    loading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("دخول إلى وصلها", fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { step = false; otp = ""; devCode = null; error = null }) { Text("رجوع لطرق الدخول") }
                    }

                    error?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = AuthDanger, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Security, null, tint = AuthGreen, modifier = Modifier.size(15.dp))
                Spacer(Modifier.size(6.dp))
                Text("تسجيل الدخول آمن ونستخدم بياناتك لتشغيل حساب وصلها فقط", color = AuthMuted, fontSize = 10.sp)
            }
        }
    }
}

private fun normalizePhoneForApi(value: String): String = when {
    value.startsWith("+") -> value
    value.startsWith("963") -> "+$value"
    value.startsWith("0") -> "+963${value.drop(1)}"
    else -> "+963$value"
}
