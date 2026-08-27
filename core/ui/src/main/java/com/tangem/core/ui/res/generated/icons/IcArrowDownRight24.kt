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

private var _ic_arrow_down_right_24: ImageVector? = null

val Icons.ic_arrow_down_right_24: ImageVector
    get() {
        if (_ic_arrow_down_right_24 != null) return _ic_arrow_down_right_24!!
        _ic_arrow_down_right_24 = ImageVector.Builder(
            name = "ic_arrow_down_right_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.5004 19.5C19.0521 19.4996 19.4999 19.0516 19.5004 18.5V9.00001C19.5002 8.44819 19.0522 8.00038 18.5004 8.00001C17.9483 8.00001 17.5007 8.44796 17.5004 9.00001V16.086L6.20746 4.79298C5.81705 4.40276 5.18389 4.4029 4.7934 4.79298C4.40321 5.18347 4.40314 5.8166 4.7934 6.20704L16.0864 17.5H9.00043C8.44831 17.5 8.0007 17.948 8.00043 18.5C8.00097 19.0518 8.44848 19.5 9.00043 19.5H18.5004Z"),
            )
        }.build()
        return _ic_arrow_down_right_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownRight24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_right_24,
        contentDescription = null,
    )
}