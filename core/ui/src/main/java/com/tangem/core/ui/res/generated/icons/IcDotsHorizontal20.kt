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

private var _ic_dots_horizontal_20: ImageVector? = null

val Icons.ic_dots_horizontal_20: ImageVector
    get() {
        if (_ic_dots_horizontal_20 != null) return _ic_dots_horizontal_20!!
        _ic_dots_horizontal_20 = ImageVector.Builder(
            name = "ic_dots_horizontal_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.99511 8.83301C5.63812 8.83301 6.16497 9.35366 6.16503 10C6.16495 10.6434 5.64141 11.1669 4.99804 11.167C4.39495 11.1669 3.89681 10.7067 3.83691 10.1191L3.83105 10C3.82843 9.35253 4.35619 8.83332 4.99511 8.83301Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99706 8.83301C10.6401 8.83301 11.1669 9.35366 11.167 10C11.1669 10.6434 10.6434 11.1669 9.99999 11.167C9.3969 11.1669 8.89877 10.7067 8.83886 10.1191L8.833 10C8.83038 9.35253 9.35814 8.83332 9.99706 8.83301Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15 8.83301C15.6428 8.83327 16.1698 9.35382 16.1699 10C16.1698 10.6432 15.6461 11.1667 15.0029 11.167C14.3997 11.167 13.9017 10.7068 13.8418 10.1191L13.8359 10C13.8333 9.35245 14.361 8.83318 15 8.83301Z"),
            )
        }.build()
        return _ic_dots_horizontal_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDotsHorizontal20Preview() {
    Icon(
        imageVector = Icons.ic_dots_horizontal_20,
        contentDescription = null,
    )
}