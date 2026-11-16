package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditLog
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.data.model.OfficeDocument
import com.example.data.model.OfficeInvite
import com.example.data.model.PayrollProfile
import com.example.data.model.SalaryAdjustment
import com.example.data.model.TeaLog
import com.example.data.model.TeaPurchase
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class OfficeRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth,
) {
  constructor(context: Context) : this(
    createConfiguredFirestore(context.applicationContext),
    Firebase.auth,
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun <T> wrapFlow(
    operationType: OperationType,
    path: String,
    uid: String,
    source: Flow<T>,
  ): Flow<T> =
    source.catch { error ->
      val firestoreError =
        generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
      if (firestoreError == null && error is CancellationException) throw error
      val exceptionToLog = firestoreError ?: (error as? Exception)
      if (exceptionToLog != null && auth.currentUser?.uid == uid) {
        handleFirestoreError(exceptionToLog, operationType, path)
      }
      throw error
    }

  fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
    val path = "users/$uid"
    emitAll(
      wrapFlow(
        OperationType.GET,
        path,
        uid,
        db.collection("users").document(uid).snapshots().map { snap ->
          snap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
        },
      )
    )
  }

  fun observeOffice(officeId: String): Flow<Office?> = flow {
    val uid = requireUserId()
    val path = "offices/$officeId"
    emitAll(
      wrapFlow(
        OperationType.GET,
        path,
        uid,
        db.collection("offices").document(officeId).snapshots().map { snap ->
          snap.toObject(Office::class.java, ServerTimestampBehavior.ESTIMATE)
        },
      )
    )
  }

  fun observeMembers(officeId: String): Flow<List<Member>> = flow {
    val uid = requireUserId()
    val path = "offices/$officeId/members"
    emitAll(
      wrapFlow(
        OperationType.LIST,
        path,
        uid,
        db.collection("offices").document(officeId).collection("members").snapshots().map { snap ->
          snap.toObjects(Member::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedBy { it.displayName.lowercase() }
        },
      )
    )
  }

  fun observeMyMember(officeId: String, uid: String): Flow<Member?> = flow {
    val path = "offices/$officeId/members/$uid"
    emitAll(
      wrapFlow(
        OperationType.GET,
        path,
        uid,
        db.collection("offices").document(officeId).collection("members").document(uid).snapshots()
          .map { snap -> snap.toObject(Member::class.java, ServerTimestampBehavior.ESTIMATE) },
      )
    )
  }

  fun observePayrollProfiles(officeId: String, isManagerOrOwner: Boolean): Flow<List<PayrollProfile>> =
    flow {
      val uid = requireUserId()
      val path = "offices/$officeId/payroll_profiles"
      val query =
        if (isManagerOrOwner) {
          db.collection("offices").document(officeId).collection("payroll_profiles")
        } else {
          db.collection("offices").document(officeId).collection("payroll_profiles")
            .whereEqualTo("userId", uid)
        }
      emitAll(
        wrapFlow(
          OperationType.LIST,
          path,
          uid,
          query.snapshots().map { snap ->
            snap.toObjects(PayrollProfile::class.java, ServerTimestampBehavior.ESTIMATE)
          },
        )
      )
    }

  fun observeAttendance(officeId: String, isManagerOrOwner: Boolean): Flow<List<AttendanceRecord>> =
    flow {
      val uid = requireUserId()
      val path = "offices/$officeId/attendance"
      val query =
        if (isManagerOrOwner) {
          db.collection("offices").document(officeId).collection("attendance")
        } else {
          db.collection("offices").document(officeId).collection("attendance")
            .whereEqualTo("userId", uid)
        }
      emitAll(
        wrapFlow(
          OperationType.LIST,
          path,
          uid,
          query.snapshots().map { snap ->
            snap.toObjects(AttendanceRecord::class.java, ServerTimestampBehavior.ESTIMATE)
              .sortedByDescending { it.checkInEpochMs }
          },
        )
      )
    }

  fun observeLeaves(officeId: String, isManagerOrOwner: Boolean): Flow<List<LeaveRequest>> = flow {
    val uid = requireUserId()
    val path = "offices/$officeId/leaves"
    val query =
      if (isManagerOrOwner) {
        db.collection("offices").document(officeId).collection("leaves")
      } else {
        db.collection("offices").document(officeId).collection("leaves")
          .whereEqualTo("userId", uid)
      }
    emitAll(
      wrapFlow(
        OperationType.LIST,
        path,
        uid,
        query.snapshots().map { snap ->
          snap.toObjects(LeaveRequest::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        },
      )
    )
  }

  fun observeSalaryAdjustments(
    officeId: String,
    isManagerOrOwner: Boolean,
  ): Flow<List<SalaryAdjustment>> = flow {
    val uid = requireUserId()
    val path = "offices/$officeId/salary_adjustments"
    val query =
      if (isManagerOrOwner) {
        db.collection("offices").document(officeId).collection("salary_adjustments")
      } else {
        db.collection("offices").document(officeId).collection("salary_adjustments")
          .whereEqualTo("userId", uid)
      }
    emitAll(
      wrapFlow(
        OperationType.LIST,
        path,
        uid,
        query.snapshots().map { snap ->
          snap.toObjects(SalaryAdjustment::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        },
      )
    )
  }

  fun observeTeaLogs(officeId: String): Flow<List<TeaLog>> = flow {
    val uid = requireUserId()
    val path = "offices/$officeId/tea_logs"
    emitAll(
      wrapFlow(
        OperationType.LIST,
        path,
        uid,
        db.collection("offices").document(officeId).collection("tea_logs").snapshots().map { snap ->
          snap.toObjects(TeaLog::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt?.seconds ?: 0L }
        },
      )
    )
  }

  fun observeTeaPurchases(officeId: String): Flow<List<TeaPurchase>> = flow {
    val uid = requireUserId()
    val path = "offices/$officeId/tea_purchases"
    emitAll(
      wrapFlow(
        OperationType.LIST,
        path,
        uid,
        db.collection("offices").document(officeId).collection("tea_purchases").snapshots()
          .map { snap ->
            snap.toObjects(TeaPurchase::class.java, ServerTimestampBehavior.ESTIMATE)
              .sortedByDescending { it.createdAt?.seconds ?: 0L }
          },
      )
    )
  }

  fun observeDocuments(officeId: String): Flow<List<OfficeDocument>> = flow {
    val uid = requireUserId()
    val path = "offices/$officeId/documents"
    emitAll(
      wrapFlow(
        OperationType.LIST,
        path,
        uid,
        db.collection("offices").document(officeId).collection("documents").snapshots()
          .map { snap ->
            snap.toObjects(OfficeDocument::class.java, ServerTimestampBehavior.ESTIMATE)
              .sortedByDescending { it.createdAt?.seconds ?: 0L }
          },
      )
    )
  }

  fun observeAuditLogs(officeId: String): Flow<List<AuditLog>> = flow {
    val uid = requireUserId()
    val path = "offices/$officeId/audit_logs"
    emitAll(
      wrapFlow(
        OperationType.LIST,
        path,
        uid,
        db.collection("offices").document(officeId).collection("audit_logs").snapshots()
          .map { snap ->
            snap.toObjects(AuditLog::class.java, ServerTimestampBehavior.ESTIMATE)
              .sortedByDescending { it.createdAt?.seconds ?: 0L }
          },
      )
    )
  }

  suspend fun ensureUserProfile(
    displayName: String,
    email: String,
    preferredLanguage: String,
  ): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val existing = docRef.get().await().toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
    if (existing != null) {
      return@runCatching existing
    }
    val safeName = displayName.ifBlank { email.substringBefore("@").ifBlank { "Office Member" } }.take(100)
    val payload =
      mapOf(
        "userId" to uid,
        "displayName" to safeName,
        "email" to email.take(160),
        "activeOfficeId" to "",
        "preferredLanguage" to preferredLanguage.ifBlank { "en" }.take(16),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
    UserProfile(
      id = uid,
      userId = uid,
      displayName = safeName,
      email = email,
      activeOfficeId = "",
      preferredLanguage = preferredLanguage,
    )
  }

  suspend fun updatePreferredLanguage(languageCode: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      docRef
        .update(
          mapOf(
            "preferredLanguage" to languageCode.take(16),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun createOffice(
    officeName: String,
    ownerDisplayName: String,
    ownerEmail: String,
    ownerPhone: String,
    officeLat: Double,
    officeLng: Double,
    geofenceRadiusMeters: Long,
    shiftStartMinutes: Long,
    shiftEndMinutes: Long,
    lateGraceMinutes: Long,
    workingDaysPerMonth: Long,
  ): Result<Office> = runCatching {
    val uid = requireUserId()
    val officeId = "off_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val inviteCode = generateInviteCode()
    val officeRef = db.collection("offices").document(officeId)
    val officePayload =
      mapOf(
        "ownerId" to uid,
        "name" to officeName.trim().take(80),
        "inviteCode" to inviteCode,
        "officeLat" to officeLat,
        "officeLng" to officeLng,
        "geofenceRadiusMeters" to geofenceRadiusMeters.coerceIn(20L, 5000L),
        "strictGeofenceBlock" to false,
        "shiftStartMinutes" to shiftStartMinutes.coerceIn(0L, 1439L),
        "shiftEndMinutes" to shiftEndMinutes.coerceIn(0L, 1439L),
        "lateGraceMinutes" to lateGraceMinutes.coerceIn(0L, 120L),
        "lateMarksPerHalfDay" to 3L,
        "workingDaysPerMonth" to workingDaysPerMonth.coerceIn(1L, 31L),
        "monthEndReportDay" to 30L,
        "ownerReportEmail" to ownerEmail.trim().take(160),
        "ownerWhatsappPhone" to ownerPhone.trim().take(24),
        "teaDefaultPriceInr" to 12.0,
        "coffeeDefaultPriceInr" to 20.0,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      officeRef.set(officePayload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, officeRef.path)
      throw e
    }

    val safeName = ownerDisplayName.ifBlank { "Owner" }.take(100)
    val inviteRef = db.collection("office_invites").document(inviteCode)
    val memberRef = officeRef.collection("members").document(uid)
    val payrollRef = officeRef.collection("payroll_profiles").document(uid)
    val userRef = db.collection("users").document(uid)

    val batch = db.batch()
    batch.set(
      inviteRef,
      mapOf(
        "inviteCode" to inviteCode,
        "officeId" to officeId,
        "officeName" to officeName.trim().take(80),
        "ownerId" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.set(
      memberRef,
      mapOf(
        "userId" to uid,
        "officeId" to officeId,
        "ownerId" to uid,
        "displayName" to safeName,
        "designation" to "Owner",
        "role" to MemberRole.OWNER.wire,
        "active" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.set(
      payrollRef,
      mapOf(
        "userId" to uid,
        "officeId" to officeId,
        "ownerId" to uid,
        "monthlySalaryInr" to 0.0,
        "overtimeHourlyRateInr" to 0.0,
        "phone" to ownerPhone.trim().take(24),
        "email" to ownerEmail.trim().take(160),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.update(
      userRef,
      mapOf(
        "activeOfficeId" to officeId,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    try {
      batch.commit().await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = safeName,
        actorRole = MemberRole.OWNER,
        actionType = "OFFICE_CREATED",
        summary = "Created office $officeName (Invite: $inviteCode)",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, officeRef.path)
      throw e
    }

    Office(
      id = officeId,
      ownerId = uid,
      name = officeName.trim(),
      inviteCode = inviteCode,
      officeLat = officeLat,
      officeLng = officeLng,
      geofenceRadiusMeters = geofenceRadiusMeters,
      shiftStartMinutes = shiftStartMinutes,
      shiftEndMinutes = shiftEndMinutes,
      lateGraceMinutes = lateGraceMinutes,
      workingDaysPerMonth = workingDaysPerMonth,
      ownerReportEmail = ownerEmail,
      ownerWhatsappPhone = ownerPhone,
    )
  }

  suspend fun joinOfficeByInviteCode(
    rawCode: String,
    displayName: String,
    email: String,
    phone: String,
    designation: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val code = rawCode.trim().uppercase()
    require(code.matches(Regex("^[A-Z0-9]{6}$"))) { "Invite code must be 6 letters/digits." }
    val inviteRef = db.collection("office_invites").document(code)
    val invite =
      try {
        inviteRef.get().await().toObject(OfficeInvite::class.java, ServerTimestampBehavior.ESTIMATE)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, inviteRef.path)
        throw e
      } ?: throw IllegalArgumentException("Invalid office invite code.")

    val officeId = invite.officeId
    val safeName = displayName.ifBlank { "Employee" }.take(100)
    val officeRef = db.collection("offices").document(officeId)
    val memberRef = officeRef.collection("members").document(uid)
    val existingMember = memberRef.get().await()

    val batch = db.batch()
    val isNewMember = !existingMember.exists()
    if (isNewMember) {
      batch.set(
        memberRef,
        mapOf(
          "userId" to uid,
          "officeId" to officeId,
          "ownerId" to invite.ownerId,
          "displayName" to safeName,
          "designation" to designation.ifBlank { "Staff" }.take(80),
          "role" to MemberRole.EMPLOYEE.wire,
          "active" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
      batch.set(
        officeRef.collection("payroll_profiles").document(uid),
        mapOf(
          "userId" to uid,
          "officeId" to officeId,
          "ownerId" to invite.ownerId,
          "monthlySalaryInr" to 0.0,
          "overtimeHourlyRateInr" to 0.0,
          "phone" to phone.trim().take(24),
          "email" to email.trim().take(160),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    }
    batch.update(
      db.collection("users").document(uid),
      mapOf(
        "activeOfficeId" to officeId,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    try {
      batch.commit().await()
      if (isNewMember) {
        appendAuditLog(
          officeId = officeId,
          actorUid = uid,
          actorName = safeName,
          actorRole = MemberRole.EMPLOYEE,
          actionType = "MEMBER_JOINED",
          summary = "$safeName joined ${invite.officeName}",
        )
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, memberRef.path)
      throw e
    }
    officeId
  }

  suspend fun leaveActiveOfficeWorkspace(): Result<Unit> = runCatching {
    val uid = requireUserId()
    val userRef = db.collection("users").document(uid)
    try {
      userRef.update(mapOf("activeOfficeId" to "", "updatedAt" to FieldValue.serverTimestamp())).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, userRef.path)
      throw e
    }
  }

  suspend fun updateOfficeSettings(
    office: Office,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val officeRef = db.collection("offices").document(office.id)
    val updates =
      mapOf(
        "name" to office.name.trim().take(80),
        "officeLat" to office.officeLat,
        "officeLng" to office.officeLng,
        "geofenceRadiusMeters" to office.geofenceRadiusMeters.coerceIn(20L, 5000L),
        "strictGeofenceBlock" to office.strictGeofenceBlock,
        "shiftStartMinutes" to office.shiftStartMinutes.coerceIn(0L, 1439L),
        "shiftEndMinutes" to office.shiftEndMinutes.coerceIn(0L, 1439L),
        "lateGraceMinutes" to office.lateGraceMinutes.coerceIn(0L, 120L),
        "lateMarksPerHalfDay" to office.lateMarksPerHalfDay.coerceIn(1L, 10L),
        "workingDaysPerMonth" to office.workingDaysPerMonth.coerceIn(1L, 31L),
        "monthEndReportDay" to office.monthEndReportDay.coerceIn(1L, 31L),
        "ownerReportEmail" to office.ownerReportEmail.trim().take(160),
        "ownerWhatsappPhone" to office.ownerWhatsappPhone.trim().take(24),
        "teaDefaultPriceInr" to office.teaDefaultPriceInr.coerceIn(0.0, 500.0),
        "coffeeDefaultPriceInr" to office.coffeeDefaultPriceInr.coerceIn(0.0, 500.0),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      officeRef.update(updates).await()
      appendAuditLog(
        officeId = office.id,
        actorUid = uid,
        actorName = actorName,
        actorRole = actorRole,
        actionType = "OFFICE_SETTINGS_UPDATED",
        summary = "Updated office settings (${office.name}, geofence ${office.geofenceRadiusMeters}m)",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, officeRef.path)
      throw e
    }
  }

  suspend fun updateMemberRoleAndSalary(
    officeId: String,
    member: Member,
    newRole: MemberRole,
    newDesignation: String,
    active: Boolean,
    monthlySalaryInr: Double,
    overtimeHourlyRateInr: Double,
    phone: String,
    email: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val officeRef = db.collection("offices").document(officeId)
    val memberRef = officeRef.collection("members").document(member.userId)
    val payrollRef = officeRef.collection("payroll_profiles").document(member.userId)
    val batch = db.batch()
    batch.update(
      memberRef,
      mapOf(
        "role" to newRole.wire,
        "designation" to newDesignation.trim().take(80),
        "active" to active,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    val payrollSnap = payrollRef.get().await()
    if (payrollSnap.exists()) {
      batch.update(
        payrollRef,
        mapOf(
          "monthlySalaryInr" to monthlySalaryInr.coerceAtLeast(0.0),
          "overtimeHourlyRateInr" to overtimeHourlyRateInr.coerceAtLeast(0.0),
          "phone" to phone.trim().take(24),
          "email" to email.trim().take(160),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    } else {
      batch.set(
        payrollRef,
        mapOf(
          "userId" to member.userId,
          "officeId" to officeId,
          "ownerId" to member.ownerId,
          "monthlySalaryInr" to monthlySalaryInr.coerceAtLeast(0.0),
          "overtimeHourlyRateInr" to overtimeHourlyRateInr.coerceAtLeast(0.0),
          "phone" to phone.trim().take(24),
          "email" to email.trim().take(160),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        ),
      )
    }
    try {
      batch.commit().await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = actorName,
        actorRole = actorRole,
        actionType = "MEMBER_PAY_UPDATED",
        summary = "Updated ${member.displayName}: role=${newRole.wire}, salary=₹${monthlySalaryInr.toInt()}",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, memberRef.path)
      throw e
    }
  }

  suspend fun checkIn(
    officeId: String,
    record: AttendanceRecord,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "${uid}_${record.dateKey}"
    val docRef = db.collection("offices").document(officeId).collection("attendance").document(docId)
    val payload =
      mapOf(
        "userId" to uid,
        "officeId" to officeId,
        "userName" to record.userName.take(100),
        "dateKey" to record.dateKey.take(16),
        "checkInEpochMs" to record.checkInEpochMs,
        "checkOutEpochMs" to 0L,
        "checkInLat" to record.checkInLat,
        "checkInLng" to record.checkInLng,
        "distanceMeters" to record.distanceMeters,
        "isOutOfRange" to record.isOutOfRange,
        "isMockLocation" to false,
        "status" to record.status,
        "lateMinutes" to record.lateMinutes.coerceAtLeast(0L),
        "overtimeMinutes" to 0L,
        "selfieBase64" to record.selfieBase64.take(240000),
        "note" to record.note.take(300),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      val rangeTag = if (record.isOutOfRange) " [OUT OF RANGE: ${record.distanceMeters.toInt()}m]" else ""
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = record.userName,
        actorRole = actorRole,
        actionType = "CHECK_IN",
        summary = "${record.userName} checked in (${record.status})$rangeTag",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
    docId
  }

  suspend fun checkOut(
    officeId: String,
    record: AttendanceRecord,
    checkOutEpochMs: Long,
    overtimeMinutes: Long,
    note: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("offices").document(officeId).collection("attendance").document(record.id)
    try {
      docRef
        .update(
          mapOf(
            "checkOutEpochMs" to checkOutEpochMs,
            "overtimeMinutes" to overtimeMinutes.coerceIn(0L, 1440L),
            "note" to note.take(300),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = actorName,
        actorRole = actorRole,
        actionType = "CHECK_OUT",
        summary = "$actorName checked out (OT: ${overtimeMinutes}m)",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun submitLeave(
    officeId: String,
    leave: LeaveRequest,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "lv_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val docRef = db.collection("offices").document(officeId).collection("leaves").document(docId)
    val payload =
      mapOf(
        "userId" to uid,
        "officeId" to officeId,
        "userName" to leave.userName.take(100),
        "leaveType" to leave.leaveType,
        "startDateKey" to leave.startDateKey.take(16),
        "endDateKey" to leave.endDateKey.take(16),
        "daysCount" to leave.daysCount.coerceIn(0.5, 60.0),
        "reason" to leave.reason.trim().take(400),
        "status" to "pending",
        "reviewedBy" to "",
        "reviewNote" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = leave.userName,
        actorRole = actorRole,
        actionType = "LEAVE_REQUESTED",
        summary = "${leave.userName} requested ${leave.daysCount}d ${leave.leaveType} leave (${leave.startDateKey})",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
    docId
  }

  suspend fun reviewLeave(
    officeId: String,
    leave: LeaveRequest,
    approved: Boolean,
    reviewNote: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("offices").document(officeId).collection("leaves").document(leave.id)
    val nextStatus = if (approved) "approved" else "rejected"
    try {
      docRef
        .update(
          mapOf(
            "status" to nextStatus,
            "reviewedBy" to uid,
            "reviewNote" to reviewNote.trim().take(300),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = actorName,
        actorRole = actorRole,
        actionType = if (approved) "LEAVE_APPROVED" else "LEAVE_REJECTED",
        summary = "$actorName $nextStatus ${leave.userName}'s leave (${leave.startDateKey})",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun addSalaryAdjustment(
    officeId: String,
    targetUserId: String,
    targetUserName: String,
    monthKey: String,
    type: String,
    amountInr: Double,
    note: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "adj_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val docRef =
      db.collection("offices").document(officeId).collection("salary_adjustments").document(docId)
    val payload =
      mapOf(
        "userId" to targetUserId,
        "officeId" to officeId,
        "monthKey" to monthKey.take(10),
        "type" to type,
        "amountInr" to amountInr.coerceAtLeast(1.0),
        "note" to note.trim().take(300),
        "addedBy" to uid,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = actorName,
        actorRole = actorRole,
        actionType = "SALARY_ADJUSTMENT",
        summary = "Added $type ₹${amountInr.toInt()} for $targetUserName ($monthKey)",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
    docId
  }

  suspend fun logTeaOrCoffee(
    officeId: String,
    userName: String,
    item: String,
    quantity: Long,
    unitCostInr: Double,
    dateKey: String,
    monthKey: String,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "tea_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val docRef = db.collection("offices").document(officeId).collection("tea_logs").document(docId)
    val payload =
      mapOf(
        "userId" to uid,
        "officeId" to officeId,
        "userName" to userName.take(100),
        "item" to item,
        "quantity" to quantity.coerceIn(1L, 20L),
        "unitCostInr" to unitCostInr.coerceAtLeast(0.0),
        "dateKey" to dateKey.take(16),
        "monthKey" to monthKey.take(10),
        "createdAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = userName,
        actorRole = actorRole,
        actionType = "TEA_LOGGED",
        summary = "$userName logged $quantity x $item",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
    docId
  }

  suspend fun deleteTeaLog(officeId: String, logId: String): Result<Unit> = runCatching {
    requireUserId()
    val docRef = db.collection("offices").document(officeId).collection("tea_logs").document(logId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  suspend fun addTeaPurchase(
    officeId: String,
    addedByName: String,
    category: String,
    quantityLabel: String,
    amountInr: Double,
    dateKey: String,
    monthKey: String,
    note: String,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "pur_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val docRef =
      db.collection("offices").document(officeId).collection("tea_purchases").document(docId)
    val payload =
      mapOf(
        "officeId" to officeId,
        "addedBy" to uid,
        "addedByName" to addedByName.take(100),
        "category" to category,
        "quantityLabel" to quantityLabel.trim().take(60),
        "amountInr" to amountInr.coerceAtLeast(1.0),
        "dateKey" to dateKey.take(16),
        "monthKey" to monthKey.take(10),
        "note" to note.trim().take(300),
        "createdAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = addedByName,
        actorRole = actorRole,
        actionType = "PANTRY_PURCHASE",
        summary = "Recorded $category ($quantityLabel) ₹${amountInr.toInt()}",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
    docId
  }

  suspend fun addDocument(
    officeId: String,
    document: OfficeDocument,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = "doc_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val docRef = db.collection("offices").document(officeId).collection("documents").document(docId)
    val payload =
      mapOf(
        "officeId" to officeId,
        "createdBy" to uid,
        "createdByName" to document.createdByName.take(100),
        "serialNo" to document.serialNo.trim().take(40),
        "direction" to document.direction,
        "dateKey" to document.dateKey.take(16),
        "partyName" to document.partyName.trim().take(120),
        "subject" to document.subject.trim().take(200),
        "status" to document.status,
        "scanBase64" to document.scanBase64.take(280000),
        "note" to document.note.trim().take(400),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    try {
      docRef.set(payload).await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = document.createdByName,
        actorRole = actorRole,
        actionType = "DOCUMENT_ADDED",
        summary = "Added ${document.direction} ${document.serialNo}: ${document.subject}",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
    docId
  }

  suspend fun updateDocumentStatus(
    officeId: String,
    doc: OfficeDocument,
    newStatus: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("offices").document(officeId).collection("documents").document(doc.id)
    try {
      docRef
        .update(
          mapOf(
            "status" to newStatus,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      appendAuditLog(
        officeId = officeId,
        actorUid = uid,
        actorName = actorName,
        actorRole = actorRole,
        actionType = "DOCUMENT_UPDATED",
        summary = "Updated ${doc.serialNo} status to $newStatus",
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  suspend fun appendAuditLog(
    officeId: String,
    actorUid: String,
    actorName: String,
    actorRole: MemberRole,
    actionType: String,
    summary: String,
  ) {
    val logId = "aud_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val logRef = db.collection("offices").document(officeId).collection("audit_logs").document(logId)
    val payload =
      mapOf(
        "officeId" to officeId,
        "actorUid" to actorUid,
        "actorName" to actorName.ifBlank { "Member" }.take(100),
        "actorRole" to actorRole.wire,
        "actionType" to actionType.take(60),
        "summary" to summary.take(400),
        "createdAt" to FieldValue.serverTimestamp(),
      )
    try {
      logRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, logRef.path)
    }
  }

  companion object {
    private fun createConfiguredFirestore(appContext: Context): FirebaseFirestore {
      val dbId = appContext.getString(R.string.firestore_database_id)
      val instance = FirebaseFirestore.getInstance(dbId)
      try {
        instance.firestoreSettings =
          FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
            .build()
      } catch (_: IllegalStateException) {
        // Already configured once in this process
      }
      return instance
    }

    private fun generateInviteCode(): String {
      val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
      return (1..6).map { chars.random() }.joinToString("")
    }
  }
}
