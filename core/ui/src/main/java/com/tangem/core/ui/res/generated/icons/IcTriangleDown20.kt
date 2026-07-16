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

private var _ic_triangle_down_20: ImageVector? = null

val Icons.ic_triangle_down_20: ImageVector
    get() {
        if (_ic_triangle_down_20 != null) return _ic_triangle_down_20!!
        _ic_triangle_down_20 = ImageVector.Builder(
            name = "ic_triangle_down_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.6009 4.99902C16.4228 4.99902 16.935 5.88585 16.5208 6.59201L10.9199 16.1416C10.509 16.8423 9.49103 16.8423 9.08007 16.1416L3.4792 6.59201C3.06504 5.88585 3.57717 4.99902 4.39914 4.99902H15.6009Z"),
            )
        }.build()
        return _ic_triangle_down_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleDown20Preview() {
    Icon(
        imageVector = Icons.ic_triangle_down_20,
        contentDescription = null,
    )
}