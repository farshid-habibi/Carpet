package com.farsh.carpetmapreader.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = RawSienna,
    secondary = LightAmber,
    tertiary = SaffronGold,
    background = SlateCarbon,
    surface = DeepLustre,
    onPrimary = Color.Black,
    onBackground = SoftCream,
    onSurface = SoftCream
  )

private val LightColorScheme =
  lightColorScheme(
    primary = ClayRed,
    secondary = Terracotta,
    tertiary = SaffronGold,
    background = SoftCream,
    surface = WarmSand,
    onPrimary = Color.White,
    onBackground = DarkCharcoal,
    onSurface = DarkCharcoal
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
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

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
