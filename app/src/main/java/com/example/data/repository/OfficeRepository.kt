// OfficeZen purely local Room SQLite repository (100% offline, zero login/cloud required)
package com.example.data.repository

import android.content.Context
import com.example.data.local.AuditLogEntity
import com.example.data.local.OfficeDao
import com.example.data.local.OfficeDatabase
import com.example.data.local.toEntity
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSyncStatus
import com.example.data.model.AuditLog
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.data.model.OfficeDocument
import com.example.data.model.PayrollProfile
import com.example.data.model.SalaryAdjustment
import com.example.data.model.TeaLog
import com.example.data.model.TeaPurchase
import com.example.data.model.UserProfile
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class OfficeRepository(
  private val dao: OfficeDao,
) {
  constructor(context: Context) : this(OfficeDatabase.getInstance(context.applicationContext).officeDao())

  fun observeUserProfile(uid: String): Flow<UserProfile?> =
    dao.observeUserProfile(uid).map { it?.toDomain() }

  fun observeAllOffices(): Flow<List<Office>> =
    dao.observeAllOffices().map { list -> list.map { it.toDomain() } }

  fun observeOffice(officeId: String): Flow<Office?> =
    dao.observeOffice(officeId).map { it?.toDomain() }

  fun observeMembers(officeId: String): Flow<List<Member>> =
    dao.observeMembers(officeId).map { list -> list.map { it.toDomain() } }

  fun observeMyMember(officeId: String, uid: String): Flow<Member?> =
    dao.observeMyMember(officeId, uid).map { it?.toDomain() }

  fun observePayrollProfiles(officeId: String): Flow<List<PayrollProfile>> =
    dao.observePayrollProfiles(officeId).map { list -> list.map { it.toDomain() } }

  fun observeAttendance(officeId: String): Flow<List<AttendanceRecord>> =
    dao.observeAttendance(officeId).map { list -> list.map { it.toDomain() } }

  fun observePendingAttendanceCount(officeId: String): Flow<Int> =
    dao.observePendingAttendanceCount(officeId)

  fun observeSyncedAttendanceCount(officeId: String): Flow<Int> =
    dao.observeSyncedAttendanceCount(officeId)

  fun observeAllPendingAttendanceCount(): Flow<Int> =
    dao.observeAllPendingAttendanceCount()

  suspend fun getPendingAttendanceRecords(): List<AttendanceRecord> =
    dao.getPendingAttendanceRecords().map { it.toDomain() }

  suspend fun getOfficeById(officeId: String): Office? =
    dao.getOffice(officeId)?.toDomain()

  suspend fun markAttendanceSyncState(
    id: String,
    syncStatus: String,
    attemptEpochMs: Long,
    syncError: String = "",
  ) {
    dao.updateAttendanceSyncState(id, syncStatus, attemptEpochMs, syncError.take(300))
  }

  suspend fun retryFailedAttendance() {
    dao.retryAllFailedAttendance()
  }

  fun observeLeaves(officeId: String): Flow<List<LeaveRequest>> =
    dao.observeLeaves(officeId).map { list -> list.map { it.toDomain() } }

  fun observeSalaryAdjustments(officeId: String): Flow<List<SalaryAdjustment>> =
    dao.observeSalaryAdjustments(officeId).map { list -> list.map { it.toDomain() } }

  fun observeTeaLogs(officeId: String): Flow<List<TeaLog>> =
    dao.observeTeaLogs(officeId).map { list -> list.map { it.toDomain() } }

  fun observeTeaPurchases(officeId: String): Flow<List<TeaPurchase>> =
    dao.observeTeaPurchases(officeId).map { list -> list.map { it.toDomain() } }

  fun observeDocuments(officeId: String): Flow<List<OfficeDocument>> =
    dao.observeDocuments(officeId).map { list -> list.map { it.toDomain() } }

  fun observeAuditLogs(officeId: String): Flow<List<AuditLog>> =
    dao.observeAuditLogs(officeId).map { list -> list.map { it.toDomain() } }

  suspend fun ensureUserProfile(
    uid: String,
    displayName: String,
    email: String = "",
    preferredLanguage: String = "en",
  ): Result<UserProfile> = runCatching {
    val existing = dao.getUserProfile(uid)
    if (existing != null) {
      return@runCatching existing.toDomain()
    }
    val now = System.currentTimeMillis()
    val safeName = displayName.trim().ifBlank { "Office Owner" }.take(100)
    val safeLang = preferredLanguage.ifBlank { "en" }.take(16)
    val profile =
      UserProfile(
        id = uid,
        userId = uid,
        displayName = safeName,
        email = email.take(160),
        activeOfficeId = "",
        preferredLanguage = safeLang,
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertUserProfile(profile.toEntity())
    profile
  }

  suspend fun updatePreferredLanguage(uid: String, languageCode: String): Result<Unit> = runCatching {
    dao.updatePreferredLanguage(uid, languageCode.take(16), System.currentTimeMillis())
  }

  suspend fun switchActiveOffice(uid: String, officeId: String): Result<Unit> = runCatching {
    val existing = dao.getUserProfile(uid)
    val now = System.currentTimeMillis()
    if (existing == null) {
      dao.upsertUserProfile(
        UserProfile(
          id = uid,
          userId = uid,
          displayName = "Office Owner",
          activeOfficeId = officeId,
          preferredLanguage = "en",
          createdAtEpochMs = now,
          updatedAtEpochMs = now,
        ).toEntity()
      )
    } else {
      dao.updateActiveOffice(uid, officeId, now)
    }
  }

  suspend fun createOffice(
    uid: String,
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
    preferredLanguage: String = "en",
  ): Result<Office> = runCatching {
    val trimmedOfficeName = officeName.trim().take(80)
    require(trimmedOfficeName.isNotEmpty()) { "Please enter an office name." }
    val safeName = ownerDisplayName.trim().ifBlank { "Office Owner" }.take(100)
    val now = System.currentTimeMillis()

    val officeId = "off_${UUID.randomUUID().toString().replace("-", "").take(16)}"
    val inviteCode = generateInviteCode()
    val office =
      Office(
        id = officeId,
        ownerId = uid,
        name = trimmedOfficeName,
        inviteCode = inviteCode,
        officeLat = officeLat,
        officeLng = officeLng,
        geofenceRadiusMeters = geofenceRadiusMeters.coerceIn(20L, 5000L),
        strictGeofenceBlock = false,
        shiftStartMinutes = shiftStartMinutes.coerceIn(0L, 1439L),
        shiftEndMinutes = shiftEndMinutes.coerceIn(0L, 1439L),
        lateGraceMinutes = lateGraceMinutes.coerceIn(0L, 120L),
        lateMarksPerHalfDay = 3L,
        workingDaysPerMonth = workingDaysPerMonth.coerceIn(1L, 31L),
        monthEndReportDay = 30L,
        ownerReportEmail = ownerEmail.trim().take(160),
        ownerWhatsappPhone = ownerPhone.trim().take(24),
        teaDefaultPriceInr = 12.0,
        coffeeDefaultPriceInr = 20.0,
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertOffice(office.toEntity())

    val member =
      Member(
        id = "${officeId}_${uid}",
        userId = uid,
        officeId = officeId,
        ownerId = uid,
        displayName = safeName,
        designation = "Owner",
        role = MemberRole.OWNER.wire,
        active = true,
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertMember(member.toEntity())

    val payrollProfile =
      PayrollProfile(
        id = "${officeId}_${uid}",
        userId = uid,
        officeId = officeId,
        ownerId = uid,
        monthlySalaryInr = 0.0,
        overtimeHourlyRateInr = 0.0,
        phone = ownerPhone.trim().take(24),
        email = ownerEmail.trim().take(160),
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertPayrollProfile(payrollProfile.toEntity())

    val existingUser = dao.getUserProfile(uid)
    if (existingUser != null) {
      dao.upsertUserProfile(
        existingUser.copy(
          displayName = safeName,
          activeOfficeId = officeId,
          updatedAtEpochMs = now,
        )
      )
    } else {
      dao.upsertUserProfile(
        UserProfile(
          id = uid,
          userId = uid,
          displayName = safeName,
          email = ownerEmail.trim().take(160),
          activeOfficeId = officeId,
          preferredLanguage = preferredLanguage.ifBlank { "en" }.take(16),
          createdAtEpochMs = now,
          updatedAtEpochMs = now,
        ).toEntity()
      )
    }

    appendAuditLog(
      officeId = officeId,
      actorUid = uid,
      actorName = safeName,
      actorRole = MemberRole.OWNER,
      actionType = "OFFICE_CREATED",
      summary = "Created local office $trimmedOfficeName (Code: $inviteCode)",
    )
    office
  }

  suspend fun joinLocalOfficeByInviteCode(
    uid: String,
    rawCode: String,
    displayName: String,
    phone: String,
    designation: String,
  ): Result<String> = runCatching {
    val code = rawCode.trim().uppercase()
    require(code.matches(Regex("^[A-Z0-9]{6}$"))) { "Invite code must be 6 letters/digits." }
    val office = dao.getOfficeByInviteCode(code)?.toDomain()
      ?: throw IllegalArgumentException("No local office found on this device with code $code.")
    val now = System.currentTimeMillis()
    val safeName = displayName.trim().ifBlank { "Staff Member" }.take(100)
    val existingMember = dao.getMember(office.id, uid)
    if (existingMember == null) {
      dao.upsertMember(
        Member(
          id = "${office.id}_${uid}",
          userId = uid,
          officeId = office.id,
          ownerId = office.ownerId,
          displayName = safeName,
          designation = designation.trim().ifBlank { "Staff" }.take(80),
          role = MemberRole.EMPLOYEE.wire,
          active = true,
          createdAtEpochMs = now,
          updatedAtEpochMs = now,
        ).toEntity()
      )
      dao.upsertPayrollProfile(
        PayrollProfile(
          id = "${office.id}_${uid}",
          userId = uid,
          officeId = office.id,
          ownerId = office.ownerId,
          monthlySalaryInr = 0.0,
          overtimeHourlyRateInr = 0.0,
          phone = phone.trim().take(24),
          createdAtEpochMs = now,
          updatedAtEpochMs = now,
        ).toEntity()
      )
    }
    dao.updateActiveOffice(uid, office.id, now)
    office.id
  }

  suspend fun addLocalMember(
    officeId: String,
    ownerId: String,
    displayName: String,
    designation: String,
    role: MemberRole,
    monthlySalaryInr: Double,
    overtimeHourlyRateInr: Double,
    phone: String,
    actorUid: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Member> = runCatching {
    val safeName = displayName.trim().take(100)
    require(safeName.isNotEmpty()) { "Please enter staff member name." }
    val now = System.currentTimeMillis()
    val newUserId = "staff_${UUID.randomUUID().toString().replace("-", "").take(12)}"
    val member =
      Member(
        id = "${officeId}_${newUserId}",
        userId = newUserId,
        officeId = officeId,
        ownerId = ownerId,
        displayName = safeName,
        designation = designation.trim().ifBlank { "Staff" }.take(80),
        role = role.wire,
        active = true,
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertMember(member.toEntity())

    val profile =
      PayrollProfile(
        id = "${officeId}_${newUserId}",
        userId = newUserId,
        officeId = officeId,
        ownerId = ownerId,
        monthlySalaryInr = monthlySalaryInr.coerceAtLeast(0.0),
        overtimeHourlyRateInr = overtimeHourlyRateInr.coerceAtLeast(0.0),
        phone = phone.trim().take(24),
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertPayrollProfile(profile.toEntity())

    appendAuditLog(
      officeId = officeId,
      actorUid = actorUid,
      actorName = actorName,
      actorRole = actorRole,
      actionType = "MEMBER_ADDED",
      summary = "Added $safeName (${role.wire}, ₹${monthlySalaryInr.toInt()}/mo)",
    )
    member
  }

  suspend fun leaveActiveOfficeWorkspace(uid: String): Result<Unit> = runCatching {
    dao.updateActiveOffice(uid, "", System.currentTimeMillis())
  }

  suspend fun updateOfficeSettings(
    office: Office,
    actorUid: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    val now = System.currentTimeMillis()
    val clean =
      office.copy(
        name = office.name.trim().take(80),
        geofenceRadiusMeters = office.geofenceRadiusMeters.coerceIn(20L, 5000L),
        shiftStartMinutes = office.shiftStartMinutes.coerceIn(0L, 1439L),
        shiftEndMinutes = office.shiftEndMinutes.coerceIn(0L, 1439L),
        lateGraceMinutes = office.lateGraceMinutes.coerceIn(0L, 120L),
        lateMarksPerHalfDay = office.lateMarksPerHalfDay.coerceIn(1L, 10L),
        workingDaysPerMonth = office.workingDaysPerMonth.coerceIn(1L, 31L),
        monthEndReportDay = office.monthEndReportDay.coerceIn(1L, 31L),
        ownerReportEmail = office.ownerReportEmail.trim().take(160),
        ownerWhatsappPhone = office.ownerWhatsappPhone.trim().take(24),
        teaDefaultPriceInr = office.teaDefaultPriceInr.coerceIn(0.0, 500.0),
        coffeeDefaultPriceInr = office.coffeeDefaultPriceInr.coerceIn(0.0, 500.0),
        updatedAtEpochMs = now,
      )
    dao.upsertOffice(clean.toEntity())
    appendAuditLog(
      officeId = clean.id,
      actorUid = actorUid,
      actorName = actorName,
      actorRole = actorRole,
      actionType = "OFFICE_SETTINGS_UPDATED",
      summary = "Updated office settings (${clean.name}, geofence ${clean.geofenceRadiusMeters}m)",
    )
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
    actorUid: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    val now = System.currentTimeMillis()
    dao.upsertMember(
      member.copy(
        id = member.id.ifBlank { "${officeId}_${member.userId}" },
        role = newRole.wire,
        designation = newDesignation.trim().take(80),
        active = active,
        updatedAtEpochMs = now,
      ).toEntity()
    )
    val existingProfile = dao.getPayrollProfile(officeId, member.userId)
    val nextProfile =
      PayrollProfile(
        id = "${officeId}_${member.userId}",
        userId = member.userId,
        officeId = officeId,
        ownerId = member.ownerId,
        monthlySalaryInr = monthlySalaryInr.coerceAtLeast(0.0),
        overtimeHourlyRateInr = overtimeHourlyRateInr.coerceAtLeast(0.0),
        phone = phone.trim().take(24),
        email = email.trim().take(160),
        createdAtEpochMs = existingProfile?.createdAtEpochMs ?: now,
        updatedAtEpochMs = now,
      )
    dao.upsertPayrollProfile(nextProfile.toEntity())
    appendAuditLog(
      officeId = officeId,
      actorUid = actorUid,
      actorName = actorName,
      actorRole = actorRole,
      actionType = "MEMBER_PAY_UPDATED",
      summary = "Updated ${member.displayName}: role=${newRole.wire}, salary=₹${monthlySalaryInr.toInt()}",
    )
  }

  suspend fun checkIn(
    officeId: String,
    record: AttendanceRecord,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val now = System.currentTimeMillis()
    val docId = record.id.ifBlank { "${record.userId}_${record.dateKey}" }
    val safeSelfie = if (record.selfieBase64.length <= 240_000) record.selfieBase64 else ""
    val clean =
      record.copy(
        id = docId,
        officeId = officeId,
        userName = record.userName.take(100),
        dateKey = record.dateKey.take(16),
        isMockLocation = false,
        lateMinutes = record.lateMinutes.coerceAtLeast(0L),
        overtimeMinutes = 0L,
        selfieBase64 = safeSelfie,
        note = record.note.take(300),
        syncStatus = AttendanceSyncStatus.PENDING,
        lastSyncAttemptEpochMs = 0L,
        syncError = "",
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertAttendance(clean.toEntity())
    val rangeTag = if (clean.isOutOfRange) " [OUT OF RANGE: ${clean.distanceMeters.toInt()}m]" else ""
    appendAuditLog(
      officeId = officeId,
      actorUid = clean.userId,
      actorName = clean.userName,
      actorRole = actorRole,
      actionType = "CHECK_IN",
      summary = "${clean.userName} checked in (${clean.status})$rangeTag",
    )
    docId
  }

  suspend fun checkOut(
    officeId: String,
    record: AttendanceRecord,
    checkOutEpochMs: Long,
    overtimeMinutes: Long,
    note: String,
    actorUid: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    val docId = record.id.ifBlank { "${record.userId}_${record.dateKey}" }
    val current = dao.getAttendanceById(docId)?.toDomain() ?: record
    val updated =
      current.copy(
        id = docId,
        officeId = officeId,
        checkOutEpochMs = checkOutEpochMs,
        overtimeMinutes = overtimeMinutes.coerceIn(0L, 1440L),
        note = note.take(300),
        syncStatus = AttendanceSyncStatus.PENDING,
        syncError = "",
        updatedAtEpochMs = System.currentTimeMillis(),
      )
    dao.upsertAttendance(updated.toEntity())
    appendAuditLog(
      officeId = officeId,
      actorUid = actorUid,
      actorName = actorName,
      actorRole = actorRole,
      actionType = "CHECK_OUT",
      summary = "$actorName checked out (OT: ${overtimeMinutes}m)",
    )
  }

  suspend fun submitLeave(
    officeId: String,
    leave: LeaveRequest,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val now = System.currentTimeMillis()
    val docId = "lv_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val clean =
      leave.copy(
        id = docId,
        officeId = officeId,
        userName = leave.userName.take(100),
        startDateKey = leave.startDateKey.take(16),
        endDateKey = leave.endDateKey.take(16),
        daysCount = leave.daysCount.coerceIn(0.5, 60.0),
        reason = leave.reason.trim().take(400),
        status = "pending",
        reviewedBy = "",
        reviewNote = "",
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertLeave(clean.toEntity())
    appendAuditLog(
      officeId = officeId,
      actorUid = clean.userId,
      actorName = clean.userName,
      actorRole = actorRole,
      actionType = "LEAVE_REQUESTED",
      summary = "${clean.userName} requested ${clean.daysCount}d ${clean.leaveType} leave (${clean.startDateKey})",
    )
    docId
  }

  suspend fun reviewLeave(
    officeId: String,
    leave: LeaveRequest,
    approved: Boolean,
    reviewNote: String,
    actorUid: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    val nextStatus = if (approved) "approved" else "rejected"
    dao.upsertLeave(
      leave.copy(
        officeId = officeId,
        status = nextStatus,
        reviewedBy = actorUid,
        reviewNote = reviewNote.trim().take(300),
        updatedAtEpochMs = System.currentTimeMillis(),
      ).toEntity()
    )
    appendAuditLog(
      officeId = officeId,
      actorUid = actorUid,
      actorName = actorName,
      actorRole = actorRole,
      actionType = if (approved) "LEAVE_APPROVED" else "LEAVE_REJECTED",
      summary = "$actorName $nextStatus ${leave.userName}'s leave (${leave.startDateKey})",
    )
  }

  suspend fun addSalaryAdjustment(
    officeId: String,
    targetUserId: String,
    targetUserName: String,
    monthKey: String,
    type: String,
    amountInr: Double,
    note: String,
    actorUid: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val now = System.currentTimeMillis()
    val docId = "adj_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val adj =
      SalaryAdjustment(
        id = docId,
        userId = targetUserId,
        officeId = officeId,
        monthKey = monthKey.take(10),
        type = type,
        amountInr = amountInr.coerceAtLeast(1.0),
        note = note.trim().take(300),
        addedBy = actorUid,
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertSalaryAdjustment(adj.toEntity())
    appendAuditLog(
      officeId = officeId,
      actorUid = actorUid,
      actorName = actorName,
      actorRole = actorRole,
      actionType = "SALARY_ADJUSTMENT",
      summary = "Added $type ₹${amountInr.toInt()} for $targetUserName ($monthKey)",
    )
    docId
  }

  suspend fun logTeaOrCoffee(
    officeId: String,
    userId: String,
    userName: String,
    item: String,
    quantity: Long,
    unitCostInr: Double,
    dateKey: String,
    monthKey: String,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val now = System.currentTimeMillis()
    val docId = "tea_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val log =
      TeaLog(
        id = docId,
        userId = userId,
        officeId = officeId,
        userName = userName.take(100),
        item = item,
        quantity = quantity.coerceIn(1L, 20L),
        unitCostInr = unitCostInr.coerceAtLeast(0.0),
        dateKey = dateKey.take(16),
        monthKey = monthKey.take(10),
        createdAtEpochMs = now,
      )
    dao.upsertTeaLog(log.toEntity())
    appendAuditLog(
      officeId = officeId,
      actorUid = userId,
      actorName = userName,
      actorRole = actorRole,
      actionType = "TEA_LOGGED",
      summary = "$userName logged $quantity x $item",
    )
    docId
  }

  suspend fun deleteTeaLog(logId: String): Result<Unit> = runCatching {
    dao.deleteTeaLog(logId)
  }

  suspend fun addTeaPurchase(
    officeId: String,
    addedByUid: String,
    addedByName: String,
    category: String,
    quantityLabel: String,
    amountInr: Double,
    dateKey: String,
    monthKey: String,
    note: String,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val now = System.currentTimeMillis()
    val docId = "pur_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val purchase =
      TeaPurchase(
        id = docId,
        officeId = officeId,
        addedBy = addedByUid,
        addedByName = addedByName.take(100),
        category = category,
        quantityLabel = quantityLabel.trim().take(60),
        amountInr = amountInr.coerceAtLeast(1.0),
        dateKey = dateKey.take(16),
        monthKey = monthKey.take(10),
        note = note.trim().take(300),
        createdAtEpochMs = now,
      )
    dao.upsertTeaPurchase(purchase.toEntity())
    appendAuditLog(
      officeId = officeId,
      actorUid = addedByUid,
      actorName = addedByName,
      actorRole = actorRole,
      actionType = "PANTRY_PURCHASE",
      summary = "Recorded $category ($quantityLabel) ₹${amountInr.toInt()}",
    )
    docId
  }

  suspend fun addDocument(
    officeId: String,
    document: OfficeDocument,
    actorRole: MemberRole,
  ): Result<String> = runCatching {
    val now = System.currentTimeMillis()
    val docId = "doc_${UUID.randomUUID().toString().replace("-", "").take(14)}"
    val safeScan = if (document.scanBase64.length <= 280_000) document.scanBase64 else ""
    val clean =
      document.copy(
        id = docId,
        officeId = officeId,
        createdByName = document.createdByName.take(100),
        serialNo = document.serialNo.trim().take(40),
        dateKey = document.dateKey.take(16),
        partyName = document.partyName.trim().take(120),
        subject = document.subject.trim().take(200),
        scanBase64 = safeScan,
        note = document.note.trim().take(400),
        createdAtEpochMs = now,
        updatedAtEpochMs = now,
      )
    dao.upsertDocument(clean.toEntity())
    appendAuditLog(
      officeId = officeId,
      actorUid = clean.createdBy,
      actorName = clean.createdByName,
      actorRole = actorRole,
      actionType = "DOCUMENT_ADDED",
      summary = "Added ${clean.direction} ${clean.serialNo}: ${clean.subject}",
    )
    docId
  }

  suspend fun updateDocumentStatus(
    officeId: String,
    doc: OfficeDocument,
    newStatus: String,
    actorUid: String,
    actorName: String,
    actorRole: MemberRole,
  ): Result<Unit> = runCatching {
    dao.upsertDocument(
      doc.copy(
        officeId = officeId,
        status = newStatus,
        updatedAtEpochMs = System.currentTimeMillis(),
      ).toEntity()
    )
    appendAuditLog(
      officeId = officeId,
      actorUid = actorUid,
      actorName = actorName,
      actorRole = actorRole,
      actionType = "DOCUMENT_UPDATED",
      summary = "Updated ${doc.serialNo} status to $newStatus",
    )
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
    dao.insertAuditLog(
      AuditLogEntity(
        id = logId,
        officeId = officeId,
        actorUid = actorUid,
        actorName = actorName.ifBlank { "Member" }.take(100),
        actorRole = actorRole.wire,
        actionType = actionType.take(60),
        summary = summary.take(400),
        createdAtEpochMs = System.currentTimeMillis(),
      )
    )
  }

  companion object {
    const val LOCAL_DEFAULT_USER_ID = "local_owner"

    fun friendlyErrorMessage(error: Throwable, fallback: String): String =
      error.localizedMessage?.takeIf { it.isNotBlank() } ?: fallback

    private fun generateInviteCode(): String {
      val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
      return (1..6).map { chars.random() }.joinToString("")
    }
  }
}
