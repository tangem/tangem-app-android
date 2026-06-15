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

private var _ic_triangle_up_20: ImageVector? = null

val Icons.ic_triangle_up_20: ImageVector
    get() {
        if (_ic_triangle_up_20 != null) return _ic_triangle_up_20!!
        _ic_triangle_up_20 = ImageVector.Builder(
            name = "ic_triangle_up_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.5996 15.9996C16.4216 15.9996 16.9337 15.1128 16.5196 14.4067L10.9187 4.85707C10.5078 4.15637 9.48981 4.15637 9.07885 4.85707L3.47798 14.4067C3.06381 15.1128 3.57595 15.9996 4.39792 15.9996H15.5996Z"),
            )
        }.build()
        return _ic_triangle_up_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleUp20Preview() {
    Icon(
        imageVector = Icons.ic_triangle_up_20,
        contentDescription = null,
    )
}