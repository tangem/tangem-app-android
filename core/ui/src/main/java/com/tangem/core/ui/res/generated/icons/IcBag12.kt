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

private var _ic_bag_12: ImageVector? = null

val Icons.ic_bag_12: ImageVector
    get() {
        if (_ic_bag_12 != null) return _ic_bag_12!!
        _ic_bag_12 = ImageVector.Builder(
            name = "ic_bag_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.5 4.5C7.77603 4.50013 8 4.72394 8 5V5.5C7.99999 6.53555 7.21265 7.38689 6.2041 7.48926L6 7.5L5.7959 7.48926C4.78723 7.387 4.00001 6.53564 4 5.5V5C4 4.72386 4.22386 4.5 4.5 4.5C4.77603 4.50013 5 4.72394 5 5V5.5C5.00001 6.05224 5.44771 6.5 6 6.5C6.55218 6.49987 6.99999 6.05216 7 5.5V5C7 4.72386 7.22386 4.5 7.5 4.5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9 1C9.82841 1 10.5 1.67158 10.5 2.5V2.88379C10.8063 3.15842 11 3.55619 11 4V9.5C10.9999 10.3283 10.3284 11 9.5 11H2.5C1.67165 11 1.0001 10.3283 1 9.5V4C1 3.55619 1.19366 3.15842 1.5 2.88379V2.5C1.5 1.67158 2.17159 1 3 1H9ZM2.5 3.5C2.22384 3.5 2 3.72389 2 4V9.5C2.0001 9.77603 2.2239 10 2.5 10H9.5C9.7761 10 9.9999 9.77603 10 9.5V4C10 3.72389 9.77616 3.5 9.5 3.5H2.5ZM3 2C2.72384 2 2.5 2.22389 2.5 2.5H9.5C9.5 2.22389 9.27616 2 9 2H3Z"),
            )
        }.build()
        return _ic_bag_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcBag12Preview() {
    Icon(
        imageVector = Icons.ic_bag_12,
        contentDescription = null,
    )
}