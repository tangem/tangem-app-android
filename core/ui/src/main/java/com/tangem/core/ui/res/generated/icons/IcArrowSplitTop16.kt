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

private var _ic_arrow_split_top_16: ImageVector? = null

val Icons.ic_arrow_split_top_16: ImageVector
    get() {
        if (_ic_arrow_split_top_16 != null) return _ic_arrow_split_top_16!!
        _ic_arrow_split_top_16 = ImageVector.Builder(
            name = "ic_arrow_split_top_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.66278 6.15311C3.34703 6.55766 2.93 6.40893 2.78107 5.89134L2.03041 3.31528C1.91126 2.90477 2.16148 2.58351 2.59042 2.60136L5.27728 2.6906C5.81346 2.70844 6.05176 3.08325 5.74197 3.48186L5.20579 4.16603L5.22962 4.17793C6.32581 4.90375 7.66626 6.41488 7.98796 7.41436H8.01179C8.32755 6.42083 9.67395 4.90375 10.7701 4.17793L10.7999 4.16008L10.2518 3.40452C9.95396 2.99401 10.1982 2.62515 10.7404 2.6311L13.4272 2.6192C13.8562 2.6192 14.0945 2.95237 13.9634 3.36287L13.1293 5.90918C12.9685 6.42083 12.5395 6.55766 12.2357 6.13526L11.6578 5.33805L11.5267 5.42134C10.3293 6.18285 8.72074 8.05689 8.72074 9.41929V12.6022C8.72074 13.1198 8.46457 13.3994 7.99988 13.3994C7.53519 13.3994 7.28497 13.1198 7.28497 12.6022V9.41929C7.28497 8.05689 5.66452 6.18285 4.47301 5.42134L4.31216 5.3202L3.66278 6.15311Z"),
            )
        }.build()
        return _ic_arrow_split_top_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowSplitTop16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_split_top_16,
        contentDescription = null,
    )
}