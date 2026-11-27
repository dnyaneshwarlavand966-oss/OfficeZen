// OfficeZen GPS geofence & mock-location validator
package com.example.domain

import android.location.Location
import android.os.Build
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeofenceCheckResult(
  val distanceMeters: Double,
  val isWithinGeofence: Boolean,
  val isMockLocation: Boolean,
)

object GeofenceValidator {
  private const val EARTH_RADIUS_METERS = 6371000.0

  fun isValidCoordinate(lat: Double, lng: Double): Boolean =
    !lat.isNaN() &&
      !lng.isNaN() &&
      !lat.isInfinite() &&
      !lng.isInfinite() &&
      lat in -90.0..90.0 &&
      lng in -180.0..180.0

  fun isUnsetOfficeCoordinate(lat: Double, lng: Double): Boolean =
    !isValidCoordinate(lat, lng) || (lat == 0.0 && lng == 0.0)

  fun haversineDistanceMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
  ): Double {
    if (!isValidCoordinate(lat1, lng1) || !isValidCoordinate(lat2, lng2)) {
      return Double.NaN
    }
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a =
      (sin(dLat / 2).pow(2) +
        cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2))
        .coerceIn(0.0, 1.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
  }

  @Suppress("DEPRECATION")
  fun isLocationMocked(location: Location): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      location.isMock
    } else {
      location.isFromMockProvider
    }
  }

  fun evaluateLocation(
    location: Location,
    officeLat: Double,
    officeLng: Double,
    radiusMeters: Long,
  ): GeofenceCheckResult {
    val distance = haversineDistanceMeters(location.latitude, location.longitude, officeLat, officeLng)
    val mocked = isLocationMocked(location)
    val within = !distance.isNaN() && distance <= radiusMeters.toDouble()
    return GeofenceCheckResult(
      distanceMeters = if (distance.isNaN()) 0.0 else distance,
      isWithinGeofence = within,
      isMockLocation = mocked,
    )
  }
}
