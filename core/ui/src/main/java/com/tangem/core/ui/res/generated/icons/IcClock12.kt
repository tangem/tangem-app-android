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

private var _ic_clock_12: ImageVector? = null

val Icons.ic_clock_12: ImageVector
    get() {
        if (_ic_clock_12 != null) return _ic_clock_12!!
        _ic_clock_12 = ImageVector.Builder(
            name = "ic_clock_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.24857 3.21543C6.52471 3.21543 6.74857 3.43929 6.74857 3.71543V6.21543C6.74857 6.49157 6.52471 6.71543 6.24857 6.71543H4.24857C3.97243 6.71543 3.74857 6.49157 3.74857 6.21543C3.74857 5.93929 3.97243 5.71543 4.24857 5.71543H5.74857V3.71543C5.74857 3.43929 5.97243 3.21543 6.24857 3.21543Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M6 1C8.76142 1 11 3.23858 11 6C11 8.76142 8.76142 11 6 11C3.23858 11 1 8.76142 1 6C1 3.23858 3.23858 1 6 1ZM6 2C3.79086 2 2 3.79086 2 6C2 8.20914 3.79086 10 6 10C8.20914 10 10 8.20914 10 6C10 3.79086 8.20914 2 6 2Z"),
            )
        }.build()
        return _ic_clock_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClock12Preview() {
    Icon(
        imageVector = Icons.ic_clock_12,
        contentDescription = null,
    )
}