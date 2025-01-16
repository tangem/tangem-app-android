package com.tangem.domain.visa.model

data class VisaCardActivationResponse(
    val signedActivationOrder: SignedActivationOrder,
    val rootOTP: VisaRootOTP,
)