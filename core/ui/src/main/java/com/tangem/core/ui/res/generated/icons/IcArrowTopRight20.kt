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

private var _ic_arrow_top_right_20: ImageVector? = null

val Icons.ic_arrow_top_right_20: ImageVector
    get() {
        if (_ic_arrow_top_right_20 != null) return _ic_arrow_top_right_20!!
        _ic_arrow_top_right_20 = ImageVector.Builder(
            name = "ic_arrow_top_right_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M15.2501 4C15.4489 4.00002 15.6397 4.07909 15.7803 4.21973C15.921 4.36038 16.0001 4.5511 16.0001 4.75V12.25C16.0001 12.6642 15.6642 13 15.2501 13C14.8358 13 14.5001 12.6642 14.5001 12.25V6.56055L5.28032 15.7803C4.98744 16.0731 4.51267 16.0731 4.21978 15.7803C3.9269 15.4874 3.9269 15.0126 4.21978 14.7197L13.4395 5.5H7.75005C7.33586 5.49997 7.00005 5.16419 7.00005 4.75C7.00005 4.33581 7.33586 4.00003 7.75005 4H15.2501Z"),
            )
        }.build()
        return _ic_arrow_top_right_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopRight20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_right_20,
        contentDescription = null,
    )
}