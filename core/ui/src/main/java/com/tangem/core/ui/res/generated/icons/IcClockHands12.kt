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

private var _ic_clock_hands_12: ImageVector? = null

val Icons.ic_clock_hands_12: ImageVector
    get() {
        if (_ic_clock_hands_12 != null) return _ic_clock_hands_12!!
        _ic_clock_hands_12 = ImageVector.Builder(
            name = "ic_clock_hands_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.33333 1.99597C6.70152 1.99597 7 2.30271 7 2.68109V6.10667C7 6.48505 6.70152 6.79178 6.33333 6.79178H3.66667C3.29848 6.79178 3 6.48505 3 6.10667C3 5.72829 3.29848 5.42155 3.66667 5.42155H5.66667V2.68109C5.66667 2.30271 5.96514 1.99597 6.33333 1.99597Z"),
            )
        }.build()
        return _ic_clock_hands_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClockHands12Preview() {
    Icon(
        imageVector = Icons.ic_clock_hands_12,
        contentDescription = null,
    )
}