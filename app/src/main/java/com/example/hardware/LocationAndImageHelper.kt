// OfficeZen location & compressed bitmap utilities
package com.example.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.net.Uri
import android.util.Base64
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

object LocationAndImageHelper {

  @SuppressLint("MissingPermission")
  suspend fun fetchCurrentLocation(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()
    return try {
      val fresh =
        suspendCancellableCoroutine<Location?> { cont ->
          cont.invokeOnCancellation { cts.cancel() }
          client
            .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
            .addOnSuccessListener { loc -> cont.resume(loc) }
            .addOnFailureListener { cont.resume(null) }
        }
      fresh ?: client.lastLocation.await()
    } catch (_: Exception) {
      try {
        client.lastLocation.await()
      } catch (_: Exception) {
        null
      }
    }
  }

  fun bitmapToBase64(bitmap: Bitmap, maxDimension: Int = 360, quality: Int = 65): String {
    val scaled = scaleBitmap(bitmap, maxDimension)
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
  }

  fun uriToBase64(context: Context, uri: Uri, maxDimension: Int = 640, quality: Int = 70): String? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val decoded = BitmapFactory.decodeStream(stream) ?: return null
        bitmapToBase64(decoded, maxDimension, quality)
      }
    } catch (_: Exception) {
      null
    }
  }

  fun decodeBase64(base64: String): Bitmap? {
    if (base64.isBlank()) return null
    return try {
      val bytes = Base64.decode(base64, Base64.DEFAULT)
      BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (_: Exception) {
      null
    }
  }

  fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    if (degrees == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
  }

  private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDimension && h <= maxDimension) return bitmap
    val ratio = w.toFloat() / h.toFloat()
    val targetW: Int
    val targetH: Int
    if (w >= h) {
      targetW = maxDimension
      targetH = (maxDimension / ratio).toInt().coerceAtLeast(1)
    } else {
      targetH = maxDimension
      targetW = (maxDimension * ratio).toInt().coerceAtLeast(1)
    }
    return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
  }
}
