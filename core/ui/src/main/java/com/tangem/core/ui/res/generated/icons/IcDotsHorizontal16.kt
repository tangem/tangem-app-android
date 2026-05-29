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

private var _ic_dots_horizontal_16: ImageVector? = null

val Icons.ic_dots_horizontal_16: ImageVector
    get() {
        if (_ic_dots_horizontal_16 != null) return _ic_dots_horizontal_16!!
        _ic_dots_horizontal_16 = ImageVector.Builder(
            name = "ic_dots_horizontal_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.99608 7.04102C4.52397 7.04114 4.95677 7.46928 4.95702 8C4.95676 8.52829 4.52635 8.95886 3.99804 8.95898C3.50298 8.95873 3.09443 8.58087 3.04491 8.09863L3.04003 8C3.03819 7.46831 3.47144 7.04126 3.99608 7.04102Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.99804 7.04102C8.52582 7.04128 8.95872 7.46936 8.95897 8C8.95871 8.5282 8.52819 8.95872 7.99999 8.95898C7.50494 8.95873 7.09639 8.58085 7.04687 8.09863L7.04198 8C7.04015 7.46831 7.47339 7.04126 7.99804 7.04102Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M11.999 7.04102C12.527 7.04102 12.9597 7.4692 12.96 8C12.9597 8.52836 12.5294 8.95898 12.001 8.95898C11.506 8.95861 11.0973 8.58081 11.0478 8.09863L11.043 8C11.0411 7.46839 11.4745 7.04139 11.999 7.04102Z"),
            )
        }.build()
        return _ic_dots_horizontal_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDotsHorizontal16Preview() {
    Icon(
        imageVector = Icons.ic_dots_horizontal_16,
        contentDescription = null,
    )
}