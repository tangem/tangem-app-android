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

private var _ic_arrow_left_16: ImageVector? = null

val Icons.ic_arrow_left_16: ImageVector
    get() {
        if (_ic_arrow_left_16 != null) return _ic_arrow_left_16!!
        _ic_arrow_left_16 = ImageVector.Builder(
            name = "ic_arrow_left_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.25277 3.18508C7.00873 2.94166 6.61289 2.94146 6.36898 3.18508L1.99593 7.55813C1.75241 7.80203 1.75258 8.19791 1.99593 8.44192L6.36898 12.815C6.61298 13.0585 7.00881 13.0586 7.25277 12.815C7.49666 12.571 7.49659 12.1743 7.25277 11.9302L3.9471 8.62453H13.4998C13.8448 8.62443 14.1247 8.34453 14.1248 7.99953C14.1244 7.65483 13.8446 7.37464 13.4998 7.37453H3.94808L7.25277 4.06887C7.4965 3.82484 7.49654 3.42909 7.25277 3.18508Z"),
            )
        }.build()
        return _ic_arrow_left_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowLeft16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_left_16,
        contentDescription = null,
    )
}