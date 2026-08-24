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

private var _ic_wallet_checkmark_28: ImageVector? = null

val Icons.ic_wallet_checkmark_28: ImageVector
    get() {
        if (_ic_wallet_checkmark_28 != null) return _ic_wallet_checkmark_28!!
        _ic_wallet_checkmark_28 = ImageVector.Builder(
            name = "ic_wallet_checkmark_28",
            defaultWidth = 28.dp,
            defaultHeight = 28.dp,
            viewportWidth = 28f,
            viewportHeight = 28f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.6094 11.9922C13.0975 11.5042 13.8889 11.5041 14.377 11.9922C14.8644 12.4803 14.8647 13.2718 14.377 13.7598L12.1279 16.0088C11.8937 16.243 11.5754 16.3748 11.2441 16.375C10.9129 16.375 10.5947 16.2428 10.3604 16.0088L9.01074 14.6592C8.52267 14.171 8.52261 13.3797 9.01074 12.8916C9.49888 12.4036 10.2902 12.4036 10.7783 12.8916L11.2441 13.3574L12.6094 11.9922Z"),
            )
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M20.1689 5.55957C22.2772 5.55975 23.9861 7.26971 23.9863 9.37793V10.5566C24.605 10.9638 25.0137 11.6641 25.0137 12.46V15.541C25.0135 16.3365 24.6046 17.0352 23.9863 17.4424V18.623C23.9858 20.731 22.277 22.4402 20.1689 22.4404H6.81641C4.70819 22.4404 2.99856 20.7311 2.99805 18.623V9.37793C2.99829 7.26976 4.70726 5.55981 6.81543 5.55957H20.1689ZM6.81543 8.06055C6.08797 8.06079 5.49829 8.65047 5.49805 9.37793V18.623C5.49856 19.3504 6.08891 19.9404 6.81641 19.9404H20.1689C20.8963 19.9402 21.4858 19.3503 21.4863 18.623V17.8184H20.168C18.0599 17.818 16.3508 16.1081 16.3506 14C16.3508 11.8919 18.0599 10.183 20.168 10.1826H21.4863V9.37793C21.4861 8.65043 20.8965 8.06073 20.1689 8.06055H6.81543ZM20.168 12.6826C19.4406 12.683 18.8508 13.2726 18.8506 14C18.8508 14.7274 19.4406 15.318 20.168 15.3184H22.5137V12.6826H20.168Z"),
            )
        }.build()
        return _ic_wallet_checkmark_28!!
    }

@Composable
@Preview(showBackground = true)
private fun IcWalletCheckmark28Preview() {
    Icon(
        imageVector = Icons.ic_wallet_checkmark_28,
        contentDescription = null,
    )
}