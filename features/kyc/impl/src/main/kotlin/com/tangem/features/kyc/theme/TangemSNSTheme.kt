package com.tangem.features.kyc.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.sumsub.sns.core.theme.*
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.features.kyc.impl.R

/**
 * Custom theme settings for SumSub SDK
 */
internal object TangemSNSTheme {

    @Suppress("MagicNumber")
    fun theme(context: Context): SNSTheme {
        val robotoRegular = ResourcesCompat.getFont(context, R.font.roboto_regular) ?: Typeface.DEFAULT
        val robotoMedium = ResourcesCompat.getFont(context, R.font.roboto_medium) ?: Typeface.DEFAULT_BOLD
        return SNSTheme {
            colors {
                backgroundCommon = SNSThemeColor(TangemColorPalette.White.toArgb())
                contentStrong = SNSThemeColor(TangemColorPalette.Black.toArgb())
                contentNeutral = SNSThemeColor(TangemColorPalette.Dark2.toArgb())
                contentWeak = SNSThemeColor(TangemColorPalette.Light5.toArgb())
                primaryButtonBackground = SNSThemeColor(TangemColorPalette.Black.toArgb())
                primaryButtonContent = SNSThemeColor(TangemColorPalette.White.toArgb())
                primaryButtonBackgroundDisabled = SNSThemeColor(TangemColorPalette.Light3.toArgb())
                primaryButtonContentDisabled = SNSThemeColor(TangemColorPalette.Light5.toArgb())
                fieldBackground = SNSThemeColor(TangemColorPalette.Light1.toArgb())
                fieldBorder = SNSThemeColor(TangemColorPalette.Light3.toArgb())
                fieldBorderFocused = SNSThemeColor(TangemColorPalette.Azure.toArgb())
                fieldBorderDisabled = SNSThemeColor(TangemColorPalette.Light2.toArgb())
                fieldPlaceholder = SNSThemeColor(TangemColorPalette.Dark1.toArgb())
                fieldTint = SNSThemeColor(TangemColorPalette.Azure.toArgb())
                fieldContent = SNSThemeColor(TangemColorPalette.Black.toArgb())
                listSelectedItemBackground = SNSThemeColor(TangemColorPalette.Azure.copy(alpha = 0.1f).toArgb())
                cardPlainBackground = SNSThemeColor(TangemColorPalette.White.toArgb())
                contentLink = SNSThemeColor(TangemColorPalette.Azure.toArgb())
                statusBarColor = SNSThemeColor(TangemColorPalette.White.toArgb())
                contentWarning = SNSThemeColor(TangemColorPalette.Azure.toArgb())
                contentCritical = SNSThemeColor(TangemColorPalette.Amaranth.toArgb())
                contentSuccess = SNSThemeColor(TangemColorPalette.Azure.toArgb())
            }
            fonts {
                body = SNSThemeFont(robotoRegular, 14)
                caption = SNSThemeFont(robotoRegular, 12)
                headline1 = SNSThemeFont(robotoMedium, 34)
                headline2 = SNSThemeFont(robotoMedium, 24)
                subtitle1 = SNSThemeFont(robotoMedium, 16)
                subtitle2 = SNSThemeFont(robotoMedium, 14)
            }
            metrics {
                screenHorizontalMargin = 16.dpToPx(context)
                buttonHeight = 64.dpToPx(context)
                buttonCornerRadius = 14.dpToPx(context)
                buttonBorderWidth = 0f
                fieldHeight = 52.dpToPx(context)
                fieldCornerRadius = 8.dpToPx(context)
                fieldBorderWidth = 1.dpToPx(context)
                cardCornerRadius = 12.dpToPx(context)
                cardBorderWidth = 1.dpToPx(context)
                listSeparatorHeight = 1.dpToPx(context)
                listSeparatorMarginLeft = 16.dpToPx(context)
                listSeparatorMarginRight = 16.dpToPx(context)
                bottomSheetCornerRadius = 24.dpToPx(context)
                viewportBorderWidth = 2.dpToPx(context)
                documentFrameBorderWidth = 1.dpToPx(context)
                documentFrameCornerRadius = 12.dpToPx(context)
                documentFrameCornerSize = 8.dpToPx(context)
                segmentedControlCornerRadius = 8.dpToPx(context)

                screenHeaderAlignment = SNSThemeMetric.TextAlignment.CENTER
                sectionHeaderAlignment = SNSThemeMetric.TextAlignment.CENTER
                verificationStepCardStyle = SNSThemeMetric.CardStyle.PLAIN
                supportItemCardStyle = SNSThemeMetric.CardStyle.PLAIN
                documentTypeCardStyle = SNSThemeMetric.CardStyle.BORDERED
                selectedCountryCardStyle = SNSThemeMetric.CardStyle.BORDERED
                agreementCardStyle = SNSThemeMetric.CardStyle.PLAIN
                videoIdentLanguageCardStyle = SNSThemeMetric.CardStyle.PLAIN
                videoIdentStepCardStyle = SNSThemeMetric.CardStyle.PLAIN
                sumsubIdCardStyle = SNSThemeMetric.CardStyle.PLAIN
                activityIndicatorStyle = SNSThemeMetric.Size.MEDIUM
            }
        }
    }

    private fun Int.dpToPx(context: Context): Float {
        return this * context.resources.displayMetrics.density
    }
}