package com.tangem.tap.common.compose

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.tangem.wallet.R

object TangemTypography {

    val body1 = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontSize = 16.0.sp,
        letterSpacing = 0.5.sp,
        lineHeight = 24.0.sp,
    )
    val body2 = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontSize = 14.0.sp,
        letterSpacing = 0.25.sp,
        lineHeight = 20.0.sp,
    )
    val button = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontSize = 14.0.sp,
        letterSpacing = 0.10000000149011612.sp,
        lineHeight = 20.0.sp,
    )
    val caption = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontSize = 12.0.sp,
        letterSpacing = 0.4000000059604645.sp,
        lineHeight = 16.0.sp,
    )
    val headline1 = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_regular)),
        fontSize = 34.0.sp,
        letterSpacing = 0.0.sp,
        lineHeight = 44.0.sp,
    )
    val headline2 = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontSize = 24.0.sp,
        letterSpacing = 0.18000000715255737.sp,
        lineHeight = 32.0.sp,
    )
    val headline3 = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontSize = 20.0.sp,
        letterSpacing = 0.15000000596046448.sp,
        lineHeight = 24.0.sp,
    )
    val overline = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontSize = 10.0.sp,
        letterSpacing = 1.5.sp,
        lineHeight = 16.0.sp,
    )
    val subtitle1 = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontSize = 16.0.sp,
        letterSpacing = 0.15000000596046448.sp,
        lineHeight = 24.0.sp,
    )
    val subtitle2 = TextStyle(
        fontFamily = FontFamily(Font(R.font.roboto_medium)),
        fontSize = 14.0.sp,
        letterSpacing = 0.10000000149011612.sp,
        lineHeight = 24.0.sp,
    )
}
