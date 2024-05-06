package com.tangem.managetokens.presentation.managetokens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * A chart with a solid line and gradient underneath. It can accept values of any range and number.
 * If the last value is bigger or equal to the first, the chart is of accent color, otherwise it's warning color.
 *
 * @param values a list of float values for a chart.
 **/
@Composable
fun PriceChangesChart(values: ImmutableList<Float>, modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        if (values.size < 2) return // escape without drawing when there are not enough points

        val lineColor = if (values.last() >= values.first()) {
            TangemTheme.colors.icon.accent
        } else {
            TangemTheme.colors.icon.warning
        }
        val gradient = Brush.verticalGradient(
            colors = listOf(lineColor.copy(alpha = 0.21f), lineColor.copy(alpha = 0.0f)),
        )
        Chart(list = values, lineColor = lineColor, gradient = gradient, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Chart(list: ImmutableList<Float>, lineColor: Color, gradient: Brush, modifier: Modifier = Modifier) {
    val max = list.max()
    val min = list.min()
    val zipList: List<Pair<Float, Float>> = list.zipWithNext()

    for (pair in zipList) {
        val fromValuePercentage = getValuePercentageForRange(pair.first, max, min)
        val toValuePercentage = getValuePercentageForRange(pair.second, max, min)

        Canvas(
            modifier = modifier.fillMaxHeight(),
            onDraw = {
                val fromPoint = Offset(x = 0f, y = size.height.times(1 - fromValuePercentage))
                val toPoint = Offset(x = size.width, y = size.height.times(1 - toValuePercentage))

                val path = drawChartLineAndCreatePath(fromPoint = fromPoint, toPoint = toPoint, lineColor = lineColor)

                fillChart(
                    path = path,
                    fromPoint = fromPoint,
                    toPoint = toPoint,
                    size = size,
                    gradient = gradient,
                )
            },
        )
    }
}

private fun DrawScope.drawChartLineAndCreatePath(fromPoint: Offset, toPoint: Offset, lineColor: Color): Path {
    val path = Path()
    path.moveTo(fromPoint.x, fromPoint.y)
    path.lineTo(toPoint.x, toPoint.y)
    drawPath(
        path = path,
        color = lineColor,
        style = Stroke(width = 1f),
    )
    return path
}

private fun DrawScope.fillChart(path: Path, fromPoint: Offset, toPoint: Offset, size: Size, gradient: Brush) {
    path.lineTo(toPoint.x, size.height)
    path.lineTo(fromPoint.x, size.height)
    path.lineTo(0f, fromPoint.y)
    drawPath(
        path = path,
        brush = gradient,
    )
}

private fun getValuePercentageForRange(value: Float, max: Float, min: Float): Float {
    return if (max == min) { // to draw a straight line when all values are the same
        val modifiedMax = max + 1
        val modifiedMin = min - 1
        (value - modifiedMin) / (modifiedMax - modifiedMin)
    } else {
        (value - min) / (max - min)
    }
}

@Preview(widthDp = 150, heightDp = 150, showBackground = true)
@Composable
private fun Chart_Positive_Preview() {
    TangemThemePreview(isDark = true) {
        PriceChangesChart(
            persistentListOf(1f, 2f, 4f, 1f, 5f),
        )
    }
}

@Preview(widthDp = 150, heightDp = 150, showBackground = true)
@Composable
private fun Chart_Negative_Preview() {
    TangemThemePreview(isDark = true) {
        PriceChangesChart(
            persistentListOf(10f, 2f, 4f, 1f, 5f),
        )
    }
}

@Preview(widthDp = 150, heightDp = 150, showBackground = true)
@Composable
private fun Chart_Neutral_Preview() {
    TangemThemePreview(isDark = true) {
        PriceChangesChart(
            persistentListOf(5f, 2f, 4f, 1f, 5f),
        )
    }
}
