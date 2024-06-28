package com.tangem.core.ui.res

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.R

private val RobotoFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
)

@Immutable
data class TangemTypography internal constructor(
    val head: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit(value = 0f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 44f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val h1: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = 0f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 44f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val h2: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = 0.18f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 32f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val h3: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = 0.15f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 24f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val subtitle1: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = 0.15f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 24f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val subtitle2: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = 0.1f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val body1: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = 0.5f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 24f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val body2: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = 0.25f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val button: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = 0.1f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val caption1: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = 0.4f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 16f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val caption2: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = 0.4f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 16f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
    val overline: TextStyle = TextStyle(
        fontFamily = RobotoFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = 1.5f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 16f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    ),
)