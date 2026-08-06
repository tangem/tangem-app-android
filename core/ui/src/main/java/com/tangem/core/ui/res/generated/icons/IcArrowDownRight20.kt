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

private var _ic_arrow_down_right_20: ImageVector? = null

val Icons.ic_arrow_down_right_20: ImageVector
    get() {
        if (_ic_arrow_down_right_20 != null) return _ic_arrow_down_right_20!!
        _ic_arrow_down_right_20 = ImageVector.Builder(
            name = "ic_arrow_down_right_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.2501 16C15.4489 16 15.6397 15.921 15.7803 15.7803C15.921 15.6397 16.0001 15.4489 16.0001 15.25V7.75004C16.0001 7.33585 15.6642 7.00007 15.2501 7.00004C14.8358 7.00004 14.5001 7.33583 14.5001 7.75004V13.4395L5.28032 4.21977C4.98744 3.9269 4.51267 3.92691 4.21978 4.21977C3.9269 4.51266 3.9269 4.98743 4.21978 5.28032L13.4395 14.5H7.75005C7.33586 14.5001 7.00005 14.8359 7.00005 15.25C7.00005 15.6642 7.33586 16 7.75005 16H15.2501Z"),
            )
        }.build()
        return _ic_arrow_down_right_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownRight20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_right_20,
        contentDescription = null,
    )
}