// OfficeZen domain & Firestore models
package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class MemberRole(val wire: String) {
  OWNER("owner"),
  MANAGER("manager"),
  EMPLOYEE("employee");

  companion object {
    fun fromWire(value: String): MemberRole =
      entries.firstOrNull { it.wire == value } ?: EMPLOYEE
  }
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val activeOfficeId: String = "",
  val preferredLanguage: String = "en",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class OfficeInvite(
  @DocumentId val id: String = "",
  val inviteCode: String = "",
  val officeId: String = "",
  val officeName: String = "",
  val ownerId: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Office(
  @DocumentId val id: String = "",
  val ownerId: String = "",
  val name: String = "",
  val inviteCode: String = "",
  val officeLat: Double = 0.0,
  val officeLng: Double = 0.0,
  val geofenceRadiusMeters: Long = 150L,
  val strictGeofenceBlock: Boolean = false,
  val shiftStartMinutes: Long = 570L, // 09:30
  val shiftEndMinutes: Long = 1110L, // 18:30
  val lateGraceMinutes: Long = 15L,
  val lateMarksPerHalfDay: Long = 3L,
  val workingDaysPerMonth: Long = 26L,
  val monthEndReportDay: Long = 30L,
  val ownerReportEmail: String = "",
  val ownerWhatsappPhone: String = "",
  val teaDefaultPriceInr: Double = 12.0,
  val coffeeDefaultPriceInr: Double = 20.0,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Member(
  @DocumentId val id: String = "",
  val userId: String = "",
  val officeId: String = "",
  val ownerId: String = "",
  val displayName: String = "",
  val designation: String = "",
  val role: String = MemberRole.EMPLOYEE.wire,
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  val roleEnum: MemberRole get() = MemberRole.fromWire(role)
}

@IgnoreExtraProperties
data class PayrollProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val officeId: String = "",
  val ownerId: String = "",
  val monthlySalaryInr: Double = 0.0,
  val overtimeHourlyRateInr: Double = 0.0,
  val phone: String = "",
  val email: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AttendanceRecord(
  @DocumentId val id: String = "",
  val userId: String = "",
  val officeId: String = "",
  val userName: String = "",
  val dateKey: String = "",
  val checkInEpochMs: Long = 0L,
  val checkOutEpochMs: Long = 0L,
  val checkInLat: Double = 0.0,
  val checkInLng: Double = 0.0,
  val distanceMeters: Double = 0.0,
  val isOutOfRange: Boolean = false,
  val isMockLocation: Boolean = false,
  val status: String = "on_time",
  val lateMinutes: Long = 0L,
  val overtimeMinutes: Long = 0L,
  val selfieBase64: String = "",
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class LeaveRequest(
  @DocumentId val id: String = "",
  val userId: String = "",
  val officeId: String = "",
  val userName: String = "",
  val leaveType: String = "casual",
  val startDateKey: String = "",
  val endDateKey: String = "",
  val daysCount: Double = 1.0,
  val reason: String = "",
  val status: String = "pending",
  val reviewedBy: String = "",
  val reviewNote: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class SalaryAdjustment(
  @DocumentId val id: String = "",
  val userId: String = "",
  val officeId: String = "",
  val monthKey: String = "",
  val type: String = "advance",
  val amountInr: Double = 0.0,
  val note: String = "",
  val addedBy: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TeaLog(
  @DocumentId val id: String = "",
  val userId: String = "",
  val officeId: String = "",
  val userName: String = "",
  val item: String = "tea",
  val quantity: Long = 1L,
  val unitCostInr: Double = 12.0,
  val dateKey: String = "",
  val monthKey: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class TeaPurchase(
  @DocumentId val id: String = "",
  val officeId: String = "",
  val addedBy: String = "",
  val addedByName: String = "",
  val category: String = "milk",
  val quantityLabel: String = "",
  val amountInr: Double = 0.0,
  val dateKey: String = "",
  val monthKey: String = "",
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class OfficeDocument(
  @DocumentId val id: String = "",
  val officeId: String = "",
  val createdBy: String = "",
  val createdByName: String = "",
  val serialNo: String = "",
  val direction: String = "inward",
  val dateKey: String = "",
  val partyName: String = "",
  val subject: String = "",
  val status: String = "received",
  val scanBase64: String = "",
  val note: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AuditLog(
  @DocumentId val id: String = "",
  val officeId: String = "",
  val actorUid: String = "",
  val actorName: String = "",
  val actorRole: String = MemberRole.EMPLOYEE.wire,
  val actionType: String = "",
  val summary: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
)
