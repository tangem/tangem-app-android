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

private var _ic_arrow_down_left_16: ImageVector? = null

val Icons.ic_arrow_down_left_16: ImageVector
    get() {
        if (_ic_arrow_down_left_16 != null) return _ic_arrow_down_left_16!!
        _ic_arrow_down_left_16 = ImageVector.Builder(
            name = "ic_arrow_down_left_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.6488 12.9766C3.48322 12.9766 3.3236 12.9109 3.20642 12.794C3.08944 12.6769 3.02391 12.5171 3.0238 12.3516V6.25001C3.02401 5.90501 3.30376 5.62501 3.6488 5.62501C3.99369 5.6252 4.27359 5.90513 4.2738 6.25001V10.8428L11.9086 3.20801C12.1526 2.96442 12.5484 2.96429 12.7924 3.20801C13.036 3.45208 13.0362 3.84882 12.7924 4.09278L5.15857 11.7266H9.75037C10.0952 11.7268 10.3751 12.0067 10.3754 12.3516C10.3752 12.6964 10.0952 12.9764 9.75037 12.9766H3.6488Z"),
            )
        }.build()
        return _ic_arrow_down_left_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownLeft16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_left_16,
        contentDescription = null,
    )
}