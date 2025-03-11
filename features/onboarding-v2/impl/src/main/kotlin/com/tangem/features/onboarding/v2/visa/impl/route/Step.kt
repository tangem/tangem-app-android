package com.tangem.features.onboarding.v2.visa.impl.route

@Suppress("MagicNumber")
internal fun OnboardingVisaRoute.stepNum() = when (this) {
    is OnboardingVisaRoute.Welcome,
    is OnboardingVisaRoute.WelcomeBack,
    -> 1
    OnboardingVisaRoute.AccessCode -> 2
    is OnboardingVisaRoute.ChooseWallet -> 3
    is OnboardingVisaRoute.OtherWalletApproveOption -> 4
    is OnboardingVisaRoute.TangemWalletApproveOption -> 4
    is OnboardingVisaRoute.PinCode -> 6
    is OnboardingVisaRoute.InProgress -> {
        when (this.from) {
            OnboardingVisaRoute.InProgress.From.Approve -> 5
            OnboardingVisaRoute.InProgress.From.PinCode -> 7
        }
    }
}

internal const val ONBOARDING_VISA_STEPS_COUNT = 8