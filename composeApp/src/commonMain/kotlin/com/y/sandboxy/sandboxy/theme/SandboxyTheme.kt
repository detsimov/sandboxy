package com.y.sandboxy.sandboxy.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Primary palette — cool blue
private val Primary = Color(0xFF7AA2F7)
private val OnPrimary = Color(0xFF003060)
private val PrimaryContainer = Color(0xFF2B5278)
private val OnPrimaryContainer = Color(0xFFD6E3FF)

// Secondary palette — muted teal
private val Secondary = Color(0xFF9ECAFF)
private val OnSecondary = Color(0xFF003258)
private val SecondaryContainer = Color(0xFF1E3A5F)
private val OnSecondaryContainer = Color(0xFFCDE5FF)

// Tertiary palette — soft violet
private val Tertiary = Color(0xFFBB86FC)
private val OnTertiary = Color(0xFF3B0072)
private val TertiaryContainer = Color(0xFF4F2494)
private val OnTertiaryContainer = Color(0xFFE8DEFF)

// Error
private val Error = Color(0xFFF2B8B5)
private val OnError = Color(0xFF601410)
private val ErrorContainer = Color(0xFF8C1D18)
private val OnErrorContainer = Color(0xFFF9DEDC)

// Neutrals — surface hierarchy
private val Background = Color(0xFF121212)
private val OnBackground = Color(0xFFE3E3E3)
private val Surface = Color(0xFF1E1E1E)
private val OnSurface = Color(0xFFE3E3E3)
private val SurfaceVariant = Color(0xFF2D2D30)
private val OnSurfaceVariant = Color(0xFF9E9E9E)
private val SurfaceContainerLowest = Color(0xFF0F0F0F)
private val SurfaceContainerLow = Color(0xFF1A1A1A)
private val SurfaceContainer = Color(0xFF222222)
private val SurfaceContainerHigh = Color(0xFF2A2A2D)
private val SurfaceContainerHighest = Color(0xFF333338)
private val Outline = Color(0xFF3E3E42)
private val OutlineVariant = Color(0xFF2E2E32)

private val SandboxyColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = Outline,
    outlineVariant = OutlineVariant,
)

private val SandboxyTypography = Typography(
    displayLarge = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, lineHeight = 64.sp),
    displayMedium = TextStyle(fontSize = 45.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, lineHeight = 52.sp),
    displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, fontFamily = FontFamily.SansSerif, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.SansSerif, lineHeight = 16.sp, letterSpacing = 0.5.sp),
)

private val SandboxyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SandboxyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SandboxyColorScheme,
        typography = SandboxyTypography,
        shapes = SandboxyShapes,
        content = content,
    )
}
