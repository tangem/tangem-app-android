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

private var _ic_chevron_double_vertical_collapse_20: ImageVector? = null

val Icons.ic_chevron_double_vertical_collapse_20: ImageVector
    get() {
        if (_ic_chevron_double_vertical_collapse_20 != null) return _ic_chevron_double_vertical_collapse_20!!
        _ic_chevron_double_vertical_collapse_20 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_collapse_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.2221 16.78C13.5149 17.0725 13.9898 17.0725 14.2827 16.78C14.5755 16.4871 14.5755 16.0113 14.2827 15.7184L10.5307 11.9665C10.2378 11.6736 9.76208 11.6737 9.46918 11.9665L5.71723 15.7184C5.42434 16.0113 5.42434 16.4871 5.71723 16.78C6.01006 17.0725 6.48496 17.0725 6.77777 16.78L10.0004 13.5573L13.2221 16.78Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.46918 8.0332C9.76207 8.3261 10.2378 8.3261 10.5307 8.0332L14.2827 4.28125C14.5752 3.98833 14.5754 3.5125 14.2827 3.21973C13.9899 2.9273 13.515 2.92751 13.2221 3.21973L10.0004 6.44238L6.77777 3.21973C6.48506 2.92731 6.01008 2.92751 5.71723 3.21973C5.42435 3.51261 5.42437 3.98835 5.71723 4.28125L9.46918 8.0332Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_collapse_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVerticalCollapse20Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_collapse_20,
        contentDescription = null,
    )
}