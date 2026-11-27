// OfficeZen leaves, on-device immutable audit log & grouped settings screen
package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditLog
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.data.model.OfficeDocument
import com.example.domain.MonthEndSummary
import com.example.export.PdfAndCsvExporter
import com.example.localization.DateFormatPattern
import com.example.localization.GlobalCurrencies
import com.example.localization.GlobalFormatter
import com.example.localization.GlobalLocaleConfig
import com.example.localization.IndianLanguages
import com.example.localization.LocalAppStrings
import com.example.localization.LocalGlobalLocaleConfig
import com.example.localization.ThemeMode
import com.example.notifications.NotificationHelper
import com.example.notifications.ReminderScheduler
import com.example.ui.components.CloudAttendanceSyncCard
import com.example.ui.components.EmptyPlaceholderCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedTabItem
import com.example.ui.components.StatusPill
import com.example.ui.components.ZenCard
import com.example.ui.components.ZenSegmentedTabs
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientMid
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.Marigold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.viewmodel.OfficeViewModel
import java.io.File
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun MoreAuditScreen(
  initialSubTab: Int = 0,
  openLeaveDialogInitially: Boolean,
  onClearLeaveDialogFlag: () -> Unit,
  office: Office,
  me: Member?,
  leaves: List<LeaveRequest>,
  auditLogs: List<AuditLog>,
  attendance: List<AttendanceRecord>,
  documents: List<OfficeDocument>,
  monthEndSummary: MonthEndSummary? = null,
  selectedLanguageCode: String,
  remindersEnabled: Boolean,
  pendingSyncCount: Int = 0,
  syncedCount: Int = 0,
  isNetworkOnline: Boolean = false,
  isSyncRunning: Boolean = false,
  lastSyncEpochMs: Long = 0L,
  lastSyncMessage: String = "",
  googleUserEmail: String? = null,
  isAuthLoading: Boolean = false,
  onSignInWithGoogle: () -> Unit = {},
  onSignOutGoogle: () -> Unit = {},
  onTriggerSyncNow: () -> Unit = {},
  onToggleReminders: (Boolean) -> Unit,
  onOpenLanguagePicker: () -> Unit,
  onSubmitLeave: (leaveType: String, start: String, end: String, days: Double, reason: String) -> Unit,
  onReviewLeave: (leave: LeaveRequest, approved: Boolean, note: String) -> Unit,
  onUpdateOfficeSettings: (Office) -> Unit,
  onSwitchWorkspace: () -> Unit,
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  globalLocaleConfig: GlobalLocaleConfig = LocalGlobalLocaleConfig.current,
  onSetThemeMode: (ThemeMode) -> Unit = {},
  onUpdateGlobalLocaleConfig: (GlobalLocaleConfig) -> Unit = {},
  onOpenSubscription: (() -> Unit)? = null,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val isOwner = me?.roleEnum == MemberRole.OWNER
  val isManagerOrOwner = isOwner || me?.roleEnum == MemberRole.MANAGER

  var subTab by remember { mutableIntStateOf(initialSubTab) }
  LaunchedEffect(initialSubTab) {
    subTab = initialSubTab
  }
  var showApplyLeave by remember(openLeaveDialogInitially) {
    mutableStateOf(openLeaveDialogInitially)
  }

  val notifPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      onToggleReminders(granted)
      ReminderScheduler.scheduleDailyAndMonthEndReminders(context, granted)
    }

  if (showApplyLeave) {
    ApplyLeaveDialog(
      onSubmit = { type, start, end, days, reason ->
        onSubmitLeave(type, start, end, days, reason)
        showApplyLeave = false
        onClearLeaveDialogFlag()
      },
      onDismiss = {
        showApplyLeave = false
        onClearLeaveDialogFlag()
      },
    )
  }

  val tabItems =
    remember(isManagerOrOwner, strings) {
      buildList {
        add(SegmentedTabItem(label = strings.navMore, testTag = "subtab_reports"))
        add(SegmentedTabItem(label = strings.modLeave, testTag = "subtab_leaves"))
        if (isManagerOrOwner) {
          add(SegmentedTabItem(label = strings.auditLog, testTag = "subtab_audit"))
        }
        add(SegmentedTabItem(label = strings.settings, testTag = "subtab_settings"))
      }
    }

  val safeSubTab = subTab.coerceIn(0, tabItems.lastIndex)
  val activeTag = tabItems[safeSubTab].testTag

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 700.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        ZenSegmentedTabs(
          items = tabItems,
          selectedIndex = safeSubTab,
          onSelect = { subTab = it },
        )
      }

      if (activeTag == "subtab_reports") {
        item {
          ReportsHubSection(
            office = office,
            summary = monthEndSummary,
            attendanceCount = attendance.size,
            documentsCount = documents.size,
            leavesCount = leaves.size,
            onExportJsonBackup = {
              val backup =
                JSONObject().apply {
                  put("officeName", office.name)
                  put("inviteCode", office.inviteCode)
                  put("attendanceCount", attendance.size)
                  put("leavesCount", leaves.size)
                  put("documentsCount", documents.size)
                  put(
                    "documents",
                    JSONArray(
                      documents.map { d ->
                        JSONObject().apply {
                          put("serialNo", d.serialNo)
                          put("direction", d.direction)
                          put("partyName", d.partyName)
                          put("subject", d.subject)
                          put("status", d.status)
                        }
                      }
                    ),
                  )
                }
              val dir = File(context.cacheDir, "exports").apply { mkdirs() }
              val file = File(dir, "OfficeZen_Backup_${OfficeViewModel.currentDateKey()}.json")
              file.writeText(backup.toString(2))
              PdfAndCsvExporter.shareFile(
                context = context,
                file = file,
                mimeType = "application/json",
                subject = "${office.name} Local Backup JSON",
                bodyText = "OfficeZen local backup snapshot attached.",
              )
            },
          )
        }
      } else if (activeTag == "subtab_leaves") {
        item {
          Button(
            onClick = { showApplyLeave = true },
            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("apply_leave_button"),
            shape = RoundedCornerShape(14.dp),
          ) {
            Icon(Icons.AutoMirrored.Rounded.EventNote, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.leaveRequest, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          }
        }

        if (leaves.isEmpty()) {
          item {
            EmptyPlaceholderCard(
              title = "No leave requests",
              body = "Submit casual, sick, earned, or half-day leaves here for Manager/Owner approval.",
            )
          }
        } else {
          items(leaves, key = { it.id }) { lv ->
            val (bg, fg, statusLabel) =
              when (lv.status) {
                "approved" -> Triple(StatusGreenBg, StatusGreen, strings.approved)
                "rejected" -> Triple(StatusRedBg, StatusRed, strings.rejected)
                else -> Triple(StatusAmberBg, StatusAmber, strings.pending)
              }
            ZenCard(
              cornerRadius = 18.dp,
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
                  Text(lv.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                  StatusPill(statusLabel, bg, fg)
                }
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  StatusPill(
                    text = lv.leaveType.uppercase(),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Text(
                    text = "${lv.startDateKey} → ${lv.endDateKey} (${lv.daysCount}d)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(lv.reason, style = MaterialTheme.typography.bodyLarge)
                if (lv.status == "pending" && isManagerOrOwner && (lv.userId != me?.userId || isOwner)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                  ) {
                    Button(
                      onClick = {
                        onReviewLeave(lv, true, "Approved")
                        NotificationHelper.showNotification(
                          context,
                          lv.id.hashCode(),
                          "Leave Approved",
                          "${lv.userName}'s ${lv.leaveType} leave (${lv.startDateKey}) was approved.",
                        )
                      },
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier.weight(1f).height(48.dp).testTag("approve_leave_${lv.id}"),
                    ) {
                      Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(strings.approve)
                    }
                    OutlinedButton(
                      onClick = { onReviewLeave(lv, false, "Declined") },
                      colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                      border = BorderStroke(0.75.dp, StatusRed.copy(alpha = 0.45f)),
                      shape = RoundedCornerShape(12.dp),
                      modifier = Modifier.weight(1f).height(48.dp).testTag("reject_leave_${lv.id}"),
                    ) {
                      Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                      Spacer(Modifier.width(4.dp))
                      Text(strings.reject)
                    }
                  }
                }
              }
            }
          }
        }
      } else if (activeTag == "subtab_audit" && isManagerOrOwner) {
        item {
          SectionHeader(
            title = strings.auditLog,
            subtitle = "Immutable on-device ledger (JSON exportable)",
            trailingBadge = "${auditLogs.take(60).size} events",
          )
        }
        if (auditLogs.isEmpty()) {
          item {
            EmptyPlaceholderCard(
              title = "No audit logs yet",
              body = "Every check-in, leave decision, salary change, tea purchase, and document entry appears here.",
            )
          }
        } else {
          items(auditLogs.take(60), key = { it.id }) { log ->
            ZenCard(
              cornerRadius = 16.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                Box(
                  modifier =
                    Modifier
                      .width(3.dp)
                      .fillMaxHeight()
                      .padding(vertical = 12.dp)
                      .clip(RoundedCornerShape(4.dp))
                      .background(MaterialTheme.colorScheme.primary),
                )
                Column(
                  modifier = Modifier.padding(14.dp).weight(1f),
                  verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    StatusPill(
                      text = log.actionType,
                      containerColor = MaterialTheme.colorScheme.primaryContainer,
                      contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                      useMonospace = true,
                    )
                    Text(
                      text = log.actorRole.uppercase(),
                      style = MaterialTheme.typography.labelMedium,
                      fontFamily = MonoFontFamily,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                  Text(log.summary, style = MaterialTheme.typography.bodyMedium)
                }
              }
            }
          }
        }
      } else {
        item {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Cloud Sync & Backup")
            CloudAttendanceSyncCard(
              pendingCount = pendingSyncCount,
              syncedCount = syncedCount,
              isOnline = isNetworkOnline,
              isSyncRunning = isSyncRunning,
              lastSyncEpochMs = lastSyncEpochMs,
              lastSyncMessage = lastSyncMessage,
              googleUserEmail = googleUserEmail,
              isAuthLoading = isAuthLoading,
              onSignInWithGoogle = onSignInWithGoogle,
              onSignOutGoogle = onSignOutGoogle,
              onTriggerSyncNow = onTriggerSyncNow,
            )
          }
        }

        // Grouped Hairline Settings Container (Linear / iOS-style clean rows)
        item {
          val lang = IndianLanguages.findByCode(selectedLanguageCode)
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(title = "Preferences")
            ZenCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
              Column {
                Row(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .clickable(onClick = onOpenLanguagePicker)
                      .padding(horizontal = 16.dp, vertical = 14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                      Text(strings.language, style = MaterialTheme.typography.titleMedium)
                      Text(
                        text = "${lang.nativeName} (${lang.englishName})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                  )
                }

                HorizontalDivider(
                  color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                  thickness = 0.75.dp,
                )

                Row(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 16.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                      Text("Daily & Month-End Reminders", style = MaterialTheme.typography.titleMedium)
                      Text(
                        text = "Morning check-in & month-end alerts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Switch(
                    checked = remindersEnabled,
                    onCheckedChange = { wantEnable ->
                      if (wantEnable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val ok =
                          ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                        if (ok) {
                          onToggleReminders(true)
                          ReminderScheduler.scheduleDailyAndMonthEndReminders(context, true)
                        } else {
                          notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                      } else {
                        onToggleReminders(wantEnable)
                        ReminderScheduler.scheduleDailyAndMonthEndReminders(context, wantEnable)
                      }
                    },
                  )
                }

                HorizontalDivider(
                  color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                  thickness = 0.75.dp,
                )

                Row(
                  modifier =
                    Modifier
                      .fillMaxWidth()
                      .minimumInteractiveComponentSize()
                      .clickable(onClick = onSwitchWorkspace)
                      .padding(horizontal = 16.dp, vertical = 14.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                  ) {
                    Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                      Text("Switch / Create Local Office", style = MaterialTheme.typography.titleMedium)
                      Text(
                        text = "Manage another local workspace on this device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }
                  Icon(
                    Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                  )
                }

                if (onOpenSubscription != null) {
                  HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    thickness = 0.75.dp,
                  )
                  Row(
                    modifier =
                      Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onOpenSubscription)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("settings_subscription_button"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                      Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = Marigold)
                      Column {
                        Text("Subscription & Plans", style = MaterialTheme.typography.titleMedium)
                        Text(
                          text = "Manage Free, Starter & Business workspace tiers",
                          style = MaterialTheme.typography.bodyMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                    }
                    Icon(
                      Icons.AutoMirrored.Rounded.ArrowForwardIos,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(14.dp),
                    )
                  }
                }
              }
            }
          }
        }

        item {
          AppearanceAndRegionalSettingsSection(
            themeMode = themeMode,
            localeConfig = globalLocaleConfig,
            onSetThemeMode = onSetThemeMode,
            onUpdateLocaleConfig = onUpdateGlobalLocaleConfig,
          )
        }

        if (isOwner) {
          item {
            OwnerSettingsCard(
              office = office,
              onSave = onUpdateOfficeSettings,
              onExportBackupJson = {
                val backup =
                  JSONObject().apply {
                    put("officeName", office.name)
                    put("inviteCode", office.inviteCode)
                    put("attendanceCount", attendance.size)
                    put("leavesCount", leaves.size)
                    put("documentsCount", documents.size)
                    put(
                      "documents",
                      JSONArray(
                        documents.map { d ->
                          JSONObject().apply {
                            put("serialNo", d.serialNo)
                            put("direction", d.direction)
                            put("partyName", d.partyName)
                            put("subject", d.subject)
                            put("status", d.status)
                          }
                        }
                      ),
                    )
                  }
                val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                val file = File(dir, "OfficeZen_Backup_${OfficeViewModel.currentDateKey()}.json")
                file.writeText(backup.toString(2))
                PdfAndCsvExporter.shareFile(
                  context = context,
                  file = file,
                  mimeType = "application/json",
                  subject = "${office.name} Local Backup JSON",
                  bodyText = "OfficeZen local backup snapshot attached.",
                )
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun AppearanceAndRegionalSettingsSection(
  themeMode: ThemeMode,
  localeConfig: GlobalLocaleConfig,
  onSetThemeMode: (ThemeMode) -> Unit,
  onUpdateLocaleConfig: (GlobalLocaleConfig) -> Unit,
) {
  var showCurrencyDialog by remember { mutableStateOf(false) }
  var showDateFormatDialog by remember { mutableStateOf(false) }
  var showTimeZoneDialog by remember { mutableStateOf(false) }

  if (showCurrencyDialog) {
    AlertDialog(
      onDismissRequest = { showCurrencyDialog = false },
      shape = RoundedCornerShape(24.dp),
      title = { Text("Select Currency", style = MaterialTheme.typography.titleLarge) },
      text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items(GlobalCurrencies.presets, key = { it.code }) { spec ->
            val selected = spec.code.equals(localeConfig.currencyCode, ignoreCase = true)
            Surface(
              onClick = {
                onUpdateLocaleConfig(
                  localeConfig.copy(
                    currencyCode = spec.code,
                    currencySymbol = spec.symbol,
                    currencyDecimals = spec.decimalDigits,
                  )
                )
                showCurrencyDialog = false
              },
              color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "${spec.symbol} · ${spec.code} (${spec.displayName})",
                  style = MaterialTheme.typography.bodyLarge,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
                if (selected) {
                  Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCurrencyDialog = false }) { Text("Close") }
      },
    )
  }

  if (showDateFormatDialog) {
    AlertDialog(
      onDismissRequest = { showDateFormatDialog = false },
      shape = RoundedCornerShape(24.dp),
      title = { Text("Date Format", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          DateFormatPattern.entries.forEach { pattern ->
            val selected = pattern == localeConfig.dateFormatPattern
            Surface(
              onClick = {
                onUpdateLocaleConfig(localeConfig.copy(dateFormatPattern = pattern))
                showDateFormatDialog = false
              },
              color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(pattern.sampleLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                  Text(pattern.pattern, style = MaterialTheme.typography.bodySmall, fontFamily = MonoFontFamily)
                }
                if (selected) {
                  Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showDateFormatDialog = false }) { Text("Close") }
      },
    )
  }

  if (showTimeZoneDialog) {
    AlertDialog(
      onDismissRequest = { showTimeZoneDialog = false },
      shape = RoundedCornerShape(24.dp),
      title = { Text("Workspace Time Zone", style = MaterialTheme.typography.titleLarge) },
      text = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items(GlobalLocaleConfig.commonTimeZones, key = { it }) { tzId ->
            val selected = tzId == localeConfig.timeZoneId
            Surface(
              onClick = {
                onUpdateLocaleConfig(localeConfig.copy(timeZoneId = tzId))
                showTimeZoneDialog = false
              },
              color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = tzId,
                  style = MaterialTheme.typography.bodyLarge,
                  fontFamily = MonoFontFamily,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
                if (selected) {
                  Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                }
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showTimeZoneDialog = false }) { Text("Close") }
      },
    )
  }

  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    SectionHeader(
      title = "Appearance & Global Regional",
      subtitle = "Theme, currency, calendar & timezone formatting",
    )
    ZenCard(
      cornerRadius = 18.dp,
      modifier = Modifier.fillMaxWidth().testTag("appearance_regional_card"),
    ) {
      Column {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Atelier Theme Mode", style = MaterialTheme.typography.titleMedium)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            ThemeMode.entries.forEach { mode ->
              val label =
                when (mode) {
                  ThemeMode.SYSTEM -> "System"
                  ThemeMode.LIGHT -> "Alabaster"
                  ThemeMode.DARK -> "Obsidian"
                }
              FilterChip(
                selected = themeMode == mode,
                onClick = { onSetThemeMode(mode) },
                label = { Text(label) },
                modifier = Modifier.weight(1f).testTag("theme_mode_${mode.wire}"),
              )
            }
          }
        }

        HorizontalDivider(
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          thickness = 0.75.dp,
        )

        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { showCurrencyDialog = true }
              .padding(horizontal = 16.dp, vertical = 14.dp)
              .testTag("regional_currency_button"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(Icons.Rounded.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
              Text("Display Currency", style = MaterialTheme.typography.titleMedium)
              Text(
                text = "${localeConfig.currencySymbol} (${localeConfig.currencyCode}) · Sample ${GlobalFormatter.formatCurrency(12500.0, localeConfig)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Icon(
            Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
          )
        }

        HorizontalDivider(
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          thickness = 0.75.dp,
        )

        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { showDateFormatDialog = true }
              .padding(horizontal = 16.dp, vertical = 14.dp)
              .testTag("regional_date_format_button"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
              Text("Date Format", style = MaterialTheme.typography.titleMedium)
              Text(
                text = localeConfig.dateFormatPattern.sampleLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = MonoFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Icon(
            Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
          )
        }

        HorizontalDivider(
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          thickness = 0.75.dp,
        )

        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
              Text("24-Hour Clock", style = MaterialTheme.typography.titleMedium)
              Text(
                text = if (localeConfig.use24HourClock) "Using 14:30 format" else "Using 02:30 PM format",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Switch(
            checked = localeConfig.use24HourClock,
            onCheckedChange = { use24 ->
              onUpdateLocaleConfig(localeConfig.copy(use24HourClock = use24))
            },
            modifier = Modifier.testTag("regional_24h_switch"),
          )
        }

        HorizontalDivider(
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          thickness = 0.75.dp,
        )

        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text("Week Starts On", style = MaterialTheme.typography.titleMedium)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = localeConfig.firstDayOfWeek == Calendar.MONDAY,
              onClick = { onUpdateLocaleConfig(localeConfig.copy(firstDayOfWeek = Calendar.MONDAY)) },
              label = { Text("Monday") },
              modifier = Modifier.testTag("regional_week_monday"),
            )
            FilterChip(
              selected = localeConfig.firstDayOfWeek == Calendar.SUNDAY,
              onClick = { onUpdateLocaleConfig(localeConfig.copy(firstDayOfWeek = Calendar.SUNDAY)) },
              label = { Text("Sunday") },
              modifier = Modifier.testTag("regional_week_sunday"),
            )
          }
        }

        HorizontalDivider(
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
          thickness = 0.75.dp,
        )

        Row(
          modifier =
            Modifier
              .fillMaxWidth()
              .minimumInteractiveComponentSize()
              .clickable { showTimeZoneDialog = true }
              .padding(horizontal = 16.dp, vertical = 14.dp)
              .testTag("regional_timezone_button"),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
              Text("Workspace Time Zone", style = MaterialTheme.typography.titleMedium)
              Text(
                text = localeConfig.timeZoneId,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = MonoFontFamily,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Icon(
            Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun OwnerSettingsCard(
  office: Office,
  onSave: (Office) -> Unit,
  onExportBackupJson: () -> Unit,
) {
  var name by remember(office) { mutableStateOf(office.name) }
  var radius by remember(office) { mutableStateOf(office.geofenceRadiusMeters.toString()) }
  var strictBlock by remember(office) { mutableStateOf(office.strictGeofenceBlock) }
  var grace by remember(office) { mutableStateOf(office.lateGraceMinutes.toString()) }
  var latePerHalf by remember(office) { mutableStateOf(office.lateMarksPerHalfDay.toString()) }
  var workingDays by remember(office) { mutableStateOf(office.workingDaysPerMonth.toString()) }
  var reportEmail by remember(office) { mutableStateOf(office.ownerReportEmail) }
  var whatsappPhone by remember(office) { mutableStateOf(office.ownerWhatsappPhone) }
  var teaPrice by remember(office) { mutableStateOf(office.teaDefaultPriceInr.toInt().toString()) }
  var coffeePrice by remember(office) { mutableStateOf(office.coffeeDefaultPriceInr.toInt().toString()) }

  ZenCard(
    cornerRadius = 20.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.size(34.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              Icons.Rounded.Settings,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
          }
        }
        Text("Owner Office Rules & Backup", style = MaterialTheme.typography.titleLarge)
      }
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Office Name") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = radius,
          onValueChange = { radius = it },
          label = { Text("Geofence (m)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = grace,
          onValueChange = { grace = it },
          label = { Text("Late Grace (min)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = latePerHalf,
          onValueChange = { latePerHalf = it },
          label = { Text("Late Marks = ½ Day") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = workingDays,
          onValueChange = { workingDays = it },
          label = { Text("Work Days / Mo") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = teaPrice,
          onValueChange = { teaPrice = it },
          label = { Text("Tea ₹/cup") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = coffeePrice,
          onValueChange = { coffeePrice = it },
          label = { Text("Coffee ₹/cup") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f),
        )
      }
      OutlinedTextField(
        value = reportEmail,
        onValueChange = { reportEmail = it },
        label = { Text("Owner Report Email") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = whatsappPhone,
        onValueChange = { whatsappPhone = it },
        label = { Text("Owner WhatsApp Number") },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Strictly block out-of-geofence check-ins",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
          )
          Switch(checked = strictBlock, onCheckedChange = { strictBlock = it })
        }
      }

      Button(
        onClick = {
          onSave(
            office.copy(
              name = name.ifBlank { office.name },
              geofenceRadiusMeters = (radius.toLongOrNull() ?: office.geofenceRadiusMeters).coerceIn(20L, 5000L),
              strictGeofenceBlock = strictBlock,
              lateGraceMinutes = (grace.toLongOrNull() ?: office.lateGraceMinutes).coerceIn(0L, 120L),
              lateMarksPerHalfDay = (latePerHalf.toLongOrNull() ?: office.lateMarksPerHalfDay).coerceIn(1L, 10L),
              workingDaysPerMonth = (workingDays.toLongOrNull() ?: office.workingDaysPerMonth).coerceIn(1L, 31L),
              ownerReportEmail = reportEmail,
              ownerWhatsappPhone = whatsappPhone,
              teaDefaultPriceInr = teaPrice.toDoubleOrNull() ?: office.teaDefaultPriceInr,
              coffeeDefaultPriceInr = coffeePrice.toDoubleOrNull() ?: office.coffeeDefaultPriceInr,
            )
          )
        },
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("save_office_settings_button"),
      ) {
        Text("Save Office Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = onExportBackupJson,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("export_json_backup_button"),
      ) {
        Icon(Icons.Rounded.Backup, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Export Local Backup (JSON)")
      }
    }
  }
}

@Composable
private fun ApplyLeaveDialog(
  onSubmit: (leaveType: String, start: String, end: String, days: Double, reason: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val today = remember { OfficeViewModel.currentDateKey() }
  var leaveType by remember { mutableStateOf("casual") }
  var startDate by remember { mutableStateOf(today) }
  var endDate by remember { mutableStateOf(today) }
  var daysText by remember { mutableStateOf("1") }
  var reason by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    title = { Text("Apply Leave", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("casual", "sick", "earned").forEach { t ->
            FilterChip(
              selected = leaveType == t,
              onClick = { leaveType = t },
              label = { Text(t.replaceFirstChar { it.uppercase() }) },
            )
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("half_day" to "Half Day", "unpaid" to "Unpaid").forEach { (k, lbl) ->
            FilterChip(selected = leaveType == k, onClick = { leaveType = k }, label = { Text(lbl) })
          }
        }
        OutlinedTextField(
          value = startDate,
          onValueChange = { startDate = it },
          label = { Text("Start Date (YYYY-MM-DD)") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = endDate,
          onValueChange = { endDate = it },
          label = { Text("End Date (YYYY-MM-DD)") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = daysText,
          onValueChange = { daysText = it },
          label = { Text("Days Count") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = reason,
          onValueChange = { reason = it },
          label = { Text("Reason") },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        shape = RoundedCornerShape(14.dp),
        onClick = {
          onSubmit(leaveType, startDate, endDate, (daysText.toDoubleOrNull() ?: 1.0).coerceIn(0.5, 60.0), reason)
        },
        enabled = reason.isNotBlank() && startDate.isNotBlank(),
      ) {
        Text("Submit")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun ReportsHubSection(
  office: Office,
  summary: MonthEndSummary?,
  attendanceCount: Int,
  documentsCount: Int,
  leavesCount: Int,
  onExportJsonBackup: () -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val localeConfig = LocalGlobalLocaleConfig.current

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      shadowElevation = 3.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientMid, HeroGradientEnd)))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = strings.monthEndReport,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = Color.White,
            )
            Text(
              text = if (summary != null) "Cycle ${summary.monthKey}" else office.name,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.85f),
            )
          }
          Icon(
            imageVector = Icons.Rounded.Assessment,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
          )
        }

        if (summary != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              color = Color.White.copy(alpha = 0.16f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = GlobalFormatter.formatCurrency(summary.totalSalaryPayableInr, localeConfig),
                  style = MaterialTheme.typography.titleMedium,
                  fontFamily = MonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text = strings.netPay,
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.85f),
                )
              }
            }
            Surface(
              color = Color.White.copy(alpha = 0.16f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "${summary.totalPresentDays}d",
                  style = MaterialTheme.typography.titleMedium,
                  fontFamily = MonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text = strings.present,
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.85f),
                )
              }
            }
            Surface(
              color = Color.White.copy(alpha = 0.16f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f),
            ) {
              Column(modifier = Modifier.padding(10.dp)) {
                Text(
                  text = "${summary.totalTeaCups + summary.totalCoffeeCups}",
                  style = MaterialTheme.typography.titleMedium,
                  fontFamily = MonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text = "Pantry Cups",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.85f),
                )
              }
            }
          }
        }
      }
    }

    ZenCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = "Export & Share Reports",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "$attendanceCount attendance · $documentsCount documents · $leavesCount leaves stored on device",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (summary != null) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = {
                val file = PdfAndCsvExporter.generateMonthEndPdf(context, office, summary)
                PdfAndCsvExporter.shareFile(
                  context = context,
                  file = file,
                  mimeType = "application/pdf",
                  subject = "${office.name} Month-End PDF (${summary.monthKey})",
                  bodyText = "Month-end PDF report attached.",
                )
              },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).height(48.dp).testTag("reports_pdf_export_button"),
            ) {
              Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text("Export PDF")
            }
            OutlinedButton(
              onClick = {
                val file = PdfAndCsvExporter.generateMonthEndCsv(context, office, summary)
                PdfAndCsvExporter.shareFile(
                  context = context,
                  file = file,
                  mimeType = "text/csv",
                  subject = "${office.name} Excel CSV (${summary.monthKey})",
                  bodyText = "Month-end Excel CSV report attached.",
                )
              },
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.weight(1f).height(48.dp).testTag("reports_csv_export_button"),
            ) {
              Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.exportCsv)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedButton(
              onClick = {
                val file = PdfAndCsvExporter.generateMonthEndPdf(context, office, summary)
                val body = PdfAndCsvExporter.buildMonthEndSummaryText(office, summary)
                PdfAndCsvExporter.shareFile(
                  context = context,
                  file = file,
                  mimeType = "application/pdf",
                  subject = "${office.name} Month-End Report (${summary.monthKey})",
                  bodyText = body,
                  targetPackage = "com.whatsapp",
                )
              },
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.shareWhatsapp)
            }
            OutlinedButton(
              onClick = {
                val file = PdfAndCsvExporter.generateMonthEndPdf(context, office, summary)
                val body = PdfAndCsvExporter.buildMonthEndSummaryText(office, summary)
                PdfAndCsvExporter.shareFile(
                  context = context,
                  file = file,
                  mimeType = "application/pdf",
                  subject = "${office.name} Month-End Report (${summary.monthKey})",
                  bodyText = body,
                  emailRecipient = office.ownerReportEmail,
                )
              },
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier.weight(1f).height(48.dp),
            ) {
              Icon(Icons.Rounded.Email, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(6.dp))
              Text(strings.shareEmail)
            }
          }
        }

        OutlinedButton(
          onClick = onExportJsonBackup,
          shape = RoundedCornerShape(12.dp),
          border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Export Local Backup (JSON)")
        }
      }
    }
  }
}

