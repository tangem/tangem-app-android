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

private var _ic_gas_station_28: ImageVector? = null

val Icons.ic_gas_station_28: ImageVector
    get() {
        if (_ic_gas_station_28 != null) return _ic_gas_station_28!!
        _ic_gas_station_28 = ImageVector.Builder(
            name = "ic_gas_station_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M14.9717 3.50024C16.7317 3.5003 18.167 4.92317 18.167 6.68774V22.0002H22.75C23.4404 22.0002 24 22.5599 24 23.2502C23.9999 23.9405 23.4403 24.5002 22.75 24.5002H5.25C4.55969 24.5002 4.00007 23.9405 4 23.2502C4 22.5599 4.55964 22.0002 5.25 22.0002H5.94434V6.68774C5.94434 4.92317 7.37869 3.5003 9.13867 3.50024H14.9717ZM8.44434 14.6721V22.0002H15.667V14.6721H8.44434ZM9.13867 6.00024C8.75099 6.0003 8.44434 6.31229 8.44434 6.68774V12.1721H15.667V6.68774C15.667 6.31229 15.3594 6.0003 14.9717 6.00024H9.13867Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.9473 6.15259C19.4344 5.66369 20.2258 5.66179 20.7148 6.14868L23.0625 8.48755C23.6621 9.08498 24 9.89658 24 10.7434V17.2063C24 18.7034 22.7825 19.9094 21.291 19.9094C19.7997 19.9092 18.5831 18.7032 18.583 17.2063V13.8157C18.583 13.1254 19.1427 12.5657 19.833 12.5657C20.5233 12.5657 21.083 13.1253 21.083 13.8157V17.2063C21.0831 17.3141 21.172 17.4092 21.291 17.4094C21.4103 17.4094 21.5 17.3142 21.5 17.2063V10.7434C21.5 10.5626 21.4275 10.3883 21.2979 10.259L18.9512 7.92017C18.4623 7.43304 18.4605 6.64165 18.9473 6.15259Z"),
            )
        }.build()
        return _ic_gas_station_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGasStation28Preview() {
    Icon(
        imageVector = Icons.ic_gas_station_28,
        contentDescription = null,
    )
}