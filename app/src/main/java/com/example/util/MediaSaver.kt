package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MediaSaver {

    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        sceneName: String,
        isEnhanced: Boolean
    ): Uri? {
        val prefix = if (isEnhanced) "SMART_AUTO" else "AUTO"
        return saveBitmapToGallery(
            context = context,
            bitmap = bitmap,
            prefix = prefix,
            profileName = sceneName.uppercase()
        )
    }

    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        prefix: String = "AI_ENHANCED",
        profileName: String = "NATURAL"
    ): Uri? = withContext(Dispatchers.IO) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        val fileName = "${prefix}_${profileName}_$timeStamp.jpg"

        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/AISmartCamera")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        var imageUri: Uri? = null
        try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            imageUri = resolver.insert(collection, contentValues)
            if (imageUri != null) {
                resolver.openOutputStream(imageUri)?.use { outputStream: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 98, outputStream)
                    outputStream.flush()
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            AppLogger.addLog("STORAGE", "Failed to save to MediaStore: ${e.message}", "ERROR")

            // Fallback for devices where MediaStore fails: save to app external files dir
            try {
                val picturesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "AISmartCamera").apply { mkdirs() }
                val file = File(appDir, fileName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 98, out)
                    out.flush()
                }
                imageUri = Uri.fromFile(file)
            } catch (fallbackEx: Exception) {
                fallbackEx.printStackTrace()
                AppLogger.addLog("STORAGE", "Fallback save failed: ${fallbackEx.message}", "ERROR")
            }
        }

        imageUri
    }
}
