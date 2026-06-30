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

private var _ic_trash_bin_24: ImageVector? = null

val Icons.ic_trash_bin_24: ImageVector
    get() {
        if (_ic_trash_bin_24 != null) return _ic_trash_bin_24!!
        _ic_trash_bin_24 = ImageVector.Builder(
            name = "ic_trash_bin_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 2C15.3807 2 16.5 3.11929 16.5 4.5V5.5H20C20.5523 5.5 21 5.94772 21 6.5C21 7.05228 20.5523 7.5 20 7.5H19.5V19C19.5 20.6569 18.1569 22 16.5 22H7.5C5.84315 22 4.5 20.6569 4.5 19V7.5H4C3.44772 7.5 3 7.05228 3 6.5C3 5.94772 3.44772 5.5 4 5.5H7.5V4.5C7.5 3.11929 8.61929 2 10 2H14ZM6.5 19C6.5 19.5523 6.94772 20 7.5 20H16.5C17.0523 20 17.5 19.5523 17.5 19V7.5H6.5V19ZM10 4C9.72386 4 9.5 4.22386 9.5 4.5V5.5H14.5V4.5C14.5 4.22386 14.2761 4 14 4H10Z"),
            )
        }.build()
        return _ic_trash_bin_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcTrashBin24Preview() {
    Icon(
        imageVector = Icons.ic_trash_bin_24,
        contentDescription = null,
    )
}