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

private var _ic_clock_28: ImageVector? = null

val Icons.ic_clock_28: ImageVector
    get() {
        if (_ic_clock_28 != null) return _ic_clock_28!!
        _ic_clock_28 = ImageVector.Builder(
            name = "ic_clock_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.522 6.77881C15.2123 6.77881 15.7719 7.33851 15.772 8.02881V14.5981C15.7717 15.2883 15.2122 15.8481 14.522 15.8481H9.74463C9.05481 15.8477 8.49492 15.288 8.49463 14.5981C8.4947 13.9081 9.05468 13.3486 9.74463 13.3481H13.272V8.02881C13.272 7.33887 13.8321 6.77938 14.522 6.77881Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.3101 2.00439C20.7939 2.16878 25.9993 7.47709 25.9995 14.0005C25.9993 20.6274 20.6274 25.9993 14.0005 25.9995C7.37353 25.9993 2.00068 20.6274 2.00049 14.0005C2.00072 7.37357 7.37355 2.00065 14.0005 2.00049L14.3101 2.00439ZM14.0005 4.50049C8.75426 4.50065 4.50072 8.75428 4.50049 14.0005C4.50068 19.2467 8.75424 23.4993 14.0005 23.4995C19.2467 23.4993 23.4993 19.2467 23.4995 14.0005C23.4993 8.7543 19.2467 4.50068 14.0005 4.50049Z"),
            )
        }.build()
        return _ic_clock_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcClock28Preview() {
    Icon(
        imageVector = Icons.ic_clock_28,
        contentDescription = null,
    )
}