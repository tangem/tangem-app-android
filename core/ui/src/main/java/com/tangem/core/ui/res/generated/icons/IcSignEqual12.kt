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

private var _ic_sign_equal_12: ImageVector? = null

val Icons.ic_sign_equal_12: ImageVector
    get() {
        if (_ic_sign_equal_12 != null) return _ic_sign_equal_12!!
        _ic_sign_equal_12 = ImageVector.Builder(
            name = "ic_sign_equal_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.5 7.5C9.77614 7.5 10 7.72386 10 8C10 8.27614 9.77614 8.5 9.5 8.5H2.5C2.22386 8.5 2 8.27614 2 8C2 7.72386 2.22386 7.5 2.5 7.5H9.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.5 3.5C9.77614 3.5 10 3.72386 10 4C10 4.27614 9.77614 4.5 9.5 4.5H2.5C2.22386 4.5 2 4.27614 2 4C2 3.72386 2.22386 3.5 2.5 3.5H9.5Z"),
            )
        }.build()
        return _ic_sign_equal_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignEqual12Preview() {
    Icon(
        imageVector = Icons.ic_sign_equal_12,
        contentDescription = null,
    )
}