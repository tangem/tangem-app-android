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

private var _ic_wallet_checkmark_24: ImageVector? = null

val Icons.ic_wallet_checkmark_24: ImageVector
    get() {
        if (_ic_wallet_checkmark_24 != null) return _ic_wallet_checkmark_24!!
        _ic_wallet_checkmark_24 = ImageVector.Builder(
            name = "ic_wallet_checkmark_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M10.8721 10.1826C11.2626 9.79211 11.8956 9.79214 12.2861 10.1826C12.6763 10.5732 12.6765 11.2063 12.2861 11.5967L10.0645 13.8184C9.67404 14.2086 9.0409 14.2085 8.65039 13.8184L7.31738 12.4854C6.92691 12.0949 6.92698 11.4618 7.31738 11.0713C7.7079 10.6808 8.34092 10.6808 8.73145 11.0713L9.35742 11.6973L10.8721 10.1826Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.0518 5.10742C18.766 5.1078 20.1562 6.49757 20.1562 8.21191V9.19141C20.6622 9.51969 20.9971 10.0893 20.9971 10.7373V13.2627C20.9969 13.9103 20.6618 14.4784 20.1562 14.8066V15.7891C20.1559 17.5032 18.7659 18.8932 17.0518 18.8936H6.10742C4.39309 18.8935 3.00231 17.5033 3.00195 15.7891V8.21191C3.00202 6.49738 4.39289 5.10749 6.10742 5.10742H17.0518ZM6.10742 7.10742C5.49746 7.10749 5.003 7.60195 5.00293 8.21191V15.7891C5.00328 16.3988 5.49766 16.8935 6.10742 16.8936H17.0518C17.6613 16.8932 18.1559 16.3986 18.1562 15.7891V15.1045H17.0508C15.3365 15.1043 13.9466 13.7143 13.9463 12C13.9464 10.2855 15.3364 8.89571 17.0508 8.89551H18.1562V8.21191C18.1562 7.60214 17.6615 7.1078 17.0518 7.10742H6.10742ZM17.0508 10.8955C16.4409 10.8957 15.9464 11.3901 15.9463 12C15.9466 12.6097 16.4411 13.1043 17.0508 13.1045H18.9971V10.8955H17.0508Z"),
            )
        }.build()
        return _ic_wallet_checkmark_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletCheckmark24Preview() {
    Icon(
        imageVector = Icons.ic_wallet_checkmark_24,
        contentDescription = null,
    )
}