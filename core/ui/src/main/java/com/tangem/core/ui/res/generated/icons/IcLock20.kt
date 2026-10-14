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

private var _ic_lock_20: ImageVector? = null

val Icons.ic_lock_20: ImageVector
    get() {
        if (_ic_lock_20 != null) return _ic_lock_20!!
        _ic_lock_20 = ImageVector.Builder(
            name = "ic_lock_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10 2.5C12.071 2.5 13.75 4.17904 13.75 6.25V7C14.993 7 16 8.00704 16 9.25V15.25C16 16.493 14.993 17.5 13.75 17.5H6.25C5.00704 17.5 4 16.493 4 15.25V9.25C4 8.00704 5.00704 7 6.25 7V6.25C6.25 4.17904 7.92904 2.5 10 2.5ZM6.25 8.5C5.83546 8.5 5.5 8.83546 5.5 9.25V15.25C5.5 15.6645 5.83546 16 6.25 16H13.75C14.1645 16 14.5 15.6645 14.5 15.25V9.25C14.5 8.83546 14.1645 8.5 13.75 8.5H6.25ZM10 4C8.75746 4 7.75 5.00746 7.75 6.25V7H12.25V6.25C12.25 5.00746 11.2425 4 10 4Z"),
            )
        }.build()
        return _ic_lock_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLock20Preview() {
    Icon(
        imageVector = Icons.ic_lock_20,
        contentDescription = null,
    )
}