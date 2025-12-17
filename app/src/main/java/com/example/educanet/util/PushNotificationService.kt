package com.example.educanet.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.educanet.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PushNotificationService : FirebaseMessagingService() {

    // Se dispara cuando se genera un nuevo token de FCM o se actualiza.
    // Es crucial guardar este token en el servidor para enviar notificaciones dirigidas.
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        sendTokenToServer(token)
    }

    // Se llama cuando se recibe un mensaje mientras la app está en primer plano.
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extraer título y cuerpo de la notificación
        val title = remoteMessage.notification?.title ?: "Nueva notificación"
        val body = remoteMessage.notification?.body ?: "Has recibido un nuevo mensaje."

        Log.d("FCM", "Message received: $title - $body")

        // Mostrar la notificación
        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "educanet_channel"
        val channelName = "Educanet Notificaciones"
        val notificationId = System.currentTimeMillis().toInt()

        // Crear canal de notificación (necesario para Android 8.0+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Canal para notificaciones de Educanet"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Construir la notificación.
        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Asegúrate de tener este ícono en res/drawable
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Mostrar la notificación.
        with(NotificationManagerCompat.from(this)) {
            // Se necesita permiso POST_NOTIFICATIONS en Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    // Si no hay permiso, no se puede mostrar.
                    // La UI debería haberlo pedido ya.
                    Log.w("FCM", "POST_NOTIFICATIONS permission not granted.")
                    return
                }
            }
            notify(notificationId, builder.build())
        }
    }

    // Guarda el token en el perfil del usuario en Firestore.
    private fun sendTokenToServer(token: String?) {
        if (token == null) return
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) return

        // Usamos un coroutine para la operación de red.
        CoroutineScope(Dispatchers.IO).launch {
            val db = Firebase.firestore
            db.collection("users").document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener { Log.d("FCM", "Token updated successfully.") }
                .addOnFailureListener { e -> Log.e("FCM", "Error updating token", e) }
        }
    }
}