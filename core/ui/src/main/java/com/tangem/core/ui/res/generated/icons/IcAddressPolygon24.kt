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

private var _ic_address_polygon_24: ImageVector? = null

val Icons.ic_address_polygon_24: ImageVector
    get() {
        if (_ic_address_polygon_24 != null) return _ic_address_polygon_24!!
        _ic_address_polygon_24 = ImageVector.Builder(
            name = "ic_address_polygon_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M7.09277 4.21716C7.66252 3.92737 8.33748 3.92737 8.90723 4.21716L11.9072 5.74353C12.5777 6.08477 12.9999 6.7744 13 7.52673V8.43884C12.9999 8.99101 12.5522 9.43884 12 9.43884C11.4478 9.43884 11.0001 8.99101 11 8.43884V7.52673L8 6.00037L5 7.52673V11.3871L8 12.9125L15.0928 9.30505C15.6625 9.01526 16.3375 9.01526 16.9072 9.30505L19.9072 10.8304C20.5777 11.1717 20.9999 11.8613 21 12.6136V16.474C20.9998 17.2262 20.5776 17.915 19.9072 18.2562L16.9072 19.7826C16.3375 20.0724 15.6625 20.0724 15.0928 19.7826L12.0928 18.2562C11.4224 17.915 11.0002 17.2262 11 16.474V15.5609C11.0001 15.0087 11.4478 14.5609 12 14.5609C12.5522 14.5609 12.9999 15.0087 13 15.5609V16.474L16 17.9994L19 16.474V12.6136L16 11.0873L8.90723 14.6957C8.33749 14.9855 7.66251 14.9855 7.09277 14.6957L4.09277 13.1693C3.42245 12.8281 3.00021 12.1392 3 11.3871V7.52673C3.00008 6.7744 3.42229 6.08477 4.09277 5.74353L7.09277 4.21716Z"),
            )
        }.build()
        return _ic_address_polygon_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAddressPolygon24Preview() {
    Icon(
        imageVector = Icons.ic_address_polygon_24,
        contentDescription = null,
    )
}