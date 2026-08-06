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

private var _ic_arrow_down_right_28: ImageVector? = null

val Icons.ic_arrow_down_right_28: ImageVector
    get() {
        if (_ic_arrow_down_right_28 != null) return _ic_arrow_down_right_28!!
        _ic_arrow_down_right_28 = ImageVector.Builder(
            name = "ic_arrow_down_right_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.2505 23.5C21.9404 23.4996 22.5003 22.94 22.5005 22.25V11.25C22.5003 10.56 21.9404 10.0004 21.2505 10C20.5603 10.0001 20.0006 10.5599 20.0005 11.25V19.0889L7.16257 5.39552C6.69058 4.89225 5.89958 4.86637 5.39597 5.3379C4.89258 5.80992 4.86671 6.60087 5.33836 7.10451L18.3637 21H10.2505C9.56032 21.0001 9.00062 21.5599 9.00047 22.25C9.00065 22.9401 9.56034 23.4999 10.2505 23.5H21.2505Z"),
            )
        }.build()
        return _ic_arrow_down_right_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownRight28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_right_28,
        contentDescription = null,
    )
}