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

private var _ic_clapper_board_12: ImageVector? = null

val Icons.ic_clapper_board_12: ImageVector
    get() {
        if (_ic_clapper_board_12 != null) return _ic_clapper_board_12!!
        _ic_clapper_board_12 = ImageVector.Builder(
            name = "ic_clapper_board_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.59862 1.51259C8.81542 1.47693 9.03854 1.519 9.2285 1.63271C9.41815 1.74638 9.56026 1.92246 9.63279 2.12782L9.66014 2.21669L9.89744 3.18251L9.91502 3.27528C9.98144 3.73803 9.69895 4.19516 9.23143 4.32216L6.5244 5.05556H9.06248C9.5875 5.05569 9.99992 5.48589 9.99998 5.99989V9.11122C9.99982 9.8706 9.39156 10.4998 8.62498 10.4999H3.64256C2.87587 10.4999 2.26772 9.87068 2.26756 9.11122V5.61513L2.041 4.69521C1.86184 3.96673 2.2888 3.2163 3.01854 3.01845L8.50682 1.53212L8.59862 1.51259ZM3.26756 9.11122C3.26772 9.33333 3.44298 9.49989 3.64256 9.49989H8.62498C8.82447 9.49976 8.99982 9.33324 8.99998 9.11122V6.05556H3.26756V9.11122ZM3.28026 3.98427C3.08682 4.0368 2.95921 4.2436 3.0117 4.45692L3.12987 4.93837L4.25487 4.63368V3.71962L3.28026 3.98427ZM5.25487 3.44814V4.3622L6.50487 4.02431V3.11025L5.25487 3.44814ZM7.50487 2.83876V3.75282L8.91404 3.37196L8.70311 2.51454L7.50487 2.83876Z"),
            )
        }.build()
        return _ic_clapper_board_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClapperBoard12Preview() {
    Icon(
        imageVector = Icons.ic_clapper_board_12,
        contentDescription = null,
    )
}