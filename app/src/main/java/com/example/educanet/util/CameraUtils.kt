package com.example.educanet.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CameraUtils {

    fun createImageUri(context: Context): Pair<Uri, File> {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, "IMG_${timeStamp}.jpg")
        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            imageFile
        )
        return imageUri to imageFile
    }
}
