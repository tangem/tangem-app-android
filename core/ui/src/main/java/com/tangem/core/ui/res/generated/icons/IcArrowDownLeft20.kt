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

private var _ic_arrow_down_left_20: ImageVector? = null

val Icons.ic_arrow_down_left_20: ImageVector
    get() {
        if (_ic_arrow_down_left_20 != null) return _ic_arrow_down_left_20!!
        _ic_arrow_down_left_20 = ImageVector.Builder(
            name = "ic_arrow_down_left_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.74988 16C4.55099 16 4.36024 15.921 4.2196 15.7803C4.07897 15.6397 3.99988 15.4489 3.99988 15.25V7.75004C3.99988 7.33585 4.33569 7.00007 4.74988 7.00004C5.16409 7.00004 5.49988 7.33583 5.49988 7.75004V13.4395L14.7196 4.21977C15.0125 3.9269 15.4873 3.92691 15.7802 4.21977C16.073 4.51266 16.073 4.98743 15.7802 5.28032L6.56042 14.5H12.2499C12.6641 14.5001 12.9999 14.8359 12.9999 15.25C12.9999 15.6642 12.6641 16 12.2499 16H4.74988Z"),
            )
        }.build()
        return _ic_arrow_down_left_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownLeft20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_left_20,
        contentDescription = null,
    )
}