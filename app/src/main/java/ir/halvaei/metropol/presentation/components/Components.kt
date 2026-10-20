package ir.halvaei.metropol.presentation.components

import android.app.Activity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import ir.halvaei.metropol.domain.model.MetroLine
import ir.halvaei.metropol.presentation.theme.MetroTheme
import ir.halvaei.metropol.presentation.util.color
import ir.halvaei.metropol.presentation.util.lineColor
import ir.halvaei.metropol.presentation.util.lineOnColor
import ir.halvaei.metropol.presentation.util.loc
import ir.halvaei.metropol.presentation.util.onColor

/** Sets status/navigation bar icon contrast for the current screen. */
@Composable
fun SystemBarsAppearance(lightIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(lightIcons) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !lightIcons
                isAppearanceLightNavigationBars = !lightIcons
            }
        }
        onDispose { }
    }
}

@Composable
fun LineBadge(number: Int, modifier: Modifier = Modifier, size: Dp = 26.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(lineColor(number)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.loc(),
            color = lineOnColor(number),
            fontWeight = FontWeight.ExtraBold,
            fontSize = (size.value * 0.5f).sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun LineBadges(numbers: List<Int>, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        numbers.forEach { LineBadge(it, size = size) }
    }
}

/** Pill showing the line name in its own colours, e.g. "Line 2". */
@Composable
fun LinePill(line: MetroLine, text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(line.color())
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(text, color = line.onColor(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    brush: Brush = MetroTheme.colors.actionGradient,
) {
    val scale by animateFloatAsState(if (enabled) 1f else 0.98f, label = "btnScale")
    Box(
        modifier = modifier
            .scale(scale)
            .height(58.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(brush)
            .alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Small rounded info chip used for trip stats. */
@Composable
fun StatChip(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    container: Color = Color.White.copy(alpha = 0.14f),
    content: Color = Color.White,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = content, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Color.White.copy(alpha = 0.16f),
    tint: Color = Color.White,
    size: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

/** Elevated rounded card with a hairline border – the base surface of most content. */
@Composable
fun MetroCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = if (MetroTheme.colors.isDark) 0.dp else 6.dp,
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

/**
 * Decorative stylised metro map drawn behind hero headers: sweeping coloured lines with
 * station dots, faded so text on top stays readable.
 */
@Composable
fun MetroLinesDecoration(modifier: Modifier = Modifier, alpha: Float = 0.35f) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 5.dp.toPx()
        fun line(color: Color, points: List<Offset>) {
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val mid = Offset((p0.x + p1.x) / 2, (p0.y + p1.y) / 2)
                    quadraticTo(p0.x, p0.y, mid.x, mid.y)
                    if (i == points.lastIndex) lineTo(p1.x, p1.y)
                }
            }
            drawPath(path, color.copy(alpha = alpha), style = Stroke(width = stroke, cap = StrokeCap.Round))
            points.forEachIndexed { index, p ->
                if (index % 2 == 1) {
                    drawCircle(Color.White.copy(alpha = alpha + 0.15f), radius = stroke * 0.9f, center = p)
                    drawCircle(color.copy(alpha = alpha + 0.2f), radius = stroke * 0.5f, center = p)
                }
            }
        }
        line(lineColor(1), listOf(Offset(w * 0.72f, -20f), Offset(w * 0.70f, h * 0.35f), Offset(w * 0.78f, h * 0.7f), Offset(w * 0.74f, h * 1.1f)))
        line(lineColor(4), listOf(Offset(-20f, h * 0.55f), Offset(w * 0.35f, h * 0.5f), Offset(w * 0.7f, h * 0.56f), Offset(w + 20f, h * 0.48f)))
        line(lineColor(3), listOf(Offset(w * 1.05f, h * 0.05f), Offset(w * 0.85f, h * 0.25f), Offset(w * 0.5f, h * 0.75f), Offset(w * 0.2f, h * 1.1f)))
        line(lineColor(6), listOf(Offset(w * 0.05f, -10f), Offset(w * 0.2f, h * 0.3f), Offset(w * 0.45f, h * 0.45f), Offset(w * 0.6f, h * 0.9f)))
        line(lineColor(5), listOf(Offset(-20f, h * 0.2f), Offset(w * 0.25f, h * 0.18f), Offset(w * 0.4f, h * 0.1f), Offset(w * 0.55f, -20f)))
    }
}
