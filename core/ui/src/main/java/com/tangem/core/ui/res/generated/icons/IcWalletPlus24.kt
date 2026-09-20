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

private var _ic_wallet_plus_24: ImageVector? = null

val Icons.ic_wallet_plus_24: ImageVector
    get() {
        if (_ic_wallet_plus_24 != null) return _ic_wallet_plus_24!!
        _ic_wallet_plus_24 = ImageVector.Builder(
            name = "ic_wallet_plus_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M9.8291 9.25C10.3814 9.25 10.8291 9.69772 10.8291 10.25V11H11.5791C12.1314 11 12.5791 11.4477 12.5791 12C12.5791 12.5523 12.1314 13 11.5791 13H10.8291V13.75C10.8291 14.3023 10.3814 14.75 9.8291 14.75C9.27682 14.75 8.8291 14.3023 8.8291 13.75V13H8.0791C7.52682 13 7.0791 12.5523 7.0791 12C7.0791 11.4477 7.52682 11 8.0791 11H8.8291V10.25C8.8291 9.69772 9.27682 9.25 9.8291 9.25Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M17.0518 5.10645C18.766 5.10682 20.1562 6.49757 20.1562 8.21191V9.19141C20.6622 9.51969 20.9971 10.0893 20.9971 10.7373V13.2627C20.9969 13.9103 20.6618 14.4784 20.1562 14.8066V15.7891C20.1558 17.5031 18.7658 18.8932 17.0518 18.8936H6.10742C4.39317 18.8935 3.00244 17.5032 3.00195 15.7891V8.21191C3.00202 6.49738 4.39289 5.10651 6.10742 5.10645H17.0518ZM6.10742 7.10742C5.49746 7.10749 5.003 7.60195 5.00293 8.21191V15.7891C5.00341 16.3987 5.49774 16.8935 6.10742 16.8936H17.0518C17.6612 16.8932 18.1558 16.3985 18.1562 15.7891V15.1045H17.0508C15.3365 15.1043 13.9466 13.7143 13.9463 12C13.9464 10.2855 15.3364 8.89571 17.0508 8.89551H18.1562V8.21191C18.1562 7.60214 17.6615 7.1078 17.0518 7.10742H6.10742ZM17.0508 10.8955C16.4409 10.8957 15.9464 11.3901 15.9463 12C15.9466 12.6097 16.4411 13.1043 17.0508 13.1045H18.9971V10.8955H17.0508Z"),
            )
        }.build()
        return _ic_wallet_plus_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletPlus24Preview() {
    Icon(
        imageVector = Icons.ic_wallet_plus_24,
        contentDescription = null,
    )
}