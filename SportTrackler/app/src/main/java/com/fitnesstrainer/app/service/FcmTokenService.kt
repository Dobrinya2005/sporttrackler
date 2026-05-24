package com.fitnesstrainer.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.fitnesstrainer.app.App
import com.fitnesstrainer.app.R
import com.fitnesstrainer.app.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FcmTokenService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tokenStorage = App.instance.tokenStorage
                if (tokenStorage.isLoggedIn()) {
                    App.instance.apiService.registerFcmToken(mapOf("deviceToken" to token))
                }
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title      = message.notification?.title ?: message.data["title"] ?: return
        val body       = message.notification?.body  ?: message.data["body"]  ?: return
        val senderId   = message.data["senderId"]?.toIntOrNull()
        val senderName = message.data["senderName"] ?: title

        // Не показывать уведомление если отправитель — это сам текущий пользователь
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val myId = App.instance.tokenStorage.getUserId()
                if (senderId != null && senderId == myId) return@launch
                showNotification(title, body, senderId, senderName)
            } catch (_: Exception) {
                showNotification(title, body, senderId, senderName)
            }
        }
    }

    private fun showNotification(title: String, body: String, senderId: Int?, senderName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri = Uri.parse("android.resource://${packageName}/${R.raw.notification_chat}")

        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Сообщения чата",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description      = "Уведомления о новых сообщениях"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 150, 80, 150)
            enableLights(true)
            lightColor       = Color.parseColor("#4CAF50")
            setSound(soundUri, audioAttr)
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (senderId != null) {
                putExtra("open_chat_user_id", senderId)
                putExtra("open_chat_user_name", senderName)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, senderId ?: 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher_round)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setLargeIcon(largeIcon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(body)
                .setBigContentTitle(title)
                .setSummaryText("Спорт-трекер"))
            .setColor(Color.parseColor("#4CAF50"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 150, 80, 150))
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .build()

        manager.notify(senderId ?: System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "chat_messages_v2"
    }
}
