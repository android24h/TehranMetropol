package com.example.tehranmetropoll.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.data.model.StationDataModel

@Composable
fun MetroMap(
    allStations: List<StationDataModel> = emptyList(),
    routeStations: List<StationDataModel> = emptyList()
) {
    var scale by remember { mutableStateOf(1f) }
    val ctx = LocalContext.current

    // IMPORTANT: این را دقیق با ابعاد فایل اصلی تصویر جایگزین کن
    // (اگر Paint گفت 1920x1470) بذار 1920f و 1470f — ولی حتماً با کد زیر ابعاد واقعی فایل را هم چک کن.
    var imageWidth = 1920f
    var imageHeight = 1470f

    // به‌صورت ایمن از اندازهٔ خود Resource هم می‌گیریم و لاگ می‌زنیم:
    LaunchedEffect(Unit) {
        try {
            val bmp = android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.tehran_map)
            android.util.Log.d("MetroMapDebug", "bitmap size = ${bmp.width} x ${bmp.height}")
            imageWidth = bmp.width.toFloat()
            imageHeight = bmp.height.toFloat()
        } catch (e: Exception) {
            android.util.Log.e("MetroMapDebug", "cannot decode bitmap: ${e.message}")
        }

        // لاگِ routeStations اولیه (ترتیب، id و نام)
        android.util.Log.d("MetroMapDebug", "routeStations order: ${routeStations.map { "${it.id}:${it.name}" }}")
        android.util.Log.d("MetroMapDebug", "allStations count=${allStations.size}")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                }
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.tehran_map),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .fillMaxSize()
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = scale, scaleY = scale)
        ) {
            val canvasW = size.width
            val canvasH = size.height
            val imageAspect = imageWidth / imageHeight
            val canvasAspect = canvasW / canvasH

            val imageDrawWidth: Float
            val imageDrawHeight: Float
            val offsetX: Float
            val offsetY: Float

            if (canvasAspect > imageAspect) {
                imageDrawHeight = canvasH
                imageDrawWidth = imageDrawHeight * imageAspect
                offsetX = (canvasW - imageDrawWidth) / 2f
                offsetY = 0f
            } else {
                imageDrawWidth = canvasW
                imageDrawHeight = imageDrawWidth / imageAspect
                offsetX = 0f
                offsetY = (canvasH - imageDrawHeight) / 2f
            }

            val scaleX = imageDrawWidth / imageWidth
            val scaleY = imageDrawHeight / imageHeight

            // لاگِ تبدیل مختصات برای هر ایستگاه (مخصوصا ایستگاه‌های روت)
            routeStations.forEach { s ->
                val cx = offsetX + s.x * scaleX
                val cy = offsetY + s.y * scaleY
                android.util.Log.d("MetroMapDebug", "station(${s.id},${s.name}) img=(${s.x},${s.y}) -> canvas=(${cx},${cy})")
            }

            // رسم خط تستی (برای اطمینان از Canvas)
            drawLine(
                color = Color.Green,
                start = Offset(10f, 10f),
                end = Offset(200f, 200f),
                strokeWidth = 8f
            )

            // رسم خطوط مسیر (با استفاده از تبدیل بالا)
            if (routeStations.size > 1) {
                for (i in 0 until routeStations.size - 1) {
                    val a = routeStations[i]
                    val b = routeStations[i + 1]
                    val start = Offset(offsetX + a.x * scaleX, offsetY + a.y * scaleY)
                    val end = Offset(offsetX + b.x * scaleX, offsetY + b.y * scaleY)
                    drawLine(color = Color.Cyan, start = start, end = end, strokeWidth = 10f, cap = StrokeCap.Round)
                }
            }

            // رسم نقاط تمام ایستگاه‌ها (خاکستری کوچک)
          /*  allStations.forEach { s ->
                val cx = offsetX + s.x * scaleX
                val cy = offsetY + s.y * scaleY
                drawCircle(color = Color.DarkGray, radius = 6.dp.toPx(), center = Offset(cx, cy))
            }*/

            // رسم نقاط مسیر (قرمز بزرگ) و نوشتن اسم (برای دیباگ)
            routeStations.forEach { s ->
                val cx = offsetX + s.x * scaleX
                val cy = offsetY + s.y * scaleY
                drawCircle(color = Color.Red, radius = 12.dp.toPx(), center = Offset(cx, cy))
                // نوشتن نام (از nativeCanvas استفاده می‌کنیم)
                drawContext.canvas.nativeCanvas.apply {
                    drawText("${s.nameFa.ifBlank { s.name }}", cx + 8f, cy - 8f, android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                    })
                }
            }
        }
    }
}



