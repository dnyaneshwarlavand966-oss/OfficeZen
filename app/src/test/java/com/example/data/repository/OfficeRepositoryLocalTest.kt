package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.OfficeDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.LeaveRequest
import com.example.data.model.MemberRole
import com.example.data.model.OfficeDocument
import com.example.domain.PayrollCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfficeRepositoryLocalTest {
  private lateinit var db: OfficeDatabase
  private lateinit var repo: OfficeRepository

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db =
      Room.inMemoryDatabaseBuilder(context, OfficeDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    repo = OfficeRepository(db.officeDao())
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun createOffice_setsActiveWorkspaceAndOwnerMember() = runTest {
    val uid = OfficeRepository.LOCAL_DEFAULT_USER_ID
    repo.ensureUserProfile(uid, "Rajesh", preferredLanguage = "mr").getOrThrow()

    val office =
      repo
        .createOffice(
          uid = uid,
          officeName = "Ganesh Traders",
          ownerDisplayName = "Rajesh Patil",
          ownerEmail = "",
          ownerPhone = "+919800000001",
          officeLat = 19.0760,
          officeLng = 72.8777,
          geofenceRadiusMeters = 150L,
          shiftStartMinutes = 570L,
          shiftEndMinutes = 1110L,
          lateGraceMinutes = 15L,
          workingDaysPerMonth = 26L,
          preferredLanguage = "mr",
        )
        .getOrThrow()

    val profile = repo.observeUserProfile(uid).first()
    assertEquals(office.id, profile?.activeOfficeId)

    val myMember = repo.observeMyMember(office.id, uid).first()
    assertNotNull(myMember)
    assertEquals(MemberRole.OWNER, myMember?.roleEnum)
    assertEquals("Rajesh Patil", myMember?.displayName)

    val allOffices = repo.observeAllOffices().first()
    assertEquals(1, allOffices.size)
    assertEquals("Ganesh Traders", allOffices.first().name)
  }

  @Test
  fun nonOwnerCannotAddStaffOrChangePayrollAdjustments() = runTest {
    val uid = OfficeRepository.LOCAL_DEFAULT_USER_ID
    val office =
      repo.createOffice(
        uid = uid,
        officeName = "Security Test Office",
        ownerDisplayName = "Owner",
        ownerEmail = "",
        ownerPhone = "",
        officeLat = 0.0,
        officeLng = 0.0,
        geofenceRadiusMeters = 150L,
        shiftStartMinutes = 570L,
        shiftEndMinutes = 1110L,
        lateGraceMinutes = 60L,
        workingDaysPerMonth = 26L,
      ).getOrThrow()

    val addStaff = repo.addLocalMember(
      officeId = office.id,
      ownerId = office.ownerId,
      displayName = "Blocked Staff",
      designation = "Staff",
      role = MemberRole.EMPLOYEE,
      monthlySalaryInr = 1000.0,
      overtimeHourlyRateInr = 0.0,
      phone = "",
      actorUid = "manager",
      actorName = "Manager",
      actorRole = MemberRole.MANAGER,
    )
    assertTrue(addStaff.isFailure)
    assertTrue(repo.observeMembers(office.id).first().none { it.displayName == "Blocked Staff" })

    val adjustment = repo.addSalaryAdjustment(
      officeId = office.id,
      targetUserId = uid,
      targetUserName = "Owner",
      monthKey = "2026-11",
      type = "bonus",
      amountInr = 500.0,
      note = "Unauthorized",
      actorUid = "manager",
      actorName = "Manager",
      actorRole = MemberRole.MANAGER,
    )
    assertTrue(adjustment.isFailure)
    assertTrue(repo.observeSalaryAdjustments(office.id).first().isEmpty())
  }

  @Test
  fun localStaffPayrollTeaDocsAndAttendance_endToEnd() = runTest {
    val uid = OfficeRepository.LOCAL_DEFAULT_USER_ID
    val office =
      repo
        .createOffice(
          uid = uid,
          officeName = "Zen Studio",
          ownerDisplayName = "Owner",
          ownerEmail = "",
          ownerPhone = "",
          officeLat = 18.5204,
          officeLng = 73.8567,
          geofenceRadiusMeters = 150L,
          shiftStartMinutes = 570L,
          shiftEndMinutes = 1110L,
          lateGraceMinutes = 15L,
          workingDaysPerMonth = 26L,
        )
        .getOrThrow()

    // Add local staff member directly without any login
    val staff =
      repo
        .addLocalMember(
          officeId = office.id,
          ownerId = office.ownerId,
          displayName = "Priya Kulkarni",
          designation = "Accountant",
          role = MemberRole.EMPLOYEE,
          monthlySalaryInr = 26000.0,
          overtimeHourlyRateInr = 150.0,
          phone = "+919811111111",
          actorUid = uid,
          actorName = "Owner",
          actorRole = MemberRole.OWNER,
        )
        .getOrThrow()

    // Check-in & Check-out for staff
    val recId =
      repo
        .checkIn(
          officeId = office.id,
          record =
            AttendanceRecord(
              userId = staff.userId,
              officeId = office.id,
              userName = staff.displayName,
              dateKey = "2026-11-23",
              checkInEpochMs = 1_700_000_000_000L,
              checkInLat = 18.5204,
              checkInLng = 73.8567,
              distanceMeters = 12.0,
              isOutOfRange = false,
              status = "on_time",
            ),
          actorRole = MemberRole.EMPLOYEE,
        )
        .getOrThrow()

    val savedRec = repo.observeAttendance(office.id).first().first { it.id == recId }
    repo
      .checkOut(
        officeId = office.id,
        record = savedRec,
        checkOutEpochMs = 1_700_032_400_000L,
        overtimeMinutes = 60L,
        note = "Closed ledger",
        actorUid = uid,
        actorName = "Owner",
        actorRole = MemberRole.OWNER,
      )
      .getOrThrow()

    // Submit and approve leave
    val leaveId =
      repo
        .submitLeave(
          officeId = office.id,
          leave =
            LeaveRequest(
              userId = staff.userId,
              officeId = office.id,
              userName = staff.displayName,
              leaveType = "paid",
              startDateKey = "2026-11-24",
              endDateKey = "2026-11-24",
              daysCount = 1.0,
              reason = "Family function",
            ),
          actorRole = MemberRole.EMPLOYEE,
        )
        .getOrThrow()
    val savedLeave = repo.observeLeaves(office.id).first().first { it.id == leaveId }
    val employeeReview =
      repo.reviewLeave(office.id, savedLeave, true, "Not allowed", staff.userId, staff.displayName, MemberRole.EMPLOYEE)
    assertTrue(employeeReview.isFailure)
    assertEquals("pending", repo.observeLeaves(office.id).first().first().status)

    repo.reviewLeave(office.id, savedLeave, true, "Approved", uid, "Owner", MemberRole.OWNER).getOrThrow()
    assertEquals("approved", repo.observeLeaves(office.id).first().first().status)

    // Log tea & pantry purchase
    repo
      .logTeaOrCoffee(
        officeId = office.id,
        userId = staff.userId,
        userName = staff.displayName,
        item = "tea",
        quantity = 2L,
        unitCostInr = 12.0,
        dateKey = "2026-11-23",
        monthKey = "2026-11",
        actorRole = MemberRole.EMPLOYEE,
      )
      .getOrThrow()
    repo
      .addTeaPurchase(
        officeId = office.id,
        addedByUid = uid,
        addedByName = "Owner",
        category = "milk",
        quantityLabel = "2 L",
        amountInr = 130.0,
        dateKey = "2026-11-23",
        monthKey = "2026-11",
        note = "Morning pantry",
        actorRole = MemberRole.OWNER,
      )
      .getOrThrow()

    // Document inward & update status
    val docId =
      repo
        .addDocument(
          officeId = office.id,
          document =
            OfficeDocument(
              officeId = office.id,
              createdBy = uid,
              createdByName = "Owner",
              serialNo = "IN-2026-001",
              direction = "inward",
              dateKey = "2026-11-23",
              partyName = "Vendor Corp",
              subject = "GST Invoice",
              status = "received",
            ),
          actorRole = MemberRole.OWNER,
        )
        .getOrThrow()
    val savedDoc = repo.observeDocuments(office.id).first().first { it.id == docId }
    repo.updateDocumentStatus(office.id, savedDoc, "filed", uid, "Owner", MemberRole.OWNER).getOrThrow()
    assertEquals("filed", repo.observeDocuments(office.id).first().first().status)

    // Verify Payroll calculation
    val summary =
      PayrollCalculator.calculateMonthEndSummary(
        office = office,
        members = repo.observeMembers(office.id).first(),
        profiles = repo.observePayrollProfiles(office.id).first().associateBy { it.userId },
        monthKey = "2026-11",
        attendance = repo.observeAttendance(office.id).first(),
        leaves = repo.observeLeaves(office.id).first(),
        adjustments = repo.observeSalaryAdjustments(office.id).first(),
        teaLogs = repo.observeTeaLogs(office.id).first(),
        teaPurchases = repo.observeTeaPurchases(office.id).first(),
      )
    val staffPay = summary.employeePayrolls.first { it.member.userId == staff.userId }
    assertEquals(1, staffPay.presentDays)
    assertEquals(60L, staffPay.overtimeMinutes)
    assertEquals(2, summary.totalTeaCups)
    assertEquals(130.0, summary.totalPantryPurchaseInr, 0.01)

    // Verify audit logs recorded
    assertTrue(repo.observeAuditLogs(office.id).first().size >= 6)

    // Switch workspace & reopen via invite code
    repo.leaveActiveOfficeWorkspace(uid).getOrThrow()
    assertEquals("", repo.observeUserProfile(uid).first()?.activeOfficeId)

    repo.joinLocalOfficeByInviteCode(uid, office.inviteCode, "Owner", "", "Owner").getOrThrow()
    assertEquals(office.id, repo.observeUserProfile(uid).first()?.activeOfficeId)
  }

  @Test
  fun ownerCanAssignAccountantButAccountantCannotChangePayroll() = runTest {
    val uid = OfficeRepository.LOCAL_DEFAULT_USER_ID
    val office = repo.createOffice(
      uid = uid,
      officeName = "Accountant Role Test",
      ownerDisplayName = "Owner",
      ownerEmail = "",
      ownerPhone = "",
      officeLat = 0.0,
      officeLng = 0.0,
      geofenceRadiusMeters = 150L,
      shiftStartMinutes = 570L,
      shiftEndMinutes = 1110L,
      lateGraceMinutes = 60L,
      workingDaysPerMonth = 26L,
    ).getOrThrow()

    val accountant = repo.addLocalMember(
      officeId = office.id,
      ownerId = office.ownerId,
      displayName = "Assigned Accountant",
      designation = "Accounts",
      role = MemberRole.ACCOUNTANT,
      monthlySalaryInr = 25000.0,
      overtimeHourlyRateInr = 0.0,
      phone = "",
      actorUid = uid,
      actorName = "Owner",
      actorRole = MemberRole.OWNER,
    ).getOrThrow()

    assertEquals(MemberRole.ACCOUNTANT, accountant.roleEnum)
    assertTrue(accountant.roleEnum.canViewTeamPayroll())
    assertTrue(!accountant.roleEnum.canManageTeamPayroll())

    val blockedAdjustment = repo.addSalaryAdjustment(
      officeId = office.id,
      targetUserId = accountant.userId,
      targetUserName = accountant.displayName,
      monthKey = "2026-11",
      type = "bonus",
      amountInr = 100.0,
      note = "Must be owner approved",
      actorUid = accountant.userId,
      actorName = accountant.displayName,
      actorRole = MemberRole.ACCOUNTANT,
    )
    assertTrue(blockedAdjustment.isFailure)
    assertTrue(repo.observeSalaryAdjustments(office.id).first().isEmpty())
  }

}
