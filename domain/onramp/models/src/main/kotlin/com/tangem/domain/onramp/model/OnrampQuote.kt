package com.tangem.domain.onramp.model

import com.tangem.domain.onramp.model.error.OnrampError

sealed class OnrampQuote {

    abstract val paymentMethod: OnrampPaymentMethod
    abstract val provider: OnrampProvider

    data class Data(
        override val paymentMethod: OnrampPaymentMethod,
        override val provider: OnrampProvider,
        val fromAmount: OnrampAmount,
        val toAmount: OnrampAmount,
        val minFromAmount: OnrampAmount,
        val maxFromAmount: OnrampAmount,
    ) : OnrampQuote()

    data class AmountError(
        override val paymentMethod: OnrampPaymentMethod,
        override val provider: OnrampProvider,
        val fromAmount: OnrampAmount,
        val error: OnrampError.AmountError,
    ) : OnrampQuote()

    data class Error(
        override val paymentMethod: OnrampPaymentMethod,
        override val provider: OnrampProvider,
        val error: OnrampError,
    ) : OnrampQuote()
}