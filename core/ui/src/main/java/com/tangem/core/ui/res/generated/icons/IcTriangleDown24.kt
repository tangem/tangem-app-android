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

private var _ic_triangle_down_24: ImageVector? = null

val Icons.ic_triangle_down_24: ImageVector
    get() {
        if (_ic_triangle_down_24 != null) return _ic_triangle_down_24!!
        _ic_triangle_down_24 = ImageVector.Builder(
            name = "ic_triangle_down_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.721 4.99902C19.7074 4.99902 20.322 6.06321 19.825 6.9106L13.1039 18.3701C12.6108 19.2109 11.3892 19.2109 10.8961 18.3701L4.17504 6.9106C3.67804 6.06321 4.2926 4.99902 5.27896 4.99902H18.721Z"),
            )
        }.build()
        return _ic_triangle_down_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleDown24Preview() {
    Icon(
        imageVector = Icons.ic_triangle_down_24,
        contentDescription = null,
    )
}