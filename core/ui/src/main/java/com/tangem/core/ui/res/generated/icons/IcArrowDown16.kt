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

private var _ic_arrow_down_16: ImageVector? = null

val Icons.ic_arrow_down_16: ImageVector
    get() {
        if (_ic_arrow_down_16 != null) return _ic_arrow_down_16!!
        _ic_arrow_down_16 = ImageVector.Builder(
            name = "ic_arrow_down_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.8149 8.74725C13.0583 8.99129 13.0585 9.38713 12.8149 9.63104L8.44185 14.0041C8.19795 14.2476 7.80207 14.2474 7.55806 14.0041L3.18502 9.63104C2.94151 9.38704 2.94143 8.9912 3.18502 8.74725C3.42903 8.50336 3.82573 8.50343 4.06978 8.74725L7.37545 12.0529L7.37545 2.50018C7.37555 2.15518 7.65545 1.87532 8.00045 1.87518C8.34515 1.87567 8.62534 2.15539 8.62545 2.50018L8.62545 12.0519L11.9311 8.74725C12.1751 8.50352 12.5709 8.50348 12.8149 8.74725Z"),
            )
        }.build()
        return _ic_arrow_down_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_16,
        contentDescription = null,
    )
}