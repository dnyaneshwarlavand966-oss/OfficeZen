// OfficeZen Material 3 responsive AttendanceSummaryCard with color-coded KPI icons
package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
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
import com.example.localization.LocalAppStrings
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.OfficeZenTheme
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.StatusRed
import com.example.ui.theme.StatusRedBg
import kotlin.math.roundToInt

enum class AttendanceMetricType {
  TOTAL_STAFF,
  PRESENT,
  ABSENT,
  LATE,
}

private data class AttendanceMetricSpec(
  val type: AttendanceMetricType,
  val label: String,
  val count: Int,
  val icon: ImageVector,
  val containerColor: Color,
  val iconBadgeColor: Color,
  val accentColor: Color,
  val testTag: String,
)

@Composable
fun AttendanceSummaryCard(
  totalStaff: Int,
  presentCount: Int,
  absentCount: Int,
  lateCount: Int,
  dateLabel: String,
  modifier: Modifier = Modifier,
  title: String = LocalAppStrings.current.todayAttendanceSummary,
  inviteCodeBadge: String? = null,
  onMetricClick: ((AttendanceMetricType) -> Unit)? = null,
) {
  val strings = LocalAppStrings.current
  val safeTotal = totalStaff.coerceAtLeast(0)
  val safePresent = presentCount.coerceAtLeast(0)
  val safeAbsent = absentCount.coerceAtLeast(0)
  val safeLate = lateCount.coerceAtLeast(0)

  val rawRatio =
    if (safeTotal > 0) (safePresent.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
    else 0f
  val animatedRatio by
    animateFloatAsState(
      targetValue = rawRatio,
      animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
      label = "attendancePresenceBar",
    )
  val presencePercent = (rawRatio * 100f).roundToInt().coerceIn(0, 100)

  val metrics =
    remember(strings, safeTotal, safePresent, safeAbsent, safeLate) {
      listOf(
        AttendanceMetricSpec(
          type = AttendanceMetricType.TOTAL_STAFF,
          label = strings.totalStaff,
          count = safeTotal,
          icon = Icons.Rounded.Groups,
          containerColor = EmeraldSoftBg,
          iconBadgeColor = EmeraldContainer,
          accentColor = EmeraldDeep,
          testTag = "summary_total_staff",
        ),
        AttendanceMetricSpec(
          type = AttendanceMetricType.PRESENT,
          label = strings.present,
          count = safePresent,
          icon = Icons.Rounded.CheckCircle,
          containerColor = StatusGreenBg,
          iconBadgeColor = StatusGreen.copy(alpha = 0.16f),
          accentColor = StatusGreen,
          testTag = "summary_present",
        ),
        AttendanceMetricSpec(
          type = AttendanceMetricType.ABSENT,
          label = strings.absent,
          count = safeAbsent,
          icon = Icons.Rounded.PersonOff,
          containerColor = StatusRedBg,
          iconBadgeColor = StatusRed.copy(alpha = 0.14f),
          accentColor = StatusRed,
          testTag = "summary_absent",
        ),
        AttendanceMetricSpec(
          type = AttendanceMetricType.LATE,
          label = strings.late,
          count = safeLate,
          icon = Icons.Rounded.Schedule,
          containerColor = StatusAmberBg,
          iconBadgeColor = StatusAmber.copy(alpha = 0.18f),
          accentColor = StatusAmber,
          testTag = "summary_late",
        ),
      )
    }

  ElevatedCard(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    modifier = modifier.fillMaxWidth().testTag("today_attendance_summary_card"),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(
          modifier = Modifier.weight(1f).padding(end = 8.dp),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = dateLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (!inviteCodeBadge.isNullOrBlank()) {
          StatusPill(
            text = inviteCodeBadge,
            containerColor = EmeraldSoftBg,
            contentColor = EmeraldDeep,
            useMonospace = true,
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        Canvas(
          modifier = Modifier.weight(1f).height(6.dp),
        ) {
          val corner = CornerRadius(6.dp.toPx(), 6.dp.toPx())
          drawRoundRect(
            color = trackColor,
            cornerRadius = corner,
          )
          if (animatedRatio > 0f) {
            drawRoundRect(
              color = StatusGreen,
              size = Size(width = size.width * animatedRatio, height = size.height),
              cornerRadius = corner,
            )
          }
        }
        Text(
          text = "$presencePercent%",
          style = MaterialTheme.typography.labelMedium,
          fontFamily = MonoFontFamily,
          fontWeight = FontWeight.Bold,
          color = if (safePresent > 0) StatusGreen else MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("summary_attendance_rate"),
        )
      }

      BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val columns =
          when {
            maxWidth >= 520.dp && fontScale <= 1.25f -> 4
            maxWidth >= 240.dp -> 2
            else -> 1
          }
        val rows = remember(metrics, columns) { metrics.chunked(columns) }

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          rows.forEach { rowItems ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              rowItems.forEach { spec ->
                AttendanceMetricTile(
                  spec = spec,
                  compactHorizontalLayout = columns <= 2,
                  onClick = onMetricClick?.let { callback -> { callback(spec.type) } },
                  modifier = Modifier.weight(1f),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AttendanceMetricTile(
  spec: AttendanceMetricSpec,
  compactHorizontalLayout: Boolean,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val border = BorderStroke(0.75.dp, spec.accentColor.copy(alpha = 0.22f))
  val shape = RoundedCornerShape(16.dp)
  val semanticModifier =
    modifier
      .semantics(mergeDescendants = true) {
        contentDescription = "${spec.label}: ${spec.count}"
      }
      .testTag(spec.testTag)

  if (onClick != null) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by
      animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "metricTileScale",
      )
    Surface(
      onClick = onClick,
      interactionSource = interactionSource,
      color = spec.containerColor,
      shape = shape,
      border = border,
      modifier =
        semanticModifier
          .minimumInteractiveComponentSize()
          .graphicsLayer(scaleX = scale, scaleY = scale),
    ) {
      AttendanceMetricTileBody(spec = spec, compactHorizontalLayout = compactHorizontalLayout)
    }
  } else {
    Surface(
      color = spec.containerColor,
      shape = shape,
      border = border,
      modifier = semanticModifier,
    ) {
      AttendanceMetricTileBody(spec = spec, compactHorizontalLayout = compactHorizontalLayout)
    }
  }
}

@Composable
private fun AttendanceMetricTileBody(
  spec: AttendanceMetricSpec,
  compactHorizontalLayout: Boolean,
) {
  if (compactHorizontalLayout) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Surface(
        color = spec.iconBadgeColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.size(38.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = spec.accentColor,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(1.dp),
      ) {
        Text(
          text = spec.count.toString(),
          style = MaterialTheme.typography.titleLarge,
          fontFamily = MonoFontFamily,
          fontWeight = FontWeight.Bold,
          color = spec.accentColor,
          maxLines = 1,
        )
        Text(
          text = spec.label,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = spec.accentColor.copy(alpha = 0.90f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  } else {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Surface(
        color = spec.iconBadgeColor,
        shape = RoundedCornerShape(11.dp),
        modifier = Modifier.size(36.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = spec.icon,
            contentDescription = null,
            tint = spec.accentColor,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Text(
        text = spec.count.toString(),
        style = MaterialTheme.typography.titleLarge,
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Bold,
        color = spec.accentColor,
        maxLines = 1,
      )
      Text(
        text = spec.label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = spec.accentColor.copy(alpha = 0.90f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun AttendanceSummaryCardCompactPreview() {
  OfficeZenTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      AttendanceSummaryCard(
        totalStaff = 24,
        presentCount = 19,
        absentCount = 5,
        lateCount = 2,
        dateLabel = "Tuesday, 24 Nov 2026",
        inviteCodeBadge = "CODE ZEN42",
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 640)
@Composable
private fun AttendanceSummaryCardExpandedPreview() {
  OfficeZenTheme {
    Box(modifier = Modifier.padding(16.dp)) {
      AttendanceSummaryCard(
        totalStaff = 24,
        presentCount = 19,
        absentCount = 5,
        lateCount = 2,
        dateLabel = "Tuesday, 24 Nov 2026",
        inviteCodeBadge = "CODE ZEN42",
      )
    }
  }
}
