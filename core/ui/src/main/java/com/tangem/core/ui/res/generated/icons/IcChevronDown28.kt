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

private var _ic_chevron_down_28: ImageVector? = null

val Icons.ic_chevron_down_28: ImageVector
    get() {
        if (_ic_chevron_down_28 != null) return _ic_chevron_down_28!!
        _ic_chevron_down_28 = ImageVector.Builder(
            name = "ic_chevron_down_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.8688 9.9924C22.357 9.50442 23.1483 9.50432 23.6364 9.9924C24.1239 10.4805 24.1242 11.272 23.6364 11.76L14.8844 20.5119C14.6502 20.7458 14.3317 20.877 14.0006 20.8772C13.6695 20.877 13.3511 20.7459 13.1168 20.5119L4.36586 11.76C3.87779 11.2719 3.87789 10.4806 4.36586 9.9924C4.85402 9.50435 5.64532 9.5043 6.13344 9.9924L14.0006 17.8596L21.8688 9.9924Z"),
            )
        }.build()
        return _ic_chevron_down_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDown28Preview() {
    Icon(
        imageVector = Icons.ic_chevron_down_28,
        contentDescription = null,
    )
}