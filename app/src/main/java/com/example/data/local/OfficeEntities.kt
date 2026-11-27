// OfficeZen Room Database entities and domain mappers
package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSyncStatus
import com.example.data.model.AuditLog
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.Office
import com.example.data.model.OfficeDocument
import com.example.data.model.PayrollProfile
import com.example.data.model.SalaryAdjustment
import com.example.data.model.TeaLog
import com.example.data.model.TeaPurchase
import com.example.data.model.UserProfile

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
  @PrimaryKey val userId: String,
  val displayName: String,
  val email: String,
  val activeOfficeId: String,
  val preferredLanguage: String,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  fun toDomain(): UserProfile =
    UserProfile(
      id = userId,
      userId = userId,
      displayName = displayName,
      email = email,
      activeOfficeId = activeOfficeId,
      preferredLanguage = preferredLanguage,
      createdAtEpochMs = createdAtEpochMs,
      updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun UserProfile.toEntity(): UserProfileEntity =
  UserProfileEntity(
    userId = userId,
    displayName = displayName,
    email = email,
    activeOfficeId = activeOfficeId,
    preferredLanguage = preferredLanguage,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

@Entity(tableName = "offices")
data class OfficeEntity(
  @PrimaryKey val id: String,
  val ownerId: String,
  val name: String,
  val inviteCode: String,
  val officeLat: Double,
  val officeLng: Double,
  val geofenceRadiusMeters: Long,
  val strictGeofenceBlock: Boolean,
  val shiftStartMinutes: Long,
  val shiftEndMinutes: Long,
  val lateGraceMinutes: Long,
  val lateMarksPerHalfDay: Long,
  val workingDaysPerMonth: Long,
  val monthEndReportDay: Long,
  val ownerReportEmail: String,
  val ownerWhatsappPhone: String,
  val teaDefaultPriceInr: Double,
  val coffeeDefaultPriceInr: Double,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  fun toDomain(): Office =
    Office(
      id = id,
      ownerId = ownerId,
      name = name,
      inviteCode = inviteCode,
      officeLat = officeLat,
      officeLng = officeLng,
      geofenceRadiusMeters = geofenceRadiusMeters,
      strictGeofenceBlock = strictGeofenceBlock,
      shiftStartMinutes = shiftStartMinutes,
      shiftEndMinutes = shiftEndMinutes,
      lateGraceMinutes = lateGraceMinutes,
      lateMarksPerHalfDay = lateMarksPerHalfDay,
      workingDaysPerMonth = workingDaysPerMonth,
      monthEndReportDay = monthEndReportDay,
      ownerReportEmail = ownerReportEmail,
      ownerWhatsappPhone = ownerWhatsappPhone,
      teaDefaultPriceInr = teaDefaultPriceInr,
      coffeeDefaultPriceInr = coffeeDefaultPriceInr,
      createdAtEpochMs = createdAtEpochMs,
      updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun Office.toEntity(): OfficeEntity =
  OfficeEntity(
    id = id,
    ownerId = ownerId,
    name = name,
    inviteCode = inviteCode,
    officeLat = officeLat,
    officeLng = officeLng,
    geofenceRadiusMeters = geofenceRadiusMeters,
    strictGeofenceBlock = strictGeofenceBlock,
    shiftStartMinutes = shiftStartMinutes,
    shiftEndMinutes = shiftEndMinutes,
    lateGraceMinutes = lateGraceMinutes,
    lateMarksPerHalfDay = lateMarksPerHalfDay,
    workingDaysPerMonth = workingDaysPerMonth,
    monthEndReportDay = monthEndReportDay,
    ownerReportEmail = ownerReportEmail,
    ownerWhatsappPhone = ownerWhatsappPhone,
    teaDefaultPriceInr = teaDefaultPriceInr,
    coffeeDefaultPriceInr = coffeeDefaultPriceInr,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

@Entity(tableName = "members")
data class MemberEntity(
  @PrimaryKey val id: String, // "${officeId}_${userId}"
  val userId: String,
  val officeId: String,
  val ownerId: String,
  val displayName: String,
  val designation: String,
  val role: String,
  val active: Boolean,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  fun toDomain(): Member =
    Member(
      id = id,
      userId = userId,
      officeId = officeId,
      ownerId = ownerId,
      displayName = displayName,
      designation = designation,
      role = role,
      active = active,
      createdAtEpochMs = createdAtEpochMs,
      updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun Member.toEntity(): MemberEntity =
  MemberEntity(
    id = id.ifBlank { "${officeId}_${userId}" },
    userId = userId,
    officeId = officeId,
    ownerId = ownerId,
    displayName = displayName,
    designation = designation,
    role = role,
    active = active,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

@Entity(tableName = "payroll_profiles")
data class PayrollProfileEntity(
  @PrimaryKey val id: String, // "${officeId}_${userId}"
  val userId: String,
  val officeId: String,
  val ownerId: String,
  val monthlySalaryInr: Double,
  val overtimeHourlyRateInr: Double,
  val phone: String,
  val email: String,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  fun toDomain(): PayrollProfile =
    PayrollProfile(
      id = id,
      userId = userId,
      officeId = officeId,
      ownerId = ownerId,
      monthlySalaryInr = monthlySalaryInr,
      overtimeHourlyRateInr = overtimeHourlyRateInr,
      phone = phone,
      email = email,
      createdAtEpochMs = createdAtEpochMs,
      updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun PayrollProfile.toEntity(): PayrollProfileEntity =
  PayrollProfileEntity(
    id = id.ifBlank { "${officeId}_${userId}" },
    userId = userId,
    officeId = officeId,
    ownerId = ownerId,
    monthlySalaryInr = monthlySalaryInr,
    overtimeHourlyRateInr = overtimeHourlyRateInr,
    phone = phone,
    email = email,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

@Entity(tableName = "attendance_records")
data class AttendanceRecordEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val officeId: String,
  val userName: String,
  val dateKey: String,
  val checkInEpochMs: Long,
  val checkOutEpochMs: Long,
  val checkInLat: Double,
  val checkInLng: Double,
  val distanceMeters: Double,
  val isOutOfRange: Boolean,
  val isMockLocation: Boolean,
  val status: String,
  val lateMinutes: Long,
  val overtimeMinutes: Long,
  val selfieBase64: String,
  val note: String,
  val syncStatus: String = AttendanceSyncStatus.PENDING,
  val lastSyncAttemptEpochMs: Long = 0L,
  val syncError: String = "",
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  fun toDomain(): AttendanceRecord =
    AttendanceRecord(
      id = id,
      userId = userId,
      officeId = officeId,
      userName = userName,
      dateKey = dateKey,
      checkInEpochMs = checkInEpochMs,
      checkOutEpochMs = checkOutEpochMs,
      checkInLat = checkInLat,
      checkInLng = checkInLng,
      distanceMeters = distanceMeters,
      isOutOfRange = isOutOfRange,
      isMockLocation = isMockLocation,
      status = status,
      lateMinutes = lateMinutes,
      overtimeMinutes = overtimeMinutes,
      selfieBase64 = selfieBase64,
      note = note,
      syncStatus = syncStatus,
      lastSyncAttemptEpochMs = lastSyncAttemptEpochMs,
      syncError = syncError,
      createdAtEpochMs = createdAtEpochMs,
      updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun AttendanceRecord.toEntity(): AttendanceRecordEntity =
  AttendanceRecordEntity(
    id = id,
    userId = userId,
    officeId = officeId,
    userName = userName,
    dateKey = dateKey,
    checkInEpochMs = checkInEpochMs,
    checkOutEpochMs = checkOutEpochMs,
    checkInLat = checkInLat,
    checkInLng = checkInLng,
    distanceMeters = distanceMeters,
    isOutOfRange = isOutOfRange,
    isMockLocation = isMockLocation,
    status = status,
    lateMinutes = lateMinutes,
    overtimeMinutes = overtimeMinutes,
    selfieBase64 = selfieBase64,
    note = note,
    syncStatus = syncStatus,
    lastSyncAttemptEpochMs = lastSyncAttemptEpochMs,
    syncError = syncError,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

@Entity(tableName = "leave_requests")
data class LeaveRequestEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val officeId: String,
  val userName: String,
  val leaveType: String,
  val startDateKey: String,
  val endDateKey: String,
  val daysCount: Double,
  val reason: String,
  val status: String,
  val reviewedBy: String,
  val reviewNote: String,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  fun toDomain(): LeaveRequest =
    LeaveRequest(
      id = id,
      userId = userId,
      officeId = officeId,
      userName = userName,
      leaveType = leaveType,
      startDateKey = startDateKey,
      endDateKey = endDateKey,
      daysCount = daysCount,
      reason = reason,
      status = status,
      reviewedBy = reviewedBy,
      reviewNote = reviewNote,
      createdAtEpochMs = createdAtEpochMs,
      updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun LeaveRequest.toEntity(): LeaveRequestEntity =
  LeaveRequestEntity(
    id = id,
    userId = userId,
    officeId = officeId,
    userName = userName,
    leaveType = leaveType,
    startDateKey = startDateKey,
    endDateKey = endDateKey,
    daysCount = daysCount,
    reason = reason,
    status = status,
    reviewedBy = reviewedBy,
    reviewNote = reviewNote,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

@Entity(tableName = "salary_adjustments")
data class SalaryAdjustmentEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val officeId: String,
  val monthKey: String,
  val type: String,
  val amountInr: Double,
  val note: String,
  val addedBy: String,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  fun toDomain(): SalaryAdjustment =
    SalaryAdjustment(
      id = id,
      userId = userId,
      officeId = officeId,
      monthKey = monthKey,
      type = type,
      amountInr = amountInr,
      note = note,
      addedBy = addedBy,
      createdAtEpochMs = createdAtEpochMs,
      updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun SalaryAdjustment.toEntity(): SalaryAdjustmentEntity =
  SalaryAdjustmentEntity(
    id = id,
    userId = userId,
    officeId = officeId,
    monthKey = monthKey,
    type = type,
    amountInr = amountInr,
    note = note,
    addedBy = addedBy,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

@Entity(tableName = "tea_logs")
data class TeaLogEntity(
  @PrimaryKey val id: String,
  val userId: String,
  val officeId: String,
  val userName: String,
  val item: String,
  val quantity: Long,
  val unitCostInr: Double,
  val dateKey: String,
  val monthKey: String,
  val createdAtEpochMs: Long,
) {
  fun toDomain(): TeaLog =
    TeaLog(
      id = id,
      userId = userId,
      officeId = officeId,
      userName = userName,
      item = item,
      quantity = quantity,
      unitCostInr = unitCostInr,
      dateKey = dateKey,
      monthKey = monthKey,
      createdAtEpochMs = createdAtEpochMs,
    )
}

fun TeaLog.toEntity(): TeaLogEntity =
  TeaLogEntity(
    id = id,
    userId = userId,
    officeId = officeId,
    userName = userName,
    item = item,
    quantity = quantity,
    unitCostInr = unitCostInr,
    dateKey = dateKey,
    monthKey = monthKey,
    createdAtEpochMs = createdAtEpochMs,
  )

@Entity(tableName = "tea_purchases")
data class TeaPurchaseEntity(
  @PrimaryKey val id: String,
  val officeId: String,
  val addedBy: String,
  val addedByName: String,
  val category: String,
  val quantityLabel: String,
  val amountInr: Double,
  val dateKey: String,
  val monthKey: String,
  val note: String,
  val createdAtEpochMs: Long,
) {
  fun toDomain(): TeaPurchase =
    TeaPurchase(
      id = id,
      officeId = officeId,
      addedBy = addedBy,
      addedByName = addedByName,
      category = category,
      quantityLabel = quantityLabel,
      amountInr = amountInr,
      dateKey = dateKey,
      monthKey = monthKey,
      note = note,
      createdAtEpochMs = createdAtEpochMs,
    )
}

fun TeaPurchase.toEntity(): TeaPurchaseEntity =
  TeaPurchaseEntity(
    id = id,
    officeId = officeId,
    addedBy = addedBy,
    addedByName = addedByName,
    category = category,
    quantityLabel = quantityLabel,
    amountInr = amountInr,
    dateKey = dateKey,
    monthKey = monthKey,
    note = note,
    createdAtEpochMs = createdAtEpochMs,
  )

@Entity(tableName = "office_documents")
data class OfficeDocumentEntity(
  @PrimaryKey val id: String,
  val officeId: String,
  val createdBy: String,
  val createdByName: String,
  val serialNo: String,
  val direction: String,
  val dateKey: String,
  val partyName: String,
  val subject: String,
  val status: String,
  val scanBase64: String,
  val note: String,
  val createdAtEpochMs: Long,
  val updatedAtEpochMs: Long,
) {
  fun toDomain(): OfficeDocument =
    OfficeDocument(
      id = id,
      officeId = officeId,
      createdBy = createdBy,
      createdByName = createdByName,
      serialNo = serialNo,
      direction = direction,
      dateKey = dateKey,
      partyName = partyName,
      subject = subject,
      status = status,
      scanBase64 = scanBase64,
      note = note,
      createdAtEpochMs = createdAtEpochMs,
      updatedAtEpochMs = updatedAtEpochMs,
    )
}

fun OfficeDocument.toEntity(): OfficeDocumentEntity =
  OfficeDocumentEntity(
    id = id,
    officeId = officeId,
    createdBy = createdBy,
    createdByName = createdByName,
    serialNo = serialNo,
    direction = direction,
    dateKey = dateKey,
    partyName = partyName,
    subject = subject,
    status = status,
    scanBase64 = scanBase64,
    note = note,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
  )

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
  @PrimaryKey val id: String,
  val officeId: String,
  val actorUid: String,
  val actorName: String,
  val actorRole: String,
  val actionType: String,
  val summary: String,
  val createdAtEpochMs: Long,
) {
  fun toDomain(): AuditLog =
    AuditLog(
      id = id,
      officeId = officeId,
      actorUid = actorUid,
      actorName = actorName,
      actorRole = actorRole,
      actionType = actionType,
      summary = summary,
      createdAtEpochMs = createdAtEpochMs,
    )
}

fun AuditLog.toEntity(): AuditLogEntity =
  AuditLogEntity(
    id = id,
    officeId = officeId,
    actorUid = actorUid,
    actorName = actorName,
    actorRole = actorRole,
    actionType = actionType,
    summary = summary,
    createdAtEpochMs = createdAtEpochMs,
  )
