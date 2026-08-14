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

private var _ic_arrow_down_left_12: ImageVector? = null

val Icons.ic_arrow_down_left_12: ImageVector
    get() {
        if (_ic_arrow_down_left_12 != null) return _ic_arrow_down_left_12!!
        _ic_arrow_down_left_12 = ImageVector.Builder(
            name = "ic_arrow_down_left_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M2.99988 9.50005C2.86747 9.49994 2.74002 9.44718 2.64636 9.35357C2.55278 9.25984 2.49989 9.13251 2.49988 9.00005V4.75005C2.49994 4.47411 2.72397 4.25028 2.99988 4.25005C3.27592 4.25012 3.49981 4.47401 3.49988 4.75005V7.79302L8.64636 2.64654C8.84154 2.4514 9.15812 2.45156 9.35339 2.64654C9.54855 2.8418 9.54859 3.15834 9.35339 3.35357L4.20691 8.50005H7.24988C7.52592 8.50012 7.74981 8.72401 7.74988 9.00005C7.74984 9.27613 7.52594 9.49999 7.24988 9.50005H2.99988Z"),
            )
        }.build()
        return _ic_arrow_down_left_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownLeft12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_left_12,
        contentDescription = null,
    )
}