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

private var _ic_gas_station_20: ImageVector? = null

val Icons.ic_gas_station_20: ImageVector
    get() {
        if (_ic_gas_station_20 != null) return _ic_gas_station_20!!
        _ic_gas_station_20 = ImageVector.Builder(
            name = "ic_gas_station_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.6943 2.50024C11.8881 2.5003 12.833 3.48193 12.833 4.66431V16.0002H16.25C16.6642 16.0002 17 16.336 17 16.7502C16.9999 17.1644 16.6642 17.5002 16.25 17.5002H3.75C3.33584 17.5002 3.00008 17.1644 3 16.7502C3 16.336 3.33579 16.0002 3.75 16.0002H4.38867V4.66431C4.38867 3.48206 5.33375 2.50051 6.52734 2.50024H10.6943ZM5.88867 10.3284V16.0002H11.333V10.3284H5.88867ZM6.52734 4.00024C6.18724 4.00052 5.88867 4.28519 5.88867 4.66431V8.82837H11.333V4.66431C11.333 4.28505 11.0346 4.00031 10.6943 4.00024H6.52734Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.6416 4.38208C13.9371 4.09197 14.4119 4.09641 14.7021 4.39185L16.3789 6.0979C16.7782 6.50457 16.9999 7.05394 17 7.62329V12.3401C16.9998 13.3269 16.2106 14.1503 15.209 14.1506C14.2071 14.1506 13.4172 13.3271 13.417 12.3401V9.86548C13.4171 9.45139 13.7529 9.11548 14.167 9.11548C14.581 9.11559 14.9168 9.45146 14.917 9.86548V12.3401C14.9172 12.5239 15.0606 12.6506 15.209 12.6506C15.3572 12.6503 15.4998 12.5237 15.5 12.3401V7.62329C15.4999 7.4433 15.4296 7.27305 15.3086 7.14966L13.6318 5.44263C13.3417 5.14715 13.3462 4.67231 13.6416 4.38208Z"),
            )
        }.build()
        return _ic_gas_station_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGasStation20Preview() {
    Icon(
        imageVector = Icons.ic_gas_station_20,
        contentDescription = null,
    )
}