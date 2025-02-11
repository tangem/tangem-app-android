package com.tangem.sdk.api.visa

import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.visa.model.VisaSignedActivationDataByCardWallet

data class VisaCardActivationResponse(
    val signedActivationData: VisaSignedActivationDataByCardWallet,
    val newCardDTO: CardDTO,
)