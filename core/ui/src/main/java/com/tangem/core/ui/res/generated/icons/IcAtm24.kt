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

private var _ic_atm_24: ImageVector? = null

val Icons.ic_atm_24: ImageVector
    get() {
        if (_ic_atm_24 != null) return _ic_atm_24!!
        _ic_atm_24 = ImageVector.Builder(
            name = "ic_atm_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M13.4573 15.1082C14.0095 15.1083 14.4573 15.5559 14.4573 16.1082V17.8972C14.457 18.4492 14.0093 18.8971 13.4573 18.8972C12.9052 18.8971 12.4575 18.4492 12.4573 17.8972V16.1082C12.4573 15.556 12.9051 15.1083 13.4573 15.1082Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M18.8059 3.28589C20.4324 3.28595 21.7502 4.60372 21.7502 6.23022V10.1189C21.7502 11.7455 20.4324 13.0632 18.8059 13.0632H17.8616V18.8689C17.8616 20.4953 16.5426 21.813 14.9163 21.8132H9.08325C7.45671 21.8132 6.13892 20.4955 6.13892 18.8689V13.0632H5.19458C3.56804 13.0632 2.25024 11.7455 2.25024 10.1189V6.23022C2.2503 4.60372 3.56808 3.28595 5.19458 3.28589H18.8059ZM11.0559 19.8132H14.9163C15.4381 19.813 15.8616 19.3908 15.8616 18.8689V9.17456H11.0559V19.8132ZM8.13892 18.8689C8.13892 19.3819 8.54641 19.797 9.05591 19.8113V9.17456H8.13892V18.8689ZM5.19458 5.28589C4.67265 5.28595 4.2503 5.70829 4.25024 6.23022V10.1189C4.25024 10.6409 4.67261 11.0632 5.19458 11.0632H6.13892V8.17456C6.13905 7.62239 6.58671 7.17456 7.13892 7.17456H16.8616C17.4134 7.17506 17.8614 7.6227 17.8616 8.17456V11.0632H18.8059C19.3279 11.0632 19.7502 10.6409 19.7502 10.1189V6.23022C19.7502 5.70829 19.3278 5.28595 18.8059 5.28589H5.19458Z"),
            )
        }.build()
        return _ic_atm_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcAtm24Preview() {
    Icon(
        imageVector = Icons.ic_atm_24,
        contentDescription = null,
    )
}