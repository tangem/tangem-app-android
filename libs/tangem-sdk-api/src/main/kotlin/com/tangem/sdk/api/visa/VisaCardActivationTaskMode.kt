package com.tangem.sdk.api.visa

import com.tangem.domain.visa.model.VisaAuthChallenge
import com.tangem.domain.visa.model.VisaDataToSignByCardWallet

sealed class VisaCardActivationTaskMode {
    /**
     * Full activation process with getting remote activation status.
     */
    data class Full(
        val accessCode: String,
        val authorizationChallenge: VisaAuthChallenge.Card,
    ) : VisaCardActivationTaskMode()

    /**
     * Activation process with only sign data by card wallet.
     * This is used when activation process was interrupted and we need to finish it.
     */
    data class SignOnly(val dataToSignByCardWallet: VisaDataToSignByCardWallet) : VisaCardActivationTaskMode()
}