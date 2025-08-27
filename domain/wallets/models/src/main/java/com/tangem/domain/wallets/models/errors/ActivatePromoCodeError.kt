package com.tangem.domain.wallets.models.errors

sealed class ActivatePromoCodeError {

    data object InvalidPromoCode : ActivatePromoCodeError()

    data object ActivationFailed : ActivatePromoCodeError()

    data object PromocodeAlreadyUsed : ActivatePromoCodeError()

    data object NoBitcoinAddress : ActivatePromoCodeError()
}