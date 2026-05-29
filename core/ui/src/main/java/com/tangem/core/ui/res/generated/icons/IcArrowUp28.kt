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

private var _ic_arrow_up_28: ImageVector? = null

val Icons.ic_arrow_up_28: ImageVector
    get() {
        if (_ic_arrow_up_28 != null) return _ic_arrow_up_28!!
        _ic_arrow_up_28 = ImageVector.Builder(
            name = "ic_arrow_up_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M23.6427 11.249C24.1271 11.7405 24.1212 12.532 23.63 13.0166C23.1384 13.5009 22.347 13.4952 21.8624 13.0039L15.2511 6.29785L15.2511 24.75C15.251 25.4401 14.6912 25.9998 14.0011 26C13.3111 25.9997 12.7512 25.4401 12.7511 24.75L12.7511 6.29883L6.14075 13.0039C5.65612 13.4952 4.86375 13.501 4.37219 13.0166C3.88106 12.532 3.87536 11.7406 4.3595 11.249L13.1114 2.37207C13.3462 2.13429 13.667 2.00016 14.0011 2C14.3355 2.00012 14.6569 2.13402 14.8917 2.37207L23.6427 11.249Z"),
            )
        }.build()
        return _ic_arrow_up_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowUp28Preview() {
    Icon(
        imageVector = Icons.ic_arrow_up_28,
        contentDescription = null,
    )
}