// OfficeZen payroll & geofence unit tests
package com.example.domain

import com.example.data.model.AttendanceRecord
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.data.model.PayrollProfile
import com.example.data.model.SalaryAdjustment
import com.example.data.model.TeaLog
import com.example.data.model.TeaPurchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayrollAndGeofenceTest {

  @Test
  fun payrollCalculator_appliesThreeLateMarksAsOneHalfDayAndDeductsAdvances() {
    val office =
      Office(
        id = "off1",
        ownerId = "o1",
        name = "Test Office",
        workingDaysPerMonth = 26L,
        lateMarksPerHalfDay = 3L,
      )
    val member =
      Member(
        userId = "u1",
        officeId = "off1",
        ownerId = "o1",
        displayName = "Amit",
        role = MemberRole.EMPLOYEE.wire,
      )
    val profile =
      PayrollProfile(
        userId = "u1",
        officeId = "off1",
        ownerId = "o1",
        monthlySalaryInr = 26000.0, // Rs 1000/day
        overtimeHourlyRateInr = 200.0,
      )
    // 26 attendance entries: 23 on_time + 3 late -> 1 penalty half-day (Rs 500)
    val attendance =
      (1..23).map { day ->
        AttendanceRecord(
          userId = "u1",
          dateKey = "2026-11-${day.toString().padStart(2, '0')}",
          status = "on_time",
        )
      } +
        (24..26).map { day ->
          AttendanceRecord(
            userId = "u1",
            dateKey = "2026-11-$day",
            status = "late",
            overtimeMinutes = if (day == 26) 60L else 0L, // +Rs 200 OT
          )
        }
    val adjustments =
      listOf(
        SalaryAdjustment(userId = "u1", monthKey = "2026-11", type = "advance", amountInr = 2000.0)
      )
    val teaLogs =
      listOf(
        TeaLog(userId = "u1", item = "tea", quantity = 10L, unitCostInr = 10.0, monthKey = "2026-11")
      )
    val teaPurchases =
      listOf(
        TeaPurchase(amountInr = 150.0, monthKey = "2026-11")
      )

    val summary =
      PayrollCalculator.calculateMonthEndSummary(
        office = office,
        members = listOf(member),
        profiles = mapOf("u1" to profile),
        monthKey = "2026-11",
        attendance = attendance,
        leaves = emptyList<LeaveRequest>(),
        adjustments = adjustments,
        teaLogs = teaLogs,
        teaPurchases = teaPurchases,
      )

    val emp = summary.employeePayrolls.first()
    assertEquals(3, emp.lateMarks)
    assertEquals(1, emp.latePenaltyHalfDays)
    assertEquals(500.0, emp.attendanceDeductionInr, 0.01)
    assertEquals(200.0, emp.overtimePayInr, 0.01)
    // 26000 - 500 + 200 - 2000 = 23700
    assertEquals(23700.0, emp.netPayableInr, 0.01)
    assertEquals(10, emp.teaCups)
    assertEquals(150.0, emp.teaEstimatedCostInr, 0.01)
  }

  @Test
  fun haversineDistance_computesAccurateMetersBetweenClosePoints() {
    val d = GeofenceValidator.haversineDistanceMeters(19.0760, 72.8777, 19.0769, 72.8777)
    assertTrue("Expected ~100m, got $d", d in 90.0..115.0)
  }

  @Test
  fun haversineDistance_verifies150mGeofenceBoundaryAndLargeCityOffset() {
    // 1.35 km = 1350m? No, 150m boundary test around (18.5204, 73.8567)
    val dInside = GeofenceValidator.haversineDistanceMeters(18.5204, 73.8567, 18.5215, 73.8567) // ~122m
    val dOutside = GeofenceValidator.haversineDistanceMeters(18.5204, 73.8567, 18.5220, 73.8567) // ~178m
    assertTrue("Expected <= 150m inside geofence, got $dInside", dInside <= 150.0)
    assertTrue("Expected > 150m outside geofence, got $dOutside", dOutside > 150.0)

    // Distance between Mumbai default (19.0760, 72.8777) and Nashik/Pune corridor (~138.5 km)
    val dMumbaiToNashik = GeofenceValidator.haversineDistanceMeters(19.0760, 72.8777, 19.9975, 73.7898)
    assertTrue("Expected > 135km between Mumbai default and Nashik, got $dMumbaiToNashik", dMumbaiToNashik in 135_000.0..145_000.0)
  }

  @Test
  fun geofenceValidator_handlesInvalidUnsetAndAntipodalCoordinatesSafely() {
    assertTrue(GeofenceValidator.isUnsetOfficeCoordinate(0.0, 0.0))
    assertTrue(GeofenceValidator.isUnsetOfficeCoordinate(Double.NaN, 72.8777))
    assertTrue(!GeofenceValidator.isUnsetOfficeCoordinate(18.5204, 73.8567))
    assertTrue(GeofenceValidator.haversineDistanceMeters(Double.NaN, 0.0, 19.0, 72.0).isNaN())
    val antipodal = GeofenceValidator.haversineDistanceMeters(0.0, 0.0, 0.0, 180.0)
    assertTrue("Antipodal distance must be finite, got $antipodal", !antipodal.isNaN() && antipodal > 19_000_000.0)
  }

  @Test
  fun checkInStatus_computesOnTimeLateAndHalfDayBoundaries() {
    val shiftStart = 570L // 09:30 AM
    val grace = 60L // 10:30 AM cutoff

    val (onTimeStatus, onTimeLateMins) =
      com.example.ui.viewmodel.OfficeViewModel.computeCheckInStatusForMinutes(630L, shiftStart, grace)
    assertEquals("on_time", onTimeStatus)
    assertEquals(0L, onTimeLateMins)

    val (lateStatus, lateMins) =
      com.example.ui.viewmodel.OfficeViewModel.computeCheckInStatusForMinutes(631L, shiftStart, grace)
    assertEquals("late", lateStatus)
    assertEquals(61L, lateMins)

    val (edgeLateStatus, edgeLateMins) =
      com.example.ui.viewmodel.OfficeViewModel.computeCheckInStatusForMinutes(750L, shiftStart, grace)
    assertEquals("late", edgeLateStatus)
    assertEquals(180L, edgeLateMins)

    // After 12:30 PM (> 570 + 180 = 750 min), status switches to half_day
    val (halfDayStatus, halfDayLateMins) =
      com.example.ui.viewmodel.OfficeViewModel.computeCheckInStatusForMinutes(751L, shiftStart, grace)
    assertEquals("half_day", halfDayStatus)
    assertEquals(181L, halfDayLateMins)
  }
}
