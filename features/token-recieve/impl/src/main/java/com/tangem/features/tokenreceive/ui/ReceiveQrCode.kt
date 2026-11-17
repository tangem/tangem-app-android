package com.tangem.features.tokenreceive.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import com.tangem.features.tokenreceive.entity.ReceiveQrLayout
import com.tangem.features.tokenreceive.entity.receiveQrLayout

@Composable
internal fun ReceiveQrCode(content: String, color: Color, modifier: Modifier = Modifier) {
    val layout = remember(content) { receiveQrLayout(content) }
    Canvas(modifier = modifier) {
        val factor = size.minDimension / ReceiveQrLayout.CANVAS_SIZE
        scale(scale = factor, pivot = Offset.Zero) {
            drawDots(layout, color)
            layout.finderOrigins.forEach { drawFinder(layout, it, color) }
        }
    }
}

private fun DrawScope.drawDots(layout: ReceiveQrLayout, color: Color) {
    val moduleSize = layout.moduleSize
    val radius = moduleSize * ReceiveQrLayout.DOT_SCALE / 2
    layout.dots.forEach { module ->
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(
                x = ReceiveQrLayout.QUIET_ZONE + module.column * moduleSize + moduleSize / 2,
                y = ReceiveQrLayout.QUIET_ZONE + module.row * moduleSize + moduleSize / 2,
            ),
        )
    }
}

private fun DrawScope.drawFinder(layout: ReceiveQrLayout, origin: ReceiveQrLayout.Module, color: Color) {
    val moduleSize = layout.moduleSize
    val left = ReceiveQrLayout.QUIET_ZONE + origin.column * moduleSize
    val top = ReceiveQrLayout.QUIET_ZONE + origin.row * moduleSize
    val frameSize = ReceiveQrLayout.FINDER_MODULES * moduleSize
    val outerRadius = ReceiveQrLayout.FINDER_OUTER_RADIUS
    val innerRadius = (outerRadius - moduleSize).coerceAtLeast(0f)

    val frame = Path().apply {
        fillType = PathFillType.EvenOdd
        addRoundRect(
            RoundRect(
                left = left,
                top = top,
                right = left + frameSize,
                bottom = top + frameSize,
                cornerRadius = CornerRadius(outerRadius),
            ),
        )
        addRoundRect(
            RoundRect(
                left = left + moduleSize,
                top = top + moduleSize,
                right = left + frameSize - moduleSize,
                bottom = top + frameSize - moduleSize,
                cornerRadius = CornerRadius(innerRadius),
            ),
        )
    }
    drawPath(path = frame, color = color)

    val pupilInset = ReceiveQrLayout.FINDER_PUPIL_INSET_MODULES * moduleSize
    val pupilSize = ReceiveQrLayout.FINDER_PUPIL_MODULES * moduleSize
    drawRoundRect(
        color = color,
        topLeft = Offset(x = left + pupilInset, y = top + pupilInset),
        size = Size(width = pupilSize, height = pupilSize),
        cornerRadius = CornerRadius(ReceiveQrLayout.FINDER_PUPIL_RADIUS),
    )
}