package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
sealed class VisaActivationRemoteState {

    @Serializable
    data class CardWalletSignatureRequired(
        val activationOrderInfo: VisaActivationOrderInfo,
    ) : VisaActivationRemoteState()

    @Serializable
    data class CustomerWalletSignatureRequired(
        val activationOrderInfo: VisaActivationOrderInfo,
    ) : VisaActivationRemoteState()

    @Serializable
    data object PaymentAccountDeploying : VisaActivationRemoteState()

    @Serializable
    data class AwaitingPinCode(
        val activationOrderInfo: VisaActivationOrderInfo,
        val status: Status,
    ) : VisaActivationRemoteState() {

        enum class Status {
            WaitingForPinCode, WasError
        }
    }

    @Serializable
    data object WaitingForActivationFinishing : VisaActivationRemoteState()

    @Serializable
    data object Activated : VisaActivationRemoteState()

    @Serializable
    data object BlockedForActivation : VisaActivationRemoteState()

    @Serializable
    data object Failed : VisaActivationRemoteState()
}