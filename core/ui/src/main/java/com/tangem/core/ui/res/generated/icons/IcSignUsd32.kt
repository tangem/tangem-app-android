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
                pathData = addPathNodes("M16.6753 0.997803C17.5017 0.99781 18.1721 1.66846 18.1723 2.49487V3.86011C19.9708 4.12804 21.6136 4.72311 22.9389 5.57983C24.7677 6.76212 26.1604 8.57309 26.1606 10.7976C26.1606 11.6241 25.491 12.2945 24.6645 12.2947C23.8379 12.2946 23.1674 11.6242 23.1674 10.7976C23.1672 9.92877 22.6208 8.93841 21.3139 8.09351C20.0217 7.25819 18.1461 6.69312 15.9985 6.69312C13.8511 6.69316 11.9762 7.25828 10.6841 8.09351C9.37706 8.93843 8.82981 9.92874 8.82956 10.7976C8.82956 11.3796 8.94077 11.8116 9.1196 12.1443C9.29478 12.4701 9.57954 12.7847 10.063 13.0701C11.0888 13.6755 12.924 14.1032 15.9985 14.1033C19.2159 14.1033 21.9062 14.5002 23.8315 15.6003C24.8243 16.1677 25.6274 16.9297 26.1714 17.9138C26.712 18.8921 26.9487 20.0039 26.9487 21.2019C26.9486 23.8654 25.48 25.7229 23.3803 26.8279C21.9073 27.6031 20.1008 28.0292 18.1723 28.2039V29.5046C18.1721 30.3311 17.5018 31.0007 16.6753 31.0007C15.8488 31.0007 15.1784 30.3311 15.1782 29.5046V28.2791C12.6602 28.1615 10.3378 27.5086 8.55124 26.45C6.60512 25.2966 5.04844 23.4901 5.04831 21.2019C5.04842 20.3754 5.71888 19.7049 6.54538 19.7048C7.37193 19.7048 8.04234 20.3754 8.04245 21.2019C8.04258 22.0072 8.60283 23.0008 10.0776 23.8748C11.5199 24.7294 13.6096 25.3064 15.9985 25.3064C18.4952 25.3064 20.5809 24.9178 21.9858 24.1785C23.3074 23.4829 23.9554 22.5395 23.9555 21.2019C23.9555 20.3994 23.7983 19.8092 23.5512 19.3621C23.3075 18.9211 22.9285 18.5327 22.3462 18.2C21.1204 17.4996 19.0831 17.0964 15.9985 17.0964C12.7711 17.0964 10.2728 16.67 8.54147 15.6482C7.64675 15.12 6.94765 14.4266 6.48288 13.5623C6.02181 12.7047 5.8364 11.7623 5.8364 10.7976C5.83665 8.57302 7.23021 6.76211 9.05905 5.57983C10.7104 4.5124 12.8539 3.84952 15.1782 3.72241V2.49487C15.1784 1.66846 15.8488 0.997803 16.6753 0.997803Z"),
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