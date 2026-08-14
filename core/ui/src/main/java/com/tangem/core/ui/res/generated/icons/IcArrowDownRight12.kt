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

private var _ic_arrow_down_right_12: ImageVector? = null

val Icons.ic_arrow_down_right_12: ImageVector
    get() {
        if (_ic_arrow_down_right_12 != null) return _ic_arrow_down_right_12!!
        _ic_arrow_down_right_12 = ImageVector.Builder(
            name = "ic_arrow_down_right_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.00014 9.50005C9.13256 9.49994 9.26 9.44718 9.35366 9.35357C9.44724 9.25984 9.50013 9.13251 9.50014 9.00005V4.75005C9.50008 4.47411 9.27605 4.25028 9.00014 4.25005C8.7241 4.25012 8.50021 4.47401 8.50014 4.75005V7.79302L3.35366 2.64654C3.15849 2.4514 2.8419 2.45156 2.64663 2.64654C2.45147 2.8418 2.45143 3.15834 2.64663 3.35357L7.79311 8.50005H4.75014C4.4741 8.50012 4.25021 8.72401 4.25014 9.00005C4.25018 9.27613 4.47408 9.49999 4.75014 9.50005H9.00014Z"),
            )
        }.build()
        return _ic_arrow_down_right_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownRight12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_right_12,
        contentDescription = null,
    )
}