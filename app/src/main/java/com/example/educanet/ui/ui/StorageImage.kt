// com/example/educanet/ui/StorageImage.kt (o al final de HomeScreen.kt)
package com.example.educanet.ui.ui

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

@Composable
fun StorageImage(
    pathOrUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val ctx = LocalContext.current
    var resolved by remember(pathOrUrl) { mutableStateOf<String?>(null) }

    LaunchedEffect(pathOrUrl) {
        if (pathOrUrl.isBlank()) { resolved = null; return@LaunchedEffect }
        try {
            resolved = when {
                pathOrUrl.startsWith("http") -> pathOrUrl
                pathOrUrl.startsWith("gs://") ->
                    FirebaseStorage.getInstance().getReferenceFromUrl(pathOrUrl).downloadUrl.await().toString()
                else ->
                    FirebaseStorage.getInstance().reference.child(pathOrUrl).downloadUrl.await().toString()
            }
        } catch (_: Exception) {
            resolved = null
        }
    }

    AsyncImage(
        model = ImageRequest.Builder(ctx).data(resolved).crossfade(true).build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale
    )
}
