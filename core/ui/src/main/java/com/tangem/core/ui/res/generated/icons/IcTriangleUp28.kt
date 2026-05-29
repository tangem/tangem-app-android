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

private var _ic_triangle_up_28: ImageVector? = null

val Icons.ic_triangle_up_28: ImageVector
    get() {
        if (_ic_triangle_up_28 != null) return _ic_triangle_up_28!!
        _ic_triangle_up_28 = ImageVector.Builder(
            name = "ic_triangle_up_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.8395 21.9994C22.9903 21.9994 23.7072 20.7578 23.1274 19.7692L15.2862 6.3998C14.7109 5.41882 13.2857 5.41882 12.7104 6.3998L4.86917 19.7692C4.28934 20.7578 5.00633 21.9994 6.15708 21.9994H21.8395Z"),
            )
        }.build()
        return _ic_triangle_up_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleUp28Preview() {
    Icon(
        imageVector = Icons.ic_triangle_up_28,
        contentDescription = null,
    )
}