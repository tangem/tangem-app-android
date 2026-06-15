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

private var _ic_dots_vertical_16: ImageVector? = null

val Icons.ic_dots_vertical_16: ImageVector
    get() {
        if (_ic_dots_vertical_16 != null) return _ic_dots_vertical_16!!
        _ic_dots_vertical_16 = ImageVector.Builder(
            name = "ic_dots_vertical_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99804 11.043C8.52574 11.0432 8.95859 11.4704 8.95897 12.001C8.95897 12.5294 8.52836 12.9597 7.99999 12.96C7.50476 12.9597 7.09614 12.5821 7.04687 12.0996L7.04198 12.001C7.04028 11.4694 7.47347 11.0432 7.99804 11.043Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99804 7.04199C8.52582 7.04225 8.95872 7.46936 8.95897 8C8.95871 8.5282 8.5282 8.95872 7.99999 8.95898C7.50494 8.95873 7.09639 8.58085 7.04687 8.09863L7.04198 8C7.04015 7.46831 7.47339 7.04223 7.99804 7.04199Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99804 3.04004C8.52582 3.0403 8.95872 3.46741 8.95897 3.99805C8.95891 4.52642 8.52832 4.95677 7.99999 4.95703C7.50482 4.95678 7.09622 4.57907 7.04687 4.09668L7.04198 3.99805C7.04015 3.46635 7.47339 3.04028 7.99804 3.04004Z"),
            )
        }.build()
        return _ic_dots_vertical_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDotsVertical16Preview() {
    Icon(
        imageVector = Icons.ic_dots_vertical_16,
        contentDescription = null,
    )
}