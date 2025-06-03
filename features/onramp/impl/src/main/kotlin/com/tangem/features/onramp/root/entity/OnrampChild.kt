package com.tangem.features.onramp.root.entity

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.onramp.model.OnrampProviderWithQuote
import kotlinx.serialization.Serializable

@Serializable
internal sealed class OnrampChild {
    @Serializable
    data object Main : OnrampChild()

    @Serializable
    data object Settings : OnrampChild()

    @Serializable
    data class RedirectPage(val quote: OnrampProviderWithQuote.Data, val cryptoCurrency: CryptoCurrency) : OnrampChild()
}