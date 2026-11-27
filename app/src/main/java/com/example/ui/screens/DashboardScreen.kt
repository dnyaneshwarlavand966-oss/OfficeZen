// OfficeZen Material 3 Dashboard Screen Component (Blue & White corporate layout)
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
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceSyncStatus
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.data.model.OfficeDocument
import com.example.data.model.TeaLog
import com.example.domain.GeofenceValidator
import com.example.domain.MonthEndSummary
import com.example.hardware.LocationAndImageHelper
import com.example.localization.AppStrings
import com.example.localization.GlobalFormatter
import com.example.localization.GlobalLocaleConfig
import com.example.localization.IndianLanguages
import com.example.localization.LocalAppStrings
import com.example.localization.LocalGlobalLocaleConfig
import com.example.ui.components.AttendanceMetricType
import com.example.ui.components.AttendanceSummaryCard
import com.example.ui.components.CameraCaptureDialog
import com.example.ui.components.CloudAttendanceSyncCard
import com.example.ui.components.DashboardFeatureGrid
import com.example.ui.components.DashboardFeatureItem
import com.example.ui.components.EmptyPlaceholderCard
import com.example.ui.components.HeaderUserProfileSection
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.ZenBrandBadge
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
import com.example.ui.theme.Marigold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.OfficeZenTheme
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
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
  office: Office,
  me: Member?,
  members: List<Member>,
  attendance: List<AttendanceRecord>,
  leaves: List<LeaveRequest>,
  teaLogs: List<TeaLog>,
  documents: List<OfficeDocument>,
  monthEndSummary: MonthEndSummary?,
  isBusy: Boolean,
  selectedLanguageCode: String = "en",
  showEmbeddedHeader: Boolean = true,
  profilePhotoBitmap: Bitmap? = null,
  preferInitialsAvatar: Boolean = false,
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
  onProfileClick: (() -> Unit)? = null,
  onOpenLanguagePicker: () -> Unit = {},
  onSwitchWorkspace: (() -> Unit)? = null,
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
  onOpenSubscription: (() -> Unit)? = null,
) {
  val strings = LocalAppStrings.current
  val localeConfig = LocalGlobalLocaleConfig.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val todayKey = OfficeViewModel.currentDateKey()
  val humanDateLabel = remember(localeConfig) {
    GlobalFormatter.formatDate(System.currentTimeMillis(), localeConfig)
  }

  val myTodayRecord =
    remember(attendance, me, todayKey) {
      attendance.firstOrNull { it.userId == me?.userId && it.dateKey == todayKey }
    }
  val myTodaySelfie =
    remember(myTodayRecord?.id, myTodayRecord?.selfieBase64) {
      val rec = myTodayRecord ?: return@remember null
      LocationAndImageHelper.loadAttendanceSelfie(
        context = context,
        recordId = rec.id,
        fallbackBase64 = rec.selfieBase64,
      )
    }
  val isManagerOrOwner =
    me?.roleEnum == MemberRole.OWNER || me?.roleEnum == MemberRole.MANAGER

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
  val outOfRangeTodayCount = remember(todayAttendanceRecords) { todayAttendanceRecords.count { it.isOutOfRange } }
  val todayPantryCups = remember(teaLogs, todayKey) { teaLogs.filter { it.dateKey == todayKey }.sumOf { it.quantity } }

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

  val featureItems =
    remember(
      strings,
      presentTodayCount,
      totalStaffCount,
      documents.size,
      todayPantryCups,
      monthEndSummary?.totalSalaryPayableInr,
      pendingLeavesCount,
    ) {
      listOf(
        DashboardFeatureItem(
          title = strings.modAttendance,
          subtitle = "$presentTodayCount/$totalStaffCount present today",
          icon = Icons.Rounded.VerifiedUser,
          iconTint = EmeraldDeep,
          iconContainerColor = EmeraldSoftBg,
          testTag = "module_attendance_card",
          badgeText = if (totalStaffCount > 0) "$presentTodayCount/$totalStaffCount" else null,
          onClick = ::startAttendanceFlow,
        ),
        DashboardFeatureItem(
          title = strings.modDocuments,
          subtitle = "${documents.size} inward / outward",
          icon = Icons.Rounded.Description,
          iconTint = LedgerIndigo,
          iconContainerColor = LedgerIndigoBg,
          testTag = "home_quick_docs_button",
          badgeText = if (documents.isNotEmpty()) "${documents.size}" else null,
          onClick = onOpenDocumentsTab,
        ),
        DashboardFeatureItem(
          title = strings.modTeaCoffee,
          subtitle = "$todayPantryCups cups logged today",
          icon = Icons.Rounded.LocalCafe,
          iconTint = ChaiTerracotta,
          iconContainerColor = ChaiCream,
          testTag = "module_tea_coffee_card",
          badgeText = if (todayPantryCups > 0) "${todayPantryCups}" else null,
          onClick = onOpenTeaCoffeeTab,
        ),
        DashboardFeatureItem(
          title = strings.modPayroll,
          subtitle =
            if (monthEndSummary != null) "${GlobalFormatter.formatCurrency(monthEndSummary.totalSalaryPayableInr, localeConfig)} net cycle"
            else "3-late & overtime rules",
          icon = Icons.Rounded.AccountBalanceWallet,
          iconTint = StatusGreen,
          iconContainerColor = StatusGreenBg,
          testTag = "module_payroll_card",
          onClick = onOpenTeamPayTab,
        ),
        DashboardFeatureItem(
          title = strings.modTeam,
          subtitle = "$totalStaffCount active members",
          icon = Icons.Rounded.Groups,
          iconTint = TeamCyan,
          iconContainerColor = TeamCyanBg,
          testTag = "module_team_card",
          badgeText = if (totalStaffCount > 0) "$totalStaffCount" else null,
          onClick = onOpenTeamPayTab,
        ),
        DashboardFeatureItem(
          title = strings.modLeave,
          subtitle = "$pendingLeavesCount pending requests",
          icon = Icons.AutoMirrored.Rounded.EventNote,
          iconTint = LeaveRose,
          iconContainerColor = LeaveRoseBg,
          testTag = "home_quick_leave_button",
          badgeText = if (pendingLeavesCount > 0) "$pendingLeavesCount" else null,
          onClick = onOpenApplyLeave,
        ),
        DashboardFeatureItem(
          title = strings.modReports,
          subtitle = "PDF, CSV & WhatsApp share",
          icon = Icons.Rounded.Assessment,
          iconTint = ReportViolet,
          iconContainerColor = ReportVioletBg,
          testTag = "module_reports_card",
          onClick = onOpenReportsTab,
        ),
        DashboardFeatureItem(
          title = strings.modSettings,
          subtitle = "Shift, GPS & JSON backup",
          icon = Icons.Rounded.Settings,
          iconTint = SettingsSlate,
          iconContainerColor = SettingsSlateBg,
          testTag = "module_settings_card",
          onClick = onOpenSettingsTab,
        ),
      )
    }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
      if (showEmbeddedHeader) {
        val resolvedHeaderAvatar = if (preferInitialsAvatar) null else (profilePhotoBitmap ?: myTodaySelfie)
        DashboardHeader(
          officeName = office.name,
          userDisplayName = me?.displayName ?: "Office Owner",
          userDesignation = me?.designation ?: "",
          role = me?.roleEnum,
          userSelfieBitmap = resolvedHeaderAvatar,
          selectedLanguageCode = selectedLanguageCode,
          isNetworkOnline = isNetworkOnline,
          onOpenLanguagePicker = onOpenLanguagePicker,
          onSwitchWorkspace = onSwitchWorkspace,
          onProfileClick = onProfileClick,
        )
      }

      LazyColumn(
        modifier = Modifier.widthIn(max = 820.dp).fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // 1. Today's Attendance Summary KPIs
        item {
          AttendanceSummaryCard(
            totalStaff = totalStaffCount,
            presentCount = presentTodayCount,
            absentCount = absentTodayCount,
            lateCount = lateTodayCount,
            dateLabel = humanDateLabel,
            inviteCodeBadge = "CODE ${office.inviteCode}",
            onMetricClick = { metric ->
              when (metric) {
                AttendanceMetricType.TOTAL_STAFF -> onOpenTeamPayTab()
                AttendanceMetricType.PRESENT,
                AttendanceMetricType.ABSENT,
                AttendanceMetricType.LATE -> onOpenReportsTab()
              }
            },
          )
        }

        // 2. Selfie + GPS Shift Check-In & 1-Tap Pantry Card
        item {
          val nowMinutes = remember {
            val cal = Calendar.getInstance()
            cal.get(Calendar.HOUR_OF_DAY) * 60L + cal.get(Calendar.MINUTE)
          }
          val shiftSpan = (office.shiftEndMinutes - office.shiftStartMinutes).coerceAtLeast(60L)
          val shiftProgress =
            ((nowMinutes - office.shiftStartMinutes).toFloat() / shiftSpan.toFloat()).coerceIn(0f, 1f)

          ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
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
                      text = "${office.geofenceRadiusMeters}M GEOFENCE · SHIFT ${formatShiftMin(office.shiftStartMinutes)} - ${formatShiftMin(office.shiftEndMinutes)}",
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
                        myTodayRecord.checkOutEpochMs == 0L -> "Checked in at ${GlobalFormatter.formatTime(myTodayRecord.checkInEpochMs, localeConfig)}"
                        else -> "Shift completed for today"
                      },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }

                if (myTodaySelfie != null) {
                  Image(
                    bitmap = myTodaySelfie.asImageBitmap(),
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
                    label = "dashboardCheckInScale",
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
                    text = "${strings.checkedIn} · ${GlobalFormatter.formatTime(myTodayRecord.checkInEpochMs, localeConfig)}",
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
                        "In ${GlobalFormatter.formatTime(myTodayRecord.checkInEpochMs, localeConfig)} · Out ${GlobalFormatter.formatTime(myTodayRecord.checkOutEpochMs, localeConfig)} · OT ${myTodayRecord.overtimeMinutes}m",
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

              HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                DashboardPantryButton(
                  label = "${strings.logTea} · ${GlobalFormatter.formatCurrency(office.teaDefaultPriceInr, localeConfig)}",
                  icon = Icons.Rounded.Coffee,
                  containerColor = ChaiCream,
                  contentColor = ChaiInk,
                  onClick = { onOneTapTea("tea") },
                  modifier = Modifier.weight(1f).testTag("home_quick_tea_button"),
                )
                DashboardPantryButton(
                  label = "${strings.logCoffee} · ${GlobalFormatter.formatCurrency(office.coffeeDefaultPriceInr, localeConfig)}",
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

        // 3. Responsive Grid-Based Feature Cards
        item {
          DashboardFeatureGrid(
            items = featureItems,
            sectionTitle = strings.dashboardModules,
            sectionSubtitle = "All core office operations in one workspace",
          )
        }

        // 4. OfficeZen Premium Banner
        item {
          val bannerInteraction = remember { MutableInteractionSource() }
          val bannerPressed by bannerInteraction.collectIsPressedAsState()
          val bannerScaleState =
            animateFloatAsState(
              targetValue = if (bannerPressed) 0.97f else 1f,
              animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
              label = "premiumBannerScale",
            )
          ElevatedCard(
            onClick = onOpenSubscription ?: onOpenReportsTab,
            interactionSource = bannerInteraction,
            shape = RoundedCornerShape(20.dp),
            elevation =
              CardDefaults.elevatedCardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 1.dp,
              ),
            modifier =
              Modifier
                .fillMaxWidth()
                .graphicsLayer {
                  val s = bannerScaleState.value
                  scaleX = s
                  scaleY = s
                }
                .testTag("officezen_premium_banner"),
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
                  color = Marigold.copy(alpha = 0.22f),
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(0.75.dp, Marigold.copy(alpha = 0.55f)),
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

        // 5. Manager / Owner Attention Strip
        if (isManagerOrOwner && (pendingLeavesCount > 0 || outOfRangeTodayCount > 0)) {
          item {
            val attentionInteraction = remember { MutableInteractionSource() }
            val attentionPressed by attentionInteraction.collectIsPressedAsState()
            val attentionScaleState =
              animateFloatAsState(
                targetValue = if (attentionPressed) 0.97f else 1f,
                animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                label = "attentionCardScale",
              )
            ElevatedCard(
              onClick = onOpenApprovalsTab,
              interactionSource = attentionInteraction,
              shape = RoundedCornerShape(18.dp),
              colors = CardDefaults.elevatedCardColors(containerColor = StatusAmberBg),
              elevation =
                CardDefaults.elevatedCardElevation(
                  defaultElevation = 2.dp,
                  pressedElevation = 0.5.dp,
                ),
              modifier =
                Modifier
                  .fillMaxWidth()
                  .graphicsLayer {
                    val s = attentionScaleState.value
                    scaleX = s
                    scaleY = s
                  },
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

        // 5b. Firestore Attendance Background Sync Queue Status
        if (pendingSyncCount > 0 || syncedCount > 0 || !googleUserEmail.isNullOrBlank() || attendance.isNotEmpty()) {
          item {
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
            DashboardAttendanceRow(rec = rec, strings = strings)
          }
        }
      }
    }
  }
}

@Composable
fun DashboardHeader(
  officeName: String,
  userDisplayName: String,
  userDesignation: String,
  role: MemberRole?,
  userSelfieBitmap: Bitmap?,
  selectedLanguageCode: String,
  isNetworkOnline: Boolean = false,
  onOpenLanguagePicker: () -> Unit,
  onSwitchWorkspace: (() -> Unit)? = null,
  onProfileClick: (() -> Unit)? = null,
) {
  val strings = LocalAppStrings.current
  val currentLang = remember(selectedLanguageCode) { IndianLanguages.findByCode(selectedLanguageCode) }
  val headerGradient = Brush.linearGradient(listOf(HeroGradientStart, HeroGradientMid, HeroGradientEnd))

  Surface(
    color = Color.Transparent,
    shadowElevation = 4.dp,
    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
    modifier = Modifier.fillMaxWidth().testTag("dashboard_blue_header"),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(headerGradient)
          .statusBarsPadding()
          .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(11.dp),
        ) {
          ZenBrandBadge(size = 40.dp)
          Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
              text = strings.appName,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = Color.White,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (officeName.isNotBlank() && officeName != strings.appName) {
              Text(
                text = officeName,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            onClick = onOpenLanguagePicker,
            shape = RoundedCornerShape(50),
            color = Color.White.copy(alpha = 0.16f),
            border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.35f)),
            modifier = Modifier.minimumInteractiveComponentSize().testTag("dashboard_language_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                Icons.Rounded.Language,
                contentDescription = strings.language,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
              )
              Text(
                text = currentLang.nativeName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
              )
            }
          }
          if (onSwitchWorkspace != null) {
            Surface(
              onClick = onSwitchWorkspace,
              shape = CircleShape,
              color = Color.White.copy(alpha = 0.16f),
              border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.35f)),
              modifier = Modifier.size(44.dp).minimumInteractiveComponentSize().testTag("dashboard_switch_office_button"),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  Icons.Rounded.SwapHoriz,
                  contentDescription = strings.signOut,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp),
                )
              }
            }
          }
        }
      }

      val resolvedName = userDisplayName.ifBlank { "Office Owner" }
      val roleLabel =
        when (role) {
          MemberRole.OWNER -> strings.roleOwner
          MemberRole.MANAGER -> strings.roleManager
          MemberRole.EMPLOYEE -> strings.roleEmployee
          null -> strings.roleOwner
        }
      val subtitle = if (userDesignation.isNotBlank()) "$userDesignation · $roleLabel" else roleLabel

      HeaderUserProfileSection(
        displayName = resolvedName,
        subtitle = subtitle,
        statusText = if (isNetworkOnline) "Online" else strings.offlineReady,
        profilePhotoBitmap = userSelfieBitmap,
        onClick = onProfileClick,
      )
    }
  }
}

@Composable
private fun DashboardPantryButton(
  label: String,
  icon: ImageVector,
  containerColor: Color,
  contentColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scaleState =
    animateFloatAsState(
      targetValue = if (pressed) 0.96f else 1f,
      animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
      label = "pantryButtonScale",
    )
  Surface(
    onClick = onClick,
    interactionSource = interactionSource,
    color = containerColor,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(0.75.dp, contentColor.copy(alpha = 0.18f)),
    modifier =
      modifier
        .height(48.dp)
        .graphicsLayer {
          val s = scaleState.value
          scaleX = s
          scaleY = s
        },
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
private fun DashboardAttendanceRow(
  rec: AttendanceRecord,
  strings: AppStrings,
  localeConfig: GlobalLocaleConfig = LocalGlobalLocaleConfig.current,
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

  ElevatedCard(
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
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
            text = GlobalFormatter.formatTime(rec.checkInEpochMs, localeConfig),
            style = MaterialTheme.typography.labelLarge,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Text(
          text =
            GlobalFormatter.formatDateKey(rec.dateKey, localeConfig) +
              if (rec.checkOutEpochMs > 0L) " · Out ${GlobalFormatter.formatTime(rec.checkOutEpochMs, localeConfig)}" else "",
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
            StatusPill("${rec.distanceMeters.toInt()}m GPS", EmeraldSoftBg, EmeraldDeep, useMonospace = true)
          }
          val (syncBg, syncFg, syncLabel) =
            when (rec.syncStatus) {
              AttendanceSyncStatus.SYNCED -> Triple(StatusGreenBg, StatusGreen, "Synced")
              AttendanceSyncStatus.SYNCING -> Triple(EmeraldSoftBg, EmeraldDeep, "Syncing")
              AttendanceSyncStatus.FAILED -> Triple(StatusRedBg, StatusRed, "Retry Sync")
              else -> Triple(StatusAmberBg, StatusAmber, "Pending Sync")
            }
          StatusPill(syncLabel, syncBg, syncFg)
        }
      }
    }
  }
}

private fun formatShiftMin(totalMinutes: Long): String {
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

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
  OfficeZenTheme {
    DashboardScreen(
      office = Office(id = "preview", name = "Apex Solutions", inviteCode = "ZEN42"),
      me = Member(userId = "u1", officeId = "preview", displayName = "Dnyaneshwar", role = "owner", designation = "Director"),
      members = emptyList(),
      attendance = emptyList(),
      leaves = emptyList(),
      teaLogs = emptyList(),
      documents = emptyList(),
      monthEndSummary = null,
      isBusy = false,
      showEmbeddedHeader = true,
      onCheckIn = { _, _, _, _, _, _ -> },
      onCheckOut = { _, _ -> },
      onOneTapTea = {},
      onOpenApplyLeave = {},
      onOpenTeaCoffeeTab = {},
      onOpenDocumentsTab = {},
      onOpenTeamPayTab = {},
      onOpenReportsTab = {},
      onOpenSettingsTab = {},
      onOpenApprovalsTab = {},
      onShowError = {},
    )
  }
}
