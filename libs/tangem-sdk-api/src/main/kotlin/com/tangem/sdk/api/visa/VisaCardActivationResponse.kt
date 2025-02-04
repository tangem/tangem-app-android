package com.tangem.sdk.api.visa

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.visa.model.SignedActivationOrder
import com.tangem.domain.visa.model.VisaRootOTP

data class VisaCardActivationResponse(
    val signedActivationOrder: SignedActivationOrder,
    val rootOTP: VisaRootOTP,
    val newCardDTO: CardDTO,
)