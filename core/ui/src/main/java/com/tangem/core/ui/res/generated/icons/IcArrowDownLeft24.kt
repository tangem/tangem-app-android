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

private var _ic_arrow_down_left_24: ImageVector? = null

val Icons.ic_arrow_down_left_24: ImageVector
    get() {
        if (_ic_arrow_down_left_24 != null) return _ic_arrow_down_left_24!!
        _ic_arrow_down_left_24 = ImageVector.Builder(
            name = "ic_arrow_down_left_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.49951 19.5C4.94787 19.4996 4.50005 19.0516 4.49951 18.5V9.00001C4.49979 8.44819 4.94771 8.00038 5.49951 8.00001C6.05163 8.00001 6.49924 8.44796 6.49951 9.00001V16.086L17.7925 4.79298C18.1829 4.40276 18.816 4.4029 19.2065 4.79298C19.5967 5.18347 19.5968 5.8166 19.2065 6.20704L7.91357 17.5H14.9995C15.5516 17.5 15.9992 17.948 15.9995 18.5C15.999 19.0518 15.5515 19.5 14.9995 19.5H5.49951Z"),
            )
        }.build()
        return _ic_arrow_down_left_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowDownLeft24Preview() {
    Icon(
        imageVector = Icons.ic_arrow_down_left_24,
        contentDescription = null,
    )
}