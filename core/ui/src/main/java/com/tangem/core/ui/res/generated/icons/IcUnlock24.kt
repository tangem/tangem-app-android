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

private var _ic_unlock_24: ImageVector? = null

val Icons.ic_unlock_24: ImageVector
    get() {
        if (_ic_unlock_24 != null) return _ic_unlock_24!!
        _ic_unlock_24 = ImageVector.Builder(
            name = "ic_unlock_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 1C14.7613 1.00003 17 3.23874 17 6V8C18.6573 8 20 9.34272 20 11V19C20 20.6573 18.6573 22 17 22H7C5.34272 22 4 20.6573 4 19V11C4 9.34272 5.34272 8 7 8H15V6C15 4.34331 13.6567 3.00003 12 3C10.6038 3.00006 9.42796 3.95512 9.09473 5.24902C8.95706 5.78374 8.41169 6.10621 7.87695 5.96875C7.34229 5.83105 7.01979 5.28567 7.15723 4.75098C7.71226 2.59482 9.669 1.00006 12 1ZM7 10C6.44728 10 6 10.4473 6 11V19C6 19.5527 6.44728 20 7 20H17C17.5527 20 18 19.5527 18 19V11C18 10.4473 17.5527 10 17 10H7Z"),
            )
        }.build()
        return _ic_unlock_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcUnlock24Preview() {
    Icon(
        imageVector = Icons.ic_unlock_24,
        contentDescription = null,
    )
}