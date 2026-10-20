package com.xboard.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Brand = Color(0xFF5B5BD6)
val BrandDeep = Color(0xFF3B3BB5)
val BrandLight = Color(0xFF8E8FFF)
val Mint = Color(0xFF00C2A8)
val Coral = Color(0xFFFF7A59)
val Amber = Color(0xFFF5A524)

val HeroGradient = listOf(Color(0xFF5B5BD6), Color(0xFF7A5CF0), Color(0xFF4B3FD4))

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E8FF),
    onPrimaryContainer = Color(0xFF16154F),
    secondary = Mint,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC9F6EF),
    onSecondaryContainer = Color(0xFF00382F),
    tertiary = Coral,
    onTertiary = Color.White,
    background = Color(0xFFF6F6FB),
    onBackground = Color(0xFF111118),
    surface = Color.White,
    onSurface = Color(0xFF111118),
    surfaceVariant = Color(0xFFEFEFF7),
    onSurfaceVariant = Color(0xFF5E5E72),
    outline = Color(0xFFDCDCE9),
    outlineVariant = Color(0xFFE9E9F2),
    error = Color(0xFFD64545),
    onError = Color.White,
    errorContainer = Color(0xFFFFE3E3),
    onErrorContainer = Color(0xFF5C0F0F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA9AAFF),
    onPrimary = Color(0xFF1A1950),
    primaryContainer = Color(0xFF2D2C6E),
    onPrimaryContainer = Color(0xFFE3E2FF),
    secondary = Color(0xFF5BE0C8),
    onSecondary = Color(0xFF003830),
    secondaryContainer = Color(0xFF0E4A41),
    onSecondaryContainer = Color(0xFFC9F6EF),
    tertiary = Color(0xFFFFA184),
    onTertiary = Color(0xFF4A1400),
    background = Color(0xFF0D0D12),
    onBackground = Color(0xFFF0F0F8),
    surface = Color(0xFF16161E),
    onSurface = Color(0xFFF0F0F8),
    surfaceVariant = Color(0xFF23232E),
    onSurfaceVariant = Color(0xFFA9A9BE),
    outline = Color(0xFF353542),
    outlineVariant = Color(0xFF2A2A36),
    error = Color(0xFFFF8A8A),
    onError = Color(0xFF4A0B0B),
    errorContainer = Color(0xFF5C1717),
    onErrorContainer = Color(0xFFFFD9D9),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun XboardTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
