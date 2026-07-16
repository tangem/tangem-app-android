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

private var _ic_grid_plus_24: ImageVector? = null

val Icons.ic_grid_plus_24: ImageVector
    get() {
        if (_ic_grid_plus_24 != null) return _ic_grid_plus_24!!
        _ic_grid_plus_24 = ImageVector.Builder(
            name = "ic_grid_plus_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 13C9.65728 13 11 14.3427 11 16V19C11 20.6573 9.65728 22 8 22H5C3.34272 22 2 20.6573 2 19V16C2 14.3427 3.34272 13 5 13H8ZM5 15C4.44728 15 4 15.4473 4 16V19C4 19.5527 4.44728 20 5 20H8C8.55272 20 9 19.5527 9 19V16C9 15.4473 8.55272 15 8 15H5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.5 14C18.0523 14 18.5 14.4477 18.5 15V16.5H20C20.5523 16.5 21 16.9477 21 17.5C21 18.0523 20.5523 18.5 20 18.5H18.5V20C18.5 20.5523 18.0523 21 17.5 21C16.9477 21 16.5 20.5523 16.5 20V18.5H15C14.4477 18.5 14 18.0523 14 17.5C14 16.9477 14.4477 16.5 15 16.5H16.5V15C16.5 14.4477 16.9477 14 17.5 14Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8 2C9.65728 2 11 3.34272 11 5V8C11 9.65728 9.65728 11 8 11H5C3.34272 11 2 9.65728 2 8V5C2 3.34272 3.34272 2 5 2H8ZM5 4C4.44728 4 4 4.44728 4 5V8C4 8.55272 4.44728 9 5 9H8C8.55272 9 9 8.55272 9 8V5C9 4.44728 8.55272 4 8 4H5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M19 2C20.6573 2 22 3.34272 22 5V8C22 9.65728 20.6573 11 19 11H16C14.3427 11 13 9.65728 13 8V5C13 3.34272 14.3427 2 16 2H19ZM16 4C15.4473 4 15 4.44728 15 5V8C15 8.55272 15.4473 9 16 9H19C19.5527 9 20 8.55272 20 8V5C20 4.44728 19.5527 4 19 4H16Z"),
            )
        }.build()
        return _ic_grid_plus_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGridPlus24Preview() {
    Icon(
        imageVector = Icons.ic_grid_plus_24,
        contentDescription = null,
    )
}