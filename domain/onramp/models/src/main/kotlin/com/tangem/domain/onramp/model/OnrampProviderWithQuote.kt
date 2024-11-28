package com.tangem.domain.onramp.model

import kotlinx.serialization.Serializable

sealed interface OnrampProviderWithQuote {

    val provider: OnrampProvider

    @Serializable
    data class Data(
        override val provider: OnrampProvider,
        val paymentMethod: OnrampPaymentMethod,
        val toAmount: OnrampAmount,
        val fromAmount: OnrampAmount,
    ) : OnrampProviderWithQuote

    sealed interface Unavailable : OnrampProviderWithQuote {

        data class NotSupportedPaymentMethod(
            override val provider: OnrampProvider,
            val availablePaymentMethods: List<OnrampPaymentMethod>,
        ) : Unavailable

        data class AvailableFrom(override val provider: OnrampProvider, val amount: OnrampAmount) : Unavailable
        data class AvailableUpTo(override val provider: OnrampProvider, val amount: OnrampAmount) : Unavailable
    }
}