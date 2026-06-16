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

private var _ic_arrow_down_16: ImageVector? = null

val Icons.ic_arrow_down_16: ImageVector
    get() {
        if (_ic_arrow_down_16 != null) return _ic_arrow_down_16!!
        _ic_arrow_down_16 = ImageVector.Builder(
            name = "ic_arrow_down_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.4997 2.66669L7.4997 12.4597L3.02021 7.98016C2.82494 7.7849 2.50844 7.7849 2.31318 7.98016C2.11808 8.17544 2.11797 8.49199 2.31318 8.68719L7.64618 14.0202C7.73987 14.1139 7.86721 14.1666 7.9997 14.1667C8.13223 14.1667 8.25946 14.1139 8.35321 14.0202L13.6872 8.6872C13.8824 8.49204 13.8821 8.17545 13.6872 7.98017C13.4919 7.7849 13.1754 7.7849 12.9802 7.98017L8.4997 12.4606L8.4997 2.66669C8.4997 2.39055 8.27584 2.16669 7.9997 2.16669C7.72371 2.16686 7.4997 2.39065 7.4997 2.66669Z"),
            )
        }.build()
        return _ic_arrow_down_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_16,
        contentDescription = null,
    )
}