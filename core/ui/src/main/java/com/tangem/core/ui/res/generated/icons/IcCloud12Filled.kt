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

private var _ic_cloud_12_filled: ImageVector? = null

val Icons.ic_cloud_12_filled: ImageVector
    get() {
        if (_ic_cloud_12_filled != null) return _ic_cloud_12_filled!!
        _ic_cloud_12_filled = ImageVector.Builder(
            name = "ic_cloud_12_filled",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6 3C7.49112 3 8.70012 4.15119 8.7002 5.57129C9.69422 5.57139 10.5 6.33942 10.5 7.28613C10.4998 8.23265 9.69407 8.9999 8.7002 9H3.75C2.50746 9 1.50016 8.04076 1.5 6.85742C1.50201 5.78861 2.33303 4.88504 3.44531 4.74316C3.81716 3.70129 4.84455 3.00054 6 3Z"),
            )
        }.build()
        return _ic_cloud_12_filled!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCloud12FilledPreview() {
    Icon(
        imageVector = Icons.ic_cloud_12_filled,
        contentDescription = null,
    )
}