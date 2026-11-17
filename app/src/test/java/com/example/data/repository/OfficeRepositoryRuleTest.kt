// OfficeZen repository & Firestore rule integration tests
package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AttendanceRecord
import com.example.data.model.LeaveRequest
import com.example.data.model.MemberRole
import com.example.data.model.OfficeDocument
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficeRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createOffice_joinEmployee_checkIn_leave_tea_doc_and_permissions() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val ownerEmail = "owner_$suffix@test.com"
    val empEmail = "emp_$suffix@test.com"

    // 1. Owner signs in and creates office
    val ownerUid = signInTestUser(ownerEmail)
    val ownerRepo = OfficeRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo.ensureUserProfile("Rajesh Owner", ownerEmail, "en").getOrThrow()
    }
    val office =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo
          .createOffice(
            officeName = "Zen Traders $suffix",
            ownerDisplayName = "Rajesh Owner",
            ownerEmail = ownerEmail,
            ownerPhone = "+919876543210",
            officeLat = 19.076,
            officeLng = 72.8777,
            geofenceRadiusMeters = 150L,
            shiftStartMinutes = 570L,
            shiftEndMinutes = 1110L,
            lateGraceMinutes = 15L,
            workingDaysPerMonth = 26L,
          )
          .getOrThrow()
      }
    assertTrue(office.inviteCode.length == 6)

    // 2. Employee signs in and joins via invite code
    val empUid = signInTestUser(empEmail)
    val empRepo = OfficeRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      empRepo.ensureUserProfile("Amit Staff", empEmail, "hi").getOrThrow()
    }
    val joinedOfficeId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        empRepo
          .joinOfficeByInviteCode(
            rawCode = office.inviteCode,
            displayName = "Amit Staff",
            email = empEmail,
            phone = "9876500001",
            designation = "Executive",
          )
          .getOrThrow()
      }
    assertEquals(office.id, joinedOfficeId)

    // 3. Employee checks in (valid non-mock location) & logs tea & adds document & requests leave
    withTimeout(DEFAULT_TIMEOUT_MS) {
      empRepo
        .checkIn(
          officeId = office.id,
          record =
            AttendanceRecord(
              userId = empUid,
              officeId = office.id,
              userName = "Amit Staff",
              dateKey = "2026-11-16",
              checkInEpochMs = 1763280000000L,
              checkInLat = 19.0761,
              checkInLng = 72.8778,
              distanceMeters = 12.0,
              isOutOfRange = false,
              isMockLocation = false,
              status = "on_time",
            ),
          actorRole = MemberRole.EMPLOYEE,
        )
        .getOrThrow()
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      empRepo
        .logTeaOrCoffee(
          officeId = office.id,
          userName = "Amit Staff",
          item = "tea",
          quantity = 1L,
          unitCostInr = 12.0,
          dateKey = "2026-11-16",
          monthKey = "2026-11",
          actorRole = MemberRole.EMPLOYEE,
        )
        .getOrThrow()
    }

    withTimeout(DEFAULT_TIMEOUT_MS) {
      empRepo
        .addDocument(
          officeId = office.id,
          document =
            OfficeDocument(
              officeId = office.id,
              createdBy = empUid,
              createdByName = "Amit Staff",
              serialNo = "IN-2026-001",
              direction = "inward",
              dateKey = "2026-11-16",
              partyName = "MSEB Power",
              subject = "Monthly Electricity Bill",
              status = "received",
            ),
          actorRole = MemberRole.EMPLOYEE,
        )
        .getOrThrow()
    }

    val leaveId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        empRepo
          .submitLeave(
            officeId = office.id,
            leave =
              LeaveRequest(
                userId = empUid,
                officeId = office.id,
                userName = "Amit Staff",
                leaveType = "casual",
                startDateKey = "2026-11-20",
                endDateKey = "2026-11-20",
                daysCount = 1.0,
                reason = "Family visit",
              ),
            actorRole = MemberRole.EMPLOYEE,
          )
          .getOrThrow()
      }

    // Employee reading their own attendance succeeds; querying all attendance fails
    val myAttendance =
      withTimeout(FLOW_TIMEOUT_MS) {
        empRepo.observeAttendance(office.id, isManagerOrOwner = false).first()
      }
    assertEquals(1, myAttendance.size)

    var deniedAllAttendance = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        empRepo.observeAttendance(office.id, isManagerOrOwner = true).first()
      }
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      if (fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
        deniedAllAttendance = true
      }
    }
    assertTrue("Employee must be denied unfiltered attendance list", deniedAllAttendance)

    // 4. Owner signs back in, sets salary, approves leave, and reads audit logs
    signInTestUser(ownerEmail)
    val members = withTimeout(FLOW_TIMEOUT_MS) { ownerRepo.observeMembers(office.id).first() }
    val empMember = members.first { it.userId == empUid }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo
        .updateMemberRoleAndSalary(
          officeId = office.id,
          member = empMember,
          newRole = MemberRole.MANAGER,
          newDesignation = "Senior Coordinator",
          active = true,
          monthlySalaryInr = 32000.0,
          overtimeHourlyRateInr = 180.0,
          phone = "9876500001",
          email = empEmail,
          actorName = "Rajesh Owner",
          actorRole = MemberRole.OWNER,
        )
        .getOrThrow()
    }

    val allLeaves =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeLeaves(office.id, isManagerOrOwner = true).first()
      }
    val empLeave = allLeaves.first { it.id == leaveId }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      ownerRepo
        .reviewLeave(
          officeId = office.id,
          leave = empLeave,
          approved = true,
          reviewNote = "Approved",
          actorName = "Rajesh Owner",
          actorRole = MemberRole.OWNER,
        )
        .getOrThrow()
    }

    val audits = withTimeout(FLOW_TIMEOUT_MS) { ownerRepo.observeAuditLogs(office.id).first() }
    assertTrue(audits.isNotEmpty())
  }

  @Test
  fun createAndJoinOffice_withoutPreexistingUserDoc_succeedsAndSetsActiveOffice() = runBlocking {
    val suffix = UUID.randomUUID().toString().take(6)
    val ownerEmail = "fresh_owner_$suffix@test.com"
    val staffEmail = "fresh_staff_$suffix@test.com"

    signInTestUser(ownerEmail)
    val ownerRepo = OfficeRepository(firestore, auth)
    val office =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        ownerRepo
          .createOffice(
            officeName = "Fresh Desk $suffix",
            ownerDisplayName = "Fresh Owner",
            ownerEmail = ownerEmail,
            ownerPhone = "+919800000001",
            officeLat = 19.076,
            officeLng = 72.8777,
            geofenceRadiusMeters = 150L,
            shiftStartMinutes = 570L,
            shiftEndMinutes = 1110L,
            lateGraceMinutes = 15L,
            workingDaysPerMonth = 26L,
            preferredLanguage = "mr",
          )
          .getOrThrow()
      }
    val ownerProfile =
      withTimeout(FLOW_TIMEOUT_MS) {
        ownerRepo.observeUserProfile(auth.currentUser!!.uid).first()
      }
    assertEquals(office.id, ownerProfile?.activeOfficeId)

    signInTestUser(staffEmail)
    val staffRepo = OfficeRepository(firestore, auth)
    val joinedId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        staffRepo
          .joinOfficeByInviteCode(
            rawCode = office.inviteCode,
            displayName = "Fresh Staff",
            email = staffEmail,
            phone = "9800000002",
            designation = "Clerk",
            preferredLanguage = "mr",
          )
          .getOrThrow()
      }
    assertEquals(office.id, joinedId)
    val staffProfile =
      withTimeout(FLOW_TIMEOUT_MS) {
        staffRepo.observeUserProfile(auth.currentUser!!.uid).first()
      }
    assertEquals(office.id, staffProfile?.activeOfficeId)
  }

  @Test
  fun observeMembers_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = OfficeRepository(firestore, auth)
    var failedWithAuthOrPerm = false
    try {
      withTimeout(FLOW_TIMEOUT_MS) {
        repo.observeDocuments("nonexistent_office").first()
      }
    } catch (e: Exception) {
      val fsEx = e.unwrapFirestoreException()
      failedWithAuthOrPerm =
        e is IllegalStateException || fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
    }
    assertTrue(failedWithAuthOrPerm)
  }

  private companion object {
    const val DEFAULT_TIMEOUT_MS = 8000L
    const val FLOW_TIMEOUT_MS = 5000L
  }
}
