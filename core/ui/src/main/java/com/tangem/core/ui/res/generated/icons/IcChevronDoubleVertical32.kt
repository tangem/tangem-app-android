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

private var _ic_chevron_double_vertical_32: ImageVector? = null

val Icons.ic_chevron_double_vertical_32: ImageVector
    get() {
        if (_ic_chevron_double_vertical_32 != null) return _ic_chevron_double_vertical_32!!
        _ic_chevron_double_vertical_32 = ImageVector.Builder(
            name = "ic_chevron_double_vertical_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.5099 19.1094C22.0567 18.563 22.9438 18.5628 23.4904 19.1094C24.037 19.656 24.0367 20.5431 23.4904 21.0898L16.9904 27.5898C16.7279 27.8523 16.3713 27.9999 16.0001 28C15.6288 28 15.2725 27.8524 15.0099 27.5898L8.5099 21.0898C7.96317 20.5431 7.96317 19.6561 8.5099 19.1094C9.05667 18.563 9.94377 18.5628 10.4904 19.1094L16.0001 24.6191L21.5099 19.1094Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.0001 4C16.3713 4.0001 16.7279 4.1477 16.9904 4.41016L23.4904 10.9102C24.0369 11.4568 24.0368 12.3439 23.4904 12.8906C22.9437 13.4373 22.0566 13.4373 21.5099 12.8906L16.0001 7.38086L10.4904 12.8906C9.94366 13.4373 9.05664 13.4373 8.5099 12.8906C7.96317 12.3439 7.96317 11.4569 8.5099 10.9102L15.0099 4.41016C15.2724 4.1478 15.629 4 16.0001 4Z"),
            )
        }.build()
        return _ic_chevron_double_vertical_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronDoubleVertical32Preview() {
    Icon(
        imageVector = Icons.ic_chevron_double_vertical_32,
        contentDescription = null,
    )
}