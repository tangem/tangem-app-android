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

private var _ic_lock_28: ImageVector? = null

val Icons.ic_lock_28: ImageVector
    get() {
        if (_ic_lock_28 != null) return _ic_lock_28!!
        _ic_lock_28 = ImageVector.Builder(
            name = "ic_lock_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14 1.99976C17.3092 2.00002 19.9646 4.71439 19.9648 8.0271V9.16772C21.9391 9.20661 23.4999 10.8351 23.5 12.8054V22.3611C23.5 24.3555 21.901 25.9998 19.8926 25.9998H8.10742C6.09903 25.9998 4.5 24.3555 4.5 22.3611V12.8054C4.50011 10.8348 6.0614 9.20609 8.03613 9.16772V8.0271C8.03636 4.71437 10.6908 1.99998 14 1.99976ZM8.10742 11.6667C7.51124 11.6667 7.00011 12.1602 7 12.8054V22.3611C7 23.0064 7.51117 23.4998 8.10742 23.4998H19.8926C20.4888 23.4998 21 23.0064 21 22.3611V12.8054C20.9999 12.1602 20.4888 11.6667 19.8926 11.6667H8.10742ZM14 4.49976C12.1029 4.49999 10.5364 6.06346 10.5361 8.0271V9.16675H17.4648V8.0271C17.4646 6.06348 15.897 4.50002 14 4.49976Z"),
            )
        }.build()
        return _ic_lock_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLock28Preview() {
    Icon(
        imageVector = Icons.ic_lock_28,
        contentDescription = null,
    )
}