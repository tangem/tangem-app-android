package com.tangem.domain.swap

import com.tangem.domain.express.models.ExpressError

/**
 * Some providers screen addresses and reject the swap before signing. A provider is hidden when its error means
 * such a rejection: either a dedicated one like [ExpressError.ExchangeProviderAddressBannedByAmlError] or, for
 * providers without one, any error but user-fixable ones, [ExpressError.UnknownError] included.
 */
object SwapProviderHidingPolicy {

    private const val CHANGEHERO_PROVIDER_ID = "changehero"
    private const val CHANGELLY_PROVIDER_ID = "changelly"

    fun shouldHide(providerId: String, error: ExpressError): Boolean = when (providerId) {
        CHANGEHERO_PROVIDER_ID -> error is ExpressError.ExchangeProviderAddressBannedByAmlError
        CHANGELLY_PROVIDER_ID -> !error.isUserResolvable()
        else -> false
    }

    private fun ExpressError.isUserResolvable(): Boolean = when (this) {
        is ExpressError.AmountError.TooSmallError,
        is ExpressError.AmountError.TooBigError,
        is ExpressError.NotEnoughBalanceError,
        is ExpressError.InvalidAddressError,
        is ExpressError.ProviderDifferentAmountError -> true
        else -> false
    }
}