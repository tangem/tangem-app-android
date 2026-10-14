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

private var _ic_clapper_board_24: ImageVector? = null

val Icons.ic_clapper_board_24: ImageVector
    get() {
        if (_ic_clapper_board_24 != null) return _ic_clapper_board_24!!
        _ic_clapper_board_24 = ImageVector.Builder(
            name = "ic_clapper_board_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.7706 2.0676C18.2882 1.9296 18.8403 2.00562 19.3019 2.27756C19.7631 2.5496 20.0957 2.99539 20.2257 3.51487L20.7696 5.68869C21.0344 6.74827 20.4 7.82409 19.3448 8.10569L12.2403 10.0002H19.0001C20.1047 10.0002 21.0001 10.8956 21.0001 12.0002V19.0002C21 20.657 19.6569 22.0002 18.0001 22.0002H6.61142C4.95476 22.0001 3.61155 20.6569 3.61142 19.0002V11.1252L3.08994 9.03928C2.69263 7.44976 3.64461 5.83464 5.22763 5.41233L17.7706 2.0676ZM5.61142 19.0002C5.61155 19.5523 6.05933 20.0001 6.61142 20.0002H18.0001C18.5523 20.0002 19 19.5524 19.0001 19.0002V12.0002H5.61142V19.0002ZM5.74326 7.34494C5.21575 7.48581 4.898 8.02419 5.03037 8.55393L5.33408 9.7717L8.15439 9.01975V6.70139L5.74326 7.34494ZM10.1544 6.16819V8.48654L13.297 7.64865V5.33029L10.1544 6.16819ZM15.297 4.79709V7.11545L18.8292 6.17404L18.2862 4.00022L15.297 4.79709Z"),
            )
        }.build()
        return _ic_clapper_board_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClapperBoard24Preview() {
    Icon(
        imageVector = Icons.ic_clapper_board_24,
        contentDescription = null,
    )
}