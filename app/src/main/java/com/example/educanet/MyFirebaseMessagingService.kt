package com.example.educanet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.educanet.repo.TokenRepo
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Guarda/actualiza el token en Firestore
        TokenRepo().saveToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "Educanet"
        val body  = message.notification?.body  ?: message.data["body"]  ?: "Tienes una actualización"

        showLocalNotification(title, body)
    }

    private fun showLocalNotification(title: String, body: String) {
        val channelId = "educanet_default"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                channelId,
                "Educanet",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setColor(ContextCompat.getColor(this, android.R.color.holo_purple))
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Solo muestra si tenemos permiso en Android 13+
        val mgr = NotificationManagerCompat.from(this)
        mgr.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
    }
}
