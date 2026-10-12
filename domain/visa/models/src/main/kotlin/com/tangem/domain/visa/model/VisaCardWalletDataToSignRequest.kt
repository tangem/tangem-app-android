package com.tangem.domain.visa.model

import kotlinx.serialization.Serializable

@Serializable
data class VisaCardWalletDataToSignRequest(
    val activationOrderInfo: VisaActivationOrderInfo,
    val cardWalletAddress: String,
)