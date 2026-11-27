// OfficeZen Material 3 responsive DashboardFeatureGrid & DashboardFeatureCard components
package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.ChaiCream
import com.example.ui.theme.ChaiTerracotta
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.LeaveRose
import com.example.ui.theme.LeaveRoseBg
import com.example.ui.theme.LedgerIndigo
import com.example.ui.theme.LedgerIndigoBg
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.OfficeZenTheme
import com.example.ui.theme.ReportViolet
import com.example.ui.theme.ReportVioletBg
import com.example.ui.theme.SettingsSlate
import com.example.ui.theme.SettingsSlateBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.TeamCyan
import com.example.ui.theme.TeamCyanBg

@Immutable
data class DashboardFeatureItem(
  val title: String,
  val subtitle: String,
  val icon: ImageVector,
  val iconTint: Color,
  val iconContainerColor: Color,
  val testTag: String,
  val badgeText: String? = null,
  val onClick: () -> Unit,
)

@Composable
fun DashboardFeatureGrid(
  items: List<DashboardFeatureItem>,
  modifier: Modifier = Modifier,
  sectionTitle: String? = null,
  sectionSubtitle: String? = null,
  trailingBadge: String? = null,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (!sectionTitle.isNullOrBlank()) {
      SectionHeader(
        title = sectionTitle,
        subtitle = sectionSubtitle,
        trailingBadge = trailingBadge,
      )
    }

    BoxWithConstraints(
      modifier = Modifier.fillMaxWidth().testTag("dashboard_feature_grid"),
    ) {
      val fontScale = LocalDensity.current.fontScale
      val columns =
        when {
          maxWidth >= 760.dp && fontScale <= 1.2f -> 4
          maxWidth >= 520.dp && fontScale <= 1.25f -> 3
          maxWidth >= 260.dp -> 2
          else -> 1
        }
      val rows = remember(items, columns) { items.chunked(columns) }

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        rows.forEach { rowItems ->
          Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            rowItems.forEach { feature ->
              DashboardFeatureCard(
                item = feature,
                modifier = Modifier.weight(1f).fillMaxHeight(),
              )
            }
            repeat(columns - rowItems.size) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}

@Composable
fun DashboardFeatureCard(
  item: DashboardFeatureItem,
  modifier: Modifier = Modifier,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val cardScaleState =
    animateFloatAsState(
      targetValue = if (pressed) 0.955f else 1f,
      animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
      label = "featureCardScale",
    )
  val iconScaleState =
    animateFloatAsState(
      targetValue = if (pressed) 0.92f else 1f,
      animationSpec = spring(dampingRatio = 0.60f, stiffness = Spring.StiffnessMedium),
      label = "featureCardIconScale",
    )
  val borderColor by
    animateColorAsState(
      targetValue =
        if (pressed) item.iconTint.copy(alpha = 0.38f)
        else Color.Transparent,
      animationSpec = tween(durationMillis = 120),
      label = "featureCardBorder",
    )

  ElevatedCard(
    onClick = item.onClick,
    interactionSource = interactionSource,
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation =
      CardDefaults.elevatedCardElevation(
        defaultElevation = 3.dp,
        pressedElevation = 1.dp,
        focusedElevation = 4.dp,
        hoveredElevation = 4.dp,
      ),
    modifier =
      modifier
        .heightIn(min = 128.dp)
        .minimumInteractiveComponentSize()
        .graphicsLayer {
          val s = cardScaleState.value
          scaleX = s
          scaleY = s
        }
        .semantics(mergeDescendants = true) {
          contentDescription = "${item.title}. ${item.subtitle}"
        }
        .testTag(item.testTag),
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
          .padding(16.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Surface(
          color = item.iconContainerColor,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(0.75.dp, item.iconTint.copy(alpha = 0.16f)),
          modifier =
            Modifier
              .size(44.dp)
              .graphicsLayer {
                val s = iconScaleState.value
                scaleX = s
                scaleY = s
              },
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = item.icon,
              contentDescription = null,
              tint = item.iconTint,
              modifier = Modifier.size(23.dp),
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (!item.badgeText.isNullOrBlank()) {
            Surface(
              color = item.iconContainerColor,
              shape = RoundedCornerShape(50),
              border = BorderStroke(0.75.dp, item.iconTint.copy(alpha = 0.2f)),
            ) {
              Text(
                text = item.badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = MonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = item.iconTint,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp),
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
          text = item.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = item.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}

@Preview(showBackground = true, widthDp = 380)
@Composable
private fun DashboardFeatureGridCompactPreview() {
  OfficeZenTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      DashboardFeatureGrid(
        sectionTitle = "Office Modules",
        sectionSubtitle = "All core office operations in one workspace",
        items = samplePreviewFeatureItems(),
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 820)
@Composable
private fun DashboardFeatureGridExpandedPreview() {
  OfficeZenTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      DashboardFeatureGrid(
        sectionTitle = "Office Modules",
        sectionSubtitle = "All core office operations in one workspace",
        items = samplePreviewFeatureItems(),
      )
    }
  }
}

private fun samplePreviewFeatureItems(): List<DashboardFeatureItem> =
  listOf(
    DashboardFeatureItem("Attendance", "19/24 present today", Icons.Rounded.VerifiedUser, EmeraldDeep, EmeraldSoftBg, "preview_attendance", "19/24") {},
    DashboardFeatureItem("Documents", "14 inward / outward", Icons.Rounded.Description, LedgerIndigo, LedgerIndigoBg, "preview_docs", "14") {},
    DashboardFeatureItem("Tea & Coffee", "28 cups logged today", Icons.Rounded.LocalCafe, ChaiTerracotta, ChaiCream, "preview_tea", "28") {},
    DashboardFeatureItem("Payroll", "3-late & overtime rules", Icons.Rounded.AccountBalanceWallet, StatusGreen, StatusGreenBg, "preview_payroll") {},
    DashboardFeatureItem("Team / Employees", "24 active members", Icons.Rounded.Groups, TeamCyan, TeamCyanBg, "preview_team", "24") {},
    DashboardFeatureItem("Leave", "2 pending requests", Icons.AutoMirrored.Rounded.EventNote, LeaveRose, LeaveRoseBg, "preview_leave", "2") {},
    DashboardFeatureItem("Reports / PDF Export", "PDF, CSV & WhatsApp share", Icons.Rounded.Assessment, ReportViolet, ReportVioletBg, "preview_reports") {},
    DashboardFeatureItem("Settings", "Shift, GPS & JSON backup", Icons.Rounded.Settings, SettingsSlate, SettingsSlateBg, "preview_settings") {},
  )
