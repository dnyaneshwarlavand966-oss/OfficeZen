// OfficeZen Room DAO with reactive Flow queries and suspend mutations
package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OfficeDao {

  @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
  fun observeUserProfile(userId: String): Flow<UserProfileEntity?>

  @Query("SELECT * FROM user_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getUserProfile(userId: String): UserProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertUserProfile(profile: UserProfileEntity)

  @Query("UPDATE user_profiles SET activeOfficeId = :officeId, updatedAtEpochMs = :updatedAt WHERE userId = :userId")
  suspend fun updateActiveOffice(userId: String, officeId: String, updatedAt: Long)

  @Query("UPDATE user_profiles SET preferredLanguage = :lang, updatedAtEpochMs = :updatedAt WHERE userId = :userId")
  suspend fun updatePreferredLanguage(userId: String, lang: String, updatedAt: Long)

  @Query("SELECT * FROM offices WHERE id = :officeId LIMIT 1")
  fun observeOffice(officeId: String): Flow<OfficeEntity?>

  @Query("SELECT * FROM offices ORDER BY createdAtEpochMs DESC")
  fun observeAllOffices(): Flow<List<OfficeEntity>>

  @Query("SELECT * FROM offices WHERE id = :officeId LIMIT 1")
  suspend fun getOffice(officeId: String): OfficeEntity?

  @Query("SELECT * FROM offices WHERE inviteCode = :inviteCode LIMIT 1")
  suspend fun getOfficeByInviteCode(inviteCode: String): OfficeEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertOffice(office: OfficeEntity)

  @Query("SELECT * FROM members WHERE officeId = :officeId ORDER BY displayName COLLATE NOCASE ASC")
  fun observeMembers(officeId: String): Flow<List<MemberEntity>>

  @Query("SELECT * FROM members WHERE officeId = :officeId AND userId = :userId LIMIT 1")
  fun observeMyMember(officeId: String, userId: String): Flow<MemberEntity?>

  @Query("SELECT * FROM members WHERE officeId = :officeId AND userId = :userId LIMIT 1")
  suspend fun getMember(officeId: String, userId: String): MemberEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMember(member: MemberEntity)

  @Query("SELECT * FROM payroll_profiles WHERE officeId = :officeId")
  fun observePayrollProfiles(officeId: String): Flow<List<PayrollProfileEntity>>

  @Query("SELECT * FROM payroll_profiles WHERE officeId = :officeId AND userId = :userId LIMIT 1")
  suspend fun getPayrollProfile(officeId: String, userId: String): PayrollProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPayrollProfile(profile: PayrollProfileEntity)

  @Query("SELECT * FROM attendance_records WHERE officeId = :officeId ORDER BY checkInEpochMs DESC")
  fun observeAttendance(officeId: String): Flow<List<AttendanceRecordEntity>>

  @Query("SELECT COUNT(*) FROM attendance_records WHERE officeId = :officeId AND syncStatus IN ('pending', 'failed', 'syncing')")
  fun observePendingAttendanceCount(officeId: String): Flow<Int>

  @Query("SELECT COUNT(*) FROM attendance_records WHERE officeId = :officeId AND syncStatus = 'synced'")
  fun observeSyncedAttendanceCount(officeId: String): Flow<Int>

  @Query("SELECT COUNT(*) FROM attendance_records WHERE syncStatus IN ('pending', 'failed', 'syncing')")
  fun observeAllPendingAttendanceCount(): Flow<Int>

  @Query("SELECT * FROM attendance_records WHERE syncStatus IN ('pending', 'failed') ORDER BY createdAtEpochMs ASC")
  suspend fun getPendingAttendanceRecords(): List<AttendanceRecordEntity>

  @Query("SELECT * FROM attendance_records WHERE id = :id LIMIT 1")
  suspend fun getAttendanceById(id: String): AttendanceRecordEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAttendance(record: AttendanceRecordEntity)

  @Query("UPDATE attendance_records SET syncStatus = :syncStatus, lastSyncAttemptEpochMs = :attemptEpochMs, syncError = :syncError WHERE id = :id")
  suspend fun updateAttendanceSyncState(
    id: String,
    syncStatus: String,
    attemptEpochMs: Long,
    syncError: String,
  )

  @Query("UPDATE attendance_records SET syncStatus = 'pending', syncError = '' WHERE syncStatus = 'failed'")
  suspend fun retryAllFailedAttendance()

  @Query("SELECT * FROM leave_requests WHERE officeId = :officeId ORDER BY createdAtEpochMs DESC")
  fun observeLeaves(officeId: String): Flow<List<LeaveRequestEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertLeave(leave: LeaveRequestEntity)

  @Query("SELECT * FROM salary_adjustments WHERE officeId = :officeId ORDER BY createdAtEpochMs DESC")
  fun observeSalaryAdjustments(officeId: String): Flow<List<SalaryAdjustmentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSalaryAdjustment(adjustment: SalaryAdjustmentEntity)

  @Query("SELECT * FROM tea_logs WHERE officeId = :officeId ORDER BY createdAtEpochMs DESC")
  fun observeTeaLogs(officeId: String): Flow<List<TeaLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTeaLog(log: TeaLogEntity)

  @Query("DELETE FROM tea_logs WHERE id = :logId")
  suspend fun deleteTeaLog(logId: String)

  @Query("SELECT * FROM tea_purchases WHERE officeId = :officeId ORDER BY createdAtEpochMs DESC")
  fun observeTeaPurchases(officeId: String): Flow<List<TeaPurchaseEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTeaPurchase(purchase: TeaPurchaseEntity)

  @Query("SELECT * FROM office_documents WHERE officeId = :officeId ORDER BY createdAtEpochMs DESC")
  fun observeDocuments(officeId: String): Flow<List<OfficeDocumentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDocument(document: OfficeDocumentEntity)

  @Query("SELECT * FROM audit_logs WHERE officeId = :officeId ORDER BY createdAtEpochMs DESC")
  fun observeAuditLogs(officeId: String): Flow<List<AuditLogEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAuditLog(log: AuditLogEntity)
}
