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

private var _ic_chart_bar_vertical_24: ImageVector? = null

val Icons.ic_chart_bar_vertical_24: ImageVector
    get() {
        if (_ic_chart_bar_vertical_24 != null) return _ic_chart_bar_vertical_24!!
        _ic_chart_bar_vertical_24 = ImageVector.Builder(
            name = "ic_chart_bar_vertical_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19.0596 3C20.3913 3.00029 21.4717 4.08031 21.4717 5.41211V18.5879C21.4717 19.9197 20.3913 20.9997 19.0596 21H4.94141C3.60943 21 2.5293 19.9199 2.5293 18.5879V16.2354C2.52943 14.9035 3.60951 13.8232 4.94141 13.8232H8.17676V10.5889C8.17676 9.25689 9.25689 8.17676 10.5889 8.17676H13.8242V5.41211C13.8242 4.08013 14.9043 3 16.2363 3H19.0596ZM4.94141 15.8232C4.71408 15.8232 4.52943 16.0081 4.5293 16.2354V18.5879C4.5293 18.8153 4.71399 19 4.94141 19H8.17676V15.8232H4.94141ZM10.5889 10.1768C10.3615 10.1768 10.1768 10.3615 10.1768 10.5889V19H13.8242V10.1768H10.5889ZM16.2363 5C16.0089 5 15.8242 5.1847 15.8242 5.41211V19H19.0596C19.2867 18.9997 19.4717 18.8151 19.4717 18.5879V5.41211C19.4717 5.18488 19.2867 5.0003 19.0596 5H16.2363Z"),
            )
        }.build()
        return _ic_chart_bar_vertical_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartBarVertical24Preview() {
    Icon(
        imageVector = Icons.ic_chart_bar_vertical_24,
        contentDescription = null,
    )
}