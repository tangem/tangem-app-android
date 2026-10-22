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

private var _ic_gas_station_24: ImageVector? = null

val Icons.ic_gas_station_24: ImageVector
    get() {
        if (_ic_gas_station_24 != null) return _ic_gas_station_24!!
        _ic_gas_station_24 = ImageVector.Builder(
            name = "ic_gas_station_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.8289 3.00024C14.3064 3.00047 15.5037 4.19843 15.5037 5.67603V19.0002H19.531C20.0833 19.0002 20.531 19.448 20.531 20.0002C20.5309 20.5524 20.0832 21.0002 19.531 21.0002H4.44995C3.89773 21.0002 3.45005 20.5524 3.44995 20.0002C3.44995 19.448 3.89767 19.0002 4.44995 19.0002H5.12573V5.67603C5.12573 4.19832 6.32382 3.0003 7.80151 3.00024H12.8289ZM7.12573 12.5002V19.0002H13.5037V12.5002H7.12573ZM7.80151 5.00024C7.4284 5.0003 7.12573 5.30289 7.12573 5.67603V10.5002H13.5037V5.67603C13.5037 5.303 13.2018 5.00047 12.8289 5.00024H7.80151Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.3103 5.2688C16.7008 4.87831 17.3338 4.87835 17.7244 5.2688L19.7468 7.29126C20.2486 7.79303 20.531 8.47423 20.531 9.18384V14.7727C20.531 16.019 19.5205 17.0295 18.2742 17.0295C17.0278 17.0295 16.0173 16.0191 16.0173 14.7727V11.8411C16.0174 11.2888 16.4651 10.8411 17.0173 10.8411C17.5696 10.8411 18.0173 11.2888 18.0173 11.8411V14.7727C18.0173 14.9145 18.1324 15.0295 18.2742 15.0295C18.4159 15.0295 18.531 14.9145 18.531 14.7727V9.18384C18.531 9.00466 18.4595 8.83202 18.3328 8.70532L16.3103 6.68286C15.9198 6.29233 15.9198 5.6593 16.3103 5.2688Z"),
            )
        }.build()
        return _ic_gas_station_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGasStation24Preview() {
    Icon(
        imageVector = Icons.ic_gas_station_24,
        contentDescription = null,
    )
}