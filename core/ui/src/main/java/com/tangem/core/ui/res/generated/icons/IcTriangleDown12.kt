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

private var _ic_triangle_down_12: ImageVector? = null

val Icons.ic_triangle_down_12: ImageVector
    get() {
        if (_ic_triangle_down_12 != null) return _ic_triangle_down_12!!
        _ic_triangle_down_12 = ImageVector.Builder(
            name = "ic_triangle_down_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.36052 2.5C9.8537 2.5 10.161 3.03209 9.91248 3.45579L6.55196 9.18554C6.30538 9.60596 5.69462 9.60596 5.44804 9.18554L2.08752 3.45579C1.83902 3.03209 2.1463 2.5 2.63948 2.5H9.36052Z"),
            )
        }.build()
        return _ic_triangle_down_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleDown12Preview() {
    Icon(
        imageVector = Icons.ic_triangle_down_12,
        contentDescription = null,
    )
}