package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

enum class FontScaleLevel(val label: String, val scaleFactor: Float, val icon: String, val description: String) {
    NORMAL("Normal", 1.0f, "🔤", "100% • Tamaño estándar balanceado"),
    GRANDE("Grande", 1.20f, "🔍", "120% • Letra ampliada de fácil lectura"),
    CHICATO("Modo Chicato", 1.40f, "👓", "140% • Tamaño grande y diseño adaptado"),
    ULTRA_GRANDE("Máximo 160%", 1.60f, "🔎", "160% • Visibilidad máxima sin cortes")
}

val LocalFontScaleLevel = staticCompositionLocalOf { FontScaleLevel.NORMAL }
val LocalIsLargeFontMode = staticCompositionLocalOf { false }

private val DarkColorScheme =
  darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    secondary = EmeraldSuccess,
    onSecondary = Color.White,
    tertiary = AmberDelay,
    onTertiary = Color.Black,
    error = RoseError,
    onError = Color.White,
    background = SlateBackground,
    onBackground = TextPrimary,
    surface = SlateSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateCard,
    onSurfaceVariant = TextSecondary,
    outline = SlateCardBorder,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Default to sleek executive dark theme
  dynamicColor: Boolean = false,
  fontScaleLevel: FontScaleLevel = FontScaleLevel.NORMAL,
  content: @Composable () -> Unit,
) {
  val currentDensity = LocalDensity.current
  val targetFontScale = currentDensity.fontScale * fontScaleLevel.scaleFactor
  val isLargeMode = fontScaleLevel.scaleFactor >= 1.35f || targetFontScale >= 1.35f

  CompositionLocalProvider(
    LocalDensity provides Density(
      density = currentDensity.density,
      fontScale = targetFontScale
    ),
    LocalFontScaleLevel provides fontScaleLevel,
    LocalIsLargeFontMode provides isLargeMode
  ) {
    MaterialTheme(
      colorScheme = DarkColorScheme,
      typography = Typography,
      content = content
    )
  }
}
