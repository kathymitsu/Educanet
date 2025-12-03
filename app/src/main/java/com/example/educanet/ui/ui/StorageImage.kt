package com.example.educanet.ui.ui

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale

/**
 * Muestra una imagen por URL (https). Si es vacía o falla, usa un placeholder.
 * NO toca Storage directamente (ya guardamos el downloadUrl en Firestore).
 */
@Composable
fun StorageImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderUrl: String =
    // súbela a tu Storage (p.ej. defaults/placeholder.jpg) y pega su URL pública:
        "https://via.placeholder.com/800x400?text=Sin+portada"
) {
    val ctx = LocalContext.current
    var effective by remember(url) { mutableStateOf(url?.takeIf { it.startsWith("http") && it.isNotBlank() }) }

    LaunchedEffect(url) {
        if (url.isNullOrBlank() || !url.startsWith("http")) {
            effective = placeholderUrl
        } else {
            effective = url
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(ctx)
            .data(effective ?: placeholderUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        onError = {
            Log.e("StorageImage", "Fallo cargando imagen: ${it.result.throwable.message}")
        }
    )
}
