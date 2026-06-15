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

private var _ic_clock_20: ImageVector? = null

val Icons.ic_clock_20: ImageVector
    get() {
        if (_ic_clock_20 != null) return _ic_clock_20!!
        _ic_clock_20 = ImageVector.Builder(
            name = "ic_clock_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.3564 5.3584C10.7706 5.3585 11.1064 5.69425 11.1064 6.1084V10.25C11.1064 10.6641 10.7705 10.9999 10.3564 11H7.25C6.83594 10.9998 6.50004 10.6641 6.5 10.25C6.50004 9.83591 6.83594 9.50015 7.25 9.5H9.60645V6.1084C9.60645 5.69428 9.94236 5.35855 10.3564 5.3584Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
                pathData = addPathNodes("M10.4092 2.01074C14.6352 2.2248 17.996 5.71889 17.9961 9.99805C17.996 14.4151 14.4151 17.996 9.99805 17.9961C5.581 17.996 2.00007 14.4151 2 9.99805C2.00005 5.58099 5.58099 2.00005 9.99805 2L10.4092 2.01074ZM9.99805 3.5C6.40942 3.50005 3.50005 6.40942 3.5 9.99805C3.50007 13.5867 6.40943 16.496 9.99805 16.4961C13.5867 16.496 16.496 13.5867 16.4961 9.99805C16.496 6.40943 13.5867 3.50007 9.99805 3.5Z"),
            )
        }.build()
        return _ic_clock_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClock20Preview() {
    Icon(
        imageVector = Icons.ic_clock_20,
        contentDescription = null,
    )
}