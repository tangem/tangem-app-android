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

private var _ic_triangle_down_16: ImageVector? = null

val Icons.ic_triangle_down_16: ImageVector
    get() {
        if (_ic_triangle_down_16 != null) return _ic_triangle_down_16!!
        _ic_triangle_down_16 = ImageVector.Builder(
            name = "ic_triangle_down_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.4807 3.99951C13.1383 3.99951 13.548 4.70897 13.2166 5.2739L8.73595 12.9136C8.40718 13.4741 7.59282 13.4741 7.26405 12.9136L2.78336 5.2739C2.45203 4.70897 2.86174 3.99951 3.51931 3.99951H12.4807Z"),
            )
        }.build()
        return _ic_triangle_down_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleDown16Preview() {
    Icon(
        imageVector = Icons.ic_triangle_down_16,
        contentDescription = null,
    )
}