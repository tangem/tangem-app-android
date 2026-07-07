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

private var _ic_card_24: ImageVector? = null

val Icons.ic_card_24: ImageVector
    get() {
        if (_ic_card_24 != null) return _ic_card_24!!
        _ic_card_24 = ImageVector.Builder(
            name = "ic_card_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.57031 14C10.1225 14.0002 10.5703 14.4478 10.5703 15C10.5703 15.5522 10.1225 15.9998 9.57031 16H7C6.44772 16 6 15.5523 6 15C6 14.4477 6.44772 14 7 14H9.57031Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18 4C20.2093 4 22 5.79072 22 8V16C22 18.2093 20.2093 20 18 20H6C3.79072 20 2 18.2093 2 16V8C2 5.79072 3.79072 4 6 4H18ZM4 16C4 17.1047 4.89528 18 6 18H18C19.1047 18 20 17.1047 20 16V10H4V16ZM6 6C4.89528 6 4 6.89528 4 8H20C20 6.89528 19.1047 6 18 6H6Z"),
            )
        }.build()
        return _ic_card_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcCard24Preview() {
    Icon(
        imageVector = Icons.ic_card_24,
        contentDescription = null,
    )
}