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

private var _ic_arrow_down_12: ImageVector? = null

val Icons.ic_arrow_down_12: ImageVector
    get() {
        if (_ic_arrow_down_12 != null) return _ic_arrow_down_12!!
        _ic_arrow_down_12 = ImageVector.Builder(
            name = "ic_arrow_down_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.14582 6.64587C1.95126 6.84117 1.95085 7.15787 2.14582 7.35291L5.6468 10.8539C5.84183 11.0489 6.15852 11.0485 6.35383 10.8539L9.85481 7.35291C10.05 7.15764 10.05 6.84112 9.85481 6.64587C9.65955 6.45076 9.343 6.45072 9.14777 6.64587L6.50031 9.29333L6.50031 1.50232C6.50031 1.22622 6.2764 1.00238 6.00031 1.00232C5.72451 1.00272 5.50031 1.22642 5.50031 1.50232L5.50031 9.29333L2.85285 6.64587C2.65765 6.45075 2.34107 6.45083 2.14582 6.64587Z"),
            )
        }.build()
        return _ic_arrow_down_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDown12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_12,
        contentDescription = null,
    )
}