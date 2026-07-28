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

private var _ic_arrow_top_left_16: ImageVector? = null

val Icons.ic_arrow_top_left_16: ImageVector
    get() {
        if (_ic_arrow_top_left_16 != null) return _ic_arrow_top_left_16!!
        _ic_arrow_top_left_16 = ImageVector.Builder(
            name = "ic_arrow_top_left_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.64771 3.02344C3.48221 3.02362 3.32236 3.08903 3.20532 3.20605C3.08846 3.32313 3.02281 3.48301 3.02271 3.64844V9.75C3.02292 10.0948 3.30298 10.3746 3.64771 10.375C3.99275 10.375 4.27249 10.095 4.27271 9.75V5.15723L11.9075 12.792C12.1514 13.0357 12.5472 13.0356 12.7913 12.792C13.0353 12.5479 13.0353 12.1513 12.7913 11.9072L5.15747 4.27344H9.74927C10.0943 4.27344 10.374 3.99341 10.3743 3.64844C10.3741 3.30344 10.0943 3.02344 9.74927 3.02344H3.64771Z"),
            )
        }.build()
        return _ic_arrow_top_left_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopLeft16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_left_16,
        contentDescription = null,
    )
}