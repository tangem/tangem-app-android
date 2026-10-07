package com.tangem.features.introduction.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.domain.sociallogin.models.SocialLoginProvider

internal sealed class SocialLoginAnalyticsEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(
    category = "Onboarding / Social login",
    event = event,
    params = params + (AnalyticsParam.PRODUCT_TYPE to AnalyticsParam.ProductType.MobileWallet.value),
) {

    class ButtonContinue(provider: SocialLoginProvider, source: AnalyticsParam.ScreensSources) :
        SocialLoginAnalyticsEvents(
            event = "Button - Continue with ${provider.analyticsName}",
            params = mapOf(AnalyticsParam.SOURCE to source.value),
        )

    class RiskConsentScreenOpened : SocialLoginAnalyticsEvents(event = "Risk Consent Screen Opened")

    class RiskConsentNotConfirmed : SocialLoginAnalyticsEvents(event = "Error - Risk Consent Not Confirmed")

    class MarketingEmailsConsent(isGiven: Boolean, consentText: String) : SocialLoginAnalyticsEvents(
        event = "Consent - Marketing Emails",
        params = mapOf(
            "Consent Type" to "Offers & Updates by email",
            "Consent Given" to if (isGiven) "Yes" else "No",
            "Consent Text" to consentText,
        ),
    )

    class MinOsNotSupported : SocialLoginAnalyticsEvents(event = "Notice - Min OS Not Supported")

    class ErrorShown(errorMessage: String) : SocialLoginAnalyticsEvents(
        event = "Error Shown",
        params = mapOf(AnalyticsParam.ERROR_MESSAGE to errorMessage),
    )
}

private val SocialLoginProvider.analyticsName: String
    get() = when (this) {
        SocialLoginProvider.Google -> "Google"
    }