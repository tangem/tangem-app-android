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

private var _ic_triangle_up_16: ImageVector? = null

val Icons.ic_triangle_up_16: ImageVector
    get() {
        if (_ic_triangle_up_16 != null) return _ic_triangle_up_16!!
        _ic_triangle_up_16 = ImageVector.Builder(
            name = "ic_triangle_up_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.4797 11.9995C13.1373 11.9995 13.547 11.2901 13.2157 10.7251L8.73497 3.08546C8.4062 2.5249 7.59185 2.5249 7.26308 3.08546L2.78238 10.7251C2.45105 11.2901 2.86076 11.9995 3.51833 11.9995H12.4797Z"),
            )
        }.build()
        return _ic_triangle_up_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleUp16Preview() {
    Icon(
        imageVector = Icons.ic_triangle_up_16,
        contentDescription = null,
    )
}