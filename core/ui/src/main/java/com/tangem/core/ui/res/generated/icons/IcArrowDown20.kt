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

private var _ic_arrow_down_20: ImageVector? = null

val Icons.ic_arrow_down_20: ImageVector
    get() {
        if (_ic_arrow_down_20 != null) return _ic_arrow_down_20!!
        _ic_arrow_down_20 = ImageVector.Builder(
            name = "ic_arrow_down_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.7808 10.4873C17.0729 10.7801 17.073 11.2551 16.7808 11.5479L10.5298 17.7988C10.2371 18.0915 9.76117 18.0912 9.46825 17.7988L3.21728 11.5479C2.92445 11.255 2.92458 10.7802 3.21728 10.4873C3.51021 10.1948 3.98508 10.1946 4.27782 10.4873L9.2495 15.458L9.2495 2.75C9.24962 2.33588 9.58536 2 9.9995 2C10.413 2.00075 10.7494 2.33635 10.7495 2.75L10.7495 15.457L15.7202 10.4873C16.0131 10.1948 16.488 10.1946 16.7808 10.4873Z"),
            )
        }.build()
        return _ic_arrow_down_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_20,
        contentDescription = null,
    )
}