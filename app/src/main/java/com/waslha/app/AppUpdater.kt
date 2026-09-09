package com.waslha.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

class AppUpdater(private val context: Context) {
    private val appContext = context.applicationContext
    private val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = appContext.getSharedPreferences("waslha_update", Context.MODE_PRIVATE)

    suspend fun check(): Result<AppUpdateDto?> = withContext(Dispatchers.IO) {
        runCatching {
            ApiProvider.api.latestUpdate(BuildConfig.VERSION_CODE, "universal")
                .let { response ->
                    require(response.success) { response.message ?: "تعذر فحص التحديث" }
                    response.data?.takeIf { it.updateAvailable && it.apk != null }
                }
        }
    }

    fun downloadUpdate(update: AppUpdateDto): Long {
        val apk = requireNotNull(update.apk)
        val oldId = prefs.getLong("download_id", -1L)
        val storedVersion = prefs.getString("version", null)

        if (oldId > 0L && storedVersion == update.versionName) {
            val status = downloadStatus(oldId)
            if (status == DownloadManager.STATUS_PENDING ||
                status == DownloadManager.STATUS_RUNNING ||
                status == DownloadManager.STATUS_PAUSED ||
                status == DownloadManager.STATUS_SUCCESSFUL
            ) return oldId
        }

        val request = DownloadManager.Request(Uri.parse(apk.url))
            .setTitle("وصلها ${update.versionName}")
            .setDescription("تحديث التطبيق")
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationInExternalFilesDir(appContext, Environment.DIRECTORY_DOWNLOADS, "Waslha/updates/${apk.name}")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val id = downloadManager.enqueue(request)
        prefs.edit()
            .putLong("download_id", id)
            .putString("version", update.versionName)
            .apply()
        return id
    }

    fun downloadProgress(downloadId: Long): Pair<Long, Long> {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return 0L to 0L
            return cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)) to
                cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
        }
    }

    fun downloadStatus(downloadId: Long): Int? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
        }
    }

    fun downloadFile(downloadId: Long): File? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return null
            if (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) != DownloadManager.STATUS_SUCCESSFUL) return null
            val uri = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)))
            return when (uri.scheme) {
                "file" -> uri.path?.let(::File)
                else -> null
            }
        }
    }

    fun verifySha256(file: File, expected: String?): Boolean {
        if (expected.isNullOrBlank()) return false
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        return actual.equals(expected.trim(), ignoreCase = true)
    }

    fun canInstallPackages(): Boolean = Build.VERSION.SDK_INT < 26 || appContext.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings() {
        if (Build.VERSION.SDK_INT >= 26) {
            appContext.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${appContext.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun install(file: File) {
        require(file.exists()) { "ملف التحديث غير موجود" }
        if (!canInstallPackages()) {
            openInstallPermissionSettings()
            return
        }
        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        appContext.startActivity(intent)
    }

    fun cleanup(file: File?) {
        file?.takeIf { it.exists() }?.delete()
        prefs.edit().remove("download_id").remove("version").apply()
    }

    fun registerDownloadReceiver(onCompleted: (Long) -> Unit): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                onCompleted(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L))
            }
        }
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= 33) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            appContext.registerReceiver(receiver, filter)
        }
        return receiver
    }

    fun unregisterDownloadReceiver(receiver: BroadcastReceiver) {
        runCatching { appContext.unregisterReceiver(receiver) }
    }
}
