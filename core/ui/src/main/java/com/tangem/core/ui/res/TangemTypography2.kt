package com.tangem.core.ui.res

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.R

internal val InterFamily = FontFamily(
    Font(R.font.inter_regular),
    Font(R.font.inter_italic, style = FontStyle.Italic),
)

@Stable
class TangemTypography2 internal constructor(
    fontFamily: FontFamily,
) {
    val titleRegular44: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 44.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit(value = 0.37f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 48f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingRegular34: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = 0.37f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 40f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingBold34: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = TextUnit(value = 0.37f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 40f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingRegular28: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit(value = 0.36f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 36f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingBold28: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = TextUnit(value = 0.36f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 36f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingRegular22: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = 0.35f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 28f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingBold22: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = TextUnit(value = 0.35f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 28f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingRegular20: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = 0.38f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 24f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingSemibold20: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit(value = 0.38f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 24f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingRegular17: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = -0.41f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val headingSemibold17: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit(value = -0.2f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val bodyRegular16: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = -0.32f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val bodySemibold16: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit(value = -0.32f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val bodyRegular15: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = -0.24f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val bodySemibold15: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = -0.1f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 20f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val bodyRegular14: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = -0.1f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 16f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val captionRegular13: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = -0.08f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 16f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val captionSemibold13: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit(value = 0.1f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 16f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val captionRegular12: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = 0f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 16f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val captionSemibold12: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = TextUnit(value = 0.1f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 16f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val captionRegular11: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = TextUnit(value = 0.07f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 12f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )

    val captionSemibold11: TextStyle = TextStyle(
        fontFamily = fontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = TextUnit(value = 0.15f, type = TextUnitType.Sp),
        lineHeight = TextUnit(value = 12f, type = TextUnitType.Sp),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None,
        ),
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 1500)
@Preview(showBackground = true, widthDp = 360, heightDp = 1500, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun TangemTypography2_Preview() {
    TangemThemePreviewRedesign {
        val typographyList = sequenceOf(
            TangemTheme.typography2.titleRegular44,
            TangemTheme.typography2.headingRegular34,
            TangemTheme.typography2.headingBold34,
            TangemTheme.typography2.headingRegular28,
            TangemTheme.typography2.headingBold28,
            TangemTheme.typography2.headingRegular22,
            TangemTheme.typography2.headingBold22,
            TangemTheme.typography2.headingRegular20,
            TangemTheme.typography2.headingSemibold20,
            TangemTheme.typography2.headingRegular17,
            TangemTheme.typography2.headingSemibold17,
            TangemTheme.typography2.bodyRegular16,
            TangemTheme.typography2.bodySemibold16,
            TangemTheme.typography2.bodyRegular15,
            TangemTheme.typography2.bodySemibold15,
            TangemTheme.typography2.bodyRegular14,
            TangemTheme.typography2.captionRegular13,
            TangemTheme.typography2.captionSemibold13,
            TangemTheme.typography2.captionRegular12,
            TangemTheme.typography2.captionSemibold12,
            TangemTheme.typography2.captionRegular11,
            TangemTheme.typography2.captionSemibold11,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .background(TangemTheme.colors2.surface.level1)
                .padding(4.dp),
        ) {
            typographyList.forEach { textStyle ->
                Box(modifier = Modifier.heightIn(min = 60.dp)) {
                    Text(
                        text = "Lorem ipsum",
                        style = textStyle,
                        color = TangemTheme.colors2.text.neutral.primary,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}
// endregion