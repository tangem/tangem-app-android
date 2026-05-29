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

private var _ic_dots_horizontal_12: ImageVector? = null

val Icons.ic_dots_horizontal_12: ImageVector
    get() {
        if (_ic_dots_horizontal_12 != null) return _ic_dots_horizontal_12!!
        _ic_dots_horizontal_12 = ImageVector.Builder(
            name = "ic_dots_horizontal_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M3.07421 5.25391C3.45172 5.2923 3.74896 5.61048 3.74901 6C3.74895 6.41356 3.41256 6.74987 2.99901 6.75C2.61143 6.74993 2.29166 6.45466 2.25292 6.07715L2.24901 6C2.2475 5.58357 2.58725 5.25001 2.99804 5.25L3.07421 5.25391Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.07519 5.25391C6.45281 5.29219 6.74994 5.6104 6.74999 6C6.74992 6.41364 6.41365 6.75 5.99999 6.75C5.6124 6.74993 5.29262 6.45467 5.2539 6.07715L5.24999 6C5.24847 5.58358 5.58822 5.25001 5.99901 5.25L6.07519 5.25391Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.07616 5.25391C9.45379 5.29219 9.75091 5.61039 9.75097 6C9.7509 6.41364 9.41463 6.75 9.00097 6.75C8.61348 6.74981 8.29358 6.45461 8.25487 6.07715L8.25097 6C8.24945 5.58366 8.58931 5.25014 8.99999 5.25L9.07616 5.25391Z"),
            )
        }.build()
        return _ic_dots_horizontal_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDotsHorizontal12Preview() {
    Icon(
        imageVector = Icons.ic_dots_horizontal_12,
        contentDescription = null,
    )
}