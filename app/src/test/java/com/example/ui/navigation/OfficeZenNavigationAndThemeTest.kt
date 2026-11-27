// Robolectric Compose UI & unit tests for responsive navigation chrome, Emerald Atelier Light/Dark theme, and brand components
package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.localization.AppLocalization
import com.example.localization.ThemeMode
import com.example.ui.components.FloatingPillBottomBar
import com.example.ui.components.ZenBrandBadge
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldLightPrimary
import com.example.ui.theme.ObsidianEmerald
import com.example.ui.theme.OfficeZenTheme
import com.example.ui.theme.WarmAlabaster
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OfficeZenNavigationAndThemeTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun resolveNavigationChromeMode_matchesCompactMediumExpandedBreakpoints() {
    assertEquals(NavigationChromeMode.BOTTOM_BAR, resolveNavigationChromeMode(360f))
    assertEquals(NavigationChromeMode.BOTTOM_BAR, resolveNavigationChromeMode(599f))
    assertEquals(NavigationChromeMode.NAVIGATION_RAIL, resolveNavigationChromeMode(600f))
    assertEquals(NavigationChromeMode.NAVIGATION_RAIL, resolveNavigationChromeMode(839f))
    assertEquals(NavigationChromeMode.PERMANENT_DRAWER, resolveNavigationChromeMode(840f))
    assertEquals(NavigationChromeMode.PERMANENT_DRAWER, resolveNavigationChromeMode(1280f))
  }

  @Test
  fun floatingPillBottomBar_rendersPrimaryDestinationsAndHandlesSelection() {
    val strings = AppLocalization.forLanguage("en")
    val destinations = buildPrimaryDestinations(strings)
    val selectedIndices = mutableListOf<Int>()

    composeRule.setContent {
      OfficeZenTheme(themeMode = ThemeMode.LIGHT) {
        FloatingPillBottomBar(
          items = destinations.toDockNavItems(),
          selectedIndex = 0,
          onSelect = { selectedIndices.add(it) },
        )
      }
    }

    composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
    composeRule.onNodeWithTag("nav_team_pay").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("nav_chai_docs").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("nav_more").assertIsDisplayed()

    assertEquals(listOf(1, 2), selectedIndices)
  }

  @Test
  @Config(sdk = [36], qualifiers = "w900dp-h700dp")
  fun navigationShell_expandedWidth_rendersPermanentDrawerWithSubscriptionShortcut() {
    val strings = AppLocalization.forLanguage("en")
    val destinations = buildPrimaryDestinations(strings)
    val navigatedRoutes = mutableListOf<OfficeZenRoute>()

    composeRule.setContent {
      OfficeZenTheme(themeMode = ThemeMode.DARK) {
        OfficeZenRailAndDrawerShell(
          chromeMode = NavigationChromeMode.PERMANENT_DRAWER,
          officeName = "Atelier HQ",
          destinations = destinations,
          currentRoute = OfficeZenRoute.Home.route,
          showSubscriptionShortcut = true,
          onNavigate = { navigatedRoutes.add(it) },
        ) {
          Box(modifier = Modifier.fillMaxSize()) {
            Text("Workspace Body")
          }
        }
      }
    }

    composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
    composeRule.onNodeWithTag("nav_team_pay").assertIsDisplayed()
    composeRule.onNodeWithTag("nav_subscription").assertIsDisplayed().performClick()

    assertEquals(listOf(OfficeZenRoute.Subscription), navigatedRoutes)
  }

  @Test
  fun officeZenTheme_lightAndDarkModesApplyEmeraldAtelierAndObsidianPalettes() {
    var lightPrimary = androidx.compose.ui.graphics.Color.Unspecified
    var lightBg = androidx.compose.ui.graphics.Color.Unspecified
    var darkPrimary = androidx.compose.ui.graphics.Color.Unspecified
    var darkBg = androidx.compose.ui.graphics.Color.Unspecified

    composeRule.setContent {
      OfficeZenTheme(themeMode = ThemeMode.LIGHT) {
        lightPrimary = MaterialTheme.colorScheme.primary
        lightBg = MaterialTheme.colorScheme.background
        ZenBrandBadge()
      }
      OfficeZenTheme(themeMode = ThemeMode.DARK) {
        darkPrimary = MaterialTheme.colorScheme.primary
        darkBg = MaterialTheme.colorScheme.background
      }
    }

    assertEquals(EmeraldDeep, lightPrimary)
    assertEquals(WarmAlabaster, lightBg)
    assertEquals(EmeraldLightPrimary, darkPrimary)
    assertEquals(ObsidianEmerald, darkBg)
  }
}
