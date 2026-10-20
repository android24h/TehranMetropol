package ir.halvaei.metropol.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.StrokeJoin
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
import ir.halvaei.metropol.R
import ir.halvaei.metropol.presentation.components.SystemBarsAppearance
import ir.halvaei.metropol.presentation.theme.MetroTheme
import ir.halvaei.metropol.presentation.util.lineColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Time the intro stays up before the cross-fade to the home screen (~2.5 s in total with it). */
private const val INTRO_DURATION_MS = 2000L

/** Fraction of the screen height where the logo is centred – close to where the system splash icon sits. */
private const val LOGO_CENTER = 0.42f

private val LogoSize = 112.dp

/**
 * Quiet intro shown right after the system splash: the logo fades and settles in the centre, a
 * thin strip in the seven metro line colours opens beneath the name, then the memorial message
 * for the victims of the Metropol tragedy fades in.
 */
@Composable
fun SplashIntroScreen(onFinished: () -> Unit) {
    val finish by rememberUpdatedState(onFinished)
    val logo = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val stripes = remember { Animatable(0f) }
    val tribute = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logo.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        launch { title.animateTo(1f, tween(600, delayMillis = 250, easing = LinearOutSlowInEasing)) }
        launch { stripes.animateTo(1f, tween(700, delayMillis = 400, easing = FastOutSlowInEasing)) }
        launch { tribute.animateTo(1f, tween(800, delayMillis = 650, easing = LinearOutSlowInEasing)) }
        delay(INTRO_DURATION_MS)
        finish()
    }

    SystemBarsAppearance(lightIcons = MetroTheme.colors.isDark)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val logoCenterY = maxHeight * LOGO_CENTER

        Image(
            painter = painterResource(R.drawable.ic_app_logo),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = logoCenterY - LogoSize / 2)
                .size(LogoSize)
                .graphicsLayer {
                    val s = 0.9f + 0.1f * logo.value
                    scaleX = s
                    scaleY = s
                    alpha = logo.value
                },
        )

        Column(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = logoCenterY + LogoSize / 2 + 20.dp)
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 8.dp.toPx()
                },
            )
            Spacer(Modifier.height(18.dp))
            LineStripes(
                Modifier.graphicsLayer {
                    alpha = stripes.value
                    scaleX = stripes.value
                },
            )
            Spacer(Modifier.height(28.dp))
            Tribute(
                Modifier.graphicsLayer {
                    alpha = tribute.value
                    translationY = (1f - tribute.value) * 8.dp.toPx()
                },
            )
        }
    }
}

/** Seven thin segments in the official line colours, like the line bar of a metro map. */
@Composable
private fun LineStripes(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (line in 1..7) {
            Spacer(
                Modifier
                    .size(width = 18.dp, height = 3.dp)
                    .clip(RoundedCornerShape(50))
                    .background(lineColor(line).copy(alpha = 0.85f)),
            )
        }
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
                .size(width = 20.dp, height = 29.dp)
                .semantics { contentDescription = ribbonLabel },
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
