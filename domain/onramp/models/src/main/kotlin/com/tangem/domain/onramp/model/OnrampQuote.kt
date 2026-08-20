package com.tangem.domain.onramp.model

import com.tangem.domain.onramp.model.error.OnrampError

sealed class OnrampQuote {

    abstract val paymentMethod: OnrampPaymentMethod
    abstract val provider: OnrampProvider
    abstract val fromAmount: OnrampAmount
    abstract val countryCode: String

    data class Data(
        override val paymentMethod: OnrampPaymentMethod,
        override val provider: OnrampProvider,
        override val fromAmount: OnrampAmount,
        override val countryCode: String,
        val toAmount: OnrampAmount,
        val minFromAmount: OnrampAmount?,
        val maxFromAmount: OnrampAmount?,
        val isRestricted: Boolean = false,
    ) : OnrampQuote()

    data class AmountError(
        override val paymentMethod: OnrampPaymentMethod,
        override val provider: OnrampProvider,
        override val fromAmount: OnrampAmount,
        override val countryCode: String,
        val error: OnrampError.AmountError,
    ) : OnrampQuote()

    data class Error(
        override val paymentMethod: OnrampPaymentMethod,
        override val provider: OnrampProvider,
        override val fromAmount: OnrampAmount,
        override val countryCode: String,
        val error: OnrampError,
    ) : OnrampQuote()
}

/** Token is region-restricted for this provider — the offer is displayable but must not be executable */
val OnrampQuote.isRestricted: Boolean
    get() = this is OnrampQuote.Data && isRestricted