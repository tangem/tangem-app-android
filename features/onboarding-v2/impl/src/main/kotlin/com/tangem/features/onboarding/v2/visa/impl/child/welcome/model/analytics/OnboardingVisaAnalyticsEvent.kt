package com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class OnboardingVisaAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Onboarding / Visa", event, params) {

    class ActivationScreenOpened : OnboardingVisaAnalyticsEvent(
        event = "Activation Screen Opened",
    )

    class ButtonActivate : OnboardingVisaAnalyticsEvent(
        event = "Button - Activate",
    )

    class SettingAccessCodeStarted : OnboardingVisaAnalyticsEvent(
        event = "Setting Access Code Started",
    )

    class AccessCodeEntered : OnboardingVisaAnalyticsEvent(
        event = "Access Code Entered",
    )

    class AccessCodeReenterScreen : OnboardingVisaAnalyticsEvent(
        event = "Access Code Re-enter Screen",
    )

    class OnboardingVisa : OnboardingVisaAnalyticsEvent(
        event = "Onboarding / Visa",
    )

    data class ChooseWalletScreen(
        val type: String,
    ) : OnboardingVisaAnalyticsEvent(
        event = "Choose Wallet Screen",
        params = mapOf("Type" to type),
    )

    class WalletPrepare : OnboardingVisaAnalyticsEvent(
        event = "Wallet Prepare",
    )

    class ButtonApprove : OnboardingVisaAnalyticsEvent(
        event = "Button - Approve",
    )

    class GoToWebsiteOpened : OnboardingVisaAnalyticsEvent(
        event = "Go To Website Opened",
    )

    class ButtonBrowser : OnboardingVisaAnalyticsEvent(
        event = "Button - Browser",
    )

    class ButtonShareLink : OnboardingVisaAnalyticsEvent(
        event = "Button - Share Link",
    )

    class ActivationInProgressScreen : OnboardingVisaAnalyticsEvent(
        event = "Activation In Progress Screen",
    )

    class PinCodeScreenOpened : OnboardingVisaAnalyticsEvent(
        event = "PIN Code Screen Opened",
    )

    class PinEntered : OnboardingVisaAnalyticsEvent(
        event = "PIN Entered",
        params = mapOf("Type" to "Visa"),
    )

    class BiometricScreenOpened : OnboardingVisaAnalyticsEvent(
        event = "Biometric Screen Opened",
    )

    class SuccessScreenOpened : OnboardingVisaAnalyticsEvent(
        event = "Success Screen Opened",
    )

    class ErrorPinValidation : OnboardingVisaAnalyticsEvent(
        event = "Error - Pin Validation",
    )
}