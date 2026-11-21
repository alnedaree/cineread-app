package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val StudioDarkColorScheme = darkColorScheme(
    primary = OnAirCrimson,
    onPrimary = Color.White,
    primaryContainer = OnAirCrimsonContainer,
    onPrimaryContainer = OnAirCrimsonSoft,
    secondary = BroadcastGold,
    onSecondary = StudioObsidian,
    secondaryContainer = BroadcastGoldContainer,
    onSecondaryContainer = BroadcastGoldSoft,
    tertiary = VoiceSignalCyan,
    onTertiary = StudioObsidian,
    tertiaryContainer = VoiceSignalCyanContainer,
    onTertiaryContainer = Color(0xFFA5F3FC),
    background = StudioObsidian,
    onBackground = InkPrimaryDark,
    surface = StudioNavySurface,
    onSurface = InkPrimaryDark,
    surfaceVariant = StudioCardSurface,
    onSurfaceVariant = InkSecondaryDark,
    surfaceContainerLowest = StudioObsidian,
    surfaceContainerLow = StudioNavySurface,
    surfaceContainer = StudioCardSurface,
    surfaceContainerHigh = StudioCardElevated,
    surfaceContainerHighest = Color(0xFF2A395E),
    outline = StudioBorder,
    outlineVariant = Color(0xFF1F2B47),
    error = Color(0xFFFF6B81),
    onError = StudioObsidian,
    errorContainer = Color(0xFF531320),
    onErrorContainer = Color(0xFFFFD6DC)
)

private val NewsroomLightColorScheme = lightColorScheme(
    primary = OnAirCrimsonLight,
    onPrimary = Color.White,
    primaryContainer = OnAirCrimsonLightContainer,
    onPrimaryContainer = Color(0xFF7A0D23),
    secondary = BroadcastAmberLight,
    onSecondary = Color.White,
    secondaryContainer = BroadcastAmberLightContainer,
    onSecondaryContainer = Color(0xFF6B3D06),
    tertiary = VoiceTealLight,
    onTertiary = Color.White,
    tertiaryContainer = VoiceTealLightContainer,
    onTertiaryContainer = Color(0xFF083344),
    background = NewsroomPaper,
    onBackground = InkPrimaryLight,
    surface = NewsroomSurface,
    onSurface = InkPrimaryLight,
    surfaceVariant = NewsroomSurfaceVariant,
    onSurfaceVariant = InkSecondaryLight,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAFE),
    surfaceContainer = NewsroomSurface,
    surfaceContainerHigh = NewsroomSurfaceVariant,
    surfaceContainerHighest = Color(0xFFDEE6F3),
    outline = NewsroomBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val AtheerShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AtheerRadioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else NewsroomLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AtheerTypography,
        shapes = AtheerShapes,
        content = content
    )
}
