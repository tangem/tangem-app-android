@file:Suppress("all")

package com.tangem.core.ui.res.generated.icons

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Auto-generated from design tokens. Do not edit manually.
 */

private var _ic_chart_line_vertical_24: ImageVector? = null

val Icons.ic_chart_line_vertical_24: ImageVector
    get() {
        if (_ic_chart_line_vertical_24 != null) return _ic_chart_line_vertical_24!!
        _ic_chart_line_vertical_24 = ImageVector.Builder(
            name = "ic_chart_line_vertical_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.66699 11.7783C10.2191 11.7786 10.667 12.2262 10.667 12.7783V19.001C10.6665 19.5527 10.2187 20.0007 9.66699 20.001C9.11511 20.0009 8.66749 19.5528 8.66699 19.001V12.7783C8.66699 12.2261 9.11481 11.7784 9.66699 11.7783ZM5 14.8887C5.5522 14.8887 5.99987 15.3365 6 15.8887V19C5.99988 19.5522 5.55221 20 5 20C4.44789 19.9999 4.00012 19.5521 4 19V15.8887C4.00013 15.3366 4.4479 14.8888 5 14.8887ZM14.333 7.88867C14.8852 7.88867 15.3329 8.3365 15.333 8.88867V19C15.3329 19.5522 14.8852 20 14.333 20C13.7809 19.9999 13.3331 19.5521 13.333 19V8.88867C13.3331 8.33657 13.7809 7.88879 14.333 7.88867ZM19 4C19.5523 4 20 4.44772 20 5V19C20 19.5523 19.5523 20 19 20C18.4478 19.9999 18 19.5522 18 19V5C18 4.44779 18.4478 4.00012 19 4Z"),
            )
        }.build()
        return _ic_chart_line_vertical_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartLineVertical24Preview() {
    Icon(
        imageVector = Icons.ic_chart_line_vertical_24,
        contentDescription = null,
    )
}