package com.waslha.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class UpdateGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        setContent { UpdateGateScreen() }
    }

    @Composable
    private fun UpdateGateScreen() {
        val updater = remember { AppUpdater(this@UpdateGateActivity) }
        val sessionStore = remember { SessionStore(this@UpdateGateActivity) }
        var checking by remember { mutableStateOf(true) }
        var update by remember { mutableStateOf<AppUpdateDto?>(null) }
        var error by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(false) }
        var progress by remember { mutableIntStateOf(0) }
        var downloadId by remember { mutableLongStateOf(-1L) }
        var verifiedFile by remember { mutableStateOf<java.io.File?>(null) }
        var needsInstallPermission by remember { mutableStateOf(false) }

        fun continueToApp() {
            val target = if (sessionStore.isSignedIn) {
                if (!sessionStore.activeTripId.isNullOrBlank()) TripResumeActivity::class.java else WaslhaCustomerActivity::class.java
            } else AuthActivity::class.java
            startActivity(Intent(this@UpdateGateActivity, target).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
        }

        LaunchedEffect(Unit) {
            updater.cleanupStaleDownloads()
            updater.check()
                .onSuccess { result -> update = result }
                .onFailure { error = true }
            checking = false
        }

        DisposableEffect(Unit) { onDispose { } }

        LaunchedEffect(downloading, downloadId) {
            if (!downloading || downloadId <= 0L) return@LaunchedEffect
            while (downloading) {
                val status = updater.downloadStatus(downloadId)
                val (downloaded, total) = updater.downloadProgress(downloadId)
                if (total > 0L) progress = ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                when (status) {
                    android.app.DownloadManager.STATUS_SUCCESSFUL -> {
                        val file = updater.downloadFile(downloadId)
                        val item = update
                        if (file != null && item != null && updater.verifySha256(file, item.apk?.sha256)) {
                            downloading = false
                            verifiedFile = file
                            needsInstallPermission = !updater.canInstallPackages()
                            if (!needsInstallPermission) updater.install(file)
                        } else { downloading = false; error = true }
                    }
                    android.app.DownloadManager.STATUS_FAILED -> { downloading = false; error = true }
                }
                delay(500)
            }
        }

        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                    when {
                        checking -> {
                            CircularProgressIndicator(); Spacer(Modifier.height(18.dp))
                            Text("جاري فحص إصدار وصلها…", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        downloading -> {
                            Text("تحديث وصلها", fontSize = 26.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(8.dp)); Text("جاري تنزيل التحديث… $progress%", fontSize = 15.sp)
                            Spacer(Modifier.height(18.dp)); LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp)); Text("التنزيل يتم عبر مدير Android ويستأنف بعد انقطاع الاتصال.", fontSize = 12.sp)
                        }
                        needsInstallPermission && verifiedFile != null -> {
                            Text("التحديث جاهز للتثبيت", fontSize = 26.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(10.dp)); Text("اسمح لوصلها بتثبيت التحديث مرة واحدة من إعدادات Android.", fontSize = 14.sp, textAlign = TextAlign.Start)
                            Spacer(Modifier.height(22.dp))
                            Button(onClick = {
                                needsInstallPermission = false
                                val file = verifiedFile
                                if (file != null && !updater.install(file)) needsInstallPermission = true
                            }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("متابعة التثبيت", fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(onClick = ::continueToApp, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("لاحقاً") }
                        }
                        update != null -> {
                            val item = update!!
                            Text("تحديث جديد متوفر 🎉", fontSize = 28.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(10.dp)); Text("الإصدار ${item.versionName}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp)); Text("حجم التحديث: ${formatSize(item.apk?.size ?: 0L)}", fontSize = 14.sp)
                            if (item.releaseNotes.isNotBlank()) { Spacer(Modifier.height(14.dp)); Text(item.releaseNotes.take(500), fontSize = 13.sp, textAlign = TextAlign.Start) }
                            Spacer(Modifier.height(24.dp))
                            Button(onClick = {
                                error = false; progress = 0; verifiedFile = null; needsInstallPermission = false
                                downloadId = updater.downloadUpdate(item); downloading = true
                            }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("تحديث الآن", fontWeight = FontWeight.Bold) }
                            if (!item.mandatory) { Spacer(Modifier.height(10.dp)); OutlinedButton(onClick = ::continueToApp, modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("لاحقاً") } }
                            if (error) { Spacer(Modifier.height(10.dp)); Text("فشل تنزيل أو التحقق من ملف التحديث. حاول مرة أخرى.", fontSize = 12.sp); TextButton(onClick = { error = false; progress = 0; downloadId = updater.downloadUpdate(item); downloading = true }) { Text("إعادة المحاولة") } }
                        }
                        else -> {
                            Text(if (error) "تعذر فحص التحديث. نكمل تشغيل التطبيق بشكل طبيعي." else "وصلها محدثة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(18.dp)); Button(onClick = ::continueToApp, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("متابعة") }
                        }
                    }
                }
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "غير معروف"
        val mb = bytes / 1024.0 / 1024.0
        return if (mb >= 1) String.format("%.1f MB", mb) else String.format("%.0f KB", bytes / 1024.0)
    }
}
