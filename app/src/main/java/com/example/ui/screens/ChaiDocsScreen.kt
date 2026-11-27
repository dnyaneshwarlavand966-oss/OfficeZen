// OfficeZen pantry counter & inward/outward document ledger screen
package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
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
import com.example.ui.components.SegmentedTabItem
import com.example.ui.components.StatusPill
import com.example.ui.components.ZenCard
import com.example.ui.components.ZenSegmentedTabs
import com.example.ui.theme.ChaiCream
import com.example.ui.theme.ChaiInk
import com.example.ui.theme.ChaiTerracotta
import com.example.ui.theme.EditorialSerifFontFamily
import com.example.ui.theme.EspressoBg
import com.example.ui.theme.EspressoBrown
import com.example.ui.theme.LedgerIndigo
import com.example.ui.theme.LedgerIndigoBg
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
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
  val isManagerOrOwner =
    me?.roleEnum == MemberRole.OWNER || me?.roleEnum == MemberRole.MANAGER

  var subTab by remember(initialSubTab) { mutableIntStateOf(initialSubTab) }
  var showPurchaseDialog by remember { mutableStateOf(false) }
  var showAddDocDialog by remember { mutableStateOf(false) }
  var previewScanDoc by remember { mutableStateOf<OfficeDocument?>(null) }

  var docSearch by remember { mutableStateOf("") }
  var docFilter by remember { mutableStateOf("all") }

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
      shape = RoundedCornerShape(24.dp),
      title = {
        Text(
          text = "${doc.serialNo} · ${doc.subject}",
          style = MaterialTheme.typography.titleLarge,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "${doc.direction.uppercase()} · ${doc.partyName} · ${doc.dateKey}",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = MonoFontFamily,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (bmp != null) {
            Image(
              bitmap = bmp.asImageBitmap(),
              contentDescription = "Document scan",
              modifier =
                Modifier.fillMaxWidth()
                  .height(250.dp)
                  .clip(RoundedCornerShape(16.dp))
                  .border(0.75.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
              contentScale = ContentScale.Fit,
            )
          }
          Text("Update Status", style = MaterialTheme.typography.labelLarge)
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
      contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ZenSegmentedTabs(
          items =
            listOf(
              SegmentedTabItem(
                label = "${strings.tea} / ${strings.coffee}",
                testTag = "subtab_chai",
              ),
              SegmentedTabItem(
                label = "${strings.inward} / ${strings.outward}",
                testTag = "subtab_docs",
              ),
            ),
          selectedIndex = subTab,
          onSelect = { subTab = it },
        )
      }

      if (subTab == 0) {
        // Warm Editorial Chai Counter Card
        item {
          ZenCard(
            cornerRadius = 24.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Image(
                  painter = painterResource(id = R.drawable.img_chai_banner_1795504692697),
                  contentDescription = "Office pantry chai and coffee",
                  contentScale = ContentScale.Crop,
                  modifier =
                    Modifier
                      .size(72.dp)
                      .clip(RoundedCornerShape(16.dp))
                      .border(0.75.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                  Text(
                    text = "PANTRY COUNTER",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = MonoFontFamily,
                    color = ChaiTerracotta,
                  )
                  Text(
                    text = "1-Tap Chai & Coffee",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = EditorialSerifFontFamily,
                    fontWeight = FontWeight.SemiBold,
                  )
                  Text(
                    text = "Tap once to log a cup for ${me?.displayName ?: "team"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Button(
                  onClick = { onOneTapTea("tea") },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = ChaiTerracotta,
                      contentColor = Color.White,
                    ),
                  modifier = Modifier.weight(1f).height(58.dp).testTag("chai_log_tea_button"),
                  shape = RoundedCornerShape(16.dp),
                ) {
                  Icon(Icons.Rounded.Coffee, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text(strings.logTea, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Button(
                  onClick = { onOneTapTea("coffee") },
                  colors =
                    ButtonDefaults.buttonColors(
                      containerColor = EspressoBg,
                      contentColor = EspressoBrown,
                    ),
                  border = BorderStroke(0.75.dp, EspressoBrown.copy(alpha = 0.28f)),
                  modifier = Modifier.weight(1f).height(58.dp).testTag("chai_log_coffee_button"),
                  shape = RoundedCornerShape(16.dp),
                ) {
                  Icon(Icons.Rounded.LocalCafe, contentDescription = null)
                  Spacer(Modifier.width(8.dp))
                  Text(strings.logCoffee, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
              }

              if (monthEndSummary != null) {
                Surface(
                  color = ChaiCream,
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(0.75.dp, ChaiTerracotta.copy(alpha = 0.22f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Column {
                      Text(
                        text = "MONTHLY CUPS",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = MonoFontFamily,
                        color = ChaiInk.copy(alpha = 0.72f),
                      )
                      Text(
                        text = "${monthEndSummary.totalTeaCups} Tea · ${monthEndSummary.totalCoffeeCups} Coffee",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = ChaiInk,
                      )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text(
                        text = "TOTAL SPEND",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = MonoFontFamily,
                        color = ChaiInk.copy(alpha = 0.72f),
                      )
                      Text(
                        text = "₹${monthEndSummary.totalPantryPurchaseInr.roundToInt()} (₹${monthEndSummary.effectiveCostPerCupInr}/cup)",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = ChaiTerracotta,
                      )
                    }
                  }
                }
              }

              if (isManagerOrOwner) {
                OutlinedButton(
                  onClick = { showPurchaseDialog = true },
                  shape = RoundedCornerShape(14.dp),
                  border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
                  modifier = Modifier.fillMaxWidth().height(48.dp).testTag("add_pantry_purchase_button"),
                ) {
                  Icon(Icons.Rounded.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(Modifier.width(8.dp))
                  Text("${strings.addPurchase} (Milk, Sugar, Tea Powder)")
                }
              }
            }
          }
        }

        // Per-Employee Tea Cost Share grouped list
        if (monthEndSummary != null && monthEndSummary.employeePayrolls.isNotEmpty()) {
          item {
            SectionHeader(
              title = "Per-Employee Pantry Share",
              subtitle = "Monthly cups & estimated cost",
              trailingBadge = "${monthEndSummary.employeePayrolls.size} staff",
            )
          }
          item {
            ZenCard(cornerRadius = 18.dp, modifier = Modifier.fillMaxWidth()) {
              Column {
                monthEndSummary.employeePayrolls.forEachIndexed { idx, row ->
                  Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                      Surface(
                        color = ChaiCream,
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp),
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Text(
                            text = row.member.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = ChaiTerracotta,
                          )
                        }
                      }
                      Text(row.member.displayName, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                      text = "${row.teaCups} cups · ₹${row.teaEstimatedCostInr.roundToInt()}",
                      style = MaterialTheme.typography.titleMedium,
                      fontFamily = MonoFontFamily,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  if (idx < monthEndSummary.employeePayrolls.lastIndex) {
                    HorizontalDivider(
                      color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
                      thickness = 0.75.dp,
                    )
                  }
                }
              }
            }
          }
        }

        item {
          SectionHeader(
            title = "Recent Tea Logs",
            trailingBadge = "${teaLogs.take(15).size} recent",
          )
        }
        items(teaLogs.take(15), key = { it.id }) { log ->
          val isTea = log.item.lowercase() == "tea"
          ZenCard(
            cornerRadius = 16.dp,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp).fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Surface(
                  color = if (isTea) ChaiCream else EspressoBg,
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.size(36.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      imageVector = if (isTea) Icons.Rounded.Coffee else Icons.Rounded.LocalCafe,
                      contentDescription = null,
                      tint = if (isTea) ChaiTerracotta else EspressoBrown,
                      modifier = Modifier.size(18.dp),
                    )
                  }
                }
                Column {
                  Text("${log.userName} · ${log.item.uppercase()}", style = MaterialTheme.typography.titleMedium)
                  Text(
                    text = log.dateKey,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
              if (log.userId == me?.userId || isManagerOrOwner) {
                IconButton(onClick = { onDeleteTeaLog(log.id) }) {
                  Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Undo log",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        if (teaPurchases.isNotEmpty()) {
          item { SectionHeader(title = "Pantry Purchases", trailingBadge = "${teaPurchases.take(15).size}") }
          items(teaPurchases.take(15), key = { it.id }) { pur ->
            ZenCard(
              cornerRadius = 16.dp,
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(14.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column {
                  Text(
                    text = "${pur.category.replace("_", " ").uppercase()} (${pur.quantityLabel})",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = "${pur.dateKey} · ${pur.addedByName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = "₹${pur.amountInr.roundToInt()}",
                  style = MaterialTheme.typography.titleMedium,
                  fontFamily = MonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  color = ChaiTerracotta,
                )
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
              modifier = Modifier.fillMaxWidth().height(54.dp).testTag("add_document_button"),
              shape = RoundedCornerShape(16.dp),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null)
              Spacer(Modifier.width(8.dp))
              Text(strings.addDocument, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
              value = docSearch,
              onValueChange = { docSearch = it },
              placeholder = { Text("Search serial no, sender/receiver, subject...") },
              leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              colors =
                OutlinedTextFieldDefaults.colors(
                  focusedContainerColor = MaterialTheme.colorScheme.surface,
                  unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                  unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                ),
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
            val isInward = doc.direction == "inward"
            ZenCard(
              cornerRadius = 18.dp,
              onClick = { previewScanDoc = doc },
              modifier = Modifier.fillMaxWidth(),
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
                    modifier =
                      Modifier.size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(0.75.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                  )
                } else {
                  Surface(
                    color = if (isInward) LedgerIndigoBg else ChaiCream,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(48.dp),
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(
                        Icons.Rounded.Description,
                        contentDescription = null,
                        tint = if (isInward) LedgerIndigo else ChaiTerracotta,
                      )
                    }
                  }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Text(
                        text = doc.serialNo,
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = MonoFontFamily,
                        fontWeight = FontWeight.Bold,
                      )
                      StatusPill(
                        text = doc.direction.uppercase(),
                        containerColor = if (isInward) LedgerIndigoBg else ChaiCream,
                        contentColor = if (isInward) LedgerIndigo else ChaiInk,
                      )
                    }
                    StatusPill(
                      text = doc.status.uppercase(),
                      containerColor = if (doc.status == "pending") StatusAmberBg else StatusGreenBg,
                      contentColor = if (doc.status == "pending") StatusAmber else StatusGreen,
                    )
                  }
                  Text(
                    text = doc.subject,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "${doc.partyName} · ${doc.dateKey}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = MonoFontFamily,
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
    shape = RoundedCornerShape(24.dp),
    title = { Text("Add Pantry Purchase", style = MaterialTheme.typography.titleLarge) },
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
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = amountText,
          onValueChange = { amountText = it },
          label = { Text("Amount Paid (₹)") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = note,
          onValueChange = { note = it },
          label = { Text("Vendor / Note (optional)") },
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
        scanBase64 =
          LocationAndImageHelper.bitmapToBoundedBase64(
            bitmap = bmp,
            maxDimension = 640,
            initialQuality = 70,
            maxBase64Chars = LocationAndImageHelper.MAX_FIRESTORE_DOC_SCAN_B64_CHARS,
          )
        showCamera = false
      },
      onDismiss = { showCamera = false },
    )
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    title = { Text("New Register Entry", style = MaterialTheme.typography.titleLarge) },
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
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = subject,
          onValueChange = { subject = it },
          label = { Text("Subject / Document Title") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
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
            shape = RoundedCornerShape(14.dp),
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
            shape = RoundedCornerShape(14.dp),
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
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        shape = RoundedCornerShape(14.dp),
        onClick = { onSave(direction, party, subject, status, scanBase64, note) },
        enabled = party.isNotBlank() && subject.isNotBlank(),
      ) {
        Text("Save Entry")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
