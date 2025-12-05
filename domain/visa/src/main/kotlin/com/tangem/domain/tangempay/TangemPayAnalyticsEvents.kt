package com.tangem.domain.tangempay

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class TangemPayAnalyticsEvents(
    categoryName: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = categoryName, event = event, params = params) {

    data object ActivationScreenOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Activation Screen Opened",
    )

    data object ViewTermsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Button - Visa View Terms",
    )

    data object GetCardClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Button - Visa Get Card",
    )

    data object KycFlowOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa KYC Flow Opened",
    )

    data object IssuingBannerDisplayed : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa Issuing Banner Displayed",
    )

    data object MainScreenOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Visa Main Screen Opened",
    )

    data object ReceiveFundsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Receive",
    )

    data object SwapClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Swap",
    )
}