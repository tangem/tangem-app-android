package com.tangem.domain.onramp.model

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

    sealed class Error : OnrampQuote() {

        data class AmountTooSmallError(
            override val paymentMethod: OnrampPaymentMethod,
            override val provider: OnrampProvider,
            val fromAmount: OnrampAmount,
            val requiredAmount: OnrampAmount,
        ) : Error()

        data class AmountTooBigError(
            override val paymentMethod: OnrampPaymentMethod,
            override val provider: OnrampProvider,
            val fromAmount: OnrampAmount,
            val requiredAmount: OnrampAmount,
        ) : Error()
    }
}