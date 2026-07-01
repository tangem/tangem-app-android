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

private var _ic_triangle_down_28: ImageVector? = null

val Icons.ic_triangle_down_28: ImageVector
    get() {
        if (_ic_triangle_down_28 != null) return _ic_triangle_down_28!!
        _ic_triangle_down_28 = ImageVector.Builder(
            name = "ic_triangle_down_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.8412 4.99902C22.992 4.99902 23.709 6.24057 23.1291 7.2292L15.2879 20.5986C14.7126 21.5796 13.2874 21.5796 12.7121 20.5986L4.87088 7.2292C4.29105 6.24058 5.00804 4.99902 6.15879 4.99902H21.8412Z"),
            )
        }.build()
        return _ic_triangle_down_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleDown28Preview() {
    Icon(
        imageVector = Icons.ic_triangle_down_28,
        contentDescription = null,
    )
}