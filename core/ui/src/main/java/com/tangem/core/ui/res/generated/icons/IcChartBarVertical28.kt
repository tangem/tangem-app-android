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

private var _ic_chart_bar_vertical_28: ImageVector? = null

val Icons.ic_chart_bar_vertical_28: ImageVector
    get() {
        if (_ic_chart_bar_vertical_28 != null) return _ic_chart_bar_vertical_28!!
        _ic_chart_bar_vertical_28 = ImageVector.Builder(
            name = "ic_chart_bar_vertical_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M22.125 3.54004C23.7128 3.54004 25 4.82722 25 6.41504V21.582C24.9998 23.1697 23.7127 24.457 22.125 24.457H17.25C17.2438 24.457 17.2376 24.4561 17.2314 24.4561H5.875C4.28718 24.4561 3 23.1689 3 21.5811V18.873C3.00013 17.2853 4.28726 15.998 5.875 15.998H9.5V12.373C9.5002 10.7854 10.7873 9.49805 12.375 9.49805H16V6.41504C16 4.82722 17.2872 3.54004 18.875 3.54004H22.125ZM18.875 6.04004C18.6679 6.04004 18.5 6.20793 18.5 6.41504V21.957H22.125C22.332 21.957 22.4998 21.789 22.5 21.582V6.41504C22.5 6.20793 22.3321 6.04004 22.125 6.04004H18.875ZM5.875 18.498C5.66797 18.498 5.50013 18.6661 5.5 18.873V21.5811C5.5 21.7882 5.66789 21.9561 5.875 21.9561H9.5V18.498H5.875ZM12.375 11.998C12.168 11.998 12.0002 12.1661 12 12.373V21.9561H16V11.998H12.375Z"),
            )
        }.build()
        return _ic_chart_bar_vertical_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartBarVertical28Preview() {
    Icon(
        imageVector = Icons.ic_chart_bar_vertical_28,
        contentDescription = null,
    )
}