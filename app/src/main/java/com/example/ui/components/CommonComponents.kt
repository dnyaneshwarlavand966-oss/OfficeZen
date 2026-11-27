// OfficeZen corporate Blue & White Material 3 components, header, bottom navigation & CameraX viewfinder
package com.example.ui.components

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Login
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Apartment
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.MemberRole
import com.example.hardware.LocationAndImageHelper
import com.example.localization.GlobalFormatter
import com.example.localization.IndianLanguages
import com.example.localization.LocalAppStrings
import com.example.localization.LocalGlobalLocaleConfig
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldNight
import com.example.ui.theme.EmeraldSoftBg
import com.example.ui.theme.HeroGradientEnd
import com.example.ui.theme.HeroGradientMid
import com.example.ui.theme.HeroGradientStart
import com.example.ui.theme.LimeSpark
import com.example.ui.theme.Marigold
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusAmberBg
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusGreenBg

@Composable
fun ZenAmbientBackground(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  val baseColor = MaterialTheme.colorScheme.background
  val emeraldTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.055f)
  val marigoldTint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.03f)

  Box(
    modifier =
      modifier
        .fillMaxSize()
        .background(baseColor)
        .background(
          Brush.verticalGradient(
            colors = listOf(emeraldTint, marigoldTint, Color.Transparent),
          )
        ),
    content = content,
  )
}

@Composable
fun ZenBrandBadge(modifier: Modifier = Modifier, size: Dp = 40.dp) {
  Surface(
    color = Color.White,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, Marigold.copy(alpha = 0.35f)),
    shadowElevation = 3.dp,
    modifier = modifier.size(size),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = Icons.Rounded.Apartment,
        contentDescription = "OfficeZen Logo",
        tint = EmeraldDeep,
        modifier = Modifier.size(size * 0.58f),
      )
      Box(
        modifier =
          Modifier
            .align(Alignment.TopEnd)
            .padding(4.dp)
            .size((size * 0.16f).coerceAtLeast(5.dp))
            .clip(CircleShape)
            .background(Marigold),
      )
    }
  }
}

@Composable
fun ZenCard(
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.85f),
  cornerRadius: Dp = 18.dp,
  elevation: Dp = 2.dp,
  onClick: (() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  val shape = RoundedCornerShape(cornerRadius)
  val border = BorderStroke(0.75.dp, borderColor)
  val colors = CardDefaults.cardColors(containerColor = containerColor)
  val elev = CardDefaults.cardElevation(defaultElevation = elevation)

  if (onClick != null) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by
      animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "cardPressScale",
      )
    Card(
      onClick = onClick,
      interactionSource = interactionSource,
      modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
      shape = shape,
      colors = colors,
      border = border,
      elevation = elev,
      content = content,
    )
  } else {
    Card(
      modifier = modifier,
      shape = shape,
      colors = colors,
      border = border,
      elevation = elev,
      content = content,
    )
  }
}

data class SegmentedTabItem(
  val label: String,
  val testTag: String,
)

@Composable
fun ZenSegmentedTabs(
  items: List<SegmentedTabItem>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(4.dp).fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      items.forEachIndexed { index, item ->
        val selected = index == selectedIndex
        val bg by
          animateColorAsState(
            targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "tabBg",
          )
        val fg by
          animateColorAsState(
            targetValue =
              if (selected) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "tabFg",
          )
        Surface(
          onClick = { onSelect(index) },
          color = bg,
          shape = RoundedCornerShape(10.dp),
          shadowElevation = if (selected) 2.dp else 0.dp,
          modifier =
            Modifier
              .weight(1f)
              .minimumInteractiveComponentSize()
              .testTag(item.testTag),
        ) {
          Box(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = item.label,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = fg,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
fun OfficeTopBar(
  officeName: String,
  role: MemberRole?,
  selectedLanguageCode: String,
  onOpenLanguagePicker: () -> Unit,
  onSwitchWorkspace: (() -> Unit)? = null,
  userDisplayName: String = "",
  userDesignation: String = "",
  userSelfieBitmap: Bitmap? = null,
  showProfileStrip: Boolean = false,
  isNetworkOnline: Boolean = false,
  onProfileClick: (() -> Unit)? = null,
) {
  val strings = LocalAppStrings.current
  val currentLang = remember(selectedLanguageCode) { IndianLanguages.findByCode(selectedLanguageCode) }
  val headerGradient =
    Brush.linearGradient(
      colors = listOf(HeroGradientStart, HeroGradientMid, HeroGradientEnd),
    )

  Surface(
    color = Color.Transparent,
    shadowElevation = 4.dp,
    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(
      modifier =
        Modifier
          .fillMaxWidth()
          .background(headerGradient)
          .statusBarsPadding()
          .padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = if (showProfileStrip) 16.dp else 14.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Top Brand Row: OfficeZen Logo + Language + Workspace Switch
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp),
          ) {
            ZenBrandBadge(size = 40.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
              Text(
                text = strings.appName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (officeName.isNotBlank() && officeName != strings.appName) {
                Text(
                  text = officeName,
                  style = MaterialTheme.typography.bodyMedium,
                  color = Color.White.copy(alpha = 0.85f),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Surface(
              onClick = onOpenLanguagePicker,
              shape = RoundedCornerShape(50),
              color = Color.White.copy(alpha = 0.16f),
              border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.35f)),
              modifier = Modifier.minimumInteractiveComponentSize().testTag("top_bar_language_button"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  Icons.Rounded.Language,
                  contentDescription = strings.language,
                  tint = Color.White,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = currentLang.nativeName,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = Color.White,
                  maxLines = 1,
                )
              }
            }
            if (onSwitchWorkspace != null) {
              Surface(
                onClick = onSwitchWorkspace,
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.16f),
                border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.35f)),
                modifier = Modifier.size(44.dp).minimumInteractiveComponentSize().testTag("top_bar_switch_office_button"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    Icons.Rounded.SwapHoriz,
                    contentDescription = strings.signOut,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }
        }

        // User Profile + Online/Device Status Bar inside the Blue Header
        if (showProfileStrip) {
          val resolvedName = userDisplayName.ifBlank { "Office Owner" }
          val roleLabel =
            when (role) {
              MemberRole.OWNER -> strings.roleOwner
              MemberRole.MANAGER -> strings.roleManager
              MemberRole.EMPLOYEE -> strings.roleEmployee
              null -> strings.roleOwner
            }
          val subtitle =
            if (userDesignation.isNotBlank()) "$userDesignation · $roleLabel"
            else roleLabel

          HeaderUserProfileSection(
            displayName = resolvedName,
            subtitle = subtitle,
            statusText = if (isNetworkOnline) "Online" else strings.offlineReady,
            profilePhotoBitmap = userSelfieBitmap,
            onClick = onProfileClick,
          )
        }
      }
    }
  }
}

fun computeAvatarInitials(displayName: String): String {
  val parts = displayName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
  if (parts.isEmpty()) return "OZ"
  if (parts.size == 1) {
    return parts.first().take(2).uppercase()
  }
  val first = parts.first().firstOrNull()?.uppercaseChar() ?: 'O'
  val last = parts.last().firstOrNull()?.uppercaseChar() ?: 'Z'
  return "$first$last"
}

@Composable
fun UserProfileAvatar(
  displayName: String,
  profilePhotoBitmap: Bitmap?,
  modifier: Modifier = Modifier,
  size: Dp = 44.dp,
  showOnlineIndicator: Boolean = true,
  avatarContainerColor: Color = Color.White,
  initialsColor: Color = EmeraldDeep,
  borderColor: Color = Color.White,
) {
  val initials = remember(displayName) { computeAvatarInitials(displayName) }

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center,
  ) {
    if (profilePhotoBitmap != null) {
      Image(
        bitmap = profilePhotoBitmap.asImageBitmap(),
        contentDescription = "$displayName profile photo",
        contentScale = ContentScale.Crop,
        modifier =
          Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .border(1.75.dp, borderColor, CircleShape)
            .testTag("user_profile_photo"),
      )
    } else {
      Surface(
        color = avatarContainerColor,
        shape = CircleShape,
        border = BorderStroke(1.5.dp, borderColor.copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxSize().testTag("user_profile_initials_avatar"),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text(
            text = initials,
            style =
              if (size >= 60.dp) MaterialTheme.typography.headlineSmall
              else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = initialsColor,
            maxLines = 1,
          )
        }
      }
    }

    if (showOnlineIndicator) {
      Box(
        modifier =
          Modifier
            .align(Alignment.BottomEnd)
            .size((size * 0.28f).coerceIn(10.dp, 16.dp))
            .clip(CircleShape)
            .background(Color(0xFF10B981))
            .border(1.75.dp, EmeraldNight, CircleShape),
      )
    }
  }
}

@Composable
fun HeaderUserProfileSection(
  displayName: String,
  subtitle: String,
  statusText: String,
  profilePhotoBitmap: Bitmap?,
  onClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = { onClick?.invoke() },
    enabled = onClick != null,
    color = Color.White.copy(alpha = 0.14f),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.26f)),
    modifier =
      modifier
        .fillMaxWidth()
        .minimumInteractiveComponentSize()
        .testTag("header_user_profile_section"),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      UserProfileAvatar(
        displayName = displayName,
        profilePhotoBitmap = profilePhotoBitmap,
        size = 44.dp,
        showOnlineIndicator = true,
      )

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
          text = displayName,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = Color.White.copy(alpha = 0.84f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      val pulseTransition = rememberInfiniteTransition(label = "statusPulse")
      val pulseAlpha by
        pulseTransition.animateFloat(
          initialValue = 0.55f,
          targetValue = 1.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1200),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "statusDotAlpha",
        )
      Surface(
        color = Color(0xFF064E3B).copy(alpha = 0.72f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.75.dp, Color(0xFF34D399).copy(alpha = 0.55f)),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          Box(
            modifier =
              Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(Color(0xFF34D399).copy(alpha = pulseAlpha)),
          )
          Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
fun ProfileAvatarDialog(
  displayName: String,
  subtitle: String,
  officeName: String,
  profilePhotoBitmap: Bitmap?,
  onPickPhoto: () -> Unit,
  onRemovePhoto: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    title = {
      Text("User Profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        UserProfileAvatar(
          displayName = displayName,
          profilePhotoBitmap = profilePhotoBitmap,
          size = 84.dp,
          showOnlineIndicator = true,
          avatarContainerColor = MaterialTheme.colorScheme.primaryContainer,
          initialsColor = MaterialTheme.colorScheme.primary,
          borderColor = MaterialTheme.colorScheme.primary,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = displayName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          if (officeName.isNotBlank()) {
            Text(
              text = officeName,
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Button(
          onClick = onPickPhoto,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pick_profile_photo_button"),
        ) {
          Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(8.dp))
          Text(if (profilePhotoBitmap != null) "Change Profile Photo" else "Choose Profile Photo")
        }
        if (profilePhotoBitmap != null) {
          OutlinedButton(
            onClick = onRemovePhoto,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("remove_profile_photo_button"),
          ) {
            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Use Initials Avatar (${computeAvatarInitials(displayName)})")
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

data class DockNavItem(
  val label: String,
  val icon: ImageVector,
  val testTag: String,
  val unselectedIcon: ImageVector? = null,
)

@Composable
fun FloatingPillBottomBar(
  items: List<DockNavItem>,
  selectedIndex: Int,
  onSelect: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.75f), thickness = 0.75.dp)
    NavigationBar(
      containerColor = MaterialTheme.colorScheme.surface,
      tonalElevation = 0.dp,
      windowInsets = WindowInsets.navigationBars,
    ) {
      items.forEachIndexed { idx, item ->
        val selected = idx == selectedIndex
        val iconScale by
          animateFloatAsState(
            targetValue = if (selected) 1.08f else 1.0f,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
            label = "navIconScale",
          )
        NavigationBarItem(
          selected = selected,
          onClick = { onSelect(idx) },
          icon = {
            Icon(
              imageVector = if (selected) item.icon else (item.unselectedIcon ?: item.icon),
              contentDescription = item.label,
              modifier = Modifier.size(22.dp).graphicsLayer(scaleX = iconScale, scaleY = iconScale),
            )
          },
          label = {
            Text(
              text = item.label,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          },
          colors =
            NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
          modifier = Modifier.testTag(item.testTag),
        )
      }
    }
  }
}

@Composable
fun LanguagePickerDialog(
  selectedCode: String,
  onSelect: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val strings = LocalAppStrings.current
  var query by remember { mutableStateOf("") }
  val filtered =
    remember(query) {
      if (query.isBlank()) IndianLanguages.all
      else {
        val q = query.trim().lowercase()
        IndianLanguages.all.filter {
          it.englishName.lowercase().contains(q) ||
            it.nativeName.lowercase().contains(q) ||
            it.code.lowercase().contains(q)
        }
      }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(26.dp),
    title = {
      Text(
        "${strings.language} (22 Indian + English)",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
        OutlinedTextField(
          value = query,
          onValueChange = { query = it },
          placeholder = { Text("Search Hindi, Marathi, Tamil...") },
          leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          items(filtered, key = { it.code }) { lang ->
            val isSelected = lang.code == selectedCode
            Surface(
              color =
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(14.dp),
              border =
                if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
              modifier =
                Modifier.fillMaxWidth()
                  .clip(RoundedCornerShape(14.dp))
                  .clickable {
                    onSelect(lang.code)
                    onDismiss()
                  }
                  .testTag("lang_option_${lang.code}"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Column {
                  Text(
                    text = lang.nativeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                  )
                  Text(
                    text = "${lang.englishName}${if (lang.isRtl) " · RTL" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                if (isSelected) {
                  Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text("Close") }
    },
  )
}

@Composable
fun CameraCaptureDialog(
  title: String,
  useFrontCamera: Boolean,
  onCaptured: (Bitmap) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  var captureError by remember { mutableStateOf<String?>(null) }
  var isCapturing by remember { mutableStateOf(false) }

  val previewView = remember {
    PreviewView(context).apply {
      layoutParams =
        ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT,
        )
      scaleType = PreviewView.ScaleType.FILL_CENTER
    }
  }
  val imageCapture = remember {
    ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
  }

  DisposableEffect(useFrontCamera) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val executor = ContextCompat.getMainExecutor(context)
    var cameraProvider: ProcessCameraProvider? = null
    cameraProviderFuture.addListener(
      {
        try {
          cameraProvider = cameraProviderFuture.get()
          val preview =
            Preview.Builder().build().also {
              it.surfaceProvider = previewView.surfaceProvider
            }
          val selector =
            if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA
            else CameraSelector.DEFAULT_BACK_CAMERA
          cameraProvider?.unbindAll()
          cameraProvider?.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        } catch (e: Exception) {
          captureError = e.localizedMessage ?: "Camera unavailable on this device"
        }
      },
      executor,
    )
    onDispose {
      cameraProvider?.unbindAll()
    }
  }

  Dialog(onDismissRequest = onDismiss) {
    Surface(
      shape = RoundedCornerShape(28.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(title, style = MaterialTheme.typography.titleLarge)
          Surface(
            onClick = onDismiss,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(40.dp).minimumInteractiveComponentSize(),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(Icons.Rounded.Close, contentDescription = "Close camera", modifier = Modifier.size(18.dp))
            }
          }
        }

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(310.dp)
              .clip(RoundedCornerShape(20.dp))
              .background(Color(0xFF0A1413)),
          contentAlignment = Alignment.Center,
        ) {
          AndroidView(factory = { previewView }, modifier = Modifier.matchParentSize())
          Canvas(modifier = Modifier.matchParentSize().padding(20.dp)) {
            val bracket = 28.dp.toPx()
            val stroke = 3.dp.toPx()
            val c = LimeSpark.copy(alpha = 0.88f)
            drawLine(c, Offset(0f, 0f), Offset(bracket, 0f), stroke, StrokeCap.Round)
            drawLine(c, Offset(0f, 0f), Offset(0f, bracket), stroke, StrokeCap.Round)
            drawLine(c, Offset(size.width, 0f), Offset(size.width - bracket, 0f), stroke, StrokeCap.Round)
            drawLine(c, Offset(size.width, 0f), Offset(size.width, bracket), stroke, StrokeCap.Round)
            drawLine(c, Offset(0f, size.height), Offset(bracket, size.height), stroke, StrokeCap.Round)
            drawLine(c, Offset(0f, size.height), Offset(0f, size.height - bracket), stroke, StrokeCap.Round)
            drawLine(c, Offset(size.width, size.height), Offset(size.width - bracket, size.height), stroke, StrokeCap.Round)
            drawLine(c, Offset(size.width, size.height), Offset(size.width, size.height - bracket), stroke, StrokeCap.Round)
          }
          if (captureError != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.padding(16.dp),
            ) {
              Text(
                text = captureError!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(14.dp),
              )
            }
          }
        }

        Button(
          onClick = {
            isCapturing = true
            imageCapture.takePicture(
              ContextCompat.getMainExecutor(context),
              object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                  val bmp = image.toBitmap()
                  val rotated =
                    LocationAndImageHelper.rotateBitmap(
                      bmp,
                      image.imageInfo.rotationDegrees.toFloat(),
                    )
                  image.close()
                  isCapturing = false
                  onCaptured(rotated)
                }

                override fun onError(exception: ImageCaptureException) {
                  isCapturing = false
                  captureError = exception.localizedMessage ?: "Capture failed"
                }
              },
            )
          },
          enabled = !isCapturing && captureError == null,
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("camera_capture_button"),
          shape = RoundedCornerShape(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
          Icon(Icons.Rounded.CameraAlt, contentDescription = null)
          Spacer(Modifier.width(8.dp))
          Text(if (isCapturing) "Capturing..." else "Capture Photo", style = MaterialTheme.typography.titleMedium)
        }
      }
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  subtitle: String? = null,
  trailingBadge: String? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Bottom,
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
      )
      if (!subtitle.isNullOrBlank()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    if (!trailingBadge.isNullOrBlank()) {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
      ) {
        Text(
          text = trailingBadge,
          style = MaterialTheme.typography.labelMedium,
          fontFamily = MonoFontFamily,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }
  }
}

@Composable
fun StatusPill(
  text: String,
  containerColor: Color = StatusGreenBg,
  contentColor: Color = StatusGreen,
  useMonospace: Boolean = false,
) {
  Surface(
    color = containerColor,
    shape = RoundedCornerShape(50),
    border = BorderStroke(0.5.dp, contentColor.copy(alpha = 0.22f)),
  ) {
    Text(
      text = text,
      color = contentColor,
      style = MaterialTheme.typography.labelMedium,
      fontFamily = if (useMonospace) MonoFontFamily else null,
      fontWeight = FontWeight.SemiBold,
      maxLines = 1,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
    )
  }
}

@Composable
fun EmptyPlaceholderCard(title: String, body: String) {
  ZenCard(
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    cornerRadius = 20.dp,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.padding(24.dp).fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CircleShape,
        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.size(44.dp),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            Icons.Rounded.Workspaces,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
      )
      Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
fun CloudAttendanceSyncCard(
  pendingCount: Int,
  syncedCount: Int,
  isOnline: Boolean,
  isSyncRunning: Boolean,
  lastSyncEpochMs: Long,
  lastSyncMessage: String,
  googleUserEmail: String?,
  isAuthLoading: Boolean,
  onSignInWithGoogle: () -> Unit,
  onSignOutGoogle: () -> Unit,
  onTriggerSyncNow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val localeConfig = LocalGlobalLocaleConfig.current
  val statusBg =
    when {
      isSyncRunning -> EmeraldSoftBg
      pendingCount > 0 -> StatusAmberBg
      else -> StatusGreenBg
    }
  val statusFg =
    when {
      isSyncRunning -> EmeraldDeep
      pendingCount > 0 -> StatusAmber
      else -> StatusGreen
    }
  val statusIcon =
    when {
      !isOnline -> Icons.Rounded.CloudOff
      isSyncRunning || pendingCount > 0 -> Icons.Rounded.CloudSync
      else -> Icons.Rounded.CloudDone
    }

  ZenCard(
    cornerRadius = 18.dp,
    modifier = modifier.fillMaxWidth().testTag("cloud_attendance_sync_card"),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Surface(
            color = statusBg,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = statusIcon,
                contentDescription = "Attendance Cloud Sync",
                tint = statusFg,
                modifier = Modifier.size(22.dp),
              )
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = "Firestore Attendance Sync",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
              text =
                if (isOnline) "Network connected · Auto-upload enabled"
                else "Offline · Auto-uploads when connected",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        StatusPill(
          text = if (isOnline) "ONLINE" else "OFFLINE",
          containerColor = if (isOnline) StatusGreenBg else StatusAmberBg,
          contentColor = if (isOnline) StatusGreen else StatusAmber,
          useMonospace = true,
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StatusPill(
          text = "$pendingCount Pending",
          containerColor = if (pendingCount > 0) StatusAmberBg else MaterialTheme.colorScheme.surfaceVariant,
          contentColor = if (pendingCount > 0) StatusAmber else MaterialTheme.colorScheme.onSurfaceVariant,
          useMonospace = true,
        )
        StatusPill(
          text = "$syncedCount Synced",
          containerColor = StatusGreenBg,
          contentColor = StatusGreen,
          useMonospace = true,
        )
        if (lastSyncEpochMs > 0L) {
          val formattedTime =
            remember(lastSyncEpochMs, localeConfig) {
              GlobalFormatter.formatTime(lastSyncEpochMs, localeConfig)
            }
          StatusPill(
            text = "Last $formattedTime",
            containerColor = EmeraldSoftBg,
            contentColor = EmeraldDeep,
            useMonospace = true,
          )
        }
      }

      if (lastSyncMessage.isNotBlank()) {
        Text(
          text = lastSyncMessage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (googleUserEmail.isNullOrBlank()) {
        Button(
          onClick = onSignInWithGoogle,
          enabled = !isAuthLoading,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("google_sign_in_button"),
        ) {
          if (isAuthLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = MaterialTheme.colorScheme.onPrimary,
              strokeWidth = 2.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text("Signing in...")
          } else {
            Icon(Icons.AutoMirrored.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sign in with Google", fontWeight = FontWeight.SemiBold)
          }
        }
      } else {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Button(
            onClick = onTriggerSyncNow,
            enabled = !isSyncRunning,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f).height(48.dp).testTag("sync_pending_attendance_button"),
          ) {
            Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = if (isSyncRunning) "Syncing..." else "Sync Now",
              fontWeight = FontWeight.SemiBold,
            )
          }
          OutlinedButton(
            onClick = onSignOutGoogle,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.height(48.dp).testTag("google_sign_out_button"),
          ) {
            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign out Google", modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Sign Out")
          }
        }
        Text(
          text = "Google Account: $googleUserEmail",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
