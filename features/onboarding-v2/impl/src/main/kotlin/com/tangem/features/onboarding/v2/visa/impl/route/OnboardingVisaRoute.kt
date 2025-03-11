package com.tangem.features.onboarding.v2.visa.impl.route

import com.tangem.domain.visa.model.VisaActivationInput
import com.tangem.domain.visa.model.VisaActivationOrderInfo
import com.tangem.domain.visa.model.VisaCardWalletDataToSignRequest
import com.tangem.features.onboarding.v2.visa.impl.common.PreparationDataForApprove
import kotlinx.serialization.Serializable

@Serializable
internal sealed class OnboardingVisaRoute {

    @Serializable
    data object Welcome : OnboardingVisaRoute()

    @Serializable
    data class WelcomeBack(
        val activationInput: VisaActivationInput,
        val dataToSignByCardWalletRequest: VisaCardWalletDataToSignRequest,
    ) : OnboardingVisaRoute()

    @Serializable
    data object AccessCode : OnboardingVisaRoute()

    @Serializable
    data class ChooseWallet(
        val preparationDataForApprove: PreparationDataForApprove,
    ) : OnboardingVisaRoute()

    @Serializable
    data class TangemWalletApproveOption(
        val preparationDataForApprove: PreparationDataForApprove,
        val foundWalletCardId: String?,
        val allowNavigateBack: Boolean,
    ) : OnboardingVisaRoute()

    @Serializable
    data class OtherWalletApproveOption(
        val preparationDataForApprove: PreparationDataForApprove,
    ) : OnboardingVisaRoute()

    @Serializable
    data class InProgress(
        val from: From,
    ) : OnboardingVisaRoute() {
        enum class From {
            Approve, PinCode
        }
    }

    @Serializable
    data class PinCode(val activationOrderInfo: VisaActivationOrderInfo) : OnboardingVisaRoute()
}