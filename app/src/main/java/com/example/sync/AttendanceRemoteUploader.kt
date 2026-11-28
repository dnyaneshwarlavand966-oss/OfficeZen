// OfficeZen Firestore uploader for pending Room attendance records (rules-aligned)
package com.example.sync

import android.content.Context
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.Office
import com.example.data.repository.OfficeRepository
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

interface AttendanceRemoteUploader {
  fun isAuthenticated(): Boolean

  fun currentUserId(): String?

  suspend fun uploadAttendanceRecord(record: AttendanceRecord, office: Office?): Result<Unit>
}

class FirebaseAttendanceUploader(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) : AttendanceRemoteUploader {

  // CRITICAL: Always initialize Firestore with R.string.firestore_database_id
  constructor(context: Context) : this(
    db =
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      ),
    auth = Firebase.auth,
  )

  override fun isAuthenticated(): Boolean = !auth.currentUser?.uid.isNullOrBlank()

  override fun currentUserId(): String? = auth.currentUser?.uid?.takeIf { it.isNotBlank() }

  private fun requireUserId(): String =
    currentUserId()
      ?: throw IllegalStateException("User must sign in with Google before syncing attendance to Firestore.")

  override suspend fun uploadAttendanceRecord(record: AttendanceRecord, office: Office?): Result<Unit> {
    var activePath = "offices/${record.officeId}/attendance/${record.id}"
    return try {
      val uid = requireUserId()
      val officeId = sanitizeFirestoreId(record.officeId)
      require(officeId.isNotEmpty()) { "Invalid officeId for attendance sync" }

      ensureOfficeAndMembership(uid = uid, officeId = officeId, office = office, fallbackUserName = record.userName)

      val targetUserId =
        if (record.userId == OfficeRepository.LOCAL_DEFAULT_USER_ID || record.userId.isBlank()) {
          uid
        } else {
          record.userId
        }
      val safeDateKey = record.dateKey.trim().ifBlank { "unknown" }.take(16)
      val attendanceDocId =
        if (record.userId == OfficeRepository.LOCAL_DEFAULT_USER_ID) {
          sanitizeFirestoreId("${uid}_${safeDateKey}")
        } else {
          sanitizeFirestoreId(record.id.ifBlank { "${targetUserId}_${safeDateKey}" })
        }

      val docRef =
        db.collection("offices")
          .document(officeId)
          .collection("attendance")
          .document(attendanceDocId)
      activePath = docRef.path

      val safeStatus =
        if (record.status in VALID_ATTENDANCE_STATUSES) record.status else "on_time"
      val safeSelfie = record.selfieBase64.take(240_000)
      val safeNote = record.note.take(300)
      val safeCheckInMs = if (record.checkInEpochMs > 0L) record.checkInEpochMs else System.currentTimeMillis()
      val safeCheckOutMs = record.checkOutEpochMs.coerceAtLeast(0L)
      val safeLateMins = record.lateMinutes.coerceIn(0L, 1440L)
      val safeOvertimeMins = record.overtimeMinutes.coerceIn(0L, 1440L)
      val safeLat = record.checkInLat.coerceIn(-90.0, 90.0)
      val safeLng = record.checkInLng.coerceIn(-180.0, 180.0)
      val safeDistance = record.distanceMeters.coerceAtLeast(0.0)

      val existing =
        try {
          docRef.get().await()
        } catch (_: Exception) {
          null
        }

      if (existing != null && existing.exists()) {
        val updatePayload =
          mapOf(
            "checkOutEpochMs" to safeCheckOutMs,
            "overtimeMinutes" to safeOvertimeMins,
            "note" to safeNote,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(updatePayload).await()
      } else {
        // Exact 18 fields enforced by firestore.rules isValidAttendance + keys().hasAll/hasOnly (no "id")
        val createPayload =
          mapOf(
            "userId" to targetUserId,
            "officeId" to officeId,
            "userName" to record.userName.trim().ifBlank { "Staff Member" }.take(100),
            "dateKey" to safeDateKey,
            "checkInEpochMs" to safeCheckInMs,
            "checkOutEpochMs" to safeCheckOutMs,
            "checkInLat" to safeLat,
            "checkInLng" to safeLng,
            "distanceMeters" to safeDistance,
            "isOutOfRange" to record.isOutOfRange,
            "isMockLocation" to false,
            "status" to safeStatus,
            "lateMinutes" to safeLateMins,
            "overtimeMinutes" to safeOvertimeMins,
            "selfieBase64" to safeSelfie,
            "note" to safeNote,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(createPayload).await()
      }

      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, activePath)
      Result.failure(e)
    }
  }

  private suspend fun ensureOfficeAndMembership(
    uid: String,
    officeId: String,
    office: Office?,
    fallbackUserName: String,
  ) {
    val officeRef = db.collection("offices").document(officeId)
    val officeSnap =
      try {
        officeRef.get().await()
      } catch (_: Exception) {
        null
      }

    if (officeSnap == null || !officeSnap.exists()) {
      val cleanInvite =
        office?.inviteCode
          ?.trim()
          ?.uppercase()
          ?.filter { it in 'A'..'Z' || it in '0'..'9' }
          ?.padEnd(6, '0')
          ?.take(6)
          ?: "ZEN001"
      val officePayload =
        mapOf(
          "ownerId" to uid,
          "name" to (office?.name?.trim()?.ifBlank { "OfficeZen Workspace" } ?: "OfficeZen Workspace").take(80),
          "inviteCode" to cleanInvite,
          "officeLat" to (office?.officeLat ?: 0.0).coerceIn(-90.0, 90.0),
          "officeLng" to (office?.officeLng ?: 0.0).coerceIn(-180.0, 180.0),
          "geofenceRadiusMeters" to (office?.geofenceRadiusMeters ?: 150L).coerceIn(20L, 5000L),
          "strictGeofenceBlock" to (office?.strictGeofenceBlock ?: false),
          "shiftStartMinutes" to (office?.shiftStartMinutes ?: 570L).coerceIn(0L, 1439L),
          "shiftEndMinutes" to (office?.shiftEndMinutes ?: 1110L).coerceIn(0L, 1439L),
          "lateGraceMinutes" to (office?.lateGraceMinutes ?: 60L).coerceIn(0L, 120L),
          "lateMarksPerHalfDay" to (office?.lateMarksPerHalfDay ?: 3L).coerceIn(1L, 10L),
          "workingDaysPerMonth" to (office?.workingDaysPerMonth ?: 26L).coerceIn(1L, 31L),
          "monthEndReportDay" to (office?.monthEndReportDay ?: 30L).coerceIn(1L, 31L),
          "ownerReportEmail" to (office?.ownerReportEmail ?: "").take(160),
          "ownerWhatsappPhone" to (office?.ownerWhatsappPhone ?: "").take(24),
          "teaDefaultPriceInr" to (office?.teaDefaultPriceInr ?: 12.0).coerceIn(0.0, 500.0),
          "coffeeDefaultPriceInr" to (office?.coffeeDefaultPriceInr ?: 20.0).coerceIn(0.0, 500.0),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      officeRef.set(officePayload).await()
    }

    val memberRef = officeRef.collection("members").document(uid)
    val memberSnap =
      try {
        memberRef.get().await()
      } catch (_: Exception) {
        null
      }
    if (memberSnap == null || !memberSnap.exists()) {
      val ownerId = officeSnap?.getString("ownerId")?.takeIf { it.isNotBlank() } ?: uid
      val role = if (ownerId == uid) "owner" else "employee"
      val memberPayload =
        mapOf(
          "userId" to uid,
          "officeId" to officeId,
          "ownerId" to ownerId,
          "displayName" to
            (auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
              ?: fallbackUserName.takeIf { it.isNotBlank() }
              ?: "Office Member").take(100),
          "designation" to if (role == "owner") "Owner" else "Staff",
          "role" to role,
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      memberRef.set(memberPayload).await()
    }
  }

  companion object {
    private val VALID_ATTENDANCE_STATUSES = setOf("on_time", "late", "half_day")

    fun sanitizeFirestoreId(raw: String): String =
      raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
  }
}
