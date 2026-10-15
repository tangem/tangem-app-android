package com.tangem.domain.wallets.models.errors

sealed class ActivatePromoCodeError {

    data object InvalidPromoCode : ActivatePromoCodeError()

    data object ActivationFailed : ActivatePromoCodeError()

    data object PromocodeAlreadyUsed : ActivatePromoCodeError()

    data object CampaignExpired : ActivatePromoCodeError()

    /** The address or the payout network/currency do not match the promo campaign */
    data object InvalidRequest : ActivatePromoCodeError()

    data object NetworkError : ActivatePromoCodeError()
}