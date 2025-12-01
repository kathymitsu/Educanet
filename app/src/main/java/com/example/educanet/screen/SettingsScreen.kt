@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.educanet.screen

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val st = remember { FirebaseStorage.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }

    var avatarUrl by remember { mutableStateOf(auth.currentUser?.photoUrl?.toString().orEmpty()) }
    var isUploading by remember { mutableStateOf(false) }

    // ---------- GALERÍA ----------
    val pickFromGallery = rememberLauncherForActivityResult(GetContent()) { uri: Uri? ->
        if (uri == null || uid.isBlank()) return@rememberLauncherForActivityResult
        scope.launch {
            isUploading = true
            try {
                val url = uploadAvatarFromUri(st, uid, uri)
                persistAvatar(db, auth, uid, url)
                avatarUrl = url
                snack.showSnackbar("Avatar actualizado desde galería")
            } catch (e: Exception) {
                handleAvatarError(e, snack)
            } finally {
                isUploading = false
            }
        }
    }

    // ---------- CÁMARA (bitmap en memoria) ----------
    val takeFromCamera = rememberLauncherForActivityResult(TakePicturePreview()) { bmp: Bitmap? ->
        if (bmp == null || uid.isBlank()) return@rememberLauncherForActivityResult
        scope.launch {
            isUploading = true
            try {
                val url = uploadAvatarFromBitmap(st, uid, bmp)
                persistAvatar(db, auth, uid, url)
                avatarUrl = url
                snack.showSnackbar("Avatar actualizado desde cámara")
            } catch (e: Exception) {
                handleAvatarError(e, snack)
            } finally {
                isUploading = false
            }
        }
    }

    // ---------- PERMISO DE CÁMARA ----------
    val requestCameraPerm = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            takeFromCamera.launch(null)
        } else {
            scope.launch { snack.showSnackbar("Permiso de cámara denegado") }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Vista previa del avatar
            if (avatarUrl.isNotBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(MaterialTheme.shapes.extraLarge)
                )
            } else {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(120.dp)
                ) {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sin\navatar")
                    }
                }
            }

            if (isUploading) {
                CircularProgressIndicator()
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(
                    enabled = !isUploading,
                    onClick = {
                        pickFromGallery.launch("image/*")
                    }
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Galería")
                }

                FilledTonalButton(
                    enabled = !isUploading,
                    onClick = {
                        requestCameraPerm.launch(android.Manifest.permission.CAMERA)
                    }
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cámara")
                }
            }
        }
    }
}

/* ------------ Helpers de subida ------------- */

private suspend fun uploadAvatarFromUri(
    storage: FirebaseStorage,
    uid: String,
    uri: Uri
): String {
    // Siempre usamos la misma ruta: sobreescribe avatar anterior
    val ref = storage.reference.child("avatars/$uid.jpg")
    ref.putFile(uri).await()          // sube el archivo
    return ref.downloadUrl.await().toString() // obtiene URL pública
}

private suspend fun uploadAvatarFromBitmap(
    storage: FirebaseStorage,
    uid: String,
    bmp: Bitmap
): String {
    val ref = storage.reference.child("avatars/$uid.jpg")
    val baos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
    val data = baos.toByteArray()
    ref.putBytes(data).await()
    return ref.downloadUrl.await().toString()
}

private suspend fun persistAvatar(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    uid: String,
    url: String
) {
    // Guarda en Firestore (perfil usuario)
    db.collection("users").document(uid)
        .update(mapOf("avatar" to url))
        .await()

    // Actualiza foto en Firebase Auth
    val profile = UserProfileChangeRequest.Builder()
        .setPhotoUri(url.toUri())
        .build()
    auth.currentUser?.updateProfile(profile)?.await()
}

/**
 * Maneja errores al subir avatar, para que "Object does not exist at location"
 * no se vea como un error grave en la app.
 */
private suspend fun handleAvatarError(
    e: Exception,
    snack: SnackbarHostState
) {
    val msg = e.message ?: "Error al actualizar avatar"

    // Este es el mensaje feo de Firebase Storage
    if (msg.contains("Object does not exist at location")) {
        // Lo convertimos en algo más entendible / suave
        snack.showSnackbar("No se encontró la imagen en Storage. Intenta subirla nuevamente.")
    } else {
        snack.showSnackbar("Error al subir avatar: $msg")
    }
}
