package com.waslha.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AuthGreen = Color(0xFF0B8F63)
private val AuthInk = Color(0xFF101817)
private val AuthMuted = Color(0xFF687571)

@Composable
fun PhoneAuthScreen(viewModel: PhoneAuthViewModel, onSignedIn: () -> Unit) {
    val state by viewModel.state.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        if (state.step == AuthStep.Otp) {
            IconButton(onClick = { viewModel.phoneChanged(state.phone) }) { Icon(Icons.Default.ArrowBack, "رجوع") }
            Icon(Icons.Default.Lock, null, tint = AuthGreen, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            Text("تأكيد رقم الهاتف", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AuthInk)
            Text("أدخل الرمز المرسل إلى ${state.phone}", color = AuthMuted)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.otp,
                onValueChange = viewModel::otpChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                placeholder = { Text("••••••") }
            )
            state.devCode?.let {
                Spacer(Modifier.height(8.dp))
                Text("رمز التطوير: $it", color = AuthGreen, fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.verifyCode() },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuthGreen)
            ) {
                if (state.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                else Text("تأكيد ودخول", fontWeight = FontWeight.Bold)
            }
        } else {
            Icon(Icons.Default.Phone, null, tint = AuthGreen, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(18.dp))
            Text("أهلاً بك في وصلها", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = AuthInk)
            Text("سجّل رقم هاتفك حتى نجهّز رحلتك بأمان", color = AuthMuted)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::phoneChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                placeholder = { Text("مثال: +963 9xxxxxxxx") }
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = viewModel::requestCode,
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(17.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AuthGreen)
            ) {
                if (state.loading) CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                else Text("إرسال رمز التحقق", fontWeight = FontWeight.Bold)
            }
        }
        state.error?.let { error ->
            Spacer(Modifier.height(12.dp))
            Text(error, color = Color(0xFFB42318), fontSize = 13.sp)
        }
    }
    if (state.step == AuthStep.SignedIn) onSignedIn()
}
