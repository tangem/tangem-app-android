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

private var _ic_arrow_down_12: ImageVector? = null

val Icons.ic_arrow_down_12: ImageVector
    get() {
        if (_ic_arrow_down_12 != null) return _ic_arrow_down_12!!
        _ic_arrow_down_12 = ImageVector.Builder(
            name = "ic_arrow_down_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.5 2L5.5 9.04297L2.35352 5.89648C2.15825 5.70122 1.84175 5.70122 1.64649 5.89648C1.45122 6.09175 1.45122 6.40825 1.64649 6.60352L5.64648 10.6035L5.72266 10.666C5.80419 10.7204 5.90056 10.75 6 10.75C6.13261 10.75 6.25975 10.6973 6.35352 10.6035L10.3535 6.60352C10.5488 6.40826 10.5488 6.09175 10.3535 5.89649C10.1583 5.70122 9.84175 5.70122 9.64649 5.89649L6.5 9.04297L6.5 2C6.5 1.72386 6.27614 1.5 6 1.5C5.72386 1.5 5.5 1.72386 5.5 2Z"),
            )
        }.build()
        return _ic_arrow_down_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_12,
        contentDescription = null,
    )
}