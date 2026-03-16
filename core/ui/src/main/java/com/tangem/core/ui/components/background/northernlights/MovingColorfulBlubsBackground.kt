@file:Suppress("MagicNumber")
package com.tangem.core.ui.components.background.northernlights

import androidx.compose.runtime.Composable
import android.graphics.BlurMaskFilter
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import com.tangem.core.ui.res.LocalIsInDarkTheme

@Suppress("LongMethod")
@Composable
internal fun MovingColorfulBlubsBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "FluidMeshGradient")
    val isDark = LocalIsInDarkTheme.current

    // ── Circle 1 (left) — matches dc1 / lc1 from shader version ─────────────
    val color1 by transition.animateColor(
        initialValue = if (isDark) Color(0xFF0D0D3A) else Color(0xFFCCB8EE),
        targetValue = if (isDark) Color(0xFF1C1C6E) else Color(0xFFBBA0E8),
        animationSpec = infiniteRepeatable(
            animation = tween(4_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "color1",
    )
    val x1 by transition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(5_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "x1",
    )
    val y1 by transition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "y1",
    )

    // ── Circle 2 (right) — matches dc2 / lc2 from shader version ────────────
    val color2 by transition.animateColor(
        initialValue = if (isDark) Color(0xFF0A1238) else Color(0xFFB8C8F0),
        targetValue = if (isDark) Color(0xFF112266) else Color(0xFF9AAEE8),
        animationSpec = infiniteRepeatable(
            animation = tween(5_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(1_500),
        ),
        label = "color2",
    )
    val x2 by transition.animateFloat(
        initialValue = 0.68f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(7_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "x2",
    )
    val y2 by transition.animateFloat(
        initialValue = 0.02f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(5_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(2_000),
        ),
        label = "y2",
    )

    // ── Oval (center) — matches dc4 / lc4 from shader version ───────────────
    val ovalColor by transition.animateColor(
        initialValue = if (isDark) Color(0xFF081A30) else Color(0xFFB8C4EE),
        targetValue = if (isDark) Color(0xFF113355) else Color(0xFFA8B4E8),
        animationSpec = infiniteRepeatable(
            animation = tween(7_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(2_500),
        ),
        label = "ovalColor",
    )
    // ── Circle 3 (center) — matches dc3 / lc3 from shader version ───────────
    val color3 by transition.animateColor(
        initialValue = if (isDark) Color(0xFF110A38) else Color(0xFFDDC8F5),
        targetValue = if (isDark) Color(0xFF2A1666) else Color(0xFFCCB0EE),
        animationSpec = infiniteRepeatable(
            animation = tween(6_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(3_000),
        ),
        label = "color3",
    )
    val x3 by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(6_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(1_000),
        ),
        label = "x3",
    )
    val y3 by transition.animateFloat(
        initialValue = 0.0f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4_500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(500),
        ),
        label = "y3",
    )

    var blurRadiusState by remember { mutableFloatStateOf(0f) }
    val circlePaint1 = remember { Paint() }
    val circlePaint2 = remember { Paint() }
    val circlePaint3 = remember { Paint() }
    val ovalPaint = remember { Paint() }

    Canvas(modifier = modifier) {
        val blurRadius = (size.minDimension * 0.28f).coerceIn(60f, 300f)
        val circleRadius = size.width * 0.52f

        // Update maskFilter only when blur radius changes meaningfully
        if (blurRadiusState != blurRadius) {
            blurRadiusState = blurRadius
            val mf = BlurMaskFilter(blurRadius, BlurMaskFilter.Blur.NORMAL)
            circlePaint1.asFrameworkPaint().maskFilter = mf
            circlePaint2.asFrameworkPaint().maskFilter = mf
            circlePaint3.asFrameworkPaint().maskFilter = mf
            ovalPaint.asFrameworkPaint().maskFilter = mf
        }

        circlePaint1.color = color1.copy(alpha = 0.85f)
        circlePaint2.color = color2.copy(alpha = 0.85f)
        circlePaint3.color = color3.copy(alpha = 0.85f)
        ovalPaint.color = ovalColor.copy(alpha = 0.80f)

        drawIntoCanvas { canvas ->
            canvas.drawCircle(Offset(x1 * size.width, y1 * size.height), circleRadius, circlePaint1)
            canvas.drawCircle(Offset(x2 * size.width, y2 * size.height), circleRadius, circlePaint2)
            canvas.drawCircle(Offset(x3 * size.width, y3 * size.height), circleRadius, circlePaint3)

            val halfW = size.width * 0.68f
            val halfH = size.width * 0.24f
            val ovalCx = size.width * 0.50f
            val ovalCy = 0f

            canvas.drawOval(
                Rect(left = ovalCx - halfW, top = ovalCy - halfH, right = ovalCx + halfW, bottom = ovalCy + halfH),
                ovalPaint,
            )
        }
    }
}