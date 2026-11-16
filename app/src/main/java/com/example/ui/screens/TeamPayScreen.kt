// OfficeZen team payroll, payslip PDF & month-end export screen
package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.data.model.PayrollProfile
import com.example.domain.EmployeePayrollSummary
import com.example.domain.MonthEndSummary
import com.example.export.PdfAndCsvExporter
import com.example.localization.LocalAppStrings
import com.example.ui.components.EmptyPlaceholderCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
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
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val isOwner = me?.roleEnum == MemberRole.OWNER
  val isManagerOrOwner = isOwner || me?.roleEnum == MemberRole.MANAGER
  val profilesMap = remember(profiles) { profiles.associateBy { it.userId } }

  var editingMember by remember { mutableStateOf<Member?>(null) }
  var adjustmentMember by remember { mutableStateOf<Member?>(null) }

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
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Invite Code & Month Selector
      item {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(20.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("Month: $monthKey", style = MaterialTheme.typography.titleMedium)
              Text(
                "Late Rule: ${office.lateMarksPerHalfDay} late = 1 half-day · Invite: ${office.inviteCode}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            OutlinedTextField(
              value = monthKey,
              onValueChange = { if (it.length <= 7) onMonthChange(it) },
              label = { Text("YYYY-MM") },
              singleLine = true,
              modifier = Modifier.width(120.dp),
            )
          }
        }
      }

      // Month-End Owner Report & Export Card (Owner & Manager)
      if (isManagerOrOwner && summary != null) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text(strings.monthEndReport, style = MaterialTheme.typography.titleLarge)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                MetricCol("Total Salary", "₹${summary.totalSalaryPayableInr.roundToInt()}")
                MetricCol("Present Days", "${summary.totalPresentDays}")
                MetricCol("Chai + Coffee", "${summary.totalTeaCups + summary.totalCoffeeCups}")
                MetricCol("Pantry Spend", "₹${summary.totalPantryPurchaseInr.roundToInt()}")
              }
              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Button(
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
                  modifier = Modifier.weight(1f).height(48.dp).testTag("export_whatsapp_button"),
                ) {
                  Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(strings.shareWhatsapp, maxLines = 1)
                }
                FilledTonalButton(
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
                  modifier = Modifier.weight(1f).height(48.dp).testTag("export_email_button"),
                ) {
                  Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(strings.shareEmail, maxLines = 1)
                }
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                OutlinedButton(
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
                  modifier = Modifier.weight(1f).height(48.dp).testTag("export_pdf_button"),
                ) {
                  Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text("PDF", maxLines = 1)
                }
                OutlinedButton(
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
                  modifier = Modifier.weight(1f).height(48.dp).testTag("export_csv_button"),
                ) {
                  Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(6.dp))
                  Text(strings.exportCsv, maxLines = 1)
                }
              }
            }
          }
        }
      }

      item {
        SectionHeader(
          title = if (isManagerOrOwner) "Team & Monthly Payroll" else "My Salary & Payslip",
          subtitle = "Auto-computed from attendance, 3-late rule, leaves, overtime & advances",
        )
      }

      val payrollRows =
        (summary?.employeePayrolls ?: emptyList()).let { list ->
          if (isManagerOrOwner) list else list.filter { it.member.userId == me?.userId }
        }

      if (payrollRows.isEmpty()) {
        item {
          EmptyPlaceholderCard(
            title = "No payroll entries yet",
            body = "Share invite code ${office.inviteCode} with your staff or wait for team data to sync.",
          )
        }
      } else {
        items(payrollRows, key = { it.member.userId }) { row ->
          PayrollMemberCard(
            row = row,
            isOwner = isOwner,
            isManagerOrOwner = isManagerOrOwner,
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
private fun MetricCol(label: String, value: String) {
  Column {
    Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
  }
}

@Composable
private fun PayrollMemberCard(
  row: EmployeePayrollSummary,
  isOwner: Boolean,
  isManagerOrOwner: Boolean,
  onEdit: () -> Unit,
  onAddAdjustment: () -> Unit,
  onGeneratePayslip: () -> Unit,
) {
  val strings = LocalAppStrings.current
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(20.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(row.member.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            StatusPill(
              text = row.member.role.uppercase(),
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
          Text(
            "${row.member.designation} · Base ₹${row.monthlyBaseSalaryInr.roundToInt()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (isOwner) {
          IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_member_${row.member.userId}")) {
            Icon(Icons.Rounded.Edit, contentDescription = "Edit role and salary")
          }
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          "Present: ${row.presentDays}d · Late: ${row.lateMarks} (${row.latePenaltyHalfDays} ½d)",
          style = MaterialTheme.typography.bodyMedium,
        )
        Text(
          "OT: ${row.overtimeMinutes}m",
          style = MaterialTheme.typography.bodyMedium,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          "Adv/Ded: -₹${(row.advancesInr + row.manualDeductionsInr + row.attendanceDeductionInr).roundToInt()} · Tea: ${row.teaCups} cups (₹${row.teaEstimatedCostInr.roundToInt()})",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(strings.netPay, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text(
            "₹${row.netPayableInr.roundToInt()}",
            style = MaterialTheme.typography.headlineSmall,
            color = StatusGreen,
            fontWeight = FontWeight.Bold,
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (isManagerOrOwner) {
            OutlinedButton(
              onClick = onAddAdjustment,
              modifier = Modifier.height(48.dp),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text("Adv/Ded")
            }
          }
          FilledTonalButton(
            onClick = onGeneratePayslip,
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
    title = { Text("Edit ${member.displayName}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (isOwner) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MemberRole.entries.forEach { role ->
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
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = salaryText,
          onValueChange = { salaryText = it },
          label = { Text("Monthly Salary (₹)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = otRateText,
          onValueChange = { otRateText = it },
          label = { Text("Overtime Rate (₹/hour)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = phone,
          onValueChange = { phone = it },
          label = { Text("Phone") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          modifier = Modifier.fillMaxWidth(),
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Active Member")
          Switch(checked = active, onCheckedChange = { active = it })
        }
      }
    },
    confirmButton = {
      Button(
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
    title = { Text("Salary Adjustment · ${member.displayName}") },
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
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Note / Reason") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
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
