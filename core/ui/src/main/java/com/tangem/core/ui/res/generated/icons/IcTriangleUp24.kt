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

private var _ic_triangle_up_24: ImageVector? = null

val Icons.ic_triangle_up_24: ImageVector
    get() {
        if (_ic_triangle_up_24 != null) return _ic_triangle_up_24!!
        _ic_triangle_up_24 = ImageVector.Builder(
            name = "ic_triangle_up_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.7196 18.9998C19.7059 18.9998 20.3205 17.9356 19.8235 17.0882L13.1025 5.62868C12.6093 4.78784 11.3878 4.78784 10.8946 5.62868L4.17358 17.0882C3.67658 17.9356 4.29114 18.9998 5.2775 18.9998H18.7196Z"),
            )
        }.build()
        return _ic_triangle_up_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleUp24Preview() {
    Icon(
        imageVector = Icons.ic_triangle_up_24,
        contentDescription = null,
    )
}