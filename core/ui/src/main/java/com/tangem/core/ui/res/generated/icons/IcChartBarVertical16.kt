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

private var _ic_chart_bar_vertical_16: ImageVector? = null

val Icons.ic_chart_bar_vertical_16: ImageVector
    get() {
        if (_ic_chart_bar_vertical_16 != null) return _ic_chart_bar_vertical_16!!
        _ic_chart_bar_vertical_16 = ImageVector.Builder(
            name = "ic_chart_bar_vertical_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.75 3.125C12.5094 3.125 13.125 3.74061 13.125 4.5V11.5C13.125 12.2594 12.5094 12.875 11.75 12.875H4.25C3.49061 12.875 2.875 12.2594 2.875 11.5V10.25C2.875 9.49061 3.49061 8.875 4.25 8.875H5.875V7.25C5.875 6.49061 6.49061 5.875 7.25 5.875H8.875V4.5C8.875 3.74061 9.49061 3.125 10.25 3.125H11.75ZM4.25 10.125C4.18096 10.125 4.125 10.181 4.125 10.25V11.5C4.125 11.569 4.18096 11.625 4.25 11.625H5.875V10.125H4.25ZM7.25 7.125C7.18096 7.125 7.125 7.18096 7.125 7.25V11.625H8.875V7.125H7.25ZM10.25 4.375C10.181 4.375 10.125 4.43096 10.125 4.5V11.625H11.75C11.819 11.625 11.875 11.569 11.875 11.5V4.5C11.875 4.43096 11.819 4.375 11.75 4.375H10.25Z"),
            )
        }.build()
        return _ic_chart_bar_vertical_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartBarVertical16Preview() {
    Icon(
        imageVector = Icons.ic_chart_bar_vertical_16,
        contentDescription = null,
    )
}