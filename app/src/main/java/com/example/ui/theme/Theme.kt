package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme =
  lightColorScheme(
    primary = ZenTeal,
    onPrimary = Color.White,
    primaryContainer = SoftSage,
    onPrimaryContainer = ZenTealDark,
    secondary = SaffronAmber,
    onSecondary = Color.White,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = ZenTealLight,
    onTertiary = Color.White,
    background = WarmIvory,
    onBackground = CharcoalInk,
    surface = CardWhite,
    onSurface = CharcoalInk,
    surfaceVariant = SurfaceVariantWarm,
    onSurfaceVariant = MutedSlate,
    outline = OutlineWarm,
    error = StatusRed,
    onError = Color.White,
    errorContainer = StatusRedBg,
    onErrorContainer = StatusRed,
  )

private val DarkColorScheme =
  darkColorScheme(
    primary = DarkTealPrimary,
    onPrimary = ZenTealDark,
    primaryContainer = DarkTealContainer,
    onPrimaryContainer = SoftSage,
    secondary = SaffronAmber,
    onSecondary = Color.White,
    secondaryContainer = OnSaffronContainer,
    onSecondaryContainer = SaffronContainer,
    background = DarkBackground,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMutedSlate,
    outline = DarkSurfaceVariant,
    error = Color(0xFFFCA5A5),
    onError = StatusRed,
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFEE2E2),
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
    content = content,
  )
}
