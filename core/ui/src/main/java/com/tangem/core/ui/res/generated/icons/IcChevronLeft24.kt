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

private var _ic_chevron_left_24: ImageVector? = null

val Icons.ic_chevron_left_24: ImageVector
    get() {
        if (_ic_chevron_left_24 != null) return _ic_chevron_left_24!!
        _ic_chevron_left_24 = ImageVector.Builder(
            name = "ic_chevron_left_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.791 4.29216C14.1813 3.90195 14.8145 3.9023 15.2051 4.29216C15.5954 4.68265 15.5954 5.31573 15.2051 5.70622L8.91309 11.9982L15.2051 18.2902C15.5955 18.6807 15.5955 19.3137 15.2051 19.7043C14.8146 20.0947 14.1815 20.0947 13.791 19.7043L6.79199 12.7052C6.6049 12.5178 6.49908 12.2631 6.49902 11.9982C6.49922 11.7333 6.6047 11.4785 6.79199 11.2912L13.791 4.29216Z"),
            )
        }.build()
        return _ic_chevron_left_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronLeft24Preview() {
    Icon(
        imageVector = Icons.ic_chevron_left_24,
        contentDescription = null,
    )
}