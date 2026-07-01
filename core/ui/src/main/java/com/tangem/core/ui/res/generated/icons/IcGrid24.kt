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

private var _ic_grid_24: ImageVector? = null

val Icons.ic_grid_24: ImageVector
    get() {
        if (_ic_grid_24 != null) return _ic_grid_24!!
        _ic_grid_24 = ImageVector.Builder(
            name = "ic_grid_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.74902 13.249C9.40615 13.249 10.749 14.5919 10.749 16.249V18.998C10.7488 20.6551 9.40603 21.998 7.74902 21.998H5C3.34319 21.9978 2.00018 20.6549 2 18.998V16.249C2 14.592 3.34308 13.2493 5 13.249H7.74902ZM5 15.249C4.44756 15.2493 4 15.6967 4 16.249V18.998C4.00018 19.5502 4.44767 19.9978 5 19.998H7.74902C8.30156 19.998 8.74884 19.5504 8.74902 18.998V16.249C8.74902 15.6965 8.30167 15.249 7.74902 15.249H5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.998 13.249C20.6552 13.249 21.998 14.5919 21.998 16.249V18.998C21.9979 20.6551 20.6551 21.998 18.998 21.998H16.249C14.5921 21.9979 13.2492 20.655 13.249 18.998V16.249C13.249 14.5919 14.592 13.2491 16.249 13.249H18.998ZM16.249 15.249C15.6965 15.2491 15.249 15.6966 15.249 16.249V18.998C15.2492 19.5503 15.6966 19.9979 16.249 19.998H18.998C19.5506 19.998 19.9979 19.5504 19.998 18.998V16.249C19.998 15.6965 19.5507 15.249 18.998 15.249H16.249Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.74902 2C9.40602 2 10.7488 3.34302 10.749 5V7.74902C10.749 9.40619 9.40615 10.749 7.74902 10.749H5C3.34308 10.7488 2 9.40604 2 7.74902V5C2.00021 3.34317 3.34321 2.00024 5 2H7.74902ZM5 4C4.44769 4.00024 4.00021 4.44783 4 5V7.74902C4 8.30138 4.44756 8.74879 5 8.74902H7.74902C8.30167 8.74902 8.74902 8.30152 8.74902 7.74902V5C8.74881 4.44768 8.30154 4 7.74902 4H5Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.998 2C20.655 2 21.9978 3.34302 21.998 5V7.74902C21.998 9.40619 20.6552 10.749 18.998 10.749H16.249C14.592 10.7489 13.249 9.40612 13.249 7.74902V5C13.2492 3.34308 14.5921 2.00011 16.249 2H18.998ZM16.249 4C15.6966 4.00011 15.2492 4.44775 15.249 5V7.74902C15.249 8.30146 15.6965 8.74892 16.249 8.74902H18.998C19.5507 8.74902 19.998 8.30152 19.998 7.74902V5C19.9978 4.44768 19.5506 4 18.998 4H16.249Z"),
            )
        }.build()
        return _ic_grid_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGrid24Preview() {
    Icon(
        imageVector = Icons.ic_grid_24,
        contentDescription = null,
    )
}