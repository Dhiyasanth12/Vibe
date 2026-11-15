package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream

object ImageUtils {
    private const val DATA_URI_PREFIX = "data:image/jpeg;base64,"

    fun uriToCompressedDataUri(
        context: Context,
        uri: Uri,
        maxDimension: Int = 720,
        quality: Int = 68
    ): String? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            }
            val origW = boundsOpts.outWidth
            val origH = boundsOpts.outHeight
            if (origW <= 0 || origH <= 0) return null

            var inSampleSize = 1
            while (origW / inSampleSize > maxDimension * 2 || origH / inSampleSize > maxDimension * 2) {
                inSampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            val sampled = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            val scale = minOf(
                maxDimension.toFloat() / sampled.width.toFloat(),
                maxDimension.toFloat() / sampled.height.toFloat(),
                1f
            )
            val finalBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    sampled,
                    (sampled.width * scale).toInt().coerceAtLeast(1),
                    (sampled.height * scale).toInt().coerceAtLeast(1),
                    true
                ).also {
                    if (it !== sampled) sampled.recycle()
                }
            } else {
                sampled
            }

            val baos = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            finalBitmap.recycle()
            val bytes = baos.toByteArray()
            DATA_URI_PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    fun decodeDataUriToImageBitmap(dataUri: String): ImageBitmap? {
        if (!dataUri.startsWith("data:image")) return null
        return try {
            val base64Part = dataUri.substringAfter("base64,", "")
            if (base64Part.isBlank()) return null
            val bytes = Base64.decode(base64Part, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }
}
