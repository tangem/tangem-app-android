package com.tangem.features.onramp.main.entity

import com.tangem.domain.onramp.model.OnrampProviderWithQuote

interface OnrampIntents {
    fun onAmountValueChanged(value: String)
    fun openSettings()
    fun openCurrenciesList()
    fun onBuyClick(quote: OnrampProviderWithQuote.Data)
    fun openProviders()
    fun onRefresh()
    fun onLinkClick(link: String)
}