// Responsive Android Navigation Compose routes & adaptive navigation shell (BottomBar / Rail / Drawer)
package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.localization.AppStrings
import com.example.ui.components.DockNavItem
import com.example.ui.components.ZenBrandBadge

sealed class OfficeZenRoute(
  val route: String,
  val testTag: String,
) {
  data object Home : OfficeZenRoute("officezen/home", "nav_home")
  data object TeamPay : OfficeZenRoute("officezen/team_pay", "nav_team_pay")
  data object ChaiDocs : OfficeZenRoute("officezen/chai_docs", "nav_chai_docs")
  data object MoreAudit : OfficeZenRoute("officezen/more_audit", "nav_more")
  data object Subscription : OfficeZenRoute("officezen/subscription", "nav_subscription")
}

data class TopLevelDestination(
  val route: OfficeZenRoute,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

enum class NavigationChromeMode {
  BOTTOM_BAR,
  NAVIGATION_RAIL,
  PERMANENT_DRAWER,
}

fun resolveNavigationChromeMode(availableWidthDp: Float): NavigationChromeMode =
  when {
    availableWidthDp >= 840f -> NavigationChromeMode.PERMANENT_DRAWER
    availableWidthDp >= 600f -> NavigationChromeMode.NAVIGATION_RAIL
    else -> NavigationChromeMode.BOTTOM_BAR
  }

fun buildPrimaryDestinations(strings: AppStrings): List<TopLevelDestination> =
  listOf(
    TopLevelDestination(
      route = OfficeZenRoute.Home,
      label = strings.navHome,
      selectedIcon = Icons.Rounded.Home,
      unselectedIcon = Icons.Outlined.Home,
      testTag = "nav_home",
    ),
    TopLevelDestination(
      route = OfficeZenRoute.TeamPay,
      label = strings.navPeoplePay,
      selectedIcon = Icons.Rounded.Groups,
      unselectedIcon = Icons.Outlined.Groups,
      testTag = "nav_team_pay",
    ),
    TopLevelDestination(
      route = OfficeZenRoute.ChaiDocs,
      label = strings.navChaiDocs,
      selectedIcon = Icons.Rounded.Description,
      unselectedIcon = Icons.Outlined.Description,
      testTag = "nav_chai_docs",
    ),
    TopLevelDestination(
      route = OfficeZenRoute.MoreAudit,
      label = strings.navMore,
      selectedIcon = Icons.Rounded.Assessment,
      unselectedIcon = Icons.Outlined.Assessment,
      testTag = "nav_more",
    ),
  )

fun List<TopLevelDestination>.toDockNavItems(): List<DockNavItem> =
  map { dest ->
    DockNavItem(
      label = dest.label,
      icon = dest.selectedIcon,
      unselectedIcon = dest.unselectedIcon,
      testTag = dest.testTag,
    )
  }

@Composable
fun OfficeZenRailAndDrawerShell(
  chromeMode: NavigationChromeMode,
  officeName: String,
  destinations: List<TopLevelDestination>,
  currentRoute: String?,
  showSubscriptionShortcut: Boolean = true,
  onNavigate: (OfficeZenRoute) -> Unit,
  content: @Composable () -> Unit,
) {
  when (chromeMode) {
    NavigationChromeMode.BOTTOM_BAR -> {
      Box(modifier = Modifier.fillMaxSize()) {
        content()
      }
    }

    NavigationChromeMode.NAVIGATION_RAIL -> {
      val railItemColors =
        NavigationRailItemDefaults.colors(
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary,
          indicatorColor = MaterialTheme.colorScheme.primaryContainer,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
          containerColor = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxHeight(),
        ) {
          Spacer(modifier = Modifier.height(8.dp))
          destinations.forEach { dest ->
            val selected = currentRoute == dest.route.route
            NavigationRailItem(
              selected = selected,
              onClick = { onNavigate(dest.route) },
              colors = railItemColors,
              icon = {
                Icon(
                  imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                  contentDescription = dest.label,
                )
              },
              label = {
                Text(
                  text = dest.label,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              modifier = Modifier.testTag(dest.testTag),
            )
          }
          if (showSubscriptionShortcut) {
            Spacer(modifier = Modifier.weight(1f))
            val subSelected = currentRoute == OfficeZenRoute.Subscription.route
            NavigationRailItem(
              selected = subSelected,
              onClick = { onNavigate(OfficeZenRoute.Subscription) },
              colors = railItemColors,
              icon = {
                Icon(
                  imageVector = if (subSelected) Icons.Rounded.WorkspacePremium else Icons.Outlined.WorkspacePremium,
                  contentDescription = "Plans",
                )
              },
              label = { Text("Plans", maxLines = 1) },
              modifier = Modifier.testTag(OfficeZenRoute.Subscription.testTag),
            )
            Spacer(modifier = Modifier.height(8.dp))
          }
        }
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
          content()
        }
      }
    }

    NavigationChromeMode.PERMANENT_DRAWER -> {
      val drawerItemColors =
        NavigationDrawerItemDefaults.colors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
          selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
          unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
          unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      PermanentNavigationDrawer(
        drawerContent = {
          PermanentDrawerSheet(
            drawerContainerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.width(248.dp),
          ) {
            Column(
              modifier = Modifier.fillMaxHeight().padding(horizontal = 14.dp, vertical = 16.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  ZenBrandBadge(size = 36.dp)
                  Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                      text = "OfficeZen",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                      text = officeName.ifBlank { "Atelier Workspace" },
                      style = MaterialTheme.typography.labelMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
              }

              Spacer(modifier = Modifier.height(6.dp))

              destinations.forEach { dest ->
                val selected = currentRoute == dest.route.route
                NavigationDrawerItem(
                  selected = selected,
                  onClick = { onNavigate(dest.route) },
                  colors = drawerItemColors,
                  shape = RoundedCornerShape(14.dp),
                  icon = {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = dest.label,
                      modifier = Modifier.size(20.dp),
                    )
                  },
                  label = {
                    Text(
                      text = dest.label,
                      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  },
                  modifier = Modifier.testTag(dest.testTag),
                )
              }

              if (showSubscriptionShortcut) {
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(4.dp))
                val subSelected = currentRoute == OfficeZenRoute.Subscription.route
                NavigationDrawerItem(
                  selected = subSelected,
                  onClick = { onNavigate(OfficeZenRoute.Subscription) },
                  colors = drawerItemColors,
                  shape = RoundedCornerShape(14.dp),
                  icon = {
                    Icon(
                      imageVector = if (subSelected) Icons.Rounded.WorkspacePremium else Icons.Outlined.WorkspacePremium,
                      contentDescription = "Subscription & Plans",
                      modifier = Modifier.size(20.dp),
                    )
                  },
                  label = {
                    Text(
                      text = "Subscription & Plans",
                      fontWeight = if (subSelected) FontWeight.Bold else FontWeight.Medium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  },
                  modifier = Modifier.testTag(OfficeZenRoute.Subscription.testTag),
                )
              }
            }
          }
        },
      ) {
        Box(modifier = Modifier.fillMaxSize()) {
          content()
        }
      }
    }
  }
}
