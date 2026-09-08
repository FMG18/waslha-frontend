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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch

class UpdateGateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiProvider.init(this)
        setContent { UpdateGateScreen() }
    }

    @androidx.compose.runtime.Composable
    private fun UpdateGateScreen() {
        val updater = remember { AppUpdater(this@UpdateGateActivity) }
        val sessionStore = remember { SessionStore(this@UpdateGateActivity) }
        var checking by remember { mutableStateOf(true) }
        var update by remember { mutableStateOf<AppUpdateDto?>(null) }
        var error by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(false) }
        var progress by remember { mutableIntStateOf(0) }
        var downloadId by remember { mutableLongStateOf(-1L) }

        fun continueToApp() {
            val target = if (sessionStore.isSignedIn) MainActivity::class.java else AuthActivity::class.java
            startActivity(Intent(this@UpdateGateActivity, target).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
            finish()
        }

        LaunchedEffect(Unit) {
            updater.check()
                .onSuccess { result -> update = result }
                .onFailure { error = true }
            checking = false
        }

        LaunchedEffect(downloading, downloadId) {
            if (!downloading || downloadId <= 0L) return@LaunchedEffect
            while (downloading) {
                val query = android.app.DownloadManager.Query().setFilterById(downloadId)
                val manager = getSystemService(DOWNLOAD_SERVICE) as android.app.DownloadManager
                manager.query(query).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        if (total > 0) progress = ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(android.app.DownloadManager.COLUMN_STATUS))
                        when (status) {
                            android.app.DownloadManager.STATUS_SUCCESSFUL -> {
                                val file = updater.downloadFile(downloadId)
                                val item = update
                                if (file != null && item != null && updater.verifySha256(file, item.apk?.sha256)) {
                                    downloading = false
                                    updater.install(file)
                                } else {
                                    downloading = false
                                    error = true
                                }
                            }
                            android.app.DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                error = true
                            }
                        }
                    }
                }
                delay(300)
            }
        }

        MaterialTheme {
            Surface(Modifier.fillMaxSize()) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    when {
                        checking -> {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(18.dp))
                            Text("جاري فحص إصدار وصلها…", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        downloading -> {
                            Text("تحديث وصلها", fontSize = 26.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(8.dp))
                            Text("جاري تنزيل التحديث… $progress%", fontSize = 15.sp)
                            Spacer(Modifier.height(18.dp))
                            androidx.compose.material3.LinearProgressIndicator(
                                progress = { progress / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("تقدر تترك التطبيق، مدير التنزيل في Android يكمل التحميل.", fontSize = 12.sp)
                        }
                        update != null -> {
                            val item = update!!
                            Text("تحديث جديد متوفر 🎉", fontSize = 28.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.height(10.dp))
                            Text("الإصدار ${item.versionName}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("حجم التحديث: ${formatSize(item.apk?.size ?: 0L)}", fontSize = 14.sp)
                            if (item.releaseNotes.isNotBlank()) {
                                Spacer(Modifier.height(14.dp))
                                Text(item.releaseNotes.take(500), fontSize = 13.sp, textAlign = TextAlign.Start)
                            }
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = {
                                    error = false
                                    downloadId = updater.downloadUpdate(item)
                                    downloading = true
                                },
                                modifier = Modifier.fillMaxWidth().height(54.dp)
                            ) { Text("تحديث الآن", fontWeight = FontWeight.Bold) }
                            if (!item.mandatory) {
                                Spacer(Modifier.height(10.dp))
                                OutlinedButton(onClick = ::continueToApp, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                                    Text("لاحقاً")
                                }
                            }
                        }
                        else -> {
                            Text(if (error) "تعذر فحص التحديث. نكمل تشغيل التطبيق بشكل طبيعي." else "وصلها محدثة", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(18.dp))
                            Button(onClick = ::continueToApp, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                                Text("متابعة")
                            }
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
