@file:Suppress("all")

package com.tangem.core.ui.res.generated

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Auto-generated from design tokens. Do not edit manually.
 */
@Stable
class TangemTypography3 internal constructor(fontFamily: FontFamily) {
    val display: Display = Display(fontFamily)
    val heading: Heading = Heading(fontFamily)
    val body: Body = Body(fontFamily)
    val subheading: Subheading = Subheading(fontFamily)
    val caption: Caption = Caption(fontFamily)

    @Stable
    class Display internal constructor(fontFamily: FontFamily) {
        val medium: TextStyle = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 44.sp,
            lineHeight = 52.sp,
            letterSpacing = (-0.92).sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
            lineBreak = LineBreak.Heading,
        )
    }

    @Stable
    class Heading internal constructor(fontFamily: FontFamily) {
        val medium: TextStyle = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            lineHeight = 33.sp,
            letterSpacing = (-0.37).sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
            lineBreak = LineBreak.Heading,
        )
        val small: TextStyle = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 24.sp,
            letterSpacing = (-0.12).sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
            lineBreak = LineBreak.Heading,
        )
    }

    @Stable
    class Body internal constructor(fontFamily: FontFamily) {
        val medium: TextStyle = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.02.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        )
    }

    @Stable
    class Subheading internal constructor(fontFamily: FontFamily) {
        val medium: TextStyle = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.07.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        )
    }

    @Stable
    class Caption internal constructor(fontFamily: FontFamily) {
        val medium: TextStyle = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.18.sp,
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.None,
            ),
        )
    }
}