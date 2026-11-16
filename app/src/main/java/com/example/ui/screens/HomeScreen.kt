package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AttendanceRecord
import com.example.data.model.LeaveRequest
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.domain.GeofenceValidator
import com.example.hardware.LocationAndImageHelper
import com.example.localization.LocalAppStrings
import com.example.ui.components.CameraCaptureDialog
import com.example.ui.components.EmptyPlaceholderCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.SaffronAmber
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import com.example.ui.viewmodel.OfficeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
  office: Office,
  me: Member?,
  attendance: List<AttendanceRecord>,
  leaves: List<LeaveRequest>,
  isBusy: Boolean,
  onCheckIn: (lat: Double, lng: Double, distM: Double, outOfRange: Boolean, selfieBase64: String, note: String) -> Unit,
  onCheckOut: (todayRecord: AttendanceRecord, note: String) -> Unit,
  onOneTapTea: (String) -> Unit,
  onOpenApplyLeave: () -> Unit,
  onOpenDocumentsTab: () -> Unit,
  onOpenApprovalsTab: () -> Unit,
  onShowError: (String) -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val todayKey = remember { OfficeViewModel.currentDateKey() }

  val myTodayRecord =
    remember(attendance, me, todayKey) {
      attendance.firstOrNull { it.userId == me?.userId && it.dateKey == todayKey }
    }
  val isManagerOrOwner =
    me?.roleEnum == MemberRole.OWNER || me?.roleEnum == MemberRole.MANAGER
  val pendingLeavesCount = remember(leaves) { leaves.count { it.status == "pending" } }
  val outOfRangeTodayCount =
    remember(attendance, todayKey) {
      attendance.count { it.dateKey == todayKey && it.isOutOfRange }
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
      val selfieB64 = LocationAndImageHelper.bitmapToBase64(bitmap, maxDimension = 320, quality = 65)
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
      modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Hero Attendance Card
      item {
        Card(
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column {
            Box(
              modifier =
                Modifier.fillMaxWidth()
                  .background(
                    Brush.horizontalGradient(
                      listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary,
                      )
                    )
                  )
                  .padding(20.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Text(
                    text = "Namaste, ${me?.displayName ?: "Team"}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                  )
                  Text(
                    text = "$todayKey · Invite: ${office.inviteCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                  )
                }
                val selfieBmp =
                  remember(myTodayRecord?.selfieBase64) {
                    LocationAndImageHelper.decodeBase64(myTodayRecord?.selfieBase64 ?: "")
                  }
                if (selfieBmp != null) {
                  Image(
                    bitmap = selfieBmp.asImageBitmap(),
                    contentDescription = "Today's check-in selfie",
                    modifier = Modifier.size(56.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                  )
                }
              }
            }

            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Icon(
                  Icons.Rounded.GpsFixed,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = "Geofence: ${office.geofenceRadiusMeters}m · Anti-Mock GPS Active",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }

              if (myTodayRecord == null) {
                Button(
                  onClick = ::startAttendanceFlow,
                  enabled = !isBusy && !isLocating,
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(64.dp)
                      .testTag("attendance_action_button"),
                  shape = RoundedCornerShape(18.dp),
                ) {
                  if (isLocating || isBusy) {
                    CircularProgressIndicator(
                      modifier = Modifier.size(24.dp),
                      color = MaterialTheme.colorScheme.onPrimary,
                      strokeWidth = 2.5.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(if (isLocating) "Verifying GPS..." else "Saving...")
                  } else {
                    Icon(Icons.Rounded.CameraFront, contentDescription = null, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(strings.takeSelfieCheckIn, style = MaterialTheme.typography.titleMedium)
                  }
                }
              } else if (myTodayRecord.checkOutEpochMs == 0L) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  StatusPill(
                    text = "${strings.checkedIn}: ${formatTime(myTodayRecord.checkInEpochMs)}",
                    containerColor = StatusGreenBg,
                    contentColor = StatusGreen,
                  )
                  if (myTodayRecord.isOutOfRange) {
                    StatusPill(
                      text = "${strings.outOfRange} (${myTodayRecord.distanceMeters.toInt()}m)",
                      containerColor = StatusAmberBg,
                      contentColor = StatusAmber,
                    )
                  }
                }
                Button(
                  onClick = { onCheckOut(myTodayRecord, myTodayRecord.note) },
                  enabled = !isBusy,
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = SaffronAmber,
                      contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                  modifier =
                    Modifier.fillMaxWidth()
                      .height(60.dp)
                      .testTag("attendance_checkout_button"),
                  shape = RoundedCornerShape(18.dp),
                ) {
                  Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text(strings.checkOut, style = MaterialTheme.typography.titleMedium)
                }
              } else {
                Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = StatusGreen)
                  Text(
                    text =
                      "In ${formatTime(myTodayRecord.checkInEpochMs)} · Out ${formatTime(myTodayRecord.checkOutEpochMs)} · OT ${myTodayRecord.overtimeMinutes}m",
                    style = MaterialTheme.typography.titleMedium,
                    color = StatusGreen,
                  )
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
                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = StatusRed)
                    Text(
                      text = lastWarning!!,
                      style = MaterialTheme.typography.bodyMedium,
                      color = StatusRed,
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Large 1-Tap Quick Actions
      item {
        SectionHeader(title = "Quick 1-Tap Actions")
      }
      item {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledTonalButton(
              onClick = { onOneTapTea("tea") },
              modifier = Modifier.weight(1f).height(64.dp).testTag("home_quick_tea_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Rounded.Coffee, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(strings.logTea, style = MaterialTheme.typography.titleMedium)
            }
            FilledTonalButton(
              onClick = { onOneTapTea("coffee") },
              modifier = Modifier.weight(1f).height(64.dp).testTag("home_quick_coffee_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Rounded.LocalCafe, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(strings.logCoffee, style = MaterialTheme.typography.titleMedium)
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledTonalButton(
              onClick = onOpenApplyLeave,
              modifier = Modifier.weight(1f).height(58.dp).testTag("home_quick_leave_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.AutoMirrored.Rounded.EventNote, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(strings.leaveRequest, maxLines = 1)
            }
            FilledTonalButton(
              onClick = onOpenDocumentsTab,
              modifier = Modifier.weight(1f).height(58.dp).testTag("home_quick_docs_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Rounded.Description, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(strings.navChaiDocs, maxLines = 1)
            }
          }
        }
      }

      // Manager / Owner Attention Banner
      if (isManagerOrOwner && (pendingLeavesCount > 0 || outOfRangeTodayCount > 0)) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = SaffronContainer),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenApprovalsTab,
          ) {
            Row(
              modifier = Modifier.padding(16.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = StatusAmber)
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "Attention Needed",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = StatusAmber,
                )
                Text(
                  text = "$pendingLeavesCount pending leave(s) · $outOfRangeTodayCount out-of-range check-in(s) today",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
              }
            }
          }
        }
      }

      // Attendance Log
      item {
        SectionHeader(
          title = if (isManagerOrOwner) "Team Attendance Feed" else "My Recent Attendance",
          subtitle = "Selfie, GPS distance & shift status",
        )
      }

      if (attendance.isEmpty()) {
        item {
          EmptyPlaceholderCard(
            title = "No attendance records yet",
            body = "Tap '${strings.takeSelfieCheckIn}' above to record today's attendance with GPS and selfie verification.",
          )
        }
      } else {
        items(attendance.take(20), key = { it.id }) { rec ->
          AttendanceRowCard(rec, strings)
        }
      }
    }
  }
}

@Composable
private fun AttendanceRowCard(
  rec: AttendanceRecord,
  strings: com.example.localization.AppStrings,
) {
  val bmp = remember(rec.selfieBase64) { LocationAndImageHelper.decodeBase64(rec.selfieBase64) }
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(18.dp),
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
          modifier = Modifier.size(48.dp).clip(CircleShape),
          contentScale = ContentScale.Crop,
        )
      } else {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = CircleShape,
          modifier = Modifier.size(48.dp),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = rec.userName.take(1).uppercase(),
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(rec.userName, style = MaterialTheme.typography.titleMedium)
        Text(
          text =
            "${rec.dateKey} · In ${formatTime(rec.checkInEpochMs)}" +
              if (rec.checkOutEpochMs > 0L) " · Out ${formatTime(rec.checkOutEpochMs)}" else "",
          style = MaterialTheme.typography.bodyMedium,
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
            StatusPill("${strings.outOfRange} ${rec.distanceMeters.toInt()}m", StatusAmberBg, StatusAmber)
          } else {
            StatusPill("${rec.distanceMeters.toInt()}m", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
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
