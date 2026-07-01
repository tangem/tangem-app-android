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

private var _ic_chevron_double_vertical_20: ImageVector? = null

val Icons.ic_chevron_double_vertical_20: ImageVector
    get() {
        if (_ic_chevron_double_vertical_20 != null) return _ic_chevron_double_vertical_20!!
        _ic_chevron_double_vertical_20 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.2221 11.9663C13.5149 11.6737 13.9898 11.6737 14.2827 11.9663C14.5755 12.2591 14.5755 12.7349 14.2827 13.0278L10.5307 16.7797C10.2378 17.0726 9.76208 17.0726 9.46918 16.7797L5.71723 13.0278C5.42434 12.7349 5.42434 12.2591 5.71723 11.9663C6.01006 11.6737 6.48496 11.6737 6.77777 11.9663L10.0004 15.1889L13.2221 11.9663Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.46918 3.22016C9.76207 2.92726 10.2378 2.92726 10.5307 3.22016L14.2827 6.97211C14.5752 7.26503 14.5754 7.74086 14.2827 8.03363C13.9899 8.32606 13.515 8.32585 13.2221 8.03363L10.0004 4.81098L6.77777 8.03363C6.48506 8.32605 6.01008 8.32585 5.71723 8.03363C5.42435 7.74075 5.42437 7.26501 5.71723 6.97211L9.46918 3.22016Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVertical20Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_20,
        contentDescription = null,
    )
}