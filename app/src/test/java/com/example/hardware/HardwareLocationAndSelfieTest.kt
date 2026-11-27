// OfficeZen GPS freshness, accuracy, geofence evaluation, and bounded selfie storage tests
package com.example.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.example.domain.GeofenceValidator
import com.example.ui.components.computeAvatarInitials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class HardwareLocationAndSelfieTest {

  @Test
  fun isLocationAcceptable_rejectsStaleCoarseAndNullIslandLocations() {
    val nowNanos = 600_000_000_000L // 600 seconds

    val freshAccurate =
      Location("fused").apply {
        latitude = 18.5204
        longitude = 73.8567
        accuracy = 14f
        elapsedRealtimeNanos = nowNanos - 10_000_000_000L // 10s ago
      }
    assertTrue(
      LocationAndImageHelper.isLocationAcceptable(
        location = freshAccurate,
        nowElapsedNanos = nowNanos,
      )
    )

    val staleLocation =
      Location("fused").apply {
        latitude = 19.0760
        longitude = 72.8777
        accuracy = 15f
        elapsedRealtimeNanos = nowNanos - 300_000_000_000L // 5 min ago (> 2 min limit)
      }
    assertFalse(
      LocationAndImageHelper.isLocationAcceptable(
        location = staleLocation,
        nowElapsedNanos = nowNanos,
      )
    )

    val coarseLocation =
      Location("network").apply {
        latitude = 19.0760
        longitude = 72.8777
        accuracy = 650f
        elapsedRealtimeNanos = nowNanos - 5_000_000_000L
      }
    assertFalse(
      LocationAndImageHelper.isLocationAcceptable(
        location = coarseLocation,
        nowElapsedNanos = nowNanos,
      )
    )

    val nullIsland =
      Location("gps").apply {
        latitude = 0.0
        longitude = 0.0
        accuracy = 10f
        elapsedRealtimeNanos = nowNanos
      }
    assertFalse(
      LocationAndImageHelper.isLocationAcceptable(
        location = nullIsland,
        nowElapsedNanos = nowNanos,
      )
    )
  }

  @Test
  fun evaluateLocation_correctlyIdentifiesInsideAndOutside150mGeofence() {
    val officeLat = 18.52040
    val officeLng = 73.85670

    val nearLoc =
      Location("fused").apply {
        latitude = 18.52085 // ~50m north
        longitude = 73.85670
      }
    val nearCheck = GeofenceValidator.evaluateLocation(nearLoc, officeLat, officeLng, 150L)
    assertTrue(nearCheck.isWithinGeofence)
    assertTrue(nearCheck.distanceMeters in 40.0..65.0)

    val farLoc =
      Location("fused").apply {
        latitude = 19.0760
        longitude = 72.8777
      }
    val farCheck = GeofenceValidator.evaluateLocation(farLoc, officeLat, officeLng, 150L)
    assertFalse(farCheck.isWithinGeofence)
    assertTrue(farCheck.distanceMeters > 100_000.0)
  }

  @Test
  fun bitmapToBoundedBase64_producesDecodableCompactThumbnailUnderLimit() {
    val bmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888)
    for (x in 0 until 640 step 8) {
      for (y in 0 until 480 step 8) {
        bmp.setPixel(x, y, Color.rgb((x * 3) % 255, (y * 5) % 255, ((x + y) * 2) % 255))
      }
    }

    val encoded =
      LocationAndImageHelper.bitmapToBoundedBase64(
        bitmap = bmp,
        maxDimension = 128,
        initialQuality = 55,
        maxBase64Chars = LocationAndImageHelper.MAX_FIRESTORE_SELFIE_B64_CHARS,
      )
    assertTrue("Expected compact Base64 <= 16000 chars, got ${encoded.length}", encoded.length in 100..LocationAndImageHelper.MAX_FIRESTORE_SELFIE_B64_CHARS)

    val decoded = LocationAndImageHelper.decodeBase64(encoded)
    assertNotNull("Bounded Base64 must remain valid decodable JPEG", decoded)
  }

  @Test
  fun saveAndLoadAttendanceSelfie_cachesLocallyAndFallsBackToBase64() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.rgb(20, 120, 90))
    }
    val recordId = "test_uid_2026-11-17"
    val saved = LocationAndImageHelper.saveAttendanceSelfieLocally(context, recordId, bmp)
    assertTrue(saved)

    val loadedLocal = LocationAndImageHelper.loadAttendanceSelfie(context, recordId, fallbackBase64 = "")
    assertNotNull(loadedLocal)
    assertEquals(200, loadedLocal!!.width)

    val thumbB64 = LocationAndImageHelper.bitmapToBoundedBase64(bmp, 96, 55, 8000)
    val loadedFallback =
      LocationAndImageHelper.loadAttendanceSelfie(context, "nonexistent_doc", fallbackBase64 = thumbB64)
    assertNotNull(loadedFallback)
  }

  @Test
  fun computeAvatarInitials_handlesFullSingleAndBlankNames() {
    assertEquals("DL", computeAvatarInitials("Dnyaneshwar Lavand"))
    assertEquals("OO", computeAvatarInitials("Office Owner"))
    assertEquals("RA", computeAvatarInitials("Rahul"))
    assertEquals("AP", computeAvatarInitials("  amit   kumar  patil "))
    assertEquals("OZ", computeAvatarInitials("   "))
  }

  @Test
  fun saveLoadAndDeleteProfilePhoto_fallsBackToNullWhenDeleted() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userId = "local_test_user"
    assertNull(LocationAndImageHelper.loadProfilePhoto(context, userId))

    val bmp = Bitmap.createBitmap(180, 180, Bitmap.Config.ARGB_8888).apply {
      eraseColor(Color.rgb(37, 99, 235))
    }
    assertTrue(LocationAndImageHelper.saveProfilePhoto(context, userId, bmp))

    val loaded = LocationAndImageHelper.loadProfilePhoto(context, userId)
    assertNotNull(loaded)
    assertEquals(180, loaded!!.width)

    assertTrue(LocationAndImageHelper.deleteProfilePhoto(context, userId))
    assertNull(LocationAndImageHelper.loadProfilePhoto(context, userId))
  }
}
