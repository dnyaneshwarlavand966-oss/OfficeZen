// OfficeZen editorial local workspace onboarding & office switcher screen
package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.Office
import com.example.hardware.LocationAndImageHelper
import com.example.localization.LocalAppStrings
import com.example.ui.components.SegmentedTabItem
import com.example.ui.components.StatusPill
import com.example.ui.components.ZenCard
import com.example.ui.components.ZenSegmentedTabs
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.MonoFontFamily
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
  isBusy: Boolean,
  defaultOwnerName: String = "Office Owner",
  existingOffices: List<Office> = emptyList(),
  errorMessage: String? = null,
  onClearError: () -> Unit = {},
  onSelectExistingOffice: (String) -> Unit = {},
  onCreateOffice: (
    officeName: String,
    ownerDisplayName: String,
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

  var mode by remember { mutableIntStateOf(0) }

  var inviteCode by remember { mutableStateOf("") }
  var designation by remember { mutableStateOf("Executive") }
  var staffPhone by remember { mutableStateOf("") }

  var officeName by remember { mutableStateOf("") }
  var ownerName by remember(defaultOwnerName) { mutableStateOf(defaultOwnerName) }
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
            latText = String.format(Locale.US, "%.5f", loc.latitude)
            lngText = String.format(Locale.US, "%.5f", loc.longitude)
            gpsNote = "Captured office GPS coordinates"
          } else {
            gpsNote = "Could not lock GPS; enter coordinates manually or retry."
          }
        }
      } else {
        gpsNote = "Location permission denied. You can enter coordinates manually."
      }
    }

  LaunchedEffect(Unit) {
    val hasLocPermission =
      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
    if (hasLocPermission && latText == "19.0760" && lngText == "72.8777") {
      isFetchingGps = true
      val loc = LocationAndImageHelper.fetchCurrentLocation(context)
      isFetchingGps = false
      if (loc != null && latText == "19.0760" && lngText == "72.8777") {
        latText = String.format(Locale.US, "%.5f", loc.latitude)
        lngText = String.format(Locale.US, "%.5f", loc.longitude)
        gpsNote = "Auto-detected current office GPS"
      }
    }
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.widthIn(max = 560.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      // Editorial Welcome Block with clean framed artwork thumbnail (no heavy overlay slab)
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_workspace_setup_1795504692168),
          contentDescription = "Setup workspace geofence illustration",
          modifier =
            Modifier
              .size(72.dp)
              .clip(RoundedCornerShape(18.dp))
              .border(0.75.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
          contentScale = ContentScale.Crop,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = "ON-DEVICE LEDGER",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "Setup Local Workspace",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = EditorialSerifFontFamily,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Text(
            text = "100% offline on this phone — zero sign-in required.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (existingOffices.isNotEmpty()) {
        ZenCard(
          cornerRadius = 20.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
              text = "Saved Local Offices",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            existingOffices.forEachIndexed { idx, saved ->
              FilledTonalButton(
                onClick = { onSelectExistingOffice(saved.id) },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("open_saved_office_${saved.id}"),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text = saved.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    StatusPill(text = saved.inviteCode, useMonospace = true)
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                  }
                }
              }
              if (idx < existingOffices.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), thickness = 0.5.dp)
              }
            }
          }
        }
      }

      ZenSegmentedTabs(
        items =
          listOf(
            SegmentedTabItem(strings.createOffice, "tab_create_office"),
            SegmentedTabItem(strings.joinOffice, "tab_join_office"),
          ),
        selectedIndex = mode,
        onSelect = { next ->
          mode = next
          onClearError()
        },
      )

      if (!errorMessage.isNullOrBlank()) {
        Surface(
          color = MaterialTheme.colorScheme.errorContainer,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
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

      ZenCard(
        cornerRadius = 22.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          if (mode == 0) {
            Text(
              text = "Business Identity & Geofence",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
              value = officeName,
              onValueChange = {
                officeName = it
                onClearError()
              },
              label = { Text("Business / Office Name") },
              placeholder = { Text("e.g., Shree Ganesh Traders") },
              leadingIcon = { Icon(Icons.Rounded.Business, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("create_office_name_input"),
            )
            OutlinedTextField(
              value = ownerName,
              onValueChange = { ownerName = it },
              label = { Text("Owner / Admin Name") },
              placeholder = { Text("e.g., Rajesh Sharma") },
              leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("create_owner_name_input"),
            )
            OutlinedTextField(
              value = ownerPhone,
              onValueChange = { ownerPhone = it },
              label = { Text("Owner WhatsApp Number") },
              placeholder = { Text("+9198XXXXXXXX") },
              leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              modifier = Modifier.fillMaxWidth(),
            )

            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
              shape = RoundedCornerShape(16.dp),
              border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    label = { Text("Latitude") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                  )
                  OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    label = { Text("Longitude") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                  )
                }
                FilledTonalButton(
                  onClick = {
                    val locOk =
                      ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    if (locOk) {
                      isFetchingGps = true
                      scope.launch {
                        val loc = LocationAndImageHelper.fetchCurrentLocation(context)
                        isFetchingGps = false
                        if (loc != null) {
                          latText = String.format(Locale.US, "%.5f", loc.latitude)
                          lngText = String.format(Locale.US, "%.5f", loc.longitude)
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
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                  Icon(Icons.Rounded.MyLocation, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text(if (isFetchingGps) "Locking Office GPS..." else "Use Current Location as Office")
                }
                if (gpsNote != null) {
                  Text(
                    text = gpsNote!!,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                  )
                }
              }
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
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
              )
              OutlinedTextField(
                value = workingDaysText,
                onValueChange = { workingDaysText = it },
                label = { Text("Work Days / Mo") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
              )
            }

            Button(
              onClick = {
                val parsedLat = latText.replace(',', '.').trim().toDoubleOrNull() ?: 19.0760
                val parsedLng = lngText.replace(',', '.').trim().toDoubleOrNull() ?: 72.8777
                onCreateOffice(
                  officeName.ifBlank { "My Office" },
                  ownerName.ifBlank { defaultOwnerName },
                  ownerPhone,
                  parsedLat,
                  parsedLng,
                  (radiusText.toLongOrNull() ?: 150L).coerceIn(20L, 5000L),
                  570L,
                  1110L,
                  15L,
                  (workingDaysText.toLongOrNull() ?: 26L).coerceIn(1L, 31L),
                )
              },
              enabled = !isBusy && officeName.isNotBlank(),
              modifier = Modifier.fillMaxWidth().height(56.dp).testTag("create_office_submit_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              if (isBusy) {
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              } else {
                Icon(Icons.Rounded.Business, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.createOffice, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
            }
          } else {
            Text(
              text = "Open Saved Local Office",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
              value = inviteCode,
              onValueChange = {
                inviteCode = it.uppercase().take(6)
                onClearError()
              },
              label = { Text("6-Character Office Code") },
              placeholder = { Text("e.g., ZEN482") },
              leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
              modifier = Modifier.fillMaxWidth().testTag("join_invite_code_input"),
            )
            OutlinedTextField(
              value = designation,
              onValueChange = { designation = it },
              label = { Text("Your Designation") },
              placeholder = { Text("e.g., Accountant, Sales, Helper") },
              leadingIcon = { Icon(Icons.Rounded.WorkOutline, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
              value = staffPhone,
              onValueChange = { staffPhone = it },
              label = { Text("Mobile Number") },
              leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
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
                CircularProgressIndicator(
                  modifier = Modifier.size(22.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.onPrimary,
                )
              } else {
                Icon(Icons.Rounded.GroupAdd, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.joinOffice, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }
  }
}
