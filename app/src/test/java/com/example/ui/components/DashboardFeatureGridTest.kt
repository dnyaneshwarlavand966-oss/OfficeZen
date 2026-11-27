// OfficeZen Compose Robolectric UI tests for responsive DashboardFeatureGrid
package com.example.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.localization.AppLocalization
import com.example.ui.theme.BluePrimary
import com.example.ui.theme.BlueSoftBg
import com.example.ui.theme.ChaiCream
import com.example.ui.theme.ChaiTerracotta
import com.example.ui.theme.LeaveRose
import com.example.ui.theme.LeaveRoseBg
import com.example.ui.theme.LedgerIndigo
import com.example.ui.theme.LedgerIndigoBg
import com.example.ui.theme.OfficeZenTheme
import com.example.ui.theme.ReportViolet
import com.example.ui.theme.ReportVioletBg
import com.example.ui.theme.SettingsSlate
import com.example.ui.theme.SettingsSlateBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg
import com.example.ui.theme.TeamCyan
import com.example.ui.theme.TeamCyanBg
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DashboardFeatureGridTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  @Config(sdk = [36], qualifiers = "w400dp-h800dp")
  fun featureGrid_compactPhone_placesTwoColumnsAndHandlesCardClicks() {
    val clicked = mutableListOf<String>()
    val items = buildSampleItems(langCode = "en") { clicked.add(it) }

    composeRule.setContent {
      OfficeZenTheme {
        Box(modifier = Modifier.width(360.dp)) {
          DashboardFeatureGrid(
            items = items,
            sectionTitle = "Office Modules",
            sectionSubtitle = "All core office operations",
          )
        }
      }
    }

    composeRule.onNodeWithTag("dashboard_feature_grid").assertIsDisplayed()
    composeRule.onNodeWithTag("module_attendance_card").assertIsDisplayed().assertTextContains("Attendance", substring = true)
    composeRule.onNodeWithTag("home_quick_docs_button").assertIsDisplayed().assertTextContains("Documents", substring = true)
    composeRule.onNodeWithTag("module_tea_coffee_card").assertIsDisplayed().assertTextContains("Tea & Coffee", substring = true)

    val attendanceBounds = composeRule.onNodeWithTag("module_attendance_card").fetchSemanticsNode().boundsInRoot
    val docsBounds = composeRule.onNodeWithTag("home_quick_docs_button").fetchSemanticsNode().boundsInRoot
    val teaBounds = composeRule.onNodeWithTag("module_tea_coffee_card").fetchSemanticsNode().boundsInRoot

    assertEquals(attendanceBounds.top, docsBounds.top, 2f)
    assertTrue("Expected Tea & Coffee card on row 2 below Attendance", teaBounds.top > attendanceBounds.bottom)

    composeRule.onNodeWithTag("home_quick_docs_button").performClick()
    composeRule.onNodeWithTag("module_tea_coffee_card").performClick()
    assertEquals(listOf("docs", "tea"), clicked)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w900dp-h700dp")
  fun featureGrid_expandedTablet_placesFourColumnsWithMarathiTitles() {
    val items = buildSampleItems(langCode = "mr") {}

    composeRule.setContent {
      OfficeZenTheme {
        Box(modifier = Modifier.width(800.dp)) {
          DashboardFeatureGrid(
            items = items,
            sectionTitle = "ऑफिस विभाग",
          )
        }
      }
    }

    val attendanceBounds = composeRule.onNodeWithTag("module_attendance_card").fetchSemanticsNode().boundsInRoot
    val payrollBounds = composeRule.onNodeWithTag("module_payroll_card").fetchSemanticsNode().boundsInRoot
    val teamBounds = composeRule.onNodeWithTag("module_team_card").fetchSemanticsNode().boundsInRoot

    assertEquals(attendanceBounds.top, payrollBounds.top, 2f)
    assertTrue("Expected 4th card (Payroll) to the right of 1st card (Attendance)", payrollBounds.left > attendanceBounds.right)
    assertTrue("Expected 5th card (Team) on row 2", teamBounds.top > attendanceBounds.bottom)

    composeRule.onNodeWithTag("home_quick_docs_button").assertTextContains("कागदपत्रे (आवक/जावक)", substring = true)
    composeRule.onNodeWithTag("module_tea_coffee_card").assertTextContains("चहा व कॉफी", substring = true)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w400dp-h800dp")
  fun featureCard_pressInteraction_animatesTactileScaleAndRestoresOnRelease() {
    val clicked = mutableListOf<String>()
    val items = buildSampleItems(langCode = "en") { clicked.add(it) }

    composeRule.mainClock.autoAdvance = false
    composeRule.setContent {
      OfficeZenTheme {
        Box(modifier = Modifier.width(360.dp)) {
          DashboardFeatureGrid(
            items = items,
            sectionTitle = "Office Modules",
          )
        }
      }
    }
    composeRule.mainClock.advanceTimeByFrame()

    val idleBounds = composeRule.onNodeWithTag("module_attendance_card").fetchSemanticsNode().boundsInRoot
    composeRule.onNodeWithTag("module_attendance_card").performTouchInput { down(center) }
    composeRule.mainClock.advanceTimeBy(160)

    val pressedBounds = composeRule.onNodeWithTag("module_attendance_card").fetchSemanticsNode().boundsInRoot
    assertTrue(
      "Expected pressed card width (${pressedBounds.width}) to scale down below idle (${idleBounds.width})",
      pressedBounds.width < idleBounds.width,
    )
    assertTrue(
      "Expected pressed card height (${pressedBounds.height}) to scale down below idle (${idleBounds.height})",
      pressedBounds.height < idleBounds.height,
    )

    composeRule.onNodeWithTag("module_attendance_card").performTouchInput { up() }
    composeRule.mainClock.advanceTimeBy(500)

    val releasedBounds = composeRule.onNodeWithTag("module_attendance_card").fetchSemanticsNode().boundsInRoot
    assertTrue(
      "Expected released card width (${releasedBounds.width}) to return to idle (${idleBounds.width})",
      abs(releasedBounds.width - idleBounds.width) < 1f,
    )
    assertEquals(listOf("attendance"), clicked)
  }

  private fun buildSampleItems(
    langCode: String,
    onSelect: (String) -> Unit,
  ): List<DashboardFeatureItem> {
    val strings = AppLocalization.forLanguage(langCode)
    return listOf(
      DashboardFeatureItem(strings.modAttendance, "15/20 present", Icons.Rounded.VerifiedUser, BluePrimary, BlueSoftBg, "module_attendance_card", "15/20") { onSelect("attendance") },
      DashboardFeatureItem(strings.modDocuments, "12 inward / outward", Icons.Rounded.Description, LedgerIndigo, LedgerIndigoBg, "home_quick_docs_button", "12") { onSelect("docs") },
      DashboardFeatureItem(strings.modTeaCoffee, "18 cups logged", Icons.Rounded.LocalCafe, ChaiTerracotta, ChaiCream, "module_tea_coffee_card", "18") { onSelect("tea") },
      DashboardFeatureItem(strings.modPayroll, "3-late & OT rules", Icons.Rounded.AccountBalanceWallet, StatusGreen, StatusGreenBg, "module_payroll_card") { onSelect("payroll") },
      DashboardFeatureItem(strings.modTeam, "20 members", Icons.Rounded.Groups, TeamCyan, TeamCyanBg, "module_team_card", "20") { onSelect("team") },
      DashboardFeatureItem(strings.modLeave, "2 pending", Icons.AutoMirrored.Rounded.EventNote, LeaveRose, LeaveRoseBg, "home_quick_leave_button", "2") { onSelect("leave") },
      DashboardFeatureItem(strings.modReports, "PDF & CSV export", Icons.Rounded.Assessment, ReportViolet, ReportVioletBg, "module_reports_card") { onSelect("reports") },
      DashboardFeatureItem(strings.modSettings, "Shift & GPS", Icons.Rounded.Settings, SettingsSlate, SettingsSlateBg, "module_settings_card") { onSelect("settings") },
    )
  }
}
