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

private var _ic_dots_vertical_20: ImageVector? = null

val Icons.ic_dots_vertical_20: ImageVector
    get() {
        if (_ic_dots_vertical_20 != null) return _ic_dots_vertical_20!!
        _ic_dots_vertical_20 = ImageVector.Builder(
            name = "ic_dots_vertical_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99804 13.6729C10.6317 13.673 11.1504 14.1879 11.1504 14.8271C11.15 15.463 10.6347 15.9801 10.001 15.9805C9.40643 15.9804 8.9155 15.5255 8.85644 14.9443L8.85058 14.8271C8.84789 14.1866 9.3681 13.6731 9.99804 13.6729Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99804 8.82422C10.6316 8.82442 11.1502 9.33944 11.1504 9.97852C11.1502 10.6146 10.6349 11.1315 10.001 11.1318C9.40628 11.1318 8.91529 10.677 8.85644 10.0957L8.85058 9.97852C8.84803 9.33805 9.36819 8.82442 9.99804 8.82422Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.99804 3.97656C10.6316 3.97676 11.1503 4.49172 11.1504 5.13086C11.1502 5.7669 10.6349 6.28382 10.001 6.28418C9.40628 6.2841 8.91529 5.82938 8.85644 5.24805L8.85058 5.13086C8.84796 4.49033 9.36814 3.97677 9.99804 3.97656Z"),
            )
        }.build()
        return _ic_dots_vertical_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcDotsVertical20Preview() {
    Icon(
        imageVector = Icons.ic_dots_vertical_20,
        contentDescription = null,
    )
}