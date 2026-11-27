package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.localization.ThemeMode

private val LightColorScheme =
  lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = Marigold,
    onSecondary = Color.White,
    secondaryContainer = MarigoldSoft,
    onSecondaryContainer = MarigoldInk,
    tertiary = TeamCyan,
    onTertiary = Color.White,
    tertiaryContainer = TeamCyanBg,
    onTertiaryContainer = EmeraldNight,
    background = WarmAlabaster,
    onBackground = ObsidianInk,
    surface = AlabasterPure,
    onSurface = ObsidianInk,
    surfaceVariant = AlabasterSoft,
    onSurfaceVariant = StoneMuted,
    outline = AlabasterHairline,
    outlineVariant = AlabasterHairline.copy(alpha = 0.65f),
    error = StatusRed,
    onError = Color.White,
    errorContainer = StatusRedBg,
    onErrorContainer = StatusRed,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = EmeraldLightPrimary,
    onPrimary = Color(0xFF042622),
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = MarigoldGlow,
    onSecondary = Color(0xFF2B1702),
    secondaryContainer = Color(0xFF472906),
    onSecondaryContainer = MarigoldSoft,
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF052E2B),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = ObsidianEmerald,
    onBackground = DarkInk,
    surface = ObsidianSurface,
    onSurface = DarkInk,
    surfaceVariant = ObsidianSurfaceElevated,
    onSurfaceVariant = DarkStoneMuted,
    outline = ObsidianHairline,
    outlineVariant = ObsidianHairline.copy(alpha = 0.7f),
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2),
  )

val OfficeZenShapes =
  Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
  )

@Composable
fun OfficeZenTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = OfficeZenShapes,
    content = content,
  )
}

@Composable
fun OfficeZenTheme(
  themeMode: ThemeMode,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val dark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }
  OfficeZenTheme(
    darkTheme = dark,
    dynamicColor = dynamicColor,
    content = content,
  )
}
