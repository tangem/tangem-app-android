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

private var _ic_chart_bar_vertical_20: ImageVector? = null

val Icons.ic_chart_bar_vertical_20: ImageVector
    get() {
        if (_ic_chart_bar_vertical_20 != null) return _ic_chart_bar_vertical_20!!
        _ic_chart_bar_vertical_20 = ImageVector.Builder(
            name = "ic_chart_bar_vertical_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.2949 3.25C16.2935 3.25041 17.1034 4.05997 17.1035 5.05859V14.9414C17.1034 15.9401 16.2935 16.7496 15.2949 16.75H7.90234C7.89585 16.7502 7.88934 16.751 7.88281 16.751H4.70605C3.70734 16.7509 2.89675 15.9401 2.89648 14.9414V13.1768C2.89661 12.178 3.70725 11.3683 4.70605 11.3682H7.13281V8.94141C7.13294 7.9426 7.9426 7.13294 8.94141 7.13281H11.3682V5.05859C11.3683 4.05979 12.178 3.25013 13.1768 3.25H15.2949ZM4.70605 12.8682C4.53568 12.8683 4.39759 13.0064 4.39746 13.1768V14.9414C4.39772 15.1117 4.53576 15.2509 4.70605 15.251H7.13281V12.8682H4.70605ZM8.94141 8.63281C8.77103 8.63294 8.63294 8.77103 8.63281 8.94141V15.25H11.3682V8.63281H8.94141ZM13.1768 4.75C13.0064 4.75013 12.8683 4.88822 12.8682 5.05859V15.25H15.2949C15.4651 15.2496 15.6034 15.1116 15.6035 14.9414V5.05859C15.6034 4.8884 15.4651 4.75041 15.2949 4.75H13.1768Z"),
            )
        }.build()
        return _ic_chart_bar_vertical_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartBarVertical20Preview() {
    Icon(
        imageVector = Icons.ic_chart_bar_vertical_20,
        contentDescription = null,
    )
}