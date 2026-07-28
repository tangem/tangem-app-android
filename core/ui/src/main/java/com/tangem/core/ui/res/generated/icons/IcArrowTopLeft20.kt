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

private var _ic_arrow_top_left_20: ImageVector? = null

val Icons.ic_arrow_top_left_20: ImageVector
    get() {
        if (_ic_arrow_top_left_20 != null) return _ic_arrow_top_left_20!!
        _ic_arrow_top_left_20 = ImageVector.Builder(
            name = "ic_arrow_top_left_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M4.74988 4C4.55099 4.00002 4.36024 4.07909 4.2196 4.21973C4.07897 4.36038 3.99988 4.5511 3.99988 4.75V12.25C3.99988 12.6642 4.33569 13 4.74988 13C5.16409 13 5.49988 12.6642 5.49988 12.25V6.56055L14.7196 15.7803C15.0125 16.0731 15.4873 16.0731 15.7802 15.7803C16.073 15.4874 16.073 15.0126 15.7802 14.7197L6.56042 5.5H12.2499C12.6641 5.49997 12.9999 5.16419 12.9999 4.75C12.9999 4.33581 12.6641 4.00003 12.2499 4H4.74988Z"),
            )
        }.build()
        return _ic_arrow_top_left_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopLeft20Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_left_20,
        contentDescription = null,
    )
}