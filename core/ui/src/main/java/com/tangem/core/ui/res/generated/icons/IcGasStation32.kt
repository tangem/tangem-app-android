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

private var _ic_gas_station_32: ImageVector? = null

val Icons.ic_gas_station_32: ImageVector
    get() {
        if (_ic_gas_station_32 != null) return _ic_gas_station_32!!
        _ic_gas_station_32 = ImageVector.Builder(
            name = "ic_gas_station_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.1104 4.00024C19.1514 4.00024 20.8329 5.64183 20.833 7.69946V25.0002H26C26.8284 25.0002 27.5 25.6718 27.5 26.5002C27.4999 27.3286 26.8284 28.0002 26 28.0002H6C5.17161 28.0002 4.50007 27.3286 4.5 26.5002C4.5 25.6718 5.17157 25.0002 6 25.0002H6.72168V7.69946C6.72175 5.64198 8.40352 4.00047 10.4443 4.00024H17.1104ZM9.72168 16.844V25.0002H17.833V16.844H9.72168ZM10.4443 7.00024C10.031 7.00047 9.72175 7.32801 9.72168 7.69946V13.844H17.833V7.69946C17.8329 7.32788 17.5239 7.00024 17.1104 7.00024H10.4443Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M21.6006 7.03833C22.1832 6.44969 23.1329 6.44425 23.7217 7.02661L26.4043 9.68188C27.1039 10.3744 27.5 11.3169 27.5 12.303V19.6399C27.4997 21.3935 26.0678 22.789 24.334 22.7893C22.5999 22.7893 21.1672 21.3937 21.167 19.6399V15.7903C21.1672 14.9621 21.8387 14.2903 22.667 14.2903C23.4952 14.2904 24.1668 14.9621 24.167 15.7903V19.6399C24.1673 19.7078 24.2275 19.7893 24.334 19.7893C24.4401 19.789 24.4997 19.7077 24.5 19.6399V12.303C24.5 12.1226 24.4276 11.9461 24.2939 11.8137L21.6123 9.15942C21.0236 8.5768 21.0182 7.62717 21.6006 7.03833Z"),
            )
        }.build()
        return _ic_gas_station_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGasStation32Preview() {
    Icon(
        imageVector = Icons.ic_gas_station_32,
        contentDescription = null,
    )
}