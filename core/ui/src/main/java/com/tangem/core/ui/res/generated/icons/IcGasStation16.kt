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

private var _ic_gas_station_16: ImageVector? = null

val Icons.ic_gas_station_16: ImageVector
    get() {
        if (_ic_gas_station_16 != null) return _ic_gas_station_16!!
        _ic_gas_station_16 = ImageVector.Builder(
            name = "ic_gas_station_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.54102 2C9.50696 2 10.25 2.8069 10.25 3.75098V12.75H12.875C13.2202 12.75 13.5 13.0298 13.5 13.375C13.4999 13.7201 13.2201 14 12.875 14H3.125C2.77986 14 2.50006 13.7201 2.5 13.375C2.5 13.0298 2.77982 12.75 3.125 12.75H3.58301V3.75098C3.58301 2.80706 4.32531 2.00027 5.29102 2H8.54102ZM4.83301 8.28906V12.75H9V8.28906H4.83301ZM5.29102 3.25C5.06051 3.25028 4.83301 3.45168 4.83301 3.75098V7.03906H9V3.75098C9 3.45147 8.77169 3.25 8.54102 3.25H5.29102Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8164 3.50195C11.065 3.26283 11.4609 3.27016 11.7002 3.51855L13.0078 4.87793C13.3253 5.20791 13.4999 5.65074 13.5 6.10742V9.86328C13.4998 10.6517 12.8787 11.332 12.0625 11.332C11.2463 11.332 10.6252 10.6517 10.625 9.86328V7.89258C10.6251 7.54752 10.9049 7.26758 11.25 7.26758C11.5951 7.26759 11.8749 7.54753 11.875 7.89258V9.86328C11.8752 10.0071 11.9815 10.082 12.0625 10.082C12.1435 10.082 12.2498 10.0071 12.25 9.86328V6.10742C12.2499 5.96707 12.1961 5.83634 12.1074 5.74414L10.7998 4.38574C10.5606 4.13708 10.5679 3.74128 10.8164 3.50195Z"),
            )
        }.build()
        return _ic_gas_station_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcGasStation16Preview() {
    Icon(
        imageVector = Icons.ic_gas_station_16,
        contentDescription = null,
    )
}