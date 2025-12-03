package com.example.educanet

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.educanet.repo.TokenRepo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Crea el canal por defecto (Android 8+).
 */
fun createDefaultChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val ch = NotificationChannel(
            "educanet_default", "Educanet",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        ContextCompat.getSystemService(context, NotificationManager::class.java)
            ?.createNotificationChannel(ch)
    }
}

/**
 * Obtiene el FCM token actual y lo guarda en Firestore.
 */
suspend fun refreshAndSyncToken() {
    val token = FirebaseMessaging.getInstance().token.await()
    TokenRepo().saveToken(token)
}

/**
 * Se suscribe a topics según rol:
 *  - admin/profesor/estudiante/apoderado
 *  - Además: topics específicos por usuario
 */
suspend fun setupMessagingSubscriptions(role: String?, children: List<String>) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val fm = FirebaseMessaging.getInstance()

    // Siempre: topic general de clases
    fm.subscribeToTopic("classes").await()

    // Por rol
    when (role) {
        "admin" -> {
            fm.subscribeToTopic("admin").await()
        }
        "profesor" -> {
            fm.subscribeToTopic("teacher-$uid").await()
        }
        "estudiante" -> {
            fm.subscribeToTopic("student-$uid").await()
        }
        "apoderado" -> {
            fm.subscribeToTopic("guardian-$uid").await()
            // Si quieres notificar por cada hijo:
            children.forEach { childUid ->
                fm.subscribeToTopic("student-$childUid").await()
            }
        }
        else -> { /* nada */ }
    }
}
