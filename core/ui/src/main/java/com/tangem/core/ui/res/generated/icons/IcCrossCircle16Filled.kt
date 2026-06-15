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

private var _ic_cross_circle_16_filled: ImageVector? = null

val Icons.ic_cross_circle_16_filled: ImageVector
    get() {
        if (_ic_cross_circle_16_filled != null) return _ic_cross_circle_16_filled!!
        _ic_cross_circle_16_filled = ImageVector.Builder(
            name = "ic_cross_circle_16_filled",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 2C11.3137 2 14 4.68629 14 8C14 11.3137 11.3137 14 8 14C4.68629 14 2 11.3137 2 8C2 4.68629 4.68629 2 8 2ZM10.8154 5.18262C10.5714 4.9388 10.1757 4.93985 9.93164 5.18359L8 7.11426L6.06934 5.18359C5.82553 4.93982 5.42969 4.93935 5.18555 5.18262C4.94161 5.42649 4.94104 5.82226 5.18457 6.06641L7.11621 7.99805L5.18457 9.93066C4.94076 10.1748 4.94159 10.5705 5.18555 10.8145C5.42967 11.0583 5.82536 11.0585 6.06934 10.8145L8 8.88184L9.93164 10.8145C10.1757 11.0583 10.5714 11.0584 10.8154 10.8145C11.059 10.5704 11.0592 10.1746 10.8154 9.93066L8.88379 7.99805L10.8154 6.06641C11.0588 5.82225 11.0593 5.42645 10.8154 5.18262Z"),
            )
        }.build()
        return _ic_cross_circle_16_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCrossCircle16FilledPreview() {
    Icon(
        imageVector = Icons.ic_cross_circle_16_filled,
        contentDescription = null,
    )
}