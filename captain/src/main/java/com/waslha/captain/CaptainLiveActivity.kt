package com.waslha.captain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val LG = Color(0xFF0B805E)
private val LI = Color(0xFF14211C)
private val LM = Color(0xFF6E7D76)
private val LB = Color(0xFFF4F7F6)
private val LR = Color(0xFFB42318)

class CaptainLiveActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CaptainApiProvider.init(this)
        setContent { MaterialTheme { CaptainEntry() } }
    }
}

@Composable
private fun CaptainEntry() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val session = remember { CaptainSession(context) }
    var signedIn by remember { mutableStateOf(session.isSignedIn) }
    Surface(Modifier.fillMaxSize(), color = LB) {
        if (signedIn) {
            CaptainHomeV3(session) { session.clear(); signedIn = false }
        } else {
            CaptainLoginV3 { response -> session.save(response); signedIn = true }
        }
    }
}

@Composable
private fun CaptainLoginV3(onSuccess: (VerifySessionResponse) -> Unit) {
    val scope = rememberCoroutineScope()
    var phone by remember { mutableStateOf("+963") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var devCode by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun normalizedPhone(): String {
        val digits = phone.filter(Char::isDigit).removePrefix("963")
        return "+963$digits"
    }

    fun submit() {
        val value = normalizedPhone()
        if (value.removePrefix("+963").length < 8) { error = "أدخل رقم هاتف سوري صحيح"; return }
        if (sent && code.length < 4) { error = "أدخل رمز التحقق"; return }
        busy = true; error = null
        scope.launch {
            if (!sent) {
                runCatching { CaptainApiProvider.api.requestCode(OtpRequest(value)) }
                    .onSuccess { r -> if (r.success) { sent = true; devCode = r.data?.devCode } else error = r.message ?: "تعذر إرسال الرمز" }
                    .onFailure { error = it.message ?: "تعذر الاتصال بالخادم" }
            } else {
                runCatching { CaptainApiProvider.api.verifyCode(VerifyOtpRequest(value, code.trim())) }
                    .onSuccess { r ->
                        val data = r.data
                        if (r.success && data != null && data.role.equals("driver", true)) onSuccess(data)
                        else error = r.message ?: "الحساب غير مسجل ككابتن"
                    }
                    .onFailure { error = it.message ?: "تعذر تسجيل الدخول" }
            }
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(Color.White)) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(72.dp).background(LG, RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) { Text("و", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black) }
                Text("وصلها كابتن", color = LI, fontSize = 28.sp, fontWeight = FontWeight.Black)
                Text("رحلاتك تبدأ من هنا", color = LM, fontSize = 12.sp)
                OutlinedTextField(phone, { input ->
                    val digits = input.filter(Char::isDigit)
                    phone = if (digits.isEmpty()) "+963" else "+963${digits.removePrefix("963")}".take(13)
                }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رقم الهاتف السوري") })
                if (sent) {
                    OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("رمز التحقق") })
                    devCode?.let { Text("رمز الاختبار: $it", color = LG, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                }
                error?.let { Text(it, color = LR, fontSize = 11.sp, textAlign = TextAlign.Center) }
                Button(onClick = ::submit, enabled = !busy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = LG, contentColor = Color.White)) {
                    Text(if (sent) "دخول" else "إرسال رمز التحقق", fontWeight = FontWeight.Black)
                }
                if (sent) TextButton(onClick = { sent = false; code = ""; devCode = null; error = null }) { Text("تغيير الرقم", color = LG) }
            }
        }
    }
}
