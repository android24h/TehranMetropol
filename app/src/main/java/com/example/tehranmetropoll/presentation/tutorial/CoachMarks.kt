package com.example.tehranmetropoll.presentation.tutorial

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.presentation.theme.MetroTheme
import com.example.tehranmetropoll.presentation.util.loc
import kotlin.math.roundToInt

/** Home-screen elements the walkthrough can point at. */
enum class CoachTarget { ORIGIN, DESTINATION, MAP, SWAP, FASTEST_ROUTE, LINES, MENU, HELP }

private data class CoachStep(
    val target: CoachTarget,
    @StringRes val title: Int,
    @StringRes val body: Int,
    /** Round icon buttons get a circular spotlight. */
    val circle: Boolean = false,
)

private val steps = listOf(
    CoachStep(CoachTarget.ORIGIN, R.string.coach_origin_title, R.string.coach_origin_body),
    CoachStep(CoachTarget.DESTINATION, R.string.coach_destination_title, R.string.coach_destination_body),
    CoachStep(CoachTarget.MAP, R.string.coach_map_title, R.string.coach_map_body, circle = true),
    CoachStep(CoachTarget.SWAP, R.string.coach_swap_title, R.string.coach_swap_body, circle = true),
    CoachStep(CoachTarget.FASTEST_ROUTE, R.string.coach_fastest_title, R.string.coach_fastest_body),
    CoachStep(CoachTarget.LINES, R.string.coach_lines_title, R.string.coach_lines_body),
    CoachStep(CoachTarget.MENU, R.string.coach_menu_title, R.string.coach_menu_body, circle = true),
    CoachStep(CoachTarget.HELP, R.string.coach_help_title, R.string.coach_help_body, circle = true),
)

/** Tracks where each [CoachTarget] is on screen so the overlay can spotlight it. */
@OptIn(ExperimentalFoundationApi::class)
@Stable
class CoachMarkState {
    internal val bounds = mutableStateMapOf<CoachTarget, Rect>()
    internal val requesters = mutableMapOf<CoachTarget, BringIntoViewRequester>()

    internal fun requester(target: CoachTarget) = requesters.getOrPut(target) { BringIntoViewRequester() }
}

@Composable
fun rememberCoachMarkState() = remember { CoachMarkState() }

/** Registers this element as a walkthrough target (its window bounds, and scroll-into-view). */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.coachTarget(state: CoachMarkState, target: CoachTarget): Modifier = this
    .bringIntoViewRequester(state.requester(target))
    .onGloballyPositioned { state.bounds[target] = it.boundsInWindow() }

/**
 * Full-screen coach-mark overlay drawn on top of the real home screen: dims everything, cuts an
 * animated spotlight around the current target and shows a tooltip card next to it. Tapping
 * anywhere advances; "Skip" or Back ends the tour.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoachMarkOverlay(state: CoachMarkState, onFinish: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]
    val isLast = index == steps.lastIndex
    val next: () -> Unit = { if (isLast) onFinish() else index++ }

    BackHandler(onBack = onFinish)

    LaunchedEffect(step.target) {
        state.requesters[step.target]?.bringIntoView()
    }

    val density = LocalDensity.current
    var overlayOrigin by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(Size.Zero) }
    val padding = with(density) { 8.dp.toPx() }

    // Spotlight rect in overlay coordinates, padded around the target.
    val target = state.bounds[step.target]
        ?.translate(-overlayOrigin)
        ?.inflate(padding)

    val hole = remember { Animatable(Rect.Zero, Rect.VectorConverter) }
    var hasHole by remember { mutableStateOf(false) }
    LaunchedEffect(target) {
        if (target == null) return@LaunchedEffect
        if (!hasHole) {
            hole.snapTo(target)
            hasHole = true
        } else {
            hole.animateTo(target, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow))
        }
    }
    val cornerTarget = with(density) {
        val r = hole.value
        if (step.circle) minOf(r.width, r.height) / 2 else 20.dp.toPx()
    }
    val corner by animateFloatAsState(cornerTarget, label = "corner")

    val pulse = rememberInfiniteTransition(label = "pulse")
    val ring by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300), RepeatMode.Restart),
        label = "ring",
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val scrim = if (MetroTheme.colors.isDark) Color.Black.copy(alpha = 0.78f) else Color(0xFF0B0E1F).copy(alpha = 0.72f)

    AnimatedVisibility(visible, enter = fadeIn(tween(350)), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    overlayOrigin = it.positionInWindow()
                    overlaySize = it.size.let { s -> Size(s.width.toFloat(), s.height.toFloat()) }
                }
                .pointerInput(isLast) { detectTapGestures { next() } },
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen },
            ) {
                drawRect(scrim)
                if (hasHole) {
                    val r = hole.value
                    drawRoundRect(
                        color = Color.Black,
                        topLeft = r.topLeft,
                        size = r.size,
                        cornerRadius = CornerRadius(corner),
                        blendMode = BlendMode.Clear,
                    )
                    // Expanding, fading ring that draws the eye to the spotlight.
                    val grow = 14.dp.toPx() * ring
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.55f * (1f - ring)),
                        topLeft = r.topLeft - Offset(grow, grow),
                        size = Size(r.width + grow * 2, r.height + grow * 2),
                        cornerRadius = CornerRadius(corner + grow),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.9f),
                        topLeft = r.topLeft,
                        size = r.size,
                        cornerRadius = CornerRadius(corner),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }

            CoachCard(
                step = step,
                index = index,
                isLast = isLast,
                spotlight = if (hasHole) hole.value else null,
                overlayHeight = overlaySize.height,
                onNext = next,
                onSkip = onFinish,
            )
        }
    }
}

@Composable
private fun CoachCard(
    step: CoachStep,
    index: Int,
    isLast: Boolean,
    spotlight: Rect?,
    overlayHeight: Float,
    onNext: () -> Unit,
    onSkip: () -> Unit,
) {
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    var cardHeight by remember { mutableIntStateOf(0) }
    val gap = with(density) { 16.dp.toPx() }

    // Prefer below the spotlight; flip above when there is no room.
    val targetY = when {
        spotlight == null || overlayHeight == 0f -> (overlayHeight - cardHeight) / 2
        spotlight.bottom + gap + cardHeight <= overlayHeight - bottomInset - gap -> spotlight.bottom + gap
        else -> (spotlight.top - gap - cardHeight).coerceAtLeast(topInset + gap)
    }
    val y by animateDpAsState(
        with(density) { targetY.coerceAtLeast(0f).toDp() },
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
        label = "cardY",
    )

    Surface(
        modifier = Modifier
            .absoluteOffset { IntOffset(0, y.roundToPx()) }
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .onSizeChanged { cardHeight = it.height }
            // Taps on the card itself shouldn't fall through as "next".
            .pointerInput(Unit) { detectTapGestures { } },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
    ) {
        Column(Modifier.padding(start = 20.dp, end = 12.dp, top = 18.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.tutorial_step, (index + 1).loc(), steps.size.loc()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
                Spacer(Modifier.weight(1f))
                StepDots(index)
                Spacer(Modifier.width(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (fadeIn(tween(260, delayMillis = 80)) + slideInVertically { it / 6 }) togetherWith
                        (fadeOut(tween(140)) + slideOutVertically { -it / 6 })
                },
                label = "coachText",
            ) { s ->
                Column(Modifier.padding(end = 8.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
                    Text(stringResource(s.title), style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(s.body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.tutorial_tap_hint),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isLast) {
                    TextButton(onClick = onSkip) { Text(stringResource(R.string.tutorial_skip)) }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onNext,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Text(stringResource(if (isLast) R.string.tutorial_done else R.string.tutorial_next))
                }
            }
        }
    }
}

@Composable
private fun StepDots(current: Int) {
    Row {
        steps.indices.forEach { i ->
            val width by animateDpAsState(if (i == current) 16.dp else 6.dp, label = "dot")
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(width = width, height = 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (i <= current) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}
