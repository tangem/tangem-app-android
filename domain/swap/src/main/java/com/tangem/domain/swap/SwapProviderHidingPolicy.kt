package com.tangem.domain.swap

import com.tangem.domain.express.models.ExpressError

/** Decides whether a provider is hidden after a failed swap, when its error means it rejected the swap. */
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