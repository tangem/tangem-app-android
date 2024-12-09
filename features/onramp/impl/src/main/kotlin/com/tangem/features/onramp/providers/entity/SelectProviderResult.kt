package com.tangem.features.onramp.providers.entity

import com.tangem.domain.onramp.model.OnrampAmount
import com.tangem.domain.onramp.model.OnrampPaymentMethod
import com.tangem.domain.onramp.model.OnrampProvider
import com.tangem.domain.onramp.model.OnrampQuote

sealed class SelectProviderResult {

    abstract val paymentMethod: OnrampPaymentMethod
    abstract val provider: OnrampProvider

    data class ProviderWithQuote(
        override val paymentMethod: OnrampPaymentMethod,
        override val provider: OnrampProvider,
        val fromAmount: OnrampAmount,
        val toAmount: OnrampAmount,
    ) : SelectProviderResult()

    data class ProviderWithError(
        override val paymentMethod: OnrampPaymentMethod,
        override val provider: OnrampProvider,
        val quoteError: OnrampQuote.Error,
    ) : SelectProviderResult()
}