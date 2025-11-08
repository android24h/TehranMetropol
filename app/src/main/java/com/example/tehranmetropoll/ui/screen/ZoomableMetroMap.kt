import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun ZoomOnlyMetroMap(
    painter: Painter,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp) // ارتفاع دلخواه برای نقشه
            .background(Color.White)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painter,
            contentDescription = "Tehran Metro Map",
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    // این قسمت باعث میشه زوم از مرکز تصویر باشه نه گوشه
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                )
                .fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    }
}
