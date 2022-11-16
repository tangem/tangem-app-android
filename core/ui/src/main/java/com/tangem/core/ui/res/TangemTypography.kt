package com.tangem.core.ui.res

import androidx.compose.material.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.R

private val RobotoFamily = FontFamily(
    Font(R.font.roboto_regular, FontWeight.Normal),
    Font(R.font.roboto_medium, FontWeight.Medium),
)

@OptIn(ExperimentalUnitApi::class)
val TangemTypography = Typography(
    defaultFontFamily = RobotoFamily,
    h1 = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(0f, TextUnitType.Sp),
        lineHeight = TextUnit(44f, TextUnitType.Sp),
    ),
    h2 = TextStyle(
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(0.18f, TextUnitType.Sp),
        lineHeight = TextUnit(32f, TextUnitType.Sp),
    ),
    h3 = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(0.15f, TextUnitType.Sp),
        lineHeight = TextUnit(24f, TextUnitType.Sp),
    ),
    subtitle1 = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(0.15f, TextUnitType.Sp),
        lineHeight = TextUnit(24f, TextUnitType.Sp),
    ),
    subtitle2 = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(0.5f, TextUnitType.Sp),
        lineHeight = TextUnit(24f, TextUnitType.Sp),
    ),
    body1 = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(0.5f, TextUnitType.Sp),
        lineHeight = TextUnit(24f, TextUnitType.Sp),
    ),
    body2 = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(0.25f, TextUnitType.Sp),
        lineHeight = TextUnit(20f, TextUnitType.Sp),
    ),
    button = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(0.1f, TextUnitType.Sp),
        lineHeight = TextUnit(16f, TextUnitType.Sp),
    ),
    caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(0.4f, TextUnitType.Sp),
        lineHeight = TextUnit(16f, TextUnitType.Sp),
    ),
    overline = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(1.5f, TextUnitType.Sp),
        lineHeight = TextUnit(16f, TextUnitType.Sp),
    ),
)
