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

private var _ic_arrow_top_right_12: ImageVector? = null

val Icons.ic_arrow_top_right_12: ImageVector
    get() {
        if (_ic_arrow_top_right_12 != null) return _ic_arrow_top_right_12!!
        _ic_arrow_top_right_12 = ImageVector.Builder(
            name = "ic_arrow_top_right_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.00008 2.5C9.13249 2.50011 9.25994 2.55287 9.3536 2.64648C9.44718 2.74021 9.50007 2.86754 9.50008 3V7.25C9.50002 7.52595 9.27599 7.74977 9.00008 7.75C8.72404 7.74993 8.50015 7.52604 8.50008 7.25V4.20703L3.3536 9.35352C3.15842 9.54865 2.84184 9.54849 2.64657 9.35352C2.45141 9.15826 2.45137 8.84172 2.64657 8.64648L7.79305 3.5H4.75008C4.47404 3.49993 4.25015 3.27604 4.25008 3C4.25012 2.72393 4.47402 2.50007 4.75008 2.5H9.00008Z"),
            )
        }.build()
        return _ic_arrow_top_right_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopRight12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_right_12,
        contentDescription = null,
    )
}