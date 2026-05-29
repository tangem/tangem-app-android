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

private var _ic_chevron_up_12: ImageVector? = null

val Icons.ic_chevron_up_12: ImageVector
    get() {
        if (_ic_chevron_up_12 != null) return _ic_chevron_up_12!!
        _ic_chevron_up_12 = ImageVector.Builder(
            name = "ic_chevron_up_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.14292 7.60641C1.94805 7.41115 1.94788 7.09453 2.14292 6.89938L5.6439 3.3984C5.83905 3.20339 6.15568 3.20354 6.35093 3.3984L9.85191 6.89938C10.047 7.09461 10.047 7.41119 9.85191 7.60641C9.65669 7.80151 9.34011 7.80148 9.14487 7.60641L5.99741 4.45895L2.84995 7.60641C2.65478 7.80147 2.33817 7.80135 2.14292 7.60641Z"),
            )
        }.build()
        return _ic_chevron_up_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcChevronUp12Preview() {
    Icon(
        imageVector = Icons.ic_chevron_up_12,
        contentDescription = null,
    )
}