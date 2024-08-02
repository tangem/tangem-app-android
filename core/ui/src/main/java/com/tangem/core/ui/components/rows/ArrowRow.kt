package com.tangem.core.ui.components.rows

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.*

@Composable
inline fun ArrowRow(
    isLastItem: Boolean,
    content: @Composable() (BoxScope.() -> Unit),
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(all = TangemTheme.dimens.spacing0),
) {
    val density = LocalDensity.current.density
    val defaultRowHeight = TangemTheme.dimens.size0
    var itemHeight by remember { mutableStateOf(defaultRowHeight) }

    Row(
        modifier = modifier.onSizeChanged { size ->
            val height = size.height.toFloat()
            if (height != itemHeight.toPx(density)) {
                itemHeight = convertPxToDp(px = height, density = density)
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        ChildArrow(
            childHeight = itemHeight,
            isLastChild = isLastItem,
        )

        Box(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxWidth(),
            contentAlignment = Alignment.CenterStart,
        ) {
            content()
        }
    }
}

@Suppress("LongParameterList")
@Immutable
private class ChildArrowScope(
    val figureRect: Rect,
    val arrowHeadRect: Rect,
    val curvedArrowRect: Rect,
    val arrowStrokeWidth: Float,
    val arrowHeadRadius: Float,
    val strokeColor: Color,
    drawScope: DrawScope,
) : DrawScope by drawScope

@Composable
fun ChildArrow(childHeight: Dp, isLastChild: Boolean) {
    val figureWidth = TangemTheme.dimens.size40

    val strokeColor = TangemTheme.colors.stroke.secondary
    val arrowStrokeWidthDp = TangemTheme.dimens.size1

    val arrowHeadRadiusDp = TangemTheme.dimens.size1

    val figureRectDp = DpRect(
        origin = DpOffset.Zero,
        size = DpSize(width = TangemTheme.dimens.size40, height = childHeight),
    )

    val arrowHeadSize = DpSize(
        width = TangemTheme.dimens.size6,
        height = TangemTheme.dimens.size6,
    )
    val arrowHeadRectDp = DpRect(
        origin = DpOffset(
            x = figureWidth - arrowHeadSize.width,
            y = figureRectDp.size.center.y - arrowHeadSize.center.y,
        ),
        size = arrowHeadSize,
    )

    val curvedArrowRectDp = DpRect(
        top = figureRectDp.top,
        left = TangemTheme.dimens.size18,
        right = figureRectDp.right - arrowHeadRectDp.width,
        bottom = figureRectDp.size.center.y,
    )

    Canvas(
        modifier = Modifier
            .width(figureWidth)
            .height(childHeight),
    ) {
        val scope = ChildArrowScope(
            figureRect = figureRectDp.toRect(),
            arrowHeadRect = arrowHeadRectDp.toRect(),
            curvedArrowRect = curvedArrowRectDp.toRect(),
            arrowStrokeWidth = arrowStrokeWidthDp.toPx(),
            arrowHeadRadius = arrowHeadRadiusDp.toPx(),
            strokeColor = strokeColor,
            drawScope = this,
        )

        scope.drawCurveArrow()
        scope.drawArrowHead()

        if (!isLastChild) {
            scope.drawArrowLine()
        }
    }
}

private fun ChildArrowScope.drawArrowHead() {
    val arrowHeadPath = Path().apply {
        moveTo(arrowHeadRect.centerRight)
        lineTo(arrowHeadRect.topLeft)
        lineTo(arrowHeadRect.bottomLeft)
        close()
    }
    val paint = Paint().apply {
        color = strokeColor
        style = PaintingStyle.Fill
        pathEffect = PathEffect.cornerPathEffect(arrowHeadRadius)
    }
    drawIntoCanvas { canvas ->
        canvas.drawOutline(
            outline = Outline.Generic(arrowHeadPath),
            paint = paint,
        )
    }
}

private fun ChildArrowScope.drawCurveArrow() {
    val curveArrowPath = Path().apply {
        moveTo(curvedArrowRect.topLeft)
        quadraticBezierTo(
            control = curvedArrowRect.bottomLeft,
            end = curvedArrowRect.bottomRight,
        )
    }
    drawPath(
        path = curveArrowPath,
        color = strokeColor,
        style = Stroke(width = arrowStrokeWidth),
    )
}

private fun ChildArrowScope.drawArrowLine() {
    drawLine(
        color = strokeColor,
        start = curvedArrowRect.topLeft,
        end = Offset(curvedArrowRect.left, figureRect.bottom),
        strokeWidth = arrowStrokeWidth,
    )
}