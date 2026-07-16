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

private var _ic_clock_24: ImageVector? = null

val Icons.ic_clock_24: ImageVector
    get() {
        if (_ic_clock_24 != null) return _ic_clock_24!!
        _ic_clock_24 = ImageVector.Builder(
            name = "ic_clock_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.5 6C13.0523 6 13.5 6.44772 13.5 7V12.5C13.5 13.0523 13.0523 13.5 12.5 13.5H8.5C7.94772 13.5 7.5 13.0523 7.5 12.5C7.5 11.9477 7.94772 11.5 8.5 11.5H11.5V7C11.5 6.44772 11.9477 6 12.5 6Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12 2C17.5228 2 22 6.47715 22 12C22 17.5228 17.5228 22 12 22C6.47715 22 2 17.5228 2 12C2 6.47715 6.47715 2 12 2ZM12 4C7.58172 4 4 7.58172 4 12C4 16.4183 7.58172 20 12 20C16.4183 20 20 16.4183 20 12C20 7.58172 16.4183 4 12 4Z"),
            )
        }.build()
        return _ic_clock_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClock24Preview() {
    Icon(
        imageVector = Icons.ic_clock_24,
        contentDescription = null,
    )
}