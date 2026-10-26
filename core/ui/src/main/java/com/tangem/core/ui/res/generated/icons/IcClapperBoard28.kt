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

private var _ic_clapper_board_28: ImageVector? = null

val Icons.ic_clapper_board_28: ImageVector
    get() {
        if (_ic_clapper_board_28 != null) return _ic_clapper_board_28!!
        _ic_clapper_board_28 = ImageVector.Builder(
            name = "ic_clapper_board_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.2739 3.0781C20.802 2.93868 21.3611 2.98732 21.854 3.21286L22.061 3.32028L22.2564 3.44821C22.6956 3.7672 23.0133 4.23142 23.148 4.76462L23.7427 7.11911L23.7896 7.35251C23.9615 8.51913 23.2239 9.63854 22.0708 9.94333L15.5444 11.667H21.6558C22.9388 11.667 23.9993 12.7002 23.9995 14V21.584C23.9993 23.4819 22.449 25 20.562 25H8.10596C6.21904 24.9998 4.66872 23.4819 4.66846 21.584V13.0713L4.10401 10.8349C3.64441 9.01347 4.74808 7.17862 6.55518 6.70114L20.2739 3.0781ZM7.16846 21.584C7.16872 22.0787 7.57723 22.4998 8.10596 22.5H20.562C21.0909 22.5 21.4993 22.0788 21.4995 21.584V14.167H7.16846V21.584ZM7.19385 9.11911C6.6925 9.25169 6.40808 9.74911 6.52784 10.2236L6.82081 11.3847L9.63819 10.6396V8.47263L7.19385 9.11911ZM12.1382 7.81247V9.97946L15.2612 9.15524V6.98728L12.1382 7.81247ZM17.7612 6.32712V8.49411L21.2769 7.56638L20.7632 5.53415L17.7612 6.32712Z"),
            )
        }.build()
        return _ic_clapper_board_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClapperBoard28Preview() {
    Icon(
        imageVector = Icons.ic_clapper_board_28,
        contentDescription = null,
    )
}