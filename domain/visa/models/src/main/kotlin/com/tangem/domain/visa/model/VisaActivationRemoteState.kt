package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
enum class VisaActivationRemoteState {
    CardWalletSignatureRequired,
    CustomerWalletSignatureRequired,
    PaymentAccountDeploying,
    WaitingPinCode,
    WaitingForActivationFinishing,
    Activated,
    BlockedForActivation,
}