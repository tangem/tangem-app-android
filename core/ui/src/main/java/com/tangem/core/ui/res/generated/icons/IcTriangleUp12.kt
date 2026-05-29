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

private var _ic_triangle_up_12: ImageVector? = null

val Icons.ic_triangle_up_12: ImageVector
    get() {
        if (_ic_triangle_up_12 != null) return _ic_triangle_up_12!!
        _ic_triangle_up_12 = ImageVector.Builder(
            name = "ic_triangle_up_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.35979 8.99988C9.85297 8.99988 10.1602 8.46778 9.91175 8.04409L6.55123 2.31434C6.30465 1.89392 5.69389 1.89392 5.44731 2.31434L2.08679 8.04409C1.83829 8.46778 2.14557 8.99988 2.63875 8.99988H9.35979Z"),
            )
        }.build()
        return _ic_triangle_up_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleUp12Preview() {
    Icon(
        imageVector = Icons.ic_triangle_up_12,
        contentDescription = null,
    )
}