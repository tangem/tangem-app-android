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

private var _ic_arrow_right_16: ImageVector? = null

val Icons.ic_arrow_right_16: ImageVector
    get() {
        if (_ic_arrow_right_16 != null) return _ic_arrow_right_16!!
        _ic_arrow_right_16 = ImageVector.Builder(
            name = "ic_arrow_right_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.74725 3.18508C8.99129 2.94166 9.38713 2.94146 9.63104 3.18508L14.0041 7.55813C14.2476 7.80203 14.2474 8.19791 14.0041 8.44192L9.63104 12.815C9.38704 13.0585 8.9912 13.0586 8.74725 12.815C8.50336 12.571 8.50343 12.1743 8.74725 11.9302L12.0529 8.62453H2.50018C2.15518 8.62443 1.87532 8.34453 1.87518 7.99953C1.87567 7.65483 2.15539 7.37464 2.50018 7.37453H12.0519L8.74725 4.06887C8.50352 3.82484 8.50348 3.42909 8.74725 3.18508Z"),
            )
        }.build()
        return _ic_arrow_right_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRight16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_right_16,
        contentDescription = null,
    )
}