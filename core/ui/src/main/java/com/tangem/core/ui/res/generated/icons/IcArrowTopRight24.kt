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

private var _ic_arrow_top_right_24: ImageVector? = null

val Icons.ic_arrow_top_right_24: ImageVector
    get() {
        if (_ic_arrow_top_right_24 != null) return _ic_arrow_top_right_24!!
        _ic_arrow_top_right_24 = ImageVector.Builder(
            name = "ic_arrow_top_right_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.5004 4.5C19.0521 4.50037 19.4999 4.9484 19.5004 5.5V15C19.5002 15.5518 19.0522 15.9996 18.5004 16C17.9483 16 17.5007 15.5521 17.5004 15V7.91406L6.20746 19.207C5.81705 19.5973 5.18389 19.5971 4.7934 19.207C4.40321 18.8165 4.40314 18.1834 4.7934 17.793L16.0864 6.5H9.00043C8.44831 6.5 8.0007 6.05205 8.00043 5.5C8.00097 4.94817 8.44848 4.5 9.00043 4.5H18.5004Z"),
            )
        }.build()
        return _ic_arrow_top_right_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowTopRight24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_top_right_24,
        contentDescription = null,
    )
}