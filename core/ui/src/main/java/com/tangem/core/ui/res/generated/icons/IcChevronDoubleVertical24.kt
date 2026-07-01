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

private var _ic_chevron_double_vertical_24: ImageVector? = null

val Icons.ic_chevron_double_vertical_24: ImageVector
    get() {
        if (_ic_chevron_double_vertical_24 != null) return _ic_chevron_double_vertical_24!!
        _ic_chevron_double_vertical_24 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.2929 14.293C16.6834 13.9025 17.3164 13.9025 17.707 14.293C18.0974 14.6835 18.0974 15.3165 17.707 15.707L12.707 20.707C12.5194 20.8945 12.2651 21 11.9999 21C11.7347 21 11.4804 20.8945 11.2929 20.707L6.29289 15.707C5.90237 15.3165 5.90237 14.6835 6.29289 14.293C6.68342 13.9025 7.31644 13.9025 7.70696 14.293L11.9999 18.5859L16.2929 14.293Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.9999 2.99902C12.265 2.99904 12.5195 3.1046 12.707 3.29199L17.707 8.29297C18.0972 8.68352 18.0974 9.3166 17.707 9.70703C17.3164 10.0973 16.6833 10.0965 16.2929 9.70605L11.9999 5.41309L7.70696 9.70605C7.31643 10.0966 6.68342 10.0966 6.29289 9.70605C5.90258 9.31551 5.90244 8.68245 6.29289 8.29199L11.2929 3.29199C11.4804 3.10463 11.7348 2.99902 11.9999 2.99902Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVertical24Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_24,
        contentDescription = null,
    )
}