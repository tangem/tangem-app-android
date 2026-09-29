package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
sealed class VisaCardActivationStatus {

    @Serializable
    data class Activated(
        val visaAuthTokens: VisaAuthTokens,
    ) : VisaCardActivationStatus()

    @Serializable
    data class ActivationStarted(
        val activationInput: VisaActivationInput,
        val authTokens: VisaAuthTokens,
        val remoteState: VisaActivationRemoteState,
        val cardWalletAddress: String,
    ) : VisaCardActivationStatus()

    @Serializable
    data class NotStartedActivation(
        val activationInput: VisaActivationInput,
    ) : VisaCardActivationStatus()

    @Serializable
    data object Blocked : VisaCardActivationStatus()

    @Serializable
    data object RefreshTokenExpired : VisaCardActivationStatus()
}