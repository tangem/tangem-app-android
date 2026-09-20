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

private var _ic_arrow_up_down_16: ImageVector? = null

val Icons.ic_arrow_up_down_16: ImageVector
    get() {
        if (_ic_arrow_up_down_16 != null) return _ic_arrow_up_down_16!!
        _ic_arrow_up_down_16 = ImageVector.Builder(
            name = "ic_arrow_up_down_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.13168 2.54395C5.16793 2.54495 5.20396 2.54752 5.2391 2.55469C5.24911 2.5567 5.25856 2.56099 5.2684 2.56348C5.33753 2.58114 5.40336 2.60956 5.46273 2.64941C5.49669 2.67221 5.52866 2.69773 5.55746 2.72656L7.87679 5.04688C8.12067 5.29101 8.11988 5.68668 7.87582 5.93066C7.63165 6.17416 7.23588 6.1746 6.99203 5.93066L5.74008 4.67676V12.833C5.74008 13.1782 5.46025 13.458 5.11508 13.458C4.76995 13.4579 4.49008 13.1781 4.49008 12.833V4.67578L3.23519 5.93066C2.99107 6.17423 2.59531 6.17457 2.3514 5.93066C2.1077 5.68674 2.1079 5.29093 2.3514 5.04688L4.67269 2.72559C4.76048 2.63793 4.87212 2.57868 4.99203 2.55469L5.1141 2.54297C5.11962 2.54297 5.12619 2.5438 5.13168 2.54395Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8846 2.54102C11.2296 2.54102 11.5093 2.82106 11.5096 3.16602V11.3242L12.7645 10.0703C13.0085 9.82664 13.4043 9.82665 13.6483 10.0703C13.8921 10.3143 13.892 10.71 13.6483 10.9541L11.327 13.2754C11.2099 13.3923 11.0511 13.4579 10.8856 13.458C10.7199 13.458 10.5604 13.3916 10.4432 13.2744L8.12289 10.9551C7.87912 10.7111 7.88012 10.3153 8.12386 10.0713C8.36789 9.82752 8.76365 9.82656 9.00765 10.0703L10.2596 11.3232V3.16699C10.2596 2.82186 10.5395 2.5411 10.8846 2.54102Z"),
            )
        }.build()
        return _ic_arrow_up_down_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUpDown16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_down_16,
        contentDescription = null,
    )
}