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

private var _ic_chevron_double_vertical_16: ImageVector? = null

val Icons.ic_chevron_double_vertical_16: ImageVector
    get() {
        if (_ic_chevron_double_vertical_16 != null) return _ic_chevron_double_vertical_16!!
        _ic_chevron_double_vertical_16 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.9306 9.53027C11.1738 9.28531 11.5694 9.28322 11.8144 9.52637C12.0592 9.76954 12.0604 10.1652 11.8173 10.4102L8.44232 13.8105C8.32502 13.9286 8.1654 13.9951 7.99896 13.9951C7.83254 13.9951 7.67292 13.9286 7.5556 13.8105L4.1806 10.4102C3.93746 10.1652 3.93857 9.76953 4.18353 9.52637C4.42851 9.28322 4.82416 9.28531 5.06732 9.53027L7.99896 12.4834L10.9306 9.53027Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99896 2C8.16534 2.00003 8.32504 2.06658 8.44232 2.18457L11.8173 5.58496C12.0604 5.82986 12.0591 6.22556 11.8144 6.46875C11.5694 6.7119 11.1738 6.70981 10.9306 6.46484L7.99896 3.51172L5.06732 6.46484C4.82416 6.70981 4.42851 6.7119 4.18353 6.46875C3.9387 6.22557 3.9375 5.82989 4.1806 5.58496L7.5556 2.18457C7.6729 2.06662 7.8326 2 7.99896 2Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVertical16Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_16,
        contentDescription = null,
    )
}