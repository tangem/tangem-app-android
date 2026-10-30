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

private var _ic_gas_station_12: ImageVector? = null

val Icons.ic_gas_station_12: ImageVector
    get() {
        if (_ic_gas_station_12 != null) return _ic_gas_station_12!!
        _ic_gas_station_12 = ImageVector.Builder(
            name = "ic_gas_station_12",
            defaultWidth = 12.dp,
            defaultHeight = 12.dp,
            viewportWidth = 12f,
            viewportHeight = 12f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M6.44434 1.5C7.18308 1.50006 7.83301 2.07161 7.83301 2.83789V9.5H10C10.2761 9.5 10.5 9.72386 10.5 10C10.4999 10.2761 10.2761 10.5 10 10.5H2C1.72389 10.5 1.50006 10.2761 1.5 10C1.5 9.72386 1.72386 9.5 2 9.5H2.38867V2.83789C2.38867 2.07174 3.03875 1.50025 3.77734 1.5H6.44434ZM3.38867 6.25V9.5H6.83301V6.25H3.38867ZM3.77734 2.5C3.53459 2.50024 3.38867 2.67886 3.38867 2.83789V5.25H6.83301V2.83789C6.83301 2.67878 6.68732 2.50005 6.44434 2.5H3.77734Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.30273 2.64453C8.49206 2.44394 8.80888 2.43405 9.00977 2.62305L10.083 3.63477C10.3463 3.88293 10.4998 4.22636 10.5 4.59082V7.38574C10.5 8.0362 9.94984 8.51438 9.33398 8.51465C8.71792 8.51465 8.16699 8.03638 8.16699 7.38574V5.91992C8.16715 5.64391 8.39095 5.41992 8.66699 5.41992C8.94295 5.42002 9.16684 5.64398 9.16699 5.91992V7.38574C9.16699 7.4292 9.21367 7.51465 9.33398 7.51465C9.45392 7.51441 9.5 7.42914 9.5 7.38574V4.59082C9.49984 4.51131 9.46717 4.42807 9.39746 4.3623L8.32422 3.35156C8.12342 3.16224 8.11363 2.84548 8.30273 2.64453Z"),
            )
        }.build()
        return _ic_gas_station_12!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGasStation12Preview() {
    Icon(
        imageVector = Icons.ic_gas_station_12,
        contentDescription = null,
    )
}