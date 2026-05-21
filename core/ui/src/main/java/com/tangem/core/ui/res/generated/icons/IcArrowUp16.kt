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

private var _ic_arrow_up_16: ImageVector? = null

val Icons.ic_arrow_up_16: ImageVector
    get() {
        if (_ic_arrow_up_16 != null) return _ic_arrow_up_16!!
        _ic_arrow_up_16 = ImageVector.Builder(
            name = "ic_arrow_up_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.49966 13.6667C7.49966 13.9427 7.72367 14.1665 7.99966 14.1667C8.27581 14.1667 8.49966 13.9428 8.49966 13.6667V3.87372L12.9801 8.35321C13.1754 8.54847 13.4919 8.54847 13.6872 8.35321C13.8821 8.15792 13.8823 7.84133 13.6872 7.64618L8.35318 2.31317C8.1579 2.11807 7.84136 2.11796 7.64615 2.31317L2.31314 7.64618C2.11793 7.84139 2.11804 8.15793 2.31314 8.35321C2.5084 8.54847 2.82491 8.54847 3.02017 8.35321L7.49966 3.87372V13.6667Z"),
            )
        }.build()
        return _ic_arrow_up_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp16Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_16,
        contentDescription = null,
    )
}