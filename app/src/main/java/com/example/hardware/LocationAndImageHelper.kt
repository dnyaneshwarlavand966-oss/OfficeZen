// OfficeZen location & compressed bitmap utilities
package com.example.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.location.Location
import android.net.Uri
import android.os.SystemClock
import android.util.Base64
import com.example.domain.GeofenceValidator
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

object LocationAndImageHelper {

  private const val GPS_FETCH_TIMEOUT_MS = 10_000L
  private const val MAX_CACHED_LOCATION_AGE_MS = 120_000L
  private const val MAX_ACCEPTABLE_ACCURACY_METERS = 200f
  const val MAX_FIRESTORE_SELFIE_B64_CHARS = 16_000
  const val MAX_FIRESTORE_DOC_SCAN_B64_CHARS = 120_000

  fun isLocationAcceptable(
    location: Location?,
    maxAgeMs: Long = MAX_CACHED_LOCATION_AGE_MS,
    maxAccuracyMeters: Float = MAX_ACCEPTABLE_ACCURACY_METERS,
    nowElapsedNanos: Long = SystemClock.elapsedRealtimeNanos(),
  ): Boolean {
    if (location == null) return false
    if (!GeofenceValidator.isValidCoordinate(location.latitude, location.longitude)) return false
    if (location.latitude == 0.0 && location.longitude == 0.0) return false
    if (location.hasAccuracy() && location.accuracy > maxAccuracyMeters) return false
    if (location.elapsedRealtimeNanos > 0L && nowElapsedNanos > 0L) {
      val ageMs = (nowElapsedNanos - location.elapsedRealtimeNanos) / 1_000_000L
      if (ageMs > maxAgeMs) return false
    }
    return true
  }

  @SuppressLint("MissingPermission")
  suspend fun fetchCurrentLocation(context: Context): Location? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    val cts = CancellationTokenSource()
    return try {
      val fresh =
        withTimeoutOrNull(GPS_FETCH_TIMEOUT_MS) {
          suspendCancellableCoroutine<Location?> { cont ->
            cont.invokeOnCancellation { cts.cancel() }
            client
              .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
              .addOnSuccessListener { loc -> cont.resume(loc) }
              .addOnFailureListener { cont.resume(null) }
          }
        }
      if (fresh == null) {
        cts.cancel()
      }
      if (fresh != null &&
        GeofenceValidator.isValidCoordinate(fresh.latitude, fresh.longitude) &&
        !(fresh.latitude == 0.0 && fresh.longitude == 0.0) &&
        (!fresh.hasAccuracy() || fresh.accuracy <= 250f)
      ) {
        return fresh
      }

      val last =
        try {
          withTimeoutOrNull(2_000L) {
            suspendCancellableCoroutine<Location?> { cont ->
              client.lastLocation
                .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                .addOnCanceledListener { if (cont.isActive) cont.resume(null) }
            }
          }
        } catch (_: Exception) {
          null
        }
      when {
        isLocationAcceptable(last, maxAgeMs = MAX_CACHED_LOCATION_AGE_MS, maxAccuracyMeters = 150f) ->
          last
        fresh != null &&
          GeofenceValidator.isValidCoordinate(fresh.latitude, fresh.longitude) &&
          !(fresh.latitude == 0.0 && fresh.longitude == 0.0) -> fresh
        else -> null
      }
    } catch (_: Exception) {
      cts.cancel()
      val last =
        try {
          withTimeoutOrNull(2_000L) {
            suspendCancellableCoroutine<Location?> { cont ->
              client.lastLocation
                .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                .addOnCanceledListener { if (cont.isActive) cont.resume(null) }
            }
          }
        } catch (_: Exception) {
          null
        }
      if (isLocationAcceptable(last, maxAgeMs = MAX_CACHED_LOCATION_AGE_MS, maxAccuracyMeters = 150f)) {
        last
      } else {
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

  fun bitmapToBoundedBase64(
    bitmap: Bitmap,
    maxDimension: Int = 128,
    initialQuality: Int = 55,
    maxBase64Chars: Int = MAX_FIRESTORE_SELFIE_B64_CHARS,
  ): String {
    var dim = maxDimension.coerceAtLeast(48)
    while (dim >= 48) {
      val scaled = scaleBitmap(bitmap, dim)
      var quality = initialQuality.coerceIn(25, 90)
      while (quality >= 25) {
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
        val encoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        if (encoded.length <= maxBase64Chars) {
          return encoded
        }
        quality -= 15
      }
      dim = (dim * 3) / 4
    }
    return ""
  }

  fun saveAttendanceSelfieLocally(
    context: Context,
    recordId: String,
    bitmap: Bitmap,
  ): Boolean {
    if (recordId.isBlank()) return false
    return try {
      val dir = File(context.filesDir, "attendance_selfies").apply { mkdirs() }
      val safeName = recordId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
      val file = File(dir, "$safeName.jpg")
      val scaled = scaleBitmap(bitmap, 320)
      FileOutputStream(file).use { stream ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  fun loadAttendanceSelfie(
    context: Context,
    recordId: String,
    fallbackBase64: String,
  ): Bitmap? {
    if (recordId.isNotBlank()) {
      try {
        val safeName = recordId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        val file = File(File(context.filesDir, "attendance_selfies"), "$safeName.jpg")
        if (file.exists() && file.length() > 0L) {
          BitmapFactory.decodeFile(file.absolutePath)?.let { return it }
        }
      } catch (_: Exception) {
        // Fallback cleanly to Firestore micro-thumbnail
      }
    }
    return decodeBase64(fallbackBase64)
  }

  fun saveProfilePhotoFromUri(
    context: Context,
    userId: String,
    uri: Uri,
  ): Bitmap? {
    if (userId.isBlank()) return null
    return try {
      val decoded =
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream)
        } ?: return null
      val scaled = scaleBitmap(decoded, 320)
      if (saveProfilePhoto(context, userId, scaled)) scaled else null
    } catch (_: Exception) {
      null
    }
  }

  fun saveProfilePhoto(
    context: Context,
    userId: String,
    bitmap: Bitmap,
  ): Boolean {
    if (userId.isBlank()) return false
    return try {
      val dir = File(context.filesDir, "profile_photos").apply { mkdirs() }
      val safeName = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
      val file = File(dir, "$safeName.jpg")
      val scaled = scaleBitmap(bitmap, 320)
      FileOutputStream(file).use { stream ->
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
      }
      true
    } catch (_: Exception) {
      false
    }
  }

  fun loadProfilePhoto(
    context: Context,
    userId: String,
  ): Bitmap? {
    if (userId.isBlank()) return null
    return try {
      val safeName = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
      val file = File(File(context.filesDir, "profile_photos"), "$safeName.jpg")
      if (file.exists() && file.length() > 0L) {
        BitmapFactory.decodeFile(file.absolutePath)
      } else {
        null
      }
    } catch (_: Exception) {
      null
    }
  }

  fun deleteProfilePhoto(
    context: Context,
    userId: String,
  ): Boolean {
    if (userId.isBlank()) return false
    return try {
      val safeName = userId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
      val file = File(File(context.filesDir, "profile_photos"), "$safeName.jpg")
      if (file.exists()) file.delete() else true
    } catch (_: Exception) {
      false
    }
  }

  fun uriToBase64(
    context: Context,
    uri: Uri,
    maxDimension: Int = 640,
    quality: Int = 70,
    maxBase64Chars: Int = MAX_FIRESTORE_DOC_SCAN_B64_CHARS,
  ): String? {
    return try {
      context.contentResolver.openInputStream(uri)?.use { stream ->
        val decoded = BitmapFactory.decodeStream(stream) ?: return null
        bitmapToBoundedBase64(decoded, maxDimension, quality, maxBase64Chars)
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
