package com.tangem.features.onboarding.v2.visa.impl.route

import kotlinx.serialization.Serializable

@Serializable
sealed class OnboardingVisaRoute {

    @Serializable
    data class Welcome(val isWelcomeBack: Boolean) : OnboardingVisaRoute()

    @Serializable
    data object AccessCode : OnboardingVisaRoute()

    @Serializable
    data object ChooseWallet : OnboardingVisaRoute()

    @Serializable
    data object TangemWalletApproveOption : OnboardingVisaRoute()

    @Serializable
    data object OtherWalletApproveOption : OnboardingVisaRoute()

    @Serializable
    data object InProgress : OnboardingVisaRoute()

    @Serializable
    data object PinCode : OnboardingVisaRoute()
}