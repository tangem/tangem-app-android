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

private var _ic_sign_usd_16: ImageVector? = null

val Icons.ic_sign_usd_16: ImageVector
    get() {
        if (_ic_sign_usd_16 != null) return _ic_sign_usd_16!!
        _ic_sign_usd_16 = ImageVector.Builder(
            name = "ic_sign_usd_16",
            defaultWidth = 16.dp,
            defaultHeight = 16.dp,
            viewportWidth = 16f,
            viewportHeight = 16f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M8.34985 0.500183C8.626 0.500183 8.84985 0.724041 8.84985 1.00018V1.9494C9.83996 2.06353 10.7395 2.37327 11.4465 2.83026C12.3465 3.41205 12.9904 4.27563 12.9905 5.30389C12.9902 5.57981 12.7665 5.80389 12.4905 5.80389C12.2148 5.8035 11.9907 5.57957 11.9905 5.30389C11.9904 4.72855 11.6292 4.13921 10.9036 3.6701C10.1829 3.20439 9.15743 2.89963 7.99927 2.8996C6.84092 2.89965 5.81462 3.20425 5.09399 3.6701C4.36861 4.13917 4.00715 4.72866 4.00708 5.30389C4.00714 5.64045 4.07116 5.91136 4.1897 6.13202C4.30718 6.3505 4.49402 6.54862 4.78247 6.71893C5.37924 7.07105 6.39182 7.29215 7.99927 7.29218C9.6545 7.29219 10.9886 7.49914 11.9221 8.03241C12.3988 8.30481 12.7762 8.66529 13.0305 9.12518C13.2837 9.58331 13.3996 10.1119 13.3997 10.6965C13.3996 11.9549 12.7132 12.8333 11.6965 13.3683C10.912 13.7811 9.92062 13.997 8.84985 14.0695V15.0012C8.84935 15.2769 8.62569 15.5012 8.34985 15.5012C8.07411 15.5011 7.85035 15.2768 7.84985 15.0012V14.0969C6.48072 14.0739 5.22168 13.7381 4.27954 13.1799C3.3143 12.6078 2.599 11.7458 2.59888 10.6965C2.59897 10.4205 2.82293 10.1966 3.09888 10.1965C3.37496 10.1965 3.59879 10.4204 3.59888 10.6965C3.599 11.2505 3.9816 11.8408 4.78931 12.3195C5.58633 12.7917 6.72036 13.0997 7.99927 13.0998C9.31403 13.0998 10.4462 12.8963 11.2307 12.4836C11.9874 12.0853 12.3996 11.5119 12.3997 10.6965C12.3996 10.2442 12.3106 9.89022 12.1555 9.60956C12.0015 9.33094 11.7657 9.09468 11.426 8.90057C10.7262 8.50083 9.61008 8.29219 7.99927 8.29218C6.3405 8.29215 5.10608 8.07156 4.27368 7.58026C3.84786 7.32886 3.52304 7.00402 3.30884 6.60565C3.09606 6.20972 3.00714 5.76838 3.00708 5.30389C3.00715 4.2758 3.6513 3.41205 4.55103 2.83026C5.4218 2.26733 6.5853 1.92658 7.84985 1.90155V1.00018C7.84985 0.724107 8.0738 0.500291 8.34985 0.500183Z"),
            )
        }.build()
        return _ic_sign_usd_16!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignUsd16Preview() {
    Icon(
        imageVector = Icons.ic_sign_usd_16,
        contentDescription = null,
    )
}