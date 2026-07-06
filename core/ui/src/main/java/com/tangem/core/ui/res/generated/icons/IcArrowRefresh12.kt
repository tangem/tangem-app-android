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

private var _ic_arrow_refresh_12: ImageVector? = null

val Icons.ic_arrow_refresh_12: ImageVector
    get() {
        if (_ic_arrow_refresh_12 != null) return _ic_arrow_refresh_12!!
        _ic_arrow_refresh_12 = ImageVector.Builder(
            name = "ic_arrow_refresh_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.96777 5.4668C10.2623 5.4668 10.501 5.70545 10.501 6C10.501 8.48579 8.48581 10.501 6 10.501C4.62026 10.5007 3.39017 9.87706 2.56641 8.90039V9.30664C2.56606 9.6009 2.32754 9.83984 2.0332 9.83984C1.73913 9.83953 1.50035 9.6007 1.5 9.30664V7.65332C1.5 7.35896 1.73892 7.12043 2.0332 7.12012H3.68652C3.98107 7.12012 4.21973 7.35877 4.21973 7.65332C4.21954 7.94772 3.98096 8.18652 3.68652 8.18652H3.35938C3.98887 8.94784 4.93716 9.43331 6 9.43359C7.89672 9.43359 9.43457 7.89668 9.43457 6C9.43457 5.7056 9.67343 5.46705 9.96777 5.4668Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.00098 1.5C7.38045 1.50006 8.61056 2.12258 9.43457 3.09863V2.69434C9.43457 2.39994 9.67344 2.16138 9.96777 2.16113C10.2623 2.16113 10.501 2.39978 10.501 2.69434V4.34766C10.5006 4.64187 10.2621 4.88086 9.96777 4.88086H8.31445C8.02049 4.88046 7.78164 4.64162 7.78125 4.34766C7.78125 4.05335 8.02024 3.81486 8.31445 3.81445H8.64355C8.01382 3.05222 7.06456 2.56647 6.00098 2.56641C4.10455 2.56659 2.56762 4.10363 2.56738 6C2.56713 6.29422 2.32841 6.53399 2.03418 6.53418C1.73978 6.53418 1.50123 6.29434 1.50098 6C1.50121 3.51452 3.51547 1.50018 6.00098 1.5Z"),
            )
        }.build()
        return _ic_arrow_refresh_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcArrowRefresh12Preview() {
    Icon(
        imageVector = Icons.ic_arrow_refresh_12,
        contentDescription = null,
    )
}