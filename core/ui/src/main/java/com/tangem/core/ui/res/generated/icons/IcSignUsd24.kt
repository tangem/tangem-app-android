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

private var _ic_sign_usd_24: ImageVector? = null

val Icons.ic_sign_usd_24: ImageVector
    get() {
        if (_ic_sign_usd_24 != null) return _ic_sign_usd_24!!
        _ic_sign_usd_24 = ImageVector.Builder(
            name = "ic_sign_usd_24",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M12.5 0.999939C13.0523 0.999967 13.5 1.4477 13.5 1.99994V3.10443C14.8543 3.29374 16.0895 3.73518 17.0791 4.37494C18.4146 5.23826 19.416 6.54999 19.4161 8.14838C19.4159 8.70042 18.9681 9.1482 18.4161 9.14838C17.8639 9.14838 17.4162 8.70053 17.4161 8.14838C17.416 7.45585 16.9801 6.69261 15.9932 6.05463C15.0164 5.42322 13.607 4.99994 11.9991 4.99994C10.3911 4.99994 8.98175 5.42322 8.00493 6.05463C7.01805 6.69261 6.5821 7.45585 6.58208 8.14838C6.58211 8.59299 6.66703 8.93094 6.80962 9.19623C6.94984 9.457 7.17592 9.70361 7.54887 9.92377C8.33404 10.3872 9.7168 10.704 11.9991 10.704C14.3767 10.704 16.3448 10.9986 17.7452 11.7988C18.4654 12.2104 19.0447 12.7607 19.4366 13.4697C19.8262 14.1748 19.999 14.9788 19.9991 15.8515C19.9991 17.7759 18.9408 19.1184 17.4141 19.9218C16.3144 20.5005 14.9553 20.8157 13.5 20.9384V21.9999C13.5 22.5522 13.0523 22.9999 12.5 22.9999C11.9478 22.9999 11.5 22.5522 11.5 21.9999V20.9882C9.61042 20.9154 7.86886 20.4334 6.53911 19.6454C5.11504 18.8015 3.99907 17.4923 3.99907 15.8515C3.99926 15.2994 4.4469 14.8515 4.99907 14.8515C5.55124 14.8515 5.99888 15.2994 5.99907 15.8515C5.99907 16.5015 6.44958 17.2674 7.55864 17.9247C8.64607 18.5691 10.214 18.9999 11.9991 18.9999C13.8561 18.9999 15.421 18.7114 16.4834 18.1523C17.4902 17.6224 17.9991 16.89 17.9991 15.8515C17.999 15.2432 17.8801 14.7877 17.6866 14.4374C17.4952 14.0912 17.1991 13.79 16.753 13.5351C15.82 13.002 14.2881 12.704 11.9991 12.704C9.61468 12.704 7.78877 12.388 6.53227 11.6464C5.88451 11.2641 5.38161 10.7641 5.0479 10.1435C4.71679 9.52759 4.58211 8.84875 4.58208 8.14838C4.5821 6.54999 5.58354 5.23826 6.91899 4.37494C8.14813 3.5804 9.75561 3.09054 11.5 3.01166V1.99994C11.5001 1.44768 11.9478 0.999939 12.5 0.999939Z"),
            )
        }.build()
        return _ic_sign_usd_24!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignUsd24Preview() {
    Icon(
        imageVector = Icons.ic_sign_usd_24,
        contentDescription = null,
    )
}