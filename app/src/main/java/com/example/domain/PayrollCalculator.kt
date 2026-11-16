// OfficeZen automated payroll & chai cost calculator
package com.example.domain

import com.example.data.model.AttendanceRecord
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.Office
import com.example.data.model.PayrollProfile
import com.example.data.model.SalaryAdjustment
import com.example.data.model.TeaLog
import com.example.data.model.TeaPurchase
import kotlin.math.max
import kotlin.math.roundToInt

data class EmployeePayrollSummary(
  val member: Member,
  val monthKey: String,
  val monthlyBaseSalaryInr: Double,
  val workingDaysInMonth: Int,
  val presentDays: Int,
  val onTimeDays: Int,
  val lateMarks: Int,
  val explicitHalfDays: Int,
  val latePenaltyHalfDays: Int,
  val totalHalfDays: Int,
  val paidLeaveDays: Double,
  val unpaidLeaveDays: Double,
  val absentDays: Double,
  val overtimeMinutes: Long,
  val overtimePayInr: Double,
  val advancesInr: Double,
  val manualDeductionsInr: Double,
  val bonusesInr: Double,
  val attendanceDeductionInr: Double,
  val netPayableInr: Double,
  val teaCups: Int,
  val teaEstimatedCostInr: Double,
)

data class MonthEndSummary(
  val monthKey: String,
  val employeePayrolls: List<EmployeePayrollSummary>,
  val totalSalaryPayableInr: Double,
  val totalPresentDays: Int,
  val totalLateMarks: Int,
  val totalHalfDays: Int,
  val totalOvertimeHours: Double,
  val totalTeaCups: Int,
  val totalCoffeeCups: Int,
  val totalPantryPurchaseInr: Double,
  val effectiveCostPerCupInr: Double,
)

object PayrollCalculator {
  fun calculateEmployeePayroll(
    office: Office,
    member: Member,
    profile: PayrollProfile?,
    monthKey: String,
    attendance: List<AttendanceRecord>,
    leaves: List<LeaveRequest>,
    adjustments: List<SalaryAdjustment>,
    teaLogs: List<TeaLog>,
    effectiveCostPerCupInr: Double,
  ): EmployeePayrollSummary {
    val monthAttendance = attendance.filter {
      it.userId == member.userId && it.dateKey.startsWith(monthKey)
    }
    val monthLeaves = leaves.filter {
      it.userId == member.userId &&
        it.status == "approved" &&
        it.startDateKey.startsWith(monthKey)
    }
    val monthAdjustments = adjustments.filter {
      it.userId == member.userId && it.monthKey == monthKey
    }
    val monthTea = teaLogs.filter {
      it.userId == member.userId && it.monthKey == monthKey
    }

    val workingDays = office.workingDaysPerMonth.toInt().coerceIn(1, 31)
    val baseSalary = profile?.monthlySalaryInr ?: 0.0
    val perDayPay = if (workingDays > 0) baseSalary / workingDays else 0.0

    val onTimeDays = monthAttendance.count { it.status == "on_time" }
    val lateMarks = monthAttendance.count { it.status == "late" }
    val explicitHalfDays = monthAttendance.count { it.status == "half_day" }
    val presentDays = monthAttendance.size

    val lateRule = office.lateMarksPerHalfDay.toInt().coerceAtLeast(1)
    val latePenaltyHalfDays = lateMarks / lateRule
    val totalHalfDays = explicitHalfDays + latePenaltyHalfDays

    val paidLeaveDays = monthLeaves
      .filter { it.leaveType in setOf("casual", "sick", "earned") }
      .sumOf { it.daysCount }
    val unpaidLeaveDays = monthLeaves
      .filter { it.leaveType == "unpaid" || it.leaveType == "half_day" }
      .sumOf { if (it.leaveType == "half_day") it.daysCount * 0.5 else it.daysCount }

    val coveredDays = presentDays.toDouble() + paidLeaveDays
    val absentDays = if (presentDays == 0 && paidLeaveDays == 0.0) {
      0.0
    } else {
      max(0.0, workingDays - coveredDays - unpaidLeaveDays)
    }

    val halfDayDeduction = totalHalfDays * 0.5 * perDayPay
    val unpaidAndAbsentDeduction = (unpaidLeaveDays + absentDays) * perDayPay
    val attendanceDeduction = ((halfDayDeduction + unpaidAndAbsentDeduction) * 100.0).roundToInt() / 100.0

    val overtimeMinutes = monthAttendance.sumOf { it.overtimeMinutes }
    val overtimeRate = profile?.overtimeHourlyRateInr ?: 0.0
    val overtimePay = ((overtimeMinutes / 60.0) * overtimeRate * 100.0).roundToInt() / 100.0

    val advances = monthAdjustments.filter { it.type == "advance" }.sumOf { it.amountInr }
    val manualDeductions = monthAdjustments.filter { it.type == "deduction" }.sumOf { it.amountInr }
    val bonuses = monthAdjustments.filter { it.type == "bonus" }.sumOf { it.amountInr }

    val rawNet = baseSalary - attendanceDeduction + overtimePay + bonuses - advances - manualDeductions
    val netPayable = max(0.0, (rawNet * 100.0).roundToInt() / 100.0)

    val teaCups = monthTea.sumOf { it.quantity.toInt() }
    val teaCost = teaCups * effectiveCostPerCupInr

    return EmployeePayrollSummary(
      member = member,
      monthKey = monthKey,
      monthlyBaseSalaryInr = baseSalary,
      workingDaysInMonth = workingDays,
      presentDays = presentDays,
      onTimeDays = onTimeDays,
      lateMarks = lateMarks,
      explicitHalfDays = explicitHalfDays,
      latePenaltyHalfDays = latePenaltyHalfDays,
      totalHalfDays = totalHalfDays,
      paidLeaveDays = paidLeaveDays,
      unpaidLeaveDays = unpaidLeaveDays,
      absentDays = absentDays,
      overtimeMinutes = overtimeMinutes,
      overtimePayInr = overtimePay,
      advancesInr = advances,
      manualDeductionsInr = manualDeductions,
      bonusesInr = bonuses,
      attendanceDeductionInr = attendanceDeduction,
      netPayableInr = netPayable,
      teaCups = teaCups,
      teaEstimatedCostInr = (teaCost * 100.0).roundToInt() / 100.0,
    )
  }

  fun calculateMonthEndSummary(
    office: Office,
    members: List<Member>,
    profiles: Map<String, PayrollProfile>,
    monthKey: String,
    attendance: List<AttendanceRecord>,
    leaves: List<LeaveRequest>,
    adjustments: List<SalaryAdjustment>,
    teaLogs: List<TeaLog>,
    teaPurchases: List<TeaPurchase>,
  ): MonthEndSummary {
    val monthLogs = teaLogs.filter { it.monthKey == monthKey }
    val monthPurchases = teaPurchases.filter { it.monthKey == monthKey }
    val totalTeaCups = monthLogs.filter { it.item == "tea" }.sumOf { it.quantity.toInt() }
    val totalCoffeeCups = monthLogs.filter { it.item == "coffee" }.sumOf { it.quantity.toInt() }
    val totalCups = totalTeaCups + totalCoffeeCups
    val totalPurchaseSpend = monthPurchases.sumOf { it.amountInr }
    val fallbackTeaCost = monthLogs.sumOf { it.quantity * it.unitCostInr }
    val effectiveCostPerCup = when {
      totalCups > 0 && totalPurchaseSpend > 0.0 -> totalPurchaseSpend / totalCups
      totalCups > 0 -> fallbackTeaCost / totalCups
      else -> office.teaDefaultPriceInr
    }

    val payrolls = members.filter { it.active }.map { member ->
      calculateEmployeePayroll(
        office = office,
        member = member,
        profile = profiles[member.userId],
        monthKey = monthKey,
        attendance = attendance,
        leaves = leaves,
        adjustments = adjustments,
        teaLogs = teaLogs,
        effectiveCostPerCupInr = effectiveCostPerCup,
      )
    }

    return MonthEndSummary(
      monthKey = monthKey,
      employeePayrolls = payrolls,
      totalSalaryPayableInr = payrolls.sumOf { it.netPayableInr },
      totalPresentDays = payrolls.sumOf { it.presentDays },
      totalLateMarks = payrolls.sumOf { it.lateMarks },
      totalHalfDays = payrolls.sumOf { it.totalHalfDays },
      totalOvertimeHours = (payrolls.sumOf { it.overtimeMinutes } / 60.0 * 10.0).roundToInt() / 10.0,
      totalTeaCups = totalTeaCups,
      totalCoffeeCups = totalCoffeeCups,
      totalPantryPurchaseInr = if (totalPurchaseSpend > 0.0) totalPurchaseSpend else fallbackTeaCost,
      effectiveCostPerCupInr = (effectiveCostPerCup * 100.0).roundToInt() / 100.0,
    )
  }
}
