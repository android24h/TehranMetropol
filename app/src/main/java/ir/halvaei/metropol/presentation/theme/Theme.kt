package ir.halvaei.metropol.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ir.halvaei.metropol.R

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
)

internal val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2FE),
    onSecondaryContainer = Color(0xFF0C4A6E),
    tertiary = Color(0xFF9333EA),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF3B0764),
    background = Color(0xFFF4F5FB),
    onBackground = Color(0xFF151827),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF151827),
    surfaceVariant = Color(0xFFEEF0F8),
    onSurfaceVariant = Color(0xFF5B6178),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8F9FD),
    surfaceContainer = Color(0xFFF1F3FA),
    surfaceContainerHigh = Color(0xFFEBEEF7),
    surfaceContainerHighest = Color(0xFFE4E8F3),
    outline = Color(0xFFCBD0E0),
    outlineVariant = Color(0xFFE3E6F0),
    error = Color(0xFFE11D48),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF7DD3FC),
    onSecondary = Color(0xFF082F49),
    secondaryContainer = Color(0xFF0C4A6E),
    onSecondaryContainer = Color(0xFFE0F2FE),
    tertiary = Color(0xFFD8B4FE),
    onTertiary = Color(0xFF3B0764),
    tertiaryContainer = Color(0xFF581C87),
    onTertiaryContainer = Color(0xFFF3E8FF),
    background = Color(0xFF0A0E1A),
    onBackground = Color(0xFFE7E9F3),
    surface = Color(0xFF121829),
    onSurface = Color(0xFFE7E9F3),
    surfaceVariant = Color(0xFF1C2337),
    onSurfaceVariant = Color(0xFFA4ABC3),
    surfaceContainerLowest = Color(0xFF0D1220),
    surfaceContainerLow = Color(0xFF131A2B),
    surfaceContainer = Color(0xFF182033),
    surfaceContainerHigh = Color(0xFF1E273C),
    surfaceContainerHighest = Color(0xFF253047),
    outline = Color(0xFF3A4560),
    outlineVariant = Color(0xFF263049),
    error = Color(0xFFFB7185),
)

@Immutable
data class MetroColors(
    val heroGradient: Brush,
    val actionGradient: Brush,
    val origin: Color,
    val destination: Color,
    val transfer: Color,
    val success: Color,
    val warning: Color,
    /** Highlight for women-only cars. */
    val women: Color,
    val isDark: Boolean,
)

private val LightMetroColors = MetroColors(
    heroGradient = Brush.linearGradient(listOf(Color(0xFF1E1B4B), Color(0xFF3730A3), Color(0xFF7C3AED))),
    actionGradient = Brush.horizontalGradient(listOf(Color(0xFF4F46E5), Color(0xFF7C3AED), Color(0xFFC026D3))),
    origin = Color(0xFF10B981),
    destination = Color(0xFFF43F5E),
    transfer = Color(0xFFF59E0B),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
    women = Color(0xFFDB2777),
    isDark = false,
)

private val DarkMetroColors = MetroColors(
    heroGradient = Brush.linearGradient(listOf(Color(0xFF0B0A24), Color(0xFF221C6B), Color(0xFF4C1D95))),
    actionGradient = Brush.horizontalGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFD946EF))),
    origin = Color(0xFF34D399),
    destination = Color(0xFFFB7185),
    transfer = Color(0xFFFBBF24),
    success = Color(0xFF34D399),
    warning = Color(0xFFFBBF24),
    women = Color(0xFFF472B6),
    isDark = true,
)

val LocalMetroColors = staticCompositionLocalOf { LightMetroColors }

private fun Typography.withFont(family: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = family)
    return copy(
        displayLarge = displayLarge.f(), displayMedium = displayMedium.f(), displaySmall = displaySmall.f(),
        headlineLarge = headlineLarge.f().copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = headlineMedium.f().copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.f().copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.f().copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.f().copy(fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.f().copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.f(), bodyMedium = bodyMedium.f(), bodySmall = bodySmall.f(),
        labelLarge = labelLarge.f().copy(fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.f(), labelSmall = labelSmall.f(),
    )
}

private val MetroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MetroTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMetroColors provides if (darkTheme) DarkMetroColors else LightMetroColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = Typography().withFont(Vazirmatn),
            shapes = MetroShapes,
            content = content,
        )
    }
}

object MetroTheme {
    val colors: MetroColors
        @Composable get() = LocalMetroColors.current
}
