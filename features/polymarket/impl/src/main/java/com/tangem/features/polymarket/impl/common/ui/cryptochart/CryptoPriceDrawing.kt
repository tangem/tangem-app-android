@file:Suppress("MagicNumber")

package com.tangem.features.polymarket.impl.common.ui.cryptochart

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.Point
import com.tangem.features.polymarket.impl.common.ui.cryptochart.PolymarketCryptoChart.TimeRange

internal fun DrawScope.drawCryptoPrice(points: List<Point>, range: TimeRange, scale: CryptoChartScale, color: Color) {
    val path = Path()
    var segmentStart = 0f
    var lastX = 0f
    val fill = Brush.verticalGradient(listOf(color.copy(alpha = 0.24f), Color.Transparent))
    fun finishSegment() {
        drawPath(path, color, style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        path.lineTo(lastX, size.height)
        path.lineTo(segmentStart, size.height)
        path.close()
        drawPath(path, fill)
        path.reset()
    }
    clipRect {
        points.forEachIndexed { index, point ->
            val x = range.x(point.timestampMillis) * size.width
            val y = scale.y(point.price) * size.height
            if (index == 0 || point.isSegmentStart) {
                if (index > 0) finishSegment()
                path.moveTo(x, y)
                segmentStart = x
            } else {
                path.lineTo(x, y)
            }
            lastX = x
        }
        if (points.isNotEmpty()) finishSegment()
    }
    points.lastOrNull()?.let { latest ->
        drawCircle(
            color,
            3.dp.toPx(),
            Offset(range.x(latest.timestampMillis) * size.width, scale.y(latest.price) * size.height),
        )
    }
}