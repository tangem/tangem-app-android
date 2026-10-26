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

private var _ic_chart_ascending_24: ImageVector? = null

val Icons.ic_chart_ascending_24: ImageVector
    get() {
        if (_ic_chart_ascending_24 != null) return _ic_chart_ascending_24!!
        _ic_chart_ascending_24 = ImageVector.Builder(
            name = "ic_chart_ascending_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.99805 3.57178C4.55033 3.57178 4.99805 4.01949 4.99805 4.57178V18.4282H20.001C20.5533 18.4282 21.001 18.8759 21.001 19.4282C21.001 19.9805 20.5533 20.4282 20.001 20.4282H3.99805C3.44576 20.4282 2.99805 19.9805 2.99805 19.4282V4.57178C2.99805 4.01949 3.44576 3.57178 3.99805 3.57178Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.0625 6.74463C20.1066 6.7473 20.1508 6.75175 20.1943 6.76025C20.2492 6.77094 20.3022 6.78689 20.3535 6.80615C20.3616 6.80919 20.3699 6.81169 20.3779 6.81494L20.3916 6.8208C20.7504 6.97267 21.0028 7.32773 21.0029 7.7417V9.97998C21.0027 10.5317 20.5546 10.9795 20.0029 10.98C19.5053 10.98 19.0924 10.6162 19.0156 10.1401L15.708 13.4487C14.9895 14.1676 13.826 14.1654 13.1084 13.4478L11.4258 11.7651L7.28516 15.9067C6.89469 16.297 6.26156 16.297 5.87109 15.9067C5.48059 15.5162 5.48058 14.8822 5.87109 14.4917L10.125 10.2378L10.2646 10.1118C10.9865 9.52294 12.052 9.56358 12.7256 10.2378L14.4072 11.9194L17.6006 8.72705C17.1262 8.64888 16.7646 8.23819 16.7646 7.7417C16.7648 7.18953 17.2124 6.7417 17.7646 6.7417H20.0029C20.0229 6.74172 20.0428 6.74345 20.0625 6.74463Z"),
            )
        }.build()
        return _ic_chart_ascending_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChartAscending24Preview() {
    Icon(
        imageVector = Icons.ic_chart_ascending_24,
        contentDescription = null,
    )
}