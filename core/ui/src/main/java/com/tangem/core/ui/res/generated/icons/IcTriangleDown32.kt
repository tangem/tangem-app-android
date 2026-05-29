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

private var _ic_triangle_down_32: ImageVector? = null

val Icons.ic_triangle_down_32: ImageVector
    get() {
        if (_ic_triangle_down_32 != null) return _ic_triangle_down_32!!
        _ic_triangle_down_32 = ImageVector.Builder(
            name = "ic_triangle_down_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.9614 8.99902C26.2765 8.99902 27.0959 10.4179 26.4333 11.5478L17.4719 26.8271C16.8144 27.9482 15.1856 27.9483 14.5281 26.8271L5.56672 11.5478C4.90406 10.4179 5.72347 8.99902 7.03862 8.99902H24.9614Z"),
            )
        }.build()
        return _ic_triangle_down_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleDown32Preview() {
    Icon(
        imageVector = Icons.ic_triangle_down_32,
        contentDescription = null,
    )
}