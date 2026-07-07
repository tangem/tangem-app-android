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

private var _ic_arrow_left_12: ImageVector? = null

val Icons.ic_arrow_left_12: ImageVector
    get() {
        if (_ic_arrow_left_12 != null) return _ic_arrow_left_12!!
        _ic_arrow_left_12 = ImageVector.Builder(
            name = "ic_arrow_left_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.64638 2.14623C4.84125 1.95157 5.15811 1.95224 5.35341 2.14623C5.54826 2.34146 5.54835 2.6581 5.35341 2.85327L2.70595 5.50073H10.497C10.7726 5.50073 10.9962 5.72523 10.997 6.00073C10.9966 6.27659 10.7729 6.50073 10.497 6.50073H2.70595L5.35341 9.14819C5.54837 9.34338 5.54834 9.66001 5.35341 9.85522C5.15819 10.0501 4.84154 10.0502 4.64638 9.85522L1.1454 6.35424C0.951454 6.159 0.950881 5.8421 1.1454 5.64721L4.64638 2.14623Z"),
            )
        }.build()
        return _ic_arrow_left_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowLeft12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_left_12,
        contentDescription = null,
    )
}