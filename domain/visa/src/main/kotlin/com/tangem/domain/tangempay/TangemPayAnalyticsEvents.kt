package com.tangem.domain.tangempay

import com.tangem.core.analytics.models.AnalyticsEvent

sealed class TangemPayAnalyticsEvents(
    categoryName: String,
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = categoryName, event = event, params = params) {

    class ActivationScreenOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Activation Screen Opened",
    )

    class ViewTermsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Button - Visa View Terms",
    )

    class GetCardClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Button - Visa Get Card",
    )

    class KycFlowOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa KYC Flow Opened",
    )

    class IssuingBannerDisplayed : TangemPayAnalyticsEvents(
        categoryName = "Visa Onboarding",
        event = "Visa Issuing Banner Displayed",
    )

    class MainScreenOpened : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Visa Main Screen Opened",
    )

    class ReceiveFundsClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Receive",
    )

    class SwapClicked : TangemPayAnalyticsEvents(
        categoryName = "Visa Screen",
        event = "Button - Visa Swap",
    )
}