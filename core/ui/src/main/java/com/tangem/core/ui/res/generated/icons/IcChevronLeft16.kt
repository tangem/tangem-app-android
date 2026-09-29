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

private var _ic_chevron_left_16: ImageVector? = null

val Icons.ic_chevron_left_16: ImageVector
    get() {
        if (_ic_chevron_left_16 != null) return _ic_chevron_left_16!!
        _ic_chevron_left_16 = ImageVector.Builder(
            name = "ic_chevron_left_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.1167 3.18182C9.36075 2.93815 9.75749 2.93803 10.0015 3.18182C10.2452 3.42566 10.2448 3.82152 10.0015 4.06561L6.0708 7.99725L10.0015 11.9269C10.2454 12.1709 10.2452 12.5676 10.0015 12.8117C9.7574 13.0557 9.36077 13.0557 9.1167 12.8117L4.74463 8.43866C4.50075 8.19459 4.50068 7.7989 4.74463 7.55487L9.1167 3.18182Z"),
            )
        }.build()
        return _ic_chevron_left_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronLeft16Preview() {
    Icon(
        imageVector = Icons.ic_chevron_left_16,
        contentDescription = null,
    )
}