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

private var _ic_cross_circle_20_filled: ImageVector? = null

val Icons.ic_cross_circle_20_filled: ImageVector
    get() {
        if (_ic_cross_circle_20_filled != null) return _ic_cross_circle_20_filled!!
        _ic_cross_circle_20_filled = ImageVector.Builder(
            name = "ic_cross_circle_20_filled",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 2C14.4183 2 18 5.58172 18 10C18 14.4183 14.4183 18 10 18C5.58172 18 2 14.4183 2 10C2 5.58172 5.58172 2 10 2ZM13.5312 6.46973C13.2384 6.17694 12.7626 6.17697 12.4697 6.46973L10 8.93945L7.53027 6.46973C7.23742 6.17694 6.76261 6.17698 6.46973 6.46973C6.17701 6.76261 6.17695 7.23743 6.46973 7.53027L8.93945 10L6.4707 12.4697C6.17799 12.7626 6.17792 13.2374 6.4707 13.5303C6.76358 13.8227 7.23849 13.8229 7.53125 13.5303L10 11.0605L12.4697 13.5303C12.7626 13.8229 13.2374 13.8229 13.5303 13.5303C13.823 13.2375 13.8229 12.7626 13.5303 12.4697L11.0605 10L13.5312 7.53027C13.8236 7.23751 13.8235 6.76254 13.5312 6.46973Z"),
            )
        }.build()
        return _ic_cross_circle_20_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCrossCircle20FilledPreview() {
    Icon(
        imageVector = Icons.ic_cross_circle_20_filled,
        contentDescription = null,
    )
}