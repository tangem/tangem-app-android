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

private var _ic_arrow_top_left_12: ImageVector? = null

val Icons.ic_arrow_top_left_12: ImageVector
    get() {
        if (_ic_arrow_top_left_12 != null) return _ic_arrow_top_left_12!!
        _ic_arrow_top_left_12 = ImageVector.Builder(
            name = "ic_arrow_top_left_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.99988 2.5C2.86747 2.50011 2.74002 2.55287 2.64636 2.64648C2.55278 2.74021 2.49989 2.86754 2.49988 3V7.25C2.49994 7.52595 2.72397 7.74977 2.99988 7.75C3.27592 7.74993 3.49981 7.52604 3.49988 7.25V4.20703L8.64636 9.35352C8.84154 9.54865 9.15812 9.54849 9.35339 9.35352C9.54855 9.15826 9.54859 8.84172 9.35339 8.64648L4.20691 3.5H7.24988C7.52592 3.49993 7.74981 3.27604 7.74988 3C7.74984 2.72393 7.52594 2.50007 7.24988 2.5H2.99988Z"),
            )
        }.build()
        return _ic_arrow_top_left_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopLeft12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_left_12,
        contentDescription = null,
    )
}