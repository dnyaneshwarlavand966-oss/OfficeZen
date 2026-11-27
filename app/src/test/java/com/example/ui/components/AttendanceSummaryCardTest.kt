// OfficeZen Compose Robolectric UI tests for responsive AttendanceSummaryCard
package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.example.localization.AppLocalization
import com.example.localization.LocalAppStrings
import com.example.ui.theme.OfficeZenTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AttendanceSummaryCardTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun attendanceSummaryCard_compactWidth_displaysAllMetricsInTwoByTwoGridAndHandlesClicks() {
    val clickedMetrics = mutableListOf<AttendanceMetricType>()

    composeRule.setContent {
      OfficeZenTheme {
        Box(modifier = Modifier.width(360.dp)) {
          AttendanceSummaryCard(
            totalStaff = 20,
            presentCount = 15,
            absentCount = 5,
            lateCount = 3,
            dateLabel = "Tuesday, 24 Nov 2026",
            inviteCodeBadge = "CODE ZEN42",
            onMetricClick = { clickedMetrics.add(it) },
          )
        }
      }
    }

    composeRule.onNodeWithTag("today_attendance_summary_card").assertIsDisplayed()
    composeRule.onNodeWithTag("summary_attendance_rate").assertTextContains("75%")
    composeRule.onNodeWithTag("summary_total_staff").assertIsDisplayed().assertTextContains("20", substring = true)
    composeRule.onNodeWithTag("summary_present").assertIsDisplayed().assertTextContains("15", substring = true)
    composeRule.onNodeWithTag("summary_absent").assertIsDisplayed().assertTextContains("5", substring = true)
    composeRule.onNodeWithTag("summary_late").assertIsDisplayed().assertTextContains("3", substring = true)

    val totalBounds = composeRule.onNodeWithTag("summary_total_staff").fetchSemanticsNode().boundsInRoot
    val presentBounds = composeRule.onNodeWithTag("summary_present").fetchSemanticsNode().boundsInRoot
    val absentBounds = composeRule.onNodeWithTag("summary_absent").fetchSemanticsNode().boundsInRoot

    // In 2x2 compact mode, Total Staff & Present share row 1 while Absent drops to row 2
    assertEquals(totalBounds.top, presentBounds.top, 2f)
    assertTrue("Expected Absent tile on second row below Total Staff", absentBounds.top > totalBounds.bottom)

    composeRule.onNodeWithTag("summary_total_staff").performClick()
    composeRule.onNodeWithTag("summary_late").performClick()
    assertEquals(listOf(AttendanceMetricType.TOTAL_STAFF, AttendanceMetricType.LATE), clickedMetrics)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w800dp-h600dp")
  fun attendanceSummaryCard_expandedWidth_placesAllFourTilesInSingleRowWithMarathiLabels() {
    composeRule.setContent {
      OfficeZenTheme {
        CompositionLocalProvider(LocalAppStrings provides AppLocalization.forLanguage("mr")) {
          Box(modifier = Modifier.width(680.dp)) {
            AttendanceSummaryCard(
              totalStaff = 12,
              presentCount = 9,
              absentCount = 3,
              lateCount = 1,
              dateLabel = "मंगळवार, 24 नोव्हेंबर",
            )
          }
        }
      }
    }

    composeRule.onNodeWithTag("summary_attendance_rate").assertTextContains("75%")
    composeRule.onNodeWithTag("summary_total_staff").assertTextContains("एकूण कर्मचारी", substring = true)
    composeRule.onNodeWithTag("summary_present").assertTextContains("हजर", substring = true)
    composeRule.onNodeWithTag("summary_absent").assertTextContains("गैरहजर", substring = true)
    composeRule.onNodeWithTag("summary_late").assertTextContains("उशीर", substring = true)

    val totalBounds = composeRule.onNodeWithTag("summary_total_staff").fetchSemanticsNode().boundsInRoot
    val lateBounds = composeRule.onNodeWithTag("summary_late").fetchSemanticsNode().boundsInRoot
    assertEquals(totalBounds.top, lateBounds.top, 2f)
    assertTrue("Expected Late tile to the right of Total Staff in 4-column row", lateBounds.left > totalBounds.right)
  }
}
