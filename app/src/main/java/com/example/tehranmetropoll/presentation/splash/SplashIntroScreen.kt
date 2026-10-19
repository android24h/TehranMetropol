package com.example.tehranmetropoll.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.presentation.components.SystemBarsAppearance
import com.example.tehranmetropoll.presentation.theme.MetroTheme
import com.example.tehranmetropoll.presentation.util.lineColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Total time the intro stays up – long enough to read the tribute comfortably. */
private const val INTRO_DURATION_MS = 3000L

/** Fraction of the screen height where the logo is centred. */
private const val LOGO_CENTER = 0.36f

/**
 * Animated intro shown right after the system splash: the seven metro lines draw in from the
 * screen edges and arrive at the logo, then the name and a memorial message for the victims of
 * the Metropol tragedy fade in.
 */
@Composable
fun SplashIntroScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val lines = remember { Animatable(0f) }
    val logo = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tribute = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logo.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        launch { lines.animateTo(1f, tween(1500, delayMillis = 100, easing = FastOutSlowInEasing)) }
        launch { title.animateTo(1f, tween(700, delayMillis = 350, easing = LinearOutSlowInEasing)) }
        launch { tribute.animateTo(1f, tween(900, delayMillis = 550, easing = LinearOutSlowInEasing)) }
        delay(INTRO_DURATION_MS)
        finish()
    }

    SystemBarsAppearance(lightIcons = MetroTheme.colors.isDark)

    val glow = rememberInfiniteTransition(label = "glow")
    val glowScale by glow.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "glowScale",
    )

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val logoCenterY = maxHeight * LOGO_CENTER
        val surface = MaterialTheme.colorScheme.background

        ConvergingLines(
            progress = lines.value,
            centerYFraction = LOGO_CENTER,
            dotFill = surface,
            modifier = Modifier.fillMaxSize(),
        )

        // Soft glow + logo
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = logoCenterY - 80.dp)
                .size(160.dp)
                .graphicsLayer {
                    alpha = logo.value.coerceIn(0f, 1f) * 0.9f
                    scaleX = glowScale
                    scaleY = glowScale
                }
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        )
        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = logoCenterY - 56.dp)
                .size(112.dp)
                .graphicsLayer {
                    val s = 0.6f + 0.4f * logo.value
                    scaleX = s
                    scaleY = s
                    alpha = logo.value.coerceIn(0f, 1f)
                },
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = logoCenterY + 84.dp)
                .graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 24.dp.toPx()
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Tribute(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 36.dp, vertical = 40.dp)
                .graphicsLayer {
                    alpha = tribute.value
                    translationY = (1f - tribute.value) * 12.dp.toPx()
                },
        )
    }
}

@Composable
private fun Tribute(modifier: Modifier = Modifier) {
    val ribbonLabel = stringResource(R.string.splash_ribbon)
    Column(
        modifier.widthIn(max = 420.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        MourningRibbon(
            Modifier
                .size(width = 22.dp, height = 32.dp)
                .semantics { contentDescription = ribbonLabel },
        )
        Box(
            Modifier
                .width(36.dp)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outline),
        )
        Text(
            stringResource(R.string.splash_tribute),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 1.9.em, letterSpacing = 0.01.em),
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The black awareness ribbon worn in mourning; outlined softly on dark backgrounds. */
@Composable
private fun MourningRibbon(modifier: Modifier = Modifier) {
    val dark = MetroTheme.colors.isDark
    Canvas(modifier) {
        val sx = size.width / 24f
        val sy = size.height / 34f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        val path = Path().apply {
            p(6f, 33f).let { moveTo(it.x, it.y) }
            cubicTo(p(8f, 27f), p(16f, 17f), p(16.5f, 10f))
            cubicTo(p(17f, 5f), p(15f, 2f), p(12f, 2f))
            cubicTo(p(9f, 2f), p(7f, 5f), p(7.5f, 10f))
            cubicTo(p(8f, 17f), p(16f, 27f), p(18f, 33f))
        }
        val width = 3.6f * sx
        if (dark) {
            drawPath(path, Color.White.copy(alpha = 0.32f), style = Stroke(width + 2.dp.toPx(), join = StrokeJoin.Round))
        }
        drawPath(path, Color(0xFF111111), style = Stroke(width, join = StrokeJoin.Round))
    }
}

private fun Path.cubicTo(a: Offset, b: Offset, c: Offset) = cubicTo(a.x, a.y, b.x, b.y, c.x, c.y)

/**
 * Seven lines in the official line colours. Each one enters from the edge of the screen, bends
 * at 45° like a metro map and ends at a station dot just outside the logo.
 */
@Composable
private fun ConvergingLines(
    progress: Float,
    centerYFraction: Float,
    dotFill: Color,
    modifier: Modifier = Modifier,
) {
    // Outward directions in degrees (0 = right, -90 = up), fanned around the top of the logo.
    val angles = remember { listOf(-178f, -148f, -120f, -90f, -60f, -32f, -2f) }
    Canvas(modifier) {
        val center = Offset(size.width / 2, size.height * centerYFraction)
        val stroke = 7.dp.toPx()
        val corner = PathEffect.cornerPathEffect(28.dp.toPx())
        angles.forEachIndexed { index, deg ->
            // Stagger: each line starts a little after the previous one.
            val start = index * 0.06f
            val local = ((progress - start) / (1f - 0.36f)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEachIndexed
            drawMetroLine(
                index = index,
                angle = deg,
                center = center,
                progress = local,
                stroke = stroke,
                corner = corner,
                dotFill = dotFill,
            )
        }
    }
}

private fun DrawScope.drawMetroLine(
    index: Int,
    angle: Float,
    center: Offset,
    progress: Float,
    stroke: Float,
    corner: PathEffect,
    dotFill: Color,
) {
    val color = lineColor(index + 1)
    val rad = angle * PI.toFloat() / 180f
    val dir = Offset(cos(rad), sin(rad))
    val inner = center + dir * 92.dp.toPx()
    val elbow = center + dir * (150 + 30 * (index % 3)).dp.toPx()
    // From the elbow, run straight (horizontally or vertically) off-screen.
    val outer = if (abs(dir.x) > abs(dir.y) * 1.2f) {
        Offset(if (dir.x > 0) size.width + stroke * 2 else -stroke * 2, elbow.y)
    } else {
        Offset(elbow.x, -stroke * 2)
    }
    val full = Path().apply {
        moveTo(outer.x, outer.y)
        lineTo(elbow.x, elbow.y)
        lineTo(inner.x, inner.y)
    }
    val measure = PathMeasure().apply { setPath(full, false) }
    val partial = Path()
    measure.getSegment(0f, measure.length * progress, partial, true)
    drawPath(
        partial,
        color.copy(alpha = 0.9f),
        style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = corner),
    )
    // Station dot pops in once the line has arrived.
    val dot = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)
    if (dot > 0f) {
        val r = stroke * 0.95f * dot
        drawCircle(dotFill, radius = r, center = inner)
        drawCircle(color, radius = r, center = inner, style = Stroke(width = stroke * 0.45f))
    }
}
