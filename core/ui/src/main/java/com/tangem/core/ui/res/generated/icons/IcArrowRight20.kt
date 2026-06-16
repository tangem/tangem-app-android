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

private var _ic_arrow_right_20: ImageVector? = null

val Icons.ic_arrow_right_20: ImageVector
    get() {
        if (_ic_arrow_right_20 != null) return _ic_arrow_right_20!!
        _ic_arrow_right_20 = ImageVector.Builder(
            name = "ic_arrow_right_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.4874 3.21724C10.7802 2.92513 11.2553 2.925 11.548 3.21724L17.799 9.46822C18.0916 9.76094 18.0913 10.2368 17.799 10.5297L11.548 16.7807C11.2551 17.0735 10.7803 17.0734 10.4874 16.7807C10.195 16.4878 10.1947 16.0129 10.4874 15.7202L15.4581 10.7485H2.75012C2.336 10.7484 2.00012 10.4126 2.00012 9.99849C2.00087 9.58499 2.33647 9.24861 2.75012 9.24849H15.4572L10.4874 4.27779C10.195 3.98486 10.1947 3.50999 10.4874 3.21724Z"),
            )
        }.build()
        return _ic_arrow_right_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRight20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_right_20,
        contentDescription = null,
    )
}