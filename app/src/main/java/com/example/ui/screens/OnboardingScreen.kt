// OfficeZen workspace create/join onboarding screen
package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.hardware.LocationAndImageHelper
import com.example.localization.LocalAppStrings
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
  isBusy: Boolean,
  errorMessage: String? = null,
  onClearError: () -> Unit = {},
  onCreateOffice: (
    officeName: String,
    ownerPhone: String,
    lat: Double,
    lng: Double,
    radiusM: Long,
    shiftStartMin: Long,
    shiftEndMin: Long,
    graceMin: Long,
    workingDays: Long,
  ) -> Unit,
  onJoinOffice: (inviteCode: String, designation: String, phone: String) -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var mode by remember { mutableIntStateOf(0) } // 0 = Create Office (Owner), 1 = Join Office (Staff)

  // Join fields
  var inviteCode by remember { mutableStateOf("") }
  var designation by remember { mutableStateOf("Executive") }
  var staffPhone by remember { mutableStateOf("") }

  // Create fields
  var officeName by remember { mutableStateOf("") }
  var ownerPhone by remember { mutableStateOf("") }
  var latText by remember { mutableStateOf("19.0760") }
  var lngText by remember { mutableStateOf("72.8777") }
  var radiusText by remember { mutableStateOf("150") }
  var workingDaysText by remember { mutableStateOf("26") }
  var isFetchingGps by remember { mutableStateOf(false) }
  var gpsNote by remember { mutableStateOf<String?>(null) }

  val locationPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
      val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
        grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
      if (granted) {
        isFetchingGps = true
        scope.launch {
          val loc = LocationAndImageHelper.fetchCurrentLocation(context)
          isFetchingGps = false
          if (loc != null) {
            latText = String.format("%.5f", loc.latitude)
            lngText = String.format("%.5f", loc.longitude)
            gpsNote = "Captured office GPS coordinates"
          } else {
            gpsNote = "Could not lock GPS; enter coordinates manually or retry."
          }
        }
      } else {
        gpsNote = "Location permission denied. You can enter coordinates manually."
      }
    }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.widthIn(max = 560.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Text(
        text = "Setup Your Workspace",
        style = MaterialTheme.typography.headlineMedium,
      )
      Text(
        text = "Business owners create an office to get an invite code. Managers & staff join with the 6-character code.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
          selected = mode == 0,
          onClick = {
            mode = 0
            onClearError()
          },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          modifier = Modifier.testTag("tab_create_office"),
        ) {
          Text(strings.createOffice, maxLines = 1)
        }
        SegmentedButton(
          selected = mode == 1,
          onClick = {
            mode = 1
            onClearError()
          },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("tab_join_office"),
        ) {
          Text(strings.joinOffice, maxLines = 1)
        }
      }

      if (!errorMessage.isNullOrBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("onboarding_error_banner"),
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
          )
        }
      }

      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          if (mode == 0) {
            OutlinedTextField(
              value = officeName,
              onValueChange = {
                officeName = it
                onClearError()
              },
              label = { Text("Business / Office Name") },
              placeholder = { Text("e.g., Shree Ganesh Traders") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("create_office_name_input"),
            )
            OutlinedTextField(
              value = ownerPhone,
              onValueChange = { ownerPhone = it },
              label = { Text("Owner WhatsApp Number") },
              placeholder = { Text("+9198XXXXXXXX") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier.fillMaxWidth(),
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = latText,
                onValueChange = { latText = it },
                label = { Text("Office Latitude") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = lngText,
                onValueChange = { lngText = it },
                label = { Text("Office Longitude") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
              )
            }
            OutlinedButton(
              onClick = {
                val fineOk =
                  ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                if (fineOk) {
                  isFetchingGps = true
                  scope.launch {
                    val loc = LocationAndImageHelper.fetchCurrentLocation(context)
                    isFetchingGps = false
                    if (loc != null) {
                      latText = String.format("%.5f", loc.latitude)
                      lngText = String.format("%.5f", loc.longitude)
                      gpsNote = "Captured current office GPS"
                    } else {
                      gpsNote = "GPS unavailable; keeping default/manual coordinates."
                    }
                  }
                } else {
                  locationPermissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.ACCESS_FINE_LOCATION,
                      Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                  )
                }
              },
              enabled = !isFetchingGps,
              modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
              Icon(Icons.Rounded.MyLocation, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(if (isFetchingGps) "Getting GPS..." else "Use Current Location as Office")
            }
            if (gpsNote != null) {
              Text(
                text = gpsNote!!,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              OutlinedTextField(
                value = radiusText,
                onValueChange = { radiusText = it },
                label = { Text("Geofence (m)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = workingDaysText,
                onValueChange = { workingDaysText = it },
                label = { Text("Work Days / Mo") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
              )
            }

            Button(
              onClick = {
                onCreateOffice(
                  officeName.ifBlank { "My Office" },
                  ownerPhone,
                  latText.toDoubleOrNull() ?: 19.0760,
                  lngText.toDoubleOrNull() ?: 72.8777,
                  (radiusText.toLongOrNull() ?: 150L).coerceIn(20L, 5000L),
                  570L, // 09:30 AM
                  1110L, // 06:30 PM
                  15L,
                  (workingDaysText.toLongOrNull() ?: 26L).coerceIn(1L, 31L),
                )
              },
              enabled = !isBusy && officeName.isNotBlank(),
              modifier = Modifier.fillMaxWidth().height(56.dp).testTag("create_office_submit_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              if (isBusy) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
              } else {
                Icon(Icons.Rounded.Business, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.createOffice, style = MaterialTheme.typography.titleMedium)
              }
            }
          } else {
            OutlinedTextField(
              value = inviteCode,
              onValueChange = {
                inviteCode = it.uppercase().take(6)
                onClearError()
              },
              label = { Text("6-Character Invite Code") },
              placeholder = { Text("e.g., ZEN482") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
              modifier = Modifier.fillMaxWidth().testTag("join_invite_code_input"),
            )
            OutlinedTextField(
              value = designation,
              onValueChange = { designation = it },
              label = { Text("Your Designation") },
              placeholder = { Text("e.g., Accountant, Sales, Helper") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = staffPhone,
              onValueChange = { staffPhone = it },
              label = { Text("Mobile Number") },
              singleLine = true,
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier.fillMaxWidth(),
            )
            Button(
              onClick = { onJoinOffice(inviteCode, designation, staffPhone) },
              enabled = !isBusy && inviteCode.trim().length == 6,
              modifier = Modifier.fillMaxWidth().height(56.dp).testTag("join_office_submit_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              if (isBusy) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
              } else {
                Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.joinOffice, style = MaterialTheme.typography.titleMedium)
              }
            }
          }
        }
      }
    }
  }
}
