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
}
