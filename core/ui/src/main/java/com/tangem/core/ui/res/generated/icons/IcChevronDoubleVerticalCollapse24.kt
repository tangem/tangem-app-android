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

private var _ic_chevron_double_vertical_collapse_24: ImageVector? = null

val Icons.ic_chevron_double_vertical_collapse_24: ImageVector
    get() {
        if (_ic_chevron_double_vertical_collapse_24 != null) return _ic_chevron_double_vertical_collapse_24!!
        _ic_chevron_double_vertical_collapse_24 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_collapse_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.2929 20.7071C16.6834 21.0976 17.3164 21.0976 17.707 20.7071C18.0974 20.3166 18.0974 19.6836 17.707 19.2931L12.707 14.2931C12.5194 14.1055 12.2651 14.0001 11.9999 14.0001C11.7347 14.0001 11.4804 14.1056 11.2929 14.2931L6.29289 19.2931C5.90237 19.6836 5.90237 20.3166 6.29289 20.7071C6.68342 21.0976 7.31644 21.0976 7.70696 20.7071L11.9999 16.4142L16.2929 20.7071Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.9999 9.99933C12.265 9.99931 12.5195 9.89375 12.707 9.70636L17.707 4.70539C18.0972 4.31484 18.0974 3.68176 17.707 3.29133C17.3164 2.90101 16.6833 2.90185 16.2929 3.2923L11.9999 7.58527L7.70696 3.2923C7.31643 2.90178 6.68342 2.90178 6.29289 3.2923C5.90258 3.68284 5.90244 4.31591 6.29289 4.70636L11.2929 9.70636C11.4804 9.89372 11.7348 9.99933 11.9999 9.99933Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_collapse_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVerticalCollapse24Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_collapse_24,
        contentDescription = null,
    )
}