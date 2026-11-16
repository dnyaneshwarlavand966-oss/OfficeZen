// OfficeZen chai/coffee tracker & inward/outward document register screen
package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.Member
import com.example.data.model.MemberRole
import com.example.data.model.Office
import com.example.data.model.OfficeDocument
import com.example.data.model.TeaLog
import com.example.data.model.TeaPurchase
import com.example.domain.MonthEndSummary
import com.example.hardware.LocationAndImageHelper
import com.example.localization.LocalAppStrings
import com.example.ui.components.CameraCaptureDialog
import com.example.ui.components.EmptyPlaceholderCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.SaffronContainer
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.viewmodel.OfficeViewModel
import kotlin.math.roundToInt

@Composable
fun ChaiDocsScreen(
  initialSubTab: Int,
  office: Office,
  me: Member?,
  teaLogs: List<TeaLog>,
  teaPurchases: List<TeaPurchase>,
  documents: List<OfficeDocument>,
  monthEndSummary: MonthEndSummary?,
  onOneTapTea: (String) -> Unit,
  onDeleteTeaLog: (String) -> Unit,
  onAddPurchase: (category: String, qtyLabel: String, amountInr: Double, note: String) -> Unit,
  onAddDocument: (direction: String, party: String, subject: String, status: String, scanBase64: String, note: String) -> Unit,
  onUpdateDocumentStatus: (OfficeDocument, String) -> Unit,
) {
  val strings = LocalAppStrings.current
  val context = LocalContext.current
  val isManagerOrOwner =
    me?.roleEnum == MemberRole.OWNER || me?.roleEnum == MemberRole.MANAGER

  var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) } // 0 = Chai, 1 = Docs
  var showPurchaseDialog by remember { mutableStateOf(false) }
  var showAddDocDialog by remember { mutableStateOf(false) }
  var previewScanDoc by remember { mutableStateOf<OfficeDocument?>(null) }

  var docSearch by remember { mutableStateOf("") }
  var docFilter by remember { mutableStateOf("all") } // all, inward, outward

  if (showPurchaseDialog) {
    AddPurchaseDialog(
      onSave = { cat, qty, amt, note ->
        onAddPurchase(cat, qty, amt, note)
        showPurchaseDialog = false
      },
      onDismiss = { showPurchaseDialog = false },
    )
  }

  if (showAddDocDialog) {
    AddDocumentDialog(
      onSave = { dir, party, subj, status, scanB64, note ->
        onAddDocument(dir, party, subj, status, scanB64, note)
        showAddDocDialog = false
      },
      onDismiss = { showAddDocDialog = false },
    )
  }

  previewScanDoc?.let { doc ->
    val bmp = remember(doc.scanBase64) { LocationAndImageHelper.decodeBase64(doc.scanBase64) }
    AlertDialog(
      onDismissRequest = { previewScanDoc = null },
      title = { Text("${doc.serialNo} · ${doc.subject}") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("${doc.direction.uppercase()} · ${doc.partyName} · ${doc.dateKey}")
          if (bmp != null) {
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = "Document scan",
              modifier = Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(14.dp)),
              contentScale = ContentScale.Fit,
            )
          }
          Text("Status: ${doc.status}")
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("pending", "received", "dispatched", "archived").forEach { st ->
              FilterChip(
                selected = doc.status == st,
                onClick = {
                  onUpdateDocumentStatus(doc, st)
                  previewScanDoc = null
                },
                label = { Text(st.replaceFirstChar { it.uppercase() }) },
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { previewScanDoc = null }) { Text("Close") }
      },
    )
  }

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 700.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          SegmentedButton(
            selected = subTab == 0,
            onClick = { subTab = 0 },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            modifier = Modifier.testTag("subtab_chai"),
          ) {
            Text("${strings.tea} / ${strings.coffee}", maxLines = 1)
          }
          SegmentedButton(
            selected = subTab == 1,
            onClick = { subTab = 1 },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            modifier = Modifier.testTag("subtab_docs"),
          ) {
            Text("${strings.inward} / ${strings.outward}", maxLines = 1)
          }
        }
      }

      if (subTab == 0) {
        // 1-Tap Tea & Coffee Card
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Text("Tap once to log your cup", style = MaterialTheme.typography.titleLarge)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Button(
                  onClick = { onOneTapTea("tea") },
                  modifier = Modifier.weight(1f).height(64.dp).testTag("chai_log_tea_button"),
                  shape = RoundedCornerShape(16.dp),
                ) {
                  Icon(Icons.Rounded.Coffee, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text(strings.logTea, style = MaterialTheme.typography.titleMedium)
                }
                FilledTonalButton(
                  onClick = { onOneTapTea("coffee") },
                  modifier = Modifier.weight(1f).height(64.dp).testTag("chai_log_coffee_button"),
                  shape = RoundedCornerShape(16.dp),
                ) {
                  Icon(Icons.Rounded.LocalCafe, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text(strings.logCoffee, style = MaterialTheme.typography.titleMedium)
                }
              }

              if (monthEndSummary != null) {
                Surface(
                  color = SaffronContainer,
                  shape = RoundedCornerShape(14.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column {
                      Text("Monthly Cups", style = MaterialTheme.typography.labelMedium)
                      Text(
                        "${monthEndSummary.totalTeaCups} Tea · ${monthEndSummary.totalCoffeeCups} Coffee",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text("Total Spend", style = MaterialTheme.typography.labelMedium)
                      Text(
                        "₹${monthEndSummary.totalPantryPurchaseInr.roundToInt()} (₹${monthEndSummary.effectiveCostPerCupInr}/cup)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                      )
                    }
                  }
                }
              }

              if (isManagerOrOwner) {
                OutlinedButton(
                  onClick = { showPurchaseDialog = true },
                  modifier = Modifier.fillMaxWidth().height(50.dp).testTag("add_pantry_purchase_button"),
                ) {
                  Icon(Icons.Rounded.ShoppingBag, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text("${strings.addPurchase} (Milk, Sugar, Tea Powder)")
                }
              }
            }
          }
        }

        // Per-Employee Tea Cost Share
        if (monthEndSummary != null && monthEndSummary.employeePayrolls.isNotEmpty()) {
          item {
            SectionHeader(title = "Per-Employee Tea/Coffee Cost", subtitle = "Monthly cups & estimated cost")
          }
          items(monthEndSummary.employeePayrolls, key = { "tea_${it.member.userId}" }) { row ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(row.member.displayName, style = MaterialTheme.typography.titleMedium)
                Text(
                  "${row.teaCups} cups · ₹${row.teaEstimatedCostInr.roundToInt()}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        // Recent Tea Logs & Purchases
        item { SectionHeader(title = "Recent Tea Logs") }
        items(teaLogs.take(15), key = { it.id }) { log ->
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column {
                Text("${log.userName} · ${log.item.uppercase()}", style = MaterialTheme.typography.titleMedium)
                Text(log.dateKey, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              if (log.userId == me?.userId || isManagerOrOwner) {
                IconButton(onClick = { onDeleteTeaLog(log.id) }) {
                  Icon(Icons.Rounded.Delete, contentDescription = "Undo log")
                }
              }
            }
          }
        }

        if (teaPurchases.isNotEmpty()) {
          item { SectionHeader(title = "Pantry Purchases") }
          items(teaPurchases.take(15), key = { it.id }) { pur ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column {
                  Text(
                    "${pur.category.replace("_", " ").uppercase()} (${pur.quantityLabel})",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text("${pur.dateKey} · ${pur.addedByName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("₹${pur.amountInr.roundToInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        // Inward / Outward Document Register
        item {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = { showAddDocDialog = true },
              modifier = Modifier.fillMaxWidth().height(56.dp).testTag("add_document_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(strings.addDocument, style = MaterialTheme.typography.titleMedium)
            }

            OutlinedTextField(
              value = docSearch,
              onValueChange = { docSearch = it },
              placeholder = { Text("Search serial no, sender/receiver, subject...") },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("document_search_input"),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              listOf("all" to "All", "inward" to strings.inward, "outward" to strings.outward).forEach { (key, label) ->
                FilterChip(
                  selected = docFilter == key,
                  onClick = { docFilter = key },
                  label = { Text(label) },
                )
              }
            }
          }
        }

        val filteredDocs =
          documents.filter { doc ->
            val dirOk = docFilter == "all" || doc.direction == docFilter
            val q = docSearch.trim().lowercase()
            val queryOk =
              q.isBlank() ||
                doc.serialNo.lowercase().contains(q) ||
                doc.partyName.lowercase().contains(q) ||
                doc.subject.lowercase().contains(q)
            dirOk && queryOk
          }

        if (filteredDocs.isEmpty()) {
          item {
            EmptyPlaceholderCard(
              title = "No register entries found",
              body = "Tap '${strings.addDocument}' to record an inward or outward letter, courier, invoice, or parcel with a photo scan.",
            )
          }
        } else {
          items(filteredDocs, key = { it.id }) { doc ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier.fillMaxWidth().clickable { previewScanDoc = doc },
            ) {
              Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                val thumb = remember(doc.scanBase64) { LocationAndImageHelper.decodeBase64(doc.scanBase64) }
                if (thumb != null) {
                  Image(
                    bitmap = thumb.asImageBitmap(),
                    contentDescription = "Document scan thumbnail",
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop,
                  )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(doc.serialNo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatusPill(
                      text = doc.status.uppercase(),
                      containerColor = if (doc.status == "pending") StatusAmberBg else StatusGreenBg,
                      contentColor = if (doc.status == "pending") StatusAmber else StatusGreen,
                    )
                  }
                  Text(doc.subject, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                  Text(
                    "${doc.direction.uppercase()} · ${doc.partyName} · ${doc.dateKey}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AddPurchaseDialog(
  onSave: (category: String, qtyLabel: String, amountInr: Double, note: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var category by remember { mutableStateOf("milk") }
  var qtyLabel by remember { mutableStateOf("2 Litre") }
  var amountText by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Pantry Purchase") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("milk" to "Milk", "sugar" to "Sugar", "tea_powder" to "Tea Powder").forEach { (k, lbl) ->
            FilterChip(selected = category == k, onClick = { category = k }, label = { Text(lbl) })
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("coffee_powder" to "Coffee", "other" to "Other").forEach { (k, lbl) ->
            FilterChip(selected = category == k, onClick = { category = k }, label = { Text(lbl) })
          }
        }
        OutlinedTextField(
          value = qtyLabel,
          onValueChange = { qtyLabel = it },
          label = { Text("Quantity (e.g. 2 Litre, 1 kg)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Amount Paid (₹)") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Vendor / Note (optional)") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amountText.toDoubleOrNull() ?: 0.0
          if (amt > 0.0 && qtyLabel.isNotBlank()) onSave(category, qtyLabel, amt, note)
        },
        enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0.0 && qtyLabel.isNotBlank(),
      ) {
        Text("Save")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AddDocumentDialog(
  onSave: (direction: String, party: String, subject: String, status: String, scanBase64: String, note: String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  var direction by remember { mutableStateOf("inward") }
  var party by remember { mutableStateOf("") }
  var subject by remember { mutableStateOf("") }
  var status by remember { mutableStateOf("received") }
  var note by remember { mutableStateOf("") }
  var scanBase64 by remember { mutableStateOf("") }
  var showCamera by remember { mutableStateOf(false) }

  // Zero-permission Android Photo Picker (Google Play compliant)
  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scanBase64 = LocationAndImageHelper.uriToBase64(context, uri) ?: ""
      }
    }

  val cameraPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) showCamera = true
    }

  if (showCamera) {
    CameraCaptureDialog(
      title = "Scan Document",
      useFrontCamera = false,
      onCaptured = { bmp ->
        scanBase64 = LocationAndImageHelper.bitmapToBase64(bmp, maxDimension = 640, quality = 70)
        showCamera = false
      },
      onDismiss = { showCamera = false },
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Register Entry") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = direction == "inward",
            onClick = {
              direction = "inward"
              status = "received"
            },
            label = { Text("Inward") },
          )
          FilterChip(
            selected = direction == "outward",
            onClick = {
              direction = "outward"
              status = "dispatched"
            },
            label = { Text("Outward") },
          )
        }
        OutlinedTextField(
          value = party,
          onValueChange = { party = it },
          label = { Text(if (direction == "inward") "Sender Name" else "Receiver Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Subject / Document Title") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedButton(
            onClick = {
              val ok =
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                  PackageManager.PERMISSION_GRANTED
              if (ok) showCamera = true
              else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Scan", maxLines = 1)
          }
          OutlinedButton(
            onClick = {
              photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
              )
            },
            modifier = Modifier.weight(1f),
          ) {
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Gallery", maxLines = 1)
          }
        }
        if (scanBase64.isNotBlank()) {
          StatusPill("Photo / scan attached", StatusGreenBg, StatusGreen)
        }
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Remarks (optional)") },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onSave(direction, party, subject, status, scanBase64, note) },
        enabled = party.isNotBlank() && subject.isNotBlank(),
      ) {
        Text("Save Entry")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
