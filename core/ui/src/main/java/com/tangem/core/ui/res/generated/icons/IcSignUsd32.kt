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

private var _ic_sign_usd_32: ImageVector? = null

val Icons.ic_sign_usd_32: ImageVector
    get() {
        if (_ic_sign_usd_32 != null) return _ic_sign_usd_32!!
        _ic_sign_usd_32 = ImageVector.Builder(
            name = "ic_sign_usd_32",
            defaultWidth = 32.dp,
            defaultHeight = 32.dp,
            viewportWidth = 32f,
            viewportHeight = 32f,
        ).apply {
            addPath(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero,
                pathData = addPathNodes("M16.6753 0.997559C17.5017 0.997566 18.1721 1.66822 18.1723 2.49463V3.85986C19.9708 4.12779 21.6136 4.72287 22.9389 5.57959C24.7677 6.76187 26.1604 8.57284 26.1606 10.7974C26.1606 11.6239 25.491 12.2943 24.6645 12.2944C23.8379 12.2944 23.1674 11.6239 23.1674 10.7974C23.1672 9.92853 22.6208 8.93817 21.3139 8.09326C20.0217 7.25795 18.1461 6.69287 15.9985 6.69287C13.8511 6.69292 11.9762 7.25804 10.6841 8.09326C9.37706 8.93819 8.82981 9.92849 8.82956 10.7974C8.82956 11.3793 8.94077 11.8113 9.1196 12.144C9.29478 12.4698 9.57954 12.7844 10.063 13.0698C11.0888 13.6753 12.924 14.103 15.9985 14.103C19.2159 14.103 21.9062 14.5 23.8315 15.6001C24.8243 16.1674 25.6274 16.9295 26.1714 17.9136C26.712 18.8918 26.9487 20.0036 26.9487 21.2017C26.9486 23.8652 25.48 25.7226 23.3803 26.8276C21.9073 27.6029 20.1008 28.029 18.1723 28.2036V29.5044C18.1721 30.3308 17.5018 31.0005 16.6753 31.0005C15.8488 31.0005 15.1784 30.3308 15.1782 29.5044V28.2788C12.6602 28.1613 10.3378 27.5084 8.55124 26.4497C6.60512 25.2964 5.04844 23.4899 5.04831 21.2017C5.04842 20.3752 5.71888 19.7046 6.54538 19.7046C7.37193 19.7046 8.04233 20.3751 8.04245 21.2017C8.04258 22.007 8.60283 23.0005 10.0776 23.8745C11.5199 24.7292 13.6096 25.3061 15.9985 25.3062C18.4952 25.3062 20.5809 24.9175 21.9858 24.1782C23.3074 23.4827 23.9554 22.5392 23.9555 21.2017C23.9555 20.3992 23.7983 19.809 23.5512 19.3618C23.3075 18.9208 22.9285 18.5325 22.3462 18.1997C21.1204 17.4993 19.0831 17.0962 15.9985 17.0962C12.7711 17.0962 10.2728 16.6698 8.54147 15.6479C7.64675 15.1198 6.94765 14.4263 6.48288 13.562C6.02181 12.7045 5.8364 11.7621 5.8364 10.7974C5.83665 8.57278 7.23021 6.76187 9.05905 5.57959C10.7104 4.51215 12.8539 3.84928 15.1782 3.72217V2.49463C15.1784 1.66821 15.8488 0.997559 16.6753 0.997559Z"),
            )
        }.build()
        return _ic_sign_usd_32!!
    }

@Composable
@Preview(showBackground = true)
private fun IcSignUsd32Preview() {
    Icon(
        imageVector = Icons.ic_sign_usd_32,
        contentDescription = null,
    )
}