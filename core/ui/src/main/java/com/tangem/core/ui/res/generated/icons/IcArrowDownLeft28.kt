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

private var _ic_arrow_down_left_28: ImageVector? = null

val Icons.ic_arrow_down_left_28: ImageVector
    get() {
        if (_ic_arrow_down_left_28 != null) return _ic_arrow_down_left_28!!
        _ic_arrow_down_left_28 = ImageVector.Builder(
            name = "ic_arrow_down_left_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.74951 23.5C6.05975 23.4996 5.49982 22.9398 5.49951 22.25V11.25C5.4998 10.5602 6.05973 10.0005 6.74951 10C7.43954 10.0002 7.99923 10.56 7.99951 11.25V19.0889L20.8374 5.39555C21.3094 4.89245 22.1005 4.86648 22.604 5.33793C23.1074 5.80989 23.1331 6.60088 22.6616 7.10453L9.63623 21H17.7495C18.4395 21.0002 18.9992 21.56 18.9995 22.25C18.9992 22.94 18.4395 23.4999 17.7495 23.5H6.74951Z"),
            )
        }.build()
        return _ic_arrow_down_left_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownLeft28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_left_28,
        contentDescription = null,
    )
}