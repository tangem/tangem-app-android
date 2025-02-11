package com.tangem.features.onboarding.v2.visa.impl.route

@Suppress("MagicNumber")
internal fun OnboardingVisaRoute.stepNum() = when (this) {
    is OnboardingVisaRoute.Welcome -> 1
    OnboardingVisaRoute.AccessCode -> 2
    is OnboardingVisaRoute.ChooseWallet -> 3
    is OnboardingVisaRoute.OtherWalletApproveOption -> 4
    is OnboardingVisaRoute.TangemWalletApproveOption -> 4
    OnboardingVisaRoute.InProgress -> 5
    is OnboardingVisaRoute.PinCode -> 6
}

internal const val ONBOARDING_VISA_STEPS_COUNT = 7