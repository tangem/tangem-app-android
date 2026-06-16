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

private var _ic_chevron_right_12: ImageVector? = null

val Icons.ic_chevron_right_12: ImageVector
    get() {
        if (_ic_chevron_right_12 != null) return _ic_chevron_right_12!!
        _ic_chevron_right_12 = ImageVector.Builder(
            name = "ic_chevron_right_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.3938 2.14306C4.58908 1.94819 4.9057 1.94797 5.10084 2.14306L8.60181 5.64404C8.79675 5.83919 8.79663 6.15585 8.60181 6.35107L5.10084 9.85205C4.90563 10.0472 4.58905 10.0471 4.3938 9.85205C4.19862 9.65681 4.19865 9.34027 4.3938 9.14502L7.54127 5.99756L4.3938 2.85009C4.19867 2.6549 4.19878 2.33833 4.3938 2.14306Z"),
            )
        }.build()
        return _ic_chevron_right_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronRight12Preview() {
    Icon(
        imageVector = Icons.ic_chevron_right_12,
        contentDescription = null,
    )
}