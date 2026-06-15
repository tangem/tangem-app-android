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

private var _ic_triangle_up_32: ImageVector? = null

val Icons.ic_triangle_up_32: ImageVector
    get() {
        if (_ic_triangle_up_32 != null) return _ic_triangle_up_32!!
        _ic_triangle_up_32 = ImageVector.Builder(
            name = "ic_triangle_up_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M24.9594 24.9995C26.2746 24.9995 27.094 23.5806 26.4313 22.4507L17.4699 7.17141C16.8124 6.05029 15.1837 6.05028 14.5262 7.17141L5.56477 22.4507C4.9021 23.5806 5.72152 24.9995 7.03667 24.9995H24.9594Z"),
            )
        }.build()
        return _ic_triangle_up_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTriangleUp32Preview() {
    Icon(
        imageVector = Icons.ic_triangle_up_32,
        contentDescription = null,
    )
}