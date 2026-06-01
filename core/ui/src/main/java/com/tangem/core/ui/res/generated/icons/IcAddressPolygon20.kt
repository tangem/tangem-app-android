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

private var _ic_address_polygon_20: ImageVector? = null

val Icons.ic_address_polygon_20: ImageVector
    get() {
        if (_ic_address_polygon_20 != null) return _ic_address_polygon_20!!
        _ic_address_polygon_20 = ImageVector.Builder(
            name = "ic_address_polygon_20",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 20f,
            viewportHeight = 20f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M5.78125 3.21402C6.30813 2.92909 6.94286 2.92905 7.46973 3.21402L9.82031 4.48453C10.3935 4.79465 10.751 5.39437 10.751 6.04605V6.80484C10.7504 7.21849 10.4147 7.55467 10.001 7.55484C9.5871 7.55484 9.25152 7.21859 9.25098 6.80484V6.04605C9.25095 5.94518 9.19511 5.85194 9.10645 5.80386L6.75586 4.53335C6.67424 4.48924 6.57575 4.48921 6.49414 4.53335L4.14453 5.80386C4.05587 5.85194 4.00002 5.94518 4 6.04605V9.38882C4.00037 9.48941 4.056 9.58215 4.14453 9.63003L6.49414 10.9015C6.57557 10.9455 6.6744 10.9454 6.75586 10.9015L12.5312 7.7775C13.0582 7.4925 13.6938 7.4925 14.2207 7.7775L16.5703 9.04898C17.1435 9.35911 17.501 9.9588 17.501 10.6105V13.9523C17.5008 14.6039 17.1434 15.2038 16.5703 15.5138L14.2207 16.7853C13.6939 17.0701 13.058 17.0701 12.5312 16.7853L10.1816 15.5138C9.6085 15.2038 9.25119 14.6039 9.25098 13.9523V13.1945C9.25098 12.7803 9.58676 12.4445 10.001 12.4445C10.415 12.4447 10.751 12.7804 10.751 13.1945V13.9523C10.7512 14.053 10.806 14.1465 10.8945 14.1945L13.2451 15.466C13.3266 15.5099 13.4254 15.5099 13.5068 15.466L15.8574 14.1945C15.946 14.1465 16.0008 14.053 16.001 13.9523V10.6105C16.001 10.5097 15.946 10.4164 15.8574 10.3683L13.5068 9.09683C13.4253 9.05275 13.3267 9.05274 13.2451 9.09683L7.46973 12.2209C6.94305 12.5056 6.30796 12.5055 5.78125 12.2209L3.43066 10.9494C2.85767 10.6394 2.50037 10.0402 2.5 9.38882V6.04605C2.50002 5.39437 2.85752 4.79465 3.43066 4.48453L5.78125 3.21402Z"),
            )
        }.build()
        return _ic_address_polygon_20!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAddressPolygon20Preview() {
    Icon(
        imageVector = Icons.ic_address_polygon_20,
        contentDescription = null,
    )
}