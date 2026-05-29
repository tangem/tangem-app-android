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

private var _ic_arrow_right_28: ImageVector? = null

val Icons.ic_arrow_right_28: ImageVector
    get() {
        if (_ic_arrow_right_28 != null) return _ic_arrow_right_28!!
        _ic_arrow_right_28 = ImageVector.Builder(
            name = "ic_arrow_right_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.9832 4.36926C15.4679 3.87819 16.2593 3.87231 16.7508 4.35657L25.6278 13.1075C25.8658 13.3423 25.9997 13.6638 25.9998 13.9982C25.9997 14.3322 25.8655 14.6531 25.6278 14.8878L16.7508 23.6398C16.2593 24.1238 15.4678 24.1182 14.9832 23.6271C14.499 23.1356 14.5048 22.3431 14.9959 21.8585L21.701 15.2482H3.24985C2.5599 15.2479 2.00017 14.6881 1.99985 13.9982C2.00016 13.3082 2.5599 12.7484 3.24985 12.7482H21.702L14.9959 6.13684C14.5048 5.6523 14.4991 4.86082 14.9832 4.36926Z"),
            )
        }.build()
        return _ic_arrow_right_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRight28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_right_28,
        contentDescription = null,
    )
}