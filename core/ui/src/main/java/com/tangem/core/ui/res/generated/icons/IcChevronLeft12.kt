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

private var _ic_chevron_left_12: ImageVector? = null

val Icons.ic_chevron_left_12: ImageVector
    get() {
        if (_ic_chevron_left_12 != null) return _ic_chevron_left_12!!
        _ic_chevron_left_12 = ImageVector.Builder(
            name = "ic_chevron_left_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.89468 2.14332C7.08983 1.94822 7.40643 1.94842 7.60171 2.14332C7.79632 2.33861 7.79671 2.65529 7.60171 2.85035L4.45425 5.99781L7.60171 9.14527C7.79645 9.34056 7.79676 9.65721 7.60171 9.8523C7.4066 10.0472 7.08993 10.047 6.89468 9.8523L3.3937 6.35133C3.19885 6.15609 3.19873 5.83945 3.3937 5.64429L6.89468 2.14332Z"),
            )
        }.build()
        return _ic_chevron_left_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronLeft12Preview() {
    Icon(
        imageVector = Icons.ic_chevron_left_12,
        contentDescription = null,
    )
}