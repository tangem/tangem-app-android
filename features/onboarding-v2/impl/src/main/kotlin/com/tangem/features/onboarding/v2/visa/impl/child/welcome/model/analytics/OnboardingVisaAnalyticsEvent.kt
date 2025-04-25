package com.tangem.features.onboarding.v2.visa.impl.child.welcome.model.analytics

import com.tangem.core.analytics.models.AnalyticsEvent

internal sealed class OnboardingVisaAnalyticsEvent(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Onboarding / Visa", event, params) {

    data object ActivationScreenOpened : OnboardingVisaAnalyticsEvent(
        event = "Activation Screen Opened",
    )

    data object ButtonActivate : OnboardingVisaAnalyticsEvent(
        event = "Button - Activate",
    )

    data object SettingAccessCodeStarted : OnboardingVisaAnalyticsEvent(
        event = "Setting Access Code Started",
    )

    data object AccessCodeEntered : OnboardingVisaAnalyticsEvent(
        event = "Access Code Entered",
    )

    data object AccessCodeReenterScreen : OnboardingVisaAnalyticsEvent(
        event = "Access Code Re-enter Screen",
    )

    data object OnboardingVisa : OnboardingVisaAnalyticsEvent(
        event = "Onboarding / Visa",
    )

    data class ChooseWalletScreen(
        val type: String,
    ) : OnboardingVisaAnalyticsEvent(
        event = "Choose Wallet Screen",
        params = mapOf("Type" to type),
    )

    data object WalletPrepare : OnboardingVisaAnalyticsEvent(
        event = "Wallet Prepare",
    )

    data object ButtonApprove : OnboardingVisaAnalyticsEvent(
        event = "Button - Approve",
    )

    data object GoToWebsiteOpened : OnboardingVisaAnalyticsEvent(
        event = "Go To Website Opened",
    )

    data object ButtonBrowser : OnboardingVisaAnalyticsEvent(
        event = "Button - Browser",
    )

    data object ButtonShareLink : OnboardingVisaAnalyticsEvent(
        event = "Button - Share Link",
    )

    data object ActivationInProgressScreen : OnboardingVisaAnalyticsEvent(
        event = "Activation In Progress Screen",
    )

    data object PinCodeScreenOpened : OnboardingVisaAnalyticsEvent(
        event = "PIN Code Screen Opened",
    )

    data object PinEntered : OnboardingVisaAnalyticsEvent(
        event = "PIN Entered",
        params = mapOf("Type" to "Visa"),
    )

    data object BiometricScreenOpened : OnboardingVisaAnalyticsEvent(
        event = "Biometric Screen Opened",
    )

    data object SuccessScreenOpened : OnboardingVisaAnalyticsEvent(
        event = "Success Screen Opened",
    )

    data object ErrorPinValidation : OnboardingVisaAnalyticsEvent(
        event = "Error - Pin Validation",
    )
}