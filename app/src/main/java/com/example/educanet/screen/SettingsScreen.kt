package com.example.educanet.screen

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage // Asume que estás usando Coil
import com.google.firebase.auth.FirebaseAuth
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
// Importaciones de Coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// Importaciones de I/O
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import android.Manifest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val appContext = LocalContext.current.applicationContext
    val auth = remember { FirebaseAuth.getInstance() }
    val uid = auth.currentUser?.uid.orEmpty()

    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var avatarImagePath by remember { mutableStateOf<Uri?>(null) }
    var loadingStatus by remember { mutableStateOf(false) }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    // --- Permisos ---
    val readStoragePermissionToAsk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val storagePermissionState = rememberPermissionState(readStoragePermissionToAsk)

    // 🔥 Permiso de Notificaciones (requerido para el profesor)
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    // Función simulada para obtener el rol (En un proyecto real, esto se cargaría de Firestore)
    val userRole = remember(uid) {
        // Lógica de ejemplo: si el UID coincide con un ID de profesor conocido
        if (uid == "ALGÚN_UID_DE_PROFESOR") "professor" else "student"
    }

    // ---------- Launchers de galería y cámara ----------
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                scope.launch {
                    val localFile = copyUriToAppStorage(appContext, uri)

                    if (localFile != null) {
                        loadingStatus = true

                        val success = saveAvatarLocally(Uri.fromFile(localFile), appContext)

                        loadingStatus = false
                        if (success) {
                            val finalUri = getLatestImageUri(appContext)
                            if (finalUri != null) {
                                avatarImagePath = finalUri
                            }
                            scope.launch { snack.showSnackbar("Imagen guardada localmente.") }
                        } else {
                            scope.launch { snack.showSnackbar("Error al guardar la imagen localmente.") }
                        }
                        localFile.delete()
                    } else {
                        scope.launch { snack.showSnackbar("Error: No se pudo copiar la imagen.") }
                    }
                }
            } else {
                scope.launch { snack.showSnackbar("No se seleccionó ninguna imagen.") }
            }
        }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            scope.launch {
                if (success && cameraUri != null) {
                    loadingStatus = true

                    val saveSuccess = saveAvatarLocally(cameraUri!!, appContext)

                    loadingStatus = false
                    if (saveSuccess) {
                        val finalUri = getLatestImageUri(appContext)
                        if (finalUri != null) {
                            avatarImagePath = finalUri
                        }
                        scope.launch { snack.showSnackbar("Foto guardada localmente.") }
                    } else {
                        scope.launch { snack.showSnackbar("Error al guardar la foto localmente.") }
                    }

                    deleteFileFromUri(appContext, cameraUri!!)
                    cameraUri = null
                } else if (cameraUri != null) {
                    deleteFileFromUri(appContext, cameraUri!!)
                    cameraUri = null
                    scope.launch { snack.showSnackbar("Captura de cámara cancelada o fallida.") }
                }
            }
        }

    // ... (El resto de funciones auxiliares como createCameraUriWithFileProvider) ...
    fun createCameraUriWithFileProvider(context: Context): Uri? {
        try {
            val tempFile = File(
                context.externalCacheDir,
                "temp_avatar_${System.currentTimeMillis()}.jpg"
            )

            if (!tempFile.exists()) {
                tempFile.createNewFile()
            }

            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                tempFile
            )
        } catch (e: Exception) {
            scope.launch { snack.showSnackbar("Error al preparar la URI de la cámara: ${e.message}") }
            return null
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // ---------- Avatar UI ----------
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFF1E9FF)),
                contentAlignment = Alignment.Center
            ) {
                if (avatarImagePath != null) {
                    AsyncImage(
                        model = avatarImagePath,
                        contentDescription = "Avatar Local",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "Sin\navatar",
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (loadingStatus) {
                CircularProgressIndicator()
            }

            // ---------- Botones de Galería/Cámara ----------
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (storagePermissionState.status.isGranted) {
                            galleryLauncher.launch("image/*")
                        } else {
                            storagePermissionState.launchPermissionRequest()
                        }
                    },
                    enabled = !loadingStatus && uid.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Galería")
                }

                Button(
                    onClick = {
                        val uri = createCameraUriWithFileProvider(appContext)
                        if (uri != null) {
                            cameraUri = uri
                            cameraLauncher.launch(uri)
                        } else {
                            scope.launch {
                                snack.showSnackbar("No se pudo crear archivo temporal.")
                            }
                        }
                    },
                    enabled = !loadingStatus && uid.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cámara")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ---------- Botón de Permiso de Notificaciones ----------
            Button(
                onClick = { notificationPermissionState.launchPermissionRequest() },
                enabled = !notificationPermissionState.status.isGranted,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (notificationPermissionState.status.isGranted) Color.Green else Color.Red
                )
            ) {
                Text(if (notificationPermissionState.status.isGranted) "✅ Notificaciones ON" else "🔔 Activar Notificaciones")
            }

            // ---------- Ejemplo de Recurso con Permiso de Eliminación ----------
            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Text("Gestión de Recursos (Demo Profesor)", style = MaterialTheme.typography.titleMedium)

            // Simulación de un recurso subido por el mismo usuario (para probar 'canDelete')
            ResourceItem(
                resourceTitle = "Recurso 1: Guía de estudio (Subido por usted)",
                resourceUploaderUid = uid, // Simulación: El recurso lo subió este usuario
                currentUserRole = userRole,
                onDelete = { title -> scope.launch { snack.showSnackbar("Recurso '$title' eliminado (si las reglas de Firebase lo permiten).") } }
            )

            // Simulación de un recurso subido por otro usuario
            ResourceItem(
                resourceTitle = "Recurso 2: Video de la clase (Otro usuario)",
                resourceUploaderUid = "OTRO_UID_DIFERENTE", // Otro usuario
                currentUserRole = userRole,
                onDelete = { title -> scope.launch { snack.showSnackbar("Recurso '$title' eliminado (si las reglas de Firebase lo permiten).") } }
            )
        }
    }
}

// ----------------------------------------------------------------------
// FUNCIONES AUXILIARES DE RECURSOS Y GUARDADO
// ----------------------------------------------------------------------

/**
 * Componente que demuestra el permiso de eliminación basado en roles.
 */
@Composable
fun ResourceItem(
    resourceTitle: String,
    resourceUploaderUid: String,
    currentUserRole: String,
    onDelete: (String) -> Unit
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUserId = auth.currentUser?.uid

    // Permiso de eliminación:
    // 1. Es profesor.
    // 2. O subió el recurso.
    val canDelete = (currentUserRole == "professor") || (currentUserId == resourceUploaderUid)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = resourceTitle,
            modifier = Modifier.weight(1f)
        )

        if (canDelete) {
            IconButton(
                onClick = { onDelete(resourceTitle) },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Eliminar Recurso")
            }
        } else {
            // Un estudiante o un usuario sin permisos solo puede ver
            Text("Solo ver", style = MaterialTheme.typography.bodySmall)
        }
    }
}


/**
 * Copia el contenido de cualquier URI externa (Galería)
 * a un archivo temporal seguro en el caché de la aplicación.
 */
private fun copyUriToAppStorage(context: Context, uri: Uri): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)

        val tempFile = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")

        if (inputStream != null) {
            val outputStream = FileOutputStream(tempFile)

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            tempFile
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Función auxiliar para borrar el archivo temporal creado por FileProvider
 */
private fun deleteFileFromUri(context: Context, uri: Uri) {
    try {
        val file = uri.path?.let { File(it) }
        file?.let {
            if (it.exists()) {
                val ignored = it.delete()
            }
        }
    } catch (_: Exception) {
        // Ignorar si falla la eliminación
    }
}

/**
 * 💾 Guarda el archivo en la galería del dispositivo (storage local).
 */
private fun saveAvatarLocally(
    uri: Uri,
    context: Context
): Boolean {
    val resolver: ContentResolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "MiAvatar_${System.currentTimeMillis()}.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/EducanetAvatars")
    }

    var outputStream: OutputStream? = null
    var success = false

    try {
        val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        if (imageUri != null) {
            outputStream = resolver.openOutputStream(imageUri)
        }

        if (outputStream != null) {
            val inputStream = resolver.openInputStream(uri)

            if (inputStream != null) {
                inputStream.copyTo(outputStream)
                success = true
            }
            inputStream?.close()
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        outputStream?.close()
    }
    return success
}

/**
 * 🔍 Obtiene la URI de la última imagen guardada en MediaStore para mostrarla.
 */
private fun getLatestImageUri(context: Context): Uri? {
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val projection = arrayOf(MediaStore.Images.Media._ID)
    val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

    context.contentResolver.query(
        collection,
        projection,
        null,
        null,
        sortOrder
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val id = cursor.getLong(idColumn)
            return Uri.withAppendedPath(collection, id.toString())
        }
    }
    return null
}