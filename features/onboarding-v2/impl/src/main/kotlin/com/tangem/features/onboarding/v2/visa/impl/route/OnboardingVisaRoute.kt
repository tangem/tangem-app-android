package com.tangem.features.onboarding.v2.visa.impl.route

import com.tangem.domain.visa.model.VisaDataForApprove
import kotlinx.serialization.Serializable

@Serializable
sealed class OnboardingVisaRoute {

    @Serializable
    data class Welcome(val isWelcomeBack: Boolean) : OnboardingVisaRoute()

    @Serializable
    data object AccessCode : OnboardingVisaRoute()

    @Serializable
    data class ChooseWallet(val visaDataForApprove: VisaDataForApprove) : OnboardingVisaRoute()

    @Serializable
    data class TangemWalletApproveOption(
        val visaDataForApprove: VisaDataForApprove,
        val allowNavigateBack: Boolean,
    ) : OnboardingVisaRoute()

    @Serializable
    data class OtherWalletApproveOption(val visaDataForApprove: VisaDataForApprove) : OnboardingVisaRoute()

    @Serializable
    data object InProgress : OnboardingVisaRoute()

    @Serializable
    data class PinCode(val activationOrderId: String) : OnboardingVisaRoute()
}