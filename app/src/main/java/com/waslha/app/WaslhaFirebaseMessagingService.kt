package com.waslha.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class WaslhaFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        FcmRegistration.sendToken(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val notification = message.notification ?: return
        createChannel()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return

        val id = (message.data["notificationId"] ?: message.messageId ?: System.currentTimeMillis().toString())
            .hashCode()
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.waslha_brand_logo)
            .setContentTitle(notification.title ?: "وصلها")
            .setContentText(notification.body ?: "لديك إشعار جديد")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        NotificationManagerCompat.from(this).notify(id, builder.build())
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "إشعارات وصلها", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "تنبيهات الرحلات وحالة الطلب"
            }
        )
    }

    companion object {
        const val CHANNEL_ID = "waslha_trips"
    }
}
