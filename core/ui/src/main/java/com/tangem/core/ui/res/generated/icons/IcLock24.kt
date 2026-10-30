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

private var _ic_lock_24: ImageVector? = null

val Icons.ic_lock_24: ImageVector
    get() {
        if (_ic_lock_24 != null) return _ic_lock_24!!
        _ic_lock_24 = ImageVector.Builder(
            name = "ic_lock_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C14.7613 2 17 4.23872 17 7V8C18.6573 8 20 9.34272 20 11V19C20 20.6573 18.6573 22 17 22H7C5.34272 22 4 20.6573 4 19V11C4 9.34272 5.34272 8 7 8V7C7 4.23872 9.23872 2 12 2ZM7 10C6.44728 10 6 10.4473 6 11V19C6 19.5527 6.44728 20 7 20H17C17.5527 20 18 19.5527 18 19V11C18 10.4473 17.5527 10 17 10H7ZM12 4C10.3433 4 9 5.34328 9 7V8H15V7C15 5.34328 13.6567 4 12 4Z"),
            )
        }.build()
        return _ic_lock_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcLock24Preview() {
    Icon(
        imageVector = Icons.ic_lock_24,
        contentDescription = null,
    )
}