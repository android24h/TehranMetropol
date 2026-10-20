package ir.halvaei.metropol.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import ir.halvaei.metropol.core.localizeDigits
import ir.halvaei.metropol.domain.model.MetroLine
import ir.halvaei.metropol.domain.model.Station
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/** True when the UI is currently rendered in Persian. */
val isPersian: Boolean
    @Composable @ReadOnlyComposable
    get() = LocalConfiguration.current.locales[0].language == "fa"

@Composable @ReadOnlyComposable
fun Station.displayName(): String = name(isPersian)

@Composable @ReadOnlyComposable
fun Station.secondaryName(): String = alternateName(isPersian)

@Composable @ReadOnlyComposable
fun MetroLine.displayName(): String = name(isPersian)

@Composable @ReadOnlyComposable
fun Int.loc(): String = toString().localizeDigits(isPersian)

@Composable @ReadOnlyComposable
fun String.loc(): String = localizeDigits(isPersian)

/** Rounds travel estimates up – it is kinder to arrive early than late. */
fun Double.ceilMinutes(): Int = ceil(this - 0.05).toInt().coerceAtLeast(1)

fun Double.roundMinutes(): Int = roundToInt().coerceAtLeast(1)

@Composable @ReadOnlyComposable
fun formatClock(minuteOfDay: Int): String {
    val m = ((minuteOfDay % 1440) + 1440) % 1440
    return String.format(Locale.US, "%02d:%02d", m / 60, m % 60).loc()
}

fun MetroLine.color(): Color = Color(colorArgb)
fun MetroLine.onColor(): Color = Color(onColorArgb)

fun lineColor(number: Int): Color = when (number) {
    1 -> Color(0xFFE0001B)
    2 -> Color(0xFF2F4389)
    3 -> Color(0xFF67C5F5)
    4 -> Color(0xFFF8E100)
    5 -> Color(0xFF007E46)
    6 -> Color(0xFFEF639F)
    7 -> Color(0xFF8E1F83)
    else -> Color(0xFF9E9E9E)
}

fun lineOnColor(number: Int): Color = when (number) {
    3 -> Color(0xFF0B2540)
    4 -> Color(0xFF2B2500)
    else -> Color.White
}
