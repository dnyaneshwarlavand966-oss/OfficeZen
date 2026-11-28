// OfficeZen neo-bank style team payroll, payslip PDF & month-end export screen
package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.canViewTeamPayroll
import com.example.data.model.Office
import com.example.data.model.PayrollProfile
import com.example.security.PayrollPinManager
import com.example.domain.EmployeePayrollSummary
import com.example.domain.MonthEndSummary
import com.example.export.PdfAndCsvExporter
import com.example.localization.LocalAppStrings
import com.example.ui.components.EmptyPlaceholderCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZenCard
import com.example.ui.theme.ChaiTerracotta
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.LimeInk
import com.example.ui.theme.LimeSpark
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.PineDeep
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TeamPayScreen(
  office: Office,
  me: Member?,
  members: List<Member>,
  profiles: List<PayrollProfile>,
  monthKey: String,
  summary: MonthEndSummary?,
  onMonthChange: (String) -> Unit,
  onUpdateMember: (
    member: Member,
    newRole: MemberRole,
    designation: String,
    active: Boolean,
    salaryInr: Double,
    otRateInr: Double,
    phone: String,
    email: String,
  ) -> Unit,
  onAddAdjustment: (targetMember: Member, type: String, amountInr: Double, note: String) -> Unit,
  onAddLocalMember: (
    displayName: String,
    designation: String,
    role: MemberRole,
    monthlySalaryInr: Double,
    overtimeHourlyRateInr: Double,
    phone: String,
  ) -> Unit = { _, _, _, _, _, _ -> },
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val isOwner = me?.roleEnum == MemberRole.OWNER
  val isPayrollAdmin = me?.roleEnum?.canViewTeamPayroll() == true
  val profilesMap = remember(profiles) { profiles.associateBy { it.userId } }

  var editingMember by remember { mutableStateOf<Member?>(null) }
  var adjustmentMember by remember { mutableStateOf<Member?>(null) }
  var showAddStaffDialog by remember { mutableStateOf(false) }
  var payrollPinUnlocked by remember(office.id, me?.userId) { mutableStateOf(false) }

  // Team payroll is gated on this device. The PIN is protected by Android Keystore;
  // cloud/server authorization must still be enforced before enabling remote payroll.
  if (isPayrollAdmin && !payrollPinUnlocked) {
    PayrollPinGate(
      isOwner = isOwner,
      onUnlocked = { payrollPinUnlocked = true },
    )
    return
  }

  if (showAddStaffDialog) {
    AddStaffDialog(
      onSave = { name, desig, role, sal, ot, phone ->
        onAddLocalMember(name, desig, role, sal, ot, phone)
        showAddStaffDialog = false
      },
      onDismiss = { showAddStaffDialog = false },
    )
  }

  editingMember?.let { target ->
    val prof = profilesMap[target.userId]
    EditMemberPayDialog(
      member = target,
      profile = prof,
      isOwner = isOwner,
      onSave = { role, desig, active, sal, ot, phone, email ->
        onUpdateMember(target, role, desig, active, sal, ot, phone, email)
        editingMember = null
      },
      onDismiss = { editingMember = null },
    )
  }

  adjustmentMember?.let { target ->
    AddAdjustmentDialog(
      member = target,
      onSave = { type, amount, note ->
        onAddAdjustment(target, type, amount, note)
        adjustmentMember = null
      },
      onDismiss = { adjustmentMember = null },
    )
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 700.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Clean Month Stepper Bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "PAYROLL CYCLE",
              style = MaterialTheme.typography.labelMedium,
              fontFamily = MonoFontFamily,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "${office.lateMarksPerHalfDay} late = ½ day · ${office.workingDaysPerMonth}d/mo",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(50),
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            ) {
              IconButton(
                onClick = { onMonthChange(stepMonth(monthKey, -1)) },
                modifier = Modifier.size(40.dp).minimumInteractiveComponentSize(),
              ) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous month", modifier = Modifier.size(20.dp))
              }
              Text(
                text = monthKey,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp),
              )
              IconButton(
                onClick = { onMonthChange(stepMonth(monthKey, +1)) },
                modifier = Modifier.size(40.dp).minimumInteractiveComponentSize(),
              ) {
                Icon(Icons.Rounded.ChevronRight, contentDescription = "Next month", modifier = Modifier.size(20.dp))
              }
            }
          }
        }
      }

      // Neo-Bank Treasury Payroll Card (Manager / Owner)
      if (isPayrollAdmin && summary != null) {
        item {
          Surface(
            color = PineDeep,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = strings.monthEndReport.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFontFamily,
                    color = Color.White.copy(alpha = 0.7f),
                  )
                  Text(
                    text = "₹${summary.totalSalaryPayableInr.roundToInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    fontFamily = EditorialSerifFontFamily,
                    color = Color.White,
                  )
                  Text(
                    text = "Total Net Payable · ${members.size} team member(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.78f),
                  )
                }
                StatusPill(
                  text = office.inviteCode,
                  containerColor = Color.White.copy(alpha = 0.14f),
                  contentColor = LimeSpark,
                  useMonospace = true,
                )
              }

              // Thin proportional Canvas bar (Salary vs Pantry)
              val salaryAmt = summary.totalSalaryPayableInr.toFloat().coerceAtLeast(1f)
              val pantryAmt = summary.totalPantryPurchaseInr.toFloat().coerceAtLeast(0f)
              val totalSpend = (salaryAmt + pantryAmt).coerceAtLeast(1f)
              val salaryFrac = (salaryAmt / totalSpend).coerceIn(0.08f, 1f)

              Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Canvas(modifier = Modifier.fillMaxWidth().height(7.dp)) {
                  val salW = size.width * salaryFrac
                  drawRoundRect(
                    color = LimeSpark,
                    size = Size(salW, size.height),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                  )
                  if (size.width - salW > 6.dp.toPx()) {
                    drawRoundRect(
                      color = ChaiTerracotta,
                      topLeft = Offset(salW + 4.dp.toPx(), 0f),
                      size = Size((size.width - salW - 4.dp.toPx()).coerceAtLeast(0f), size.height),
                      cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    )
                  }
                }
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = "Present ${summary.totalPresentDays}d",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFontFamily,
                    color = LimeSpark,
                  )
                  Text(
                    text = "${summary.totalTeaCups + summary.totalCoffeeCups} cups · Pantry ₹${summary.totalPantryPurchaseInr.roundToInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFontFamily,
                    color = Color.White.copy(alpha = 0.8f),
                  )
                }
              }

              // Crisp Export Dock
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                TreasuryActionChip(
                  label = strings.shareWhatsapp,
                  icon = Icons.Rounded.Share,
                  isPrimary = true,
                  onClick = {
                    val file = PdfAndCsvExporter.generateMonthEndPdf(context, office, summary)
                    val body = PdfAndCsvExporter.buildMonthEndSummaryText(office, summary)
                    PdfAndCsvExporter.shareFile(
                      context = context,
                      file = file,
                      mimeType = "application/pdf",
                      subject = "${office.name} Month-End Report ($monthKey)",
                      bodyText = body,
                      targetPackage = "com.whatsapp",
                    )
                  },
                  modifier = Modifier.weight(1f).testTag("export_whatsapp_button"),
                )
                TreasuryActionChip(
                  label = strings.shareEmail,
                  icon = Icons.Rounded.Email,
                  onClick = {
                    val file = PdfAndCsvExporter.generateMonthEndPdf(context, office, summary)
                    val body = PdfAndCsvExporter.buildMonthEndSummaryText(office, summary)
                    PdfAndCsvExporter.shareFile(
                      context = context,
                      file = file,
                      mimeType = "application/pdf",
                      subject = "${office.name} Month-End Report ($monthKey)",
                      bodyText = body,
                      emailRecipient = office.ownerReportEmail,
                    )
                  },
                  modifier = Modifier.weight(1f).testTag("export_email_button"),
                )
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                TreasuryActionChip(
                  label = "PDF",
                  icon = Icons.Rounded.PictureAsPdf,
                  onClick = {
                    val file = PdfAndCsvExporter.generateMonthEndPdf(context, office, summary)
                    PdfAndCsvExporter.shareFile(
                      context = context,
                      file = file,
                      mimeType = "application/pdf",
                      subject = "${office.name} Month-End PDF ($monthKey)",
                      bodyText = "Month-end PDF report attached.",
                    )
                  },
                  modifier = Modifier.weight(1f).testTag("export_pdf_button"),
                )
                TreasuryActionChip(
                  label = strings.exportCsv,
                  icon = Icons.Rounded.TableChart,
                  onClick = {
                    val file = PdfAndCsvExporter.generateMonthEndCsv(context, office, summary)
                    PdfAndCsvExporter.shareFile(
                      context = context,
                      file = file,
                      mimeType = "text/csv",
                      subject = "${office.name} Excel CSV ($monthKey)",
                      bodyText = "Month-end Excel CSV report attached.",
                    )
                  },
                  modifier = Modifier.weight(1f).testTag("export_csv_button"),
                )
              }
            }
          }
        }
      }

      val payrollRows =
        (summary?.employeePayrolls ?: emptyList()).let { list ->
          if (isPayrollAdmin) list else list.filter { it.member.userId == me?.userId }
        }

      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SectionHeader(
            title = if (isPayrollAdmin) "Team & Monthly Payroll" else "My Salary & Payslip",
            subtitle = "3-late rule, leaves, overtime & advances",
            trailingBadge = "${payrollRows.size} staff",
          )
          if (isPayrollAdmin) {
            Button(
              onClick = { showAddStaffDialog = true },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().height(50.dp).testTag("add_staff_button"),
            ) {
              Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("Add Staff Member", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      if (payrollRows.isEmpty()) {
        item {
          EmptyPlaceholderCard(
            title = "No payroll entries yet",
            body = "Tap Add Staff Member above to add employees on this device.",
          )
        }
      } else {
        items(payrollRows, key = { it.member.userId }) { row ->
          PayrollMemberCard(
            row = row,
            isOwner = isOwner,
            isPayrollAdmin = isPayrollAdmin,
            onEdit = { editingMember = row.member },
            onAddAdjustment = { adjustmentMember = row.member },
            onGeneratePayslip = {
              val file = PdfAndCsvExporter.generatePayslipPdf(context, office, row)
              PdfAndCsvExporter.shareFile(
                context = context,
                file = file,
                mimeType = "application/pdf",
                subject = "${row.member.displayName} Payslip ($monthKey)",
                bodyText = "Salary payslip for ${row.member.displayName} ($monthKey): Net Rs. ${row.netPayableInr.roundToInt()}",
              )
            },
          )
        }
      }
    }
  }
}

@Composable
private fun TreasuryActionChip(
  label: String,
  icon: ImageVector,
  isPrimary: Boolean = false,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    color = if (isPrimary) LimeSpark else Color.White.copy(alpha = 0.12f),
    shape = RoundedCornerShape(13.dp),
    border = if (isPrimary) null else BorderStroke(0.75.dp, Color.White.copy(alpha = 0.16f)),
    modifier = modifier.height(48.dp),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isPrimary) LimeInk else Color.White,
        modifier = Modifier.size(17.dp),
      )
      Spacer(Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = if (isPrimary) LimeInk else Color.White,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun PayrollMemberCard(
  row: EmployeePayrollSummary,
  isOwner: Boolean,
  isPayrollAdmin: Boolean,
  onEdit: () -> Unit,
  onAddAdjustment: () -> Unit,
  onGeneratePayslip: () -> Unit,
) {
  val strings = LocalAppStrings.current
  ZenCard(
    cornerRadius = 20.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = CircleShape,
          border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.size(44.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = row.member.displayName.take(1).uppercase(),
              style = MaterialTheme.typography.titleMedium,
              fontFamily = EditorialSerifFontFamily,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = row.member.displayName,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.weight(1f, fill = false),
            )
            StatusPill(
              text = row.member.role.uppercase(),
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          Text(
            text = "${row.member.designation} · Base ₹${row.monthlyBaseSalaryInr.roundToInt()}",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (isOwner) {
          IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_member_${row.member.userId}")) {
            Icon(
              Icons.Rounded.Edit,
              contentDescription = "Edit role and salary",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        StatusPill(
          text = "Present ${row.presentDays}d",
          containerColor = StatusGreenBg,
          contentColor = StatusGreen,
          useMonospace = true,
        )
        StatusPill(
          text = "Late ${row.lateMarks} (${row.latePenaltyHalfDays}½d)",
          containerColor = if (row.lateMarks > 0) StatusAmberBg else MaterialTheme.colorScheme.surfaceVariant,
          contentColor = if (row.lateMarks > 0) StatusAmber else MaterialTheme.colorScheme.onSurfaceVariant,
          useMonospace = true,
        )
        StatusPill(
          text = "OT ${row.overtimeMinutes}m",
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          useMonospace = true,
        )
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Adv/Ded: -₹${(row.advancesInr + row.manualDeductionsInr + row.attendanceDeductionInr).roundToInt()}",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "Tea: ${row.teaCups} cups (₹${row.teaEstimatedCostInr.roundToInt()})",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.secondary,
          )
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f), thickness = 0.75.dp)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = strings.netPay.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "₹${row.netPayableInr.roundToInt()}",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = EditorialSerifFontFamily,
            color = StatusGreen,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (isPayrollAdmin) {
            OutlinedButton(
              onClick = onAddAdjustment,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Adv/Ded")
            }
          }
          FilledTonalButton(
            onClick = onGeneratePayslip,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp).testTag("payslip_pdf_${row.member.userId}"),
          ) {
            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(strings.payslipPdf)
          }
        }
      }
    }
  }
}

private fun stepMonth(monthKey: String, deltaMonths: Int): String {
  val parts = monthKey.split("-")
  val year = parts.getOrNull(0)?.toIntOrNull() ?: return monthKey
  val month = parts.getOrNull(1)?.toIntOrNull() ?: return monthKey
  val total = (year * 12 + (month - 1)) + deltaMonths
  val nextYear = total / 12
  val nextMonth = (total % 12) + 1
  return String.format(Locale.US, "%04d-%02d", nextYear, nextMonth)
}

@Composable
private fun EditMemberPayDialog(
  member: Member,
  profile: PayrollProfile?,
  isOwner: Boolean,
  onSave: (MemberRole, String, Boolean, Double, Double, String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedRole by remember { mutableStateOf(member.roleEnum) }
  var designation by remember { mutableStateOf(member.designation) }
  var active by remember { mutableStateOf(member.active) }
  var salaryText by remember { mutableStateOf((profile?.monthlySalaryInr ?: 0.0).roundToInt().toString()) }
  var otRateText by remember { mutableStateOf((profile?.overtimeHourlyRateInr ?: 0.0).roundToInt().toString()) }
  var phone by remember { mutableStateOf(profile?.phone ?: "") }
  var email by remember { mutableStateOf(profile?.email ?: "") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    title = { Text("Edit ${member.displayName}", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isOwner) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (if (member.roleEnum == MemberRole.OWNER) listOf(MemberRole.OWNER) else listOf(MemberRole.EMPLOYEE, MemberRole.MANAGER, MemberRole.ACCOUNTANT)).forEach { role ->
              FilterChip(
                selected = selectedRole == role,
                onClick = { selectedRole = role },
                label = { Text(role.wire.replaceFirstChar { it.uppercase() }) },
              )
            }
          }
        }
        OutlinedTextField(
          value = designation,
          onValueChange = { designation = it },
          label = { Text("Designation") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = salaryText,
          onValueChange = { salaryText = it },
          label = { Text("Monthly Salary (₹)") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = otRateText,
          onValueChange = { otRateText = it },
          label = { Text("Overtime Rate (₹/hour)") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Active Member", style = MaterialTheme.typography.bodyLarge)
          Switch(checked = active, onCheckedChange = { active = it })
        }
      }
    },
    confirmButton = {
      Button(
        shape = RoundedCornerShape(14.dp),
        onClick = {
          onSave(
            selectedRole,
            designation,
            active,
            salaryText.toDoubleOrNull() ?: 0.0,
            otRateText.toDoubleOrNull() ?: 0.0,
            phone,
            email,
          )
        },
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun AddAdjustmentDialog(
  member: Member,
  onSave: (type: String, amount: Double, note: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var type by remember { mutableStateOf("advance") }
  var amountText by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    title = { Text("Salary Adjustment · ${member.displayName}", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("advance", "deduction", "bonus").forEach { opt ->
            FilterChip(
              selected = type == opt,
              onClick = { type = opt },
              label = { Text(opt.replaceFirstChar { it.uppercase() }) },
            )
          }
        }
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Amount (₹)") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Note / Reason") },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        shape = RoundedCornerShape(14.dp),
        onClick = {
          val amt = amountText.toDoubleOrNull() ?: 0.0
          if (amt > 0.0) onSave(type, amt, note)
        },
        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0,
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}

@Composable
private fun AddStaffDialog(
  onSave: (
    displayName: String,
    designation: String,
    role: MemberRole,
    monthlySalaryInr: Double,
    overtimeHourlyRateInr: Double,
    phone: String,
  ) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var designation by remember { mutableStateOf("Executive") }
  var selectedRole by remember { mutableStateOf(MemberRole.EMPLOYEE) }
  var salaryText by remember { mutableStateOf("18000") }
  var otRateText by remember { mutableStateOf("120") }
  var phone by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    title = { Text("Add Staff Member", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Full Name") },
          placeholder = { Text("e.g., Priya Kulkarni") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("add_staff_name_input"),
        )
        OutlinedTextField(
          value = designation,
          onValueChange = { designation = it },
          label = { Text("Designation") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(MemberRole.EMPLOYEE, MemberRole.MANAGER, MemberRole.ACCOUNTANT).forEach { role ->
            FilterChip(
              selected = selectedRole == role,
              onClick = { selectedRole = role },
              label = { Text(role.wire.replaceFirstChar { it.uppercase() }) },
            )
          }
        }
        OutlinedTextField(
          value = salaryText,
          onValueChange = { salaryText = it },
          label = { Text("Monthly Salary (₹)") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = otRateText,
          onValueChange = { otRateText = it },
          label = { Text("Overtime Rate (₹/hour)") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Mobile Number") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        shape = RoundedCornerShape(14.dp),
        enabled = name.isNotBlank(),
        onClick = {
          onSave(
            name.trim(),
            designation.trim().ifBlank { "Staff" },
            selectedRole,
            salaryText.toDoubleOrNull() ?: 0.0,
            otRateText.toDoubleOrNull() ?: 0.0,
            phone.trim(),
          )
        },
        modifier = Modifier.testTag("confirm_add_staff_button"),
      ) {
        Text("Add Staff")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  )
}


@Composable
private fun PayrollPinGate(
  isOwner: Boolean,
  onUnlocked: () -> Unit,
) {
  val context = LocalContext.current
  val manager = remember(context) { PayrollPinManager(context) }
  var hasPin by remember(manager) { mutableStateOf(manager.hasPin()) }
  var pin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var error by remember { mutableStateOf("") }

  Box(
    modifier = Modifier.fillMaxSize().padding(20.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = if (hasPin) "Payroll PIN Required" else "Set Boss Payroll PIN",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = if (hasPin) {
            "Enter the six-digit PIN set by the Office Owner to open team payroll."
          } else if (isOwner) {
            "Create a six-digit PIN. It is protected by this device's Android Keystore and is not stored as plain text."
          } else {
            "The Boss must set the Payroll PIN on this device before an assigned Accountant can open team payroll."
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (hasPin || isOwner) {
          OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(6); error = "" },
            label = { Text(if (hasPin) "Six-digit Payroll PIN" else "Create six-digit PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().testTag("payroll_pin_input"),
          )
        }

        if (!hasPin && isOwner) {
          OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it.filter(Char::isDigit).take(6); error = "" },
            label = { Text("Confirm PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth().testTag("payroll_pin_confirm_input"),
          )
        }

        if (error.isNotBlank()) {
          Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (hasPin) {
          Button(
            onClick = {
              if (manager.verifyPin(pin)) {
                error = ""
                onUnlocked()
              } else {
                error = "PIN चुकीचा आहे किंवा प्रयत्न तात्पुरते लॉक झाले आहेत."
                pin = ""
              }
            },
            enabled = PayrollPinManager.isValidPin(pin),
            modifier = Modifier.fillMaxWidth().testTag("verify_payroll_pin_button"),
          ) { Text("Unlock Payroll") }
        } else if (isOwner) {
          Button(
            onClick = {
              if (pin != confirmPin) {
                error = "दोन्ही PIN जुळत नाहीत."
              } else if (manager.setPin(pin)) {
                hasPin = true
                pin = ""
                confirmPin = ""
                onUnlocked()
              } else {
                error = "PIN सुरक्षितपणे सेव्ह करता आला नाही. पुन्हा प्रयत्न करा."
              }
            },
            enabled = PayrollPinManager.isValidPin(pin) && pin == confirmPin,
            modifier = Modifier.fillMaxWidth().testTag("set_payroll_pin_button"),
          ) { Text("Set PIN & Continue") }
        }

        Text(
          "टीप: हा PIN या डिव्हाइसवरील अतिरिक्त लॉक आहे. ऑनलाइन/क्लाउड Payroll साठी सर्व्हरवरील स्वतंत्र अधिकार तपासणी आवश्यक आहे.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}
