package com.tangem.features.onboarding.v2.visa.impl.route

@Suppress("MagicNumber")
internal fun OnboardingVisaRoute.stepNum() = when (this) {
    is OnboardingVisaRoute.Welcome -> 1
    OnboardingVisaRoute.AccessCode -> 2
    OnboardingVisaRoute.ChooseWallet -> 3
    OnboardingVisaRoute.OtherWalletApproveOption -> 4
    OnboardingVisaRoute.TangemWalletApproveOption -> 4
    OnboardingVisaRoute.InProgress -> 5
    OnboardingVisaRoute.PinCode -> 6
}

internal const val ONBOARDING_VISA_STEPS_COUNT = 7
