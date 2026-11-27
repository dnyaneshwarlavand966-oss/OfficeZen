package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceRecord
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.data.model.OfficeDocument
import com.example.data.model.TeaLog
import com.example.domain.GeofenceValidator
import com.example.domain.MonthEndSummary
import com.example.hardware.LocationAndImageHelper
import com.example.localization.LocalAppStrings
import com.example.ui.components.CameraCaptureDialog
import com.example.ui.components.EmptyPlaceholderCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZenCard
import com.example.ui.theme.ChaiCream
import com.example.ui.theme.ChaiInk
import com.example.ui.theme.ChaiTerracotta
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldNight
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.EspressoBg
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientMid
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.LeaveRose
import com.example.ui.theme.LeaveRoseBg
import com.example.ui.theme.LedgerIndigo
import com.example.ui.theme.LedgerIndigoBg
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ReportViolet
import com.example.ui.theme.ReportVioletBg
import com.example.ui.theme.SettingsSlate
import com.example.ui.theme.SettingsSlateBg
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.theme.TeamCyan
import com.example.ui.theme.TeamCyanBg
import com.example.ui.viewmodel.OfficeViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  office: Office,
  me: Member?,
  members: List<Member>,
  attendance: List<AttendanceRecord>,
  leaves: List<LeaveRequest>,
  teaLogs: List<TeaLog>,
  documents: List<OfficeDocument>,
  monthEndSummary: MonthEndSummary?,
  isBusy: Boolean,
  onCheckIn: (lat: Double, lng: Double, distM: Double, outOfRange: Boolean, selfieBase64: String, note: String) -> Unit,
  onCheckOut: (todayRecord: AttendanceRecord, note: String) -> Unit,
  onOneTapTea: (String) -> Unit,
  onOpenApplyLeave: () -> Unit,
  onOpenTeaCoffeeTab: () -> Unit,
  onOpenDocumentsTab: () -> Unit,
  onOpenTeamPayTab: () -> Unit,
  onOpenReportsTab: () -> Unit,
  onOpenSettingsTab: () -> Unit,
  onOpenApprovalsTab: () -> Unit,
  onShowError: (String) -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val todayKey = OfficeViewModel.currentDateKey()
  val humanDateLabel = remember { SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(Date()) }

  val myTodayRecord =
    remember(attendance, me, todayKey) {
      attendance.firstOrNull { it.userId == me?.userId && it.dateKey == todayKey }
    }
  val isManagerOrOwner =
    me?.roleEnum == MemberRole.OWNER || me?.roleEnum == MemberRole.MANAGER

  // Real Today Attendance KPI computation from Room data
  val activeStaffList = remember(members) { members.filter { it.active }.ifEmpty { members } }
  val totalStaffCount = activeStaffList.size
  val todayAttendanceRecords = remember(attendance, todayKey) { attendance.filter { it.dateKey == todayKey } }
  val presentTodayCount = remember(todayAttendanceRecords) { todayAttendanceRecords.map { it.userId }.distinct().size }
  val lateTodayCount =
    remember(todayAttendanceRecords) {
      todayAttendanceRecords
        .filter { it.status == "late" || it.status == "half_day" }
        .map { it.userId }
        .distinct()
        .size
    }
  val absentTodayCount = (totalStaffCount - presentTodayCount).coerceAtLeast(0)

  val pendingLeavesCount = remember(leaves) { leaves.count { it.status == "pending" } }
  val outOfRangeTodayCount =
    remember(todayAttendanceRecords) {
      todayAttendanceRecords.count { it.isOutOfRange }
    }
  val todayPantryCups =
    remember(teaLogs, todayKey) {
      teaLogs.filter { it.dateKey == todayKey }.sumOf { it.quantity }
    }

  var showSelfieCamera by remember { mutableStateOf(false) }
  var isLocating by remember { mutableStateOf(false) }
  var lastWarning by remember { mutableStateOf<String?>(null) }

  val permissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
      val camOk = grants[Manifest.permission.CAMERA] == true
      val locOk =
        grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
          grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (camOk && locOk) {
        showSelfieCamera = true
      } else {
        lastWarning = "Camera and Location permissions are needed for selfie attendance."
      }
    }

  fun startAttendanceFlow() {
    lastWarning = null
    val camOk =
      ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED
    val locOk =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (camOk && locOk) {
      showSelfieCamera = true
    } else {
      permissionLauncher.launch(
        arrayOf(
          Manifest.permission.CAMERA,
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
        )
      )
    }
  }

  fun handleSelfieCaptured(bitmap: Bitmap) {
    showSelfieCamera = false
    isLocating = true
    scope.launch {
      val location = LocationAndImageHelper.fetchCurrentLocation(context)
      isLocating = false
      if (location == null) {
        val msg = "Unable to lock GPS location. Move near a window or enable GPS and try again."
        lastWarning = msg
        onShowError(msg)
        return@launch
      }
      if (GeofenceValidator.isUnsetOfficeCoordinate(office.officeLat, office.officeLng)) {
        val msg = "Office GPS location is not configured yet. Ask your Owner to update office location."
        lastWarning = msg
        onShowError(msg)
        return@launch
      }
      val check =
        GeofenceValidator.evaluateLocation(
          location = location,
          officeLat = office.officeLat,
          officeLng = office.officeLng,
          radiusMeters = office.geofenceRadiusMeters,
        )
      if (check.isMockLocation) {
        lastWarning = strings.mockGpsBlocked
        onShowError(strings.mockGpsBlocked)
        return@launch
      }
      if (!check.isWithinGeofence && office.strictGeofenceBlock) {
        val msg =
          "Outside office geofence (${check.distanceMeters.toInt()}m away; max ${office.geofenceRadiusMeters}m). Strict block enabled."
        lastWarning = msg
        onShowError(msg)
        return@launch
      }
      val uid = me?.userId.orEmpty()
      if (uid.isNotBlank()) {
        LocationAndImageHelper.saveAttendanceSelfieLocally(context, "${uid}_${todayKey}", bitmap)
      }
      val selfieB64 =
        LocationAndImageHelper.bitmapToBoundedBase64(
          bitmap = bitmap,
          maxDimension = 128,
          initialQuality = 55,
          maxBase64Chars = LocationAndImageHelper.MAX_FIRESTORE_SELFIE_B64_CHARS,
        )
      onCheckIn(
        location.latitude,
        location.longitude,
        check.distanceMeters,
        !check.isWithinGeofence,
        selfieB64,
        if (!check.isWithinGeofence) "Out of office range (${check.distanceMeters.toInt()}m)" else "",
      )
    }
  }

  if (showSelfieCamera) {
    CameraCaptureDialog(
      title = strings.takeSelfieCheckIn,
      useFrontCamera = true,
      onCaptured = ::handleSelfieCaptured,
      onDismiss = { showSelfieCamera = false },
    )
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 700.dp).fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // 1. Today's Attendance Summary Card (Total Staff, Present, Absent, Late)
      item {
        ZenCard(
          cornerRadius = 20.dp,
          elevation = 2.dp,
          modifier = Modifier.fillMaxWidth().testTag("today_attendance_summary_card"),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = strings.todayAttendanceSummary,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = humanDateLabel,
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              StatusPill(
                text = "CODE ${office.inviteCode}",
                containerColor = EmeraldSoftBg,
                contentColor = EmeraldDeep,
                useMonospace = true,
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              AttendanceStatTile(
                label = strings.totalStaff,
                value = totalStaffCount.toString(),
                containerColor = EmeraldSoftBg,
                accentColor = EmeraldDeep,
                modifier = Modifier.weight(1f).testTag("summary_total_staff"),
              )
              AttendanceStatTile(
                label = strings.present,
                value = presentTodayCount.toString(),
                containerColor = StatusGreenBg,
                accentColor = StatusGreen,
                modifier = Modifier.weight(1f).testTag("summary_present"),
              )
              AttendanceStatTile(
                label = strings.absent,
                value = absentTodayCount.toString(),
                containerColor = StatusRedBg,
                accentColor = StatusRed,
                modifier = Modifier.weight(1f).testTag("summary_absent"),
              )
              AttendanceStatTile(
                label = strings.late,
                value = lateTodayCount.toString(),
                containerColor = StatusAmberBg,
                accentColor = StatusAmber,
                modifier = Modifier.weight(1f).testTag("summary_late"),
              )
            }
          }
        }
      }

      // 2. Selfie + GPS Shift Check-In / Check-Out Action Card
      item {
        val nowMinutes = remember {
          val cal = Calendar.getInstance()
          cal.get(Calendar.HOUR_OF_DAY) * 60L + cal.get(Calendar.MINUTE)
        }
        val shiftSpan = (office.shiftEndMinutes - office.shiftStartMinutes).coerceAtLeast(60L)
        val shiftProgress =
          ((nowMinutes - office.shiftStartMinutes).toFloat() / shiftSpan.toFloat()).coerceIn(0f, 1f)

        ZenCard(
          cornerRadius = 20.dp,
          elevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                  Icon(
                    Icons.Rounded.GpsFixed,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = "${office.geofenceRadiusMeters}M GEOFENCE · SHIFT ${formatMinutes(office.shiftStartMinutes)} - ${formatMinutes(office.shiftEndMinutes)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
                Text(
                  text =
                    when {
                      myTodayRecord == null -> "Mark Today's Attendance"
                      myTodayRecord.checkOutEpochMs == 0L -> "Checked in at ${formatTime(myTodayRecord.checkInEpochMs)}"
                      else -> "Shift completed for today"
                    },
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }

              val selfieBmp =
                remember(myTodayRecord?.id, myTodayRecord?.selfieBase64) {
                  val rec = myTodayRecord ?: return@remember null
                  LocationAndImageHelper.loadAttendanceSelfie(
                    context = context,
                    recordId = rec.id,
                    fallbackBase64 = rec.selfieBase64,
                  )
                }
              if (selfieBmp != null) {
                Image(
                  bitmap = selfieBmp.asImageBitmap(),
                  contentDescription = "Today's check-in selfie",
                  modifier =
                    Modifier
                      .size(46.dp)
                      .clip(CircleShape)
                      .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                  contentScale = ContentScale.Crop,
                )
              }
            }

            // Shift progress bar
            Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
              drawRoundRect(
                color = EmeraldContainer,
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
              )
              drawRoundRect(
                color = EmeraldDeep,
                size = Size(width = size.width * shiftProgress, height = size.height),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
              )
            }

            if (myTodayRecord == null) {
              val interactionSource = remember { MutableInteractionSource() }
              val pressed by interactionSource.collectIsPressedAsState()
              val scale by
                animateFloatAsState(
                  targetValue = if (pressed) 0.98f else 1f,
                  animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                  label = "ctaScale",
                )
              Button(
                onClick = ::startAttendanceFlow,
                enabled = !isBusy && !isLocating,
                interactionSource = interactionSource,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                  ),
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .testTag("attendance_action_button"),
                shape = RoundedCornerShape(14.dp),
              ) {
                if (isLocating || isBusy) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                  )
                  Spacer(Modifier.width(10.dp))
                  Text(
                    if (isLocating) "Locking GPS..." else "Saving...",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                } else {
                  Icon(Icons.Rounded.CameraFront, contentDescription = null, modifier = Modifier.size(20.dp))
                  Spacer(Modifier.width(8.dp))
                  Text(
                    strings.takeSelfieCheckIn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            } else if (myTodayRecord.checkOutEpochMs == 0L) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                StatusPill(
                  text = "${strings.checkedIn} · ${formatTime(myTodayRecord.checkInEpochMs)}",
                  containerColor = StatusGreenBg,
                  contentColor = StatusGreen,
                  useMonospace = true,
                )
                StatusPill(
                  text =
                    if (myTodayRecord.isOutOfRange) "${strings.outOfRange} (${myTodayRecord.distanceMeters.toInt()}m)"
                    else "${myTodayRecord.distanceMeters.toInt()}m GPS",
                  containerColor = if (myTodayRecord.isOutOfRange) StatusAmberBg else EmeraldSoftBg,
                  contentColor = if (myTodayRecord.isOutOfRange) StatusAmber else EmeraldDeep,
                  useMonospace = true,
                )
              }
              Button(
                onClick = { onCheckOut(myTodayRecord, myTodayRecord.note) },
                enabled = !isBusy,
                colors =
                  ButtonDefaults.buttonColors(
                    containerColor = ChaiTerracotta,
                    contentColor = Color.White,
                  ),
                modifier =
                  Modifier.fillMaxWidth()
                    .height(50.dp)
                    .testTag("attendance_checkout_button"),
                shape = RoundedCornerShape(14.dp),
              ) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(8.dp))
                Text(strings.checkOut, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
            } else {
              Surface(
                color = StatusGreenBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(0.75.dp, StatusGreen.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = StatusGreen)
                  Text(
                    text =
                      "In ${formatTime(myTodayRecord.checkInEpochMs)} · Out ${formatTime(myTodayRecord.checkOutEpochMs)} · OT ${myTodayRecord.overtimeMinutes}m",
                    style = MaterialTheme.typography.labelLarge,
                    fontFamily = MonoFontFamily,
                    color = StatusGreen,
                  )
                }
              }
            }

            if (lastWarning != null) {
              Surface(
                color = StatusRedBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(Icons.Rounded.Warning, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                  Text(
                    text = lastWarning!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusRed,
                  )
                }
              }
            }

            // Instant 1-Tap Tea & Coffee Bar
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              QuickPantryButton(
                label = "${strings.logTea} · ₹${office.teaDefaultPriceInr.toInt()}",
                icon = Icons.Rounded.Coffee,
                containerColor = ChaiCream,
                contentColor = ChaiInk,
                onClick = { onOneTapTea("tea") },
                modifier = Modifier.weight(1f).testTag("home_quick_tea_button"),
              )
              QuickPantryButton(
                label = "${strings.logCoffee} · ₹${office.coffeeDefaultPriceInr.toInt()}",
                icon = Icons.Rounded.LocalCafe,
                containerColor = EspressoBg,
                contentColor = EspressoBrown,
                onClick = { onOneTapTea("coffee") },
                modifier = Modifier.weight(1f).testTag("home_quick_coffee_button"),
              )
            }
          }
        }
      }

      // 3. Two-Column Feature Card Layout (All 8 Dashboard Modules)
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          SectionHeader(
            title = strings.dashboardModules,
            subtitle = "All core office operations in one workspace",
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DashboardModuleCard(
              title = strings.modAttendance,
              subtitle = "$presentTodayCount/$totalStaffCount present today",
              icon = Icons.Rounded.VerifiedUser,
              iconTint = EmeraldDeep,
              iconBg = EmeraldSoftBg,
              onClick = ::startAttendanceFlow,
              modifier = Modifier.weight(1f).testTag("module_attendance_card"),
            )
            DashboardModuleCard(
              title = strings.modDocuments,
              subtitle = "${documents.size} inward / outward",
              icon = Icons.Rounded.Description,
              iconTint = LedgerIndigo,
              iconBg = LedgerIndigoBg,
              onClick = onOpenDocumentsTab,
              modifier = Modifier.weight(1f).testTag("home_quick_docs_button"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DashboardModuleCard(
              title = strings.modTeaCoffee,
              subtitle = "$todayPantryCups cups logged today",
              icon = Icons.Rounded.LocalCafe,
              iconTint = ChaiTerracotta,
              iconBg = ChaiCream,
              onClick = onOpenTeaCoffeeTab,
              modifier = Modifier.weight(1f).testTag("module_tea_coffee_card"),
            )
            DashboardModuleCard(
              title = strings.modPayroll,
              subtitle =
                if (monthEndSummary != null) "₹${monthEndSummary.totalSalaryPayableInr.roundToInt()} net cycle"
                else "3-late & overtime rules",
              icon = Icons.Rounded.AccountBalanceWallet,
              iconTint = StatusGreen,
              iconBg = StatusGreenBg,
              onClick = onOpenTeamPayTab,
              modifier = Modifier.weight(1f).testTag("module_payroll_card"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DashboardModuleCard(
              title = strings.modTeam,
              subtitle = "$totalStaffCount active members",
              icon = Icons.Rounded.Groups,
              iconTint = TeamCyan,
              iconBg = TeamCyanBg,
              onClick = onOpenTeamPayTab,
              modifier = Modifier.weight(1f).testTag("module_team_card"),
            )
            DashboardModuleCard(
              title = strings.modLeave,
              subtitle = "$pendingLeavesCount pending requests",
              icon = Icons.AutoMirrored.Rounded.EventNote,
              iconTint = LeaveRose,
              iconBg = LeaveRoseBg,
              onClick = onOpenApplyLeave,
              modifier = Modifier.weight(1f).testTag("home_quick_leave_button"),
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            DashboardModuleCard(
              title = strings.modReports,
              subtitle = "PDF, CSV & WhatsApp share",
              icon = Icons.Rounded.Assessment,
              iconTint = ReportViolet,
              iconBg = ReportVioletBg,
              onClick = onOpenReportsTab,
              modifier = Modifier.weight(1f).testTag("module_reports_card"),
            )
            DashboardModuleCard(
              title = strings.modSettings,
              subtitle = "Shift, GPS & JSON backup",
              icon = Icons.Rounded.Settings,
              iconTint = SettingsSlate,
              iconBg = SettingsSlateBg,
              onClick = onOpenSettingsTab,
              modifier = Modifier.weight(1f).testTag("module_settings_card"),
            )
          }
        }
      }

      // 4. OfficeZen Premium Banner
      item {
        Surface(
          shape = RoundedCornerShape(20.dp),
          shadowElevation = 3.dp,
          onClick = onOpenReportsTab,
          modifier = Modifier.fillMaxWidth().testTag("officezen_premium_banner"),
        ) {
          Box(
            modifier =
              Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(HeroGradientStart, HeroGradientMid, HeroGradientEnd)))
                .padding(18.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
              Surface(
                color = Color.White.copy(alpha = 0.18f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier.size(48.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Rounded.WorkspacePremium,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                  )
                }
              }
              Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  text = strings.premiumBannerTitle,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = Color.White,
                )
                Text(
                  text = strings.premiumBannerSubtitle,
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White.copy(alpha = 0.88f),
                  maxLines = 2,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
              ) {
                Text(
                  text = strings.premiumBannerCta,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = EmeraldNight,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                )
              }
            }
          }
        }
      }

      // 5. Manager / Owner Attention Strip (if pending approvals or out-of-range check-ins exist)
      if (isManagerOrOwner && (pendingLeavesCount > 0 || outOfRangeTodayCount > 0)) {
        item {
          ZenCard(
            containerColor = StatusAmberBg,
            borderColor = StatusAmber.copy(alpha = 0.35f),
            cornerRadius = 16.dp,
            elevation = 1.dp,
            onClick = onOpenApprovalsTab,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Surface(
                color = StatusAmber.copy(alpha = 0.16f),
                shape = CircleShape,
                modifier = Modifier.size(38.dp),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(20.dp))
                }
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Attention Needed",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = ChaiInk,
                )
                Text(
                  text = "$pendingLeavesCount pending leave · $outOfRangeTodayCount out-of-range today",
                  style = MaterialTheme.typography.bodyMedium,
                  color = ChaiInk.copy(alpha = 0.85f),
                )
              }
              Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = ChaiInk,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }

      // 6. Recent Attendance Records
      item {
        SectionHeader(
          title = if (isManagerOrOwner) "Recent Attendance Log" else "My Attendance",
          subtitle = "Selfie-verified GPS check-ins",
          trailingBadge = "${attendance.size} logs",
        )
      }

      if (attendance.isEmpty()) {
        item {
          EmptyPlaceholderCard(
            title = "No check-ins recorded yet",
            body = "Tap '${strings.takeSelfieCheckIn}' above to verify today's arrival.",
          )
        }
      } else {
        items(attendance.take(15), key = { it.id }) { rec ->
          AttendanceLogCard(rec = rec, strings = strings)
        }
      }
    }
  }
}

@Composable
private fun AttendanceStatTile(
  label: String,
  value: String,
  containerColor: Color,
  accentColor: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(0.75.dp, accentColor.copy(alpha = 0.18f)),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        color = accentColor,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = accentColor.copy(alpha = 0.88f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun DashboardModuleCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  iconTint: Color,
  iconBg: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ZenCard(
    cornerRadius = 18.dp,
    elevation = 2.dp,
    onClick = onClick,
    modifier = modifier.heightIn(min = 110.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        color = iconBg,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(40.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(22.dp),
          )
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Composable
private fun QuickPantryButton(
  label: String,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    color = containerColor,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(0.75.dp, contentColor.copy(alpha = 0.18f)),
    modifier = modifier.height(48.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
    ) {
      Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
private fun AttendanceLogCard(
  rec: AttendanceRecord,
  strings: com.example.localization.AppStrings,
) {
  val context = LocalContext.current
  val bmp =
    remember(rec.id, rec.selfieBase64) {
      LocationAndImageHelper.loadAttendanceSelfie(
        context = context,
        recordId = rec.id,
        fallbackBase64 = rec.selfieBase64,
      )
    }

  ZenCard(
    cornerRadius = 16.dp,
    elevation = 1.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (bmp != null) {
        Image(
          bitmap = bmp.asImageBitmap(),
          contentDescription = "${rec.userName} selfie",
          modifier =
            Modifier
              .size(46.dp)
              .clip(CircleShape)
              .border(1.dp, EmeraldOnContainer.copy(alpha = 0.25f), CircleShape),
          contentScale = ContentScale.Crop,
        )
      } else {
        Surface(
          color = EmeraldSoftBg,
          shape = CircleShape,
          modifier = Modifier.size(46.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = rec.userName.take(1).uppercase(),
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = EmeraldDeep,
            )
          }
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = rec.userName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = formatTime(rec.checkInEpochMs),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Text(
          text =
            "${rec.dateKey}" +
              if (rec.checkOutEpochMs > 0L) " · Out ${formatTime(rec.checkOutEpochMs)}" else "",
          style = MaterialTheme.typography.bodyMedium,
          fontFamily = MonoFontFamily,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          val (bg, fg, label) =
            when (rec.status) {
              "late" -> Triple(StatusAmberBg, StatusAmber, "${strings.late} (${rec.lateMinutes}m)")
              "half_day" -> Triple(StatusRedBg, StatusRed, strings.halfDay)
              else -> Triple(StatusGreenBg, StatusGreen, strings.onTime)
            }
          StatusPill(label, bg, fg)
          if (rec.isOutOfRange) {
            StatusPill("${strings.outOfRange} ${rec.distanceMeters.toInt()}m", StatusAmberBg, StatusAmber, useMonospace = true)
          } else {
            StatusPill(
              "${rec.distanceMeters.toInt()}m GPS",
              EmeraldSoftBg,
              EmeraldDeep,
              useMonospace = true,
            )
          }
        }
      }
    }
  }
}

private fun formatTime(epochMs: Long): String {
  if (epochMs <= 0L) return "--:--"
  return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(epochMs))
}

private fun formatMinutes(totalMinutes: Long): String {
  val clamped = totalMinutes.coerceIn(0L, 1439L)
  val h = (clamped / 60L).toInt()
  val m = (clamped % 60L).toInt()
  val amPm = if (h >= 12) "PM" else "AM"
  val h12 = when {
    h == 0 -> 12
    h > 12 -> h - 12
    else -> h
  }
  return String.format(Locale.US, "%d:%02d %s", h12, m, amPm)
}
